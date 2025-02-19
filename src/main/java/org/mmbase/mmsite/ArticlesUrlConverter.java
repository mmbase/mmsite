/*

This file is part of the MMBase MMSite application, 
which is part of MMBase - an open source content management system.
    Copyright (C) 2009 André van Toly

MMBase MMSite is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

MMBase MMSite is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with MMBase. If not, see <http://www.gnu.org/licenses/>.

*/

package org.mmbase.mmsite;

import org.mmbase.bridge.Cloud;
import org.mmbase.bridge.Node;
import org.mmbase.bridge.NodeList;
import org.mmbase.bridge.Query;
import org.mmbase.bridge.util.Queries;
import org.mmbase.framework.*;
import org.mmbase.framework.basic.BasicFramework;
import org.mmbase.framework.basic.BasicUrl;
import org.mmbase.framework.basic.DirectoryUrlConverter;
import org.mmbase.framework.basic.Url;
import org.mmbase.util.functions.Parameter;
import org.mmbase.util.functions.Parameters;
import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;
import org.mmbase.util.transformers.CharTransformer;
import org.mmbase.util.transformers.Identifier;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

/**
 * UrlConverter that can filter and create urls for articles, f.e. '/articles/2345/article_title'
 * This UrlConverter works on the second last piece of the path, the nodenumber. The last part of 
 * the path, 'article_title' does nothing. When related with 'posrel' to a 'pages' node, its path 
 * gets prepended to the link f.e. when the article is published on a page named 'News' it
 * can become '/news/articles/2345/article_title'. This UrlConverter relies on 
 * {@link SiteUrlConverter} for multilanguage and the optional use of an extension.
 * <br />
 *  &lt;urlconverter class="org.mmbase.mmsite.ArticlesUrlConverter"&gt;
 *    &lt;description xml:lang="en"&gt;
 *      An UrlConverter the create nice looking link for articles in mmsite.
 *    &lt;/description&gt;
 *    &lt;param name="directory"&gt;/&lt;/param&gt;
 *    &lt;param name="useTitle"&gt;true&lt;/param&gt;
 *    &lt;param name="template"&gt;/article.jsp&lt;/param&gt;
 *  &lt;/urlconverter&gt;
 *
 * @author Andr&eacute; van Toly
 * @version $Id: ArticlesUrlConverter.java 46725 2015-08-04 13:48:39Z andre $
 * @since MMBase-1.9
 */
public class ArticlesUrlConverter extends DirectoryUrlConverter {
    private static final long serialVersionUID = 0L;
    private static final Logger log = Logging.getLoggerInstance(ArticlesUrlConverter.class);

    private static CharTransformer trans = new Identifier();
    private boolean useTitle = false;
    private String template = "/article.jspx";

    private final LocaleUtil  localeUtil = LocaleUtil.getInstance();

    public ArticlesUrlConverter(BasicFramework fw) {
        super(fw);
        setDirectory("/articles/");
        addBlock(ComponentRepository.getInstance().getComponent("mmsite").getBlock("article"));
        addBlock(ComponentRepository.getInstance().getComponent("mmsite").getBlock("article-comment"));
    }

    public void setUseTitle(boolean t) {
        useTitle = t;
    }
    
    public void setTemplate(String t) {
        template = t;
    }

    @Override public int getDefaultWeight() {
        int q = super.getDefaultWeight();
        return Math.max(q, q + 2001);
    }

    public static final Parameter<Node> ARTICLE = new Parameter<Node>("article", Node.class);

    @Override
    public Parameter[] getParameterDefinition() {
        return new Parameter[] {Parameter.REQUEST, Framework.COMPONENT, Framework.BLOCK, Parameter.CLOUD, ARTICLE, LocaleUtil.LOCALE};
    }

    @Override
    public boolean isFilteredMode(Parameters frameworkParameters) throws FrameworkException {
        HttpServletRequest request = org.mmbase.framework.basic.BasicUrlConverter.getUserRequest(frameworkParameters.get(Parameter.REQUEST));
        String path = FrameworkFilter.getPath(request);
        for (String e : SiteUrlConverter.getInstance().excludedPaths) {
            if (path.startsWith("/" + e + "/")) {
                return false;
            }
        }
        return super.isFilteredMode(frameworkParameters);
    }
    
    /**
     * Generates a nice url for an 'articles'.
     */
    @Override protected void getNiceDirectoryUrl(StringBuilder b,
                                                 Block block,
                                                 Parameters parameters,
                                                 Parameters frameworkParameters,  boolean action) throws FrameworkException {
        if (log.isDebugEnabled()) {
            log.debug("" + parameters + frameworkParameters);
            log.debug("Found mmsite block: " + block);
        }
        int b_len = b.length();
        if (block.getName().indexOf("article") > -1) {
            Node n = frameworkParameters.get(ARTICLE);
            if (n == null) throw new IllegalStateException("No articles parameter used in " + frameworkParameters);
            
            // check if related to pages
            Cloud cloud = frameworkParameters.get(Parameter.CLOUD);
            //Cloud cloud = n.getCloud();
            Query query = Queries.createRelatedNodesQuery(n, cloud.getNodeManager("pages"), "posrel", "source");
            NodeList nl = cloud.getList(query);
            if (nl.size() > 1) log.warn(nl.size() + " pages found related to articles #" + n.getNumber() + ", could only return first !");
            if (nl.size() > 0) {
                Node clusterNode = nl.getNode(0);
                Node pages = cloud.getNode(clusterNode.getIntValue("pages.number"));
                if (log.isDebugEnabled()) log.debug("Found pages: " + pages.getNumber());
                
                String path = pages.getStringValue("path");
                if (path.startsWith("/")) {
                    path = path.substring(1, path.length());
                }
                if (path.endsWith("/")) path = path.substring(0, path.length() - 1);
                b.append(path);
            }

            b.append("/").append(n.getNumber());
            if (useTitle) {
                b.append("/").append(trans.transform(n.getStringValue("title")));
            }

            /* comment */
            if (block.getName().equals("article-comment")) {
                b.append("/comment");
                String preview = (String) parameters.get("preview");
                parameters.set("preview", null);
                
                if (preview != null && !"".equals(preview)) {
                    b.append("/preview");
                }
            }
            
            if (b.length() > b_len) {   // check if url is altered
                if (SiteUrlConverter.getInstance().useExtension) {
                    b.append(".").append(SiteUrlConverter.getInstance().extension);
                }
            }
            localeUtil.appendLanguage(b, frameworkParameters);

            if (log.isDebugEnabled()) {
                log.debug("b: " + b.toString());
            }
        }
    }


    /**
     * Translates the result of {@link #getNiceUrl} back to an actual JSP which can render the block.
     * Articles always get resolved by nodenumber. Structure of this url can be:
     *   /234/article_title
     *   /page_path/345/article_title
     *   /page_path/345/article_title/comment
     */
    @Override
    public Url getFilteredInternalDirectoryUrl(List<String>  path, Map<String, ?> params, Parameters frameworkParameters) throws FrameworkException {
        if (log.isDebugEnabled()) log.debug("path pieces: " + path + ", path size: " + path.size());

        HttpServletRequest request = frameworkParameters.get(Parameter.REQUEST);

        StringBuilder result = new StringBuilder();
        if (path.isEmpty()) {
            return Url.NOT;
        } else {
            result.append(template).append("?n=");

            // last element can contain language and/or extension
            String last = path.get(path.size() - 1); 
            last = localeUtil.setLanguage(last, request);
            if (SiteUrlConverter.getInstance().useExtension && last.indexOf(SiteUrlConverter.getInstance().extension) > -1) {
                last = last.substring(0, last.lastIndexOf(SiteUrlConverter.getInstance().extension) - 1);
            }
            path.set(path.size() - 1, last);    // put it back
            
            String nr;
            if (path.size() > 0) {
                
                /* TODO: comment (not configurable (yet), always 'comment' and 'comment.jsp') */
                if (path.get(path.size() - 1).equals("comment")) {
                    path.remove(path.size() - 1);
                    if (log.isDebugEnabled()) log.debug("comment! path now: " + path);
                    result = new StringBuilder("/comment.jsp?n=");
                } else if (path.get(path.size() - 1).equals("preview")) {
                    path.remove(path.size() - 1);
                    path.remove(path.size() - 1);
                    if (log.isDebugEnabled()) log.debug("preview! path now: " + path);
                    result = new StringBuilder("/comment.jsp?preview=preview&n=");
                }
                
                if (useTitle && path.size() > 1) { // uses title: nodenumber is 2nd last element
                    nr = path.get(path.size() - 2);
                } else {
                    nr = path.get(path.size() - 1);
                }
                
            } else {
                if (log.isDebugEnabled()) log.debug("path not > 0");
                return Url.NOT;
            }
            
            Cloud cloud = frameworkParameters.get(Parameter.CLOUD);
            if (log.isDebugEnabled()) log.debug("articles nr: " + nr);
            if (cloud != null && cloud.hasNode(nr)) {
                Node article = cloud.getNode(nr);
                if (! article.getNodeManager().getName().equals("articles")) {
                    return Url.NOT;
                } else if (! article.getBooleanValue("show")) {
                    log.warn("Articles not shown: " + article.getBooleanValue("show"));
                    return Url.NOT;
                } else {
                    frameworkParameters.set(ARTICLE, article);
                    result.append(nr);
                }
            } else {
                // node not found
                return Url.NOT;
            }

        }

        if (log.isDebugEnabled()) log.debug("returning: " + result.toString());
        return new BasicUrl(this, result.toString());
    }

}

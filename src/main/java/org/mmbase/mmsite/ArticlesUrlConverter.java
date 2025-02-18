package org.mmbase.mmsite;

import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.mmbase.bridge.Cloud;
import org.mmbase.bridge.Node;
import org.mmbase.bridge.NodeList;
import org.mmbase.bridge.util.Queries;
import org.mmbase.framework.Block;
import org.mmbase.framework.ComponentRepository;
import org.mmbase.framework.Framework;
import org.mmbase.framework.FrameworkException;
import org.mmbase.framework.FrameworkFilter;
import org.mmbase.framework.basic.BasicFramework;
import org.mmbase.framework.basic.BasicUrl;
import org.mmbase.framework.basic.BasicUrlConverter;
import org.mmbase.framework.basic.DirectoryUrlConverter;
import org.mmbase.framework.basic.Url;
import org.mmbase.util.functions.Parameter;
import org.mmbase.util.functions.Parameters;
import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;
import org.mmbase.util.transformers.CharTransformer;
import org.mmbase.util.transformers.Identifier;

/* loaded from: ArticlesUrlConverter.class */
public class ArticlesUrlConverter extends DirectoryUrlConverter {
    private static final long serialVersionUID = 0;
    private boolean useTitle;
    private String template;
    private final LocaleUtil localeUtil;
    private static final Logger log = Logging.getLoggerInstance(ArticlesUrlConverter.class);
    private static CharTransformer trans = new Identifier();
    public static final Parameter<Node> ARTICLE = new Parameter<>("article", Node.class);

    public ArticlesUrlConverter(BasicFramework fw) {
        super(fw);
        this.useTitle = false;
        this.template = "/article.jspx";
        this.localeUtil = LocaleUtil.getInstance();
        setDirectory("/articles/");
        addBlock(ComponentRepository.getInstance().getComponent("mmsite").getBlock("article"));
        addBlock(ComponentRepository.getInstance().getComponent("mmsite").getBlock("article-comment"));
    }

    public void setUseTitle(boolean t) {
        this.useTitle = t;
    }

    public void setTemplate(String t) {
        this.template = t;
    }

    public int getDefaultWeight() {
        int q = super.getDefaultWeight();
        return Math.max(q, q + 2001);
    }

    public Parameter[] getParameterDefinition() {
        return new Parameter[]{Parameter.REQUEST, Framework.COMPONENT, Framework.BLOCK, Parameter.CLOUD, ARTICLE, LocaleUtil.LOCALE};
    }

    public boolean isFilteredMode(Parameters frameworkParameters) throws FrameworkException {
        HttpServletRequest request = BasicUrlConverter.getUserRequest((HttpServletRequest) frameworkParameters.get(Parameter.REQUEST));
        String path = FrameworkFilter.getPath(request);
        for (String e : SiteUrlConverter.getInstance().excludedPaths) {
            if (path.startsWith("/" + e + "/")) {
                return false;
            }
        }
        return super.isFilteredMode(frameworkParameters);
    }

    protected void getNiceDirectoryUrl(StringBuilder b, Block block, Parameters parameters, Parameters frameworkParameters, boolean action) throws FrameworkException {
        if (log.isDebugEnabled()) {
            log.debug("" + parameters + frameworkParameters);
            log.debug("Found mmsite block: " + block);
        }
        int b_len = b.length();
        if (block.getName().indexOf("article") > -1) {
            Node n = (Node) frameworkParameters.get(ARTICLE);
            if (n == null) {
                throw new IllegalStateException("No articles parameter used in " + frameworkParameters);
            }
            Cloud cloud = (Cloud) frameworkParameters.get(Parameter.CLOUD);
            NodeList nl = cloud.getList(Queries.createRelatedNodesQuery(n, cloud.getNodeManager("pages"), "posrel", "source"));
            if (nl.size() > 1) {
                log.warn(nl.size() + " pages found related to articles #" + n.getNumber() + ", could only return first !");
            }
            if (nl.size() > 0) {
                Node clusterNode = nl.getNode(0);
                Node pages = cloud.getNode(clusterNode.getIntValue("pages.number"));
                if (log.isDebugEnabled()) {
                    log.debug("Found pages: " + pages.getNumber());
                }
                String path = pages.getStringValue("path");
                if (path.startsWith("/")) {
                    path = path.substring(1, path.length());
                }
                if (path.endsWith("/")) {
                    path = path.substring(0, path.length() - 1);
                }
                b.append(path);
            }
            b.append("/").append(n.getNumber());
            if (this.useTitle) {
                b.append("/").append(trans.transform(n.getStringValue("title")));
            }
            if (block.getName().equals("article-comment")) {
                b.append("/comment");
                String preview = (String) parameters.get("preview");
                parameters.set("preview", (Object) null);
                if (preview != null && !"".equals(preview)) {
                    b.append("/preview");
                }
            }
            if (b.length() > b_len && SiteUrlConverter.getInstance().useExtension) {
                b.append(".").append(SiteUrlConverter.getInstance().extension);
            }
            this.localeUtil.appendLanguage(b, frameworkParameters);
            if (log.isDebugEnabled()) {
                log.debug("b: " + b.toString());
            }
        }
    }

    public Url getFilteredInternalDirectoryUrl(List<String> path, Map<String, ?> params, Parameters frameworkParameters) throws FrameworkException {
        String nr;
        if (log.isDebugEnabled()) {
            log.debug("path pieces: " + path + ", path size: " + path.size());
        }
        HttpServletRequest request = (HttpServletRequest) frameworkParameters.get(Parameter.REQUEST);
        StringBuilder result = new StringBuilder();
        if (path.isEmpty()) {
            return Url.NOT;
        }
        result.append(this.template).append("?n=");
        String last = this.localeUtil.setLanguage(path.get(path.size() - 1), request);
        if (SiteUrlConverter.getInstance().useExtension && last.indexOf(SiteUrlConverter.getInstance().extension) > -1) {
            last = last.substring(0, last.lastIndexOf(SiteUrlConverter.getInstance().extension));
        }
        path.set(path.size() - 1, last);
        if (path.size() > 0) {
            if (path.get(path.size() - 1).equals("comment")) {
                path.remove(path.size() - 1);
                if (log.isDebugEnabled()) {
                    log.debug("comment! path now: " + path);
                }
                result = new StringBuilder("/comment.jsp?n=");
            } else if (path.get(path.size() - 1).equals("preview")) {
                path.remove(path.size() - 1);
                path.remove(path.size() - 1);
                if (log.isDebugEnabled()) {
                    log.debug("preview! path now: " + path);
                }
                result = new StringBuilder("/comment.jsp?preview=preview&n=");
            }
            if (this.useTitle && path.size() > 1) {
                nr = path.get(path.size() - 2);
            } else {
                nr = path.get(path.size() - 1);
            }
            Cloud cloud = (Cloud) frameworkParameters.get(Parameter.CLOUD);
            if (log.isDebugEnabled()) {
                log.debug("articles nr: " + nr);
            }
            if (cloud != null && cloud.hasNode(nr)) {
                Node article = cloud.getNode(nr);
                if (!article.getNodeManager().getName().equals("articles")) {
                    return Url.NOT;
                }
                if (!article.getBooleanValue("show")) {
                    log.warn("Articles not shown: " + article.getBooleanValue("show"));
                    return Url.NOT;
                }
                frameworkParameters.set(ARTICLE, article);
                result.append(nr);
                if (log.isDebugEnabled()) {
                    log.debug("returning: " + result.toString());
                }
                return new BasicUrl(this, result.toString());
            }
            return Url.NOT;
        }
        if (log.isDebugEnabled()) {
            log.debug("path not > 0");
        }
        return Url.NOT;
    }
}

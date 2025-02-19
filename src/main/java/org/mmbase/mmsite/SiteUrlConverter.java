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
import org.mmbase.bridge.ContextProvider;
import org.mmbase.bridge.Node;
import org.mmbase.bridge.NotFoundException;
import org.mmbase.framework.*;
import org.mmbase.framework.basic.BasicFramework;
import org.mmbase.framework.basic.BasicUrl;
import org.mmbase.framework.basic.DirectoryUrlConverter;
import org.mmbase.framework.basic.Url;
import org.mmbase.util.functions.Parameter;
import org.mmbase.util.functions.Parameters;
import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * The UrlConverter that can filter and create urls for pages in the site application.
 * It can be used as a '/' (root) UrlConverter. Use 'excludedPaths' to list directories to
 * exclude that might get mixed up with this one and are not mentioned in web.xml.
 * It presumes (pages) nodes with the fields 'path' and 'template'.
 * When multilanguage is turned on pages can get extensions like '.nl' and 'en', f.e. '/page.nl',
 * that can be used to display content in a different language. For this to happen a request
 * should be made with '_lang=[languagecode]' and an attribute 'org.mmbase.mmsite.language' will
 * be put on the request for further use.<br />
 * <br />
 * &lt;urlconverter class="org.mmbase.mmsite.SiteUrlConverter"&gt;<br />
 *   &lt;description xml:lang="en"&gt;UrlConverter for MMSite&lt;/description&gt;<br />
 *   &lt;param name="directory"&gt;/&lt;/param&gt;<br />
 *   &lt;param name="excludedPaths"&gt;mmbase,mmexamples&lt;/param&gt;<br />
 *   &lt;param name="useExtension"&gt;true&lt;/param&gt;<br />
 *   &lt;param name="extension"&gt;html&lt;/param&gt;<br />
 *   &lt;param name="multilanguage"&gt;true&lt;/param&gt;<br />
 * &lt;/urlconverter&gt;
 *
 * @author Andr&eacute; van Toly
 * @version $Id: SiteUrlConverter.java 46725 2015-08-04 13:48:39Z andre $
 * @since MMBase-1.9
 */
public class SiteUrlConverter extends DirectoryUrlConverter {
    private static final long serialVersionUID = 0L;
    private static final Logger log = Logging.getLoggerInstance(SiteUrlConverter.class);

    protected final List<String> excludedPaths = new ArrayList<String>();
    protected String extension = "html";
    protected boolean useExtension = false;
    private   final LocaleUtil localeUtil = LocaleUtil.getInstance();
    private static SiteUrlConverter instance;
    
    @SuppressWarnings("LeakingThisInConstructor")
    public SiteUrlConverter(BasicFramework fw) {
        super(fw);
        setDirectory("/");
        addBlock(ComponentRepository.getInstance().getComponent("mmsite").getBlock("page"));
        instance = this;
    }
    /**
     * 
     * @return The last instance of a SiteUrlConverter
     */
    public static SiteUrlConverter getInstance() {
        return instance;
    }

    public void setExcludedPaths(String l) {
        excludedPaths.clear();
        excludedPaths.addAll(Arrays.asList(l.split(",")));
    }

    public void setUseExtension(boolean t) {
        useExtension = t;
    }

    public void setExtension(String e) {
        extension = e;
    }


    @Override
    public int getDefaultWeight() {
        int q = super.getDefaultWeight();
        return Math.max(q, q + 2000);
    }


    @Override
    public Parameter[] getParameterDefinition() {
        return new Parameter[] {Parameter.REQUEST, Framework.COMPONENT, Framework.BLOCK, LocaleUtil.LOCALE};
    }

    @Override
    public boolean isFilteredMode(Parameters frameworkParameters) throws FrameworkException {
        HttpServletRequest request = org.mmbase.framework.basic.BasicUrlConverter.getUserRequest(frameworkParameters.get(Parameter.REQUEST));
        String path = FrameworkFilter.getPath(request);
        for (String e : excludedPaths) {
            if (path.startsWith("/" + e + "/")) {
                return false;
            }
        }
        return super.isFilteredMode(frameworkParameters);
    }

    /**
     * Generates a nice url linking to a template for a ('pages') node.
     */
    @Override
    protected void getNiceDirectoryUrl(StringBuilder b, Block block, Parameters parameters, Parameters frameworkParameters,  boolean action) throws FrameworkException {
        if (log.isDebugEnabled()) {
            log.debug("" + parameters + frameworkParameters);
            log.debug("Found mmsite block: " + block);
        }
        
        int b_len = b.length();
        
        if (block.getName().equals("page")) {
            
            Node n = parameters.get(Framework.N);
            if (n == null) {
                throw new IllegalStateException("No node parameter used in " + frameworkParameters);
            } else {
                parameters.set(Framework.N, null);
                
                String path = n.getStringValue("path");
                if (path.startsWith("/")) {
                    path = path.substring(1, path.length());
                }
                if (path.endsWith("/")) path = path.substring(0, path.length() - 1);
                b.append(path);
    
                if (b.length() > b_len) {   // check if url is altered
                    if (useExtension) {
                        b.append(".").append(extension);
                    }
                }
                
                localeUtil.appendLanguage(b, frameworkParameters);
                
            }

        }

        if (log.isDebugEnabled()) {
            log.debug("b: " + b.toString());
        }
    }


    /**
     * Translates the result of {@link #getNiceUrl} back to an actual JSP which can render the block
     */
    @Override
    public Url getFilteredInternalDirectoryUrl(List<String> pa, Map<String, ?> params, Parameters frameworkParameters) throws FrameworkException {
        if (log.isDebugEnabled()) {
            log.debug("params: " + params + "fw: " + frameworkParameters);
            log.debug("path pieces: " + pa + ", path size: " + pa.size());
        }

        HttpServletRequest request = frameworkParameters.get(Parameter.REQUEST);

        StringBuilder result = new StringBuilder();
        Cloud cloud = ContextProvider.getDefaultCloudContext().getCloud("mmbase");
        if (pa.size() > 0 && excludedPaths.contains(pa.get(0))) {
            if (log.isDebugEnabled()) {
                log.debug("Returning null, path in excludepaths: " + pa.get(0));
            }
            return Url.NOT;
        }

        StringBuilder sb = new StringBuilder();
        for (String piece: pa) {
            sb.append("/").append(piece);
        }
        String path = sb.toString();
        if (log.isDebugEnabled()) {
            log.debug("path: " + path);
        }

        /* language last after extension: index.html.es */
        try {
            path = localeUtil.setLanguage(path, request);
        } catch (NotFoundException nfe) {
            log.warn(nfe); // WTF
            return Url.NOT;
        }

        if (useExtension && path.indexOf(extension) > -1) {
            path = path.substring(0, path.lastIndexOf(extension) - 1);
        }

        Node node = UrlUtils.getPagebyPath(request, cloud, cloud.getNodeManager("pages"), path);
        if (node != null) {
            String template = node.getNodeValue("template").getStringValue("url");
            if (!template.startsWith("/")) {
                result.append("/");
            }
            char connector = template.indexOf("?") == -1 ? '?' : '&';
            result.append(template).append(connector).append("n=" + node.getNumber());
        } else {
            if (log.isDebugEnabled()) {
                log.debug("No node found for '" + path + "'");
            }
            return Url.NOT;
        }

        if (log.isDebugEnabled()) {
            log.debug("Returning: " + result.toString());
        }
        return new BasicUrl(this, result.toString());

    }

}

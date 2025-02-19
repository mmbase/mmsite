/*

This file is part of the MMBase MMSite application, 
which is part of MMBase - an open source content management system.
    Copyright (C) 2011 André van Toly

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

import java.util.*;

import org.mmbase.bridge.*;
import org.mmbase.bridge.util.Queries;
import org.mmbase.util.functions.NodeFunction;
import org.mmbase.util.functions.Parameter;
import org.mmbase.util.functions.Parameters;
import org.mmbase.util.LocalizedString;
import org.mmbase.bridge.util.NodeMap;
import org.mmbase.bridge.util.MapNode;
import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;


/**
 * Finds a nodes translated node. For example the translation of a node of type 'articles' has a
 * translation in a node of type 'articles_translations' to which it is related via a 'langrel'.
 * The nodemanager to hold the translations can be specified with the property 'translations.builder',
 * otherwise a nodemanager will be guessed by appending '_translations'.
 * Only the translatable fields are part of 'articles_translations', fields like dates etc. are
 * ommited. The same untranslated node is returned when no translation is found. 
 * If a field is not translated (yet), the value of the original node is used.
 * 
 * @author Andr&eacute; van Toly
 * @version $Id: NodeTranslation.java 45648 2011-04-04 19:00:18Z andre $
 */
public final class NodeTranslation extends NodeFunction<Node> {
    private static final long serialVersionUID = 0L;
    private static final Logger log = Logging.getLoggerInstance(NodeTranslation.class);
    
    public NodeTranslation() {
        super("nodetranslation", Parameter.LOCALE);
    }

    
    @Override
    public Node getFunctionValue(Node node, Parameters parameters) {
        Node translation = null;
        Cloud cloud = node.getCloud();
        NodeManager nm = node.getNodeManager();
        String translations_builder = nm.getProperty("translations.builder");
        if (translations_builder == null) {
            translations_builder = nm.getName() + "_translations";
        }
        NodeManager translationsNM = cloud.getNodeManager(translations_builder);
        
        Locale loc = parameters.get(Parameter.LOCALE);
        Locale oriloc = loc;
        String lang = loc.toString();
        
        try {
            while (loc != null && translation == null) {
                if (log.isDebugEnabled()) {
                    log.debug("Trying to find a translation in: " + loc.toString());
                }
                Query query = Queries.createRelatedNodesQuery(node, translationsNM, "langrel", "destination");
                Queries.addConstraint(query, Queries.createConstraint(query, "language", Queries.getOperator("EQUAL"), loc.toString(), null, true));
                if (log.isTraceEnabled()) {
                    log.trace("query: " + query.toSql());
                }
                
                NodeList nl = cloud.getList(query);
                if (nl.size() > 1) {
                    log.warn(nl.size() + " translations found in '" + lang + "' for node " + node.getNumber() + " !");
                }
                
                if (nl.size() > 0) {
                    Node clusterNode = nl.getNode(0); // clusternode
                    translation = cloud.getNode(clusterNode.getIntValue(translationsNM.getName() + ".number"));
                    
                    if (log.isDebugEnabled()) {
                        log.debug("Found: " + node.getNumber());
                    }
                } else {
                    loc = LocalizedString.degrade(loc, oriloc);
                }
            }
        
        } catch (Exception e) {
            log.error("Exception while building query: " + e);
        }
        
        Map<String, Object> map = new HashMap<String, Object>();
        map.putAll(new NodeMap(node));
        if (translation != null) {
            
            Map<String, Object> translationMap = new NodeMap(translation);
            Iterator<Map.Entry<String,Object>> iter = translationMap.entrySet().iterator();
            
            while(iter.hasNext()) {
                Map.Entry<String,Object> e = iter.next();
                
                // overwrite only non-empty and non-system fields
                String fldName = e.getKey();
                if (!"number".equals(fldName) && 
                    !"owner".equals(fldName) && 
                    !"otype".equals(fldName) && 
                    !"language".equals(fldName) &&
                    e.getValue() != null && !"".equals(e.getValue())) {
                    
                    map.put(fldName, e.getValue());
                }
            }
        }
        
        return new MapNode<Object>(map, cloud);
    }

}

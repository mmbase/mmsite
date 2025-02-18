package org.mmbase.mmsite;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.mmbase.bridge.Cloud;
import org.mmbase.bridge.Node;
import org.mmbase.bridge.NodeList;
import org.mmbase.bridge.NodeManager;
import org.mmbase.bridge.NodeQuery;
import org.mmbase.bridge.util.MapNode;
import org.mmbase.bridge.util.NodeMap;
import org.mmbase.bridge.util.Queries;
import org.mmbase.util.LocalizedString;
import org.mmbase.util.functions.NodeFunction;
import org.mmbase.util.functions.Parameter;
import org.mmbase.util.functions.Parameters;
import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;

/* loaded from: NodeTranslation.class */
public final class NodeTranslation extends NodeFunction<Node> {
    private static final long serialVersionUID = 0;
    private static final Logger log = Logging.getLoggerInstance(NodeTranslation.class);

    public NodeTranslation() {
        super("nodetranslation", new Parameter[]{Parameter.LOCALE});
    }

    /* renamed from: getFunctionValue */
    public Node m4getFunctionValue(Node node, Parameters parameters) {
        Node translation = null;
        Cloud cloud = node.getCloud();
        NodeManager nm = node.getNodeManager();
        String translations_builder = nm.getProperty("translations.builder");
        if (translations_builder == null) {
            translations_builder = nm.getName() + "_translations";
        }
        NodeManager translationsNM = cloud.getNodeManager(translations_builder);
        Locale loc = (Locale) parameters.get(Parameter.LOCALE);
        String lang = loc.toString();
        while (loc != null && translation == null) {
            try {
                if (log.isDebugEnabled()) {
                    log.debug("Trying to find a translation in: " + loc.toString());
                }
                NodeQuery createRelatedNodesQuery = Queries.createRelatedNodesQuery(node, translationsNM, "langrel", "destination");
                Queries.addConstraint(createRelatedNodesQuery, Queries.createConstraint(createRelatedNodesQuery, "language", Queries.getOperator("EQUAL"), loc.toString(), (Object) null, true));
                if (log.isTraceEnabled()) {
                    log.trace("query: " + createRelatedNodesQuery.toSql());
                }
                NodeList nl = cloud.getList(createRelatedNodesQuery);
                if (nl.size() > 1) {
                    log.warn(nl.size() + " translations found in '" + lang + "' for node " + node.getNumber() + " !");
                }
                if (nl.size() <= 0) {
                    loc = LocalizedString.degrade(loc, loc);
                } else {
                    Node clusterNode = nl.getNode(0);
                    translation = cloud.getNode(clusterNode.getIntValue(translationsNM.getName() + ".number"));
                    if (log.isDebugEnabled()) {
                        log.debug("Found: " + node.getNumber());
                    }
                }
            } catch (Exception e) {
                log.error("Exception while building query: " + e);
            }
        }
        Map<String, Object> map = new HashMap<>();
        map.putAll(new NodeMap(node));
        if (translation != null) {
            for (Map.Entry<String, Object> e2 : new NodeMap(translation).entrySet()) {
                String fldName = e2.getKey();
                if (!"number".equals(fldName) && !"owner".equals(fldName) && !"otype".equals(fldName) && !"language".equals(fldName) && e2.getValue() != null && !"".equals(e2.getValue())) {
                    map.put(fldName, e2.getValue());
                }
            }
        }
        return new MapNode(map, cloud);
    }
}

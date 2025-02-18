package org.mmbase.mmsite;

import org.mmbase.bridge.Field;
import org.mmbase.bridge.Node;
import org.mmbase.bridge.NodeList;
import org.mmbase.bridge.util.SearchUtil;
import org.mmbase.datatypes.processors.CommitProcessor;
import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;

/* loaded from: DeleteTranslationsProcessor.class */
public class DeleteTranslationsProcessor implements CommitProcessor {
    private static final long serialVersionUID = 0;
    public static String NOT = DeleteTranslationsProcessor.class.getName() + ".DONOT";
    private static final Logger LOG = Logging.getLoggerInstance(DeleteTranslationsProcessor.class);

    public void commit(Node node, Field field) {
        if (node.getCloud().getProperty(NOT) != null) {
            LOG.service("Not doing because of property");
            return;
        }
        String builder = node.getNodeManager().getProperty("translations.builder");
        if (node.getNumber() > 0 && builder != null && !"".equals(builder)) {
            NodeList translations = SearchUtil.findRelatedNodeList(node, builder, "langrel");
            LOG.info("Deleting " + translations.size() + " " + builder + " of #" + node.getNumber());
            for (Node tr : translations) {
                if (tr.mayDelete()) {
                    tr.delete(true);
                } else {
                    LOG.warn("May not delete #" + tr);
                }
            }
        }
    }
}

package org.mmbase.mmsite;

import org.mmbase.bridge.Field;
import org.mmbase.bridge.Node;
import org.mmbase.datatypes.processors.Related;
import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;

/* loaded from: RelatedCommentField.class */
public class RelatedCommentField {
    private static final Logger log = Logging.getLoggerInstance(RelatedCommentField.class);

    /* loaded from: RelatedCommentField$AbstractProcessor.class */
    public static abstract class AbstractProcessor extends Related.AbstractProcessor {
        protected String otherField = null;
        protected String prefixWith = null;

        public void setField(String f) {
            this.otherField = f;
        }

        public void setPrefix(String p) {
            this.prefixWith = p;
        }
    }

    /* loaded from: RelatedCommentField$Getter.class */
    public static class Getter extends AbstractProcessor {
        private static final long serialVersionUID = 1;

        public Object process(Node node, Field field, Object value) {
            if (RelatedCommentField.log.isDebugEnabled()) {
                RelatedCommentField.log.debug("getting " + node);
            }
            Node otherNode = getRelatedNode(node, field);
            if (otherNode == null || value != null) {
                RelatedCommentField.log.debug("No related node");
                if (value == null) {
                    return this.prefixWith;
                }
                return value;
            }
            String fieldName = this.otherField == null ? field.getName() : this.otherField;
            return this.prefixWith + otherNode.getValue(fieldName);
        }
    }
}

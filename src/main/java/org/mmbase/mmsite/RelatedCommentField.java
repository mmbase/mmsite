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

import java.util.*;

import org.mmbase.bridge.*;
import org.mmbase.datatypes.processors.*;
import org.mmbase.util.logging.*;

/**
 * The set- and get- processors implemented in this file can be used to make a virtual field which
 * acts as a field of a related node. Based on {@link org.mmbase.datatypes.processors.RelatedField}
 *
 *
 * @author Andre van Toly
 * @since MMBase-1.9.6
 * @version $Id: RelatedCommentField.java 43833 2010-11-25 11:44:51Z andre $
 */

public class RelatedCommentField {

    private static final Logger log = Logging.getLoggerInstance(RelatedCommentField.class);


    public abstract static class AbstractProcessor extends Related.AbstractProcessor {

        protected String otherField = null;
        protected String prefixWith = null;

        public void setField(String f) {
            otherField  = f;
        }

        public void setPrefix(String p) {
            prefixWith  = p;
        }

    }

    public static class Getter extends AbstractProcessor {
        private static final long serialVersionUID = 1L;

        public Object process(Node node, Field field, Object value) {
            if (log.isDebugEnabled()) {
                log.debug("getting "  + node);
            }
            
            Node otherNode = getRelatedNode(node, field);
            if (otherNode != null && value == null) {
                String fieldName = otherField == null ? field.getName() : otherField;
                return prefixWith + otherNode.getValue(fieldName);
            } else {
                log.debug("No related node");
                if (value == null) {
                    return prefixWith;
                } else {
                    return value;
                }
            }
        }
    }

}

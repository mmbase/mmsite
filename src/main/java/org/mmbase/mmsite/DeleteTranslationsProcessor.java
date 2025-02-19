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

import org.mmbase.bridge.*;
import org.mmbase.bridge.util.SearchUtil;
import org.mmbase.datatypes.processors.*;
import org.mmbase.util.logging.*;


/**
 * This commit-processor is used on translations of nodes, normally ending with 
 * '[nodetype]_translations'. While deleting the original node it also deletes
 * the related translations.
 *
 * @author André van Toly
 * @version $Id: DeleteTranslationsProcessor.java 46723 2015-08-04 12:41:33Z andre $
 */

public class DeleteTranslationsProcessor implements CommitProcessor {
    private static final long serialVersionUID = 0L;

    public static String NOT = DeleteTranslationsProcessor.class.getName() + ".DONOT";

    private static final Logger LOG = Logging.getLoggerInstance(DeleteTranslationsProcessor.class);
    
    
    public void commit(final Node node, final Field field) {

        if (node.getCloud().getProperty(NOT) != null) {
            LOG.service("Not doing because of property");
            return;
        }
        String builder = node.getNodeManager().getProperty("translations.builder");
        if (node.getNumber() > 0 && builder != null && !"".equals(builder)) {

            // test for 'langrel'
            try {
                node.getCloud().getRelationManager("langrel");
            } catch (NotFoundException nfe) {
                LOG.warn("RelationManager 'langrel' not found: " + nfe);
                return;
            }

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

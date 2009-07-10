package ie.tcd.imm.hits.knime.util.leaf.ordering;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;

/**
 * <code>NodeDialog</code> for the "LeafOrdering" Node.
 * Reorders a tree to an optimal ordering. See <tt>New Hierarchical Clustering</tt> node.
 *
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more 
 * complex dialog please derive directly from 
 * {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author <a href="bakosg@tcd.ie">Gabor Bakos</a>
 */
public class LeafOrderingNodeDialog extends DefaultNodeSettingsPane {

    /**
     * New pane for configuring the LeafOrdering node.
     */
    protected LeafOrderingNodeDialog() {

    }
}


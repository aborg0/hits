package ie.tcd.imm.hits.knime.view.dendrogram;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;

/**
 * <code>NodeDialog</code> for the "Dendrogram" Node.
 * Allows to create dendrogram with a heatmap of parameters.
 *
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more 
 * complex dialog please derive directly from 
 * {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author TCD
 */
public class DendrogramNodeDialog extends DefaultNodeSettingsPane {

    /**
     * New pane for configuring the Dendrogram node.
     */
    protected DendrogramNodeDialog() {

    }
}


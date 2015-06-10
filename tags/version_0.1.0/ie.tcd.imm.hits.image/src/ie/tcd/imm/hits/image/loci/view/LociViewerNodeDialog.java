package ie.tcd.imm.hits.image.loci.view;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;

/**
 * <code>NodeDialog</code> for the "OMEViewer" Node.
 * Shows images based on OME.
 *
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more 
 * complex dialog please derive directly from 
 * {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
public class LociViewerNodeDialog extends DefaultNodeSettingsPane {

    /**
     * New pane for configuring the OMEViewer node.
     */
    protected LociViewerNodeDialog() {

    }
}


package ie.tcd.imm.hits.image.omero.read;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;

/**
 * <code>NodeDialog</code> for the "OMEROReader" Node.
 * Allows to import data from OMERO servers.
 *
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more 
 * complex dialog please derive directly from 
 * {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
public class OMEROReaderNodeDialog extends DefaultNodeSettingsPane {

    /**
     * New pane for configuring the OMEROReader node.
     */
    protected OMEROReaderNodeDialog() {

    }
}


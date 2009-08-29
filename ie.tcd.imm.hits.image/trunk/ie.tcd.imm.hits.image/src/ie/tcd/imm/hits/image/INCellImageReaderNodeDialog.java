package ie.tcd.imm.hits.image;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentFileSelectionWithPreview;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * <code>NodeDialog</code> for the "INCellImageReader" Node. This node
 * reads/handles INCell images.
 * 
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more
 * complex dialog please derive directly from
 * {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
public class INCellImageReaderNodeDialog extends DefaultNodeSettingsPane {

	/**
	 * New pane for configuring the INCellImageReader node.
	 */
	protected INCellImageReaderNodeDialog() {
		addDialogComponent(new DialogComponentFileSelectionWithPreview(
				new SettingsModelString(
						INCellImageReaderNodeModel.CFGKEY_FOLDER,
						INCellImageReaderNodeModel.DEFAULT_FOLDER),
				INCellImageReaderNodeModel.CFGKEY_FOLDER, ".xdce"));
	}
}

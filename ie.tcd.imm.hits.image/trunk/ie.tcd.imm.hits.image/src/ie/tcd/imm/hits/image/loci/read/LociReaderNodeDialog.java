package ie.tcd.imm.hits.image.loci.read;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentFileSelectionWithPreview;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * <code>NodeDialog</code> for the "OMEReader" Node. This node reads image
 * information in OME format.
 * 
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more
 * complex dialog please derive directly from
 * {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
public class LociReaderNodeDialog extends DefaultNodeSettingsPane {

	private DialogComponentFileSelectionWithPreview fileChooser;

	/**
	 * New pane for configuring the OMEReader node.
	 */
	protected LociReaderNodeDialog() {
		fileChooser = new DialogComponentFileSelectionWithPreview(
				new SettingsModelString(LociReaderNodeModel.CFGKEY_FOLDER,
						LociReaderNodeModel.DEFAULT_FOLDER),
				LociReaderNodeModel.CFGKEY_FOLDER, ".xdce");
		addDialogComponent(fileChooser);
	}

	@Override
	public void onCancel() {
		super.onCancel();
		fileChooser.stopPreview();
	}

	@Override
	public void saveAdditionalSettingsTo(final NodeSettingsWO settings)
			throws InvalidSettingsException {
		super.saveAdditionalSettingsTo(settings);
		fileChooser.stopPreview();
	}
}

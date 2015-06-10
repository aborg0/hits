package ie.tcd.imm.hits.image.loci.read;

import java.util.ArrayList;

import javax.swing.JFileChooser;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileFilter;

import loci.formats.ChannelSeparator;
import loci.formats.gui.ComboFileFilter;
import loci.formats.gui.ExtensionFileFilter;
import loci.formats.gui.GUITools;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentFileSelectionWithPreview;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.defaultnodesettings.UpdatableComponent;

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
		final ArrayList<String> validExtensions = computeExtensions();
		fileChooser = new DialogComponentFileSelectionWithPreview(
				new SettingsModelString(LociReaderNodeModel.CFGKEY_FOLDER,
						LociReaderNodeModel.DEFAULT_FOLDER),
				LociReaderNodeModel.CFGKEY_FOLDER, JFileChooser.OPEN_DIALOG,
				true, validExtensions
						.toArray(new String[validExtensions.size()]));
		fileChooser.setExtension("xdce");
		addDialogComponent(fileChooser);
		final SettingsModelString extensionModel = new SettingsModelString(
				LociReaderNodeModel.CFGKEY_EXTENSION,
				LociReaderNodeModel.DEFAULT_EXTENSION);
		addDialogComponent(new DialogComponentStringSelection(extensionModel,
				"extensions: ", computeExtensions()));
		extensionModel.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(final ChangeEvent e) {
				fileChooser.setExtension(extensionModel.getStringValue());
			}
		});
		addDialogComponent(new UpdatableComponent() {
			@Override
			protected void updateComponent() {
				fileChooser.setExtension(extensionModel.getStringValue());
			}
		});
	}

	/**
	 * @return The available extensions. Each contained element is a group,
	 *         where the elements are separated by {@code |}.
	 */
	static ArrayList<String> computeExtensions() {
		final FileFilter[] fileFilters = GUITools
				.buildFileFilters(new ChannelSeparator());
		final ArrayList<String> validExtensions = new ArrayList<String>();
		for (final FileFilter fileFilter : fileFilters) {
			if (fileFilter instanceof ComboFileFilter) {
				final ComboFileFilter cff = (ComboFileFilter) fileFilter;
				for (final FileFilter ff : cff.getFilters()) {
					addExtension(ff, validExtensions);
				}
			}
			addExtension(fileFilter, validExtensions);
		}
		if (validExtensions.size() > 0) {
			validExtensions.add(0, "");
		}
		return validExtensions;
	}

	/**
	 * @param fileFilter
	 *            The {@link FileFilter} to find extensions.
	 * @param validExtensions
	 *            The result valid extensions.
	 */
	private static void addExtension(final FileFilter fileFilter,
			final ArrayList<String> validExtensions) {
		if (fileFilter instanceof ExtensionFileFilter) {
			final ExtensionFileFilter ff = (ExtensionFileFilter) fileFilter;
			final StringBuilder sb = new StringBuilder();
			for (final String extension : ff.getExtensions()) {
				sb.append('.').append(extension).append("|");
			}
			if (sb.length() > 0) {
				sb.setLength(sb.length() - 1);
				validExtensions.add(sb.toString());
			}
		}
	}

	/** {@inheritDoc} */
	@Override
	public void onCancel() {
		super.onCancel();
		fileChooser.stopPreview();
	}

	/** {@inheritDoc} */
	@Override
	public void saveAdditionalSettingsTo(final NodeSettingsWO settings)
			throws InvalidSettingsException {
		super.saveAdditionalSettingsTo(settings);
		fileChooser.stopPreview();
	}
}

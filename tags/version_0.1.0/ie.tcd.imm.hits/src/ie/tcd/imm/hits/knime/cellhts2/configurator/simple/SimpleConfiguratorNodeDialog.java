package ie.tcd.imm.hits.knime.cellhts2.configurator.simple;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentFileChooser;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

import edu.umd.cs.findbugs.annotations.DefaultAnnotation;

/**
 * <code>NodeDialog</code> for the "SimpleConfigurator" Node. This node reads
 * the specified CellHTS 2 configuration files for using them as input for
 * CellHTS nodes.
 * 
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more
 * complex dialog please derive directly from
 * {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
@NotThreadSafe
@DefaultAnnotation(Nonnull.class)
public class SimpleConfiguratorNodeDialog extends DefaultNodeSettingsPane {

	/**
	 * New pane for configuring SimpleConfigurator node dialog. This is just a
	 * suggestion to demonstrate possible default dialog components.
	 */
	protected SimpleConfiguratorNodeDialog() {
		super();
		final DialogComponentFileChooser plateConf = new DialogComponentFileChooser(
				new SettingsModelString(
						SimpleConfiguratorNodeModel.CFGKEY_PLATE_CONFIG,
						SimpleConfiguratorNodeModel.DEFAULT_PLATE_CONFIG),
				SimpleConfiguratorNodeModel.CFGKEY_PLATE_CONFIG,
				"Plateconf.txt", ".txt");
		plateConf.setBorderTitle("Plate configuration file:");
		plateConf.setToolTipText("The plate configuration file.");
		addDialogComponent(plateConf);
		final DialogComponentFileChooser description = new DialogComponentFileChooser(
				new SettingsModelString(
						SimpleConfiguratorNodeModel.CFGKEY_DESCRIPTION_FILE,
						SimpleConfiguratorNodeModel.DEFAULT_DESCRIPTION),
				SimpleConfiguratorNodeModel.CFGKEY_DESCRIPTION_FILE,
				"Description.txt", ".txt");
		description.setBorderTitle("Description file:");
		description.setToolTipText("The description file of the experiment.");
		addDialogComponent(description);

	}
}

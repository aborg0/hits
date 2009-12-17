package ie.tcd.imm.hits.image.omero.read;

import ie.tcd.imm.hits.image.omero.read.OMEROReaderNodeModel.ConnectionSpeed;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentNumberEdit;
import org.knime.core.node.defaultnodesettings.DialogComponentPasswordField;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelEnum;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * <code>NodeDialog</code> for the "OMEROReader" Node. Allows to import data
 * from OMERO servers.
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
		super();
		createNewGroup("server");
		setHorizontalPlacement(true);
		addDialogComponent(new DialogComponentString(new SettingsModelString(
				OMEROReaderNodeModel.CFGKEY_SERVER_NAME,
				OMEROReaderNodeModel.DEFAULT_SERVER_NAME), "Server: "));
		addDialogComponent(new DialogComponentNumberEdit(
				new SettingsModelIntegerBounded(
						OMEROReaderNodeModel.CFGKEY_SERVER_PORT,
						OMEROReaderNodeModel.DEFAULT_SERVER_PORT, 0, 65535),
				"Port: "));
		final SettingsModelEnum<ConnectionSpeed> connectionSpeedModel = new SettingsModelEnum<ConnectionSpeed>(
				OMEROReaderNodeModel.CFGKEY_CONNECTION_SPEED,
				OMEROReaderNodeModel.DEFAULT_CONNECTION_SPEED, ConnectionSpeed
						.values());
		addDialogComponent(new DialogComponentStringSelection(
				connectionSpeedModel, "Connection speed: ",
				connectionSpeedModel.getDisplayTexts()));
		createNewGroup("user");
		setHorizontalPlacement(false);
		addDialogComponent(new DialogComponentString(new SettingsModelString(
				OMEROReaderNodeModel.CFGKEY_USERNAME,
				OMEROReaderNodeModel.DEFAULT_USERNAME), "User name: "));
		addDialogComponent(new DialogComponentPasswordField(
				new SettingsModelString(OMEROReaderNodeModel.CFGKEY_PASSWORD,
						OMEROReaderNodeModel.DEFAULT_PASSWORD), "Password: "));
	}
}

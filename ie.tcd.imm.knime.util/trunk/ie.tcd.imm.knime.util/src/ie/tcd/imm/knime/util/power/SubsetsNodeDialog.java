package ie.tcd.imm.knime.util.power;

import org.knime.core.data.DataValue;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelColumnName;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * <code>NodeDialog</code> for the "Subsets" Node. Generates all possible
 * subsets of the input.
 * 
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more
 * complex dialog please derive directly from
 * {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
public class SubsetsNodeDialog extends DefaultNodeSettingsPane {

	/**
	 * New pane for configuring the Subsets node.
	 */
	protected SubsetsNodeDialog() {
		@SuppressWarnings("unchecked")
		final DialogComponentColumnNameSelection origColumnName = new DialogComponentColumnNameSelection(
				new SettingsModelColumnName(
						SubsetsNodeModel.CFGKEY_ORIGINAL_COLUMN_NAME, ""),
				"Generating from this column: ", 0, DataValue.class);
		addDialogComponent(origColumnName);
		addDialogComponent(new DialogComponentString(new SettingsModelString(
				SubsetsNodeModel.CFGKEY_NEW_COLUMN_NAME,
				SubsetsNodeModel.DEFAULT_NEW_COLUMN_NAME), "New column name: "));
		addDialogComponent(new DialogComponentBoolean(new SettingsModelBoolean(
				SubsetsNodeModel.CFGKEY_CREATE_MULTISET,
				SubsetsNodeModel.DEFAULT_CREATE_MULTISET), "Create multiset? "));
	}
}

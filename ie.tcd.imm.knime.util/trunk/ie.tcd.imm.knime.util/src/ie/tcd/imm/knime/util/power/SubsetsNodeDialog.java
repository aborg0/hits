package ie.tcd.imm.knime.util.power;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnFilter;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelFilterString;

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
		final DialogComponentColumnFilter origColumnName = new DialogComponentColumnFilter(
				new SettingsModelFilterString(
						SubsetsNodeModel.CFGKEY_COLUMN_NAMES), 0);
		addDialogComponent(origColumnName);
		addDialogComponent(new DialogComponentBoolean(new SettingsModelBoolean(
				SubsetsNodeModel.CFGKEY_CREATE_MULTISET,
				SubsetsNodeModel.DEFAULT_CREATE_MULTISET), "Create multiset? "));
	}
}

package ie.tcd.imm.hits.knime.stat.p;

import org.knime.core.data.DoubleValue;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnFilter;
import org.knime.core.node.defaultnodesettings.DialogComponentNumberEdit;
import org.knime.core.node.defaultnodesettings.SettingsModelFilterString;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;

/**
 * <code>NodeDialog</code> for the "ScoreToPValue" Node. Converts scores to p
 * values (also computes the predicted frequency of that value if a sample size
 * selected).
 * 
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more
 * complex dialog please derive directly from
 * {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author <a href="bakosg@tcd.ie">Gabor Bakos</a>
 */
public class ScoreToPValueNodeDialog extends DefaultNodeSettingsPane {

	/**
	 * New pane for configuring the ScoreToPValue node.
	 */
	protected ScoreToPValueNodeDialog() {
		@SuppressWarnings("unchecked")
		final DialogComponentColumnFilter columns = new DialogComponentColumnFilter(
				new SettingsModelFilterString(
						ScoreToPValueNodeModel.CFGKEY_COLUMNS), 0,
				DoubleValue.class);
		columns.setIncludeTitle("Columns with Z scores");
		addDialogComponent(columns);
		final DialogComponentNumberEdit sampleCount = new DialogComponentNumberEdit(
				new SettingsModelIntegerBounded(
						ScoreToPValueNodeModel.CFGKEY_SAMPLE_COUNT,
						ScoreToPValueNodeModel.DEFAULT_SAMPLE_COUNT, 0,
						Integer.MAX_VALUE), "Number of samples: ", 9);
		sampleCount.setToolTipText("0 means no frequency will be computed.");
		addDialogComponent(sampleCount);
	}
}

package ie.tcd.imm.hits.knime.ranking;

import ie.tcd.imm.hits.knime.ranking.RankNodeModel.TieHandling;
import ie.tcd.imm.hits.knime.view.heatmap.HeatmapNodeModel.StatTypes;

import java.util.ArrayList;

import org.knime.core.data.StringValue;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.DialogComponentStringListSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.defaultnodesettings.SettingsModelStringArray;

/**
 * <code>NodeDialog</code> for the "Rank" Node. This node ranks the results
 * for each parameter and for each normalisation methods with selectable neutral
 * values and the direction of the upregulation.<br>
 * The downregulated values has negative rankings, the upregulated has positive
 * ones. If present it uses the (final) well annotation information of the
 * wells, and only the sample wells are ranked. If the well annotations are not
 * present the ranking is based on all wells with non-missing values.
 * 
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more
 * complex dialog please derive directly from
 * {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
public class RankNodeDialog extends DefaultNodeSettingsPane {

	/**
	 * New pane for configuring Rank node dialog.
	 */
	protected RankNodeDialog() {
		super();
		// TODO add tooltips
		@SuppressWarnings("unchecked")
		final DialogComponentColumnNameSelection wellAnnotationName = new DialogComponentColumnNameSelection(
				new SettingsModelString(RankNodeModel.CFGKEY_WELL_ANNOTATION,
						RankNodeModel.DEFAULT_WELL_ANNOTATION),
				"Well annotation", 0, StringValue.class);
		addDialogComponent(wellAnnotationName);
		final DialogComponentString rankPrefix = new DialogComponentString(
				new SettingsModelString(RankNodeModel.CFGKEY_RANK_PREFIX,
						RankNodeModel.CFGKEY_RANK_PREFIX), "rank prefix");
		addDialogComponent(rankPrefix);
		final DialogComponentStringSelection rankingGroups = new DialogComponentStringSelection(
				new SettingsModelString(RankNodeModel.CFGKEY_RANKING_GROUPS,
						RankNodeModel.DEFAULT_RANKING_GROUPS), "Ranking groups");
		addDialogComponent(rankingGroups);
		final ArrayList<String> possStats = new ArrayList<String>(6);
		for (final StatTypes statTypes : new StatTypes[] { StatTypes.score,
				StatTypes.normalized, StatTypes.median, StatTypes.raw,
				StatTypes.rawPerMedian }) {
			possStats.add(statTypes.name());
		}
		final DialogComponentStringListSelection statistics = new DialogComponentStringListSelection(
				new SettingsModelStringArray(RankNodeModel.CFGKEY_STATISTICS,
						RankNodeModel.DEFAULT_STATISTICS), "Statistics: ",
				possStats, true, 5);
		addDialogComponent(statistics);
		// FIXME Here should go an editor for the regulations, in the meantime
		// it is a simple String component.
		// TODO connect to the possStats and statistics components
		final DialogComponentString regulation = new DialogComponentString(
				new SettingsModelString(RankNodeModel.CFGKEY_REGULATION,
						RankNodeModel.DEFAULT_REGULATION), "Regulation: ");
		addDialogComponent(regulation);
		addDialogComponent(new DialogComponentStringSelection(
				new SettingsModelString(RankNodeModel.CFGKEY_TIE,
						RankNodeModel.DEFAULT_TIE), "Tie handling: ",
				TieHandling.increase.name(), TieHandling.continuous.name()));
	}
}

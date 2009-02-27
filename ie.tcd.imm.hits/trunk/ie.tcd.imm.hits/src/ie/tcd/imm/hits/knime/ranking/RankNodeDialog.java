package ie.tcd.imm.hits.knime.ranking;

import ie.tcd.imm.hits.knime.ranking.RankNodeModel.RankingGroups;
import ie.tcd.imm.hits.knime.ranking.RankNodeModel.TieHandling;
import ie.tcd.imm.hits.knime.util.ModelBuilder.SpecAnalyser;
import ie.tcd.imm.hits.knime.view.heatmap.HeatmapNodeModel.StatTypes;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.StringValue;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.DialogComponentStringListSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.defaultnodesettings.SettingsModelStringArray;
import org.knime.core.node.defaultnodesettings.UpdatableComponent;

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

	private static final String[] EMPTY_ARRAY = new String[] { "" };

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
						RankNodeModel.CFGKEY_RANK_PREFIX), "Rank prefix");
		addDialogComponent(rankPrefix);
		final DialogComponentStringSelection rankingGroups = new DialogComponentStringSelection(
				new SettingsModelString(RankNodeModel.CFGKEY_RANKING_GROUPS,
						RankNodeModel.DEFAULT_RANKING_GROUPS),
				"Ranking groups", RankingGroups.experiment.name(),
				RankingGroups.plate.name(), RankingGroups.replicate.name(),
				RankingGroups.plateAndReplicate.name());
		addDialogComponent(rankingGroups);
		final ArrayList<String> possStats = new ArrayList<String>(6);
		for (final StatTypes statTypes : new StatTypes[] { StatTypes.score,
				StatTypes.normalized, StatTypes.median, StatTypes.meanOrDiff,
				StatTypes.raw, StatTypes.rawPerMedian }) {
			possStats.add(statTypes.name());
		}
		final DialogComponentStringListSelection statistics = new DialogComponentStringListSelection(
				new SettingsModelStringArray(RankNodeModel.CFGKEY_STATISTICS,
						RankNodeModel.DEFAULT_STATISTICS), "Statistics: ",
				possStats, true, 5);
		addDialogComponent(statistics);
		final DialogComponentStringListSelection parameters = new DialogComponentStringListSelection(
				new SettingsModelStringArray(RankNodeModel.CFGKEY_PARAMETERS,
						RankNodeModel.DEFAULT_PARAMETERS), "Parameters: ", "");
		addDialogComponent(parameters);
		// FIXME Here should go an editor for the regulations, in the meantime
		// it is a simple String component.
		// TODO connect to the possStats and statistics components
		final DialogComponentString regulation = new DialogComponentString(
				new SettingsModelString(RankNodeModel.CFGKEY_REGULATION,
						RankNodeModel.DEFAULT_REGULATION), "Regulation: ");
		addDialogComponent(regulation);
		final DialogComponentStringSelection tieHandling = new DialogComponentStringSelection(
				new SettingsModelString(RankNodeModel.CFGKEY_TIE,
						RankNodeModel.DEFAULT_TIE), "Tie handling: ",
				TieHandling.increase.name(), TieHandling.continuous.name());
		addDialogComponent(tieHandling);
		addDialogComponent(new UpdatableComponent() {
			@Override
			protected void updateComponent() {
				super.updateComponent();
				final SpecAnalyser specAnalyser = new SpecAnalyser(
						(DataTableSpec) getLastTableSpec(0));
				final List<String> params = specAnalyser.getParameters();
				parameters.replaceListItems(params, computeSelections(params,
						((SettingsModelStringArray) parameters.getModel())
								.getStringArrayValue()));
				final ArrayList<String> possStats = new ArrayList<String>(6);
				final EnumSet<StatTypes> stats = specAnalyser.getStatistics();
				for (final StatTypes statTypes : new StatTypes[] {
						StatTypes.score, StatTypes.normalized,
						StatTypes.median, StatTypes.meanOrDiff, StatTypes.raw,
						StatTypes.rawPerMedian }) {
					if (stats.contains(statTypes)) {
						possStats.add(statTypes.name());
					}
				}
				statistics.replaceListItems(possStats, computeSelections(
						possStats, ((SettingsModelStringArray) statistics
								.getModel()).getStringArrayValue()));
			}

			private String[] computeSelections(final List<String> params,
					final String[] selections) {
				final List<String> intersect = new ArrayList<String>(
						selections.length);
				for (final String selection : selections) {
					if (params.contains(selection)) {
						intersect.add(selection);
					}
				}
				return intersect.isEmpty() ? EMPTY_ARRAY : intersect
						.toArray(new String[intersect.size()]);
			}
		});
	}
}
package ie.tcd.imm.hits.knime.view.dendrogram;

import org.knime.base.node.mine.cluster.hierarchical.HierarchicalClusterNodeModel;
import org.knime.base.node.mine.cluster.hierarchical.distfunctions.DistanceFunction;
import org.knime.base.node.mine.cluster.hierarchical.distfunctions.EuclideanDist;
import org.knime.base.node.mine.cluster.hierarchical.distfunctions.ManhattanDist;
import org.knime.core.data.DataValue;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.IntValue;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnFilter;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelFilterString;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * <code>NodeDialog</code> for the "Dendrogram" Node. Allows to create
 * dendrogram with a heatmap of parameters.
 * 
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more
 * complex dialog please derive directly from
 * {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author TCD
 */
public class DendrogramNodeDialog extends DefaultNodeSettingsPane {

	/**
	 * An array with the available distance functions names.
	 */
	public static final DistanceFunction[] DISTANCE_FUNCTIONS = new DistanceFunction[] {
			EuclideanDist.EUCLIDEAN_DISTANCE, ManhattanDist.MANHATTEN_DISTANCE };

	private static String[] linkageTypes;

	private static String[] distanceFunctionNames;

	/**
	 * Puts the names of the linkage enum fields into a string array and the
	 * names of the distance functions in another string array.
	 */
	static {
		DendrogramNodeDialog.linkageTypes = new String[HierarchicalClusterNodeModel.Linkage
				.values().length];
		int i = 0;
		for (final HierarchicalClusterNodeModel.Linkage l : HierarchicalClusterNodeModel.Linkage
				.values()) {
			DendrogramNodeDialog.linkageTypes[i++] = l.name();
		}
		i = 0;
		DendrogramNodeDialog.distanceFunctionNames = new String[DistanceFunction.Names
				.values().length];
		for (final DistanceFunction.Names n : DistanceFunction.Names.values()) {
			DendrogramNodeDialog.distanceFunctionNames[i++] = n.name();
		}
	}

	/**
	 * Creates a new <code>NodeDialogPane</code> for hierarchical clustering in
	 * order to set the parameters.
	 */
	protected DendrogramNodeDialog() {

		addDialogComponent(new DialogComponentNumber(DendrogramNodeDialog
				.createSettingsNumberOfClusters(), "Number output cluster:", 1));

		addDialogComponent(new DialogComponentStringSelection(
				DendrogramNodeDialog.createSettingsDistanceFunction(),
				"Distance function:",
				DendrogramNodeDialog.distanceFunctionNames));

		addDialogComponent(new DialogComponentStringSelection(
				DendrogramNodeDialog.createSettingsLinkageType(),
				"Linkage type:", DendrogramNodeDialog.linkageTypes));

		addDialogComponent(new DialogComponentBoolean(DendrogramNodeDialog
				.createSettingsCacheKeys(), "Cache distances"));

		final Class<? extends DataValue>[] allowedTypes = new Class[] {
				DoubleValue.class, IntValue.class };
		addDialogComponent(new DialogComponentColumnFilter(DendrogramNodeDialog
				.createSettingsColumns(), 0, allowedTypes));
		// addDialogComponent(new DialogComponentBoolean(new
		// SettingsModelBoolean(
		// DendrogramNodeModel.CFGKEY_REARRANGE_PARAMETERS,
		// DendrogramNodeModel.DEFAULT_REARRANGE_PARAMETERS),
		// "Rearrange parameters?"));

	}

	static SettingsModelIntegerBounded createSettingsNumberOfClusters() {
		return new SettingsModelIntegerBounded(
				HierarchicalClusterNodeModel.NRCLUSTERS_KEY, 3, 1,
				Integer.MAX_VALUE);
	}

	static SettingsModelString createSettingsDistanceFunction() {
		return new SettingsModelString(
				HierarchicalClusterNodeModel.DISTFUNCTION_KEY,
				DendrogramNodeDialog.distanceFunctionNames[0]);
	}

	static SettingsModelString createSettingsLinkageType() {
		return new SettingsModelString(
				HierarchicalClusterNodeModel.LINKAGETYPE_KEY,
				HierarchicalClusterNodeModel.Linkage.SINGLE.name());
	}

	static SettingsModelBoolean createSettingsCacheKeys() {
		return new SettingsModelBoolean(
				HierarchicalClusterNodeModel.USE_CACHE_KEY, true);
	}

	static SettingsModelFilterString createSettingsColumns() {
		return new SettingsModelFilterString(
				HierarchicalClusterNodeModel.SELECTED_COLUMNS_KEY);
	}

	/**
	 * New pane for configuring the Dendrogram node.
	 */
	// protected DendrogramNodeDialog() {
	// super();
	// }
}

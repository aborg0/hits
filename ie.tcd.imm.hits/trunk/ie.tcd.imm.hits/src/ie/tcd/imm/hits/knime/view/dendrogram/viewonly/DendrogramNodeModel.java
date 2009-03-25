/*
 * 
 */
package ie.tcd.imm.hits.knime.view.dendrogram.viewonly;

import ie.tcd.imm.hits.knime.util.ModelBuilder;
import ie.tcd.imm.hits.knime.view.heatmap.HeatmapNodeModel.StatTypes;
import ie.tcd.imm.hits.util.swing.colour.ColourSelector.RangeType;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.knime.base.node.mine.cluster.hierarchical.ClusterTreeModel;
import org.knime.base.node.mine.cluster.hierarchical.view.ClusterViewNode;
import org.knime.base.node.util.DataArray;
import org.knime.base.node.util.DefaultDataArray;
import org.knime.base.node.viz.plotter.DataProvider;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.container.ContainerTable;
import org.knime.core.data.container.DataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;

/**
 * This is the model implementation of Dendrogram. Allows to create dendrogram
 * with a heatmap of parameters.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
@SuppressWarnings("restriction")
public class DendrogramNodeModel extends NodeModel implements DataProvider {
	/** The logger. */
	static final NodeLogger logger = NodeLogger
			.getLogger(DendrogramNodeModel.class);

	// /** Configuration key for the parameter rearrangement. */
	// protected static final String CFGKEY_REARRANGE_PARAMETERS =
	// "ie.tcd.imm.hits.knime.view.dendrogram.rearrangeParameters";
	// /** Default value of the parameter rearrangement. */
	// protected static final boolean DEFAULT_REARRANGE_PARAMETERS = true;

	/** Reset should clear it. */
	private final Map<String, Integer> mapOfKeys = new HashMap<String, Integer>();

	private final Map<String, Map<StatTypes, Map<RangeType, Double>>> ranges = new HashMap<String, Map<StatTypes, Map<RangeType, Double>>>();

	/** The selected columns. */
	protected List<String> selectedColumns = new ArrayList<String>();

	// private final SettingsModelBoolean rearrangeParameters = new
	// SettingsModelBoolean(
	// DendrogramNodeModel.CFGKEY_REARRANGE_PARAMETERS,
	// DendrogramNodeModel.DEFAULT_REARRANGE_PARAMETERS);

	private DataArray origData;

	private ClusterViewNode root;

	/**
	 * Constructor for the node model.
	 */
	protected DendrogramNodeModel() {
		super(new PortType[] { ClusterTreeModel.TYPE, BufferedDataTable.TYPE },
				new PortType[0]);
	}

	@Override
	protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs)
			throws InvalidSettingsException {
		return new PortObjectSpec[0];
	}

	@Override
	protected BufferedDataTable[] execute(final PortObject[] data,
			final ExecutionContext exec) throws Exception {
		selectedColumns.clear();
		final DataTableSpec spec = (DataTableSpec) ((ClusterTreeModel) data[0])
				.getSpec();
		for (final DataColumnSpec colSpec : spec) {
			if (colSpec.getType().isASuperTypeOf(colSpec.getType())) {
				selectedColumns.add(colSpec.getName());
			}
		}
		root = ((ClusterTreeModel) data[0]).getRoot();
		// if (rearrangeParameters.getBooleanValue()) {
		// final HalfDoubleMatrix cache = new HalfDoubleMatrix(cols.size(),
		// false);
		// final Names nameOfDistanceFunction = DistanceFunction.Names
		// .valueOf(new SettingsModelString(
		// HierarchicalClusterNodeModel.DISTFUNCTION_KEY,
		// DistanceFunction.Names.values()[0].name())
		// .getStringValue());
		// final DistanceFunction distFunc;
		// switch (nameOfDistanceFunction) {
		// case Manhattan:
		// distFunc = ManhattanDist.MANHATTEN_DISTANCE;
		// break;
		// case Euclidean:
		// distFunc = EuclideanDist.EUCLIDEAN_DISTANCE;
		// break;
		// default:
		// throw new UnsupportedOperationException(
		// "Not supported distance function.");
		// }
		// }
		origData = new DefaultDataArray((BufferedDataTable) data[1], 1,
				((BufferedDataTable) data[1]).getRowCount());
		mapOfKeys.clear();
		int i = 0;
		for (final DataRow row : origData/* data[0] */) {
			mapOfKeys.put(row.getKey().getString(), Integer.valueOf(i++));
		}
		// logger.debug(new Date(System.currentTimeMillis()).toGMTString());
		fillStats((BufferedDataTable) data[1]);
		// logger.debug(new Date(System.currentTimeMillis()).toGMTString());
		return new BufferedDataTable[0];
	}

	/**
	 * @param table
	 *            A table with the original values.
	 */
	private void fillStats(final DataTable table) {
		final Map<String, Map<StatTypes, List<Double>>> vals = new HashMap<String, Map<StatTypes, List<Double>>>();
		// final Map<String, Map<StatTypes, Map<RangeType, Double>>> ret = new
		// HashMap<String, Map<StatTypes, Map<RangeType, Double>>>();
		ranges.clear();
		final int[] colIndices = new int[selectedColumns.size()];
		final String[] colNames = new String[selectedColumns.size()];
		{
			int i = 0;
			for (final String colName : selectedColumns) {
				colNames[i] = colName;
				colIndices[i++] = table.getDataTableSpec().findColumnIndex(
						colName);
				final EnumMap<StatTypes, List<Double>> valMap = new EnumMap<StatTypes, List<Double>>(
						StatTypes.class);
				valMap.put(StatTypes.raw, new ArrayList<Double>());
				vals.put(colName, valMap);
				final EnumMap<StatTypes, Map<RangeType, Double>> retMaps = new EnumMap<StatTypes, Map<RangeType, Double>>(
						StatTypes.class);
				ranges.put(colName, retMaps);
				retMaps.put(StatTypes.raw, new EnumMap<RangeType, Double>(
						RangeType.class));
			}
		}
		for (final DataRow row : table) {
			for (int i = 0; i < colIndices.length; ++i) {
				final DataCell cell = row.getCell(colIndices[i]);
				if (cell instanceof DoubleValue) {
					final DoubleValue val = (DoubleValue) cell;
					vals.get(colNames[i]).get(StatTypes.raw).add(
							Double.valueOf(val.getDoubleValue()));
				}
			}
		}
		ModelBuilder.computeStatistics(ranges, vals);
	}

	/**
	 * @return The selected columns.
	 */
	public List<String> getSelectedColumns() {
		return selectedColumns == null ? Collections.<String> emptyList()
				: Collections.unmodifiableList(selectedColumns);
	}

	@Override
	protected void validateSettings(final NodeSettingsRO settings)
			throws InvalidSettingsException {// No settings.
	}

	@Override
	protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
			throws InvalidSettingsException {
		// No settings
	}

	@Override
	protected void saveSettingsTo(final NodeSettingsWO settings) {// No settings
	}

	/**
	 * @return The map of the keys to positions ({@code 0}-based); unmodifiable.
	 */
	public Map<String, Integer> getMap() {
		return Collections.unmodifiableMap(mapOfKeys);
	}

	@Override
	protected void loadInternals(final File nodeInternDir,
			final ExecutionMonitor exec) throws IOException,
			CanceledExecutionException {
		final ContainerTable data = DataContainer.readFromZip(new File(
				nodeInternDir, DendrogramNodeModel.CFG_H_CLUST_DATA));
		origData = new DefaultDataArray(data, 1, data.getRowCount());
		int i = 0;
		for (final DataRow dataRow : origData) {
			mapOfKeys.put(dataRow.getKey().getString(), Integer.valueOf(i++));
		}
		fillStats(origData);
	}

	private static final String CFG_H_CLUST_DATA = "hClustData";

	@Override
	protected void saveInternals(final File nodeInternDir,
			final ExecutionMonitor exec) throws IOException,
			CanceledExecutionException {
		final File dataFile = new File(nodeInternDir,
				DendrogramNodeModel.CFG_H_CLUST_DATA);
		DataContainer.writeToZip(origData, dataFile, exec);
	}

	/**
	 * @return The actual ranges.
	 */
	Map<String, Map<StatTypes, Map<RangeType, Double>>> getRange() {
		return ranges;
	}

	@Override
	protected void reset() {
		mapOfKeys.clear();
		ranges.clear();
		root = null;
		origData = null;
		selectedColumns.clear();
	}

	/**
	 * @return The original data table.
	 */
	public DataArray getOrigData() {
		return origData;
	}

	public ClusterViewNode getRoot() {
		return root;
	}

	@Override
	public DataArray getDataArray(final int index) {
		return origData;
	}
}

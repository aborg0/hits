/*
 * 
 */
package ie.tcd.imm.hits.knime.view.dendrogram;

import ie.tcd.imm.hits.knime.util.ModelBuilder;
import ie.tcd.imm.hits.knime.view.heatmap.HeatmapNodeModel.StatTypes;
import ie.tcd.imm.hits.util.swing.colour.ColourSelector.RangeType;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.knime.base.node.mine.cluster.hierarchical.HierarchicalClusterNodeModel;
import org.knime.base.node.util.DataArray;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.container.DataContainer;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelFilterString;

/**
 * This is the model implementation of Dendrogram. Allows to create dendrogram
 * with a heatmap of parameters.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
public class DendrogramNodeModel extends HierarchicalClusterNodeModel {
	/** The logger. */
	static final NodeLogger logger = NodeLogger
			.getLogger(DendrogramNodeModel.class);

	/** Reset should clear it. */
	private final Map<String, Integer> mapOfKeys = new HashMap<String, Integer>();

	private final Map<String, Map<StatTypes, Map<RangeType, Double>>> ranges = new HashMap<String, Map<StatTypes, Map<RangeType, Double>>>();

	/**
	 * The {@link SettingsModelFilterString} for selected columns.
	 */
	protected SettingsModelFilterString selectedColumns;

	private BufferedDataTable origData;
	{
		try {
			final Class<?> cls = Class
					.forName("org.knime.base.node.mine.cluster.hierarchical.HierarchicalClusterNodeDialog");
			final Method method = cls
					.getDeclaredMethod("createSettingsColumns");
			method.setAccessible(true);
			selectedColumns = (SettingsModelFilterString) method.invoke(null);
		} catch (final Exception e) {
			throw new RuntimeException(e);
		}

	}

	/**
	 * Constructor for the node model.
	 */
	protected DendrogramNodeModel() {
		super();
	}

	@Override
	protected BufferedDataTable[] execute(final BufferedDataTable[] data,
			final ExecutionContext exec) throws Exception {
		final BufferedDataTable[] superResult = super.execute(data, exec);
		origData = data[0];
		mapOfKeys.clear();
		if (selectedColumns.getExcludeList().size() == 0
				&& selectedColumns.getIncludeList().size() == 0) {
			final List<String> incl = new LinkedList<String>();
			for (final DataColumnSpec colSpec : data[0].getDataTableSpec()) {
				if (colSpec.getType().isASuperTypeOf(DoubleCell.TYPE)) {
					incl.add(colSpec.getName());
				}
			}
			selectedColumns.setIncludeList(incl);
		}
		int i = 0;
		for (final DataRow row : superResult[0]) {
			mapOfKeys.put(row.getKey().getString(), Integer.valueOf(i++));
		}
		// logger.debug(new Date(System.currentTimeMillis()).toGMTString());
		fillStats(data[0]);
		// logger.debug(new Date(System.currentTimeMillis()).toGMTString());
		DendrogramNodeModel.logger.info("Selected columns: "
				+ selectedColumns.getIncludeList());
		return superResult;
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
		final int[] colIndices = new int[selectedColumns.getIncludeList()
				.size()];
		final String[] colNames = new String[selectedColumns.getIncludeList()
				.size()];
		{
			int i = 0;
			for (final String colName : selectedColumns.getIncludeList()) {
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
				: selectedColumns.getIncludeList();
	}

	@Override
	protected void validateSettings(final NodeSettingsRO settings)
			throws InvalidSettingsException {
		super.validateSettings(settings);
		selectedColumns.validateSettings(settings);
	}

	@Override
	protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
			throws InvalidSettingsException {
		super.loadValidatedSettingsFrom(settings);
		selectedColumns.loadSettingsFrom(settings);
	}

	@Override
	protected void saveSettingsTo(final NodeSettingsWO settings) {
		super.saveSettingsTo(settings);
		selectedColumns.saveSettingsTo(settings);
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
		super.loadInternals(nodeInternDir, exec);
		final DataArray dataArray = getDataArray(1);
		int i = 0;
		for (final DataRow dataRow : dataArray) {
			mapOfKeys.put(dataRow.getKey().getString(), Integer.valueOf(i++));
		}
		fillStats(dataArray);
	}

	private static final String CFG_H_CLUST_DATA = "hClustData";

	@Override
	protected void saveInternals(final File nodeInternDir,
			final ExecutionMonitor exec) throws IOException,
			CanceledExecutionException {
		super.saveInternals(nodeInternDir, exec);
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
}

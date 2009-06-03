/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package ie.tcd.imm.knime.util.merge;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import java.io.File;
import java.io.IOException;

import org.knime.base.node.util.DefaultDataArray;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowKey;
import org.knime.core.data.container.WrappedTable;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelFilterString;
import org.knime.core.util.Pair;

/**
 * This is the model implementation of Merge. Resorts the rows. It is mostly
 * like an "anti-sort".
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
public class MergeNodeModel extends NodeModel {

	// the logger instance
	private static final NodeLogger logger = NodeLogger
			.getLogger(MergeNodeModel.class);

	/** Configuration key for stop if there were missing row from a series */
	static final String CFGKEY_HALT_ON_ERROR = "haltOnError";

	/** Stop on errors. */
	static final boolean DEFAULT_HALT_ON_ERROR = true;

	/** Configuration key for columns to work with. */
	static final String CFGKEY_MERGE_COLUMNS = "mergeColumns";

	// static final String CFGKEY_MERGE_ORDER = "mergeOrder";

	/** No selection */
	static final String[] DEFAULT_MERGE_COLUMNS = new String[0];

	// static final boolean[] DEFAULT_SORT_ORDERS = new boolean[0];

	/** Configuration key for work in memory. */
	static final String CFGKEY_SORT_IN_MEMORY = "sortInMemory";

	/** Sort in memory */
	static final boolean DEFAULT_SORT_IN_MEMORY = true;

	/** Configuration key to reverse the order within a group. */
	static final String CFGKEY_SORT_ORDER_REVERSED = "sortOrderReversed";

	/** Do not reverse, leave original order. */
	static final boolean DEFAULT_SORT_ORDER_REVERSED = false;

	private final SettingsModelFilterString mergeColumns = new SettingsModelFilterString(
			CFGKEY_MERGE_COLUMNS);

	private final SettingsModelBoolean sortInMemory = new SettingsModelBoolean(
			CFGKEY_SORT_IN_MEMORY, DEFAULT_SORT_IN_MEMORY);

	private final SettingsModelBoolean haltOnError = new SettingsModelBoolean(
			MergeNodeModel.CFGKEY_HALT_ON_ERROR,
			MergeNodeModel.DEFAULT_HALT_ON_ERROR);

	private final SettingsModelBoolean sortOrderReversed = new SettingsModelBoolean(
			MergeNodeModel.CFGKEY_SORT_ORDER_REVERSED,
			MergeNodeModel.DEFAULT_SORT_ORDER_REVERSED);

	/**
	 * Constructor for the node model.
	 */
	protected MergeNodeModel() {
		super(1, 1);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
			final ExecutionContext exec) throws Exception {
		final List<String> columns = mergeColumns.getIncludeList();
		final BufferedDataTable inputTable = inData[0];
		final DataTableSpec dataTableSpec = inputTable.getDataTableSpec();
		final int[] colIndices = computeColIndices(columns, dataTableSpec);
		final Map<Map<Integer, DataCell>, List<Pair<RowKey, Integer>>> patternToKeys = computePatternToKeys(
				inputTable, colIndices);
		final BufferedDataContainer container = exec
				.createDataContainer(inputTable.getDataTableSpec());
		if (sortInMemory.getBooleanValue()) {
			final DefaultDataArray dataArray = new DefaultDataArray(
					new WrappedTable(inputTable), 1, inputTable.getRowCount(),
					exec);
			for (final List<Integer> rowIndices : generateBlocks(patternToKeys,
					inputTable.getRowCount())) {
				for (final Integer rowInteger : rowIndices) {
					container.addRowToTable(dataArray.getRow(rowInteger
							.intValue()));
				}
			}
		} else {// FIXME do something good here

		}
		container.close();
		final BufferedDataTable out = container.getTable();
		return new BufferedDataTable[] { out };
	}

	/**
	 * @param patternToKeys
	 * @param rowCount
	 * @return Ordered list of row indices (starting from {@code 0}).
	 */
	private Iterable<List<Integer>> generateBlocks(
			final Map<Map<Integer, DataCell>, List<Pair<RowKey, Integer>>> patternToKeys,
			final int rowCount) {
		return new Iterable<List<Integer>>() {

			@Override
			public Iterator<List<Integer>> iterator() {
				return new Iterator<List<Integer>>() {
					private static final String HEADER = "Problem with value | column\n";

					// private int lastRowIndex = -1;
					private boolean hasNext = rowCount > 0;

					@SuppressWarnings("synthetic-access")
					private final boolean reverse = sortOrderReversed
							.getBooleanValue();
					@SuppressWarnings("synthetic-access")
					private final boolean haltIfError = haltOnError
							.getBooleanValue();

					private final Map<Map<Integer, DataCell>, Iterator<Pair<RowKey, Integer>>> iterators = new HashMap<Map<Integer, DataCell>, Iterator<Pair<RowKey, Integer>>>();
					{
						for (final Entry<Map<Integer, DataCell>, List<Pair<RowKey, Integer>>> entry : patternToKeys
								.entrySet()) {
							iterators.put(entry.getKey(), entry.getValue()
									.iterator());
						}
					}

					private final StringBuilder sb = new StringBuilder(HEADER);

					@Override
					public List<Integer> next() {
						if (!hasNext()) {
							throw new IllegalStateException(
									"Already collected all of the groups.");
						}
						final Set<Integer> ret = new HashSet<Integer>();
						boolean allHasNext = true;
						hasNext = false;
						for (final Entry<Map<Integer, DataCell>, Iterator<Pair<RowKey, Integer>>> iterEntry : iterators
								.entrySet()) {
							final Iterator<Pair<RowKey, Integer>> iter = iterEntry
									.getValue();
							if (!iter.hasNext()) {
								allHasNext = false;
							} else {
								hasNext = true;
							}
						}
						if (allHasNext || !haltIfError) {

							for (final Entry<Map<Integer, DataCell>, Iterator<Pair<RowKey, Integer>>> iterEntry : iterators
									.entrySet()) {
								final Iterator<Pair<RowKey, Integer>> iter = iterEntry
										.getValue();
								if (iter.hasNext()) {
									final Pair<RowKey, Integer> next = iter
											.next();
									final boolean added = ret.add(next
											.getSecond());
									assert added || !added;
								} else {
									for (final Entry<Integer, DataCell> entry : iterEntry
											.getKey().entrySet()) {
										sb
												.append(entry.getValue())
												.append("|")
												.append(
														entry.getKey()
																.intValue() + 1)
												.append("\n");
									}
								}
							}
						} else {// !allHasNext && haltIfError
							boolean anyHasNext = false;
							Map<Integer, DataCell> sample = new HashMap<Integer, DataCell>();
							for (final Entry<Map<Integer, DataCell>, Iterator<Pair<RowKey, Integer>>> iterEntry : iterators
									.entrySet()) {
								final Iterator<Pair<RowKey, Integer>> iter = iterEntry
										.getValue();
								if (iter.hasNext()) {
									anyHasNext = true;
								} else {
									sample = iterEntry.getKey();
								}
							}
							if (anyHasNext) {
								final StringBuilder errorMessage = new StringBuilder(
										HEADER);
								for (final Entry<Integer, DataCell> entry : sample
										.entrySet()) {
									errorMessage
											.append(entry.getValue())
											.append("|")
											.append(
													entry.getKey().intValue() + 1)
											.append("\n");
								}
								throw new IllegalStateException(errorMessage
										.toString());
							}
						}
						final ArrayList<Integer> list = new ArrayList<Integer>(
								ret);
						Collections.sort(list);
						// lastRowIndex = list.get(list.size() - 1).intValue();
						if (reverse) {
							Collections.reverse(list);
						}
						return list;
					}

					@SuppressWarnings("synthetic-access")
					@Override
					public boolean hasNext() {
						final boolean ret = hasNext;
						if (!ret && sb.length() > HEADER.length()) {
							logger.debug(sb);
						}
						return ret;
					}

					@Override
					public void remove() {
						throw new UnsupportedOperationException();
					}
				};
			}
		};
	}

	/**
	 * Compute the row content to row indices/keys list map.
	 * 
	 * @param inputTable
	 *            The input {@link BufferedDataTable}.
	 * @param colIndices
	 *            The interesting column indices.
	 * @return A mapping from the different row contents to the row identifiers
	 *         lists.
	 */
	private Map<Map<Integer, DataCell>, List<Pair<RowKey, Integer>>> computePatternToKeys(
			final BufferedDataTable inputTable, final int[] colIndices) {
		final Map<Map<Integer, DataCell>, List<Pair<RowKey, Integer>>> patternToKeys = new HashMap<Map<Integer, DataCell>, List<Pair<RowKey, Integer>>>();
		{
			int rowIndex = 0;
			for (final DataRow row : inputTable) {
				final HashMap<Integer, DataCell> key = new HashMap<Integer, DataCell>();
				for (int i = colIndices.length; i-- > 0;) {
					if (colIndices[i] != -1) {
						final DataCell cell = row.getCell(colIndices[i]);
						key.put(Integer.valueOf(colIndices[i]), cell);
					}
				}
				if (!patternToKeys.containsKey(key)) {
					patternToKeys.put(key,
							new ArrayList<Pair<RowKey, Integer>>());
				}
				final List<Pair<RowKey, Integer>> list = patternToKeys.get(key);
				list.add(new Pair<RowKey, Integer>(row.getKey(), Integer
						.valueOf(rowIndex)));
				++rowIndex;
			}
		}
		return patternToKeys;
	}

	/**
	 * Selects the interesting column indices.
	 * 
	 * @param columns
	 *            The name of interesting columns.
	 * @param dataTableSpec
	 *            The {@link DataTableSpec}.
	 * @return The indices of columns. Might contain {@code -1} values where not
	 *         found.
	 * @see DataTableSpec#findColumnIndex(String)
	 */
	private int[] computeColIndices(final List<String> columns,
			final DataTableSpec dataTableSpec) {
		final int[] colIndices = new int[columns.size()];
		{
			int index = 0;
			for (final String column : columns) {
				colIndices[index++] = dataTableSpec.findColumnIndex(column);
			}
		}
		return colIndices;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void reset() {
		// Models build during execute are cleared here.
		// Also data handled in load/saveInternals will be erased here.
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
			throws InvalidSettingsException {

		// TODO: check if user settings are available, fit to the incoming
		// table structure, and the incoming types are feasible for the node
		// to execute. If the node can execute in its current state return
		// the spec of its output data table(s) (if you can, otherwise an array
		// with null elements), or throw an exception with a useful user message

		return inSpecs;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveSettingsTo(final NodeSettingsWO settings) {
		mergeColumns.saveSettingsTo(settings);
		sortInMemory.saveSettingsTo(settings);
		haltOnError.saveSettingsTo(settings);
		sortOrderReversed.saveSettingsTo(settings);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
			throws InvalidSettingsException {
		mergeColumns.loadSettingsFrom(settings);
		sortInMemory.loadSettingsFrom(settings);
		haltOnError.loadSettingsFrom(settings);
		sortOrderReversed.loadSettingsFrom(settings);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void validateSettings(final NodeSettingsRO settings)
			throws InvalidSettingsException {
		mergeColumns.validateSettings(settings);
		sortInMemory.validateSettings(settings);
		haltOnError.validateSettings(settings);
		sortOrderReversed.validateSettings(settings);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadInternals(final File internDir,
			final ExecutionMonitor exec) throws IOException,
			CanceledExecutionException {

		// Everything handed to output ports is loaded automatically (data
		// returned by the execute method, models loaded in loadModelContent,
		// and user settings set through loadSettingsFrom - is all taken care
		// of). Load here only the other internals that need to be restored
		// (e.g. data used by the views).

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveInternals(final File internDir,
			final ExecutionMonitor exec) throws IOException,
			CanceledExecutionException {
		// Everything written to output ports is saved automatically (data
		// returned by the execute method, models saved in the saveModelContent,
		// and user settings saved through saveSettingsTo - is all taken care
		// of). Save here only the other internals that need to be preserved
		// (e.g. data used by the views).
	}

}

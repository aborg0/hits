package ie.tcd.imm.knime.util.power;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.collection.CollectionCellFactory;
import org.knime.core.data.collection.ListCell;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelFilterString;

/**
 * This is the model implementation of Subsets. Generates all possible subsets
 * of the input.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
public class SubsetsNodeModel extends NodeModel {

	/** Configuration key for the original column names. */
	static final String CFGKEY_COLUMN_NAMES = "original.columns";
	/** Configuration key for the multiset creation property. */
	static final String CFGKEY_CREATE_MULTISET = "create.multiset";
	/** Default value of the multiset creation property. */
	static final boolean DEFAULT_CREATE_MULTISET = false;

	private final SettingsModelFilterString origColumns = new SettingsModelFilterString(
			CFGKEY_COLUMN_NAMES);

	private final SettingsModelBoolean createMultiSet = new SettingsModelBoolean(
			CFGKEY_CREATE_MULTISET, DEFAULT_CREATE_MULTISET);

	/**
	 * Constructor for the node model.
	 */
	protected SubsetsNodeModel() {
		super(1, 1);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
			final ExecutionContext exec) throws Exception {
		final BufferedDataTable inputTable = inData[0];
		final DataTableSpec inputSpec = inputTable.getDataTableSpec();
		final BufferedDataContainer container = exec
				.createDataContainer(new DataTableSpec(
						createResultColSpecs(inputSpec)));
		final List<List<DataCell>> rows = new ArrayList<List<DataCell>>();
		final List<String> selectedColumns = origColumns.getIncludeList();
		final int[] columnIndices = new int[selectedColumns.size()];
		for (int i = columnIndices.length; i-- > 0;) {
			columnIndices[i] = inputSpec
					.findColumnIndex(selectedColumns.get(i));
		}
		final Set<List<DataCell>> values = new HashSet<List<DataCell>>();
		for (final DataRow row : inputTable) {
			final List<DataCell> content = new ArrayList<DataCell>(
					columnIndices.length);
			for (int i = 0; i < columnIndices.length; ++i) {
				content.add(row.getCell(columnIndices[i]));
			}
			if (!values.contains(content) || createMultiSet.getBooleanValue()) {
				values.add(content);
				rows.add(content);
			}
		}
		if (rows.size() > 0) {
			addValues(container, rows,
					Collections.<List<DataCell>> emptyList(), 0
					/* actual position */, 0/* next ID */);
		}
		container.close();
		final BufferedDataTable ret = container.getTable();
		return new BufferedDataTable[] { ret };
	}

	/**
	 * Generates the subset rows to {@code container}.
	 * 
	 * @param container
	 *            The output table.
	 * @param rows
	 *            The (relevant) contents of the original table.
	 * @param currentList
	 *            The list currently set.
	 * @param actualPosition
	 *            The current position in the list of original table ({@code
	 *            rows})
	 * @param nextId
	 *            The id of the next row in the table.
	 * @return The new next id.
	 */
	private int addValues(final BufferedDataContainer container,
			final List<List<DataCell>> rows,
			final List<List<DataCell>> currentList, final int actualPosition,
			final int nextId) {
		if (actualPosition < rows.size()) {
			final int newId = addValues(container, rows, currentList,
					actualPosition + 1, nextId);
			final ArrayList<List<DataCell>> newList = new ArrayList<List<DataCell>>(
					currentList);
			final List<DataCell> dataRow = rows.get(actualPosition);
			newList.add(dataRow);
			return addValues(container, rows, newList, actualPosition + 1,
					newId);
		}
		final DataCell[] cells = new DataCell[origColumns.getIncludeList()
				.size()];
		for (int i = origColumns.getIncludeList().size(); i-- > 0;) {
			final List<DataCell> list = new ArrayList<DataCell>();
			for (final List<DataCell> row : currentList) {
				list.add(row.get(i));
			}
			cells[i] = CollectionCellFactory.createListCell(list);
		}
		container.addRowToTable(new DefaultRow(String.valueOf(nextId), cells));
		return nextId + 1;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void reset() {
		// No internal state
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
			throws InvalidSettingsException {
		final DataTableSpec dataTableSpec = inSpecs[0];
		final DataColumnSpec[] colSpecs = createResultColSpecs(dataTableSpec);
		final DataTableSpec ret = new DataTableSpec(colSpecs);
		return new DataTableSpec[] { ret };
	}

	/**
	 * @param dataTableSpec
	 *            The original {@link DataTableSpec}.
	 * @return The new column spec.
	 */
	private DataColumnSpec[] createResultColSpecs(
			final DataTableSpec dataTableSpec) {
		final List<String> columnNames = origColumns.getIncludeList();
		final DataColumnSpec[] colSpecs = new DataColumnSpec[columnNames.size()];
		for (int i = columnNames.size(); i-- > 0;) {
			colSpecs[i] = new DataColumnSpecCreator(columnNames.get(i),
					ListCell.getCollectionType(dataTableSpec.getColumnSpec(
							columnNames.get(i)).getType())).createSpec();
		}
		return colSpecs;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveSettingsTo(final NodeSettingsWO settings) {
		origColumns.saveSettingsTo(settings);
		createMultiSet.saveSettingsTo(settings);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
			throws InvalidSettingsException {
		origColumns.loadSettingsFrom(settings);
		createMultiSet.loadSettingsFrom(settings);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void validateSettings(final NodeSettingsRO settings)
			throws InvalidSettingsException {
		origColumns.validateSettings(settings);
		createMultiSet.validateSettings(settings);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadInternals(final File internDir,
			final ExecutionMonitor exec) throws IOException,
			CanceledExecutionException {
		// No internal state
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveInternals(final File internDir,
			final ExecutionMonitor exec) throws IOException,
			CanceledExecutionException {
		// No internal state
	}

}

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
import org.knime.core.data.DataValue;
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
import org.knime.core.node.defaultnodesettings.SettingsModelColumnName;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * This is the model implementation of Subsets. Generates all possible subsets
 * of the input.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
public class SubsetsNodeModel extends NodeModel {

	/** Configuration key for the new column name. */
	static final String CFGKEY_NEW_COLUMN_NAME = "new.column";
	/** Default value of the new column name. */
	static final String DEFAULT_NEW_COLUMN_NAME = "subsets";
	/** Configuration key for the original column name. */
	static final String CFGKEY_ORIGINAL_COLUMN_NAME = "original.column";
	/** Configuration key for the multiset creation property. */
	static final String CFGKEY_CREATE_MULTISET = "create.multiset";
	/** Default value of the multiset creation property. */
	static final boolean DEFAULT_CREATE_MULTISET = false;

	private final SettingsModelString newColumn = new SettingsModelString(
			CFGKEY_NEW_COLUMN_NAME, DEFAULT_NEW_COLUMN_NAME);

	private final SettingsModelColumnName origColumn = new SettingsModelColumnName(
			CFGKEY_ORIGINAL_COLUMN_NAME, "");

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
		final BufferedDataContainer container = exec
				.createDataContainer(new DataTableSpec(
						createResultColSpecs(inData[0].getDataTableSpec())));
		final List<DataRow> rows = new ArrayList<DataRow>();
		final int origColumnIdx = inData[0].getSpec().findColumnIndex(
				origColumn.getColumnName());
		final Set<DataValue> values = new HashSet<DataValue>();
		for (final DataRow row : inData[0]) {
			if (!values.contains(row.getCell(origColumnIdx))
					|| createMultiSet.getBooleanValue()) {
				values.add(row.getCell(origColumnIdx));
				rows.add(row);
			}
		}
		final int lastColIdx = inData[0].getDataTableSpec().getNumColumns();
		addValues(container, rows, Collections.<DataCell> emptyList(), 0,
				origColumnIdx, lastColIdx, 0);
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
	 * @param origColumnIdx
	 *            The column index in the original rows ({@code rows} )
	 * @param lastColIdx
	 *            The last column index in the output table. (Here will go the
	 *            list of {@link DataCell}s.)
	 * @param nextId
	 *            The id of the next row in the table.
	 * @return The new next id.
	 */
	private int addValues(final BufferedDataContainer container,
			final List<DataRow> rows, final List<DataCell> currentList,
			final int actualPosition, final int origColumnIdx,
			final int lastColIdx, final int nextId) {
		if (actualPosition < rows.size()) {
			final int newId = addValues(container, rows, currentList,
					actualPosition + 1, origColumnIdx, lastColIdx, nextId);
			final ArrayList<DataCell> newList = new ArrayList<DataCell>(
					currentList);
			final DataRow dataRow = rows.get(actualPosition);
			newList.add(dataRow.getCell(origColumnIdx));
			return addValues(container, rows, newList, actualPosition + 1,
					origColumnIdx, lastColIdx, newId);
		}
		final DataRow dataRow = rows.get(0);
		final DataCell[] cells = new DataCell[lastColIdx + 1];
		for (int i = lastColIdx; i-- > 0;) {
			cells[i] = dataRow.getCell(i);
		}
		cells[lastColIdx] = CollectionCellFactory.createListCell(currentList);
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
	 * @return The new column spec.
	 */
	private DataColumnSpec[] createResultColSpecs(
			final DataTableSpec dataTableSpec) {
		final DataColumnSpec[] colSpecs = new DataColumnSpec[dataTableSpec
				.getNumColumns() + 1];
		for (int i = dataTableSpec.getNumColumns(); i-- > 0;) {
			colSpecs[i] = dataTableSpec.getColumnSpec(i);
		}
		final DataColumnSpec columnSpec = dataTableSpec
				.getColumnSpec(origColumn.getColumnName());
		if (columnSpec == null) {
			throw new IllegalArgumentException("No column selected!");
		}
		colSpecs[dataTableSpec.getNumColumns()] = new DataColumnSpecCreator(
				newColumn.getStringValue(), ListCell
						.getCollectionType(columnSpec.getType())).createSpec();
		return colSpecs;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveSettingsTo(final NodeSettingsWO settings) {
		newColumn.saveSettingsTo(settings);
		origColumn.saveSettingsTo(settings);
		createMultiSet.saveSettingsTo(settings);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
			throws InvalidSettingsException {
		newColumn.loadSettingsFrom(settings);
		origColumn.loadSettingsFrom(settings);
		createMultiSet.loadSettingsFrom(settings);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void validateSettings(final NodeSettingsRO settings)
			throws InvalidSettingsException {
		newColumn.validateSettings(settings);
		origColumn.validateSettings(settings);
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

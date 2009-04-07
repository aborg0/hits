/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package ie.tcd.imm.knime.util.pivot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.io.File;
import java.io.IOException;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnDomain;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.def.DefaultRow;
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
import org.knime.core.node.defaultnodesettings.SettingsModelFilterString;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.flowvariable.FlowVariablePortObject;
import org.knime.core.node.port.flowvariable.FlowVariablePortObjectSpec;

/**
 * This is the model implementation of Pivot. Converts some information present
 * in rows to columns.
 * 
 * TODO add HiLite support!
 * 
 * TODO improve row key generation
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
public class PivotNodeModel extends NodeModel {

	/**  */
	// private static final String VALUE_PATTERN = "\\$[^\\$]+\\$";
	/**  */
	// private static final String VAR_PATTERN = "\\$\\$[^\\$]+\\$\\$";
	// the logger instance
	private static final NodeLogger logger = NodeLogger
			.getLogger(PivotNodeModel.class);

	/** Those columns whose values will be converted to columns */
	static final String CFGKEY_TO_COLUMNS = "to columns";
	/** Those columns whose values will remain values in the converted columns */
	static final String CFGKEY_VALUES = "values";
	/** Those columns which remain the same (except row count). */
	static final String CFGKEY_KEYS = "keys";

	static final String CFGKEY_PATTERN = "pattern";

	static final String CFGKEY_BEHAVIOUR = "behaviour";

	static final String DEFAULT_BEHAVIOUR = Behaviour.fillEmpty.name();

	private final SettingsModelFilterString toColumns = new SettingsModelFilterString(
			CFGKEY_TO_COLUMNS);
	private final SettingsModelFilterString values = new SettingsModelFilterString(
			CFGKEY_VALUES);
	private final SettingsModelFilterString keys = new SettingsModelFilterString(
			CFGKEY_KEYS);

	private final SettingsModelString pattern = new SettingsModelString(
			CFGKEY_PATTERN, "");

	private final SettingsModelString behaviourModel = new SettingsModelString(
			CFGKEY_BEHAVIOUR, DEFAULT_BEHAVIOUR);

	enum Behaviour {
		fillEmpty, signalError;
	}

	/**
	 * Constructor for the node model.
	 */
	protected PivotNodeModel() {
		super(new PortType[] { BufferedDataTable.TYPE }, new PortType[] {
				BufferedDataTable.TYPE, FlowVariablePortObject.TYPE });
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected PortObject[] execute(final PortObject[] inData,
			final ExecutionContext exec) throws Exception {
		logger.debug("Pivoting start");
		final Behaviour behaviour = Behaviour.valueOf(behaviourModel
				.getStringValue());
		// the data table spec of the single output table,
		// the table will have three columns:
		final BufferedDataTable table = (BufferedDataTable) inData[0];
		final DataTableSpec outputSpec = createTableSpec(table.getSpec());
		// the execution context will provide us with storage capacity, in this
		// case a data container to which we will add rows sequentially
		// Note, this container can also handle arbitrary big data tables, it
		// will buffer to disc if necessary.
		final BufferedDataContainer container = exec
				.createDataContainer(outputSpec);
		final List<Object> parts = getParts(table.getSpec());
		final List<Column> columns = filterColumns(parts);
		final List<Map<Column, String>> vals = new ArrayList<Map<Column, String>>();
		vals.add(new HashMap<Column, String>());
		generateValues(columns, vals);
		final int[] keyIndices = findIndices(table, keys.getIncludeList());
		final int[] pivotIndices = findIndices(table, toColumns
				.getIncludeList());
		final Column[] cols = new Column[pivotIndices.length];
		for (int index = pivotIndices.length; index-- > 0;) {
			for (final Column column : columns) {
				if (column.spec.equals(table.getSpec().getColumnSpec(
						pivotIndices[index]))) {
					cols[index] = column;
					break;
				}
			}
		}
		final int[] valueIndices = findIndices(table, values.getIncludeList());
		int i = 0;
		final Map<Map<Column, String>, DataRow> connectByPivotValues = new HashMap<Map<Column, String>, DataRow>();
		final List<DataCell> keyValues = new ArrayList<DataCell>();
		int newRowCount = 0;
		int origRow = 0;
		for (final DataRow row : table) {
			if (i < vals.size()) {
				if (i == 0) {
					for (final int index : keyIndices) {
						keyValues.add(row.getCell(index));
					}
				}
				for (int index = keyIndices.length; index-- > 0;) {
					if (!row.getCell(keyIndices[index]).equals(
							keyValues.get(index))) {
						switch (behaviour) {
						case fillEmpty:
							i = 0;
							newRowCount = createNewRow(container, vals,
									keyIndices, valueIndices,
									connectByPivotValues, keyValues,
									newRowCount, row);
							break;
						case signalError:
							throw new IllegalStateException("Wrong structure: "
									+ keys.getIncludeList().get(index)
									+ "'s value: " + row.getCell(index)
									+ " is not " + keyValues.get(index));
						default:
							break;
						}
					}
				}
				processRow(pivotIndices, cols, connectByPivotValues, row);
			} else {
				i = 0;
				newRowCount = createNewRow(container, vals, keyIndices,
						valueIndices, connectByPivotValues, keyValues,
						newRowCount, row);
				processRow(pivotIndices, cols, connectByPivotValues, row);
			}
			++i;
			exec.checkCanceled();
			exec.setProgress(origRow++ * 1.0 / table.getRowCount());
		}
		createNewRow(container, vals, keyIndices, valueIndices,
				connectByPivotValues, keyValues, newRowCount, null);
		container.close();
		logger.debug("Pivoting finished");
		final BufferedDataTable out = container.getTable();
		final FlowVariablePortObject portObject = new FlowVariablePortObject();
		pushScopeVariableString("reversePattern", getReversePattern(pattern
				.getStringValue()));
		return new PortObject[] { out, portObject };
	}

	/**
	 * @param pivotIndices
	 * @param cols
	 * @param connectByPivotValues
	 * @param row
	 */
	private void processRow(final int[] pivotIndices, final Column[] cols,
			final Map<Map<Column, String>, DataRow> connectByPivotValues,
			final DataRow row) {
		final Map<Column, String> pivotMap = new HashMap<Column, String>();
		for (int index = pivotIndices.length; index-- > 0;) {
			pivotMap.put(cols[index], row.getCell(pivotIndices[index])
					.toString());
		}
		connectByPivotValues.put(pivotMap, row);
	}

	/**
	 * @param container
	 * @param vals
	 * @param keyIndices
	 * @param valueIndices
	 * @param connectByPivotValues
	 * @param keyValues
	 * @param newRowCount
	 * @param row
	 * @return
	 */
	private int createNewRow(final BufferedDataContainer container,
			final List<Map<Column, String>> vals, final int[] keyIndices,
			final int[] valueIndices,
			final Map<Map<Column, String>, DataRow> connectByPivotValues,
			final List<DataCell> keyValues, final int newRowCount,
			final DataRow row) {
		if (!connectByPivotValues.isEmpty()) {
			final List<DataCell> cells = new ArrayList<DataCell>();
			for (final DataCell dataCell : keyValues) {
				cells.add(dataCell);
			}
			addValues(connectByPivotValues, valueIndices, vals, cells);
			final DataRow newRow = new DefaultRow("Row" + newRowCount, cells);
			container.addRowToTable(newRow);
		}
		keyValues.clear();
		connectByPivotValues.clear();
		if (row != null) {
			for (final int index : keyIndices) {
				keyValues.add(row.getCell(index));
			}
		}
		return newRowCount + 1;
	}

	/**
	 * @param connectByPivotValues
	 * @param valueIndices
	 * @param vals
	 * @param cells
	 */
	private void addValues(
			final Map<Map<Column, String>, DataRow> connectByPivotValues,
			final int[] valueIndices, final List<Map<Column, String>> vals,
			final List<DataCell> cells) {
		for (final int valIndex : valueIndices) {
			addValues(connectByPivotValues, valIndex, vals, cells);
		}
	}

	/**
	 * @param connectByPivotValues
	 * @param valIndex
	 * @param vals
	 * @param cells
	 */
	private void addValues(
			final Map<Map<Column, String>, DataRow> connectByPivotValues,
			final int valIndex, final List<Map<Column, String>> vals,
			final List<DataCell> cells) {
		for (final Map<Column, String> map : vals) {
			final DataRow row = connectByPivotValues.get(map);
			cells.add(row == null ? DataType.getMissingCell() : row
					.getCell(valIndex));
		}
	}

	/**
	 * @param table
	 * @param includeList
	 * @return
	 */
	private int[] findIndices(final BufferedDataTable table,
			final List<String> includeList) {
		final int[] keyIndices = new int[includeList.size()];
		int i = 0;
		for (final String keyName : includeList) {
			keyIndices[i++] = table.getSpec().findColumnIndex(keyName);
		}
		return keyIndices;
	}

	private static final class Variable {
		final String name;

		private Variable(final String name) {
			super();
			this.name = name;
		}
	}

	private static final class Column {
		final DataColumnSpec spec;

		private Column(final DataColumnSpec spec) {
			this.spec = spec;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + (spec == null ? 0 : spec.hashCode());
			return result;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(final Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (getClass() != obj.getClass()) {
				return false;
			}
			final Column other = (Column) obj;
			if (spec == null) {
				if (other.spec != null) {
					return false;
				}
			} else if (!spec.equals(other.spec)) {
				return false;
			}
			return true;
		}
	}

	private static class VarColumn {
		static final VarColumn instance = new VarColumn();

		private VarColumn() {
			super();
		}
	}

	/**
	 * @param dataTableSpec
	 * @return
	 * @throws InvalidSettingsException
	 */
	private DataTableSpec createTableSpec(final DataTableSpec dataTableSpec)
			throws InvalidSettingsException {
		final List<Object> parts = getParts(dataTableSpec);
		final List<DataColumnSpec> spec = new ArrayList<DataColumnSpec>();
		for (final String key : keys.getIncludeList()) {
			spec.add(dataTableSpec.getColumnSpec(key));
		}
		final List<Column> cols = filterColumns(parts);
		final List<Map<Column, String>> vals = new ArrayList<Map<Column, String>>();
		vals.add(new HashMap<Column, String>());
		generateValues(cols, vals);
		for (final String valueColName : values.getIncludeList()) {
			for (final Map<Column, String> map : vals) {
				spec.add(createColSpec(parts, map, dataTableSpec
						.getColumnSpec(valueColName)));
			}
		}
		return new DataTableSpec(spec.toArray(new DataColumnSpec[spec.size()]));
	}

	/**
	 * @param parts
	 * @param map
	 * @param dataColumnSpec
	 * @return
	 */
	private DataColumnSpec createColSpec(final List<Object> parts,
			final Map<Column, String> map, final DataColumnSpec dataColumnSpec) {
		final StringBuilder name = new StringBuilder();
		for (final Object object : parts) {
			if (object instanceof String) {
				final String part = (String) object;
				name.append(part);
			} else if (object instanceof Variable) {
				final Variable part = (Variable) object;
				name.append(peekScopeVariableString(part.name));
			} else if (object instanceof VarColumn) {
				name.append(dataColumnSpec.getName());
			} else if (object instanceof Column) {
				final Column part = (Column) object;
				name.append(map.get(part));
			} else {
				throw new IllegalStateException("Unknown object: "
						+ object.getClass());
			}
		}
		final DataColumnSpecCreator creator = new DataColumnSpecCreator(
				dataColumnSpec);
		creator.setName(name.toString());
		return creator.createSpec();
	}

	/**
	 * @param cols
	 * @param vals
	 */
	private void generateValues(final List<Column> cols,
			final List<Map<Column, String>> vals) {
		if (cols.size() == 0) {
			return;
		}
		final Column column = cols.get(0);
		final List<Map<Column, String>> origVals = new ArrayList<Map<Column, String>>();
		for (final Map<Column, String> map : vals) {
			origVals.add(new HashMap<Column, String>(map));
		}
		vals.clear();
		final DataColumnDomain domain = column.spec.getDomain();
		for (final DataCell cell : domain.getValues()) {
			for (final Map<Column, String> map : origVals) {
				final HashMap<Column, String> newMap = new HashMap<Column, String>(
						map);
				newMap.put(column, cell.toString());
				vals.add(newMap);
			}
		}
		generateValues(cols.subList(1, cols.size()), vals);
	}

	/**
	 * @param parts
	 * @return
	 */
	private List<Column> filterColumns(final List<Object> parts) {
		final List<Column> cols = new ArrayList<Column>();
		for (final Object object : parts) {
			if (object instanceof Column) {
				final Column col = (Column) object;
				cols.add(col);
			}
		}
		return cols;
	}

	/**
	 * @param dataTableSpec
	 * @return
	 * @throws InvalidSettingsException
	 */
	private List<Object> getParts(final DataTableSpec dataTableSpec)
			throws InvalidSettingsException {
		final String patternVal = pattern.getStringValue();
		final String[] vars = patternVal.split("\\$");
		final List<Object> parts = new ArrayList<Object>();
		parts.add(vars[0]);
		for (int i = 1; i < vars.length; ++i) {
			if (vars[i].startsWith("["))// variable
			{
				try {
					final int terminator = vars[i].indexOf(']');
					final String varName = vars[i].substring(1, terminator);
					final String var = peekScopeVariableString(varName);
					if (var == null) {
						new InvalidSettingsException("No variable: " + varName);
					}
					parts.add(new Variable(varName));
					if (vars[i].length() - 1 != terminator) {
						parts.add(vars[i].substring(terminator + 1));
					}
				} catch (final RuntimeException e) {
					new InvalidSettingsException(e);
				}
			} else if (vars[i].startsWith("{"))// column
			{
				try {
					final int terminator = vars[i].indexOf('}');
					final String colName = vars[i].substring(1, terminator);
					if (colName.isEmpty()) {
						parts.add(VarColumn.instance);
						if (vars[i].length() - 1 != terminator) {
							parts.add(vars[i].substring(terminator + 1));
						}
						continue;
					}
					final DataColumnSpec spec = dataTableSpec
							.getColumnSpec(colName);
					if (spec == null) {
						throw new InvalidSettingsException(
								"No column with name: " + colName);
					}
					if (spec.getDomain().getValues() == null) {
						throw new InvalidSettingsException(
								"Not a nominal column: " + colName);
					}
					parts.add(new Column(spec));
					if (vars[i].length() - 1 != terminator) {
						parts.add(vars[i].substring(terminator + 1));
					}
				} catch (final RuntimeException e) {
					throw new InvalidSettingsException(e);
				}
			} else {
				throw new InvalidSettingsException(
						"The $ must be followed by {column name} or [variable name]");
			}
		}
		return parts;
	}

	/**
	 * @param stringValue
	 * @return
	 */
	private String getReversePattern(final String stringValue) {
		// TODO Auto-generated method stub
		return "";
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void reset() {
		// TODO Code executed on reset.
		// Models build during execute are cleared here.
		// Also data handled in load/saveInternals will be erased here.
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs)
			throws InvalidSettingsException {

		// TODO: check if user settings are available, fit to the incoming
		// table structure, and the incoming types are feasible for the node
		// to execute. If the node can execute in its current state return
		// the spec of its output data table(s) (if you can, otherwise an array
		// with null elements), or throw an exception with a useful user message

		return new PortObjectSpec[] {
				createTableSpec((DataTableSpec) inSpecs[0]),
				FlowVariablePortObjectSpec.INSTANCE };
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveSettingsTo(final NodeSettingsWO settings) {

		// TODO save user settings to the config object.
		toColumns.saveSettingsTo(settings);
		values.saveSettingsTo(settings);
		keys.saveSettingsTo(settings);
		pattern.saveSettingsTo(settings);
		behaviourModel.saveSettingsTo(settings);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
			throws InvalidSettingsException {

		// TODO load (valid) settings from the config object.
		// It can be safely assumed that the settings are valided by the
		// method below.
		toColumns.loadSettingsFrom(settings);
		values.loadSettingsFrom(settings);
		keys.loadSettingsFrom(settings);
		pattern.loadSettingsFrom(settings);
		behaviourModel.loadSettingsFrom(settings);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void validateSettings(final NodeSettingsRO settings)
			throws InvalidSettingsException {

		// TODO check if the settings could be applied to our model
		// e.g. if the count is in a certain range (which is ensured by the
		// SettingsModel).
		// Do not actually set any values of any member variables.
		toColumns.validateSettings(settings);
		values.validateSettings(settings);
		keys.validateSettings(settings);
		pattern.validateSettings(settings);
		behaviourModel.validateSettings(settings);
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

/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package ie.tcd.imm.knime.util.pivot;

import ie.tcd.imm.hits.knime.util.Misc;
import ie.tcd.imm.hits.knime.util.TransformingNodeModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnDomain;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.RowKey;
import org.knime.core.data.container.DataContainer;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
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
 * TODO improve row key generation
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
public class PivotNodeModel extends TransformingNodeModel {
	// the logger instance
	private static final NodeLogger logger = NodeLogger
			.getLogger(PivotNodeModel.class);

	/** Those columns whose values will be converted to columns */
	static final String CFGKEY_TO_COLUMNS = "to columns";
	/** Those columns which remain the same (except row count). */
	static final String CFGKEY_KEYS = "keys";

	/** Configuration key for the pattern to generate the columns. */
	static final String CFGKEY_PATTERN = "pattern";

	/** Configuration key for the intended behaviour. */
	static final String CFGKEY_BEHAVIOUR = "behaviour";

	/** Default value for the intended behaviour. */
	static final String DEFAULT_BEHAVIOUR = Behaviour.fillEmpty.name();

	private final SettingsModelFilterString toColumns = new SettingsModelFilterString(
			CFGKEY_TO_COLUMNS);
	private final SettingsModelFilterString keys = new SettingsModelFilterString(
			CFGKEY_KEYS);

	private final SettingsModelString pattern = new SettingsModelString(
			CFGKEY_PATTERN, "");

	private final SettingsModelString behaviourModel = new SettingsModelString(
			CFGKEY_BEHAVIOUR, DEFAULT_BEHAVIOUR);

	/** The intended behaviour. */
	enum Behaviour {
		/** Fills the non-existing parts with empty cells. */
		fillEmpty,
		/** Reports an error when non-existing parts to be generated. */
		signalError;
	}

	/**
	 * Constructor for the node model.
	 */
	protected PivotNodeModel() {
		super(new PortType[] { BufferedDataTable.TYPE }, new PortType[] {
				BufferedDataTable.TYPE, FlowVariablePortObject.TYPE },
				Collections.singletonMap(Integer.valueOf(0), Collections
						.singletonList(Integer.valueOf(0))));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected PortObject[] executeDerived(final PortObject[] inData,
			final ExecutionContext exec) throws Exception {
		logger.debug("Pivoting start");
		final Behaviour behaviour = Behaviour.valueOf(behaviourModel
				.getStringValue());
		final BufferedDataTable table = (BufferedDataTable) inData[0];
		final DataTableSpec spec = table.getSpec();
		final DataTableSpec outputSpec = createTableSpec(spec);
		final BufferedDataContainer container = exec
				.createDataContainer(outputSpec);
		final List<?> parts = getParts(spec);
		final List<Column> columns = filterColumns(parts);
		final List<Map<Column, String>> vals = new ArrayList<Map<Column, String>>();
		vals.add(new HashMap<Column, String>());
		generateValues(columns, vals);
		final int[] keyIndices = findIndices(spec, keys.getIncludeList());
		final int[] pivotIndices = findIndices(spec, toColumns.getIncludeList());
		final Column[] cols = new Column[pivotIndices.length];
		for (int index = pivotIndices.length; index-- > 0;) {
			for (final Column column : columns) {
				if (column.spec.equals(spec.getColumnSpec(pivotIndices[index]))) {
					cols[index] = column;
					break;
				}
			}
		}
		final int[] valueIndices = findIndices(spec, keys.getExcludeList());
		int i = 0;
		final Map<Map<Column, String>, DataRow> connectByPivotValues = new HashMap<Map<Column, String>, DataRow>();
		final List<DataCell> keyValues = new ArrayList<DataCell>();
		int newRowCount = 0;
		int origRow = 0;
		final Map<RowKey, Set<RowKey>> mapping = createMapping();
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
									newRowCount, row, mapping);
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
						newRowCount, row, mapping);
				processRow(pivotIndices, cols, connectByPivotValues, row);
			}
			++i;
			exec.checkCanceled();
			exec.setProgress(origRow++ * 1.0 / table.getRowCount());
		}
		createNewRow(container, vals, keyIndices, valueIndices,
				connectByPivotValues, keyValues, newRowCount, null, mapping);
		if (mapping != null) {
			setMapping(true, 0, 0, mapping);
		}
		container.close();
		logger.debug("Pivoting finished");
		final BufferedDataTable out = container.getTable();
		final FlowVariablePortObject portObject = new FlowVariablePortObject();
		pushScopeVariableString("reversePattern", getReversePattern(parts));
		return new PortObject[] { out, portObject };
	}

	@Override
	protected BufferedDataTable[] executeDerived(
			final BufferedDataTable[] inData, final ExecutionContext exec)
			throws Exception {
		throw new UnsupportedOperationException();
	}

	/**
	 * @param pivotIndices
	 * @param cols
	 * @param connectByPivotValues
	 * @param row
	 */
	private static void processRow(final int[] pivotIndices,
			final Column[] cols,
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
	 * Creates new row based on the grouped {@code vals}, and adds them the
	 * {@code container}.
	 * 
	 * @param container
	 *            A {@link DataContainer}.
	 * @param vals
	 *            Pivot columns. {@link Column}.
	 * @param keyIndices
	 *            The indices to key columns.
	 * @param valueIndices
	 *            The indices to unnamed columns. ({@link VarColumn})
	 * @param connectByPivotValues
	 *            Some adjacent rows in a map structure.
	 * @param keyValues
	 *            The values in the key columns. (<b>out parameter</b>!)
	 * @param newRowCount
	 *            The actual row number.
	 * @param row
	 *            A row to fill the {@code keyValues} list.
	 * @param mapping
	 *            The mapping from the new row keys to the original ones.
	 * @return The next row identifier.
	 */
	private static int createNewRow(final DataContainer container,
			final List<Map<Column, String>> vals, final int[] keyIndices,
			final int[] valueIndices,
			final Map<Map<Column, String>, DataRow> connectByPivotValues,
			final List<DataCell> keyValues, final int newRowCount,
			// @Nullable
			final DataRow row, final Map<RowKey, Set<RowKey>> mapping) {
		if (!connectByPivotValues.isEmpty()) {
			final List<DataCell> cells = new ArrayList<DataCell>();
			for (final DataCell dataCell : keyValues) {
				cells.add(dataCell);
			}
			addValues(connectByPivotValues, valueIndices, vals, cells);
			final DataRow newRow = new DefaultRow("Row" + newRowCount, cells);
			if (mapping != null) {
				final Set<RowKey> rowKeySet = collectOrigKeys(connectByPivotValues);
				mapping.put(newRow.getKey(), rowKeySet);
			}
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
	 *            Some adjacent rows in a map structure.
	 * @return A {@link Set} of original {@link RowKey}s from {@code
	 *         connectByPivotValues}'s {@link DataRow} values.
	 */
	private static Set<RowKey> collectOrigKeys(
			final Map<Map<Column, String>, DataRow> connectByPivotValues) {
		final Set<RowKey> rowKeySet = new HashSet<RowKey>(
				(int) (connectByPivotValues.size() * 1.5), .7f);
		for (final DataRow origRow : connectByPivotValues.values()) {
			rowKeySet.add(origRow.getKey());
		}
		return rowKeySet;
	}

	/**
	 * Adds the values for each {@code valueIndices} to {@code cells}.
	 * 
	 * @see #addValues(Map, int, List, List)
	 * 
	 * @param connectByPivotValues
	 *            A connection from pivot nodes to some nodes.
	 * @param valueIndices
	 *            The columns to select from the row.
	 * @param vals
	 *            The mapping from the pivot columns to the new column names.
	 * @param cells
	 *            The result cells.
	 */
	private static void addValues(
			final Map<Map<Column, String>, DataRow> connectByPivotValues,
			final int[] valueIndices, final List<Map<Column, String>> vals,
			final List<DataCell> cells) {
		for (final int valIndex : valueIndices) {
			addValues(connectByPivotValues, valIndex, vals, cells);
		}
	}

	/**
	 * Puts the values from the row selected by the {@code vals} from {@code
	 * connectByPivotValues} to {@code cells}.
	 * 
	 * @param connectByPivotValues
	 *            A connection from pivot nodes to some nodes.
	 * @param valIndex
	 *            The column to select from the row.
	 * @param vals
	 *            The mapping from the pivot columns to the new column names.
	 * @param cells
	 *            The result cells.
	 */
	private static void addValues(
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
	 * Finds the proper indices for each column name in {@code includeList} from
	 * the {@code spec}.
	 * 
	 * @param spec
	 *            A {@link DataTableSpec}.
	 * @param includeList
	 * @return An array of ({@code 0} based) indices of {@code includeList} in
	 *         the {@code spec}.
	 */
	private static int[] findIndices(final DataTableSpec spec,
			final List<String> includeList) {
		final int[] keyIndices = new int[includeList.size()];
		int i = 0;
		for (final String keyName : includeList) {
			keyIndices[i++] = spec.findColumnIndex(keyName);
		}
		return keyIndices;
	}

	/** Variable from the meta workflow. */
	private static final class Variable {
		final String name;

		Variable(final String name) {
			super();
			this.name = name;
		}
	}

	/** A column selected. */
	private static final class Column {
		final DataColumnSpec spec;

		Column(final DataColumnSpec spec) {
			this.spec = spec;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + (spec == null ? 0 : spec.hashCode());
			return result;
		}

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

	/** A "value" column. Singleton. */
	private static class VarColumn {
		static final VarColumn instance = new VarColumn();

		private VarColumn() {
			super();
		}
	}

	/**
	 * Creates the new {@link DataTableSpec} based on {@code dataTableSpec} and
	 * the key columns.
	 * 
	 * @param dataTableSpec
	 *            A {@link DataTableSpec}.
	 * @return The new {@link DataTableSpec}.
	 * @throws InvalidSettingsException
	 */
	private DataTableSpec createTableSpec(final DataTableSpec dataTableSpec)
			throws InvalidSettingsException {
		final List<?> parts = getParts(dataTableSpec);
		final List<DataColumnSpec> spec = new ArrayList<DataColumnSpec>();
		for (final String key : keys.getIncludeList()) {
			spec.add(dataTableSpec.getColumnSpec(key));
		}
		final List<Column> cols = filterColumns(parts);
		final List<Map<Column, String>> vals = new ArrayList<Map<Column, String>>();
		vals.add(new HashMap<Column, String>());
		generateValues(cols, vals);
		for (final String valueColName : keys.getExcludeList()) {
			for (final Map<Column, String> map : vals) {
				spec.add(createColSpec(parts, map, dataTableSpec
						.getColumnSpec(valueColName)));
			}
		}
		return new DataTableSpec(spec.toArray(new DataColumnSpec[spec.size()]));
	}

	private DataColumnSpec createColSpec(final List<?> parts,
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
	 * Generate values to {@code vals} based on the selected columns ({@code
	 * cols}).
	 * 
	 * @param cols
	 *            The selected columns.
	 * @param vals
	 *            The values for each column. (One element represents a group of
	 *            columns.) The map's values are the {@link DataCell} values'
	 *            {@link String} representations.
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

	private List<Column> filterColumns(final List<?> parts) {
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
	 * Parses the pattern, and based on that and the {@code dataTableSpec} it
	 * will return a list of {@link Variable}, {@link Column}, {@link String} or
	 * {@link VarColumn}s.
	 * 
	 * @param dataTableSpec
	 *            A {@link DataTableSpec}.
	 * @return The list of objects as the pattern describes.
	 * @throws InvalidSettingsException
	 *             If unrecognised variable, or column found.
	 */
	private List<?> getParts(final DataTableSpec dataTableSpec)
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
						throw new InvalidSettingsException("No variable: "
								+ varName);
					}
					parts.add(new Variable(varName));
					if (vars[i].length() - 1 != terminator) {
						parts.add(vars[i].substring(terminator + 1));
					}
				} catch (final RuntimeException e) {
					throw new InvalidSettingsException(e);
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
	 * Finds a reverse pattern to be applied with an <code>Unpivot</code> node.
	 * 
	 * @param parts
	 *            The parsed pattern parts.
	 * @return The reverse regular expression.
	 * @see #getParts(DataTableSpec)
	 */
	private String getReversePattern(final List<?> parts) {
		final StringBuilder sb = new StringBuilder();
		for (final Object object : parts) {
			if (object instanceof Column) {
				final Column col = (Column) object;
				sb.append("(");
				final Set<DataCell> domValues = col.spec.getDomain()
						.getValues();
				for (final DataCell cell : domValues) {
					sb.append(Matcher.quoteReplacement(cell.toString()))
							.append("|");
				}
				sb.setLength(sb.length() - (domValues.isEmpty() ? 0 : 1));
				sb.append(")");
			}
			if (object instanceof VarColumn) {
				sb.append("(?:");
				for (final String keyColName : keys.getExcludeList()) {
					sb.append(Matcher.quoteReplacement(keyColName)).append("|");
				}
				sb.setLength(sb.length()
						- (keys.getExcludeList().isEmpty() ? 0 : 1));
				sb.append(")");
			}
			if (object instanceof String) {
				final String str = (String) object;
				if (!str.isEmpty()) {
					sb.append("(?:").append(Matcher.quoteReplacement(str))
							.append(")");
				}
			}
		}
		return sb.toString();
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
	protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs)
			throws InvalidSettingsException {
		Misc.checkList(keys.getIncludeList(), (DataTableSpec) inSpecs[0]);
		Misc.checkList(keys.getExcludeList(), (DataTableSpec) inSpecs[0]);
		Misc.checkList(toColumns.getIncludeList(), (DataTableSpec) inSpecs[0]);
		Misc.checkList(toColumns.getExcludeList(), (DataTableSpec) inSpecs[0]);
		if (!toColumns.getExcludeList().containsAll(keys.getIncludeList())
				|| !toColumns.getExcludeList().containsAll(
						keys.getExcludeList())) {
			throw new InvalidSettingsException(
					"The values or the key columns are not consistent. Try reconnect the inport.");
		}
		if (toColumns.getExcludeList().size() != keys.getIncludeList().size()
				+ keys.getExcludeList().size()) {
			throw new InvalidSettingsException(
					"There is an inconsistency in the pivot, keys, values columns. Try reconnect the inport.");
		}
		for (final String toColumn : toColumns.getIncludeList()) {
			if (!pattern.getStringValue().contains("${" + toColumn + "}")) {
				logger
						.warn("The column: "
								+ toColumn
								+ " is not present in the pattern. You might want to review.");
			}
		}
		return new PortObjectSpec[] {
				createTableSpec((DataTableSpec) inSpecs[0]),
				FlowVariablePortObjectSpec.INSTANCE };
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveSettingsTo(final NodeSettingsWO settings) {
		super.saveSettingsTo(settings);
		toColumns.saveSettingsTo(settings);
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
		super.loadValidatedSettingsFrom(settings);
		toColumns.loadSettingsFrom(settings);
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
		super.validateSettings(settings);
		toColumns.validateSettings(settings);
		keys.validateSettings(settings);
		pattern.validateSettings(settings);
		behaviourModel.validateSettings(settings);
	}
}

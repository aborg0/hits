/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package com.mind_era.knime.util.unpivot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.defaultnodesettings.SettingsModelStringArray;

import com.mind_era.knime.common.util.TransformingNodeModel;

/**
 * This is the model implementation of Unpivot. Introduces new rows (and
 * column(s)) based on the column name structure.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
public class UnpivotNodeModel extends TransformingNodeModel {

	// the logger instance
	private static final NodeLogger logger = NodeLogger
			.getLogger(UnpivotNodeModel.class);

	/** Configuration key for new column names. */
	static final String CFGKEY_NEW_COLUMNS = "new columns";
	/** Default names for new columns. */
	static final String[] DEFAULT_NEW_COLUMNS = new String[0];

	/** Configuration key for pattern. */
	static final String CFGKEY_PATTERN = "pattern";
	/** Default value for regular expression pattern. */
	static final String DEFAULT_PATTERN = "";

	private final SettingsModelStringArray newColumnsModel = new SettingsModelStringArray(
			CFGKEY_NEW_COLUMNS, DEFAULT_NEW_COLUMNS);
	private final SettingsModelString patternModel = new SettingsModelString(
			CFGKEY_PATTERN, DEFAULT_PATTERN);

	/**
	 * Constructor for the node model.
	 */
	protected UnpivotNodeModel() {
		super(1, 1);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected BufferedDataTable[] executeDerived(
			final BufferedDataTable[] inData, final ExecutionContext exec)
			throws Exception {
		logger.debug("Unpivot start");

		final BufferedDataTable table = inData[0];
		final BufferedDataContainer container = exec
				.createDataContainer(createTableSpec(table.getSpec()));
		final Map<List<String>, Map<String, Integer>> parts = createParts(
				patternModel.getStringValue(), table.getSpec());
		final Set<Integer> participantColumns = new HashSet<Integer>();
		for (final Entry<List<String>, Map<String, Integer>> entry : parts
				.entrySet()) {
			for (final Integer i : entry.getValue().values()) {
				participantColumns.add(i);
			}
		}
		final Map<RowKey, Set<RowKey>> mapping = createMapping();
		int origRow = 0;
		for (final DataRow dataRow : table) {
			final List<List<DataCell>> newRowContents = new ArrayList<List<DataCell>>(
					parts.size());
			for (int i = parts.size(); i-- > 0;) {
				newRowContents.add(new ArrayList<DataCell>(dataRow
						.getNumCells()));
			}
			for (int i = 0; i < dataRow.getNumCells(); ++i) {
				if (!participantColumns.contains(Integer.valueOf(i))) {
					for (int j = parts.size(); j-- > 0;) {
						newRowContents.get(j).add(dataRow.getCell(i));
					}
				}
			}
			int j = 0;
			for (final Entry<List<String>, Map<String, Integer>> entry : parts
					.entrySet()) {
				for (final String cellValue : entry.getKey()) {
					newRowContents.get(j).add(new StringCell(cellValue));
				}
				for (final Entry<String, Integer> e : entry.getValue()
						.entrySet()) {
					newRowContents.get(j).add(
							dataRow.getCell(e.getValue().intValue()));
				}
				++j;
			}
			j = 0;
			for (final List<DataCell> rowContent : newRowContents) {
				final DefaultRow row = new DefaultRow("Row_"
						+ (origRow * parts.size() + j++), rowContent);
				if (mapping != null) {
					mapping.put(row.getKey(), Collections.singleton(dataRow
							.getKey()));
				}
				container.addRowToTable(row);
			}
			exec.checkCanceled();
			exec.setProgress(origRow++ * 1.0 / table.size());
		}
		// once we are done, we close the container and return its table
		if (mapping != null) {
			setMapping(true, 0, 0, mapping);
		}
		container.close();
		logger.debug("Unpivot finished");
		final BufferedDataTable out = container.getTable();
		return new BufferedDataTable[] { out };
	}

	/**
	 * Creates the parts from the {@link DataTableSpec} ({@code spec}) and the
	 * {@code patternString}.
	 * 
	 * @param patternString
	 *            The pattern.
	 * @param spec
	 *            The {@link DataTableSpec}.
	 * @return A {@link Map} with keys of groups from the pattern, and values
	 *         are maps with key: part of the column name not in a group;
	 *         values: the index of the column.
	 */
	static Map<List<String>, Map<String, Integer>> createParts(
			final String patternString, final DataTableSpec spec) {
		final Pattern pattern = Pattern.compile(patternString);
		final Map<List<String>, Map<String, Integer>> ret = new LinkedHashMap<List<String>, Map<String, Integer>>();
		int i = 0;
		for (final DataColumnSpec colSpec : spec) {
			final Matcher matcher = pattern.matcher(colSpec.getName());
			if (matcher.matches()) {
				final List<String> list = new ArrayList<String>(matcher
						.groupCount());
				final StringBuilder sb = new StringBuilder(colSpec.getName());
				for (int j = matcher.groupCount() + 1; j-- > 1;) {
					list.add(matcher.group(j));
					sb.delete(matcher.start(j), matcher.end(j));
				}
				if (!ret.containsKey(list)) {
					ret.put(list, new LinkedHashMap<String, Integer>());
				}
				if (sb.length() == 0) {
					sb.append(newName(spec));
				}
				ret.get(list).put(sb.toString(), Integer.valueOf(i));
			}
			++i;
		}
		return ret;
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
		try {
			return new DataTableSpec[] { createTableSpec(inSpecs[0]) };
		} catch (final PatternSyntaxException e) {
			throw new InvalidSettingsException(e);
		}
	}

	/**
	 * Creates the {@link DataTableSpec} based on the {@code dataTableSpec}, and
	 * the parameters.
	 * 
	 * @param dataTableSpec
	 *            The original {@link DataTableSpec}.
	 * @return The new {@link DataTableSpec}.
	 */
	private DataTableSpec createTableSpec(final DataTableSpec dataTableSpec) {
		final List<DataColumnSpec> specs = new ArrayList<DataColumnSpec>();
		final Pattern pattern = Pattern.compile(patternModel.getStringValue());
		final String[] newColumnNames = newColumnsModel.getStringArrayValue();
		Matcher m = null;
		final Map<String, DataType> types = new LinkedHashMap<String, DataType>();
		for (final DataColumnSpec dataColumnSpec : dataTableSpec) {
			final Matcher matcher = pattern.matcher(dataColumnSpec.getName());
			if (matcher.matches()) {
				m = matcher;
				final StringBuilder sb = new StringBuilder(dataColumnSpec
						.getName());
				for (int i = matcher.groupCount() + 1; i-- > 1;) {
					sb.delete(matcher.start(i), matcher.end(i));
				}
				if (sb.length() == 0) {
					sb.append(newName(dataTableSpec));
				}
				if (types.containsKey(sb.toString())
						&& !dataColumnSpec.getType().equals(
								types.get(sb.toString()))) {
					types.put(sb.toString(), DataType.getCommonSuperType(types
							.get(sb.toString()), dataColumnSpec.getType()));
				} else {
					types.put(sb.toString(), dataColumnSpec.getType());
				}
			} else {
				specs.add(dataColumnSpec);
			}
		}
		if (m != null) {
			for (int i = 0; i < m.groupCount(); ++i) {
				final DataColumnSpecCreator creator = new DataColumnSpecCreator(
						newColumnNames[i], StringCell.TYPE);
				specs.add(creator.createSpec());
			}
			for (final Entry<String, DataType> entry : types.entrySet()) {
				specs.add(new DataColumnSpecCreator(entry.getKey(), entry
						.getValue()).createSpec());
			}
		}
		return new DataTableSpec(specs
				.toArray(new DataColumnSpec[specs.size()]));
	}

	/**
	 * @param dataTableSpec
	 *            The original {@link DataTableSpec}.
	 * @return New name for the table column.
	 */
	private static String newName(final DataTableSpec dataTableSpec) {
		int i = 0;
		while (dataTableSpec.containsName("<empty" + i + ">")) {
			++i;
		}
		return "<empty" + i + ">";
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveSettingsTo(final NodeSettingsWO settings) {
		super.saveSettingsTo(settings);
		patternModel.saveSettingsTo(settings);
		newColumnsModel.saveSettingsTo(settings);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
			throws InvalidSettingsException {
		super.loadValidatedSettingsFrom(settings);
		patternModel.loadSettingsFrom(settings);
		newColumnsModel.loadSettingsFrom(settings);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void validateSettings(final NodeSettingsRO settings)
			throws InvalidSettingsException {
		super.validateSettings(settings);
		patternModel.validateSettings(settings);
		newColumnsModel.validateSettings(settings);
	}
}

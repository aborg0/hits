/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package ie.tcd.imm.hits.knime.biomart;

import ie.tcd.imm.hits.util.RUtil;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.container.AbstractCellFactory;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
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
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.defaultnodesettings.SettingsModelStringArray;
import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPInteger;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.REXPString;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;

/**
 * This is the model implementation of BiomartAnnotator. Adds some annotations
 * from the BioMart databases using the biomaRt R package.
 * 
 * @author bakosg@tcd.ie
 */
public class BiomartAnnotatorNodeModel extends NodeModel {
	private static final NodeLogger logger = NodeLogger
			.getLogger(BiomartAnnotatorNodeModel.class);
	/** Configuration key for defaults from eclipse. */
	static final String CFGKEY_PROXY_FROM_ECLIPSE = "ie.tcd.imm.hits.knime.proxy.eclipse";
	/** Default value for defaults from eclipse. */
	static final boolean DEFAULT_PROXY_FROM_ECLIPSE = true;
	/** Configuration key for proxy host. */
	static final String CFGKEY_PROXY_HOST = "ie.tcd.imm.hits.knime.proxy.host";
	/** Configuration key for proxy host. */
	static final String DEFAULT_PROXY_HOST = "";
	/** Configuration key for proxy port. */
	static final String CFGKEY_PROXY_PORT = "ie.tcd.imm.hits.knime.proxy.port";
	/** Default value for proxy port. */
	static final int DEFAULT_PROXY_PORT = -1;
	/** Configuration key for proxy user. */
	static final String CFGKEY_PROXY_USER = "ie.tcd.imm.hits.knime.proxy.user";
	/** Default value for proxy user. */
	static final String DEFAULT_PROXY_USER = "";
	/** Configuration key for proxy password. */
	static final String CFGKEY_PROXY_PASSWORD = "ie.tcd.imm.hits.knime.proxy.password";
	/** Default value for proxy password. */
	static final String DEFAULT_PROXY_PASSWORD = "";
	/** Configuration key for biomaRt database */
	static final String CFGKEY_BIOMART_DATABASE = "ie.tcd.imm.hits.knime.biomart.database";
	/** Default value for biomaRt database */
	static final String DEFAULT_BIOMART_DATABASE = "ensemble";
	/** Configuration key for biomaRt dataset */
	static final String CFGKEY_BIOMART_DATASET = "ie.tcd.imm.hits.knime.biomart.dataset";
	/** Default value for biomaRt dataset */
	static final String DEFAULT_BIOMART_DATASET = "hsapiens_gene_ensembl";
	/** Configuration key for biomaRt attributes */
	static final String CFGKEY_BIOMART_ATTRIBUTES = "ie.tcd.imm.hits.knime.biomart.attributes";
	/** Default value for biomaRt attributes */
	static final String[] DEFAULT_BIOMART_ATTRIBUTES = new String[] { "" };
	private final SettingsModelString biomartDatabaseModel = new SettingsModelString(
			CFGKEY_BIOMART_DATABASE, DEFAULT_BIOMART_DATABASE);
	private final SettingsModelString biomartDatasetModel = new SettingsModelString(
			CFGKEY_BIOMART_DATASET, DEFAULT_BIOMART_DATASET);
	private final SettingsModelStringArray biomartAttributesModel = new SettingsModelStringArray(
			CFGKEY_BIOMART_ATTRIBUTES, DEFAULT_BIOMART_ATTRIBUTES);
	private final SettingsModelBoolean proxyFromEclipseModel = new SettingsModelBoolean(
			CFGKEY_PROXY_FROM_ECLIPSE, DEFAULT_PROXY_FROM_ECLIPSE);
	private final SettingsModelString proxyHost = new SettingsModelString(
			CFGKEY_PROXY_HOST, DEFAULT_PROXY_HOST);
	private final SettingsModelIntegerBounded proxyPort = new SettingsModelIntegerBounded(
			CFGKEY_PROXY_PORT, DEFAULT_PROXY_PORT, -1, 65535);
	private final SettingsModelString proxyUser = new SettingsModelString(
			CFGKEY_PROXY_USER, DEFAULT_PROXY_USER);
	private final SettingsModelString proxyPassword = new SettingsModelString(
			CFGKEY_PROXY_PASSWORD, DEFAULT_PROXY_PASSWORD);

	/**
	 * Constructor for the node model.
	 */
	protected BiomartAnnotatorNodeModel() {
		super(1, 1);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
			final ExecutionContext exec) throws Exception {

		final int index = findColumn(inData[0], "gene id");
		final StringBuilder ids = collectIds(inData[0], index);
		final RConnection conn;
		try {
			conn = new RConnection(/*
									 * "127.0.0.1", 1099, 10000
									 */);
		} catch (final RserveException e) {
			logger.fatal("Failed to connect to Rserve, please start again.", e);
			throw e;
		}
		try {
			final REXP vals;
			try {
				RUtil.voidEval(conn, "library(\"biomaRt\")");
				exec.checkCanceled();
				setProxy(conn);
				exec.checkCanceled();
				conn.voidEval("mart <- useMart(\""
						+ biomartDatabaseModel.getStringValue()
						+ "\", dataset =\""
						+ biomartDatasetModel.getStringValue() + "\")");
				exec.checkCanceled();
				final StringBuilder attributes = new StringBuilder(
						"'entrezgene', ");
				for (final String attribute : biomartAttributesModel
						.getStringArrayValue()) {
					attributes.append('\'').append(attribute).append('\'')
							.append(", ");
				}
				attributes.setLength(attributes.length() - 2);
				conn
						.voidEval(" myGetBM = function(att) getBM(attributes = c(att), filter = 'entrezgene', values = unique(c("
								+ ids + ")),\n" + " mart = mart)");
				exec.checkCanceled();
				vals = RUtil.eval(conn, "myGetBM(c(" + attributes + "))");
				exec.checkCanceled();
			} finally {
				conn.close();
			}
			final Map<Integer, String[]> newValues = new HashMap<Integer, String[]>();
			final int[] keys = ((REXPInteger) vals.asList().get("entrezgene"))
					.asIntegers();
			final String[] newAttributes = biomartAttributesModel
					.getStringArrayValue();
			collectNewValues(vals, newValues, keys, newAttributes);
			final DataCell emptyCell = DataType.getMissingCell();
			final ColumnRearranger rearranger = new ColumnRearranger(inData[0]
					.getDataTableSpec());
			final int geneIdIndex = index;
			rearranger.append(new AbstractCellFactory(createNewColSpecs(
					inData[0].getDataTableSpec(), newAttributes)) {
				@Override
				public DataCell[] getCells(final DataRow row) /* => */{
					final List<DataCell> values = new ArrayList<DataCell>();
					final int value = getValue(row.getCell(geneIdIndex));
					if (value == -1
							|| !newValues.containsKey(Integer.valueOf(value))) {
						for (int i = newAttributes.length; i-- > 0;) {
							values.add(emptyCell);
						}
					} else {
						final String[] strings = newValues.get(Integer
								.valueOf(value));
						for (int i = 0; i < newAttributes.length; ++i) {
							values.add(new StringCell(strings[i] == null ? ""
									: strings[i]));
						}
					}
					return values.toArray(new DataCell[newAttributes.length]);
				}
			});
			final BufferedDataTable rearrangeTable = exec
					.createColumnRearrangeTable(inData[0], rearranger, exec);
			return new BufferedDataTable[] { rearrangeTable };
		} catch (final RuntimeException e) {
			logger.warn("Unable to use biomaRt.");
			logger.debug("Unable to use biomaRt.", e);
			throw e;
		}
	}

	/**
	 * Fills {@code newValues} with the results from {@code vals}.
	 * 
	 * @param vals
	 *            The Rserve expression of results.
	 * @param newValues
	 *            The {@link Map} of new values.
	 * @param keys
	 *            The geneIds.
	 * @param newAttributes
	 *            The new attributes column names.
	 * @throws REXPMismatchException
	 *             Error parsing the error message.
	 */
	private void collectNewValues(final REXP vals,
			final Map<Integer, String[]> newValues, final int[] keys,
			final String[] newAttributes) throws REXPMismatchException {
		final Map<String, String[]> rawAnnots = new HashMap<String, String[]>();
		for (final String string : newAttributes) {
			rawAnnots.put(string, ((REXPString) vals.asList().get(string))
					.asStrings());
		}
		for (int i = 0; i < keys.length; ++i) {
			final Integer key = Integer.valueOf(keys[i]);
			if (!newValues.containsKey(key)) {
				newValues.put(key, new String[newAttributes.length]);
			}
			final String[] origVal = newValues.get(key);
			for (int j = 0; j < newAttributes.length; j++) {
				final StringBuilder sb = new StringBuilder(
						origVal[j] == null ? "" : origVal[j] + "|");
				final String toAppend = rawAnnots.get(newAttributes[j])[i];
				if (sb.length() > 0
						&& (sb.substring(0, sb.length() - 1).equals(toAppend) || sb
								.substring(
										sb.substring(0, sb.length() - 1)
												.lastIndexOf("|") + 1,
										sb.length() - 1).equals(toAppend))) {
					sb.setLength(sb.length() - 1);
				} else {
					sb.append(toAppend);
				}
				origVal[j] = sb.toString();
			}
		}
	}

	/**
	 * Sets the proxy based on the settings.
	 * 
	 * @param conn
	 *            An {@link RConnection}.
	 * @throws RserveException
	 *             Problem setting the proxy.
	 * @throws REXPMismatchException
	 *             Problem parsing the error message.
	 */
	private void setProxy(final RConnection conn) throws RserveException,
			REXPMismatchException {
		if (!proxyHost.getStringValue().isEmpty()) {
			// http://username:password@proxy.server:8080
			final StringBuilder proxyString = new StringBuilder("http://");
			if (!proxyUser.getStringValue().isEmpty()) {
				proxyString.append(proxyUser.getStringValue());
				if (!proxyPassword.getStringValue().isEmpty()) {
					proxyString.append(':').append(
							proxyPassword.getStringValue());
				}
				proxyString.append("@");
			}
			proxyString.append(proxyHost.getStringValue()).append(':').append(
					proxyPort.getIntValue());
			RUtil.voidEval(conn, "Sys.setenv(\"http_proxy\" = \"" + proxyString
					+ "\")");
		}
	}

	/**
	 * Collects the gene ids from {@code inData}.
	 * 
	 * @param inData
	 *            The {@link BufferedDataTable}.
	 * @param index
	 *            The index of the gene id column.
	 * @return The collected (comma separated) ids in a {@link StringBuilder}.
	 */
	private StringBuilder collectIds(final BufferedDataTable inData,
			final int index) {
		final StringBuilder ids = new StringBuilder();
		for (final DataRow row : inData) {
			final DataCell dataCell = row.getCell(index);
			final int value = getValue(dataCell);
			if (value != -1) {
				ids.append(value).append(", ");
			}
		}
		if (ids.length() > 1) {
			ids.setLength(ids.length() - 2);
		} else {
			throw new IllegalStateException(
					"No (numeric) gene ids present. Execution aborted.");
		}
		return ids;
	}

	/**
	 * Finds the {@code colName} (case insensitive) column in the data table.
	 * 
	 * @param inData
	 *            A {@link BufferedDataTable}.
	 * @param colName
	 *            The name of the column.
	 * @return The index of the column.
	 */
	private static int findColumn(final BufferedDataTable inData,
			final String colName) {
		int index = -1;
		for (int i = 0; i < inData.getDataTableSpec().getNumColumns(); ++i) {
			if (inData.getDataTableSpec().getColumnSpec(i).getName()
					.equalsIgnoreCase(colName)) {
				index = i;
				break;
			}
		}
		return index;
	}

	private int getValue(final DataCell dataCell) {
		int value = -1;
		if (dataCell instanceof StringCell) {
			final StringCell strCell = (StringCell) dataCell;
			if (!strCell.getStringValue().isEmpty()) {
				try {
					value = Integer.parseInt(strCell.getStringValue());
				} catch (final RuntimeException e) {
					// if (errorCount < 4) {
					logger.debug("Wrong number: " + strCell.getStringValue());
					// }
					// ++errorCount;
				}
			}
		} else if (dataCell instanceof IntCell) {
			final IntCell intCell = (IntCell) dataCell;
			value = intCell.getIntValue();
		} else if (!dataCell.isMissing()) {
			throw new IllegalArgumentException("Wrong type of geneID: "
					+ dataCell.getClass());
		}
		return value;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void reset() {
		// Do nothing
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
			throws InvalidSettingsException {
		boolean foundGeneId = false;
		for (final DataColumnSpec spec : inSpecs[0]) {
			if (spec.getName().equalsIgnoreCase("gene id")) {
				foundGeneId = true;
				break;
			}
		}
		if (!foundGeneId) {
			throw new InvalidSettingsException("No geneID found.\n"
					+ inSpecs[0]);
		}
		final String[] newAttributes = biomartAttributesModel
				.getStringArrayValue();
		final DataColumnSpec[] newColSpecs = createNewColSpecs(inSpecs[0],
				newAttributes);
		final DataColumnSpec[] newCols = new DataColumnSpec[inSpecs[0]
				.getNumColumns()
				+ newAttributes.length];
		for (int i = inSpecs[0].getNumColumns(); i-- > 0;) {
			newCols[i] = inSpecs[0].getColumnSpec(i);
		}
		System.arraycopy(newColSpecs, 0, newCols, inSpecs[0].getNumColumns(),
				newAttributes.length);
		// newCols[inSpecs[0].getNumColumns() + i]
		return new DataTableSpec[] { new DataTableSpec(newCols) };
	}

	/**
	 * @param inSpec
	 *            The original {@link DataTableSpec}.
	 * @param newAttributes
	 *            The name of the new columns.
	 * @return The new {@link DataColumnSpec}s with {@link StringCell} types.
	 */
	private DataColumnSpec[] createNewColSpecs(final DataTableSpec inSpec,
			final String[] newAttributes) {
		final DataColumnSpec[] newColSpecs = new DataColumnSpec[newAttributes.length];
		for (int i = newAttributes.length; i-- > 0;) {
			newColSpecs[i] = new DataColumnSpecCreator(findNewName(inSpec,
					newAttributes[i], 0), StringCell.TYPE).createSpec();
		}
		return newColSpecs;
	}

	private static final Pattern numberEnding = Pattern.compile("(.+?)[0-9]+$");

	private static String findNewName(final DataTableSpec dataTableSpec,
			final String string, final int i) {
		if (!dataTableSpec.containsName(string)) {
			return string;
		}
		final Matcher matcher = numberEnding.matcher(string);
		if (matcher.matches()) {
			if (!dataTableSpec.containsName(matcher.group(1) + i)) {
				return matcher.group(1) + i;
			}
			return findNewName(dataTableSpec, string, i + 1);
		}
		return findNewName(dataTableSpec, string + "_", i);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveSettingsTo(final NodeSettingsWO settings) {
		biomartDatabaseModel.saveSettingsTo(settings);
		biomartDatasetModel.saveSettingsTo(settings);
		biomartAttributesModel.saveSettingsTo(settings);
		proxyFromEclipseModel.saveSettingsTo(settings);
		proxyHost.saveSettingsTo(settings);
		proxyPort.saveSettingsTo(settings);
		proxyUser.saveSettingsTo(settings);
		proxyPassword.saveSettingsTo(settings);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
			throws InvalidSettingsException {
		biomartDatabaseModel.loadSettingsFrom(settings);
		biomartDatasetModel.loadSettingsFrom(settings);
		biomartAttributesModel.loadSettingsFrom(settings);
		proxyFromEclipseModel.loadSettingsFrom(settings);
		proxyHost.loadSettingsFrom(settings);
		proxyPort.loadSettingsFrom(settings);
		proxyUser.loadSettingsFrom(settings);
		proxyPassword.loadSettingsFrom(settings);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void validateSettings(final NodeSettingsRO settings)
			throws InvalidSettingsException {
		biomartDatabaseModel.validateSettings(settings);
		biomartDatasetModel.validateSettings(settings);
		biomartAttributesModel.validateSettings(settings);
		proxyFromEclipseModel.validateSettings(settings);
		proxyHost.validateSettings(settings);
		proxyPort.validateSettings(settings);
		proxyUser.validateSettings(settings);
		proxyPassword.validateSettings(settings);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadInternals(final File internDir,
			final ExecutionMonitor exec) throws IOException,
			CanceledExecutionException {
		// Do nothing, no internal state
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveInternals(final File internDir,
			final ExecutionMonitor exec) throws IOException,
			CanceledExecutionException {
		// Do nothing, no internal state
	}

}

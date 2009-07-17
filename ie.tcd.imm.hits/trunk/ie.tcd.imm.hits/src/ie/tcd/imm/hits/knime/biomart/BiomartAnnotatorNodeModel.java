/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package ie.tcd.imm.hits.knime.biomart;

import ie.tcd.imm.hits.common.Format;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.io.File;
import java.io.IOException;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
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
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.defaultnodesettings.SettingsModelStringArray;
import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPInteger;
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

		final RConnection conn;
		try {
			conn = new RConnection(/*
									 * "127.0.0.1", 1099, 10000
									 */);
		} catch (final RserveException e) {
			logger.fatal("Failed to connect to Rserve, please start again.", e);
			throw e;
		}
		final BufferedDataContainer table = exec
				.createDataContainer(configure(new DataTableSpec[] { inData[0]
						.getDataTableSpec() })[0]);
		try {
			table.setMaxPossibleValues(Format._1536.getWellCount() + 1);
			conn.voidEval("library(\"biomaRt\")");
			conn.voidEval("mart <- useMart(\""
					+ biomartDatabaseModel.getStringValue() + "\", dataset =\""
					+ biomartDatasetModel.getStringValue() + "\")");
			// conn.voidEval("attrs <- listAttributes(mart)");
			// conn.voidEval("filts <- listFilters(mart)");
			final StringBuilder attributes = new StringBuilder(
					"\"entrezgene\", ");
			for (final String attribute : biomartAttributesModel
					.getStringArrayValue()) {
				attributes.append('"').append(attribute).append('"').append(
						", ");
			}
			attributes.setLength(attributes.length() - 2);
			int index = -1;
			for (int i = 0; i < inData[0].getDataTableSpec().getNumColumns(); ++i) {
				if (inData[0].getDataTableSpec().getColumnSpec(i).getName()
						.equalsIgnoreCase("gene id")) {
					index = i;
					break;
				}
			}
			final StringBuilder ids = new StringBuilder();
			final int errorCount = 0;
			for (final DataRow row : inData[0]) {
				final DataCell dataCell = row.getCell(index);
				final int value = getValue(dataCell);
				if (value != -1) {
					ids.append(value).append(", ");
				}
			}
			if (errorCount > 0) {
				logger.warn("There were " + errorCount
						+ " wrong values in the "
						+ inData[0].getDataTableSpec().getColumnSpec(index)
						+ " column.");
			}
			if (ids.length() > 1) {
				ids.setLength(ids.length() - 2);
			}
			conn
					.voidEval(" myGetBM = function(att) getBM(attributes = c(att), filter = \"entrezgene\", values = unique(c("
							+ ids + ")),\n" + " mart = mart)");
			final REXP vals = conn.eval("myGetBM(c(" + attributes + "))");
			final Map<Integer, String[]> newValues = new HashMap<Integer, String[]>();
			final int[] keys = ((REXPInteger) vals.asList().get("entrezgene"))
					.asIntegers();
			final String[] newAttributes = biomartAttributesModel
					.getStringArrayValue();
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
							&& (sb.substring(0, sb.length() - 1).equals(
									toAppend) || sb.substring(
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
			// conn.voidEval("bm2 <- myGetBM(c(\"flybasename_gene\"))");
			// conn.voidEval("bm2 <- myGetBM(c(\"Validation
			// status\"))");
			// conn.voidEval("bm3 = myGetBM(c(\"go\",
			// \"go_description\"))");
			// conn.voidEval("id <- geneAnno(xn)");
			// conn
			// .voidEval("bmAll <- cbind(oneRowPerId(bm1, id),
			// oneRowPerId(bm2,
			// id), oneRowPerId(bm3,\n"
			// + " id))");
			// conn.voidEval("bmAll <- cbind(oneRowPerId(bm1, id))");
			// conn.voidEval("bdgpbiomart <- cbind(fData(xn), bmAll)");
			// conn.voidEval("fData(xn) <- bdgpbiomart");
			// conn
			// .voidEval("fvarMetadata(xn)[names(bmAll),\"labelDescription\"] <-
			// sapply(names(bmAll),\n"
			// + " function(i) sub(\"_\", \" \", i))");
			final StringCell emptyCell = new StringCell("");
			for (final DataRow origRow : inData[0]) {
				final List<DataCell> values = new ArrayList<DataCell>(origRow
						.getNumCells()
						+ newAttributes.length);
				for (final DataCell cell : origRow) {
					values.add(cell);
				}
				final int value = getValue(origRow.getCell(index));
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
				table.addRowToTable(new DefaultRow(origRow.getKey(), values));
			}
		} catch (final RuntimeException e) {
			logger.warn("Unable to use biomaRt.");
			logger.debug("Unable to use biomaRt.", e);
			throw e;
		}

		table.close();
		return new BufferedDataTable[] { table.getTable() };
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
					logger.warn("Wrong number: " + strCell.getStringValue(), e);
					// }
					// ++errorCount;
				}
			}
		} else if (dataCell instanceof IntCell) {
			final IntCell intCell = (IntCell) dataCell;
			value = intCell.getIntValue();
		} else {
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
		// TODO: generated method stub
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
		final DataColumnSpec[] newCols = new DataColumnSpec[inSpecs[0]
				.getNumColumns()
				+ newAttributes.length];
		for (int i = inSpecs[0].getNumColumns(); i-- > 0;) {
			newCols[i] = inSpecs[0].getColumnSpec(i);
		}
		for (int i = newAttributes.length; i-- > 0;) {
			newCols[inSpecs[0].getNumColumns() + i] = new DataColumnSpecCreator(
					findNewName(inSpecs[0], newAttributes[i], 0),
					StringCell.TYPE).createSpec();
		}
		return new DataTableSpec[] { new DataTableSpec(newCols) };
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
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadInternals(final File internDir,
			final ExecutionMonitor exec) throws IOException,
			CanceledExecutionException {
		// TODO: generated method stub
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveInternals(final File internDir,
			final ExecutionMonitor exec) throws IOException,
			CanceledExecutionException {
		// TODO: generated method stub
	}

}

package ie.tcd.imm.hits.image.loci.view;

import ie.tcd.imm.hits.image.loci.OMEReaderCell;
import ie.tcd.imm.hits.util.Pair;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import loci.formats.FormatReader;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.IntValue;
import org.knime.core.data.StringValue;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * This is the model implementation of OMEViewer. Shows images based on OME.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
public class LociViewerNodeModel extends NodeModel {

	/** Plate, row, column, field, image id (series), OME data */
	private Map<String, Map<String, Map<Integer, Map<Integer, Map<Integer, FormatReader>>>>> joinTable;

	/**
	 * Constructor for the node model.
	 */
	protected LociViewerNodeModel() {
		super(2, 0);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
			final ExecutionContext exec) throws Exception {
		joinTable = new HashMap<String, Map<String, Map<Integer, Map<Integer, Map<Integer, FormatReader>>>>>();

		final Map<String, Map<String, Map<Integer, Map<Integer, Pair<FormatReader, String>>>>> xmls = new HashMap<String, Map<String, Map<Integer, Map<Integer, Pair<FormatReader, String>>>>>();
		for (final DataRow row : inData[1]) {
			final DataCell plateCell = row.getCell(0);
			final String plate;
			if (plateCell instanceof StringValue) {
				plate = ((StringValue) plateCell).getStringValue();
			} else {
				plate = null;
			}
			if (!xmls.containsKey(plate)) {
				xmls
						.put(
								plate,
								new HashMap<String, Map<Integer, Map<Integer, Pair<FormatReader, String>>>>());
			}
			final Map<String, Map<Integer, Map<Integer, Pair<FormatReader, String>>>> inner0 = xmls
					.get(plate);
			final String rowValue;
			final DataCell rowCell = row.getCell(1);
			if (rowCell instanceof StringValue) {
				rowValue = ((StringValue) rowCell).getStringValue();
			} else {
				rowValue = null;
			}
			if (!inner0.containsKey(rowValue)) {
				inner0
						.put(
								rowValue,
								new HashMap<Integer, Map<Integer, Pair<FormatReader, String>>>());
			}
			final Map<Integer, Map<Integer, Pair<FormatReader, String>>> inner1 = inner0
					.get(rowValue);
			final Integer column;
			final DataCell columnCell = row.getCell(2);
			if (columnCell instanceof IntValue) {
				column = Integer.valueOf(((IntValue) columnCell).getIntValue());
			} else {
				column = null;
			}
			if (!inner1.containsKey(column)) {
				inner1.put(column,
						new HashMap<Integer, Pair<FormatReader, String>>());
			}
			final Map<Integer, Pair<FormatReader, String>> inner2 = inner1
					.get(column);
			final Integer field;
			final DataCell fieldCell = row.getCell(3);
			if (fieldCell instanceof IntValue) {
				field = Integer.valueOf(((IntValue) fieldCell).getIntValue());
			} else {
				field = null;
			}
			inner2.put(field, Pair.apply(/*
										 * .MetadataTools .createOMEXMLMetadata(
										 */((OMEReaderCell) row.getCell(4))
					.getReader()/* ) */, ((StringValue) row.getCell(5))
					.getStringValue()));
		}

		for (final DataRow row : inData[0]) {
			final String plate;
			final DataCell plateCell = row.getCell(0);
			if (plateCell instanceof StringValue) {
				plate = ((StringValue) plateCell).getStringValue();
			} else {
				throw new IllegalStateException("wrong value: " + plateCell);
			}
			if (!joinTable.containsKey(plate)) {
				joinTable
						.put(
								plate,
								new HashMap<String, Map<Integer, Map<Integer, Map<Integer, FormatReader>>>>());
			}
			final Map<String, Map<Integer, Map<Integer, Pair<FormatReader, String>>>> inner0 = xmls
					.get(xmls.containsKey(plate) ? plate : null);
			final Map<String, Map<Integer, Map<Integer, Map<Integer, FormatReader>>>> other0 = joinTable
					.get(plate);
			final String rowValue;
			final DataCell rowCell = row.getCell(1);
			if (rowCell instanceof StringValue) {
				rowValue = ((StringValue) rowCell).getStringValue();
			} else {
				throw new IllegalStateException("wrong value: " + rowCell);
			}
			if (!other0.containsKey(rowValue)) {
				other0
						.put(
								rowValue,
								new HashMap<Integer, Map<Integer, Map<Integer, FormatReader>>>());
			}
			final Map<Integer, Map<Integer, Pair<FormatReader, String>>> inner1 = inner0
					.get(inner0.containsKey(rowValue) ? rowValue : null);
			final Map<Integer, Map<Integer, Map<Integer, FormatReader>>> other1 = other0
					.get(rowValue);
			final Integer column;
			final DataCell columnCell = row.getCell(2);
			if (columnCell instanceof IntValue) {
				column = Integer.valueOf(((IntValue) columnCell).getIntValue());
			} else {
				throw new IllegalStateException("wrong value: " + columnCell);
			}
			if (!other1.containsKey(column)) {
				other1.put(column,
						new HashMap<Integer, Map<Integer, FormatReader>>());
			}
			final Map<Integer, Pair<FormatReader, String>> inner2 = inner1
					.get(inner1.containsKey(column) ? column : null);
			final Map<Integer, Map<Integer, FormatReader>> other2 = other1
					.get(column);
			final Integer field;
			final DataCell fieldCell = row.getCell(3);
			if (fieldCell instanceof IntValue) {
				field = Integer.valueOf(((IntValue) fieldCell).getIntValue());
			} else {
				throw new IllegalStateException("wrong value: " + fieldCell);
			}
			final Pair<FormatReader, String> pair = inner2.get(inner2
					.containsKey(field) ? field : null);
			if (!other2.containsKey(field)) {
				other2.put(field, new HashMap<Integer, FormatReader>());
			}
			final Map<Integer, FormatReader> other3 = other2.get(field);

			final DataCell omeIdCell = row.getCell(4);
			final String omeId = ((StringValue) omeIdCell).getStringValue();
			if (!omeId.equals(pair.getRight())) {
				throw new IllegalStateException("Not matching ids: " + omeId
						+ " <-> " + pair.getRight());
			}
			other3.put(Integer.valueOf(((IntValue) row.getCell(5))
					.getIntValue()), pair.getLeft());
		}
		return new BufferedDataTable[] {};
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
		return new DataTableSpec[] {};
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveSettingsTo(final NodeSettingsWO settings) {
		// TODO: generated method stub
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
			throws InvalidSettingsException {
		// TODO: generated method stub
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void validateSettings(final NodeSettingsRO settings)
			throws InvalidSettingsException {
		// TODO: generated method stub
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

	public Map<String, Map<String, Map<Integer, Map<Integer, Map<Integer, FormatReader>>>>> getJoinTable() {
		return joinTable;
	}

}

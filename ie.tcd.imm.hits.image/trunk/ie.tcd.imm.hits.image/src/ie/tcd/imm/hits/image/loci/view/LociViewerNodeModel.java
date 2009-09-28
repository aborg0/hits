package ie.tcd.imm.hits.image.loci.view;

import ie.tcd.imm.hits.common.PublicConstants;
import ie.tcd.imm.hits.image.loci.LociReaderCell;
import ie.tcd.imm.hits.util.Pair;
import ie.tcd.imm.hits.util.SerializableTriple;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import loci.formats.FormatReader;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.IntValue;
import org.knime.core.data.RowKey;
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
	private static final String JOIN_TABLE_FILE = "join.zip";
	private static final String ROW_TABLE_FILE = "rows.zip";

	/** Plate, row, column, field, image id (series), LOCI data */
	private Map<String, Map<String, Map<Integer, Map<Integer, Map<Integer, FormatReader>>>>> joinTable;

	private Map<RowKey, SerializableTriple<String, String, Integer>> rowsToWells = new HashMap<RowKey, SerializableTriple<String, String, Integer>>();

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

		final int plate0Index = inData[0].getDataTableSpec().findColumnIndex(
				PublicConstants.LOCI_PLATE);
		final int row0Index = inData[0].getDataTableSpec().findColumnIndex(
				PublicConstants.LOCI_ROW);
		final int col0Index = inData[0].getDataTableSpec().findColumnIndex(
				PublicConstants.LOCI_COLUMN);
		final int field0Index = inData[0].getDataTableSpec().findColumnIndex(
				PublicConstants.LOCI_FIELD);
		final int id0Index = inData[0].getDataTableSpec().findColumnIndex(
				PublicConstants.LOCI_ID);
		final int imageId0Index = inData[0].getDataTableSpec().findColumnIndex(
				PublicConstants.IMAGE_ID);
		final int plate1Index = inData[1].getDataTableSpec().findColumnIndex(
				PublicConstants.LOCI_PLATE);
		final int row1Index = inData[1].getDataTableSpec().findColumnIndex(
				PublicConstants.LOCI_ROW);
		final int col1Index = inData[1].getDataTableSpec().findColumnIndex(
				PublicConstants.LOCI_COLUMN);
		final int field1Index = inData[1].getDataTableSpec().findColumnIndex(
				PublicConstants.LOCI_FIELD);
		final int content1Index = inData[1].getDataTableSpec().findColumnIndex(
				PublicConstants.LOCI_IMAGE_CONTENT);
		final int id1Index = inData[1].getDataTableSpec().findColumnIndex(
				PublicConstants.LOCI_ID);
		final Map<String, Map<String, Map<Integer, Map<Integer, Pair<FormatReader, String>>>>> xmls = new LinkedHashMap<String, Map<String, Map<Integer, Map<Integer, Pair<FormatReader, String>>>>>();
		for (final DataRow row : inData[1]) {
			final DataCell plateCell = row.getCell(plate1Index);
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
								new LinkedHashMap<String, Map<Integer, Map<Integer, Pair<FormatReader, String>>>>());
			}
			final Map<String, Map<Integer, Map<Integer, Pair<FormatReader, String>>>> inner0 = xmls
					.get(plate);
			final String rowValue;
			final DataCell rowCell = row.getCell(row1Index);
			if (rowCell instanceof StringValue) {
				rowValue = ((StringValue) rowCell).getStringValue();
			} else {
				rowValue = null;
			}
			if (!inner0.containsKey(rowValue)) {
				inner0
						.put(
								rowValue,
								new LinkedHashMap<Integer, Map<Integer, Pair<FormatReader, String>>>());
			}
			final Map<Integer, Map<Integer, Pair<FormatReader, String>>> inner1 = inner0
					.get(rowValue);
			final Integer column;
			final DataCell columnCell = row.getCell(col1Index);
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
			final DataCell fieldCell = row.getCell(field1Index);
			if (fieldCell instanceof IntValue) {
				field = Integer.valueOf(((IntValue) fieldCell).getIntValue());
			} else {
				field = null;
			}
			inner2.put(field, Pair.apply(/*
										 * .MetadataTools .createOMEXMLMetadata(
										 */((LociReaderCell) row
					.getCell(content1Index)).getReader()/* ) */,
					((StringValue) row.getCell(id1Index)).getStringValue()));
		}

		for (final DataRow row : inData[0]) {
			final String plate;
			final DataCell plateCell = row.getCell(plate0Index);
			if (plateCell instanceof StringValue) {
				plate = ((StringValue) plateCell).getStringValue();
			} else {
				throw new IllegalStateException("wrong value: " + plateCell);
			}
			if (!joinTable.containsKey(plate)) {
				joinTable
						.put(
								plate,
								new TreeMap<String, Map<Integer, Map<Integer, Map<Integer, FormatReader>>>>());
			}
			final Map<String, Map<Integer, Map<Integer, Pair<FormatReader, String>>>> inner0 = xmls
					.get(xmls.containsKey(plate) ? plate : null);
			final Map<String, Map<Integer, Map<Integer, Map<Integer, FormatReader>>>> other0 = joinTable
					.get(plate);
			final String rowValue;
			final DataCell rowCell = row.getCell(row0Index);
			if (rowCell instanceof StringValue) {
				rowValue = ((StringValue) rowCell).getStringValue();
			} else {
				throw new IllegalStateException("wrong value: " + rowCell);
			}
			if (!other0.containsKey(rowValue)) {
				other0
						.put(
								rowValue,
								new LinkedHashMap<Integer, Map<Integer, Map<Integer, FormatReader>>>());
			}
			final Map<Integer, Map<Integer, Pair<FormatReader, String>>> inner1 = inner0
					.get(inner0.containsKey(rowValue) ? rowValue : null);
			final Map<Integer, Map<Integer, Map<Integer, FormatReader>>> other1 = other0
					.get(rowValue);
			final Integer column;
			final DataCell columnCell = row.getCell(col0Index);
			if (columnCell instanceof IntValue) {
				column = Integer.valueOf(((IntValue) columnCell).getIntValue());
			} else {
				throw new IllegalStateException("wrong value: " + columnCell);
			}
			if (!other1.containsKey(column)) {
				other1
						.put(
								column,
								new LinkedHashMap<Integer, Map<Integer, FormatReader>>());
			}
			final Map<Integer, Pair<FormatReader, String>> inner2 = inner1
					.get(inner1.containsKey(column) ? column : null);
			final Map<Integer, Map<Integer, FormatReader>> other2 = other1
					.get(column);
			rowsToWells.put(row.getKey(), SerializableTriple.apply(plate,
					rowValue, column));
			final Integer field;
			final DataCell fieldCell = row.getCell(field0Index);
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

			final DataCell omeIdCell = row.getCell(id0Index);
			final String omeId = ((StringValue) omeIdCell).getStringValue();
			if (!omeId.equals(pair.getRight())) {
				throw new IllegalStateException("Not matching ids: " + omeId
						+ " <-> " + pair.getRight());
			}
			other3.put(Integer.valueOf(((IntValue) row.getCell(imageId0Index))
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
	@SuppressWarnings("unchecked")
	@Override
	protected void loadInternals(final File internDir,
			final ExecutionMonitor exec) throws IOException,
			CanceledExecutionException {
		{
			final File joinTableFile = new File(internDir, JOIN_TABLE_FILE);
			final FileInputStream fis = new FileInputStream(joinTableFile);
			try {
				final GZIPInputStream zis = new GZIPInputStream(fis);
				try {
					final ObjectInputStream oos = new ObjectInputStream(zis);
					try {
						final Object readObject = oos.readObject();
						if (readObject instanceof Map<?, ?>) {
							final Map<?, ?> newMap = (Map<?, ?>) readObject;
							joinTable = (Map<String, Map<String, Map<Integer, Map<Integer, Map<Integer, FormatReader>>>>>) newMap;
						}
					} catch (final ClassNotFoundException e) {
						throw new IOException(e);
					} finally {
						oos.close();
					}
				} finally {
					zis.close();
				}
			} finally {
				fis.close();
			}
		}
		{
			final File rowTableFile = new File(internDir, ROW_TABLE_FILE);
			final FileInputStream fis = new FileInputStream(rowTableFile);
			try {
				final GZIPInputStream zis = new GZIPInputStream(fis);
				try {
					final ObjectInputStream oos = new ObjectInputStream(zis);
					try {
						final Object readObject = oos.readObject();
						if (readObject instanceof Map<?, ?>) {
							final Map<?, ?> newMap = (Map<?, ?>) readObject;
							final Map<String, SerializableTriple<String, String, Integer>> loaded = (Map<String, SerializableTriple<String, String, Integer>>) newMap;
							rowsToWells.clear();
							for (final Entry<String, SerializableTriple<String, String, Integer>> entry : loaded
									.entrySet()) {
								rowsToWells.put(new RowKey(entry.getKey()),
										entry.getValue());
							}
						}
					} catch (final ClassNotFoundException e) {
						throw new IOException(e);
					} finally {
						oos.close();
					}
				} finally {
					zis.close();
				}
			} finally {
				fis.close();
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveInternals(final File internDir,
			final ExecutionMonitor exec) throws IOException,
			CanceledExecutionException {
		final File joinTableFile = new File(internDir, JOIN_TABLE_FILE);
		{
			final FileOutputStream fos = new FileOutputStream(joinTableFile);
			try {
				final GZIPOutputStream zos = new GZIPOutputStream(fos);
				try {
					final ObjectOutputStream oos = new ObjectOutputStream(zos);
					try {
						oos.writeObject(this.joinTable);
					} finally {
						oos.close();
					}
				} finally {
					zos.close();
				}
			} finally {
				fos.close();
			}
		}
		final File rowTableFile = new File(internDir, ROW_TABLE_FILE);
		final FileOutputStream fos = new FileOutputStream(rowTableFile);
		try {
			final GZIPOutputStream zos = new GZIPOutputStream(fos);
			try {
				final ObjectOutputStream oos = new ObjectOutputStream(zos);
				try {
					final HashMap<String, Serializable> toSave = new HashMap<String, Serializable>();
					for (final Entry<RowKey, ? extends Serializable> entry : rowsToWells
							.entrySet()) {
						toSave
								.put(entry.getKey().getString(), entry
										.getValue());
					}
					oos.writeObject(toSave);
				} finally {
					oos.close();
				}
			} finally {
				zos.close();
			}
		} finally {
			fos.close();
		}
	}

	/** @return Plate, row, column, field, image id (series), LOCI data */
	public Map<String, Map<String, Map<Integer, Map<Integer, Map<Integer, FormatReader>>>>> getJoinTable() {
		return joinTable;
	}

	/**
	 * @return The rowsToWells.
	 */
	public Map<RowKey, SerializableTriple<String, String, Integer>> getRowsToWells() {
		return rowsToWells;
	}
}

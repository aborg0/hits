/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package ie.tcd.imm.hits.knime.xls;

import ie.tcd.imm.hits.common.PublicConstants;
import ie.tcd.imm.hits.util.file.OpenStream;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnDomain;
import org.knime.core.data.DataColumnDomainCreator;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
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
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.defaultnodesettings.SettingsModelStringArray;

import edu.umd.cs.findbugs.annotations.DefaultAnnotation;

/**
 * This is the model implementation of Importer. Reads the data from xls files
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
@NotThreadSafe
@DefaultAnnotation(Nonnull.class)
public class ImporterNodeModel extends NodeModel {

	// the logger instance
	private static final NodeLogger logger = NodeLogger
			.getLogger(ImporterNodeModel.class);

	/** The configuration key for the files. */
	static final String CFGKEY_FILES = "ie.tcd.imm.hits.knime.xls.files";

	/** The configuration key for the annotation file path. */
	static final String CFGKEY_ANNOTATION_FILE = "ie.tcd.imm.hits.knime.xls.annot";
	/** The default annotation file path. */
	static final String DEFAULT_ANNOTATION_FILE = "";

	/** The configuration key for the well count per plate parameter. */
	static final String CFGKEY_WELL_COUNT = "ie.tcd.imm.hits.knime.wells";
	/** The default well count */
	static final int DEFAULT_WELL_COUNT = 96;
	/** The maximal value for well count */
	static final int MAX_WELL_COUNT = 384;

	/**
	 * The configuration key for the plate count ("real" plate count / replicate
	 * count).
	 */
	static final String CFGKEY_PLATE_COUNT = "ie.tcd.imm.hits.knime.plates";
	/** The default value for the plate count */
	static final int DEFAULT_PLATE_COUNT = 1;

	/** The configuration key for the replicate count. */
	static final String CFGKEY_REPLICATE_COUNT = "ie.tcd.imm.hits.knime.replicates";
	/** The default value for the replicate count */
	static final int DEFAULT_REPLICATE_COUNT = 3;
	/** The maximal value for the replicate count */
	static final int MAX_REPLICATE_COUNT = 4;

	private final SettingsModelStringArray filesModel = new SettingsModelStringArray(
			CFGKEY_FILES, new String[] {});

	private final SettingsModelString annotationFileNameModel = new SettingsModelString(
			CFGKEY_ANNOTATION_FILE, DEFAULT_ANNOTATION_FILE);

	private final SettingsModelIntegerBounded wellCountModel = new SettingsModelIntegerBounded(
			ImporterNodeModel.CFGKEY_WELL_COUNT,
			ImporterNodeModel.DEFAULT_WELL_COUNT, DEFAULT_WELL_COUNT,
			MAX_WELL_COUNT);

	private final SettingsModelIntegerBounded plateCountModel = new SettingsModelIntegerBounded(
			ImporterNodeModel.CFGKEY_PLATE_COUNT,
			ImporterNodeModel.DEFAULT_PLATE_COUNT, 1, Integer.MAX_VALUE);

	private final SettingsModelIntegerBounded replicateCountModel = new SettingsModelIntegerBounded(
			ImporterNodeModel.CFGKEY_REPLICATE_COUNT,
			ImporterNodeModel.DEFAULT_REPLICATE_COUNT, 1, MAX_REPLICATE_COUNT);

	/**
	 * Constructor for the node model.
	 */
	protected ImporterNodeModel() {

		super(0, 1);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
			final ExecutionContext exec) throws Exception {
		final boolean addAnnotations = !annotationFileNameModel
				.getStringValue().isEmpty();
		BufferedDataContainer container = null;
		int rows, cols;
		switch (wellCountModel.getIntValue()) {
		case 96:
			rows = 8;
			cols = 12;
			break;
		case 384:
			rows = 16;
			cols = 24;
			break;
		default:
			throw new UnsupportedOperationException(
					"Not implemented for other than 96, or 384 wells.");
		}
		final String[] fileNames = filesModel.getStringArrayValue();
		String[][][] annotations = null;
		for (int j = 0; j < fileNames.length; j++) {
			final String fileName = fileNames[j];
			final File file = new File(fileName);
			final InputStream fis;
			try {
				fis = OpenStream.open(new URI(fileName));
				// new FileInputStream(file);
				try {
					final POIFSFileSystem fs = new POIFSFileSystem(fis);
					final HSSFWorkbook wb = new HSSFWorkbook(fs);
					final HSSFSheet perWellSheet = wb
							.getSheet("Summary by wells");
					final HSSFRow row = perWellSheet.getRow(1);
					final int specColNum = 4;
					int columns = specColNum;
					for (short i = row.getLastCellNum(); i-- > Math.max(row
							.getFirstCellNum(), 1)
							&& row.getCell(i) != null;) {
						++columns;
					}
					exec.checkCanceled();
					if (j == 0) {
						final DataTableSpec outputSpec = getDataTableSpecFromRow(row);
						container = exec.createDataContainer(outputSpec);
						try {
							annotations = readAnnotations(plateCountModel
									.getIntValue(), rows, cols,
									annotationFileNameModel.getStringValue());
						} catch (final Exception e) {
							logger.warn(
									"Unable to read the gene annontation file: "
											+ annotationFileNameModel
													.getStringValue(), e);
						}
					} else {
						if (!getDataTableSpecFromRow(row).equalStructure(
								container.getTableSpec())) {
							assert false : "The table specification "
									+ getDataTableSpecFromRow(row)
									+ " is not the same as the original "
									+ container.getTableSpec() + " in file: "
									+ file.getAbsolutePath();
						}
					}
					final int replicateCount = replicateCountModel
							.getIntValue();
					if (perWellSheet.getLastRowNum() - 2 != rows * cols) {
						throw new IllegalStateException(
								"Wrong structure of the xls file: " + fileName);
					}
					for (int i = specColNum; i < perWellSheet.getLastRowNum() + 1; ++i) {
						final DataCell[] values = new DataCell[columns
								+ (addAnnotations ? 2 : 0)];
						values[0] = new StringCell(fileName);// barcode
						values[1] = new IntCell(1 + j / replicateCount);// plate
						values[2] = new IntCell(1 + j % replicateCount);// replicate
						final HSSFRow currentRow = perWellSheet.getRow(i);
						final String wellName = currentRow.getCell((short) 0)
								.getRichStringCellValue().getString().replace(
										" - ", "");
						values[3] = new StringCell(wellName);
						for (int c = specColNum; c < columns; ++c) {
							final HSSFCell cell = currentRow
									.getCell((short) (c - 2));
							values[c] = new DoubleCell(cell
									.getNumericCellValue());
						}
						final int wellIndex = getIndex(wellName, rows, cols);
						if (wellIndex == -1) {
							logger.debug(wellName);
						}
						final String annot = wellIndex == -1
								|| annotations == null ? null : annotations[j
								/ replicateCount][wellIndex][1];
						final String geneID = wellIndex == -1
								|| annotations == null ? null : annotations[j
								/ replicateCount][wellIndex][0];
						final String nonNullAnnot = annot == null ? "" : annot;
						final String nonNullGeneID = geneID == null ? ""
								: geneID;
						if (addAnnotations) {
							values[columns] = new StringCell(nonNullGeneID);
							values[columns + 1] = new StringCell(nonNullAnnot);
						}
						final String keyString = j / replicateCount + 1 + "_"
								+ (j % replicateCount + 1) + "_" + (i - 2);
						final DefaultRow defaultRow = new DefaultRow(
								new RowKey(keyString), values);
						container.addRowToTable(defaultRow);
					}
				} finally {
					fis.close();
				}
			} catch (final FileNotFoundException e) {
				throw new InvalidSettingsException("Not found: "
						+ file.getAbsolutePath(), e);
			} catch (final IOException e) {
				throw new InvalidSettingsException(e.getMessage(), e);
			}
			exec.setProgress(j / (double) fileNames.length, "Processing file: "
					+ fileName);
		}
		// once we are done, we close the container and return its table
		container.close();
		final BufferedDataTable out = container.getTable();
		return new BufferedDataTable[] { out };
	}

	private static String[][][] readAnnotations(final int plateCount,
			final int rows, final int cols, final String annotationFileName)
			throws IOException {
		final String[][][] ret = new String[plateCount][rows * cols][2];
		if (annotationFileName.isEmpty()) {
			return ret;
		}
		// final File file = new File(annotationFileName);
		InputStream stream;
		try {
			stream = OpenStream.open(OpenStream.convertURI(annotationFileName));
		} catch (final URISyntaxException e) {
			throw new RuntimeException(e);
		}
		try {
			final Reader fileReader = new InputStreamReader(stream);
			try {
				final BufferedReader br = new BufferedReader(fileReader);
				try {
					String line = null;
					while ((line = br.readLine()) != null) {
						final int[] indices = getIndices(line, rows, cols);
						if (indices != null) {
							try {
								final String[] parts = line.split("\t");
								ret[indices[0]][indices[1]][0] = parts.length > 2 ? parts[2]
										: null;
								ret[indices[0]][indices[1]][1] = line
										.substring(line.lastIndexOf('\t') + 1);
							} catch (final RuntimeException e) {
								// ignore, there might be more data there.
							}
						}
					}
					return ret;
				} finally {
					br.close();
				}
			} finally {
				fileReader.close();
			}
		} finally {
			stream.close();
		}
	}

	private static @Nullable
	int[] getIndices(final String line, final int rows, final int cols) {
		if (line == null) {
			return null;
		}
		final String[] parts = line.split("\t");
		if (parts.length < 3) {
			return null;
		}
		try {
			final String well = parts[1];
			final int wellIndex = getIndex(well, rows, cols);
			if (wellIndex == -1) {
				return null;
			}
			return new int[] { Integer.parseInt(parts[0]) - 1, wellIndex };
		} catch (final RuntimeException e) {
			return null;
		}
	}

	private static int getIndex(final String well, final int rows,
			final int cols) {
		final int row = Character.toLowerCase(well.charAt(0)) - 'a';
		final int col = Integer.parseInt(well.substring(1)) - 1;
		final int wellIndex = row < 0 || row >= rows || col < 0 || col >= cols ? -1
				: row * cols + col;
		return wellIndex;
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
	protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
			throws InvalidSettingsException {
		if (filesModel.getStringArrayValue().length == 0) {
			throw new InvalidSettingsException("No file set");
		}

		final String annotFile = annotationFileNameModel.getStringValue();
		try {
			final InputStream stream = OpenStream.open(OpenStream
					.convertURI(annotFile));
			stream.close();
		} catch (final IOException e) {
			throw new InvalidSettingsException("Unable to read: " + annotFile);
		}
		// if (!annotFile.isEmpty() && !new File(annotFile).canRead()) {
		// throw new InvalidSettingsException(
		// "The annotation file -if specified must be readable!");
		// }
		catch (final URISyntaxException e) {
			throw new InvalidSettingsException("Unable to convert file name ("
					+ annotFile + ") to URI: " + e.getMessage(), e);
		}

		// final File file = new File(filesModel.getStringArrayValue()[0]);
		final InputStream fis;
		try {
			fis = OpenStream.open(OpenStream.convertURI(filesModel
					.getStringArrayValue()[0]));
			// new FileInputStream(file);
			try {
				final POIFSFileSystem fs = new POIFSFileSystem(fis);
				final HSSFWorkbook wb = new HSSFWorkbook(fs);
				final HSSFSheet perWellSheet = wb.getSheet("Summary by wells");
				final HSSFRow row = perWellSheet.getRow(1);
				final DataTableSpec dataTableSpec = getDataTableSpecFromRow(row);
				return new DataTableSpec[] { dataTableSpec };
			} finally {
				fis.close();
			}
		} catch (final FileNotFoundException e) {
			String uri;
			try {
				uri = OpenStream
						.convertURI(filesModel.getStringArrayValue()[0])
						.toString();
			} catch (final URISyntaxException e1) {
				uri = "";
			}
			throw new InvalidSettingsException("Not found: "
					+ filesModel.getStringArrayValue()[0] + "(" + uri + ")", e);
		} catch (final IOException e) {
			throw new InvalidSettingsException(e.getMessage(), e);
		} catch (final URISyntaxException e) {
			throw new InvalidSettingsException("Unable to convert file name ("
					+ filesModel.getStringArrayValue()[0] + ") to URI: "
					+ e.getMessage(), e);
		}
	}

	private DataTableSpec getDataTableSpecFromRow(final HSSFRow row) {
		final List<String> header = new ArrayList<String>();
		for (int i = row.getLastCellNum(); i-- > Math.max(
				row.getFirstCellNum(), 1)
				&& row.getCell((short) i) != null;) {
			header.add(0, row.getCell((short) i).getRichStringCellValue()
					.getString());
		}
		final boolean addAnnotations = !annotationFileNameModel
				.getStringValue().isEmpty();
		final DataType[] cellTypes = new DataType[header.size()
				+ (addAnnotations ? 6 : 4)];
		for (int i = 0; i < header.size(); i++) {
			cellTypes[i + 4] = DoubleCell.TYPE;
		}
		cellTypes[0] = StringCell.TYPE;// barcode/path
		cellTypes[1] = IntCell.TYPE;// plate
		cellTypes[2] = IntCell.TYPE;// replicate
		cellTypes[3] = StringCell.TYPE;// Well
		header.add(0, PublicConstants.WELL_COL_NAME);
		header.add(0, PublicConstants.REPLICATE_COL_NAME);
		header.add(0, PublicConstants.PLATE_COL_NAME);
		header.add(0, PublicConstants.BARCODE_COLUMN);
		if (addAnnotations) {
			header.add(PublicConstants.GENE_ID_COL_NAME);
			header.add(PublicConstants.GENE_ANNOTATION_COL_NAME);
			cellTypes[cellTypes.length - 2] = StringCell.TYPE;
			cellTypes[cellTypes.length - 1] = StringCell.TYPE;
		}
		final DataTableSpec dataTableSpec = new DataTableSpec(header
				.toArray(new String[header.size()]), cellTypes);
		return addWellDomain(dataTableSpec);
	}

	/**
	 * Adds the (generated) domain of column
	 * {@value PublicConstants#WELL_COL_NAME}.
	 * 
	 * @param dataTableSpec
	 *            Result {@link DataTableSpec}.
	 * @return The new {@link DataTableSpec}.
	 */
	private DataTableSpec addWellDomain(final DataTableSpec dataTableSpec) {
		final DataColumnSpec[] resultSpecs = new DataColumnSpec[dataTableSpec
				.getNumColumns()];
		final int wells = wellCountModel.getIntValue();
		final int rows;
		switch (wells) {
		case 96:
			rows = 12;
			break;
		case 384:
			rows = 24;
			break;
		default:
			throw new IllegalStateException("Wrong plate format: " + wells);
		}
		int i = 0;
		for (final DataColumnSpec dataColumnSpec : dataTableSpec) {
			if (dataColumnSpec.getName().equalsIgnoreCase(
					PublicConstants.WELL_COL_NAME)) {
				final DataColumnSpecCreator specCreator = new DataColumnSpecCreator(
						dataColumnSpec);
				final DataCell[] possibleValues = new DataCell[wells];
				for (int j = possibleValues.length; j-- > 0;) {
					possibleValues[j] = new StringCell(Character
							.toString((char) ('A' + j / rows))
							+ Integer.toString((j % rows + 1)));
				}
				final DataColumnDomain domain = new DataColumnDomainCreator(
						possibleValues).createDomain();
				specCreator.setDomain(domain);
				resultSpecs[i++] = specCreator.createSpec();
			} else {
				resultSpecs[i++] = dataColumnSpec;
			}
		}
		return new DataTableSpec(resultSpecs);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveSettingsTo(final NodeSettingsWO settings) {
		annotationFileNameModel.saveSettingsTo(settings);
		filesModel.saveSettingsTo(settings);
		wellCountModel.saveSettingsTo(settings);
		plateCountModel.saveSettingsTo(settings);
		replicateCountModel.saveSettingsTo(settings);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
			throws InvalidSettingsException {
		annotationFileNameModel.loadSettingsFrom(settings);
		filesModel.loadSettingsFrom(settings);
		wellCountModel.loadSettingsFrom(settings);
		plateCountModel.loadSettingsFrom(settings);
		replicateCountModel.loadSettingsFrom(settings);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void validateSettings(final NodeSettingsRO settings)
			throws InvalidSettingsException {
		annotationFileNameModel.validateSettings(settings);
		wellCountModel.validateSettings(settings);
		plateCountModel.validateSettings(settings);
		replicateCountModel.validateSettings(settings);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadInternals(final File internDir,
			final ExecutionMonitor exec) throws IOException,
			CanceledExecutionException {
		// TODO load internal data.
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
		// TODO save internal models.
		// Everything written to output ports is saved automatically (data
		// returned by the execute method, models saved in the saveModelContent,
		// and user settings saved through saveSettingsTo - is all taken care
		// of). Save here only the other internals that need to be preserved
		// (e.g. data used by the views).
	}
}

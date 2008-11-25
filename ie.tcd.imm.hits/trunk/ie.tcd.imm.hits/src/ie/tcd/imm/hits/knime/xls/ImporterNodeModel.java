package ie.tcd.imm.hits.knime.xls;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.knime.core.data.DataCell;
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
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.defaultnodesettings.SettingsModelStringArray;

/**
 * This is the model implementation of Importer. Reads the data from xls files
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
public class ImporterNodeModel extends NodeModel {

	// the logger instance
	private static final NodeLogger logger = NodeLogger
			.getLogger(ImporterNodeModel.class);

	public static final String PLATE_COL_NAME = "Plate";
	public static final String REPLICATE_COL_NAME = "replicate";
	public static final String WELL_COL_NAME = "well";
	public static final String GENE_ID_COL_NAME = "GeneID";
	public static final String GENE_ANNOTATION_COL_NAME = "GeneSymbol";
	static final String CFGKEY_FILES = "ie.tcd.imm.hits.knime.xls.files";
	static final String CFGKEY_DIR = "ie.tcd.imm.hits.knime.xls.files.dir";

	static final String CFGKEY_ANNOTATION_FILE = "ie.tcd.imm.hits.knime.xls.annot";
	static final String DEFAULT_ANNOTATION_FILE = "";

	static final String CFGKEY_COMBINE_ANNOTATIONS = "ie.tcd.imm.hits.knime.xls.combine_annot";
	static final boolean DEFAULT_COMBINE_ANNOTATIONS = true;

	static final String CFGKEY_WELL_COUNT = "ie.tcd.imm.hits.knime.wells";

	static final int DEFAULT_WELL_COUNT = 96;
	static final int MAX_WELL_COUNT = 384;

	static final String CFGKEY_PLATE_COUNT = "ie.tcd.imm.hits.knime.plates";

	static final int DEFAULT_PLATE_COUNT = 1;

	static final String CFGKEY_REPLICATE_COUNT = "ie.tcd.imm.hits.knime.replicates";

	static final int DEFAULT_REPLICATE_COUNT = 3;

	static final int MAX_REPLICATE_COUNT = 4;

	private final SettingsModelStringArray filesModel = new SettingsModelStringArray(
			CFGKEY_FILES, new String[] {});

	private final SettingsModelString annotationFileNameModel = new SettingsModelString(
			CFGKEY_ANNOTATION_FILE, DEFAULT_ANNOTATION_FILE);

	private final SettingsModelBoolean combineAnnotationsModel = new SettingsModelBoolean(
			CFGKEY_COMBINE_ANNOTATIONS, DEFAULT_COMBINE_ANNOTATIONS);

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

		super(0, 2);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
			final ExecutionContext exec) throws Exception {
		BufferedDataContainer container = null;
		final BufferedDataContainer annotationsTable = exec
				.createDataContainer(getAnnotationTableSpec());
		// final String dir = dirModel.getStringValue();
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
			final FileInputStream fis;
			try {
				fis = new FileInputStream(file);
				try {
					final POIFSFileSystem fs = new POIFSFileSystem(fis);
					final HSSFWorkbook wb = new HSSFWorkbook(fs);
					final HSSFSheet perWellSheet = wb
							.getSheet("Summary by wells");
					final HSSFRow row = perWellSheet.getRow(1);
					int columns = 3;
					for (short i = row.getLastCellNum(); i-- > Math.max(row
							.getFirstCellNum(), 1)
							&& row.getCell(i) != null;) {
						++columns;
					}
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
						assert getDataTableSpecFromRow(row).equalStructure(
								container.getTableSpec());
					}
					final int replicateCount = replicateCountModel
							.getIntValue();
					if (perWellSheet.getLastRowNum() - 2 != rows * cols) {
						throw new IllegalStateException(
								"Wrong structure of the xls file: " + fileName);
					}
					for (int i = 3; i < perWellSheet.getLastRowNum() + 1; ++i) {
						final DataCell[] values = new DataCell[columns
								+ (combineAnnotationsModel.getBooleanValue() ? 2
										: 0)];
						values[0] = new IntCell(1 + (j / replicateCount));// plate
						values[1] = new IntCell(1 + (j % replicateCount));// replicate
						final HSSFRow currentRow = perWellSheet.getRow(i);
						final String wellName = currentRow.getCell(0)
								.getRichStringCellValue().getString().replace(
										" - ", "");
						values[2] = new StringCell(wellName);
						for (int c = 3; c < columns; ++c) {
							final HSSFCell cell = currentRow.getCell(c - 1);
							values[c] = new DoubleCell(cell
									.getNumericCellValue());
						}
						final int wellIndex = getIndex(wellName, rows, cols);
						if (wellIndex == -1) {
							System.out.println(wellName);
						}
						final String annot = wellIndex == -1 ? null
								: annotations[j / replicateCount][wellIndex][1];
						final String geneID = wellIndex == -1 ? null
								: annotations[j / replicateCount][wellIndex][0];
						final String nonNullAnnot = annot == null ? "" : annot;
						final String nonNullGeneID = geneID == null ? ""
								: geneID;
						if (combineAnnotationsModel.getBooleanValue()) {
							values[columns] = new StringCell(nonNullGeneID);
							values[columns + 1] = new StringCell(nonNullAnnot);
						}
						final String keyString = ((j / replicateCount) + 1)
								+ "_" + ((j % replicateCount) + 1) + "_"
								+ (i - 2);
						final DefaultRow defaultRow = new DefaultRow(
								new RowKey(keyString), values);
						final DefaultRow annotRow = new DefaultRow(new RowKey(
								keyString), values[0], values[1], values[2],
								new StringCell(nonNullGeneID), new StringCell(
										nonNullAnnot));
						container.addRowToTable(defaultRow);
						annotationsTable.addRowToTable(annotRow);
					}
					final DataType[] cellTypes = new DataType[columns];
					for (int i = 0; i < cellTypes.length; i++) {
						cellTypes[i] = DoubleCell.TYPE;
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
		annotationsTable.close();
		final BufferedDataTable out = container.getTable();
		return new BufferedDataTable[] { out, annotationsTable.getTable() };
	}

	private static String[][][] readAnnotations(final int plateCount,
			final int rows, final int cols, final String annotationFileName)
			throws IOException {
		final String[][][] ret = new String[plateCount][rows * cols][2];
		if (annotationFileName.isEmpty()) {
			return ret;
		}
		final File file = new File(annotationFileName);
		final Reader fileReader = new FileReader(file);
		try {
			final BufferedReader br = new BufferedReader(fileReader);
			try {
				String line = null;
				while ((line = br.readLine()) != null) {
					final int[] indices = getIndices(line, rows, cols);
					if (indices != null) {
						final String[] parts = line.split("\t");
						ret[indices[0]][indices[1]][0] = parts.length > 2 ? parts[2]
								: null;
						ret[indices[0]][indices[1]][1] = line.substring(line
								.lastIndexOf('\t') + 1);
					}
				}
				return ret;
			} finally {
				br.close();
			}
		} finally {
			fileReader.close();
		}
	}

	private static int[] getIndices(final String line, final int rows,
			final int cols) {
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
		final int wellIndex = (row < 0 || row >= rows || col < 0 || col >= cols) ? -1
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
		if (!annotFile.isEmpty() && !new File(annotFile).canRead()) {
			throw new InvalidSettingsException(
					"The annotation file -if specified must be readable!");
		}

		// final String dirName = dirModel.getStringValue();
		// if (dirName == null) {
		// throw new InvalidSettingsException("Must select a directory.");
		// }
		final File file = new File(filesModel.getStringArrayValue()[0]);
		final FileInputStream fis;
		try {
			fis = new FileInputStream(file);
			try {
				final POIFSFileSystem fs = new POIFSFileSystem(fis);
				final HSSFWorkbook wb = new HSSFWorkbook(fs);
				final HSSFSheet perWellSheet = wb.getSheet("Summary by wells");
				final HSSFRow row = perWellSheet.getRow(1);
				final DataTableSpec dataTableSpec = getDataTableSpecFromRow(row);
				return new DataTableSpec[] { dataTableSpec,
						getAnnotationTableSpec() };
			} finally {
				fis.close();
			}
		} catch (final FileNotFoundException e) {
			throw new InvalidSettingsException("Not found: "
					+ file.getAbsolutePath(), e);
		} catch (final IOException e) {
			throw new InvalidSettingsException(e.getMessage(), e);
		}
	}

	private static DataTableSpec getAnnotationTableSpec() {
		return new DataTableSpec(new String[] { PLATE_COL_NAME,
				REPLICATE_COL_NAME, WELL_COL_NAME, GENE_ID_COL_NAME,
				GENE_ANNOTATION_COL_NAME },
				new DataType[] { IntCell.TYPE, IntCell.TYPE, StringCell.TYPE,
						StringCell.TYPE, StringCell.TYPE });
	}

	private DataTableSpec getDataTableSpecFromRow(final HSSFRow row) {
		final List<String> header = new ArrayList<String>();
		for (short i = row.getLastCellNum(); i-- > Math.max(row
				.getFirstCellNum(), 1)
				&& row.getCell(i) != null;) {
			header.add(0, row.getCell(i).getRichStringCellValue().getString());
		}
		final DataType[] cellTypes = new DataType[header.size()
				+ (combineAnnotationsModel.getBooleanValue() ? 5 : 3)];
		for (int i = 0; i < header.size(); i++) {
			cellTypes[i + 3] = DoubleCell.TYPE;
		}
		cellTypes[0] = IntCell.TYPE;// plate
		cellTypes[1] = IntCell.TYPE;// replicate
		cellTypes[2] = StringCell.TYPE;// Well
		header.add(0, WELL_COL_NAME);
		header.add(0, REPLICATE_COL_NAME);
		header.add(0, PLATE_COL_NAME);
		if (combineAnnotationsModel.getBooleanValue()) {
			header.add(GENE_ID_COL_NAME);
			header.add(GENE_ANNOTATION_COL_NAME);
			cellTypes[cellTypes.length - 2] = StringCell.TYPE;
			cellTypes[cellTypes.length - 1] = StringCell.TYPE;
		}
		final DataTableSpec dataTableSpec = new DataTableSpec(header
				.toArray(new String[header.size()]), cellTypes);
		return dataTableSpec;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveSettingsTo(final NodeSettingsWO settings) {
		// dirModel.saveSettingsTo(settings);
		annotationFileNameModel.saveSettingsTo(settings);
		combineAnnotationsModel.saveSettingsTo(settings);
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
		// dirModel.loadSettingsFrom(settings);
		annotationFileNameModel.loadSettingsFrom(settings);
		combineAnnotationsModel.loadSettingsFrom(settings);
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
		// filesModel.validateSettings(settings);
		// dirModel.validateSettings(settings);
		annotationFileNameModel.validateSettings(settings);
		combineAnnotationsModel.validateSettings(settings);
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

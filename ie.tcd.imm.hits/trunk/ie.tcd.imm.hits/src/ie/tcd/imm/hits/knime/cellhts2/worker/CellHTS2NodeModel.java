/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package ie.tcd.imm.hits.knime.cellhts2.worker;

import ie.tcd.imm.hits.common.Format;
import ie.tcd.imm.hits.common.PossibleStatistics;
import ie.tcd.imm.hits.common.PublicConstants;
import ie.tcd.imm.hits.common.PossibleStatistics.Multiplicity;
import ie.tcd.imm.hits.common.PublicConstants.StaticUtil;
import ie.tcd.imm.hits.knime.cellhts2.configurator.simple.SimpleConfiguratorNodeModel;
import ie.tcd.imm.hits.knime.cellhts2.prefs.PreferenceConstants;
import ie.tcd.imm.hits.knime.cellhts2.prefs.ui.ColumnSelectionFieldEditor;
import ie.tcd.imm.hits.knime.xls.ImporterNodePlugin;
import ie.tcd.imm.hits.util.Misc;
import ie.tcd.imm.hits.util.Pair;
import ie.tcd.imm.hits.util.RUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.Map.Entry;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import javax.swing.JOptionPane;

import org.eclipse.core.filesystem.EFS;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
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
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelDoubleBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelDoubleRange;
import org.knime.core.node.defaultnodesettings.SettingsModelFilterString;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPDouble;
import org.rosuda.REngine.REXPFactor;
import org.rosuda.REngine.REXPInteger;
import org.rosuda.REngine.REXPLogical;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.REXPString;
import org.rosuda.REngine.REXPVector;
import org.rosuda.REngine.REngineException;
import org.rosuda.REngine.RList;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;

import edu.umd.cs.findbugs.annotations.DefaultAnnotation;

/**
 * This is the model implementation of CellHTS2. This node performs the
 * calculations using CellHTS2.
 * <p>
 * Format of the possible patterns:<br>
 * Every {@code \}, {@code /} will be replaced to the platform specific path
 * separator.<br>
 * You can put special values in your result using one of the following
 * constructs:<br>
 * <ul>
 * <li><code>{e}</code> - experiment name</li>
 * <li><code>{n}</code> - appends the normalisation method</li>
 * <li><code>{p}</code> - adds the parameters separated by the next character if
 * it is not a digit or <code>}</code>. If it is followed by digits it will try
 * to create a reasonable abbreviation from it.</li>
 * <li><code>{*}</code> - puts {@code +} sign or nothing depending on the
 * additive or multiplicative nature of normalisation method.</li>
 * </ul>
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
@NotThreadSafe
@DefaultAnnotation(Nonnull.class)
public class CellHTS2NodeModel extends NodeModel {

	/** the logger instance */
	static final NodeLogger logger = NodeLogger
			.getLogger(CellHTS2NodeModel.class);

	/** Configuration key for the experiment name */
	static final String CFGKEY_EXPERIMENT_NAME = "ie.tcd.imm.hits.knime.cellhts2.experimentname";
	/** Default value of experiment name */
	static final String DEFAULT_EXPERIMENT_NAME = "test";

	/** Configuration key for the selected parameters */
	static final String CFGKEY_PARAMETERS = "ie.tcd.imm.hits.knime.cellhts2.parameters";
	/** Default value of parameters */
	static final String DEFAULT_PARAMETERS = "";

	/** Configuration key for the normalisation method */
	static final String CFGKEY_NORMALISATION_METHOD = "ie.tcd.imm.hits.knime.cellhts2.norm";
	/**
	 * Possible values of the normalisation methods when {@code locfit} is
	 * installed.
	 */
	static final String[] POSSIBLE_NORMALISATION_METHODS_LOCFIT = new String[] {
			"every", "median", "Bscore", "POC", "negatives", "NPI", "mean",
			"shorth", "locfit", "loess" };
	/**
	 * Possible values of the normalisation methods when {@code locfit} is
	 * installed, and custom normalisation is also allowed.
	 */
	static final String[] POSSIBLE_NORMALISATION_METHODS_LOCFIT_EXPERIMENTAL = new String[] {
			"every", "median", "Bscore", "POC", "negatives", "NPI", "mean",
			"shorth", "locfit", "loess", "customA", "customB", "customC" };
	/** Possible values of the normalisation methods. */
	static final String[] POSSIBLE_NORMALISATION_METHODS = new String[] {
			"every", "median", "Bscore", "POC", "negatives", "NPI", "mean",
			"shorth" };
	/** Possible values of the normalisation methods with custom methods. */
	static final String[] POSSIBLE_NORMALISATION_METHODS_EXPERIMENTAL = new String[] {
			"every", "median", "Bscore", "POC", "negatives", "NPI", "mean",
			"shorth", "customA", "customB", "customC" };

	/** Configuration key for multiplicative or additive normalisation */
	static final String CFGKEY_IS_MULTIPLICATIVE_NORMALISATION = "ie.tcd.imm.hits.knime.cellhts2.is_multiplicative";
	/** Default value of multiplicative or additive normalisation */
	static final boolean DEFAULT_IS_MULTIPLICATIVE_NORMALISATION = false;

	/** Configuration key for log-transform parameter */
	static final String CFGKEY_LOG_TRANSFORM = "ie.tcd.imm.hits.knime.cellhts2.log";
	/** Default value of log-transform parameter */
	static final boolean DEFAULT_LOG_TRANSFORM = false;

	/** Configuration key for variance adjustment */
	static final String CFGKEY_SCALE = "ie.tcd.imm.hits.knime.cellhts2.scale";
	/** Possible values of variance adjustment */
	static final String[] POSSIBLE_SCALE = new String[] { "no adjustment",
			"by plate", "by experiment" };

	/** Configuration key for scoring method */
	static final String CFGKEY_SCORE = "ie.tcd.imm.hits.knime.cellhts2.score";
	/** Possible values of the scoring methods */
	static final String[] POSSIBLE_SCORE = new String[] { "none", "zscore",
			"NPI", "zscoreByPlate", "zscoreNonRobust", "zscoreNonRobustByPlate" };
	/** Possible values of the scoring methods with custom methods. */
	static final String[] POSSIBLE_SCORE_EXPERIMENTAL = new String[] { "none",
			"zscore", "NPI", "zscoreByPlate", "zscoreNonRobust",
			"zscoreNonRobustByPlate", "customA", "customB", "customC" };

	/** Configuration key for summarisation strategy */
	static final String CFGKEY_SUMMARISE = "ie.tcd.imm.hits.knime.cellhts2.summarize";
	/** Possible values of the summarisation strategies */
	static final String[] POSSIBLE_SUMMARISE = new String[] { "mean", "median",
			"max", "min", "rms", "closestToZero", "furthestFromZero" };

	/** Configuration key for the output folder */
	static final String CFGKEY_OUTPUT_DIR = "ie.tcd.imm.hits.knime.cellhts2.output";
	/** Default value of the output folder */
	static final String DEFAULT_OUTPUT_DIR = System.getProperty("user.home")
			+ "/results";

	/** Configuration key for the range of the heatmaps */
	static final String CFGKEY_SCORE_RANGE = "ie.tcd.imm.hits.knime.cellhts2.score.range";
	/** Default value of maximal bound of generated heatmaps. */
	static final double DEFAULT_SCORE_RANGE_MAX = 3.0;
	/** Default value of minimal bound of generated heatmaps. */
	static final double DEFAULT_SCORE_RANGE_MIN = -3.0;

	/** Configuration key for aspect ratio of the images */
	static final String CFGKEY_ASPECT_RATIO = "ie.tcd.imm.hits.knime.cellhts2.image.aspect_ratio";
	/** Default value of aspect ratio of images */
	static final double DEFAULT_ASPECT_RATIO = 1.0;

	/** Configuration key for pattern generating the folder */
	static final String CFGKEY_FOLDER_PATTERN = "ie.tcd.imm.hits.knime.cellhts2.folder_pattern";
	// /** Possible values of the pattern that generates the folders. */
	// static final String[] POSSIBLE_FOLDER_PATTERNS = new String[] { "",
	// "{e}",
	// "{e}\\{p}", "{e}\\{n}{*}\\{p15}", "{p}", "{n}\\{p}", "{e}_{p}",
	// "{e}_{n}{*}_{p15}", "{p}", "{n}_{p}"};
	/** Default value of pattern generating the folder */
	static final String DEFAULT_FOLDER_PATTERN = "{e}\\{n}{*}\\{p15}";

	/** Configuration key for generating HTML reports */
	static final String CFGKEY_GENERATE_HTML = "generate.HTML";
	/** Default value of generating HTML reports */
	static final boolean DEFAULT_GENERATE_HTML = true;

	private final SettingsModelString normMethodModel = new SettingsModelString(
			CellHTS2NodeModel.CFGKEY_NORMALISATION_METHOD,
			CellHTS2NodeModel.POSSIBLE_NORMALISATION_METHODS[1]);

	private final SettingsModelBoolean isMultiplicativeModel = new SettingsModelBoolean(
			CellHTS2NodeModel.CFGKEY_IS_MULTIPLICATIVE_NORMALISATION,
			CellHTS2NodeModel.DEFAULT_IS_MULTIPLICATIVE_NORMALISATION);

	private final SettingsModelBoolean logTransformModel = new SettingsModelBoolean(
			CellHTS2NodeModel.CFGKEY_LOG_TRANSFORM,
			CellHTS2NodeModel.DEFAULT_LOG_TRANSFORM);
	private final SettingsModelString scaleModel = new SettingsModelString(
			CellHTS2NodeModel.CFGKEY_SCALE, CellHTS2NodeModel.POSSIBLE_SCALE[0]);
	private final SettingsModelString scoreModel = new SettingsModelString(
			CellHTS2NodeModel.CFGKEY_SCORE, CellHTS2NodeModel.POSSIBLE_SCORE[1]);
	private final SettingsModelString summariseModel = new SettingsModelString(
			CellHTS2NodeModel.CFGKEY_SUMMARISE,
			CellHTS2NodeModel.POSSIBLE_SUMMARISE[0]);
	private final SettingsModelString outputDirModel = new SettingsModelString(
			CellHTS2NodeModel.CFGKEY_OUTPUT_DIR,
			CellHTS2NodeModel.DEFAULT_OUTPUT_DIR);
	private final SettingsModelString folderPatternModel = new SettingsModelString(
			CellHTS2NodeModel.CFGKEY_FOLDER_PATTERN,
			CellHTS2NodeModel.DEFAULT_FOLDER_PATTERN);
	private final SettingsModelString experimentNameModel = new SettingsModelString(
			CellHTS2NodeModel.CFGKEY_EXPERIMENT_NAME,
			CellHTS2NodeModel.DEFAULT_EXPERIMENT_NAME);
	private final SettingsModelFilterString parametersModel = new SettingsModelFilterString(
			CellHTS2NodeModel.CFGKEY_PARAMETERS, new String[0], new String[] {
					PublicConstants.PLATE_COL_NAME,
					PublicConstants.REPLICATE_COL_NAME,
					PublicConstants.WELL_COL_NAME,
					PublicConstants.GENE_ID_COL_NAME,
					PublicConstants.GENE_ANNOTATION_COL_NAME });

	private final SettingsModelDoubleRange scoreRange = new SettingsModelDoubleRange(
			CellHTS2NodeModel.CFGKEY_SCORE_RANGE,
			CellHTS2NodeModel.DEFAULT_SCORE_RANGE_MIN,
			CellHTS2NodeModel.DEFAULT_SCORE_RANGE_MAX);

	private final SettingsModelDoubleBounded aspectRatioModel = new SettingsModelDoubleBounded(
			CellHTS2NodeModel.CFGKEY_ASPECT_RATIO,
			CellHTS2NodeModel.DEFAULT_ASPECT_RATIO, 0.1, 200);

	private final SettingsModelBoolean generateHTML = new SettingsModelBoolean(
			CellHTS2NodeModel.CFGKEY_GENERATE_HTML,
			CellHTS2NodeModel.DEFAULT_GENERATE_HTML);

	private static final DataTableSpec configurationSpec = new DataTableSpec(
			new DataColumnSpecCreator("Category", StringCell.TYPE).createSpec(),
			new DataColumnSpecCreator("Key", StringCell.TYPE).createSpec(),
			new DataColumnSpecCreator("Value", StringCell.TYPE).createSpec());

	private static final DataTableSpec aggregateValuesSpec = new DataTableSpec(
			new DataColumnSpecCreator(PublicConstants.EXPERIMENT_COLUMN,
					StringCell.TYPE).createSpec(),
			new DataColumnSpecCreator(
					PublicConstants.NORMALISATION_METHOD_COLUMN,
					StringCell.TYPE).createSpec(),
			new DataColumnSpecCreator(PublicConstants.LOG_TRANSFORM_COLUMN,
					StringCell.TYPE).createSpec(),
			new DataColumnSpecCreator(
					PublicConstants.NORMALISATION_KIND_COLUMN, StringCell.TYPE)
					.createSpec(),
			new DataColumnSpecCreator(
					PublicConstants.VARIANCE_ADJUSTMENT_COLUMN, StringCell.TYPE)
					.createSpec(), new DataColumnSpecCreator(
					PublicConstants.SCORING_METHOD_COLUMN, StringCell.TYPE)
					.createSpec(), new DataColumnSpecCreator(
					PublicConstants.SUMMARISE_METHOD_COLUMN, StringCell.TYPE)
					.createSpec(), new DataColumnSpecCreator(
					PublicConstants.PLATE_COLUMN, IntCell.TYPE).createSpec(),
			new DataColumnSpecCreator(PublicConstants.REPLICATE_COLUMN,
					IntCell.TYPE).createSpec(), new DataColumnSpecCreator(
					"Parameter", StringCell.TYPE).createSpec(),
			new DataColumnSpecCreator("Replicate dynamic range",
					DoubleCell.TYPE).createSpec(), new DataColumnSpecCreator(
					"Average dynamic range", DoubleCell.TYPE).createSpec(),
			new DataColumnSpecCreator("Repeatability standard deviation",
					DoubleCell.TYPE).createSpec(), new DataColumnSpecCreator(
					"Spearman rank correlation min", DoubleCell.TYPE)
					.createSpec(), new DataColumnSpecCreator(
					"Spearman rank correlation max", DoubleCell.TYPE)
					.createSpec(), new DataColumnSpecCreator("Z' factor",
					DoubleCell.TYPE).createSpec());

	private static final String OUT_DIRS = "outdirs.txt";

	/** Constant for the expert mode. */
	static final String PROPERTY_EXPERT = "knime.expert.mode";

	private static DataTableSpec outputFolderSpec = new DataTableSpec(
			new DataColumnSpecCreator("folder", StringCell.TYPE).createSpec());

	private Map<String, String> outDirs;

	// private final SettingsModelBoolean useTCDCellHTS2ExtensionsModel = new
	// SettingsModelBoolean(
	// CFGKEY_USE_TCD_CELLHTS_EXTENSIONS,
	// DEFAULT_USE_TCD_CELLHTS_EXTENSIONS);

	private static enum CellHTS2Version {
		/**
		 * Original cellHTS2 without modifications, before version {@code 2.8.0}
		 */
		originalPre28(false, true),
		/** HiTS modified cellHTS2 before version {@code 2.8.0} */
		modifiedPre28(true, true),
		/**
		 * Original cellHTS2 without modifications, at least version {@code
		 * 2.8.0}
		 */
		original28OrCompat(false, false),
		/** HiTS modified cellHTS2 after version {@code 2.8.0} */
		modified28OrCompat(true, false),
		/**
		 * Original cellHTS2 without modifications, at least version {@code
		 * 2.10.0}
		 */
		original210OrCompat(false, false, false),
		/** HiTS modified cellHTS2 after version {@code 2.10.0} */
		modified210OrCompat(true, false, false);

		private final boolean modified, pre28;
		private final boolean pre210;

		private CellHTS2Version(final boolean modified, final boolean pre28) {
			this(modified, pre28, true);
		}

		private CellHTS2Version(final boolean modified, final boolean pre28,
				final boolean pre210) {
			this.modified = modified;
			this.pre28 = pre28;
			this.pre210 = pre210;
		}

		public boolean isModified() {
			return modified;
		}

		public boolean isPre28() {
			return pre28;
		}

		/**
		 * @return The pre210.
		 */
		public boolean isPre210() {
			return pre210;
		}
	}

	/**
	 * Constructor for the node model.
	 */
	protected CellHTS2NodeModel() {
		super(4, 5);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
			final ExecutionContext exec) throws Exception {
		final String[] normMethods = computeNormMethods();
		outDirs = computeOutDirs(normMethods);
		if (generateHTML.getBooleanValue()) {
			checkFolders();
		}
		final String experimentName = experimentNameModel.getStringValue();
		final RConnection conn;
		try {
			conn = new RConnection(/* "127.0.0.1", 1099, 10000 */);
		} catch (final RserveException e) {
			CellHTS2NodeModel.logger.fatal(
					"Failed to connect to Rserve, please start again.", e);
			throw e;
		}
		// if (!Rengine.versionCheck()) {
		// System.err
		// .println("** Version mismatch - Java files don't match library
		// version.");
		// throw new IllegalStateException("Wrong R version.");
		// }
		// final Rengine conn = new Rengine();
		try {
			if (!conn.isConnected()) {
				CellHTS2NodeModel.logger
						.fatal("Not connected to the Rserve, please restart it if possible.");
				throw new IllegalStateException("No connection to R.");
			}
			final REXPString versionExpr;
			try {
				RUtil.voidEval(conn, "library(\"Biobase\")");
				versionExpr = RUtil.eval(conn, "package.version(\"cellHTS2\")");
			} catch (final RserveException e) {
				throw new IllegalStateException(
						"This R installation has no supported cellHTS2 installed.");
			}
			final String cellHTS2Version = versionExpr.asString();
			CellHTS2Version version;
			if (cellHTS2Version.matches("2\\.[46]\\.\\d*")) {
				version = CellHTS2Version.originalPre28;
			} else if (cellHTS2Version.matches("2\\.8\\.\\d*")) {
				version = CellHTS2Version.original28OrCompat;
			} else if (cellHTS2Version.matches("2\\.10\\.\\d*")) {
				version = CellHTS2Version.original210OrCompat;
			} else {
				throw new IllegalStateException(
						"This R installation has no supported cellHTS2 installed.");
			}
			int replicateCount = 0;
			int plateCount = 0;
			int wellRowCount = 0;
			int wellColCount = 0;
			final int plateIdx = CellHTS2NodeModel.getIndex(inData,
					PublicConstants.PLATE_COL_NAME);
			final int replicateIdx = CellHTS2NodeModel.getIndex(inData,
					PublicConstants.REPLICATE_COL_NAME);
			final int wellIdx = CellHTS2NodeModel.getIndex(inData,
					PublicConstants.WELL_COL_NAME);
			for (final DataRow dataRow : inData[0]) {
				final int plate = ((IntCell) dataRow.getCell(plateIdx))
						.getIntValue();
				final int replicate = ((IntCell) dataRow.getCell(replicateIdx))
						.getIntValue();
				final String wellId = ((StringCell) dataRow.getCell(wellIdx))
						.getStringValue();
				replicateCount = Math.max(replicateCount, replicate);
				plateCount = Math.max(plateCount, plate);
				wellRowCount = Math.max(wellRowCount,
						wellId.charAt(0) - 'A' + 1);
				wellColCount = Math.max(wellColCount, Integer.parseInt(wellId
						.substring(1)));
			}
			exec.checkCanceled();
			final int wellCount = wellRowCount * wellColCount;
			final int paramCount = parametersModel.getIncludeList().size();
			CellHTS2NodeModel.logger.debug(wellRowCount + "x" + wellColCount
					+ "  " + plateCount + " plate, " + replicateCount
					+ " replicates, " + paramCount + " channels");
			if (wellCount * replicateCount * plateCount != inData[0]
					.getRowCount()) {
				final String errorMessage = "There are wrong number of input rows, please check them:\nfound: "
						+ inData[0].getRowCount()
						+ ", while expected: "
						+ plateCount
						+ "*"
						+ replicateCount
						+ "*"
						+ wellRowCount
						+ "*"
						+ wellColCount
						+ "="
						+ wellCount
						* replicateCount * plateCount;
				CellHTS2NodeModel.logger.fatal(errorMessage);
				throw new IllegalStateException(errorMessage);
			}
			sendRawValues(exec, conn, inData[0], replicateCount, plateIdx,
					replicateIdx, wellIdx, wellCount, paramCount);
			exec.setProgress(.001, "Data read.");
			convertRawInputToCellHTS2(experimentName, conn, replicateCount,
					plateCount, wellRowCount, wellColCount, wellCount,
					parametersModel.getIncludeList(), version);
			exec.checkCanceled();
			exec.setProgress(.005, "Initial object created.");

			try {
				addMiame(inData, conn);
				exec.setProgress(.006, "MIAME information added.");
			} catch (final Exception e) {
				CellHTS2NodeModel.logger.warn(
						"Failed to add MIAME information.\n" + e.getMessage(),
						e);
			}
			exec.checkCanceled();
			version = setupCellHTS2Extensions(conn, version);
			double completed = 0.01;
			CellHTS2NodeModel.logger.debug(RUtil.eval(conn, "state(x)"));
			exec.checkCanceled();
			final String additionalParams;
			switch (version) {
			case modified28OrCompat:
			case modified210OrCompat:
				additionalParams = ", colOrder=c(" + createColOrderString()
						+ ")";
				break;
			case modifiedPre28:
				additionalParams = ", channels="
						+ createChannelList(parametersModel.getIncludeList())
						+ ", colOrder=c(" + createColOrderString() + ")";
				break;
			case original28OrCompat:
			case originalPre28:
			case original210OrCompat:
				additionalParams = "";
				break;
			default:
				throw new IllegalStateException("Unsupported version");
			}
			final BufferedDataContainer scores = exec
					.createDataContainer(new DataTableSpec(computeTopTableSpec(
							inData[0].getDataTableSpec(), false)));
			scores.setMaxPossibleValues(Format._1536.getWellCount() + 1);
			final BufferedDataContainer replicates = exec
					.createDataContainer(new DataTableSpec(computeTopTableSpec(
							inData[0].getDataTableSpec(), true)));
			replicates.setMaxPossibleValues(Format._1536.getWellCount() + 1);
			final BufferedDataContainer aggregate = exec
					.createDataContainer(CellHTS2NodeModel.aggregateValuesSpec);
			for (final String normalise : normMethods) {
				exec.checkCanceled();
				exec.setProgress(completed, normalise);
				performNormalisation(exec, conn, inData,
						normMethods.length > 1, experimentName, version,
						replicateCount, plateCount, plateIdx, replicateIdx,
						wellCount, additionalParams, scores, replicates,
						aggregate, normalise);
				completed += (1.0 - .01) / normMethods.length;
			}
			if (generateHTML.getBooleanValue()) {
				writeSelectionHtml(experimentName, normMethods);
				for (final String normMethod : normMethods) {
					CellHTS2NodeModel.writePlateList(outDirs.get(normMethod)
							+ File.separatorChar + "in", plateCount,
							replicateCount, selectParameters(parametersModel
									.getIncludeList(), version), version);
				}
			}
			// conn.shutdown();
			// conn.close();

			scores.close();
			replicates.close();
			aggregate.close();
			final BufferedDataContainer configuration = exec
					.createDataContainer(CellHTS2NodeModel.configurationSpec);
			for (final DataRow row : inData[2]) {
				configuration.addRowToTable(row);
			}
			for (final String normMethod : normMethods) {
				final String category = "Configuration " + normMethod;
				for (final Pair<String, String> pair : new Pair[] {
						Pair.apply("normalisation", normMethod),
						Pair.apply("log transform?", String
								.valueOf(logTransformModel.getBooleanValue())),
						Pair.apply("multiplicative?", isMultiplicativeModel
								.getBooleanValue() ? "multiplicative"
								: "additive"),
						Pair.apply("variance adjustment", scaleModel
								.getStringValue()),
						Pair.apply("score", scoreModel.getStringValue()),
						Pair
								.apply("summarise", summariseModel
										.getStringValue()) }) {
					configuration.addRowToTable(new DefaultRow(category + "_"
							+ pair.getLeft(), category, pair.getLeft(), pair
							.getRight()));
				}
			}
			configuration.close();
			final BufferedDataContainer outputFolders = exec
					.createDataContainer(CellHTS2NodeModel.outputFolderSpec);
			if (generateHTML.getBooleanValue()) {
				int i = 0;
				for (final String outFolder : outDirs.values()) {
					outputFolders.addRowToTable(new DefaultRow(new RowKey(
							String.valueOf(++i)), new StringCell(outFolder)));
				}
			}
			outputFolders.close();
			return new BufferedDataTable[] { scores.getTable(),
					replicates.getTable(), aggregate.getTable(),
					configuration.getTable(), outputFolders.getTable() /* out */};
		} finally {
			conn.close();
		}
	}

	/**
	 * Writes an HTML to be able to select the generated normalisation method if
	 * there were more than one.
	 * 
	 * @param experimentName
	 *            Name of experiment.
	 * @param normMethods
	 *            Normalisation methods.
	 * @throws IOException
	 *             Problem writing the HTML file.
	 */
	private void writeSelectionHtml(final String experimentName,
			final String[] normMethods) throws IOException {
		if (normMethods.length > 1) {
			final File file = new File(outputDirModel.getStringValue(),
					"index.html");
			if (file.exists() || !file.canWrite()) {
				logger.warn("Unable to write normalisation selection file: "
						+ file.getAbsolutePath());
				return;
			}
			final FileWriter writer = new FileWriter(file);
			try {
				writer.append("<HTML><HEAD><TITLE>Experiment report for \""
						+ experimentName + "\"</TITLE></HEAD>\n"
						+ "<BODY><CENTER><H1>Experiment report for \""
						+ experimentName + "\"</H1></CENTER>\n" + "\n" + "");
				writer.append("<table><th><td>method</td></th>");
				for (final String normMethod : normMethods) {
					writer.append("<tr><td>")

					.append(normMethod).append("</td><td><a href=\"").append(
							normMethod).append("/index.html\">").append(
							normMethod).append("</a></td></tr>");
				}
				writer.append("</table></BODY></HTML>");
			} finally {
				writer.close();
			}
		}
	}

	/**
	 * The normalisation with a selected normalisation method ({@code normalise}
	 * ).
	 * 
	 * @param exec
	 *            The {@link ExecutionMonitor}.
	 * @param conn
	 *            {@link RConnection} to Rserve.
	 * @param inData
	 *            Input tables to plate configuration and annotation.
	 * @param multipleNormMethods
	 *            More than one norm methods?
	 * @param experimentName
	 *            Name of experiment.
	 * @param version
	 *            Used cellHTS2 version.
	 * @param replicateCount
	 *            Number of replicates.
	 * @param plateCount
	 *            Number of plates.
	 * @param plateIdx
	 *            Index of plate column in first table.
	 * @param replicateIdx
	 *            Index of replicate column in first table.
	 * @param wellCount
	 *            Number of wells on a plate.
	 * @param additionalParams
	 *            Additional parameters for some Rserve commands.
	 * @param scores
	 *            First output table.
	 * @param replicates
	 *            Second output table.
	 * @param aggregate
	 *            Third output table.
	 * @param normalise
	 *            Method of normalisation.
	 * @throws Exception
	 *             Error occurred.
	 */
	private void performNormalisation(final ExecutionMonitor exec,
			final RConnection conn, final BufferedDataTable[] inData,
			final boolean multipleNormMethods, final String experimentName,
			final CellHTS2Version version, final int replicateCount,
			final int plateCount, final int plateIdx, final int replicateIdx,
			final int wellCount, final String additionalParams,
			final BufferedDataContainer scores,
			final BufferedDataContainer replicates,
			final BufferedDataContainer aggregate, final String normalise)
			throws Exception {
		try {
			plateConfiguration(inData[1], inData[2], inData[3], conn,
					wellCount, plateCount, normalise);
		} catch (final Exception e) {
			CellHTS2NodeModel.logger.fatal(
					"Unable to set the configuration of the plates", e);
			throw e;
		}
		try {
			normalisePlates(conn, normalise);
		} catch (final Exception e) {
			CellHTS2NodeModel.logger
					.fatal("Problem with normalization step", e);
			throw e;
		}
		exec.checkCanceled();
		try {
			annotate(conn, inData[0], replicateIdx, plateIdx);
		} catch (final Exception e) {
			CellHTS2NodeModel.logger.warn("Annotation failed.", e);
		}
		exec.checkCanceled();
		try {
			addOverallStatistics(conn, plateCount, replicateCount, aggregate,
					normalise, parametersModel.getIncludeList());
		} catch (final Exception e) {
			CellHTS2NodeModel.logger
					.warn("Problem querying overall statistics. "
							+ e.getMessage(), e);
		}
		exec.checkCanceled();
		final String outDir = outDirs.get(normalise);
		final String zRange = scoreRange.getMinRange() + ", "
				+ scoreRange.getMaxRange();

		if (parametersModel.getIncludeList().size() == 1
				|| version.isModified()
				|| scoreModel.getStringValue().equals("none")) {
			try {
				RUtil.voidEval(conn,
						"xsc <- scoreReplicates(xn, sign=\"+\", method=\""
								+ scoreModel.getStringValue() + "\")");
			} catch (final Exception e) {
				CellHTS2NodeModel.logger.fatal("scoring the replicates failed",
						e);
				throw e;
			}
			exec.checkCanceled();
			try {
				RUtil
						.voidEval(
								conn,
								"xsc <- summarizeReplicates(xsc, summary=\""
										+ summariseModel.getStringValue()
										+ "\""
										+ (version.isModified() ? ", method=\"per-channel\""
												: "") + ")");
			} catch (final Exception e) {
				CellHTS2NodeModel.logger.fatal(
						"Summarizing the replicates failed.", e);
				throw e;
			}
			try {
				if (generateHTML.getBooleanValue()) {
					RUtil
							.voidEval(
									conn,
									"writeReport("
											+ (version.isPre28() ? ""
													: "raw=x, normalized=xn, scored=xsc, ")
											+ "cellHTSlist=list(\"raw\"=x, \"normalized\"=xn, \"scored\"=xsc),\n"
											+ "   force=TRUE, plotPlateArgs = TRUE,\n"
											+ "   imageScreenArgs = list(zrange=c("
											+ zRange + "), ar="
											+ aspectRatioModel.getDoubleValue()
											+ "), map=TRUE, outdir=\"" + outDir
											+ "\"" + additionalParams + ")");
				}
				// conn.voidEval("writeTab(xsc, file=\"scores.txt\")");
				exec.checkCanceled();
			} catch (final Exception e) {
				CellHTS2NodeModel.logger.fatal("Problem writing the results:\n"
						+ conn.getLastError(), e);
				throw e;
			}
			final File tempFile = version.isPre28() ? File.createTempFile(
					"topTable", ".txt") : null;
			final String topTableGenerate = "getTopTable(cellHTSlist=list(\"raw\"=x, \"normalized\"=xn, \"scored\"=xsc), file="
					+ (version.isPre28() ? "\""
							+ tempFile.getAbsolutePath().replace('\\', '/')
							+ "\"" : "NULL")
					+ (version.isModified() && !version.isPre28() ? ", channels="
							+ createChannelList(selectParameters(
									parametersModel.getIncludeList(), version))
							+ ", orderBy=\""
							+ parametersModel.getIncludeList().get(0) + "\""
							: "") + additionalParams + ")";
			logger.debug(topTableGenerate);
			final REXP topTable = RUtil.eval(conn, topTableGenerate);
			final int tableLength = ((REXPVector) topTable.asList().get(0))
					.length();
			if (tempFile != null && !tempFile.delete()) {
				tempFile.deleteOnExit();
			}
			exec.checkCanceled();
			final List<PossibleStatistics> stats = ColumnSelectionFieldEditor
					.parseString(PossibleStatistics.class, ImporterNodePlugin
							.getDefault().getPreferenceStore().getString(
									PreferenceConstants.RESULT_COL_ORDER));
			final DataColumnSpec[] additionalColumns = selectAdditionalColumns(inData[0]
					.getDataTableSpec());
			final StringCell logTransformCell = new StringCell(
					logTransformModel.getBooleanValue() ? "log" : "");
			final StringCell multCell = new StringCell(isMultiplicativeModel
					.getBooleanValue() ? "multiplicative" : "additive");
			final StringCell varianceAdjustmentCell = new StringCell(scaleModel
					.getStringValue());
			final StringCell scoreCell = new StringCell(scoreModel
					.getStringValue());
			final StringCell normCell = new StringCell(normalise);
			final StringCell summariseCell = new StringCell(summariseModel
					.getStringValue());
			final StringCell experimentCell = new StringCell(experimentName);
			for (int row = 0; row < tableLength; ++row) {
				final List<DataCell> values = new ArrayList<DataCell>();
				values.add(experimentCell);
				values.add(normCell);
				values.add(logTransformCell);
				values.add(multCell);
				values.add(varianceAdjustmentCell);
				values.add(scoreCell);
				values.add(summariseCell);
				CellHTS2NodeModel.computeTableValue(topTable.asList(), row,
						replicateCount, Collections.singletonList(values),
						false, false, stats, null, parametersModel
								.getIncludeList(), additionalColumns);
				scores
						.addRowToTable(new DefaultRow(Misc.addTrailing(
								((REXPInteger) topTable.asList().get("plate"))
										.asIntegers()[row], 3)
								+ "_"
								+ ((REXPString) topTable.asList().get("well"))
										.asStrings()[row]
								+ (multipleNormMethods ? "_" + normalise : ""),
								values));
				if (row % 50 == 0) {
					exec.checkCanceled();
				}
			}
			for (int row = 0; row < tableLength; ++row) {
				final List<List<DataCell>> rows = new ArrayList<List<DataCell>>(
						replicateCount);
				for (int i = replicateCount; i-- > 0;) {
					final List<DataCell> values = new ArrayList<DataCell>();
					values.add(experimentCell);
					values.add(normCell);
					values.add(logTransformCell);
					values.add(multCell);
					values.add(varianceAdjustmentCell);
					values.add(scoreCell);
					values.add(summariseCell);
					rows.add(values);
				}
				CellHTS2NodeModel.computeTableValue(topTable.asList(), row,
						replicateCount, rows, true, true, stats, null,
						parametersModel.getIncludeList(), additionalColumns);
				for (int repl = 0; repl < replicateCount; ++repl) {
					replicates.addRowToTable(new DefaultRow(Misc.addTrailing(
							((REXPInteger) topTable.asList().get("plate"))
									.asIntegers()[row], 3)
							+ "_"
							+ ((REXPString) topTable.asList().get("well"))
									.asStrings()[row]
							// (row + 1)
							+ "_"
							+ (repl + 1)
							+ (multipleNormMethods ? "_" + normalise : ""),
							rows.get(repl)));
				}
				if (row % 50 == 0) {
					exec.checkCanceled();
				}
			}
		} else {
			try {
				if (generateHTML.getBooleanValue()) {
					RUtil.voidEval(conn,
							"writeReport(cellHTSlist=list(\"raw\"=x, \"normalized\"=xn),\n"
									+ "   force=TRUE, plotPlateArgs = TRUE,\n"
									+ "   imageScreenArgs = list(zrange=c("
									+ zRange + "), ar=1), map=TRUE, outdir=\""
									+ outDir + "\"" + additionalParams + ")");
				}
			} catch (final Exception e) {
				CellHTS2NodeModel.logger
						.fatal("Problem writing the results", e);
				throw e;
			}
		}
		@SuppressWarnings("restriction")
		final File baseFile = ((org.eclipse.osgi.baseadaptor.BaseData) ((org.eclipse.osgi.framework.internal.core.BundleHost) ImporterNodePlugin
				.getDefault().getBundle()).getBundleData()).getBundleFile()
				.getBaseFile();
		if (normalise.toLowerCase().startsWith("custom")) {
			final File fileToCopy = new File(baseFile,
					"bin/r/customNormalisation.R");
			EFS.getStore(fileToCopy.toURI()).copy(
					EFS.getLocalFileSystem().fromLocalFile(
							new File(outDir + "/in/" + fileToCopy.getName())),
					EFS.OVERWRITE, null);
		}
		if (scoreModel.getStringValue().toLowerCase().startsWith("custom")) {
			final File fileToCopy = new File(baseFile, "bin/r/customScoring.R");
			EFS.getStore(fileToCopy.toURI()).copy(
					EFS.getLocalFileSystem().fromLocalFile(
							new File(outDir + "/in/" + fileToCopy.getName())),
					EFS.OVERWRITE, null);
		}
	}

	/**
	 * @param conn
	 *            Connection to Rserve.
	 * @param version
	 *            The previous {@link CellHTS2Version} value.
	 * @return The new {@link CellHTS2Version} value.
	 * @throws REXPMismatchException
	 *             If cannot parse the error message.
	 */
	@SuppressWarnings("restriction")
	@edu.umd.cs.findbugs.annotations.SuppressWarnings("SF")
	private CellHTS2Version setupCellHTS2Extensions(final RConnection conn,
			CellHTS2Version version) throws REXPMismatchException {
		if (ImporterNodePlugin.getDefault().getPreferenceStore().getBoolean(
				PreferenceConstants.USE_TCD_EXTENSIONS)) {
			try {
				final File rSourcesDir = new File(
						((org.eclipse.osgi.baseadaptor.BaseData) ((org.eclipse.osgi.framework.internal.core.BundleHost) ImporterNodePlugin
								.getDefault().getBundle()).getBundleData())
								.getBundleFile().getBaseFile(), "bin/r/");
				final File rSpecDir = new File(rSourcesDir,
						(version.isPre28() ? "2.7" : version.isPre210() ? "2.8"
								: "2.10"));
				if (Boolean.parseBoolean(System.getProperty(PROPERTY_EXPERT))) {
					RUtil.voidEval(conn, "setwd(\""
							+ rSourcesDir.getAbsolutePath().replace('\\', '/')
							+ "\")");
					RUtil.voidEval(conn, "source(\"perPlateScaling.R\")\n");
					RUtil.voidEval(conn, "source(\"adjustVariance.R\")\n");
					RUtil.voidEval(conn, "source(\"normalizePlates.R\")\n");
					RUtil.voidEval(conn, "source(\"customNormalisation.R\")\n");
					RUtil.voidEval(conn, "source(\"customScoring.R\")\n");
				}
				RUtil
						.voidEval(conn, "setwd(\""
								+ rSpecDir.getAbsolutePath().replace('\\', '/')
								+ "\")");
				RUtil.voidEval(conn, "library(\"cellHTS2\")");
				RUtil.voidEval(conn, "source(\"getMeasureRepAgreement.R\")");
				switch (version) {
				case originalPre28:
					RUtil.voidEval(conn, "source(\"summarizeReplicates.R\")\n");
					RUtil.voidEval(conn, "source(\"getTopTable.R\")\n");
					RUtil.voidEval(conn, "source(\"getDynamicRange.R\")\n");
					RUtil.voidEval(conn, "source(\"getZfactor.R\")\n");
					RUtil.voidEval(conn, "source(\"checkControls.R\")\n");
					RUtil.voidEval(conn, "source(\"makePlot.R\")\n");
					RUtil.voidEval(conn, "source(\"QMbyPlate.R\")\n");
					RUtil.voidEval(conn, "source(\"QMexperiment.R\")\n");
					RUtil.voidEval(conn, "source(\"imageScreen.R\")\n");
					RUtil.voidEval(conn, "source(\"writeReport.R\")");
					version = CellHTS2Version.modifiedPre28;
					break;
				case original210OrCompat:
					RUtil.voidEval(conn, "source(\"settings.R\")");
				case original28OrCompat:
					RUtil.voidEval(conn, "library(\"prada\")");
					RUtil.voidEval(conn, "source(\"summarizeReplicates.R\")\n");
					RUtil.voidEval(conn, "source(\"getTopTable.R\")\n");
					RUtil.voidEval(conn, "source(\"checkControls.R\")\n");
					RUtil.voidEval(conn, "source(\"progressReport.R\")\n");
					RUtil.voidEval(conn, "source(\"makePlot.R\")\n");
					RUtil.voidEval(conn, "source(\"AllGeneric.R\")\n");
					RUtil.voidEval(conn, "source(\"AllClasses.R\")\n");
					RUtil.voidEval(conn, "source(\"glossary.R\")\n");
					RUtil.voidEval(conn, "source(\"writeHTML-methods.R\")\n");
					RUtil.voidEval(conn, "source(\"plateConfModule.R\")\n");
					RUtil.voidEval(conn, "source(\"plateListModule.R\")\n");
					RUtil
							.voidEval(conn,
									"source(\"plateSummariesModule.R\")\n");
					RUtil.voidEval(conn, "source(\"imageScreen.R\")\n");
					RUtil.voidEval(conn,
							"source(\"screenDescriptionModule.R\")\n");
					RUtil.voidEval(conn, "source(\"screenResultsModule.R\")\n");
					RUtil.voidEval(conn, "source(\"screenScriptModule.R\")\n");
					RUtil.voidEval(conn, "source(\"screenSummaryModule.R\")\n");
					RUtil.voidEval(conn, "source(\"writeReport.R\")\n");
					version = version == CellHTS2Version.original210OrCompat ? CellHTS2Version.modified210OrCompat
							: CellHTS2Version.modified28OrCompat;
					break;
				default:
					throw new IllegalStateException("Not supported state.");
				}
				CellHTS2NodeModel.logger
						.info("Using the improved version of cellHTS2.");
			} catch (final RserveException e) {
				CellHTS2NodeModel.logger.warn(
						"Using the original version of cellHTS2, because a problem has occured: "
								+ e.getMessage(), e);
			}
		}
		return version;
	}

	/**
	 * Sends the raw data to Rserve. The name of the vector is {@code rawInput}.
	 * 
	 * @param exec
	 *            The {@link ExecutionMonitor}.
	 * @param conn
	 *            Connection to Rserve.
	 * @param inData
	 *            First input data table.
	 * @param replicateCount
	 *            Number of replicates.
	 * @param plateIdx
	 *            Index of plate column.
	 * @param replicateIdx
	 *            Index of replicate column.
	 * @param wellIdx
	 *            Index of well column.
	 * @param wellCount
	 *            Number of wells on a plate.
	 * @param paramCount
	 *            Parameter count.
	 * @throws CanceledExecutionException
	 *             User cancelled the execution.
	 */
	private void sendRawValues(final ExecutionMonitor exec,
			final RConnection conn, final BufferedDataTable inData,
			final int replicateCount, final int plateIdx,
			final int replicateIdx, final int wellIdx, final int wellCount,
			final int paramCount) throws CanceledExecutionException {
		final double[] rawValues = new double[inData.getRowCount() * paramCount];
		// final int i = 0;
		// final HashSet<String> paramSet = new HashSet<String>(
		// parametersModel.getIncludeList());
		final Map<String, Integer> paramIndices = new HashMap<String, Integer>();
		{
			final DataTableSpec tableSpec = inData.getDataTableSpec();
			for (int i = tableSpec.getNumColumns(); i-- > 0;) {
				paramIndices.put(tableSpec.getColumnSpec(i).getName(), Integer
						.valueOf(i));
			}
		}
		for (final DataRow dataRow : inData) {
			// final Iterator<DataCell> it = dataRow.iterator();
			final int plate = ((IntCell) /* it.next() */dataRow
					.getCell(plateIdx)).getIntValue() - 1;
			final int replicate = ((IntCell) /* it.next() */dataRow
					.getCell(replicateIdx)).getIntValue() - 1;
			final String wellId = ((StringCell) /* it.next() */dataRow
					.getCell(wellIdx)).getStringValue();
			final int well = Format._96.convertWellToPosition(wellId);
			// getWellNumber(wellId);
			{
				int j = 0;
				for (final String colName : parametersModel.getIncludeList()) {
					final int colIndex = paramIndices.get(colName).intValue();
					final DataCell cell = colIndex < 0 ? DataType
							.getMissingCell() : dataRow.getCell(colIndex);
					rawValues[plate * replicateCount * paramCount * wellCount
							+ replicate * paramCount * wellCount + j++
							* wellCount + well] = cell instanceof DoubleValue ? ((DoubleValue) cell)
							.getDoubleValue()
							: Double.NaN;
				}
			}
		}
		exec.checkCanceled();
		// conn.assign("input", new REXPGenericVector(list));
		try {
			conn.assign("rawInput", rawValues);
		} catch (final REngineException e) {
			CellHTS2NodeModel.logger.fatal(
					"Failed to send the raw values to R.", e);
		}
	}

	/**
	 * Checks whether the {@link #outDirs output folders} are existing or not.
	 * Asks the user about what to do.
	 * <p>
	 * Also checks the txt files for writable attribute.
	 * 
	 * @see #checkTxtFiles(File)
	 */
	private void checkFolders() {
		for (final String fileName : outDirs.values()) {
			if (new File(fileName).exists()) {
				final int result = JOptionPane
						.showOptionDialog(
								null,
								"(One of) selected folder(s) ("
										+ fileName
										+ ") is already exists.\n"
										+ "Do you want to overwrite the previous results, or interrupt the execution of this node?\n"
										+ "(The folder will not be deleted, the not overwritten files will remain there.)",
								"Overwrite existing folder?",
								JOptionPane.YES_NO_OPTION,
								JOptionPane.QUESTION_MESSAGE, null,
								new String[] { "Overwrite", "Interrupt" },
								"Interrupt");
				if (result == JOptionPane.NO_OPTION) {
					throw new IllegalArgumentException("Execution cancelled.");
				}
				checkTxtFiles(new File(fileName));
				break;
			}
		}
	}

	/**
	 * Checks whether the txt files are writable or not in the {@code dir}
	 * folder.
	 * 
	 * @param dir
	 *            An existing folder.
	 */
	private void checkTxtFiles(final File dir) {
		for (final File f : dir.listFiles()) {
			if (f.isDirectory()) {
				checkTxtFiles(f);
			}
			if (f.getName().toLowerCase().endsWith(".txt") && f.isFile()
					&& !f.canWrite()) {
				final int result = JOptionPane
						.showOptionDialog(
								null,
								"(One of) selected folder ("
										+ f.getAbsolutePath()
										+ ") is not writable. (Might be open in Excel.)\n"
										+ "Do you want to interrupt the execution of this node, or ignore (you solved) this problem?",
								"Overwrite existing folder?",
								JOptionPane.YES_NO_OPTION,
								JOptionPane.QUESTION_MESSAGE, null,
								new String[] { "Interrupt", "Ignore" },
								"Interrupt");
				if (result == JOptionPane.YES_OPTION) {
					throw new IllegalArgumentException("Execution interrupted.");
				}
				if (!f.canWrite()) {
					logger
							.warn(f.getAbsolutePath()
									+ " is still not writable.");
				}
			}
		}
	}

	/**
	 * Computes the order of parameters in the output.
	 * 
	 * @param includeList
	 *            A list of parameters.
	 * @param version
	 *            The CellHTS2 version used.
	 * @return A list of {@code includeList} values ordered alphabetically if
	 *         {@code version} is {@link CellHTS2Version#modified28OrCompat},
	 *         else the original {@code includeList}.
	 */
	private List<String> selectParameters(final List<String> includeList,
			final CellHTS2Version version) {
		switch (version) {
		case modified28OrCompat:
		case modified210OrCompat:
			final List<String> ret = new ArrayList<String>(includeList);
			Collections.sort(ret, String.CASE_INSENSITIVE_ORDER);
			return ret;
		case modifiedPre28:
		case original28OrCompat:
		case originalPre28:
		case original210OrCompat:
			return includeList;

		default:
			throw new IllegalStateException("Not supported cellHTS2 version: "
					+ version);
		}
	}

	private static int getIndex(final BufferedDataTable[] inData,
			final String colName) {
		final int plateIdx = inData[0].getDataTableSpec().findColumnIndex(
				colName);
		assert plateIdx != -1 : "No " + colName + " column";
		return plateIdx;
	}

	/**
	 * Computes the overall statistics. This will go to the 3<sup>rd</sup>
	 * outport.
	 * 
	 * @param conn
	 *            The {@link RConnection} for Rserve. It must contain the scored
	 *            CellHTS object in the {@code xsc} variable.
	 * @param plateCount
	 *            The number of plates.
	 * @param replicateCount
	 *            The number of replicates.
	 * @param aggregate
	 *            The aggregated values container. This is where the results go.
	 * @param normalise
	 *            The normalisation method.
	 * @param parameters
	 *            The parameters used.
	 * @throws RserveException
	 *             There was a problem with the execution on Rserve.
	 * @throws REXPMismatchException
	 *             We had a problem parsing the results.
	 */
	private void addOverallStatistics(final RConnection conn,
			final int plateCount, final int replicateCount,
			final BufferedDataContainer aggregate, final String normalise,
			final List<String> parameters) throws RserveException,
			REXPMismatchException {
		final REXP dynRanges = RUtil.eval(conn,
				"getDynamicRange(xn, verbose=FALSE, posControls=as.vector(rep(\"^pos$\", "
						+ parameters.size()
						+ ")), negControls=as.vector(rep(\"^neg$\", "
						+ parameters.size() + ")))");
		final REXP repMeasures = RUtil.eval(conn,
				"getMeasureRepAgreement(xn, corr.method=\"spearman\")");
		final REXP allZFactor = RUtil.eval(conn,
				"getZfactor(xn, verbose=FALSE, posControls=as.vector(rep(\"^pos$\", "
						+ parameters.size()
						+ ")), negControls=as.vector(rep(\"^neg$\", "
						+ parameters.size() + ")))");
		final double[] corrCoeffMin = replicateCount == 1 ? new double[parameters
				.size()
				* plateCount]
				: ((REXPDouble) repMeasures
						.asList()
						.get(
								repMeasures.asList().names.contains("corrCoef") ? "corrCoef"
										: "corrCoef.min")).asDoubles();
		final double[] corrCoeffMax = replicateCount == 1 ? new double[parameters
				.size()
				* plateCount]
				: ((REXPDouble) repMeasures
						.asList()
						.get(
								repMeasures.asList().names.contains("corrCoef") ? "corrCoef"
										: "corrCoef.max")).asDoubles();
		if (replicateCount == 1) {
			Arrays.fill(corrCoeffMin, Double.NaN);
			Arrays.fill(corrCoeffMax, Double.NaN);
		}
		final StringCell experimentCell = new StringCell(experimentNameModel
				.getStringValue());
		final StringCell normaliseCell = new StringCell(normalise);
		final StringCell multiplicativeCell = new StringCell(
				isMultiplicativeModel.getBooleanValue() ? "multiplicative"
						: "additive");
		final StringCell logTransformCell = new StringCell(logTransformModel
				.getBooleanValue() ? "log" : "");
		final StringCell varianceAdjustmentCell = new StringCell(scaleModel
				.getStringValue());
		final StringCell scoreCell = new StringCell(scoreModel.getStringValue());
		final StringCell summariseCell = new StringCell(summariseModel
				.getStringValue());
		for (int plate = 0; plate < plateCount; ++plate) {
			for (int repl = 0; repl < replicateCount; ++repl) {
				for (int param = 0; param < parameters.size(); ++param) {
					final List<DataCell> values = new ArrayList<DataCell>();
					values.add(experimentCell);
					values.add(normaliseCell);
					values.add(logTransformCell);
					values.add(multiplicativeCell);
					values.add(varianceAdjustmentCell);
					values.add(scoreCell);
					values.add(summariseCell);
					values.add(new IntCell(plate + 1));
					values.add(new IntCell(repl + 1));
					values.add(new StringCell(parameters.get(param)));
					final Object possDynRange = dynRanges.asList().get(0);
					if (possDynRange instanceof REXPDouble) {
						final REXPDouble dynRangesExp = (REXPDouble) possDynRange;
						final double[] range = dynRangesExp.asDoubles();
						values.add(new DoubleCell(range[param
								* (replicateCount + 1) * plateCount + repl
								* plateCount + plate]));
						values.add(new DoubleCell(range[param * plateCount
								* (replicateCount + 1) + replicateCount
								* plateCount + plate]));
					} else {
						values.add(DataType.getMissingCell());
						values.add(DataType.getMissingCell());
					}
					values.add(replicateCount == 1 ? DataType.getMissingCell()
							: new DoubleCell(((REXPDouble) repMeasures.asList()
									.get("repStDev")).asDoubles()[param
									* plateCount + plate]));
					final double coeffMin = corrCoeffMin[param * plateCount
							+ plate];
					values.add(Double.isNaN(coeffMin) ? DataType
							.getMissingCell() : new DoubleCell(coeffMin));
					final double coeffMax = corrCoeffMax[param * plateCount
							+ plate];
					values.add(Double.isNaN(coeffMax) ? DataType
							.getMissingCell() : new DoubleCell(coeffMax));
					final Object possZFactor = allZFactor.asList().get(0);
					if (possZFactor instanceof REXPDouble) {
						final REXPDouble zfactors = (REXPDouble) possZFactor;
						values.add(new DoubleCell(
								zfactors.asDoubles()[replicateCount * param
										+ repl]));
					} else {
						values.add(DataType.getMissingCell());
					}
					aggregate.addRowToTable(new DefaultRow(Misc.addTrailing(
							(plate + 1), 3)
							+ "_"
							+ (repl + 1)
							+ "_"
							+ parameters.get(param)
							+ "_" + normalise, values));
				}
			}
		}
	}

	/**
	 * @return The string to pass to cellHTS2 to influence the column order of
	 *         the results.
	 */
	private String createColOrderString() {
		final List<PossibleStatistics> statsEnums = ColumnSelectionFieldEditor
				.<PossibleStatistics> parseString(
						PossibleStatistics.class,
						ImporterNodePlugin
								.getDefault()
								.getPreferenceStore()
								.getString(PreferenceConstants.RESULT_COL_ORDER));
		final StringBuilder sb = new StringBuilder();
		for (final PossibleStatistics stat : statsEnums) {
			if (stat != PossibleStatistics.REPLICATE) {
				sb.append('"').append(stat.getRCode()).append('"').append(", ");
			}
		}
		if (sb.length() > 0) {
			sb.setLength(sb.length() - 2);
		}
		return sb.toString();
	}

	/**
	 * Writes the plate list file to {@code dir}.
	 * 
	 * @param dir
	 *            Folder of the resulting {@code Platelist.txt}.
	 * @param plateCount
	 *            Number of plates.
	 * @param replicateCount
	 *            Number of replicates.
	 * @param parameters
	 *            The input parameters.
	 * @param version
	 *            CellHTS2 version
	 */
	private static void writePlateList(final String dir, final int plateCount,
			final int replicateCount, final List<String> parameters,
			final CellHTS2Version version) {
		try {
			final FileWriter writer = new FileWriter(new File(new File(dir),
					"Platelist.txt"));
			try {
				writer.write("Filename\tPlate\tReplicate\tChannel\r\n");
				for (int p = 1; p <= plateCount; ++p) {
					for (int r = 1; r <= replicateCount; ++r) {
						for (int ch = 1; ch <= parameters.size(); ++ch) {
							switch (version) {
							case modified28OrCompat:
							case original28OrCompat:
							case original210OrCompat:
							case modified210OrCompat:
								writer
										.write(String
												.format(
														"data-%03d-%d-%d.txt\t%1$d\t%2$d\t%4$s\n",
														Integer.valueOf(p),
														Integer.valueOf(r),
														Integer.valueOf(ch),
														parameters.get(ch - 1)));
								break;
							case modifiedPre28:
							case originalPre28:
								writer
										.write(String
												.format(
														"data-%03d-%d-%d.txt\t%1$d\t%2$d\t%3$d\n",
														Integer.valueOf(p),
														Integer.valueOf(r),
														Integer.valueOf(ch)));
								break;
							default:
								throw new UnsupportedOperationException(
										"Not supported cellHTS2 version");
							}
						}
					}
				}
			} finally {
				writer.close();
			}
		} catch (final IOException e) {
			CellHTS2NodeModel.logger.warn(
					"Unable to write Platelist.txt file to: " + dir + ".", e);
		}
	}

	private Map<String, String> computeOutDirs(final String[] normMethods) {
		final String pattern = folderPatternModel.getStringValue();
		final String baseOutDir = outputDirModel.getStringValue().replace('\\',
				'/');
		final List<String> params = parametersModel.getIncludeList();
		final String experiment = experimentNameModel.getStringValue();
		final boolean isMultiplicative = isMultiplicativeModel
				.getBooleanValue();
		return CellHTS2NodeModel.computeOutDirs(normMethods, pattern,
				baseOutDir.endsWith("/") ? baseOutDir : baseOutDir + "/",
				params, experiment, isMultiplicative, scaleModel
						.getStringValue(), scoreModel.getStringValue(),
				summariseModel.getStringValue(), logTransformModel
						.getBooleanValue() ? "log" : "");
	}

	/**
	 * Generates the possible output folder names based on the parameters.
	 * 
	 * @param normMethods
	 *            The normalisation methods you are interested in.
	 * @param pattern
	 *            The pattern of the folder generation. For further syntax
	 *            information see {@link CellHTS2NodeModel}.
	 * @param baseOutDir
	 *            The prefix of all folder names.
	 * @param params
	 *            The selected parameters (like Cell Area, ...)
	 * @param experiment
	 *            The name of the experiment.
	 * @param isMultiplicative
	 *            The value of the multiplicative or additive normalisation
	 *            parameter.
	 * @param adjustment
	 *            The variance adjustment option.
	 * @param score
	 *            The scoring method.
	 * @param aggregate
	 *            The summarisation method.
	 * @param log
	 *            The log transform option.
	 * @return A {@link Map} with keys from the {@code normMethods}, and values
	 *         as folder names (absolute, or relative).
	 */
	static Map<String, String> computeOutDirs(final String[] normMethods,
			final String pattern, final String baseOutDir,
			final List<String> params, final String experiment,
			final boolean isMultiplicative, final String adjustment,
			final String score, final String aggregate, final String log) {
		final Map<String, String> outDirs = new HashMap<String, String>();
		for (final String normMethod : normMethods) {
			outDirs.put(normMethod, baseOutDir.trim());
		}
		final StringBuilder sb = new StringBuilder();
		nextChar: for (int i = 0; i < pattern.length(); ++i) {
			final Map<String, String> news = new HashMap<String, String>();
			switch (pattern.charAt(i)) {
			case '{': {
				++i;
				for (; i < pattern.length(); ++i) {
					switch (pattern.charAt(i)) {
					case 'e':
						append(normMethods, experiment, news);
						break;
					case 'n':
						if (news.isEmpty() || normMethods.length == 1) {
							for (final String normMethod : normMethods) {
								news.put(normMethod, normMethod);
							}
						} else {
							throw new IllegalStateException("wrong pattern");
						}
						break;
					case 'v':
						append(normMethods, adjustment, news);
						break;
					case 's':
						append(normMethods, score, news);
						break;
					case 'a':
						append(normMethods, aggregate, news);
						break;
					case 'l':
						append(normMethods, log, news);
						break;
					case 'p':
						final Character sep = i + 1 < pattern.length()
								&& !Character.isDigit(pattern.charAt(i + 1))
								&& pattern.charAt(i + 1) != '}' ? Character
								.valueOf(pattern.charAt(++i)) : null;
						final StringBuilder parameters = new StringBuilder();
						for (final String param : params) {
							parameters.append(param.replaceAll(
									"[^a-zA-Z ,_0-9]+", ""));
							if (sep != null) {
								parameters.append(sep);
							}
						}
						if (parameters.length() > 0 && sep != null) {
							parameters.setLength(parameters.length() - 1);
						}
						if (news.isEmpty()) {
							for (final String normMethod : normMethods) {
								news.put(normMethod, parameters.toString()
										.trim());
							}
						} else {
							sb.append(parameters.toString().trim());
						}
						break;
					case '*':
						append(normMethods, isMultiplicative ? "" : "+", news);
						// sb.append(isMultiplicative ? "" : "+");
						// if (!news.isEmpty()) {
						// for (final String string : normMethods) {
						// outDirs.put(string, outDirs.get(string) + sb
						// + news.get(string).trim());
						// }
						// sb.setLength(0);
						// news.clear();
						// }
						break;
					case '}':
						if (!news.isEmpty()) {
							for (final String string : normMethods) {
								outDirs.put(string, outDirs.get(string) + sb
										+ news.get(string).trim());
							}
							sb.setLength(0);
							news.clear();
						}
						continue nextChar;
					default:
						int num = 0;
						boolean wasDigit = false;
						for (; pattern.length() > i
								&& Character.isDigit(pattern.charAt(i)); ++i) {
							num *= 10;
							num += pattern.charAt(i) - '0';
							wasDigit = true;
						}
						if (wasDigit) {
							--i;
							for (final String normMethod : normMethods) {
								String val = news.get(normMethod);
								if (val == null) {
									continue;
								}
								if (val.length() > num) {
									val = val.replaceAll("Cell", "");
								}
								if (val.length() > num) {
									val = val.replaceAll("Nuc ", "N");
								}
								if (val.length() > num) {
									val = val
											.replaceAll("[^a-zA-Z ,_0-9]+", "");
								}
								if (val.length() > num) {
									val = val.replaceAll(
											"([A-Z]+[a-z]{2})(:?[a-z])+", "$1");
								}
								if (val.length() > num) {
									val = val.substring(0, num);
								}
								news.put(normMethod, val.trim());
							}
						}
						break;
					}
				}
				if (news.isEmpty()) {
					for (final String string : normMethods) {
						outDirs.put(string, outDirs.get(string).concat(
								sb.toString()).trim());
					}
					sb.setLength(0);
				} else {
					for (final String string : normMethods) {
						outDirs.put(string, outDirs.get(string).concat(
								sb.toString()).concat(news.get(string)).trim());
					}
					news.clear();
					sb.setLength(0);
				}
			}
				break;
			case '\\':
				// append(normMethods, "/", sb, news, outDirs);
				sb.append('/');
				break;
			default:
				// append(normMethods, Character.toString(pattern.charAt(i)),
				// sb,
				// news, outDirs);
				sb.append(pattern.charAt(i));
				break;
			}
		}
		if (sb.length() > 0) {
			for (final Entry<String, String> entry : outDirs.entrySet()) {
				entry.setValue(entry.getValue().concat(sb.toString()));
			}
		}
		return outDirs;
	}

	/**
	 * @param normMethods
	 *            The normalisation methods to use.
	 * @param adjustment
	 *            The suffix to append after the {@code normMethods}.
	 * @param news
	 *            The map of actual news.
	 */
	private static void append(final String[] normMethods,
			final String adjustment, final Map<String, String> news) {
		if (news.isEmpty()) {
			// sb.append(adjustment);
			for (final String string : normMethods) {
				news.put(string, adjustment);
			}
		} else {
			for (final String string : normMethods) {
				// outDirs.put(string, outDirs.get(string) + sb
				// + news.get(string).trim() + adjustment);
				news.put(string, news.get(string).concat(adjustment));
			}
			// sb.setLength(0);
			// news.clear();
		}
	}

	private String[] computeNormMethods() {
		final String normMethod = normMethodModel.getStringValue();
		return CellHTS2NodeModel.computeNormMethods(normMethod);
	}

	/**
	 * @param normMethod
	 *            The selected normalisation method.
	 * @return For {@code every} it will return all possible normalisation
	 *         methods, else {@code normMethod}.
	 */
	static String[] computeNormMethods(final String normMethod) {
		final String[] normMethods;
		if (normMethod
				.equals(CellHTS2NodeModel.POSSIBLE_NORMALISATION_METHODS[0])) {
			normMethods = new String[CellHTS2NodeModel.POSSIBLE_NORMALISATION_METHODS.length - 1];
			System.arraycopy(CellHTS2NodeModel.POSSIBLE_NORMALISATION_METHODS,
					1, normMethods, 0, normMethods.length);
		} else {
			normMethods = new String[] { normMethod };
		}
		return normMethods;
	}

	/**
	 * Annotates the object {@code xn} using the information from the first
	 * inport (if it is present).
	 * 
	 * @param conn
	 *            Connection to <i>Rserve</i>.
	 * @param dataTable
	 *            A {@link BufferedDataTable}.
	 * @param replicateIdx
	 *            The index of replicate values column in {@code dataTable}.
	 * @param plateIdx
	 *            The index of plate values column in {@code dataTable}.
	 * @throws RserveException
	 *             Problem with <i>Rserve</i> session.
	 * @throws REXPMismatchException
	 *             Problem parsing the error message.
	 */
	private void annotate(final RConnection conn,
			final BufferedDataTable dataTable, final int replicateIdx,
			final int plateIdx) throws RserveException, REXPMismatchException {
		final String geneSymbolName = "GeneSymbol";
		final int geneSymbol = findCol(dataTable, geneSymbolName);
		RUtil.voidEval(conn, "  nrPlate <- max(plate(xn))");
		if (geneSymbol >= 0
				&& dataTable.getDataTableSpec().getColumnSpec(geneSymbol)
						.getType().equals(StringCell.TYPE)) {
			final StringBuilder sb = new StringBuilder(
					"geneIDs <- data.frame(Plate=c(");
			for (final DataRow row : dataTable) {
				if (((IntCell) row.getCell(replicateIdx)).getIntValue() == 1) {// first
					// replicate
					sb.append(((IntCell) row.getCell(plateIdx)).getIntValue())
							.append(", ");
				}
			}
			sb.setLength(sb.length() - 2);
			sb.append("), Well=rep(pWells, nrPlate");
			sb.append("), GeneID=");
			final String geneIDName = "GeneID";
			final int geneID = findCol(dataTable, geneIDName);
			if (geneID >= 0) {
				sb.append("c(");
				for (final DataRow dataRow : dataTable) {
					if (((IntCell) dataRow.getCell(replicateIdx)).getIntValue() == 1) {// first
						// replicate
						final DataCell cell = dataRow.getCell(geneID);
						sb
								.append('"')
								.append(
										cell instanceof StringCell ? ((StringCell) cell)
												.getStringValue()
												: cell instanceof IntCell ? Integer
														.toString(((IntCell) cell)
																.getIntValue())
														: "").append('"')
								.append(", ");
					}
				}
				sb.setLength(sb.length() - 2);
			} else {
				sb.append("rep(NA, " + dataTable.getRowCount());
			}
			sb.append("), GeneSymbol=c(");
			if (dataTable.getDataTableSpec().containsName(geneSymbolName)) {
				for (final DataRow row : dataTable) {
					if (((IntCell) row.getCell(replicateIdx)).getIntValue() == 1) {// first
						// replicate
						final DataCell cell = row.getCell(geneSymbol);
						sb
								.append('"')
								.append(
										cell instanceof StringCell ? ((StringCell) cell)
												.getStringValue().trim()
												: cell instanceof IntCell ? Integer
														.toString(((IntCell) cell)
																.getIntValue())
														: "").append('"')
								.append(", ");
					}
				}
			}
			sb.setLength(sb.length() - 2);
			sb.append("))");
			CellHTS2NodeModel.logger.debug(sb);
			RUtil.voidEval(conn, sb.toString());
			// conn.voidEval("geneIDs=conf");
			// conn
			// .voidEval(" if(any(nchar(geneIDs$Well)!=3)) \n"
			// + " stop(sprintf(\"Well IDs in the gene annotation must contain 1
			// letter and 2 digits. E.g. \'A02\'.\"))\n");
			// conn
			// .voidEval(" geneIDs = geneIDs[order(geneIDs$Plate,
			// geneIDs$Well),]");

			RUtil.voidEval(conn, "  nrWpP   <- prod(pdim(xn))");
			// conn
			// .voidEval(" if (!((nrow(geneIDs)==nrWpP*nrPlate) &&
			// all(convertWellCoordinates(geneIDs$Well,
			// pdim(xn))$num==rep(1:nrWpP, times=nrPlate)) &&\n"
			// + " all(geneIDs$Plate == rep(1:nrPlate, each=nrWpP))))\n"
			// + " stop(paste(\"Invalid input file \'\", geneIDFile, \"\':
			// expecting \", nrWpP*nrPlate,\n"
			// + " \" rows, one for each well and for each plate. Please see the
			// vignette for\",\n"
			// + " \" an example.\\n\", sep=\"\"))");
			RUtil
					.voidEval(
							conn,
							"  geneIDs <- geneIDs[, !c(names(geneIDs) %in% c(\"Plate\", \"Well\")), drop=FALSE]");
			RUtil
					.voidEval(conn,
							"  geneIDs[apply(geneIDs, 2, function(i) i %in% \"NA\")] <- NA \n");
			RUtil.voidEval(conn, "  fData(xn)[names(geneIDs)] <- geneIDs\n");
			RUtil.voidEval(conn,
					"  fvarMetadata(xn)[names(geneIDs),] <- names(geneIDs)");
			RUtil.voidEval(conn, "  xn@state[[\"annotated\"]] <- TRUE\n");
			RUtil.voidEval(conn, "  validObject(xn)");
		}
	}

	/**
	 * Finds the index of a column by name (case insensitive).
	 * 
	 * @param dataTable
	 *            A {@link DataTable}.
	 * @param colName
	 *            The name of the column to find.
	 * @return The index of the found column, or {@code -1} if none found.
	 */
	private int findCol(final DataTable dataTable, final String colName) {
		int colIndex = -1;
		int c = 0;
		for (final DataColumnSpec columnSpec : dataTable.getDataTableSpec()) {
			if (columnSpec.getName().equalsIgnoreCase(colName)) {
				colIndex = c;
				break;
			}
			++c;
		}
		// assert c >= 0;
		return colIndex;
	}

	/**
	 * Normalise the plates and creates the {@code xn} object from {@code x}.
	 * 
	 * @param conn
	 *            An {@link RConnection}.
	 * @param normalise
	 *            The normalisation method to use.
	 * @throws RserveException
	 *             Problem on server side.
	 * @throws REXPMismatchException
	 *             Problem getting the error message.
	 */
	private void normalisePlates(final RConnection conn, final String normalise)
			throws RserveException, REXPMismatchException {
		final String varianceAdjust = scaleModel.getStringValue().equals(
				CellHTS2NodeModel.POSSIBLE_SCALE[1]) ? "byPlate"
				: scaleModel.getStringValue().equals(
						CellHTS2NodeModel.POSSIBLE_SCALE[2]) ? "byExperiment"
						: "none";
		final String normMethod = normalise;
		final String scaleMethod = isMultiplicativeModel.getBooleanValue() ? "multiplicative"
				: "additive";
		RUtil.voidEval(conn, "  xn <- normalizePlates(x,\n" + "    scale=\""
				+ scaleMethod + "\",\n" + "    log="
				+ (logTransformModel.getBooleanValue() ? "TRUE" : "FALSE")
				+ ",\n" + "    method=\"" + normMethod + "\",\n"
				+ "    varianceAdjust=\"" + varianceAdjust + "\")");
	}

	/**
	 * Creates {@code x} from the raw input values.
	 * 
	 * @param experimentName
	 *            The experiment name.
	 * @param conn
	 *            An {@link RConnection}.
	 * @param replicateCount
	 *            Number of replicates.
	 * @param plateCount
	 *            Number of plates.
	 * @param wellRowCount
	 *            Number of rows on a plate.
	 * @param wellColCount
	 *            Number of columns on a plate.
	 * @param wellCount
	 *            Number of wells on a plate.
	 * @param parameters
	 *            The selected parameters.
	 * @param version
	 *            The {@link CellHTS2Version}.
	 * @throws RserveException
	 *             Problem on server side.
	 * @throws REXPMismatchException
	 *             Problem getting the error message.
	 */
	private void convertRawInputToCellHTS2(final String experimentName,
			final RConnection conn, final int replicateCount,
			final int plateCount, final int wellRowCount,
			final int wellColCount, final int wellCount,
			final List<String> parameters, final CellHTS2Version version)
			throws RserveException, REXPMismatchException {
		final int paramCount = parameters.size();
		try {
			RUtil.voidEval(conn, "xraw <- array(NA_real_, dim=c(" + wellCount
					+ ", " + plateCount + ", " + replicateCount + ", "
					+ paramCount + "))");
			final String command = "for (plate in 1:" + plateCount + ")\n"
					+ "  for (replicate in 1:" + replicateCount + ")\n"
					+ "    for (channel in 1:" + paramCount + ") {\n"
					+ "				start = (plate - 1) * (" + replicateCount
					* paramCount * wellCount + ") + " + "(replicate -1) * "
					+ paramCount * wellCount + " + (channel - 1) * "
					+ wellCount + " + 1\n" + "      xraw[1:" + wellCount
					+ ", plate, replicate, channel]=rawInput[start:(start+"
					+ wellCount + "-1)]}";
			CellHTS2NodeModel.logger.debug(command);
			RUtil.voidEval(conn, command);
			CellHTS2NodeModel.logger.debug(RUtil.eval(conn, "dim(xraw)")
					.toDebugString());
			final StringBuilder channels = new StringBuilder(5 + paramCount * 5);
			channels.append("c(");
			for (int i = 1; i <= paramCount; ++i) {
				switch (version) {
				case modified28OrCompat:
				case original28OrCompat:
				case modified210OrCompat:
				case original210OrCompat:
					channels.append("\"").append(parameters.get(i - 1));
					break;
				case modifiedPre28:
				case originalPre28:
					channels.append("\"ch").append(Misc.addTrailing(i, 3));
					break;
				default:
					break;
				}
				channels.append("\", ");
			}
			channels.setLength(channels.length() - ", ".length());
			channels.append(")");
			final String createDat = "  dat <- lapply(seq_len(" + paramCount
					+ "), function(ch) \n" + "    matrix(xraw[,,,ch], ncol="
					+ replicateCount + ", nrow=" + wellCount * plateCount
					+ "))";
			RUtil.voidEval(conn, createDat);
			final String setNames = "  names(dat) <- " + channels;
			RUtil.voidEval(conn, setNames);
		} catch (final RserveException e) {
			CellHTS2NodeModel.logger.fatal(
					"Problem occured preparing the data.", e);
		}
		try {
			RUtil.voidEval(conn, "library(\"cellHTS2\")");
		} catch (final RserveException e) {
			CellHTS2NodeModel.logger
					.fatal("Unable to load cellHTS2 library", e);
			throw e;
		}
		try {
			RUtil.voidEval(conn, "  adata <- do.call(\"assayDataNew\",\n"
					+ "    c(storage.mode=\"lockedEnvironment\", dat))");
			final String pdataCommand = "  pdata <- new(\"AnnotatedDataFrame\",\n"
					+ "    data = data.frame(replicate = seq_len("
					+ replicateCount
					+ "),\n"
					+ "                      assay = rep(\""
					+ experimentName
					+ "\", "
					+ replicateCount
					+ "),\n"
					+ "                      stringsAsFactors = FALSE),\n"
					+ "    varMetadata <- data.frame(\n"
					+ "         labelDescription = c(\"Replicate number\", \"Biological assay\"),\n"
					+ "         channel = factor(rep(\"_ALL_\", 2L), levels=c(names(dat), \"_ALL_\")),\n"
					+ "         row.names = c(\"replicate\", \"assay\"),\n"
					+ "         stringsAsFactors = FALSE))\n";
			CellHTS2NodeModel.logger.debug(pdataCommand);
			RUtil.voidEval(conn, pdataCommand);
			RUtil.voidEval(conn, "  dimPlate <- c(nrow=" + wellRowCount
					+ ", ncol=" + wellColCount + ")");
			RUtil.voidEval(conn, "  well <- convertWellCoordinates(seq_len("
					+ wellCount + "), pdim=dimPlate)$letnum");
			final String fdataCommand = "  fdata <- new(\"AnnotatedDataFrame\", \n"
					+ "    data = data.frame(plate = rep(seq_len("
					+ plateCount
					+ "), each="
					+ wellCount
					+ "),\n"
					+ "                      well = rep(well, "
					+ plateCount
					+ "), \n"
					+ "                      controlStatus = factor(rep(\"unknown\", "
					+ wellCount
					+ "*"
					+ plateCount
					+ ")),\n"
					+ "                      stringsAsFactors = FALSE), \n"
					+ "    varMetadata <- data.frame(\n"
					+ "      labelDescription = c(\"Plate number\", \"Well ID\", \"Well annotation\"),\n"
					+ "      row.names = c(\"plate\", \"well\", \"controlStatus\"),\n"
					+ "         stringsAsFactors = FALSE))";
			CellHTS2NodeModel.logger.debug(fdataCommand);
			RUtil.voidEval(conn, fdataCommand);

			// conn.eval("Filename = vector(mode=\"character\", length="
			// + (plateCount * replicateCount * paramCount) + ")");
			RUtil.voidEval(conn, "Filename <- rep(\"test\", " + plateCount
					* replicateCount * paramCount + ")");
			RUtil.voidEval(conn, "pd <- matrix(nrow=" + plateCount
					* replicateCount * paramCount + ", ncol=3)");
			RUtil.voidEval(conn,
					"  intensityFiles <- vector(mode=\"list\", length="
							+ plateCount * replicateCount * paramCount + ")");
			RUtil.voidEval(conn, "wells_internal<-vector(length=" + wellCount
					+ ")");
			RUtil.voidEval(conn, "for (i in 1:" + wellRowCount
					+ ") wells_internal[(" + wellColCount + " * (i -1) + 1):("
					+ wellColCount + "*(i-1)+" + wellColCount
					+ ")] = paste(LETTERS[i][rep(1, " + wellColCount
					+ ")], formatC(1:" + wellColCount
					+ ", flag=\"0\", width=2), sep=\"\")");
			final String fillFileNames = "for (pl in 1:"
					+ plateCount
					+ ")\n"
					+ "  for (re in 1:"
					+ replicateCount
					+ ")\n"
					+ "    for (ch in 1:"
					+ paramCount
					+ ")\n"
					+ "      Filename[[(pl-1) * "
					+ replicateCount
					* paramCount
					+ " + (re - 1) * "
					+ paramCount
					+ " + ch]] <- paste(paste(\"data\", formatC(pl, flag=\"0\", width=3), re, ch, sep=\"-\"), \"txt\", sep=\".\")";
			CellHTS2NodeModel.logger.debug(fillFileNames);
			RUtil.voidEval(conn, fillFileNames);
			// System.out.println(conn.eval(
			// "paste(capture.output(print(Filename)),collapse=\"\\n\")")
			// .asString());
			if (version.isPre28()) {
				RUtil.voidEval(conn, "channelOrder <- 1:" + parameters.size());
			} else {
				RUtil.voidEval(conn, "channelOrder <- order("
						+ createChannelList(parameters) + ")");
			}
			final String fillPd = "for (pl in 1:" + plateCount + ")"
					+ "  for (re in 1:" + replicateCount + ")"
					+ "    for (ch in 1:" + paramCount + ") {"
					+ "       row = (pl-1) * " + replicateCount * paramCount
					+ " + (re - 1) * " + paramCount + " + ch\n"
					+ "      pd[row,1] = pl\n" + "      pd[row,2] = re\n"
					+ "      pd[row,3] = ch\n"
					+ "      intensityFiles[[row]] <- paste(\""
					+ experimentName
					+ "\", wells_internal, xraw[, pl, re, channelOrder[ch]])\n"
					+ "}";
			CellHTS2NodeModel.logger.debug(fillPd);
			RUtil.voidEval(conn, fillPd);

			RUtil.voidEval(conn, "  names(intensityFiles) <- Filename");
			RUtil.voidEval(conn, "status <- rep(\"OK\", " + plateCount
					* replicateCount * paramCount + ")");
			final String createXCommand = "x <- new(\"cellHTS\", \n"
					+ "   assayData = adata,\n"
					+ "   phenoData = pdata,\n"
					+ "   featureData = fdata,\n"
					+ "   plateList = cbind(Filename, "
					+ (version.isPre210() ? "status=I(status)"
							: "Status=I(status)")
					+ ", data.frame(Plate=pd[,1], Replicate=pd[,2], Channel=pd[,3])),\n"
					+ "   intensityFiles=intensityFiles)\n";
			CellHTS2NodeModel.logger.debug(createXCommand);
			RUtil.voidEval(conn, createXCommand);
		} catch (final RserveException e) {
			CellHTS2NodeModel.logger
					.fatal("Problem creating the raw object", e);
		}
		// final REXP xExpr = conn.eval("x");
		// System.out.println(xExpr.toDebugString());
		checkObject(conn, "x", "The raw object is not valid!!!");
		// System.out.println(xExpr);
	}

	/**
	 * Checks {@code objectName} in the R session.
	 * 
	 * @param conn
	 *            An {@link RConnection}.
	 * @param objectName
	 *            The name of the variable in the R session.
	 * @param errorMessage
	 *            The error message if the observed object is not valid.
	 * @throws RserveException
	 *             Problem on server side.
	 * @throws REXPMismatchException
	 *             Problem getting the error message.
	 */
	private void checkObject(final RConnection conn, final String objectName,
			final String errorMessage) throws RserveException,
			REXPMismatchException {
		if (!RUtil.<REXPLogical> eval(conn, "validObject(" + objectName + ")")
				.isTRUE()[0]) {
			CellHTS2NodeModel.logger.error(errorMessage);
			throw new IllegalStateException(errorMessage);
		}
	}

	/**
	 * Creates the channel list from {@code parameters} to pass to R.
	 * 
	 * @param parameters
	 *            The list of selected parameters.
	 * @return The {@link StringBuilder} containing the proper parameters in R
	 *         syntax.
	 */
	private StringBuilder createChannelList(final List<String> parameters) {
		final StringBuilder channelsSb = new StringBuilder("c(");
		for (final String param : parameters) {
			channelsSb.append('"').append(param).append("\", ");
		}
		channelsSb.setLength(channelsSb.length() - 2);
		channelsSb.append(')');
		return channelsSb;
	}

	/**
	 * Sets the plate configuration and makes {@code x}'s state to {@code
	 * configured}.
	 * 
	 * @param table
	 *            The second input table.
	 * @param descTable
	 *            The third input table.
	 * @param screenLogTable
	 *            The fourth input table.
	 * @param conn
	 *            An {@link RConnection}.
	 * @param wellCount
	 *            The number of wells.
	 * @param plateCount
	 *            The number of plates.
	 * @param normMethod
	 *            The used normalisation method.
	 * @throws RserveException
	 *             Problem on server side.
	 * @throws REXPMismatchException
	 *             Problem getting the error message.
	 */
	private void plateConfiguration(final BufferedDataTable table,
			final BufferedDataTable descTable,
			final BufferedDataTable screenLogTable, final RConnection conn,
			final int wellCount, final int plateCount, final String normMethod)
			throws RserveException, REXPMismatchException {
		RUtil.voidEval(conn, "pWells <- well(x)[1:" + wellCount + "]");
		// final int differentCount = countDifferentValues(table, 2, false);
		final StringBuilder sb = new StringBuilder(
				"conf <- data.frame(Plate=c(");
		for (final DataRow row : table) {
			final String plate = ((StringCell) row.getCell(0)).getStringValue()
					.trim();
			sb.append('"').append("*".equals(plate) ? " *" : plate).append('"')
					.append(", ");
		}
		sb.setLength(sb.length() - 2);
		sb.append(")");
		sb.append(", Well=c(");
		for (final DataRow row : table) {
			final String well = ((StringCell) row.getCell(1)).getStringValue()
					.trim();
			sb.append('"').append("*".equals(well) ? " *" : well).append('"')
					.append(", ");
		}
		sb.setLength(sb.length() - 2);
		sb.append(")");
		sb.append(", Content=c(");
		for (final DataRow row : table) {
			sb.append('"').append(
					((StringCell) row.getCell(2)).getStringValue()
							.toLowerCase()).append('"').append(", ");
		}
		sb.setLength(sb.length() - 2);
		sb.append(")");
		sb.append(")");
		RUtil.voidEval(conn, sb.toString());
		RUtil.voidEval(conn,
				"pcontent <- tolower(conf$Content)  ## ignore case!\n");
		RUtil.voidEval(conn, "wAnno <- factor(rep(NA, " + plateCount
				* wellCount + "), levels=unique(pcontent))");
		RUtil.voidEval(conn, "conf[conf==\"*\"] <- \" *\"");
		RUtil
				.voidEval(
						conn,
						"   for (i in 1:nrow(conf)) {\n"
								+ "     iconf <- conf[i,]\n"
								+ "     # get plate IDs\n"
								+ "     wp <- if(is.numeric(iconf$Plate)) iconf$Plate  else  c(1:"
								+ plateCount
								+ ")[regexpr(iconf$Plate, 1:"
								+ plateCount
								+ ")>0]\n"
								+ "     # get well IDs\n"
								+ "     ww <- convertWellCoordinates(pWells[regexpr(iconf$Well, pWells)>0], pdim(x))$num\n"
								+ "     #count <- append(count, sprintf(\"%d-%d-%s\", rep(wp, each=length(ww)), rep(ww, length(wp)), rep(pcontent[i], length(wp)*length(ww))) )\n"
								+ "     if(!length(wp)) stop(sprintf(\"In the plate configuration, no plate matches were found for rule specified by line %d:\\n\\t %s \\n\\t %s\", i, paste(names(conf), collapse=\"\\t\"), paste(iconf, collapse=\"\\t\")))\n"
								+ "\n"
								+ "     if(!length(ww)) stop(sprintf(\"In the plate configuration, no well matches were found for rule specified by line %d:\\n\\t %s \\n\\t %s\", i, paste(names(conf), collapse=\"\\t\"), paste(iconf, collapse=\"\\t\")))\n"
								+ " \n" + "     wAnno[ww + rep(" + wellCount
								+ "*(wp-1), each=length(ww))] = pcontent[i] \n"
								+ "   }");
		RUtil.voidEval(conn, "missAnno <- is.na(wAnno)");
		RUtil
				.voidEval(
						conn,
						"  if(sum(missAnno)) {\n"
								+ "    ind <- which(missAnno)[1:min(5, sum(missAnno))]\n"
								+ "    msg = paste(\"The following plates and wells were not covered in the plate configuration:\\n\",\n"
								+ "      \"\\tPlate Well\\n\", \"\\t\",\n"
								+ "      paste((ind-1) %/% "
								+ wellCount
								+ " + 1,  1+(ind-1)%%"
								+ wellCount
								+ ", sep=\"\\t\", collapse=\"\\n\\t\"),\n"
								+ "      if(sum(missAnno)>5) sprintf(\"\\n\\t...and %d more.\\n\", sum(missAnno)-5), \"\\n\", sep=\"\")\n"
								+ "    stop(msg)\n" + "  }\n" + "");
		RUtil.voidEval(conn, "empty <- which(wAnno==\"empty\")");
		RUtil.voidEval(conn, "xraw <- Data(x)");
		RUtil.voidEval(conn,
				"xraw[] <- apply(xraw, 2:3, replace, list=empty, NA)");
		try {
			RUtil.voidEval(conn, "slog <- NULL");
			if (screenLogTable.getRowCount() > 0) {
				final String createScreenLog = readTableTo(screenLogTable,
						"slog");
				CellHTS2NodeModel.logger.debug(createScreenLog);
				RUtil.voidEval(conn, createScreenLog);
				RUtil
						.voidEval(
								conn,
								"		      for(i in c(\"Sample\", \"Channel\")) {\n"
										+ "		        if(!(i %in% names(slog))) \n"
										+ "		          slog[[i]] <- rep(1L, nrow(slog)) \n"
										+ "		        else \n"
										+ "		          if(!all(slog[[i]] %in% 1:get(paste(\"nr\", i, sep=\"\"))))\n"
										+ "		            stop(sprintf(\"Column \'%s\' of the screen log contains invalid entries.\", i))\n"
										+ "		        }");

				// conn.voidEval("checkColumns(slog, logFile,
				// mandatory=c(\"Plate\",
				// \"Well\",
				// \"Flag\", \"Sample\", \"Channel\"), numeric=c(\"Plate\",
				// \"Sample\",
				// \"Channel\"))");

				RUtil.voidEval(conn,
						"invalidPlateID <- !(slog$Plate %in% 1:nrPlate)");
				RUtil
						.voidEval(
								conn,
								"if(sum(invalidPlateID)) stop(sprintf(\"Column \'Plate\' of the screen log contains invalid entries.\"))");
			}
			RUtil.voidEval(conn, "x@screenLog <- slog");// TODO process
			// screenlog!
		} catch (final Exception e) {
			CellHTS2NodeModel.logger
					.warn(
							"Unable to use the screen log data. Please review your settings related to the screenlog file.",
							e);
		}
		RUtil.voidEval(conn, "x@plateConf <- conf");
		{
			final List<Pair<String, SettingsModel>> pairs = new ArrayList<Pair<String, SettingsModel>>();
			pairs.add(new Pair<String, SettingsModel>("Channels",
					parametersModel));
			pairs.add(new Pair<String, SettingsModel>("Normalization",
					normMethodModel));
			pairs.add(new Pair<String, SettingsModel>("Is multiplicative?",
					isMultiplicativeModel));
			pairs.add(new Pair<String, SettingsModel>("Log transformed",
					logTransformModel));
			pairs.add(new Pair<String, SettingsModel>("Variance adjust",
					scaleModel));
			pairs.add(new Pair<String, SettingsModel>("Scoring", scoreModel));
			pairs.add(new Pair<String, SettingsModel>("Summarize replicates",
					summariseModel));
			RUtil.voidEval(conn,
					"descript <- vector(mode=\"character\", length="
							+ (descTable.getRowCount() + pairs.size()) + ")");
			int row = 1;
			for (final DataRow dataRow : descTable) {
				final StringBuilder descript = new StringBuilder("descript[")
						.append(row).append("] <- \"");
				descript.append(
						((StringCell) dataRow.getCell(1)).getStringValue())
						.append(": ").append(
								((StringCell) dataRow.getCell(2))
										.getStringValue());
				descript.append("\"");
				RUtil.voidEval(conn, descript.toString());
				++row;
			}
			for (final Pair<String, SettingsModel> pair : pairs) {
				RUtil
						.voidEval(
								conn,
								"descript["
										+ row
										+ "] <- \""
										+ pair.getLeft()
										+ "="
										+ (pair.getRight() instanceof SettingsModelString ? ((SettingsModelString) pair
												.getRight()).getStringValue()
												: pair.getRight() instanceof SettingsModelFilterString ? ((SettingsModelFilterString) pair
														.getRight())
														.getIncludeList()
														.toString()
														: Boolean
																.toString(
																		((SettingsModelBoolean) pair
																				.getRight())
																				.getBooleanValue())
																.toUpperCase())
										+ "\"");
				++row;
			}
		}
		RUtil.voidEval(conn, "x@screenDesc <- descript");
		RUtil
				.voidEval(
						conn,
						"  if (!is.null(slog)) {\n"
								+ "    ipl  = slog$Plate\n"
								+ "    irep = slog$Sample\n"
								+ "    ich  = slog$Channel\n"
								+ "    ipos = convertWellCoordinates(slog$Well, pdim(x))$num\n"
								+ "    stopifnot(!any(is.na(ipl)), !any(is.na(irep)), !any(is.na(ich)))\n"
								+ "\n" + "    xraw[cbind(ipos + " + wellCount
								+ "*(ipl-1), irep, ich)] = NA \n" + "  }");
		RUtil.voidEval(conn, "Data(x) <- xraw");
		RUtil.voidEval(conn, "fData(x)$controlStatus <- wAnno");
		RUtil.voidEval(conn,
				"stopifnot(all(fData(x)$controlStatus!=\"unknown\"))");
		RUtil.voidEval(conn, "description(x) <- miameInfo");
		RUtil.voidEval(conn, "x@state[[\"configured\"]] <- TRUE");
		checkObject(conn, "x", "The raw object is not valid!!!");
	}

	/**
	 * Read a table's content and generates an R command from it.
	 * 
	 * @param table
	 *            A {@link DataTable}.
	 * @param varName
	 *            The name of the variable in R session..
	 * @return A {@link String} representing the command to add the data to the
	 *         R session. It uses R syntax.
	 */
	private String readTableTo(final DataTable table, final String varName) {
		final StringBuilder sb = new StringBuilder(varName)
				.append(" <- data.frame(");
		int col = 0;
		for (final DataColumnSpec spec : table.getDataTableSpec()) {
			sb.append(spec.getName()).append("=c(");
			final boolean doubleType = DoubleCell.TYPE.isASuperTypeOf(spec
					.getType());
			for (final DataRow row : table) {
				final DataCell cell = row.getCell(col);
				if (cell.isMissing()) {
					sb.append(doubleType ? "NA" : "");
				} else if (cell instanceof StringCell) {
					final StringCell stringCell = (StringCell) cell;
					sb.append('"').append(stringCell.getStringValue()).append(
							'"');
				} else if (cell instanceof DoubleValue) {
					sb.append(((DoubleValue) cell).getDoubleValue());
				} else {
					throw new IllegalArgumentException("Unsupported type: "
							+ cell.getType());
				}
				sb.append(", ");
			}
			sb.setLength(sb.length() - 2);
			++col;
			sb.append("), ");
		}
		sb.setLength(sb.length() - 2);
		sb.append(")");
		return sb.toString();
	}

	/**
	 * Adds the MIAME information to the {@code miameInfo} R variable.
	 * 
	 * @param inData
	 *            Input data tables.
	 * @param conn
	 *            An {@link RConnection}.
	 * @throws RserveException
	 *             Problem on server side.
	 * @throws REXPMismatchException
	 *             Problem getting the error message.
	 */
	private void addMiame(final BufferedDataTable[] inData,
			final RConnection conn) throws RserveException,
			REXPMismatchException {
		final String[] strings = new String[] { "Lab description",
				"Experimenter name", "Laboratory", "Contact information",
				"Title", "PMIDs", "URL", "Abstract" };
		final Map<String, String> miameInfo = findValues(inData[2], strings);
		final StringBuilder sb = new StringBuilder("miameInfo <- list(");
		sb.append("sepLab=\"").append(miameInfo.get(strings[0])).append("\",");
		sb.append("name=\"").append(miameInfo.get(strings[1])).append("\",");
		sb.append("lab=\"").append(miameInfo.get(strings[2])).append("\",");
		sb.append("contact=\"").append(miameInfo.get(strings[3])).append("\",");
		sb.append("title=\"").append(miameInfo.get(strings[4])).append("\",");
		sb.append("pubMedIds=\"").append(miameInfo.get(strings[5])).append(
				"\",");
		sb.append("url=\"").append(miameInfo.get(strings[6])).append("\",");
		sb.append("abstract=\"").append(miameInfo.get(strings[7])).append('"');
		sb.append(")");
		RUtil.voidEval(conn, sb.toString());
		RUtil.voidEval(conn, "  miameInfo <- with(miameInfo, new(\"MIAME\", \n"
				+ "    name=name,\n" + "    lab = lab,\n"
				+ "    contact=contact,\n" + "    title=title,\n"
				+ "    pubMedIds=pubMedIds,\n" + "    url=url,\n"
				+ "    abstract=abstract))");
		// TODO notes!
	}

	/**
	 * 
	 * TODO review
	 * 
	 * @param table
	 *            The third input table
	 * @param strings
	 *            The headers looking for.
	 * @return A {@link Map} keys of {@code strings}. The values are
	 *         representing the ({@link StringCell}) last values of the
	 *         3<sup>rd</sup> column if in the second there is the key.
	 */
	private Map<String, String> findValues(final BufferedDataTable table,
			final String... strings) {
		final Map<String, String> ret = new HashMap<String, String>();
		for (final String str : strings) {
			ret.put(str, "");
		}
		for (final DataRow row : table) {
			for (final String string : strings) {
				if (((StringCell) row.getCell(1)).getStringValue().equals(
						string)) {
					ret.put(string, ((StringCell) row.getCell(2))
							.getStringValue());
				}
			}
		}
		return ret;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void reset() {
		// TODO Code executed on reset.
		// Models build during execute are cleared here.
		// Also data handled in load/saveInternals will be erased here.
		if (outDirs != null) {
			outDirs.clear();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
			throws InvalidSettingsException {
		final int plateIdx = checkColumn(inSpecs,
				PublicConstants.PLATE_COL_NAME);
		final int replicateIdx = checkColumn(inSpecs,
				PublicConstants.REPLICATE_COL_NAME);
		final int wellIdx = checkColumn(inSpecs, PublicConstants.WELL_COL_NAME);
		if (!inSpecs[0].getColumnSpec(plateIdx).getType().equals(IntCell.TYPE)
				|| !inSpecs[0].getColumnSpec(replicateIdx).getType().equals(
						IntCell.TYPE)
				|| !inSpecs[0].getColumnSpec(wellIdx).getType().equals(
						StringCell.TYPE)) {
			throw new InvalidSettingsException("Wrong input type on first port");
		}
		if (!inSpecs[0].getColumnSpec(plateIdx).getName().equalsIgnoreCase(
				PublicConstants.PLATE_COL_NAME)
				|| !inSpecs[0].getColumnSpec(replicateIdx).getName()
						.equalsIgnoreCase(PublicConstants.REPLICATE_COL_NAME)
				|| !inSpecs[0].getColumnSpec(wellIdx).getName()
						.equalsIgnoreCase(PublicConstants.WELL_COL_NAME)) {
			throw new InvalidSettingsException("Wrong input name on first port");
		}
		{
			final Iterator<DataColumnSpec> firstIt = inSpecs[0].iterator();
			// firstIt.next();
			// firstIt.next();
			// firstIt.next();
			final Set<String> checkedNames = new HashSet<String>();
			checkedNames.add(PublicConstants.PLATE_COL_NAME);
			checkedNames.add(PublicConstants.REPLICATE_COL_NAME);
			checkedNames.add(PublicConstants.WELL_COL_NAME);
			while (firstIt.hasNext()) {
				final DataColumnSpec spec = firstIt.next();
				if (!checkedNames.contains(spec.getName())) {
					if (!DoubleCell.TYPE.isASuperTypeOf(spec.getType())
							&& !spec.getType().equals(StringCell.TYPE)) {
						throw new InvalidSettingsException(
								"Illegal type of parameter on first port");
					}
				}
			}
		}
		if (!inSpecs[1].getColumnSpec(0).getType().equals(StringCell.TYPE)
				|| !inSpecs[1].getColumnSpec(1).getType().equals(
						StringCell.TYPE)
				|| !inSpecs[1].getColumnSpec(2).getType().equals(
						StringCell.TYPE)) {
			throw new InvalidSettingsException(
					"Wrong input type on second port");
		}
		if (!inSpecs[1].getColumnSpec(0).getName().equalsIgnoreCase(
				SimpleConfiguratorNodeModel.PLATE_COL_NAME)
				|| !inSpecs[1].getColumnSpec(1).getName().equalsIgnoreCase(
						SimpleConfiguratorNodeModel.WELL_COL_NAME)
				|| !inSpecs[1].getColumnSpec(2).getName().equalsIgnoreCase(
						SimpleConfiguratorNodeModel.CONTENT_COL_NAME)) {
			throw new InvalidSettingsException(
					"Wrong input name on second port");
		}
		if (!inSpecs[2].getColumnSpec(0).getType().equals(StringCell.TYPE)
				|| !inSpecs[2].getColumnSpec(1).getType().equals(
						StringCell.TYPE)
				|| !inSpecs[2].getColumnSpec(2).getType().equals(
						StringCell.TYPE)) {
			throw new InvalidSettingsException("Wrong input type on third port");
		}
		if (!inSpecs[2].getColumnSpec(0).getName().equalsIgnoreCase(
				SimpleConfiguratorNodeModel.DESC_GROUP_COL_NAME)
				|| !inSpecs[2].getColumnSpec(1).getName().equalsIgnoreCase(
						SimpleConfiguratorNodeModel.DESC_KEY_COL_NAME)
				|| !inSpecs[2].getColumnSpec(2).getName().equalsIgnoreCase(
						SimpleConfiguratorNodeModel.DESC_VALUE_COL_NAME)) {
			throw new InvalidSettingsException("Wrong input name on third port");
		}
		if (logTransformModel.getBooleanValue()
				&& !isMultiplicativeModel.getBooleanValue()) {
			throw new InvalidSettingsException(
					"Additive normalisation with logarithm transformation is not supported!");
		}
		// TODO check the screen.log format.
		final DataTableSpec sumTableSpec = new DataTableSpec(
				computeTopTableSpec(inSpecs[0], false));
		final DataTableSpec repTableSpec = new DataTableSpec(
				computeTopTableSpec(inSpecs[0], true));
		return new DataTableSpec[] { sumTableSpec, repTableSpec,
				CellHTS2NodeModel.aggregateValuesSpec,
				CellHTS2NodeModel.configurationSpec,
				CellHTS2NodeModel.outputFolderSpec };
	}

	private int checkColumn(final DataTableSpec[] inSpecs, final String colName)
			throws InvalidSettingsException {
		final int idx = inSpecs[0].findColumnIndex(colName);
		if (idx == -1) {
			throw new InvalidSettingsException("No " + colName
					+ " column found.");
		}
		return idx;
	}

	/**
	 * @param inputSpecs
	 *            The input table specification.
	 * @param replicateTable
	 *            Column spec of replicates, or scores table.
	 * @return The {@link DataColumnSpec}s for the first and the second
	 *         outports.
	 */
	private DataColumnSpec[] computeTopTableSpec(
			final DataTableSpec inputSpecs, final boolean replicateTable) {
		final List<DataColumnSpec> ret = new ArrayList<DataColumnSpec>();
		ret.add(new DataColumnSpecCreator(PublicConstants.EXPERIMENT_COLUMN,
				StringCell.TYPE).createSpec());
		ret.add(new DataColumnSpecCreator(
				PublicConstants.NORMALISATION_METHOD_COLUMN, StringCell.TYPE)
				.createSpec());
		ret.add(new DataColumnSpecCreator(PublicConstants.LOG_TRANSFORM_COLUMN,
				StringCell.TYPE).createSpec());
		ret.add(new DataColumnSpecCreator(
				PublicConstants.NORMALISATION_KIND_COLUMN, StringCell.TYPE)
				.createSpec());
		ret.add(new DataColumnSpecCreator(
				PublicConstants.VARIANCE_ADJUSTMENT_COLUMN, StringCell.TYPE)
				.createSpec());
		ret.add(new DataColumnSpecCreator(
				PublicConstants.SCORING_METHOD_COLUMN, StringCell.TYPE)
				.createSpec());
		ret.add(new DataColumnSpecCreator(
				PublicConstants.SUMMARISE_METHOD_COLUMN, StringCell.TYPE)
				.createSpec());
		final List<PossibleStatistics> stats = ColumnSelectionFieldEditor
				.parseString(PossibleStatistics.class, ImporterNodePlugin
						.getDefault().getPreferenceStore().getString(
								PreferenceConstants.RESULT_COL_ORDER));
		final DataColumnSpec[] additionalColumns = selectAdditionalColumns(inputSpecs);
		CellHTS2NodeModel.computeTableSpec(ret, replicateTable, stats, null,
				parametersModel.getIncludeList(), additionalColumns);
		return ret.toArray(new DataColumnSpec[ret.size()]);
	}

	/**
	 * @param inputSpecs
	 *            The input table specification.
	 * @return The additional columns that may have interest ({@code well},
	 *         {@code GeneID}, {@code GeneSymbol}).
	 */
	private DataColumnSpec[] selectAdditionalColumns(
			final DataTableSpec inputSpecs) {
		final List<DataColumnSpec> ret = new ArrayList<DataColumnSpec>();
		for (final DataColumnSpec columnSpec : inputSpecs) {
			if (columnSpec.getType() == StringCell.TYPE
					&& !columnSpec.getName().equalsIgnoreCase("well")
					&& !columnSpec.getName().equalsIgnoreCase("GeneID")
					&& !columnSpec.getName().equalsIgnoreCase("GeneSymbol")) {
				ret.add(columnSpec);
			}
		}
		return ret.toArray(new DataColumnSpec[ret.size()]);
	}

	/**
	 * Computes the outport table values representing from the {@code topTable}
	 * object based on the parameters. The result is added to {@code rets}.
	 * 
	 * @param topTable
	 *            The R table of results.
	 * @param row
	 *            The actual row number.
	 * @param numReplicates
	 *            number of replicates
	 * @param rets
	 *            The result cells.
	 * @param replicateTable
	 *            Create for replicate table, or for scores.
	 * @param turnReplicates
	 *            If {@code true} it will create a Replicate column with the
	 *            values. (Should be deprecated, Pivot node made it obsolote.)
	 * @param stats
	 *            The statistics to compute.
	 * @param ch
	 *            The channel name (might be {@code null}).
	 * @param selectedParameters
	 *            The selected parameters.
	 * @param columnSpecs
	 *            Additional columns from the original table.
	 */
	private static void computeTableValue(final RList topTable, final int row,
			final int numReplicates, final List<List<DataCell>> rets,
			final boolean replicateTable, final boolean turnReplicates,
			final List<PossibleStatistics> stats, final String ch,
			final List<String> selectedParameters,
			final DataColumnSpec... columnSpecs) {
		if (stats.isEmpty()) {
			return;
		}
		final PossibleStatistics possibleStatistics = stats.get(0);
		switch (possibleStatistics.getMultiplicity()) {
		case NONE:
			switch (possibleStatistics) {
			case GROUP_BY_CHANNELS_START: {
				int nextEnd = 0;
				for (; nextEnd < stats.size(); ++nextEnd) {
					if (stats.get(nextEnd) == PossibleStatistics.GROUP_BY_CHANNELS_END) {
						break;
					}
				}
				assert nextEnd > 0 : "missing group end: " + stats;
				for (final String param : selectedParameters) {
					CellHTS2NodeModel.computeTableValue(topTable, row,
							numReplicates, rets, replicateTable,
							turnReplicates, stats.subList(1, nextEnd), param,
							selectedParameters, columnSpecs);
				}
				if (nextEnd + 1 < stats.size()) {
					CellHTS2NodeModel.computeTableValue(topTable, row,
							numReplicates, rets, replicateTable,
							turnReplicates, stats.subList(nextEnd + 1, stats
									.size()), ch, selectedParameters,
							columnSpecs);
				}
				break;
			}
			case GROUP_BY_REPLICATES_START:
				int nextEnd = 0;
				for (; nextEnd < stats.size(); ++nextEnd) {
					if (stats.get(nextEnd) == PossibleStatistics.GROUP_BY_REPLICATES_END) {
						break;
					}
				}
				assert nextEnd > 0 : "missing group end: " + stats;
				if (replicateTable) {
					CellHTS2NodeModel.computeTableValue(topTable, row,
							numReplicates, rets, replicateTable,
							turnReplicates, stats.subList(1, nextEnd - 1), ch,
							selectedParameters, columnSpecs);
				}
				if (nextEnd + 1 < stats.size()) {
					CellHTS2NodeModel.computeTableValue(topTable, row,
							numReplicates, rets, replicateTable,
							turnReplicates, stats.subList(nextEnd + 1, stats
									.size()), ch, selectedParameters,
							columnSpecs);
				}
				break;
			default:
				break;
			}
			break;
		case UNSPECIFIED:
			for (final DataColumnSpec columnSpec : columnSpecs) {
				for (final List<DataCell> ret : rets) {
					final Object column = topTable.get(columnSpec.getName());
					ret.add(column == null ? DataType.getMissingCell()
							: new StringCell(
									((REXPString) column).asStrings()[row]));
				}
			}
			CellHTS2NodeModel.computeTableValue(topTable, row, numReplicates,
					rets, replicateTable, turnReplicates, stats.subList(1,
							stats.size()), ch, selectedParameters, columnSpecs);
			break;
		case CHANNELS:
			for (final List<DataCell> ret : rets) {
				switch (possibleStatistics) {
				case SCORE:
					ret.add(getCellFromValue(((REXPDouble) topTable
							.get("score_" + ch)).asDoubles()[row]));
					// ret.add(new DataColumnSpecCreator(possibleStatistics
					// .getDisplayText()
					// + "_" + ch, getType(possibleStatistics)).createSpec());
					break;
				case MEAN_OR_DIFF:
					ret.add(getCellFromValue(((REXPDouble) topTable
							.get((numReplicates == 2 ? "diff_" : "average_")
									+ ch)).asDoubles()[row]));
					break;
				case MEDIAN:
					ret.add(getCellFromValue(((REXPDouble) topTable
							.get("median_" + ch)).asDoubles()[row]));
					break;
				case FINAL_WELL_ANNOTATION:
					if (ch == null) {
						ret.add(new StringCell(((REXPString) topTable
								.get("finalWellAnno")).asStrings()[row]));
					} else {
						ret.add(new StringCell(((REXPString) topTable
								.get("finalWellAnno_" + ch)).asStrings()[row]));
					}
					break;
				default:
					assert possibleStatistics.getMultiplicity() != Multiplicity.CHANNELS : possibleStatistics;
					break;
				}
			}
			CellHTS2NodeModel.computeTableValue(topTable, row, numReplicates,
					rets, replicateTable, turnReplicates, stats.subList(1,
							stats.size()), ch, selectedParameters, columnSpecs);
			break;
		case REPLICATES:
			if (replicateTable) {
				int repl = 1;
				for (final List<DataCell> ret : rets) {
					switch (possibleStatistics) {
					case REPLICATE:
						if (turnReplicates) {
							ret.add(new IntCell(repl));
						}
						break;
					default:
						assert possibleStatistics.getMultiplicity() != Multiplicity.REPLICATES : possibleStatistics;
						break;
					}
					++repl;
				}
			}
			CellHTS2NodeModel.computeTableValue(topTable, row, numReplicates,
					rets, replicateTable, turnReplicates, stats.subList(1,
							stats.size()), ch, selectedParameters, columnSpecs);
			break;
		case CHANNELS_AND_REPLICATES:
			if (replicateTable) {
				for (int repl = 1; repl <= numReplicates; ++repl) {
					final List<DataCell> ret = turnReplicates ? rets
							.get(repl - 1) : rets.get(0);
					switch (possibleStatistics) {
					case RAW:
						ret
								.add(getCellFromValue(((REXPDouble) topTable
										.get("raw_r" + repl + "_" + ch))
										.asDoubles()[row]));
						break;
					case RAW_PER_PLATE_REPLICATE_MEAN:
						ret.add(getCellFromValue(((REXPDouble) topTable
								.get("raw/PlateMedian_r" + repl + "_" + ch))
								.asDoubles()[row]));
						break;
					case NORMALISED:
						ret.add(getCellFromValue(((REXPDouble) topTable
								.get("normalized_r" + repl + "_" + ch))
								.asDoubles()[row]));
						break;
					default:
						assert possibleStatistics.getMultiplicity() != Multiplicity.CHANNELS_AND_REPLICATES : possibleStatistics;
						break;
					}
				}
			}
			CellHTS2NodeModel.computeTableValue(topTable, row, numReplicates,
					rets, replicateTable, turnReplicates, stats.subList(1,
							stats.size()), ch, selectedParameters, columnSpecs);
			break;
		case SINGLE:
			int repl = 0;
			for (final List<DataCell> ret : rets) {
				switch (possibleStatistics) {
				case GENE_ID: {
					final REXPFactor geneIDs = (REXPFactor) topTable
							.get("GeneID");
					ret.add(geneIDs == null ? DataType.getMissingCell()
							: new StringCell(geneIDs.asStrings()[row]));
				}
					break;
				case GENE_SYMBOL: {
					final REXPFactor geneSymbol = (REXPFactor) topTable
							.get("GeneSymbol");
					ret.add(geneSymbol == null
							|| geneSymbol.asStrings()[row] == null ? DataType
							.getMissingCell() : new StringCell(geneSymbol
							.asStrings()[row]));
					break;
				}
				case PLATE:
					ret.add(new IntCell(((REXPInteger) topTable.get("plate"))
							.asIntegers()[row]));
					break;
				case POSITION:
					ret.add(new IntCell(
							((REXPInteger) topTable.get("position"))
									.asIntegers()[row]));
					break;
				case REPLICATE:
					if (turnReplicates) {
						ret.add(new IntCell(++repl));
					}
					break;
				case SEPARATOR:
					ret.add(new StringCell(""));
					break;
				case WELL:
					ret.add(new StringCell(((REXPString) topTable.get("well"))
							.asStrings()[row]));
					break;
				case WELL_ANNOTATION:
					ret.add(new StringCell(((REXPFactor) topTable
							.get("wellAnno")).asStrings()[row]));
					break;
				default:
					break;
				}
			}
			CellHTS2NodeModel.computeTableValue(topTable, row, numReplicates,
					rets, replicateTable, turnReplicates, stats.subList(1,
							stats.size()), ch, selectedParameters, columnSpecs);
			break;
		default:
			break;
		}
	}

	private static DataCell getCellFromValue(final double d) {
		return Double.isNaN(d) ? DataType.getMissingCell() : new DoubleCell(d);
	}

	/**
	 * The interesting part of
	 * {@link #computeTopTableSpec(DataTableSpec, boolean)}.
	 * 
	 * @param ret
	 *            The result {@link DataColumnSpec}s.
	 * @param replicateTable
	 *            Create for replicate table, or for scores.
	 * @param stats
	 *            The statistics to compute.
	 * @param ch
	 *            The channel name (might be {@code null}).
	 * @param selectedParameters
	 *            The selected parameters.
	 * @param columnSpecs
	 *            Additional columns from the original table.
	 */
	private static void computeTableSpec(final List<DataColumnSpec> ret,
			final boolean replicateTable, final List<PossibleStatistics> stats,
			final @Nullable String ch, final List<String> selectedParameters,
			final DataColumnSpec... columnSpecs) {
		if (stats.isEmpty()) {
			return;
		}
		final PossibleStatistics possibleStatistics = stats.get(0);
		switch (possibleStatistics.getMultiplicity()) {
		case NONE:
			switch (possibleStatistics) {
			case GROUP_BY_CHANNELS_START: {
				int nextEnd = 0;
				for (; nextEnd < stats.size(); ++nextEnd) {
					if (stats.get(nextEnd) == PossibleStatistics.GROUP_BY_CHANNELS_END) {
						break;
					}
				}
				assert nextEnd > 0 : "missing group end: " + stats;
				for (final String param : selectedParameters) {
					CellHTS2NodeModel.computeTableSpec(ret, replicateTable,
							stats.subList(1, nextEnd), param,
							selectedParameters, columnSpecs);
				}
				if (nextEnd + 1 < stats.size()) {
					CellHTS2NodeModel.computeTableSpec(ret, replicateTable,
							stats.subList(nextEnd + 1, stats.size()), ch,
							selectedParameters, columnSpecs);
				}
				break;
			}
			case GROUP_BY_REPLICATES_START:
				int nextEnd = 0;
				for (; nextEnd < stats.size(); ++nextEnd) {
					if (stats.get(nextEnd) == PossibleStatistics.GROUP_BY_REPLICATES_END) {
						break;
					}
				}
				assert nextEnd > 0 : "missing group end: " + stats;
				if (replicateTable) {
					CellHTS2NodeModel.computeTableSpec(ret, replicateTable,
							stats.subList(1, nextEnd - 1), ch,
							selectedParameters, columnSpecs);
				}
				if (nextEnd + 1 < stats.size()) {
					CellHTS2NodeModel.computeTableSpec(ret, replicateTable,
							stats.subList(nextEnd + 1, stats.size()), ch,
							selectedParameters, columnSpecs);
				}
				break;
			default:
				break;
			}
			break;
		case UNSPECIFIED:
			for (final DataColumnSpec columnSpec : columnSpecs) {
				ret.add(columnSpec);
			}
			CellHTS2NodeModel.computeTableSpec(ret, replicateTable, stats
					.subList(1, stats.size()), ch, selectedParameters,
					columnSpecs);
			break;
		case CHANNELS:
			ret.add(new DataColumnSpecCreator(StaticUtil
					.createPrefix(possibleStatistics)
					+ ch, CellHTS2NodeModel.getType(possibleStatistics))
					.createSpec());
			CellHTS2NodeModel.computeTableSpec(ret, replicateTable, stats
					.subList(1, stats.size()), ch, selectedParameters,
					columnSpecs);
			break;
		case REPLICATES:
			if (replicateTable) {
				ret.add(new DataColumnSpecCreator(possibleStatistics
						.getDisplayText(), CellHTS2NodeModel
						.getType(possibleStatistics)).createSpec());
			}
			CellHTS2NodeModel.computeTableSpec(ret, replicateTable, stats
					.subList(1, stats.size()), ch, selectedParameters,
					columnSpecs);
			break;
		case CHANNELS_AND_REPLICATES:
			ret.add(new DataColumnSpecCreator(StaticUtil
					.createPrefix(possibleStatistics)
					+ ch, CellHTS2NodeModel.getType(possibleStatistics))
					.createSpec());
			CellHTS2NodeModel.computeTableSpec(ret, replicateTable, stats
					.subList(1, stats.size()), ch, selectedParameters,
					columnSpecs);
			break;
		case SINGLE:
			if (possibleStatistics != PossibleStatistics.REPLICATE
					|| replicateTable) {
				ret.add(new DataColumnSpecCreator(possibleStatistics
						.getDisplayText(), CellHTS2NodeModel
						.getType(possibleStatistics)).createSpec());
			}
			CellHTS2NodeModel.computeTableSpec(ret, replicateTable, stats
					.subList(1, stats.size()), ch, selectedParameters,
					columnSpecs);
			break;
		default:
			break;
		}
	}

	/**
	 * @param possibleStatistics
	 *            A {@link PossibleStatistics}.
	 * @return The {@link DataType} representing the {@code possibleStatistics}.
	 */
	private static DataType getType(final PossibleStatistics possibleStatistics) {
		switch (possibleStatistics.getType()) {
		case Int:
			return PossibleStatistics.GENE_ID == possibleStatistics ? StringCell.TYPE
					: IntCell.TYPE;
		case Real:
			return DoubleCell.TYPE;
		case Strings:
			return StringCell.TYPE;
		default:
			throw new IllegalArgumentException("Wrong type: "
					+ possibleStatistics.getType());
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveSettingsTo(final NodeSettingsWO settings) {
		experimentNameModel.saveSettingsTo(settings);
		folderPatternModel.saveSettingsTo(settings);
		parametersModel.saveSettingsTo(settings);
		normMethodModel.saveSettingsTo(settings);
		isMultiplicativeModel.saveSettingsTo(settings);
		logTransformModel.saveSettingsTo(settings);
		scaleModel.saveSettingsTo(settings);
		scoreModel.saveSettingsTo(settings);
		summariseModel.saveSettingsTo(settings);
		outputDirModel.saveSettingsTo(settings);
		scoreRange.saveSettingsTo(settings);
		aspectRatioModel.saveSettingsTo(settings);
		generateHTML.saveSettingsTo(settings);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
			throws InvalidSettingsException {
		experimentNameModel.loadSettingsFrom(settings);
		folderPatternModel.loadSettingsFrom(settings);
		parametersModel.loadSettingsFrom(settings);
		normMethodModel.loadSettingsFrom(settings);
		isMultiplicativeModel.loadSettingsFrom(settings);
		logTransformModel.loadSettingsFrom(settings);
		scaleModel.loadSettingsFrom(settings);
		scoreModel.loadSettingsFrom(settings);
		summariseModel.loadSettingsFrom(settings);
		outputDirModel.loadSettingsFrom(settings);
		scoreRange.loadSettingsFrom(settings);
		aspectRatioModel.loadSettingsFrom(settings);
		generateHTML.loadSettingsFrom(settings);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void validateSettings(final NodeSettingsRO settings)
			throws InvalidSettingsException {
		experimentNameModel.validateSettings(settings);
		folderPatternModel.validateSettings(settings);
		parametersModel.validateSettings(settings);
		normMethodModel.validateSettings(settings);
		isMultiplicativeModel.validateSettings(settings);
		logTransformModel.validateSettings(settings);
		scaleModel.validateSettings(settings);
		scoreModel.validateSettings(settings);
		summariseModel.validateSettings(settings);
		outputDirModel.validateSettings(settings);
		scoreRange.validateSettings(settings);
		aspectRatioModel.validateSettings(settings);
		generateHTML.validateSettings(settings);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadInternals(final File internDir,
			final ExecutionMonitor exec) throws IOException,
			CanceledExecutionException {
		final File file = new File(internDir, CellHTS2NodeModel.OUT_DIRS);
		if (file.canRead()) {
			final FileInputStream fis = new FileInputStream(file);
			try {
				final InputStreamReader reader = new InputStreamReader(fis);
				try {
					final BufferedReader br = new BufferedReader(reader);
					try {
						if (outDirs == null) {
							outDirs = new TreeMap<String, String>();
						}
						String line;
						while ((line = br.readLine()) != null) {
							final String[] parts = line.split("\"");
							outDirs.put(parts[1], parts[3]);
						}
					} finally {
						br.close();
					}
				} finally {
					reader.close();
				}
			} finally {
				fis.close();
			}
		} else {
			outDirs = new TreeMap<String, String>();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveInternals(final File internDir,
			final ExecutionMonitor exec) throws IOException,
			CanceledExecutionException {
		final File file = new File(internDir, CellHTS2NodeModel.OUT_DIRS);
		final FileOutputStream fos = new FileOutputStream(file);
		try {
			final OutputStreamWriter writer = new OutputStreamWriter(fos,
					"UTF-8");
			try {
				for (final Entry<String, String> entry : outDirs.entrySet()) {
					writer.append('"').append(entry.getKey()).append('"')
							.append('\t').append('"').append(entry.getValue())
							.append('"').append('\n');
				}
			} finally {
				writer.close();
			}

		} finally {
			fos.close();
		}
	}

	/**
	 * @return the outDirs (A {@link Map} with keys from the {@code normMethods}
	 *         , and values as folder names (absolute, or relative).)
	 * @see #computeOutDirs(String[], String, String, List, String, boolean,
	 *      String, String, String, String)
	 */
	public Map<String, String> getOutDirs() {
		return new HashMap<String, String>(outDirs);
	}
}

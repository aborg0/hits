package ie.tcd.imm.hits.knime.ranking;

import ie.tcd.imm.hits.knime.util.ModelBuilder;
import ie.tcd.imm.hits.knime.util.ModelBuilder.SpecAnalyser;
import ie.tcd.imm.hits.knime.view.heatmap.HeatmapNodeModel.StatTypes;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
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
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.defaultnodesettings.SettingsModelStringArray;

import edu.umd.cs.findbugs.annotations.DefaultAnnotation;

/**
 * This is the model implementation of Rank. This node ranks the results for
 * each parameter and for each normalisation methods with selectable neutral
 * values and the direction of the upregulation.<br>
 * The downregulated values has negative rankings, the upregulated has positive
 * ones. If present it uses the (final) well annotation information of the
 * wells, and only the sample wells are ranked. If the well annotations are not
 * present the ranking is based on all wells with non-missing values.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
@DefaultAnnotation( { Nonnull.class, CheckReturnValue.class })
public class RankNodeModel extends NodeModel {

	// the logger instance
	private static final NodeLogger logger = NodeLogger
			.getLogger(RankNodeModel.class);

	/**
	 * These are the possible groups for creating the groups for the ranking.
	 */
	public static enum RankingGroups {
		/** The group is a whole experiment */
		experiment,
		/**
		 * The group is a plate within an experiment, but with the replicates.
		 * <p>
		 * If there were replicates, then the mean of the replicates will be
		 * used for the ranking.
		 */
		plate,
		/**
		 * The group is independent from the plates, but computed for each
		 * replicate in the same experiment.
		 */
		replicate,
		/** The group depends on both the plate and the replicate values. */
		plateAndReplicate;
	}

	/**
	 * The strategy to handle ties. This decides on what to do when there are
	 * ties in the order.
	 */
	public static enum TieHandling {
		/**
		 * The next in the rank will have increased ranking, like the in this
		 * case: {@code 1.2, 2.1, 2.1, 2.4} will generate: {@code 4, 2, 2, 1}
		 * rankings.
		 */
		increase,
		/**
		 * The next rank will has exactly {@code 1} difference to the tied
		 * values' rank. Like the in this case: {@code 1.2, 2.1, 2.1, 2.4} will
		 * generate: {@code 3, 2, 2, 1} rankings.
		 */
		continuous;
	}

	/** Configuration key for the well annotation column. */
	static final String CFGKEY_WELL_ANNOTATION = "ie.tcd.imm.hits.knime.ranking.well.annotation";

	/** initial default well annotation value. */
	static final String DEFAULT_WELL_ANNOTATION = "well annotation";

	/** Configuration key for the rank prefix. */
	static final String CFGKEY_RANK_PREFIX = "ie.tcd.imm.hits.knime.ranking.prefix";

	/** initial default rank prefix. */
	static final String DEFAULT_RANK_PREFIX = "rank_";

	/** Configuration key for the ranking groups. */
	static final String CFGKEY_RANKING_GROUPS = "ie.tcd.imm.hits.knime.ranking.groups";

	/** initial default ranking groups. */
	static final String DEFAULT_RANKING_GROUPS = RankingGroups.experiment
			.name();

	/** Configuration key for the statistics. */
	static final String CFGKEY_STATISTICS = "ie.tcd.imm.hits.knime.ranking.statistics";

	/** initial default statistics. */
	static final String[] DEFAULT_STATISTICS = new String[] { StatTypes.score
			.name() };

	/** Configuration key for the parameters. */
	static final String CFGKEY_PARAMETERS = "ie.tcd.imm.hits.knime.ranking.parameters";

	/** initial default parameters. */
	static final String[] DEFAULT_PARAMETERS = new String[] { "" };

	/** Configuration key for the regulation. */
	static final String CFGKEY_REGULATION = "ie.tcd.imm.hits.knime.ranking.regulation";

	/** initial default regulation. */
	static final String DEFAULT_REGULATION = "0+";

	/** Configuration key for the tie handling. */
	static final String CFGKEY_TIE = "ie.tcd.imm.hits.knime.ranking.tie.handling";

	/** initial default tie handling. */
	static final String DEFAULT_TIE = TieHandling.continuous.name();

	private final SettingsModelString wellAnnotationColumn = new SettingsModelString(
			CFGKEY_WELL_ANNOTATION, DEFAULT_WELL_ANNOTATION);

	private final SettingsModelString rankPrefix = new SettingsModelString(
			CFGKEY_RANK_PREFIX, DEFAULT_RANK_PREFIX);

	private final SettingsModelString groupingModel = new SettingsModelString(
			CFGKEY_RANKING_GROUPS, DEFAULT_RANKING_GROUPS);

	private final SettingsModelStringArray statisticsModel = new SettingsModelStringArray(
			CFGKEY_STATISTICS, DEFAULT_STATISTICS);

	private final SettingsModelStringArray parametersModel = new SettingsModelStringArray(
			CFGKEY_PARAMETERS, DEFAULT_PARAMETERS);

	private final SettingsModelString regulationModel = new SettingsModelString(
			CFGKEY_REGULATION, DEFAULT_REGULATION);

	private final SettingsModelString tieHandlingModel = new SettingsModelString(
			CFGKEY_TIE, DEFAULT_TIE);
	/** experiment, parameter, norm, plate, replicate, stat, rank */
	private final Map<String, Map<String, Map<String, Map<Integer, Map<Integer, Map<StatTypes, double[]>>>>>> ranks = new TreeMap<String, Map<String, Map<String, Map<Integer, Map<Integer, Map<StatTypes, double[]>>>>>>();

	private static final Integer noReplicates = Integer.valueOf(-1);
	private static final Integer noPlates = Integer.valueOf(-1);

	/**
	 * Constructor for the node model.
	 */
	protected RankNodeModel() {
		super(1, 1);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
			final ExecutionContext exec) throws Exception {
		final ModelBuilder modelBuilder = new ModelBuilder(inData[0]);
		final SpecAnalyser specAnalyser = modelBuilder.getSpecAnalyser();
		logger.debug("Ranking the following parameters, and statistics: "
				+ specAnalyser.getParameters() + "; "
				+ specAnalyser.getStatistics());
		exec.checkCanceled();
		final DataColumnSpec[] allColSpecs = createColSpecs(inData[0]
				.getDataTableSpec());
		final DataTableSpec outputSpec = new DataTableSpec(allColSpecs);
		final int experimentIdx = specAnalyser.getExperimentIndex();
		final int normMethodIdx = specAnalyser.getNormMethodIndex();
		final int logTransformIdx = specAnalyser.getLogTransformIndex();
		final int normKindIdx = specAnalyser.getNormKindIndex();
		final int varianceAdjustmentIdx = specAnalyser
				.getVarianceAdjustmentIndex();
		final int scoreMethodIdx = specAnalyser.getScoreMethodIndex();
		final int sumMethodIdx = specAnalyser.getSumMethodIndex();
		final int plateIdx = specAnalyser.getPlateIndex();
		final int replicateIdx = specAnalyser.getReplicateIndex();
		final int wellIdx = specAnalyser.getWellIndex();
		final RankingGroups grouping = RankingGroups.valueOf(groupingModel
				.getStringValue());
		final StatTypes[] statTypes = getStatTypes();
		final String wellAnnotCol = wellAnnotationColumn.getStringValue();
		final String[] parameters = parametersModel.getStringArrayValue();
		final TieHandling tieHandling = TieHandling.valueOf(tieHandlingModel
				.getStringValue());
		final int plateShift = modelBuilder.getMinPlate();
		for (final Entry<String, Map<String, Map<Integer, Map<String, Map<StatTypes, double[]>>>>> scoresEntry : modelBuilder
				.getScores().entrySet()) {
			final String experiment = scoresEntry.getKey();
			ranks
					.put(
							experiment,
							new TreeMap<String, Map<String, Map<Integer, Map<Integer, Map<StatTypes, double[]>>>>>());
			final Map<String, Map<String, Map<Integer, Map<Integer, Map<StatTypes, double[]>>>>> map0 = ranks
					.get(experiment);
			for (final String parameter : parameters) {
				map0
						.put(
								parameter,
								new TreeMap<String, Map<Integer, Map<Integer, Map<StatTypes, double[]>>>>());
				final Map<String, Map<Integer, Map<Integer, Map<StatTypes, double[]>>>> map1 = map0
						.get(parameter);
				for (final Entry<String, Map<Integer, Map<String, Map<StatTypes, double[]>>>> normEntry : scoresEntry
						.getValue().entrySet()) {
					final String normKey = normEntry.getKey();
					map1
							.put(
									normKey,
									new HashMap<Integer, Map<Integer, Map<StatTypes, double[]>>>());
					// original values
					final Map<Integer, Map<Integer, Map<StatTypes, double[]>>> plateMap = map1
							.get(normKey);
					switch (grouping) {
					case experiment:
					case replicate: {
						// parameter, stat, value
						final Map<String, Map<StatTypes, double[]>> sampleValues = new TreeMap<String, Map<StatTypes, double[]>>();
						// parameter, stat, value
						final Map<String, Map<StatTypes, double[]>> otherValues = new TreeMap<String, Map<StatTypes, double[]>>();
						sampleValues.put(parameter,
								new EnumMap<StatTypes, double[]>(
										StatTypes.class));
						otherValues.put(parameter,
								new EnumMap<StatTypes, double[]>(
										StatTypes.class));
						fillEmpty(modelBuilder, statTypes, sampleValues
								.get(parameter));
						fillEmpty(modelBuilder, statTypes, otherValues
								.get(parameter));
						for (final Entry<Integer, Map<String, Map<StatTypes, double[]>>> plateEntry : normEntry
								.getValue().entrySet()) {
							final Integer plate = plateEntry.getKey();
							final String[] wellTypes = modelBuilder.getTexts()
									.get(experiment).get(normKey).get(plate)
									.get(wellAnnotCol);
							if (!plateMap.containsKey(noPlates)) {
								plateMap
										.put(
												noPlates,
												new HashMap<Integer, Map<StatTypes, double[]>>());
								final Map<Integer, Map<StatTypes, double[]>> replicateMap = plateMap
										.get(noPlates);
								replicateMap.put(noReplicates,
										new EnumMap<StatTypes, double[]>(
												StatTypes.class));
							}
							final Map<StatTypes, double[]> sampleMap = sampleValues
									.get(parameter);
							final Map<StatTypes, double[]> otherMap = otherValues
									.get(parameter);
							final Map<StatTypes, double[]> statValues = plateEntry
									.getValue().get(parameter);
							for (final StatTypes stat : statTypes) {
								final double[] samples = sampleMap.get(stat);
								final double[] others = otherMap.get(stat);
								final double[] ds = statValues.get(stat);
								for (int i = ds.length; i-- > 0;) {
									((wellTypes[i].equalsIgnoreCase("sample")) ? samples
											: others)[96
											* (plate.intValue() - plateShift) + i] = ds[i];

								}
							}
							final Map<StatTypes, double[]> rankStatMap = plateMap
									.get(noPlates).get(noReplicates);
							for (final StatTypes stat : statTypes) {
								if (!rankStatMap.containsKey(stat)) {
									rankStatMap
											.put(
													stat,
													new double[96 * (modelBuilder
															.getMaxPlate()
															- plateShift + 1)]);
								}
							}
						}
						for (StatTypes stat : statTypes) {
							final double neutralValue = getNeutralValue(
									regulationModel.getStringValue(), stat,
									parameter);
							final boolean upRegulation = getUpRegulation(
									regulationModel.getStringValue(), stat,
									parameter);
							final double[] rank = plateMap.get(noPlates).get(
									noReplicates).get(stat);
							final double[] samples = sampleValues
									.get(parameter).get(stat);
							final double[] others = otherValues.get(parameter)
									.get(stat);
							computeRanking(tieHandling, neutralValue,
									upRegulation, rank, samples, others);
						}
						break;
					}
					case plate:
					case plateAndReplicate:
						// parameter, stat, plate, value
						final Map<String, Map<StatTypes, Map<Integer, double[]>>> sampleValues = new TreeMap<String, Map<StatTypes, Map<Integer, double[]>>>();
						// parameter, stat, plate, value
						final Map<String, Map<StatTypes, Map<Integer, double[]>>> otherValues = new TreeMap<String, Map<StatTypes, Map<Integer, double[]>>>();
						sampleValues.put(parameter,
								new EnumMap<StatTypes, Map<Integer, double[]>>(
										StatTypes.class));
						otherValues.put(parameter,
								new EnumMap<StatTypes, Map<Integer, double[]>>(
										StatTypes.class));
						fillEmptyEachPlate(modelBuilder, statTypes,
								sampleValues.get(parameter));
						fillEmptyEachPlate(modelBuilder, statTypes, otherValues
								.get(parameter));
						for (final Entry<Integer, Map<String, Map<StatTypes, double[]>>> plateEntry : normEntry
								.getValue().entrySet()) {
							final Integer plate = plateEntry.getKey();
							final String[] wellTypes = modelBuilder.getTexts()
									.get(experiment).get(normKey).get(plate)
									.get(wellAnnotCol);
							if (!plateMap.containsKey(plate)) {
								plateMap
										.put(
												plate,
												new HashMap<Integer, Map<StatTypes, double[]>>());
								final Map<Integer, Map<StatTypes, double[]>> replicateMap = plateMap
										.get(plate);
								replicateMap.put(noReplicates,
										new EnumMap<StatTypes, double[]>(
												StatTypes.class));
							}
							final Map<StatTypes, Map<Integer, double[]>> samplePlateMap = sampleValues
									.get(parameter);
							final Map<StatTypes, Map<Integer, double[]>> otherPlateMap = otherValues
									.get(parameter);
							final Map<StatTypes, double[]> statValues = plateEntry
									.getValue().get(parameter);
							for (final StatTypes stat : statTypes) {
								final Map<Integer, double[]> sampleMap = samplePlateMap
										.get(stat);
								final Map<Integer, double[]> otherMap = otherPlateMap
										.get(stat);
								final double[] samples = sampleMap.get(plate);
								final double[] others = otherMap.get(plate);
								final double[] ds = statValues.get(stat);
								for (int i = ds.length; i-- > 0;) {
									((wellTypes[i].equalsIgnoreCase("sample")) ? samples
											: others)[i] = ds[i];
								}
							}
							final Map<StatTypes, double[]> rankStatMap = plateMap
									.get(plate).get(noReplicates);
							for (final StatTypes stat : statTypes) {
								if (!rankStatMap.containsKey(stat)) {
									rankStatMap.put(stat, new double[96]);
								}
								final double neutralValue = getNeutralValue(
										regulationModel.getStringValue(), stat,
										parameter);
								final boolean upRegulation = getUpRegulation(
										regulationModel.getStringValue(), stat,
										parameter);
								final double[] rank = rankStatMap.get(stat);
								final double[] samples = sampleValues.get(
										parameter).get(stat).get(plate);
								final double[] others = otherValues.get(
										parameter).get(stat).get(plate);
								computeRanking(tieHandling, neutralValue,
										upRegulation, rank, samples, others);
							}
						}
						break;
					default:
						throw new IllegalStateException("Unknown grouping: "
								+ grouping);
					}
				}
			}
		}
		final BufferedDataContainer container = exec
				.createDataContainer(outputSpec);
		for (final DataRow origRow : inData[0]) {
			final List<DataCell> values = new ArrayList<DataCell>(
					allColSpecs.length);
			for (final DataCell dataCell : origRow) {
				values.add(dataCell);
			}
			for (final StatTypes stat : getStatTypes()) {
				for (final String parameter : parameters) {
					values.add(new DoubleCell(getRank(origRow, experimentIdx,
							parameter, normMethodIdx, logTransformIdx,
							normKindIdx, varianceAdjustmentIdx, scoreMethodIdx,
							sumMethodIdx, plateIdx, replicateIdx, wellIdx,
							stat, grouping, plateShift)));
				}
			}
			final DataRow newRow = new DefaultRow(origRow.getKey(), values);
			container.addRowToTable(newRow);
			exec.checkCanceled();
		}
		container.close();
		final BufferedDataTable out = container.getTable();
		return new BufferedDataTable[] { out };
	}

	private void computeRanking(final TieHandling tieHandling,
			final double neutralValue, final boolean upRegulation,
			final double[] rank, final double[] samples, final double[] others) {
		final double[] sampleCopy = Arrays.copyOf(samples, samples.length);
		Arrays.sort(sampleCopy);
		final double[] otherCopy = Arrays.copyOf(others, others.length);
		Arrays.sort(otherCopy);
		final int neutralPos = getPos(neutralValue, sampleCopy);
		handleTies(tieHandling, sampleCopy);
		int sampleCount = 0;
		for (int i = sampleCopy.length; i-- > 0;) {
			if (!Double.isNaN(sampleCopy[i])) {
				sampleCount = i + 1;
				break;
			}
		}
		for (int i = samples.length; i-- > 0;) {
			rank[i] = Double.isNaN(samples[i]) ? getRank(others[i], sampleCopy,
					neutralPos, sampleCount, upRegulation) + .5 : getRank(
					samples[i], sampleCopy, neutralPos, sampleCount,
					upRegulation);
		}
	}

	private void handleTies(final TieHandling tieHandling,
			final double[] sampleCopy) {
		if (tieHandling == TieHandling.continuous) {
			double last = sampleCopy[0];
			int cursor = 1;
			for (int i = 1; i < sampleCopy.length
					&& !Double.isNaN(sampleCopy[i]); ++i) {
				if (sampleCopy[i] != last) {
					sampleCopy[cursor++] = sampleCopy[i];
				}
				last = sampleCopy[i];
			}
			while (cursor < sampleCopy.length) {
				sampleCopy[cursor++] = Double.NaN;
			}
		}
	}

	private double getRank(final double d, final double[] sampleCopy,
			final int neutralPos, final int sampleCount,
			final boolean upregulation) {
		final int pos = getPos(d, sampleCopy);
		return (upregulation ? 1 : -1)
				* (pos >= neutralPos ? sampleCount - pos : -pos - 1);
	}

	private int getPos(final double value, final double[] values) {
		final int binSearch = Arrays.binarySearch(values, value);
		final int pos = binSearch < 0 ? -(binSearch + 1) : binSearch;
		return pos;
	}

	private boolean getUpRegulation(final String regulationPattern,
			final StatTypes stat, final String parameter) {
		// TODO Auto-generated method stub
		return regulationPattern.charAt(regulationPattern.length() - 1) != '-';
	}

	private static double getNeutralValue(final String regulationPattern,
			final StatTypes stat, final String parameter) {
		// TODO Auto-generated method stub
		return Double.parseDouble(regulationPattern.substring(0,
				regulationPattern.length() - 1));
	}

	private void fillEmpty(final ModelBuilder modelBuilder,
			final StatTypes[] statTypes, final Map<StatTypes, double[]> map) {
		for (final StatTypes stat : statTypes) {
			final double[] ds = new double[96 * (modelBuilder.getMaxPlate()
					- modelBuilder.getMinPlate() + 1)];
			for (int i = ds.length; i-- > 0;) {
				ds[i] = Double.NaN;
			}
			map.put(stat, ds);
		}
	}

	private void fillEmptyEachPlate(final ModelBuilder modelBuilder,
			final StatTypes[] statTypes,
			final Map<StatTypes, Map<Integer, double[]>> map) {
		for (final StatTypes stat : statTypes) {
			final Map<Integer, double[]> subMap = new HashMap<Integer, double[]>();
			for (int plate = modelBuilder.getMinPlate(); plate <= modelBuilder
					.getMaxPlate(); ++plate) {
				final double[] ds = new double[96];
				for (int i = ds.length; i-- > 0;) {
					ds[i] = Double.NaN;
				}
				subMap.put(Integer.valueOf(plate), ds);
			}

			map.put(stat, subMap);
		}
	}

	private double getRank(final DataRow origRow, final int experimentIdx,
			final String parameter, final int normMethodIdx,
			final int logTransformIdx, final int normKindIdx,
			final int varianceAdjustmentIdx, final int scoreMethodIdx,
			final int sumMethodIdx, final int plateIdx, final int replicateIdx,
			final int wellIdx, final StatTypes stat, RankingGroups grouping, int plateShift) {
		int plate = ((IntCell) origRow.getCell(plateIdx)).getIntValue();
		return getRank(((StringCell) origRow.getCell(experimentIdx))
				.getStringValue(), parameter, ModelBuilder.getNormKey(origRow,
				normMethodIdx, logTransformIdx, normKindIdx,
				varianceAdjustmentIdx, scoreMethodIdx, sumMethodIdx), plate,
				replicateIdx >= 0 ? ((IntCell) origRow.getCell(replicateIdx))
						.getIntValue() : noReplicates.intValue(), ModelBuilder
						.convertWellToPosition(((StringCell) origRow
								.getCell(wellIdx)).getStringValue()), stat,
				grouping, plateShift);
	}

	private double getRank(final String experiment, final String parameter,
			final String normKey, final int plate, final int replicate,
			final int well, final StatTypes stat, RankingGroups grouping,
			int plateShift) {
		final Map<String, Map<String, Map<Integer, Map<Integer, Map<StatTypes, double[]>>>>> map0 = ranks
				.get(experiment);
		if (map0 == null) {
			return 0.0;
		}
		final Map<String, Map<Integer, Map<Integer, Map<StatTypes, double[]>>>> map1 = map0
				.get(parameter);
		if (map1 == null) {
			return 0.0;
		}
		final Map<Integer, Map<Integer, Map<StatTypes, double[]>>> map2 = map1
				.get(normKey);
		if (map2 == null) {
			return 0.0;
		}
		Integer plateKey;
		switch (grouping) {
		case experiment:
		case replicate:
			plateKey = noPlates;
			break;
		case plate:
		case plateAndReplicate:
			plateKey = Integer.valueOf(plate);
			break;
		default:
			throw new IllegalStateException("Unsupported grouping: " + grouping);
		}
		final Map<Integer, Map<StatTypes, double[]>> map3 = map2.get(plateKey);
		if (map3 == null) {
			return 0.0;
		}
		final Map<StatTypes, double[]> map4 = map3.get(Integer
				.valueOf(replicate));
		if (map4 == null) {
			return 0.0;
		}
		final double[] ds = map4.get(stat);
		if (ds == null) {
			return 0.0;
		}
		int wellPos;
		switch (grouping) {
		case experiment:
		case replicate:
			wellPos = well + (plate - plateShift) * 96;
			break;
		case plate:
		case plateAndReplicate:
			wellPos = well;
			break;
		default:
			throw new UnsupportedOperationException("Not supported: "
					+ grouping);
		}
		return ds[wellPos];
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

		return new DataTableSpec[] { new DataTableSpec(
				createColSpecs(inSpecs[0])) };
	}

	private DataColumnSpec[] createColSpecs(final DataTableSpec dataTableSpec) {
		final StatTypes[] stats = getStatTypes();
		final String[] parameters = parametersModel.getStringArrayValue();
		final DataColumnSpec[] newCols = new DataColumnSpec[stats.length
				* parameters.length];
		for (int i = stats.length; i-- > 0;) {
			for (int j = parameters.length; j-- > 0;) {
				newCols[i * parameters.length + j] = new DataColumnSpecCreator(
						rankPrefix.getStringValue()
								+ StatTypes.mapToPossStats.get(stats[i])
										.getDisplayText() + "_" + parameters[j],
						DoubleCell.TYPE).createSpec();
			}
		}
		final DataColumnSpec[] colSpecs = new DataColumnSpec[dataTableSpec
				.getNumColumns()
				+ newCols.length];
		for (int i = dataTableSpec.getNumColumns(); i-- > 0;) {
			colSpecs[i] = dataTableSpec.getColumnSpec(i);
		}
		System.arraycopy(newCols, 0, colSpecs, dataTableSpec.getNumColumns(),
				newCols.length);
		return colSpecs;
	}

	private StatTypes[] getStatTypes() {
		final String[] statsAsStrings = statisticsModel.getStringArrayValue();
		final StatTypes[] stats = new StatTypes[statsAsStrings.length];
		for (int i = statsAsStrings.length; i-- > 0;) {
			stats[i] = StatTypes.valueOf(statsAsStrings[i]);
		}
		return stats;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveSettingsTo(final NodeSettingsWO settings) {
		wellAnnotationColumn.saveSettingsTo(settings);
		rankPrefix.saveSettingsTo(settings);
		groupingModel.saveSettingsTo(settings);
		statisticsModel.saveSettingsTo(settings);
		parametersModel.saveSettingsTo(settings);
		regulationModel.saveSettingsTo(settings);
		tieHandlingModel.saveSettingsTo(settings);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
			throws InvalidSettingsException {
		wellAnnotationColumn.loadSettingsFrom(settings);
		rankPrefix.loadSettingsFrom(settings);
		groupingModel.loadSettingsFrom(settings);
		statisticsModel.loadSettingsFrom(settings);
		parametersModel.loadSettingsFrom(settings);
		regulationModel.loadSettingsFrom(settings);
		tieHandlingModel.loadSettingsFrom(settings);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void validateSettings(final NodeSettingsRO settings)
			throws InvalidSettingsException {
		wellAnnotationColumn.validateSettings(settings);
		rankPrefix.validateSettings(settings);
		groupingModel.validateSettings(settings);
		statisticsModel.validateSettings(settings);
		parametersModel.validateSettings(settings);
		regulationModel.validateSettings(settings);
		tieHandlingModel.validateSettings(settings);
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

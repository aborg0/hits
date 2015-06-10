package ie.tcd.imm.hits.knime.util;

import ie.tcd.imm.hits.common.Format;
import ie.tcd.imm.hits.knime.cellhts2.prefs.PreferenceConstants.PossibleStatistics;
import ie.tcd.imm.hits.knime.view.heatmap.HeatmapNodeModel.StatTypes;
import ie.tcd.imm.hits.util.Pair;

import java.awt.Color;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.StringValue;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;

import edu.umd.cs.findbugs.annotations.DefaultAnnotation;

/**
 * Constructs a model of the data from a {@link DataTable}. This data is
 * organised by experiment, normalisation parameters, plate, replicate,
 * parameter, statistics.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
@DefaultAnnotation( { Nonnull.class, CheckReturnValue.class, Nonnegative.class })
public class ModelBuilder {
	private final DataTable table;

	private final SpecAnalyser specAnalyser;

	/** key, plate, position [0-95] */
	private final Map<String, Pair<Integer, Integer>> keyToPlateAndPosition = new HashMap<String, Pair<Integer, Integer>>();

	/**
	 * Constructs a {@link ModelBuilder} with creating a {@link SpecAnalyser}
	 * for that {@code table}.
	 * 
	 * @param table
	 *            A {@link DataTable}.
	 */
	public ModelBuilder(final DataTable table) {
		this(table, new SpecAnalyser(table.getDataTableSpec()));
	}

	/**
	 * Constructs a {@link ModelBuilder} with a compatible {@code specAnalyser}
	 * for that {@code table}.
	 * 
	 * @param table
	 *            A {@link DataTable}.
	 * @param specAnalyser
	 *            A {@link SpecAnalyser} compatible with {@code table}.
	 */
	public ModelBuilder(final DataTable table, final SpecAnalyser specAnalyser) {
		super();
		this.table = table;
		this.specAnalyser = specAnalyser;
		generate(specAnalyser);
	}

	/**
	 * Constructs the common column prefix for the {@code stat}
	 * {@link PossibleStatistics}.
	 * 
	 * @param stat
	 *            A {@link PossibleStatistics} value.
	 * @return The {@link PossibleStatistics#getDisplayText() display text}
	 *         followed by {@code _}.
	 */
	public static String createPrefix(final PossibleStatistics stat) {
		return stat.getDisplayText() + "_";
	}

	/** The prefix for {@link PossibleStatistics#RAW_PER_PLATE_REPLICATE_MEAN}. */
	public static final String RAW_PLATE_REPLICATE_MEDIAN_START = createPrefix(PossibleStatistics.RAW_PER_PLATE_REPLICATE_MEAN);

	/** The prefix for {@link PossibleStatistics#MEAN_OR_DIFF}. */
	public static final String MEAN_OR_DIFF_START = createPrefix(PossibleStatistics.MEAN_OR_DIFF);

	/** The prefix for {@link PossibleStatistics#NORMALISED}. */
	public static final String NORMALISED_START = createPrefix(PossibleStatistics.NORMALISED);

	/** The prefix for {@link PossibleStatistics#MEDIAN}. */
	public static final String MEDIAN_START = createPrefix(PossibleStatistics.MEDIAN);

	/** The prefix for {@link PossibleStatistics#RAW}. */
	public static final String RAW_START = createPrefix(PossibleStatistics.RAW);

	/** The prefix for {@link PossibleStatistics#SCORE}. */
	public static final String SCORE_START = createPrefix(PossibleStatistics.SCORE);

	private final Map<String, Map<String, Map<Integer, Map<Integer, Map<String, Map<StatTypes, double[]>>>>>> replicates = new TreeMap<String, Map<String, Map<Integer, Map<Integer, Map<String, Map<StatTypes, double[]>>>>>>();
	private final Map<String, Map<String, Map<Integer, Map<String, Map<StatTypes, double[]>>>>> scores = new TreeMap<String, Map<String, Map<Integer, Map<String, Map<StatTypes, double[]>>>>>();
	private final Map<String, Map<String, Map<Integer, Map<String, String[]>>>> texts = new TreeMap<String, Map<String, Map<Integer, Map<String, String[]>>>>();
	private final Map<String, Map<String, Map<Integer, Color[]>>> colours = new TreeMap<String, Map<String, Map<Integer, Color[]>>>();

	/** The name of the replicate column. */
	public static final String REPLICATE_COLUMN = "Replicate";

	/** The name of the plate column. */
	public static final String PLATE_COLUMN = "Plate";

	/** The name of the experiment column. */
	public static final String EXPERIMENT_COLUMN = "Experiment";

	/** The name of the normalisation method column. */
	public static final String NORMALISATION_METHOD_COLUMN = "Normalisation method";

	/** The name of the log transform column. */
	public static final String LOG_TRANSFORM_COLUMN = "log transform";

	/** The name of the normalisation kind (additive/multiplicative) column. */
	public static final String NORMALISATION_KIND_COLUMN = "Normalisation kind";

	/** The name of the variance adjustment column. */
	public static final String VARIANCE_ADJUSTMENT_COLUMN = "Variance adjustment";

	/** The name of the scoring method column. */
	public static final String SCORING_METHOD_COLUMN = "Scoring method";

	/** The name of the summarise method column. */
	public static final String SUMMARISE_METHOD_COLUMN = "Summarise method";

	private int minReplicate;

	private int maxReplicate;

	private int minPlate;

	private int maxPlate;

	/**
	 * This class is to analyse the structure of the table only by the columns.
	 * It will try to find the proper indices of the most important columns, and
	 * the present {@link SpecAnalyser#getStatistics() statistics},
	 * {@link SpecAnalyser#getParameters() parameters} (union of them).
	 */
	public static class SpecAnalyser implements Serializable {
		private static final long serialVersionUID = -8068918974431507830L;
		private int plateIndex;
		private int replicateIndex;
		private int wellIndex;
		private int experimentIndex;
		private int normMethodIndex;
		private int logTransformIndex;
		private int normKindIndex;
		private int varianceAdjustmentIndex;
		private int scoreMethodIndex;
		private int sumMethodIndex;
		private final EnumSet<StatTypes> statistics;
		private final List<String> parameters;
		private boolean hasReplicate;
		private boolean hasPlate;
		private final EnumMap<StatTypes, Map<String, Integer>> indices;
		private final Map<String, Integer> stringIndices;
		private final Map<String, Integer> valueIndices;

		/**
		 * Constructs a {@link SpecAnalyser} using {@code tableSpec}.
		 * 
		 * @param tableSpec
		 *            A {@link DataTableSpec}.
		 */
		public SpecAnalyser(final DataTableSpec tableSpec) {
			super();
			hasPlate = false;
			hasReplicate = false;
			parameters = new ArrayList<String>();
			statistics = EnumSet.noneOf(StatTypes.class);
			plateIndex = -1;
			replicateIndex = -1;
			wellIndex = -1;
			experimentIndex = -1;
			normMethodIndex = -1;
			logTransformIndex = -1;
			normKindIndex = -1;
			varianceAdjustmentIndex = -1;
			scoreMethodIndex = -1;
			sumMethodIndex = -1;
			indices = new EnumMap<StatTypes, Map<String, Integer>>(
					StatTypes.class);
			for (final StatTypes stat : StatTypes.values()) {
				indices.put(stat, new TreeMap<String, Integer>());
			}
			stringIndices = new TreeMap<String, Integer>();
			valueIndices = new TreeMap<String, Integer>();
			int idx = -1;
			for (final DataColumnSpec spec : tableSpec) {
				++idx;
				final String specName = spec.getName();
				if (specName.startsWith(SCORE_START)) {
					parameters.add(specName.substring(SCORE_START.length()));
					statistics.add(StatTypes.score);
					indices.get(StatTypes.score).put(
							specName.substring(SCORE_START.length()),
							Integer.valueOf(idx));
					continue;
				}
				if (specName.startsWith(RAW_START)) {
					statistics.add(StatTypes.raw);
					indices.get(StatTypes.raw).put(
							specName.substring(RAW_START.length()),
							Integer.valueOf(idx));
					continue;
				}
				if (specName.startsWith(MEDIAN_START)) {
					statistics.add(StatTypes.median);
					indices.get(StatTypes.median).put(
							specName.substring(MEDIAN_START.length()),
							Integer.valueOf(idx));
					continue;
				}
				if (specName.startsWith(NORMALISED_START)) {
					statistics.add(StatTypes.normalized);
					indices.get(StatTypes.normalized).put(
							specName.substring(NORMALISED_START.length()),
							Integer.valueOf(idx));
					continue;
				}
				if (specName.startsWith(MEAN_OR_DIFF_START)) {
					statistics.add(StatTypes.meanOrDiff);
					indices.get(StatTypes.meanOrDiff).put(
							specName.substring(MEAN_OR_DIFF_START.length()),
							Integer.valueOf(idx));
					continue;
				}
				if (specName.startsWith(RAW_PLATE_REPLICATE_MEDIAN_START)) {
					statistics.add(StatTypes.rawPerMedian);
					indices.get(StatTypes.rawPerMedian).put(
							specName.substring(RAW_PLATE_REPLICATE_MEDIAN_START
									.length()), Integer.valueOf(idx));
					continue;
				}
				if (specName.equalsIgnoreCase(ModelBuilder.PLATE_COLUMN)) {
					hasPlate = true;
					plateIndex = Integer.valueOf(idx);
					continue;
				}
				if (specName.equalsIgnoreCase(ModelBuilder.REPLICATE_COLUMN)) {
					hasReplicate = true;
					replicateIndex = idx;
					continue;
				}
				if (specName.equalsIgnoreCase("well")) {
					wellIndex = idx;
					continue;
				}
				if (specName.equalsIgnoreCase(ModelBuilder.EXPERIMENT_COLUMN)) {
					experimentIndex = idx;
				}
				if (specName.equalsIgnoreCase(NORMALISATION_METHOD_COLUMN)) {
					normMethodIndex = idx;
				}
				if (specName.equalsIgnoreCase(LOG_TRANSFORM_COLUMN)) {
					logTransformIndex = idx;
				}
				if (specName.equalsIgnoreCase(NORMALISATION_KIND_COLUMN)) {
					normKindIndex = idx;
				}
				if (specName.equalsIgnoreCase(VARIANCE_ADJUSTMENT_COLUMN)) {
					varianceAdjustmentIndex = idx;
				}
				if (specName.equalsIgnoreCase(SCORING_METHOD_COLUMN)) {
					scoreMethodIndex = idx;
				}
				if (specName.equalsIgnoreCase(SUMMARISE_METHOD_COLUMN)) {
					sumMethodIndex = idx;
				}
				if (spec.getType().isCompatible(StringValue.class)) {
					stringIndices.put(specName, Integer.valueOf(idx));
					continue;
				}
				if (spec.getType().isCompatible(DoubleValue.class)) {
					valueIndices.put(specName, Integer.valueOf(idx));
				}
			}
			if (!hasPlate) {
				throw new IllegalStateException("No plate information found");
			}
		}

		/**
		 * @return The parameters (like Cell 1/Form factor, ...) present in the
		 *         table.
		 */
		public List<String> getParameters() {
			return Collections.unmodifiableList(parameters);
		}

		/**
		 * @return The {@link StatTypes} present in the table.
		 */
		public EnumSet<StatTypes> getStatistics() {
			return statistics;
		}

		/**
		 * @return the plateIndex
		 */
		public int getPlateIndex() {
			return plateIndex;
		}

		/**
		 * @return the replicateIndex
		 */
		public int getReplicateIndex() {
			return replicateIndex;
		}

		/**
		 * @return the wellIndex
		 */
		public int getWellIndex() {
			return wellIndex;
		}

		/**
		 * @return the experimentIndex
		 */
		public int getExperimentIndex() {
			return experimentIndex;
		}

		/**
		 * @return the normMethodIndex
		 */
		public int getNormMethodIndex() {
			return normMethodIndex;
		}

		/**
		 * @return the logTransformIndex
		 */
		public int getLogTransformIndex() {
			return logTransformIndex;
		}

		/**
		 * @return the normKindIndex
		 */
		public int getNormKindIndex() {
			return normKindIndex;
		}

		/**
		 * @return the varianceAdjustmentIndex
		 */
		public int getVarianceAdjustmentIndex() {
			return varianceAdjustmentIndex;
		}

		/**
		 * @return the scoreMethodIndex
		 */
		public int getScoreMethodIndex() {
			return scoreMethodIndex;
		}

		/**
		 * @return the sumMethodIndex
		 */
		public int getSumMethodIndex() {
			return sumMethodIndex;
		}

		/**
		 * @return the hasReplicate
		 */
		public boolean isHasReplicate() {
			return hasReplicate;
		}

		/**
		 * @return the hasPlate
		 */
		public boolean isHasPlate() {
			return hasPlate;
		}

		/**
		 * @return the indices
		 */
		public EnumMap<StatTypes, Map<String, Integer>> getIndices() {
			return indices;
		}

		/**
		 * @return the stringIndices
		 */
		public Map<String, Integer> getStringIndices() {
			return stringIndices;
		}

		/**
		 * @return the valueIndices
		 */
		public Map<String, Integer> getValueIndices() {
			return valueIndices;
		}
	}

	private void generate(final SpecAnalyser specAnalyser) {
		minReplicate = Integer.MAX_VALUE;
		maxReplicate = Integer.MIN_VALUE;
		final int experimentIndex = specAnalyser.getExperimentIndex();
		final int normMethodIndex = specAnalyser.getNormMethodIndex();
		final int logTransformIndex = specAnalyser.getLogTransformIndex();
		final int normKindIndex = specAnalyser.getNormKindIndex();
		final int varianceAdjustmentIndex = specAnalyser
				.getVarianceAdjustmentIndex();
		final int scoreMethodIndex = specAnalyser.getScoreMethodIndex();
		final int sumMethodIndex = specAnalyser.getSumMethodIndex();
		final int plateIndex = specAnalyser.getPlateIndex();
		final int replicateIndex = specAnalyser.getReplicateIndex();
		final int wellIndex = specAnalyser.getWellIndex();
		final EnumMap<StatTypes, Map<String, Integer>> indices = specAnalyser
				.getIndices();
		final Map<String, Integer> stringIndices = specAnalyser
				.getStringIndices();
		final List<String> parameters = specAnalyser.getParameters();
		final EnumSet<StatTypes> statistics = specAnalyser.getStatistics();
		final boolean hasReplicate = specAnalyser.isHasReplicate();
		minPlate = Integer.MAX_VALUE;
		maxPlate = Integer.MIN_VALUE;
		for (final DataRow dataRow : table) {
			final String experiment = ((StringCell) dataRow
					.getCell(experimentIndex)).getStringValue();
			if (!replicates.containsKey(experiment)) {
				replicates
						.put(
								experiment,
								new TreeMap<String, Map<Integer, Map<Integer, Map<String, Map<StatTypes, double[]>>>>>());
			}
			if (!scores.containsKey(experiment)) {
				scores
						.put(
								experiment,
								new TreeMap<String, Map<Integer, Map<String, Map<StatTypes, double[]>>>>());
			}
			if (!texts.containsKey(experiment)) {
				texts
						.put(
								experiment,
								new TreeMap<String, Map<Integer, Map<String, String[]>>>());
			}
			if (!colours.containsKey(experiment)) {
				colours.put(experiment,
						new TreeMap<String, Map<Integer, Color[]>>());
			}
			final Map<String, Map<Integer, Map<Integer, Map<String, Map<StatTypes, double[]>>>>> normMethodValues = replicates
					.get(experiment);
			final Map<String, Map<Integer, Map<String, Map<StatTypes, double[]>>>> scoreNormMethodValues = scores
					.get(experiment);
			final Map<String, Map<Integer, Map<String, String[]>>> textsNormMethodValues = texts
					.get(experiment);
			final Map<String, Map<Integer, Color[]>> colourNormMethodValues = colours
					.get(experiment);
			final String normKey = getNormKey(dataRow, normMethodIndex,
					logTransformIndex, normKindIndex, varianceAdjustmentIndex,
					scoreMethodIndex, sumMethodIndex);
			if (!normMethodValues.containsKey(normKey)) {
				normMethodValues
						.put(
								normKey,
								new HashMap<Integer, Map<Integer, Map<String, Map<StatTypes, double[]>>>>());
			}
			if (!scoreNormMethodValues.containsKey(normKey)) {
				scoreNormMethodValues
						.put(
								normKey,
								new HashMap<Integer, Map<String, Map<StatTypes, double[]>>>());
			}
			if (!textsNormMethodValues.containsKey(normKey)) {
				textsNormMethodValues.put(normKey,
						new HashMap<Integer, Map<String, String[]>>());
			}
			if (!colourNormMethodValues.containsKey(normKey)) {
				colourNormMethodValues.put(normKey,
						new HashMap<Integer, Color[]>());
			}
			final Map<Integer, Map<Integer, Map<String, Map<StatTypes, double[]>>>> replicateValues = normMethodValues
					.get(normKey);
			final Map<Integer, Map<String, Map<StatTypes, double[]>>> scoreValues = scoreNormMethodValues
					.get(normKey);
			final Map<Integer, Map<String, String[]>> textValues = textsNormMethodValues
					.get(normKey);
			final Map<Integer, Color[]> colourValues = colourNormMethodValues
					.get(normKey);
			final Integer plate = getInt(dataRow, plateIndex);
			minPlate = Math.min(minPlate, plate.intValue());
			maxPlate = Math.max(maxPlate, plate.intValue());
			if (!replicateValues.containsKey(plate)) {
				replicateValues
						.put(
								plate,
								new HashMap<Integer, Map<String, Map<StatTypes, double[]>>>());
			}
			if (!scoreValues.containsKey(plate)) {
				scoreValues.put(plate,
						new HashMap<String, Map<StatTypes, double[]>>());
			}
			if (!textValues.containsKey(plate)) {
				final HashMap<String, String[]> map = new HashMap<String, String[]>();
				textValues.put(plate, map);
				for (final String colName : stringIndices.keySet()) {
					map.put(colName, new String[96]);
				}
			}
			if (!colourValues.containsKey(plate)) {
				colourValues.put(plate, new Color[96]);
			}
			final Map<String, String[]> textColumns = textValues.get(plate);
			final int well = convertWellToPosition(((StringCell) dataRow
					.getCell(wellIndex)).getStringValue());
			colourValues.get(plate)[well] = table.getDataTableSpec()
					.getRowColor(dataRow).getColor();
			for (final Entry<String, Integer> entry : stringIndices.entrySet()) {
				textColumns.get(entry.getKey())[well] = ((StringValue) dataRow
						.getCell(entry.getValue().intValue())).getStringValue();
			}
			keyToPlateAndPosition.put(dataRow.getKey().getString(),
					new Pair<Integer, Integer>(plate, Integer.valueOf(well)));
			if (hasReplicate) {
				final Integer replicate = getInt(dataRow, replicateIndex);
				minReplicate = Math.min(replicate.intValue(), minReplicate);
				maxReplicate = Math.max(replicate.intValue(), maxReplicate);
				final Map<Integer, Map<String, Map<StatTypes, double[]>>> outerMap = replicateValues
						.get(plate);
				if (!outerMap.containsKey(replicate)) {
					outerMap.put(replicate,
							new HashMap<String, Map<StatTypes, double[]>>());
				}
				final Map<String, Map<StatTypes, double[]>> paramMap = outerMap
						.get(replicate);
				for (final String param : parameters) {
					if (!paramMap.containsKey(param)) {
						paramMap.put(param, new EnumMap<StatTypes, double[]>(
								StatTypes.class));
					}
					final Map<StatTypes, double[]> enumMap = paramMap
							.get(param);
					for (final StatTypes stat : StatTypes.replicateTypes) {
						if (!statistics.contains(stat)) {
							continue;
						}
						if (!enumMap.containsKey(stat)) {
							enumMap.put(stat, createPlateValues(96));
						}
						enumMap.get(stat)[well] = ((DoubleValue) dataRow
								.getCell(indices.get(stat).get(param)
										.intValue())).getDoubleValue();
					}
				}
			}
			if (statistics.contains(StatTypes.score)
					|| statistics.contains(StatTypes.meanOrDiff)
					|| statistics.contains(StatTypes.median)) {
				final Map<String, Map<StatTypes, double[]>> map = scoreValues
						.get(plate);
				for (final String param : parameters) {
					if (!map.containsKey(param)) {
						map.put(param, new EnumMap<StatTypes, double[]>(
								StatTypes.class));
						for (final StatTypes type : StatTypes.scoreTypes) {
							map.get(param).put(type, createPlateValues(96));
						}
					}
					final Map<StatTypes, double[]> values = map.get(param);
					for (final StatTypes type : StatTypes.scoreTypes) {
						if (!statistics.contains(type)) {
							continue;
						}
						final double[] vals = values.get(type);
						if (false) {
							vals[well] = param.equals("Cell Count") ? 0.0
									: (param.equals("Nuc Displacement")) ? 1
											: -1;
						} else {
							vals[well] = ((DoubleValue) dataRow.getCell(indices
									.get(type).get(param).intValue()))
									.getDoubleValue();
						}
					}
				}
			}
		}
	}

	/**
	 * Creates a {@code double} array with length {@code count} and fills with
	 * {@link Double#NaN}.
	 * 
	 * @param count
	 *            The length of the returned {@code double} array.
	 * @return An array with {@link Double#NaN}s.
	 */
	public static double[] createPlateValues(final int count) {
		final double[] ds = new double[count];
		Arrays.fill(ds, Double.NaN);
		return ds;
	}

	/**
	 * Creates a {@link String} for the normalisation/scoring parameters.
	 * 
	 * @param dataRow
	 *            A {@link DataRow}.
	 * @param normMethodIdx
	 *            A {@code 0} based index for normalisation method.
	 * @param logTransformIdx
	 *            A {@code 0} based index for log transform.
	 * @param normKindIdx
	 *            A {@code 0} based index for multiplicative/additive
	 *            normalisation.
	 * @param varianceAdjustmentIdx
	 *            A {@code 0} based index for variance adjustment.
	 * @param scoreMethodIdx
	 *            A {@code 0} based index for scoring method.
	 * @param summariseMethodIdx
	 *            A {@code 0} based index for summarisation method of
	 *            replicates.
	 * @return A {@link String} created by the values from {@link DataRow}
	 *         separated by {@code _}.
	 */
	public static String getNormKey(final DataRow dataRow,
			final int normMethodIdx, final int logTransformIdx,
			final int normKindIdx, final int varianceAdjustmentIdx,
			final int scoreMethodIdx, final int summariseMethodIdx) {
		return dataRow.getCell(normMethodIdx) + "_"
				+ dataRow.getCell(logTransformIdx) + "_"
				+ dataRow.getCell(normKindIdx) + "_"
				+ dataRow.getCell(varianceAdjustmentIdx) + "_"
				+ dataRow.getCell(scoreMethodIdx) + "_"
				+ dataRow.getCell(summariseMethodIdx);
	}

	/**
	 * Converts a {@code [a-hA-H](0)?[0-9]} like {@code well} value to a
	 * {@code 96} plate position. The position is {@code 0}-based, and goes
	 * from {@code A1} &Rarr; {@code A12} &Rarr; {@code B1}, ..., {@code H12}.
	 * 
	 * @param well
	 *            A {@link String} matching the pattern:
	 *            {@code [a-hA-H](0)?[0-9]}.
	 * @return The {@code 0} based position on the 96 well plate. (Horizontal
	 *         first, then vertical.)
	 * @see Format#convertWellToPosition(String)
	 */
	@Deprecated
	public static int convertWellToPosition(final String well) {
		return ((Character.toLowerCase(well.charAt(0)) - 'a')
				* 12
				+ Integer.parseInt(well.substring(well.length() - 2, well
						.length())) - 1);
	}

	/**
	 * Selects the {@code index + 1}<sup>th</sup> cell value from
	 * {@code dataRow}.
	 * 
	 * @param dataRow
	 *            A {@link DataRow}.
	 * @param index
	 *            A {@code 0}-based index to select a cell in {@code dataRow}.
	 * @return The value at {@code index} position in {@code dataRow}.
	 * @throws ClassCastException
	 *             if the cell is not an {@link IntCell}.
	 * @throws ArrayIndexOutOfBoundsException
	 *             if the {@code index} is not valid.
	 */
	public static Integer getInt(final DataRow dataRow, final int index) {
		return Integer
				.valueOf(((IntCell) dataRow.getCell(index)).getIntValue());
	}

	/**
	 * @return the table
	 */
	public DataTable getTable() {
		return table;
	}

	/**
	 * @return the replicate dependent {@link StatTypes statistics} of
	 *         {@link #getTable() table}, the dimensions are these:
	 *         <ul>
	 *         <li>experiment</li>
	 *         <li>normalisations ({@link #getNormKey(DataRow, int, int, int, int, int, int)})</li>
	 *         <li>plate</li>
	 *         <li>replicate</li>
	 *         <li>parameter</li>
	 *         <li>statistics type</li>
	 *         <li>{@double double}s to for each position on plate. (May
	 *         contain {@link Double#NaN} values)</li>
	 *         </ul>
	 * @see StatTypes#isUseReplicates() {@code true}.
	 */
	public Map<String, Map<String, Map<Integer, Map<Integer, Map<String, Map<StatTypes, double[]>>>>>> getReplicates() {
		return replicates;
	}

	/**
	 * @return the replicate independent {@link StatTypes statistics} of
	 *         {@link #getTable() table}, the dimensions are these:
	 *         <ul>
	 *         <li>experiment</li>
	 *         <li>normalisations ({@link #getNormKey(DataRow, int, int, int, int, int, int)})</li>
	 *         <li>plate</li>
	 *         <li>parameter</li>
	 *         <li>statistics type</li>
	 *         <li>{@double double}s to for each position on plate. (May
	 *         contain {@link Double#NaN} values)</li>
	 *         </ul>
	 * @see StatTypes#isUseReplicates() {@code false}.
	 */
	public Map<String, Map<String, Map<Integer, Map<String, Map<StatTypes, double[]>>>>> getScores() {
		return scores;
	}

	/**
	 * @return the texts of {@link #getTable() table}, the dimensions are
	 *         these:
	 *         <ul>
	 *         <li>experiment</li>
	 *         <li>normalisations ({@link #getNormKey(DataRow, int, int, int, int, int, int)})</li>
	 *         <li>plate</li>
	 *         <li>column name</li>
	 *         <li>{@link String}s for each position on plate. (May contain
	 *         {@code null} values)</li>
	 *         </ul>
	 */
	public Map<String, Map<String, Map<Integer, Map<String, String[]>>>> getTexts() {
		return texts;
	}

	/**
	 * @return the colours of {@link #getTable() table}, the dimensions are
	 *         these:
	 *         <ul>
	 *         <li>experiment</li>
	 *         <li>normalisations ({@link #getNormKey(DataRow, int, int, int, int, int, int)})</li>
	 *         <li>plate</li>
	 *         <li>{@link Color}s for each position on plate. (May contain
	 *         {@code null} values)</li>
	 *         </ul>
	 */
	public Map<String, Map<String, Map<Integer, Color[]>>> getColours() {
		return colours;
	}

	/**
	 * @return the {@link SpecAnalyser} associated to {@link #getTable() table}.
	 */
	public SpecAnalyser getSpecAnalyser() {
		return specAnalyser;
	}

	/**
	 * @return the smallest plate value
	 */
	public int getMinPlate() {
		return minPlate;
	}

	/**
	 * @return the largest plate value
	 */
	public int getMaxPlate() {
		return maxPlate;
	}

	/**
	 * @return the smallest replicate value
	 */
	public int getMinReplicate() {
		return minReplicate;
	}

	/**
	 * @return the largest replicate value
	 */
	public int getMaxReplicate() {
		return maxReplicate;
	}

	/**
	 * @return An unmodifiable {@link Map} from the row ids to a {@link Pair} of
	 *         plate, position ({@code 0-95}) values.
	 */
	public Map<String, Pair<Integer, Integer>> getKeyToPlateAndPosition() {
		return Collections.unmodifiableMap(keyToPlateAndPosition);
	}
}

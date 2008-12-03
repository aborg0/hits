package ie.tcd.imm.hits.knime.util;

import ie.tcd.imm.hits.knime.cellhts2.prefs.PreferenceConstants.PossibleStatistics;
import ie.tcd.imm.hits.knime.view.heatmap.HeatmapNodeModel;
import ie.tcd.imm.hits.knime.view.heatmap.HeatmapNodeModel.StatTypes;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
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
@DefaultAnnotation( { Nonnull.class, CheckReturnValue.class })
public class ModelBuilder implements Serializable {
	private static final long serialVersionUID = -5713668438238539234L;

	private final DataTable table;

	public ModelBuilder(final DataTable table) {
		super();
		this.table = table;
		generate();
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

	private Map<String, Map<String, Map<Integer, Map<Integer, Map<String, Map<StatTypes, double[]>>>>>> replicates;
	private Map<String, Map<String, Map<Integer, Map<String, Map<StatTypes, double[]>>>>> scores;

	private EnumSet<StatTypes> statistics;

	private List<String> parameters;

	public Map<String, Map<String, Map<Integer, Map<Integer, Map<String, Map<StatTypes, double[]>>>>>> createReplicatesMap() {
		return replicates;
	}

	private void generate() {
		replicates = new TreeMap<String, Map<String, Map<Integer, Map<Integer, Map<String, Map<StatTypes, double[]>>>>>>();
		boolean hasPlate = false;
		boolean hasReplicate = false;
		parameters = new ArrayList<String>();
		statistics = EnumSet.noneOf(StatTypes.class);
		int plateIndex = -1;
		int replicateIndex = -1;
		int wellIndex = -1;
		int experimentIndex = -1;
		final EnumMap<StatTypes, Map<String, Integer>> indices = new EnumMap<StatTypes, Map<String, Integer>>(
				StatTypes.class);
		for (final StatTypes stat : StatTypes.values()) {
			indices.put(stat, new TreeMap<String, Integer>());
		}
		final Map<String, Integer> stringIndices = new TreeMap<String, Integer>();
		final Map<String, Integer> valueIndices = new TreeMap<String, Integer>();
		int idx = -1;
		for (final DataColumnSpec spec : table.getDataTableSpec()) {
			++idx;
			if (spec.getName().startsWith(SCORE_START)) {
				parameters.add(spec.getName().substring(SCORE_START.length()));
				statistics.add(StatTypes.score);
				indices.get(StatTypes.score).put(
						spec.getName().substring(SCORE_START.length()),
						Integer.valueOf(idx));
				continue;
			}
			if (spec.getName().startsWith(RAW_START)) {
				statistics.add(StatTypes.raw);
				indices.get(StatTypes.raw).put(
						spec.getName().substring(RAW_START.length()),
						Integer.valueOf(idx));
				continue;
			}
			if (spec.getName().startsWith(MEDIAN_START)) {
				statistics.add(StatTypes.median);
				indices.get(StatTypes.median).put(
						spec.getName().substring(MEDIAN_START.length()),
						Integer.valueOf(idx));
				continue;
			}
			if (spec.getName().startsWith(NORMALISED_START)) {
				statistics.add(StatTypes.normalized);
				indices.get(StatTypes.normalized).put(
						spec.getName().substring(NORMALISED_START.length()),
						Integer.valueOf(idx));
				continue;
			}
			if (spec.getName().startsWith(MEAN_OR_DIFF_START)) {
				statistics.add(StatTypes.meanOrDiff);
				indices.get(StatTypes.meanOrDiff).put(
						spec.getName().substring(MEAN_OR_DIFF_START.length()),
						Integer.valueOf(idx));
				continue;
			}
			if (spec.getName().startsWith(RAW_PLATE_REPLICATE_MEDIAN_START)) {
				statistics.add(StatTypes.rawPerMedian);
				indices.get(StatTypes.rawPerMedian).put(
						spec.getName().substring(
								RAW_PLATE_REPLICATE_MEDIAN_START.length()),
						Integer.valueOf(idx));
				continue;
			}
			if (spec.getName().equalsIgnoreCase("plate")) {
				hasPlate = true;
				plateIndex = Integer.valueOf(idx);
				continue;
			}
			if (spec.getName().equalsIgnoreCase("replicate")) {
				hasReplicate = true;
				replicateIndex = idx;
				continue;
			}
			if (spec.getName().equalsIgnoreCase("well")) {
				wellIndex = idx;
				continue;
			}
			if (spec.getName().equalsIgnoreCase("experiment")) {
				experimentIndex = idx;
				continue;
			}
			if (spec.getType().isCompatible(StringValue.class)) {
				stringIndices.put(spec.getName(), Integer.valueOf(idx));
				continue;
			}
			if (spec.getType().isCompatible(DoubleValue.class)) {
				valueIndices.put(spec.getName(), Integer.valueOf(idx));
			}
		}
		if (!hasPlate) {
			throw new IllegalStateException("No plate information found");
		}
		int minReplicate = Integer.MAX_VALUE;
		int maxReplicate = Integer.MIN_VALUE;
		// final int minPlate = Integer.MAX_VALUE, maxPlate = Integer.MIN_VALUE;
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
			final Map<String, Map<Integer, Map<Integer, Map<String, Map<StatTypes, double[]>>>>> normMethodValues = replicates
					.get(experiment);
			final Map<String, Map<Integer, Map<String, Map<StatTypes, double[]>>>> scoreNormMethodValues = scores
					.get(experiment);
			final String normKey = getNormKey(dataRow /* TODO add norm indices */);
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
			final Map<Integer, Map<Integer, Map<String, Map<StatTypes, double[]>>>> replicateValues = normMethodValues
					.get(normKey);
			final Map<Integer, Map<String, Map<StatTypes, double[]>>> scoreValues = scoreNormMethodValues
					.get(normKey);
			final Integer plate = getInt(dataRow, plateIndex);
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
			// if (!texts.containsKey(plate)) {
			// final HashMap<String, String[]> map = new HashMap<String,
			// String[]>();
			// texts.put(plate, map);
			// for (final String colName : stringIndices.keySet()) {
			// map.put(colName, new String[96]);
			// }
			// }
			final int well = convertWellToPosition(((StringCell) dataRow
					.getCell(wellIndex)).getStringValue());
			// keyToPlateAndPosition.put(dataRow.getKey().getId(),
			// new Pair<Integer, Integer>(plate, Integer.valueOf(well)));
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
					for (final StatTypes stat : HeatmapNodeModel.replicateTypes) {
						if (!enumMap.containsKey(stat)) {
							enumMap.put(stat, new double[96]);
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
						for (final StatTypes type : HeatmapNodeModel.scoreTypes) {
							map.get(param).put(type, new double[96]);
						}
					}
					final Map<StatTypes, double[]> values = map.get(param);
					for (final StatTypes type : new StatTypes[] {
							StatTypes.score, StatTypes.median,
							StatTypes.meanOrDiff }) {

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

	private static String getNormKey(final DataRow dataRow) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("TODO implement!");
		// return null;
	}

	public static int convertWellToPosition(final String well) {
		return ((Character.toLowerCase(well.charAt(0)) - 'a')
				* 12
				+ Integer.parseInt(well.substring(well.length() - 2, well
						.length())) - 1);
	}

	public static Integer getInt(final DataRow dataRow, final int plateIndex) {
		return Integer.valueOf(((IntCell) dataRow.getCell(plateIndex))
				.getIntValue());
	}

	public List<String> getParameters() {
		return Collections.unmodifiableList(parameters);
	}

	public EnumSet<StatTypes> getStatistics() {
		return statistics;
	}
}

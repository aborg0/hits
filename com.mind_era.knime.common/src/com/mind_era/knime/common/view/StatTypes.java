package com.mind_era.knime.common.view;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.mind_era.knime.common.PossibleStatistics;

/**
 * This enum lists all supported statistic types. Any other has no special
 * handling.
 */
public enum StatTypes {
	/** The score statistic */
	score(false, true, false),
	/** The raw values (each replicates) */
	raw(true, true, false),
	/** The median of replicates */
	median(false, true, false),
	/** The mean or the diff of replicates */
	meanOrDiff(false, true, false),
	/** The normalised values (each replicates) */
	normalised(true, true, false),
	/** The raw value divided by the (plate, replicate) median */
	rawPerMedian(true, true, false),
	/** Ranking using the replicate value */
	rankReplicates(true, true, true),
	/** Ranking <em>not</em> using the replicate value */
	rankNonReplicates(false, true, true),
	/** The experiment name. */
	experimentName(false, false, true),
	/** The normalisation, scoring parameters. */
	normalisation(false, false, true),
	/** Any other numeric value from the table (replicate specific) */
	otherNumeric(true, false, false),
	/** Any other enumerated value from the table (non replicate specific) */
	otherEnumeration(false, false, true),
	/** The plate index */
	plate(false, false, true),
	/** The replicate index */
	replicate(true, false, true),
	/** The parameters, like 'Cell 1/(Form Factor)', ... */
	parameter(false, true, true),
	/** A 'meta' StatTypes, this is for selection from the first 8 values. */
	metaStatType(false, true, true);

	/** Different values for each replicate? */
	private final boolean isUseReplicates;
	/** Different values for each parameter? */
	private final boolean isDependOnParameters;
	/** The values are enumerable, or real values? */
	private final boolean isDiscrete;

	private StatTypes(final boolean isUseReplicates,
			final boolean isDependOnParameters, final boolean isDiscrete) {
		this.isUseReplicates = isUseReplicates;
		this.isDependOnParameters = isDependOnParameters;
		this.isDiscrete = isDiscrete;
	}

	/**
	 * @return Tells whether if it has different values for the parameters
	 *         or not.
	 */
	public boolean isDependOnParameters() {
		return isDependOnParameters;
	}

	/**
	 * @return The values are discrete, or those are from a continuous real
	 *         interval.
	 */
	public boolean isDiscrete() {
		return isDiscrete;
	}

	/**
	 * @return Does it depend on the replicate parameter?
	 */
	public boolean isUseReplicates() {
		return isUseReplicates;
	}

	/**
	 * A mapping from some {@link StatTypes} &Rarr;
	 * {@link PossibleStatistics}.
	 */
	public static final Map<StatTypes, PossibleStatistics> mapToPossStats;
	/**
	 * The {@link StatTypes} with {@link StatTypes#isUseReplicates()}
	 * {@code false}.
	 */
	public static final List<StatTypes> scoreTypes = Collections
			.unmodifiableList(Arrays.asList(new StatTypes[] {
					StatTypes.score, StatTypes.median,
					StatTypes.meanOrDiff, StatTypes.rankNonReplicates }));
	/**
	 * The {@link StatTypes} with {@link StatTypes#isUseReplicates()}
	 * {@code true}.
	 */
	public static final List<StatTypes> replicateTypes = Collections
			.unmodifiableList(Arrays.asList(new StatTypes[] {
					StatTypes.raw, StatTypes.rawPerMedian,
					StatTypes.normalised, StatTypes.rankReplicates }));
	static {
		final EnumMap<StatTypes, PossibleStatistics> map = new EnumMap<StatTypes, PossibleStatistics>(
				StatTypes.class);
		map.put(StatTypes.score, PossibleStatistics.SCORE);
		map.put(StatTypes.normalised, PossibleStatistics.NORMALISED);
		map.put(StatTypes.median, PossibleStatistics.MEDIAN);
		map.put(StatTypes.meanOrDiff, PossibleStatistics.MEAN_OR_DIFF);
		map.put(StatTypes.raw, PossibleStatistics.RAW);
		map.put(StatTypes.rawPerMedian,
				PossibleStatistics.RAW_PER_PLATE_REPLICATE_MEAN);
		map.put(StatTypes.otherNumeric, PossibleStatistics.RAW);

		mapToPossStats = Collections.unmodifiableMap(map);
	}
}
package ie.tcd.imm.hits.common;

import ie.tcd.imm.hits.util.Displayable;

/** The possible values of the statistics. */
public enum PossibleStatistics implements Displayable {
	/** raw values */
	RAW("raw", Multiplicity.CHANNELS_AND_REPLICATES, Type.Real),
	/** median values */
	MEDIAN("median", Multiplicity.CHANNELS, Type.Real),
	/** mean, or for two replicates: diff */
	MEAN_OR_DIFF("mean or diff", "diffOrMean", Multiplicity.CHANNELS, Type.Real),
	/** plate value (from {@code 1}) */
	PLATE("Plate", "plate", Multiplicity.SINGLE, Type.Int),
	/** Position on plate (from {@code 1}) */
	POSITION("Position", "position", Multiplicity.SINGLE, Type.Int),
	/** replicate value (from {@code 1}) */
	REPLICATE("Replicate", "replicate", Multiplicity.SINGLE, Type.Int),
	/** Well value */
	WELL("Well", "well", Multiplicity.SINGLE, Type.Strings),
	/** score values */
	SCORE("score", Multiplicity.CHANNELS, Type.Real),
	/** well annotations */
	WELL_ANNOTATION("well annotation", "wellAnno", Multiplicity.SINGLE,
			Type.Strings),
	/** The final well annotations */
	FINAL_WELL_ANNOTATION("final well annotation", "finalWellAnno",
			Multiplicity.CHANNELS, Type.Strings),
	/** The gene id */
	GENE_ID("Gene id", "geneID", Multiplicity.SINGLE, Type.Int),
	/** The gene symbols */
	GENE_SYMBOL("Gene symbol", "geneSymbol", Multiplicity.SINGLE, Type.Strings),
	/** The gene annotations */
	GENE_ANNOTATION("Gene annotation", "geneAnnotation"),
	/** The normalised values */
	NORMALISED("normalised", "norm", Multiplicity.CHANNELS_AND_REPLICATES,
			Type.Real),
	/** The raw values divided by the plate and replicate means */
	RAW_PER_PLATE_REPLICATE_MEAN("raw/(plate, replicate mean)", "rawPerMedian",
			Multiplicity.CHANNELS_AND_REPLICATES, Type.Real),
	/** A separator column */
	SEPARATOR("empty", Multiplicity.SINGLE, Type.Strings),
	/** Start of a block taking use of replicates */
	GROUP_BY_REPLICATES_START("group by replicates start",
			"replicateGroupStart", Multiplicity.NONE, Type.Strings),
	/** End of a block taking use of replicates */
	GROUP_BY_REPLICATES_END("group by replicates end", "replicateGroupEnd",
			Multiplicity.NONE, Type.Strings),
	/** Start of a block taking use of channels */
	GROUP_BY_CHANNELS_START("group by channels start", "channelGroupStart",
			Multiplicity.NONE, Type.Strings),
	/** End of a block taking use of channels */
	GROUP_BY_CHANNELS_END("group by channels end", "channelGroupEnd",
			Multiplicity.NONE, Type.Strings);
	private final String displayText;
	private final String rCode;

	/** How many columns does this represent */
	public static enum Multiplicity {
		/** Only one */
		SINGLE,
		/** As many as channels/parameters you have */
		CHANNELS,
		/** As many as replicates you have */
		REPLICATES,
		/** Times of the parameters/channels and replicates you have */
		CHANNELS_AND_REPLICATES,
		/** unspecified/unknown at this stage (the remaining ones) */
		UNSPECIFIED,
		/** Special, none */
		NONE;
	}

	private final PossibleStatistics.Multiplicity multiplicity;

	/**
	 * The type of the values of the columns
	 */
	public static enum Type {
		/** integral */
		Int,
		/** real numbers */
		Real,
		/** strings */
		Strings
	}

	private final PossibleStatistics.Type type;

	private PossibleStatistics(final String displayText) {
		this(displayText, displayText);
	}

	private PossibleStatistics(final String displayText, final String rCode) {
		this(displayText, rCode, Multiplicity.UNSPECIFIED, Type.Strings);
	}

	private PossibleStatistics(final String displayText,
			final PossibleStatistics.Multiplicity multiplicity,
			final PossibleStatistics.Type type) {
		this(displayText, displayText, multiplicity, type);
	}

	private PossibleStatistics(final String displayText, final String rCode,
			final PossibleStatistics.Multiplicity multiplicity,
			final PossibleStatistics.Type type) {
		this.displayText = displayText;
		this.rCode = rCode;
		this.multiplicity = multiplicity;
		this.type = type;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getDisplayText() {
		return displayText;
	}

	/**
	 * @return The R code representing this option.
	 */
	public String getRCode() {
		return rCode;
	}

	/**
	 * @return The multiplicity of this option.
	 */
	public PossibleStatistics.Multiplicity getMultiplicity() {
		return multiplicity;
	}

	/**
	 * @return The type of the column values.
	 */
	public PossibleStatistics.Type getType() {
		return type;
	}
}
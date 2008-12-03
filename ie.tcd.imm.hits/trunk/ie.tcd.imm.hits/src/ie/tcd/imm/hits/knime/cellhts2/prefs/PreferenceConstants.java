package ie.tcd.imm.hits.knime.cellhts2.prefs;

/**
 * Constant definitions for plug-in preferences
 */
public class PreferenceConstants {
	/** Configuration key for the column order property. */
	public static final String RESULT_COL_ORDER = "ie.tcd.imm.hits.knime.resultColumnOrder";

	/** The possible values of the statistics. */
	public static enum PossibleStatistics implements Displayable {
		/** raw values */
		RAW("raw", Multiplicity.CHANNELS_AND_REPLICATES, Type.Real),
		/** median values */
		MEDIAN("median", Multiplicity.CHANNELS, Type.Real),
		/** mean, or for two replicates: diff */
		MEAN_OR_DIFF("mean or diff", "diffOrMean", Multiplicity.CHANNELS,
				Type.Real),
		/** plate value (from {@code 1}) */
		PLATE("plate", Multiplicity.SINGLE, Type.Int),
		/** Position on plate (from {@code 1}) */
		POSITION("position", Multiplicity.SINGLE, Type.Int),
		/** replicate value (from {@code 1}) */
		REPLICATE("replicate", Multiplicity.SINGLE, Type.Int),
		/** Well value */
		WELL("well", Multiplicity.SINGLE, Type.Strings),
		/** score values */
		SCORE("score", Multiplicity.CHANNELS, Type.Real),
		/** well annotations */
		WELL_ANNOTATION("well annotation", "wellAnno", Multiplicity.SINGLE,
				Type.Strings),
		/** The final well annotations */
		FINAL_WELL_ANNOTATION("final well annotation", "finalWellAnno",
				Multiplicity.CHANNELS, Type.Strings),
		/** The gene id */
		GENE_ID("gene id", "geneID", Multiplicity.SINGLE, Type.Int),
		/** The gene symbols */
		GENE_SYMBOL("gene symbol", "geneSymbol", Multiplicity.SINGLE,
				Type.Strings),
		/** The gene annotations */
		GENE_ANNOTATION("gene annotation", "geneAnnotation"),
		/** The normalised values */
		NORMALIZED("normalized", "norm", Multiplicity.CHANNELS_AND_REPLICATES,
				Type.Real),
		/** The raw values divided by the plate and replicate means */
		RAW_PER_PLATE_REPLICATE_MEAN("raw/(plate, replicate mean)",
				"rawPerMedian", Multiplicity.CHANNELS_AND_REPLICATES, Type.Real),
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

		private final Multiplicity multiplicity;

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

		private final Type type;

		private PossibleStatistics(final String displayText) {
			this(displayText, displayText);
		}

		private PossibleStatistics(final String displayText, final String rCode) {
			this(displayText, rCode, Multiplicity.UNSPECIFIED, Type.Strings);
		}

		private PossibleStatistics(final String displayText,
				final Multiplicity multiplicity, final Type type) {
			this(displayText, displayText, multiplicity, type);
		}

		private PossibleStatistics(final String displayText,
				final String rCode, final Multiplicity multiplicity,
				final Type type) {
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
		public Multiplicity getMultiplicity() {
			return multiplicity;
		}

		/**
		 * @return The type of the column values.
		 */
		public Type getType() {
			return type;
		}
	}

	/** Key for usage of the TCD cellHTS2 extensions preference. */
	public static final String USE_TCD_EXTENSIONS = "ie.tcd.imm.hits.knime.useTCDExtensions";

	/**
	 * Key for usage of the channel names instead of the original ch_n names.
	 * (Should be available only if used with TCD extensions.)
	 */
	public static final String USE_NAMES_INSTEAD_OF_CHANNELS = "ie.tcd.imm.hits.knime.namesInsteadOfChannels";
}

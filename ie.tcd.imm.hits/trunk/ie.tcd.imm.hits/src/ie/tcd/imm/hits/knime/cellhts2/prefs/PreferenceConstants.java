package ie.tcd.imm.hits.knime.cellhts2.prefs;

/**
 * Constant definitions for plug-in preferences
 */
public class PreferenceConstants {
	public static final String RESULT_COL_ORDER = "ie.tcd.imm.hits.knime.resultColumnOrder";

	public static enum PossibleStatistics implements Displayable {
		RAW("raw", Multiplicity.CHANNELS_AND_REPLICATES, Type.Real), MEDIAN(
				"median", Multiplicity.CHANNELS, Type.Real), MEAN_OR_DIFF(
				"mean or diff", "diffOrMean", Multiplicity.CHANNELS, Type.Real), PLATE(
				"plate", Multiplicity.SINGLE, Type.Int), POSITION("position",
				Multiplicity.SINGLE, Type.Int), REPLICATE("replicate",
				Multiplicity.SINGLE, Type.Int), WELL("well",
				Multiplicity.SINGLE, Type.Strings), SCORE("score",
				Multiplicity.CHANNELS, Type.Real), WELL_ANNOTATION(
				"well annotation", "wellAnno", Multiplicity.SINGLE,
				Type.Strings), FINAL_WELL_ANNOTATION("final well annotation",
				"finalWellAnno", Multiplicity.REPLICATES, Type.Strings), GENE_ID(
				"gene id", "geneID", Multiplicity.SINGLE, Type.Int), GENE_SYMBOL(
				"gene symbol", "geneSymbol", Multiplicity.SINGLE, Type.Strings), GENE_ANNOTATION(
				"gene annotation", "geneAnnotation"), NORMALIZED("normalized",
				"norm", Multiplicity.CHANNELS_AND_REPLICATES, Type.Real), RAW_PER_PLATE_REPLICATE_MEAN(
				"raw/(plate, replicate mean)", "rawPerMedian",
				Multiplicity.CHANNELS_AND_REPLICATES, Type.Real), SEPARATOR(
				"empty", Multiplicity.SINGLE, Type.Strings), GROUP_BY_REPLICATES_START(
				"group by replicates start", "replicateGroupStart",
				Multiplicity.NONE, Type.Strings), GROUP_BY_REPLICATES_END(
				"group by replicates end", "replicateGroupEnd",
				Multiplicity.NONE, Type.Strings), GROUP_BY_CHANNELS_START(
				"group by channels start", "channelGroupStart",
				Multiplicity.NONE, Type.Strings), GROUP_BY_CHANNELS_END(
				"group by channels end", "channelGroupEnd", Multiplicity.NONE,
				Type.Strings);
		private final String displayText;
		private final String rCode;

		public static enum Multiplicity {
			SINGLE, CHANNELS, REPLICATES, CHANNELS_AND_REPLICATES, UNSPECIFIED, NONE;
		}

		private final Multiplicity multiplicity;

		public static enum Type {
			Int, Real, Strings
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

		@Override
		public String getDisplayText() {
			return displayText;
		}

		public String getRCode() {
			return rCode;
		}

		public Multiplicity getMultiplicity() {
			return multiplicity;
		}

		public Type getType() {
			return type;
		}
	}

	public static final String USE_TCD_EXTENSIONS = "ie.tcd.imm.hits.knime.useTCDExtensions";

	public static final String USE_NAMES_INSTEAD_OF_CHANNELS = "ie.tcd.imm.hits.knime.namesInsteadOfChannels";
}

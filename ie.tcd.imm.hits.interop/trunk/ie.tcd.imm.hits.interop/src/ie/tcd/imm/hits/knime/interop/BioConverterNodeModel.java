package ie.tcd.imm.hits.knime.interop;

import ie.tcd.imm.hits.knime.util.TransformingNodeModel;
import ie.tcd.imm.hits.util.Displayable;

import java.util.HashSet;
import java.util.Set;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelColumnName;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * This is the model implementation of BioConverter. Converts between different
 * kind of plate formats.
 * 
 * @author <a href="bakosg@tcd.ie">Gabor Bakos</a>
 */
public class BioConverterNodeModel extends TransformingNodeModel {
	/**
	 * Possible conversion targets.
	 */
	public static enum ConversionDefault implements Displayable {
		/** custom, user defined */
		custom("custom"),
		/** the input data format of cellHTS2 node */
		cellHTS2Input("cellHTS2 input"),
		/** the output data format of cellHTS2 node */
		cellHTS2Output("cellHTS2 output"),
		/** HC/DC data format */
		hcdc("HC/DC");

		private final String displayText;

		private ConversionDefault(final String displayText) {
			this.displayText = displayText;
		}

		@Override
		public String getDisplayText() {
			return displayText;
		}
	}

	/** Configuration key for input general group. */
	static final String CFGKEY_GENERAL_IN_GROUP = "general.in.group";
	/** Default value for input general group. */
	static final String DEFAULT_GENERAL_IN_GROUP = ConversionDefault.cellHTS2Input
			.getDisplayText();

	/** Configuration key for output general group. */
	static final String CFGKEY_GENERAL_OUT_GROUP = "general.out.group";
	/** Default value for output general group. */
	static final String DEFAULT_GENERAL_OUT_GROUP = ConversionDefault.hcdc
			.getDisplayText();

	/** Configuration key for add unmatched columns. */
	static final String CFGKEY_ADD_UNMATCHED = "add.unmatched";
	/** Default value for add unmatched columns. */
	static final boolean DEFAULT_ADD_UNMATCHED = true;

	/** Configuration key for keep original columns. */
	static final String CFGKEY_KEEP_ORIGINAL = "keep.original";
	/** Default value for keep original columns. */
	static final boolean DEFAULT_KEEP_ORIGINAL = false;

	/** Configuration key for generate missing values. */
	static final String CFGKEY_GENERATE_MISSING = "generate.missing";
	/** Default value for generate missing values. */
	static final boolean DEFAULT_GENERATE_MISSING = true;

	/** Configuration key for input plate group. */
	static final String CFGKEY_PLATE_IN_GROUP = "plate.in.group";
	/** Default value for input plate group. */
	static final String DEFAULT_PLATE_IN_GROUP = ConversionDefault.cellHTS2Input
			.getDisplayText();

	/** Configuration key for output plate group. */
	static final String CFGKEY_PLATE_OUT_GROUP = "plate.out.group";
	/** Default value for output plate group. */
	static final String DEFAULT_PLATE_OUT_GROUP = ConversionDefault.hcdc
			.getDisplayText();

	/** Configuration key for input plate name. */
	static final String CFGKEY_PLATE_IN = "plateIn.name";
	/** Default value for input plate name. */
	static final String DEFAULT_PLATE_IN = "Plate";
	/** Configuration key for input plate format. */
	static final String CFGKEY_PLATE_IN_FORMAT = "plateIn.fromat";
	/** Default value for input plate format. */
	static final String DEFAULT_PLATE_IN_FORMAT = "%d";
	/** Configuration key for output plate name. */
	static final String CFGKEY_PLATE_OUT = "plateOut.name";
	/** Default value for output plate name. */
	static final String DEFAULT_PLATE_OUT = "Plate";
	/** Configuration key for output plate format. */
	static final String CFGKEY_PLATE_OUT_FORMAT = "plateOut.format";
	/** Default value for output plate format. */
	static final String DEFAULT_PLATE_OUT_FORMAT = "%d";

	/** Configuration key for input replicate group. */
	static final String CFGKEY_REPLICATE_IN_GROUP = "replicate.in.group";
	/** Default value for input replicate group. */
	static final String DEFAULT_REPLICATE_IN_GROUP = ConversionDefault.cellHTS2Input
			.getDisplayText();

	/** Configuration key for output plate group. */
	static final String CFGKEY_REPLICATE_OUT_GROUP = "replicate.out.group";
	/** Default value for output plate group. */
	static final String DEFAULT_REPLICATE_OUT_GROUP = ConversionDefault.hcdc
			.getDisplayText();

	/** Configuration key for input replicate name. */
	static final String CFGKEY_REPLICATE_IN = "replicateIn.name";
	/** Default value for input replicate name. */
	static final String DEFAULT_REPLICATE_IN = "Replicate";
	/** Configuration key for input replicate format. */
	static final String CFGKEY_REPLICATE_IN_FORMAT = "replicateIn.format";
	/** Default value for input replicate format. */
	static final String DEFAULT_REPLICATE_IN_FORMAT = "%d";
	/** Configuration key for output replicate name. */
	static final String CFGKEY_REPLICATE_OUT = "replicateOut.name";
	/** Default value for output replicate format. */
	static final String DEFAULT_REPLICATE_OUT = "Replicate";
	/** Configuration key for output replicate format. */
	static final String CFGKEY_REPLICATE_OUT_FORMAT = "replicateOut.format";
	/** Default value for output replicate format. */
	static final String DEFAULT_REPLICATE_OUT_FORMAT = "%d";

	private final SettingsModelString generalInGroup = new SettingsModelString(
			CFGKEY_GENERAL_IN_GROUP, DEFAULT_GENERAL_IN_GROUP);
	private final SettingsModelString generalOutGroup = new SettingsModelString(
			CFGKEY_GENERAL_OUT_GROUP, DEFAULT_GENERAL_OUT_GROUP);

	/**
	 * If {@code true} it will copy the unmatched columns by these rules, else
	 * simply omit them.
	 */
	private final SettingsModelBoolean addUnmatched = new SettingsModelBoolean(
			CFGKEY_ADD_UNMATCHED, DEFAULT_ADD_UNMATCHED);

	/**
	 * If {@code true} it will copy the original (non-conflicting) columns to
	 * the results.
	 */
	private final SettingsModelBoolean keepOriginal = new SettingsModelBoolean(
			CFGKEY_KEEP_ORIGINAL, DEFAULT_KEEP_ORIGINAL);

	/**
	 * If {@code true} it will tries to generate missing data (like
	 * replicate/plate) from data structure, else adds missing values there.
	 */
	private final SettingsModelBoolean generateMissing = new SettingsModelBoolean(
			CFGKEY_GENERATE_MISSING, DEFAULT_GENERATE_MISSING);

	private final SettingsModelString plateInGroup = new SettingsModelString(
			CFGKEY_PLATE_IN_GROUP, DEFAULT_PLATE_IN_GROUP);
	private final SettingsModelString plateOutGroup = new SettingsModelString(
			CFGKEY_PLATE_OUT_GROUP, DEFAULT_PLATE_OUT_GROUP);

	private final SettingsModelColumnName plateInModel = new SettingsModelColumnName(
			CFGKEY_PLATE_IN, DEFAULT_PLATE_IN);
	private final SettingsModelColumnName plateInFormatModel = new SettingsModelColumnName(
			CFGKEY_PLATE_IN_FORMAT, DEFAULT_PLATE_IN_FORMAT);
	private final SettingsModelColumnName plateOutModel = new SettingsModelColumnName(
			CFGKEY_PLATE_OUT, DEFAULT_PLATE_OUT);
	private final SettingsModelColumnName plateOutFormatModel = new SettingsModelColumnName(
			CFGKEY_PLATE_OUT_FORMAT, DEFAULT_PLATE_OUT_FORMAT);

	private final SettingsModelString replicateInGroup = new SettingsModelString(
			CFGKEY_REPLICATE_IN_GROUP, DEFAULT_REPLICATE_IN_GROUP);
	private final SettingsModelString replicateOutGroup = new SettingsModelString(
			CFGKEY_REPLICATE_OUT_GROUP, DEFAULT_REPLICATE_OUT_GROUP);

	private final SettingsModelColumnName replicateInModel = new SettingsModelColumnName(
			CFGKEY_REPLICATE_IN, DEFAULT_REPLICATE_IN);
	private final SettingsModelColumnName replicateInFormatModel = new SettingsModelColumnName(
			CFGKEY_REPLICATE_IN_FORMAT, DEFAULT_REPLICATE_IN_FORMAT);
	private final SettingsModelColumnName replicateOutModel = new SettingsModelColumnName(
			CFGKEY_REPLICATE_OUT, DEFAULT_REPLICATE_OUT);
	private final SettingsModelColumnName replicateOutFormatModel = new SettingsModelColumnName(
			CFGKEY_REPLICATE_OUT_FORMAT, DEFAULT_REPLICATE_OUT_FORMAT);

	private final SettingsModelString[] inNames = new SettingsModelString[] {
			plateInModel, replicateInModel };

	/**
	 * Constructor for the node model.
	 */
	protected BioConverterNodeModel() {
		super(1, 1);
	}

	@Override
	protected BufferedDataTable[] executeDerived(
			final BufferedDataTable[] inData, final ExecutionContext exec)
			throws Exception {
		final ColumnRearranger rearranger = new ColumnRearranger(inData[0]
				.getDataTableSpec());
		// rearranger.
		if (addUnmatched.getBooleanValue()) {
			final int[] allButMatched = allButMatched(
					computeMatchedColumnIndices(rearranger.createSpec()),
					inData[0].getDataTableSpec().getNumColumns());
			rearranger.remove(allButMatched);
		}
		final BufferedDataTable table = exec.createColumnRearrangeTable(
				inData[0], rearranger, exec);
		return new BufferedDataTable[] { table };
	}

	/**
	 * @param columnIndices
	 * @param numColumns
	 * @return
	 */
	private int[] allButMatched(final int[] columnIndices, final int numColumns) {
		final Set<Integer> indicesToFilter = new HashSet<Integer>();
		for (final int colIndex : columnIndices) {
			indicesToFilter.add(Integer.valueOf(colIndex));
		}
		indicesToFilter.remove(Integer.valueOf(-1));
		final int[] ret = new int[numColumns - indicesToFilter.size()];
		int u = 0;
		for (int i = 0; i < ret.length; ++i) {
			while (indicesToFilter.contains(Integer.valueOf(u))) {
				++u;
			}
			ret[i] = u++;
		}
		return ret;
	}

	/**
	 * @param dataTableSpec
	 * @return Might contain duplicates, these indices are present in {@code
	 *         dataTableSpec} (or not, because it might contain {@code -1}s).
	 */
	private int[] computeMatchedColumnIndices(final DataTableSpec dataTableSpec) {
		final int[] ret = new int[inNames.length];
		for (int i = ret.length; i-- > 0;) {
			dataTableSpec.findColumnIndex(inNames[i].getStringValue());
		}
		return ret;
	}

	@Override
	protected void reset() {
		// Do nothing
	}

	@Override
	protected void saveSettingsTo(final NodeSettingsWO settings) {
		super.saveSettingsTo(settings);
		generalInGroup.saveSettingsTo(settings);
		generalOutGroup.saveSettingsTo(settings);
		addUnmatched.saveSettingsTo(settings);
		keepOriginal.saveSettingsTo(settings);
		generateMissing.saveSettingsTo(settings);
		// Plates
		plateInGroup.saveSettingsTo(settings);
		plateInModel.saveSettingsTo(settings);
		plateInFormatModel.saveSettingsTo(settings);
		plateOutGroup.saveSettingsTo(settings);
		plateOutModel.saveSettingsTo(settings);
		plateOutFormatModel.saveSettingsTo(settings);
		// Replicates
		replicateInGroup.saveSettingsTo(settings);
		replicateInModel.saveSettingsTo(settings);
		replicateInFormatModel.saveSettingsTo(settings);
		replicateOutGroup.saveSettingsTo(settings);
		replicateOutModel.saveSettingsTo(settings);
		replicateOutFormatModel.saveSettingsTo(settings);
	}

	@Override
	protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
			throws InvalidSettingsException {
		super.loadValidatedSettingsFrom(settings);
		generalInGroup.loadSettingsFrom(settings);
		generalOutGroup.loadSettingsFrom(settings);
		addUnmatched.loadSettingsFrom(settings);
		keepOriginal.loadSettingsFrom(settings);
		generateMissing.loadSettingsFrom(settings);
		// Plates
		plateInGroup.loadSettingsFrom(settings);
		plateInModel.loadSettingsFrom(settings);
		plateInFormatModel.loadSettingsFrom(settings);
		plateOutGroup.loadSettingsFrom(settings);
		plateOutModel.loadSettingsFrom(settings);
		plateOutFormatModel.loadSettingsFrom(settings);
		// Replicates
		replicateInGroup.loadSettingsFrom(settings);
		replicateInModel.loadSettingsFrom(settings);
		replicateInFormatModel.loadSettingsFrom(settings);
		replicateOutGroup.loadSettingsFrom(settings);
		replicateOutModel.loadSettingsFrom(settings);
		replicateOutFormatModel.loadSettingsFrom(settings);
	}

	@Override
	protected void validateSettings(final NodeSettingsRO settings)
			throws InvalidSettingsException {
		super.validateSettings(settings);
		generalInGroup.validateSettings(settings);
		generalOutGroup.validateSettings(settings);
		addUnmatched.validateSettings(settings);
		keepOriginal.validateSettings(settings);
		generateMissing.validateSettings(settings);
		// Plates
		plateInGroup.validateSettings(settings);
		plateInModel.validateSettings(settings);
		plateInFormatModel.validateSettings(settings);
		plateOutGroup.validateSettings(settings);
		plateOutModel.validateSettings(settings);
		plateOutFormatModel.validateSettings(settings);
		// Replicates
		replicateInGroup.validateSettings(settings);
		replicateInModel.validateSettings(settings);
		replicateInFormatModel.validateSettings(settings);
		replicateOutGroup.validateSettings(settings);
		replicateOutModel.validateSettings(settings);
		replicateOutFormatModel.validateSettings(settings);
	}
}

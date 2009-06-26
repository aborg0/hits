package ie.tcd.imm.hits.knime.interop;

import ie.tcd.imm.hits.knime.interop.BioConverterNodeDialog.ColumnType;
import ie.tcd.imm.hits.knime.util.TransformingNodeModel;
import ie.tcd.imm.hits.util.Displayable;
import ie.tcd.imm.hits.util.Pair;
import ie.tcd.imm.hits.util.template.Token;
import ie.tcd.imm.hits.util.template.TokenizeException;
import ie.tcd.imm.hits.util.template.TokenizerFactory;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.IntValue;
import org.knime.core.data.StringValue;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
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
	/** A {@link NodeLogger}. */
	protected static final NodeLogger logger = NodeLogger
			.getLogger(BioConverterNodeModel.class);

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
	static final String DEFAULT_PLATE_OUT_FORMAT = "${Plate}";
	/** Configuration key for output plate column type. */
	static final String CFGKEY_PLATE_OUT_TYPE = "plateOut.type";
	/** Default value for output plate column type. */
	static final String DEFAULT_PLATE_OUT_TYPE = "Integer";

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
	static final String DEFAULT_REPLICATE_OUT_FORMAT = "${Replicate}";
	/** Configuration key for output replicate column type. */
	static final String CFGKEY_REPLICATE_OUT_TYPE = "replicateOut.type";
	/** Default value for output replicate column type. */
	static final String DEFAULT_REPLICATE_OUT_TYPE = "Integer";

	private static final Pattern groupStart = Pattern.compile("\\$\\{");
	private static final Pattern groupEnd = Pattern.compile("\\}");

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
	private final SettingsModelString plateInFormatModel = new SettingsModelString(
			CFGKEY_PLATE_IN_FORMAT, DEFAULT_PLATE_IN_FORMAT);
	private final SettingsModelString plateOutModel = new SettingsModelString(
			CFGKEY_PLATE_OUT, DEFAULT_PLATE_OUT);
	private final SettingsModelString plateOutFormatModel = new SettingsModelString(
			CFGKEY_PLATE_OUT_FORMAT, DEFAULT_PLATE_OUT_FORMAT);
	private final SettingsModelString plateOutTypeModel = new SettingsModelString(
			CFGKEY_PLATE_OUT_TYPE, DEFAULT_PLATE_OUT_TYPE);

	private final SettingsModelString replicateInGroup = new SettingsModelString(
			CFGKEY_REPLICATE_IN_GROUP, DEFAULT_REPLICATE_IN_GROUP);
	private final SettingsModelString replicateOutGroup = new SettingsModelString(
			CFGKEY_REPLICATE_OUT_GROUP, DEFAULT_REPLICATE_OUT_GROUP);

	private final SettingsModelColumnName replicateInModel = new SettingsModelColumnName(
			CFGKEY_REPLICATE_IN, DEFAULT_REPLICATE_IN);
	private final SettingsModelString replicateInFormatModel = new SettingsModelString(
			CFGKEY_REPLICATE_IN_FORMAT, DEFAULT_REPLICATE_IN_FORMAT);
	private final SettingsModelString replicateOutModel = new SettingsModelString(
			CFGKEY_REPLICATE_OUT, DEFAULT_REPLICATE_OUT);
	private final SettingsModelString replicateOutFormatModel = new SettingsModelString(
			CFGKEY_REPLICATE_OUT_FORMAT, DEFAULT_REPLICATE_OUT_FORMAT);
	private final SettingsModelString replicateOutTypeModel = new SettingsModelString(
			CFGKEY_REPLICATE_OUT_TYPE, DEFAULT_REPLICATE_OUT_TYPE);

	private final SettingsModelString[] inNames = new SettingsModelString[] {
			plateInModel, replicateInModel };

	/**
	 * Constructor for the node model.
	 */
	protected BioConverterNodeModel() {
		super(1, 1);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.knime.core.node.NodeModel#configure(org.knime.core.data.DataTableSpec
	 * [])
	 */
	@Override
	protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
			throws InvalidSettingsException {
		try {
			return new DataTableSpec[] { createRearranger(inSpecs[0])
					.createSpec() };
		} catch (final TokenizeException e) {
			logger.warn("Problem: " + e.getMessage(), e);
			throw new InvalidSettingsException(e);
		}
	}

	@Override
	protected BufferedDataTable[] executeDerived(
			final BufferedDataTable[] inData, final ExecutionContext exec)
			throws Exception {
		final ColumnRearranger rearranger = createRearranger(inData[0]
				.getDataTableSpec());
		// rearranger.
		final BufferedDataTable table = exec.createColumnRearrangeTable(
				inData[0], rearranger, exec);
		return new BufferedDataTable[] { table };
	}

	/**
	 * @param dataTableSpec
	 * @return
	 * @throws TokenizeException
	 */
	private ColumnRearranger createRearranger(final DataTableSpec dataTableSpec)
			throws TokenizeException {
		final ColumnRearranger ret = new ColumnRearranger(dataTableSpec);
		if (!addUnmatched.getBooleanValue()) {
			final int[] allButMatched = allButMatched(
					computeMatchedColumnIndices(ret.createSpec()),
					dataTableSpec.getNumColumns());
			ret.remove(allButMatched);
		}
		if (!keepOriginal.getBooleanValue())// Remove later not matched columns
		{

		}
		if (generateMissing.getBooleanValue())// Try to generate missing column
		// values
		{

		}
		final String plateOutName = plateOutModel.getStringValue();
		final String replicateOutName = replicateOutModel.getStringValue();
		final Map<String, List<ColumnType>> newColumns = new LinkedHashMap<String, List<ColumnType>>();
		putOrAdd(newColumns, plateOutName, ColumnType.Plate);
		putOrAdd(newColumns, replicateOutName, ColumnType.Replicate);
		final Map<ColumnType, Pattern> patterns = new EnumMap<ColumnType, Pattern>(
				ColumnType.class);
		patterns.put(ColumnType.Plate, Pattern.compile(plateInFormatModel
				.getStringValue()));
		patterns.put(ColumnType.Replicate, Pattern
				.compile(replicateInFormatModel.getStringValue()));
		final Map<ColumnType, List<Token>> outFormats = new EnumMap<ColumnType, List<Token>>(
				ColumnType.class);
		outFormats.put(ColumnType.Plate, new TokenizerFactory()
				.createGroupingTokenizer(
						new Pair<Token, List<? extends Token>>(null,
								new ArrayList<Token>()), groupStart, groupEnd,
						0).parse(plateOutFormatModel.getStringValue()));
		outFormats.put(ColumnType.Replicate, new TokenizerFactory()
				.createGroupingTokenizer(
						new Pair<Token, List<? extends Token>>(null,
								new ArrayList<Token>()), groupStart, groupEnd,
						0).parse(replicateOutFormatModel.getStringValue()));
		final EnumMap<ColumnType, Pattern> inPatterns = new EnumMap<ColumnType, Pattern>(
				ColumnType.class);
		inPatterns.put(ColumnType.Plate, Pattern.compile(plateInFormatModel
				.getStringValue()));
		inPatterns.put(ColumnType.Replicate, Pattern
				.compile(replicateInFormatModel.getStringValue()));
		final EnumMap<ColumnType, DataType> outTypes = new EnumMap<ColumnType, DataType>(
				ColumnType.class);
		outTypes.put(ColumnType.Plate, findType(plateOutTypeModel
				.getStringValue()));
		outTypes.put(ColumnType.Replicate, findType(replicateOutTypeModel
				.getStringValue()));
		final NavigableMap<String, ColumnType> dictionary = new TreeMap<String, ColumnType>(
				String.CASE_INSENSITIVE_ORDER);
		for (final ColumnType ct : ColumnType.values()) {
			dictionary.put(ct.getDisplayText(), ct);
		}
		for (final Entry<String, List<ColumnType>> entry : newColumns
				.entrySet()) {
			final String newColumn = entry.getKey();
			final ColumnType columnType = entry.getValue().iterator().next();// assert
			// sameall
			final SingleCellFactory factory = new SingleCellFactory(
					new DataColumnSpecCreator(newColumn, outTypes
							.get(columnType)).createSpec()) {
				Map<ColumnType, Integer> origColumnIndices = new EnumMap<ColumnType, Integer>(
						ColumnType.class);
				{
					origColumnIndices.put(ColumnType.Plate, Integer
							.valueOf(dataTableSpec.findColumnIndex(plateInModel
									.getColumnName())));
					origColumnIndices.put(ColumnType.Replicate, Integer
							.valueOf(dataTableSpec
									.findColumnIndex(replicateInModel
											.getColumnName())));

					if (outFormats.get(entry.getValue().iterator().next())
							.size() != 1
							&& outTypes.get(entry.getValue().iterator().next()) != StringCell.TYPE) {
						throw new IllegalStateException("Wrong format for "
								+ entry.getValue().iterator().next());
					}
				}

				// Pattern selectPattern = Pattern.compile(plateInFormatModel
				// .getStringValue());

				int count = 0;

				@Override
				public DataCell getCell(final DataRow row) {
					final Map<ColumnType, DataCell> cells = new EnumMap<ColumnType, DataCell>(
							ColumnType.class);
					for (final ColumnType ct : ColumnType.values()) {
						cells.put(ct, row.getCell(origColumnIndices.get(ct)));
					}
					// final DataCell cell = row.getCell(origColumnIndex);
					final DataCell result;
					final DataType type = outTypes.get(columnType);
					final Token firstToken = outFormats.get(columnType)
							.iterator().next();
					if (type == IntCell.TYPE) {
						final ColumnType origType = dictionary.get(firstToken
								.getText());
						final DataCell cell = row.getCell(origColumnIndices
								.get(origType).intValue());
						if (cell.isMissing()) {
							if (generateMissing.getBooleanValue()) {
								// FIXME
								result = new IntCell(count++);
							} else {
								result = DataType.getMissingCell();
							}
						} else if (cell instanceof IntValue) {
							final IntValue v = (IntValue) cell;
							result = (DataCell) v;
						} else if (cell instanceof DoubleValue) {
							final DoubleValue v = (DoubleValue) cell;
							result = new IntCell((int) Math.round(v
									.getDoubleValue()));
						} else if (cell instanceof StringValue) {
							final StringValue str = (StringValue) cell;
							result = new IntCell(Integer.parseInt(inPatterns
									.get(origType)
									.matcher(str.getStringValue()).group(1)));
						} else {
							throw new IllegalStateException(
									"Unsupported cell type: " + cell.getClass());
						}
					} else if (type == DoubleCell.TYPE) {
						final ColumnType origType = dictionary.get(firstToken
								.getText());
						final DataCell cell = row.getCell(origColumnIndices
								.get(origType).intValue());
						if (cell.isMissing()) {
							if (generateMissing.getBooleanValue()) {
								// FIXME
								result = new DoubleCell(count++);
							} else {
								result = DataType.getMissingCell();
							}
						} else if (cell instanceof DoubleValue) {
							final DoubleValue v = (DoubleValue) cell;
							result = (DataCell) v;
						} else if (cell instanceof StringValue) {
							final StringValue str = (StringValue) cell;
							result = new DoubleCell(Double
									.parseDouble(inPatterns.get(origType)
											.matcher(str.getStringValue())
											.group(1)));
						} else {
							throw new IllegalStateException(
									"Unsupported cell type: " + cell.getClass());
						}

					} else if (type == StringCell.TYPE) {
						final StringBuilder sb = new StringBuilder();
						for (final Token token : outFormats.get(columnType)) {
							if (dictionary.containsKey(token.getText())) {
								sb.append(cells.get(
										dictionary.get(token.getText()))
										.toString());
							} else {
								sb.append(token.getText());
							}
						}
						result = new StringCell(sb.toString());
					} else {
						throw new IllegalStateException("Wrong type: " + type);
					}

					return result;
				}
			};
			if (ret.createSpec().containsName(newColumn)) {
				ret.replace(factory, newColumn);
			} else {
				ret.append(factory);
			}
		}
		return ret;
	}

	/**
	 * @param typeName
	 * @return
	 */
	private DataType findType(final String typeName) {
		if ("Integer".equalsIgnoreCase(typeName)) {
			return IntCell.TYPE;
		}
		if ("Real".equalsIgnoreCase(typeName)) {
			return DoubleCell.TYPE;
		}
		if ("String".equalsIgnoreCase(typeName)) {
			return StringCell.TYPE;
		}
		throw new IllegalArgumentException("Unsupported type: " + typeName);
	}

	/**
	 * @param <K>
	 * @param <V>
	 * @param map
	 * @param key
	 * @param value
	 */
	private static <K, V> void putOrAdd(final Map<K, List<V>> map, final K key,
			final V value) {
		if (!map.containsKey(key)) {
			map.put(key, new ArrayList<V>());
		}
		map.get(key).add(value);
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
		plateOutTypeModel.saveSettingsTo(settings);
		// Replicates
		replicateInGroup.saveSettingsTo(settings);
		replicateInModel.saveSettingsTo(settings);
		replicateInFormatModel.saveSettingsTo(settings);
		replicateOutGroup.saveSettingsTo(settings);
		replicateOutModel.saveSettingsTo(settings);
		replicateOutFormatModel.saveSettingsTo(settings);
		replicateOutTypeModel.saveSettingsTo(settings);
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
		plateOutTypeModel.loadSettingsFrom(settings);
		// Replicates
		replicateInGroup.loadSettingsFrom(settings);
		replicateInModel.loadSettingsFrom(settings);
		replicateInFormatModel.loadSettingsFrom(settings);
		replicateOutGroup.loadSettingsFrom(settings);
		replicateOutModel.loadSettingsFrom(settings);
		replicateOutFormatModel.loadSettingsFrom(settings);
		replicateOutTypeModel.loadSettingsFrom(settings);
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
		plateOutTypeModel.validateSettings(settings);
		// Replicates
		replicateInGroup.validateSettings(settings);
		replicateInModel.validateSettings(settings);
		replicateInFormatModel.validateSettings(settings);
		replicateOutGroup.validateSettings(settings);
		replicateOutModel.validateSettings(settings);
		replicateOutFormatModel.validateSettings(settings);
		replicateOutTypeModel.validateSettings(settings);
	}
}

package ie.tcd.imm.hits.knime.interop;

import ie.tcd.imm.hits.knime.interop.BioConverterNodeDialog.ColumnType;
import ie.tcd.imm.hits.knime.interop.BioConverterNodeDialog.DialogType;
import ie.tcd.imm.hits.knime.interop.config.Default;
import ie.tcd.imm.hits.knime.interop.config.Root;
import ie.tcd.imm.hits.knime.util.TransformingNodeModel;
import ie.tcd.imm.hits.util.Misc;
import ie.tcd.imm.hits.util.Pair;
import ie.tcd.imm.hits.util.template.CompoundToken;
import ie.tcd.imm.hits.util.template.SimpleToken;
import ie.tcd.imm.hits.util.template.Token;
import ie.tcd.imm.hits.util.template.TokenizeException;
import ie.tcd.imm.hits.util.template.Tokenizer;
import ie.tcd.imm.hits.util.template.TokenizerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.annotation.Nullable;
import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnDomainCreator;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
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
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelColumnName;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.xml.sax.SAXException;

/**
 * This is the model implementation of BioConverter. Converts between different
 * kind of plate formats.
 * 
 * @author <a href="bakosg@tcd.ie">Gabor Bakos</a>
 */
public class BioConverterNodeModel extends TransformingNodeModel {
	/**  */
	private static final String CONFIGURATION_XSD = "interop.xsd";

	/** The default path (relative to this class) for the configurations. */
	public static final String CONFIGURATION_XML = "interop.xml";

	/** A {@link NodeLogger}. */
	protected static final NodeLogger logger = NodeLogger
			.getLogger(BioConverterNodeModel.class);

	/** Configuration key for input general group. */
	static final String CFGKEY_GENERAL_IN_GROUP = "general.in.group";
	/** Default value for input general group. */
	/** Configuration key for output general group. */
	static final String CFGKEY_GENERAL_OUT_GROUP = "general.out.group";
	/** Default value for output general group. */
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
	static final boolean DEFAULT_GENERATE_MISSING = false;

	private static final Pattern groupStart = Pattern.compile("\\$\\{");
	private static final Pattern groupEnd = Pattern.compile("\\}");
	private static final List<Pair<Pair<ColumnType, Boolean>, DialogType>> possibleKeys;
	static {
		final List<Pair<Pair<ColumnType, Boolean>, DialogType>> tmp = new ArrayList<Pair<Pair<ColumnType, Boolean>, DialogType>>(
				ColumnType.values().length
						* (2 * DialogType.values().length - 3));
		for (final ColumnType colType : ColumnType.values()) {
			for (final DialogType dt : DialogType.values()) {
				if (dt != DialogType.type) {
					tmp
							.add(new Pair<Pair<ColumnType, Boolean>, DialogType>(
									new Pair<ColumnType, Boolean>(colType,
											Boolean.TRUE), dt));
				}
				tmp.add(new Pair<Pair<ColumnType, Boolean>, DialogType>(
						new Pair<ColumnType, Boolean>(colType, Boolean.FALSE),
						dt));
			}
		}
		possibleKeys = Collections.unmodifiableList(tmp);
	}

	private final SettingsModelString generalInGroup;
	private final SettingsModelString generalOutGroup;

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

	/** The {@link SettingsModelString}s used in the model. */
	private final Map<ColumnType, Map<Boolean, Map<DialogType, SettingsModelString>>> settingsModels;
	/** The {@link SettingsModelInteger}s used in the model. */
	private final Map<ColumnType, SettingsModelInteger> positionModels;

	/**
	 * Constructor for the node model.
	 * 
	 * @param root
	 *            The configuration for defaults/profiles.
	 */
	protected BioConverterNodeModel(final Root root) {
		super(1, 1);
		settingsModels = generateSettingsModels(root);
		positionModels = generatePositionModels(root);
		generalInGroup = new SettingsModelString(CFGKEY_GENERAL_IN_GROUP, root
				.getProfiles().getProfile().get(0).getName());
		generalOutGroup = new SettingsModelString(CFGKEY_GENERAL_OUT_GROUP,
				root.getProfiles().getProfile().get(1).getName());
	}

	/**
	 * @param root
	 *            The configuration of defaults/profiles.
	 * @return The {@link SettingsModelInteger}s of positions.
	 * @see #generateKey(ColumnType, boolean, DialogType)
	 */
	private Map<ColumnType, SettingsModelInteger> generatePositionModels(
			final Root root) {
		final Map<ColumnType, SettingsModelInteger> ret = new EnumMap<ColumnType, SettingsModelInteger>(
				ColumnType.class);
		for (final ColumnType columnType : ColumnType.values()) {
			ret.put(columnType, new SettingsModelIntegerBounded(generateKey(
					columnType, false, DialogType.position), Integer
					.parseInt(findDefault(root, columnType, false,
							DialogType.position)), -20, 20));
		}
		return ret;
	}

	/**
	 * @param root
	 *            The configuration for defaults/profiles.
	 * @return The {@link SettingsModel}s based on the values from {@code root},
	 *         and the {@link #possibleKeys()}. (Does not contain the
	 *         {@link DialogType#position} values.)
	 * @see #generateKey(ColumnType, boolean, DialogType)
	 */
	static Map<ColumnType, Map<Boolean, Map<DialogType, SettingsModelString>>> generateSettingsModels(
			final Root root) {
		final Map<ColumnType, Map<Boolean, Map<DialogType, SettingsModelString>>> ret = new EnumMap<ColumnType, Map<Boolean, Map<DialogType, SettingsModelString>>>(
				ColumnType.class);
		final List<Default> defaults = root.getDefaults().getDefault();
		for (final ColumnType ct : ColumnType.values()) {

			Map<Boolean, Map<DialogType, SettingsModelString>> ioMap;
			ret
					.put(
							ct,
							ioMap = new HashMap<Boolean, Map<DialogType, SettingsModelString>>(
									2, 1.0f));
			for (final boolean input : new boolean[] { true, false }) {
				final Map<DialogType, SettingsModelString> settings;
				ioMap
						.put(
								Boolean.valueOf(input),
								settings = new EnumMap<DialogType, SettingsModelString>(
										DialogType.class));
				for (final DialogType dt : DialogType.values()) {
					if (input
							&& (dt == DialogType.type || dt == DialogType.position)) {
						continue;
					}
					final String key = generateKey(ct, input, dt);
					String def = null;
					for (final Default d : defaults) {
						if (d.getColumn() == ct && d.isInput() == input
								&& d.getDialog() == dt) {
							def = d.getValue();
						}
					}
					if (def == null) {
						throw new IllegalStateException(
								"No defaults defined for " + ct + " "
										+ (input ? "input" : "output") + " "
										+ dt + "\nCorrect in interop.xml");
					}
					settings
							.put(
									dt,
									input && dt == DialogType.name ? new SettingsModelColumnName(
											key, def)
											: new SettingsModelString(key, def));
				}
			}

		}
		return ret;
	}

	@Override
	protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
			throws InvalidSettingsException {
		try {
			return new DataTableSpec[] { createRearranger(inSpecs[0]).getLeft()
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
		final Pair<ColumnRearranger, Map<String, Set<DataCell>>> pair = createRearranger(inData[0]
				.getDataTableSpec());
		final BufferedDataTable table = exec.createColumnRearrangeTable(
				inData[0], pair.getLeft(), exec);
		final Map<String, Set<DataCell>> domains = pair.getRight();
		final DataColumnSpec[] newSpecs = new DataColumnSpec[table
				.getDataTableSpec().getNumColumns()];
		for (int i = table.getDataTableSpec().getNumColumns(); i-- > 0;) {
			final DataColumnSpec oldSpec = table.getDataTableSpec()
					.getColumnSpec(i);
			if (domains.containsKey(oldSpec.getName())) {
				final DataColumnSpecCreator dataColumnSpecCreator = new DataColumnSpecCreator(
						oldSpec);
				dataColumnSpecCreator.setDomain(new DataColumnDomainCreator(
						domains.get(oldSpec.getName())).createDomain());
				newSpecs[i] = dataColumnSpecCreator.createSpec();
			} else {
				newSpecs[i] = oldSpec;
			}
		}
		final DataTableSpec newSpec = new DataTableSpec(newSpecs);
		return new BufferedDataTable[] { exec.createSpecReplacerTable(table,
				newSpec) };
	}

	@SuppressWarnings("unchecked")
	private static final List<Class<? extends Token>> acceptedTokens = Arrays
			.asList(SimpleToken.class, CompoundToken.class);

	/**
	 * Creates a {@link ColumnRearranger} based on the {@code dataTableSpec} and
	 * the {@link #settingsModels}.
	 * 
	 * @param dataTableSpec
	 *            A {@link DataTableSpec}.
	 * @return A new {@link ColumnRearranger}.
	 * @throws TokenizeException
	 *             If there is a problem with one of the output formats.
	 */
	private Pair<ColumnRearranger, Map<String, Set<DataCell>>> createRearranger(
			final DataTableSpec dataTableSpec) throws TokenizeException {
		final ColumnRearranger ret = new ColumnRearranger(dataTableSpec);
		if (!addUnmatched.getBooleanValue()) {
			final int[] allButMatched = allButMatched(
					computeMatchedColumnIndices(ret.createSpec()),
					dataTableSpec.getNumColumns());
			ret.remove(allButMatched);
		}
		if (!keepOriginal.getBooleanValue())// Remove later not matched columns
		{
			for (final ColumnType ct : ColumnType.values()) {
				final String origName = settingsModels.get(ct)
						.get(Boolean.TRUE).get(DialogType.name)
						.getStringValue();
				if (origName != null && ret.indexOf(origName) != -1) {
					ret.remove(origName);
				}
			}
		}
		final EnumMap<ColumnType, DataType> outTypes = new EnumMap<ColumnType, DataType>(
				ColumnType.class);
		for (final ColumnType ct : ColumnType.values()) {
			final DataType outputType = findType(settingsModels.get(ct).get(
					Boolean.FALSE).get(DialogType.type).getStringValue());
			if (outputType != null) {
				outTypes.put(ct, outputType);
			}
		}
		final Map<String, List<ColumnType>> newColumns = new LinkedHashMap<String, List<ColumnType>>();
		for (final ColumnType ct : ColumnType.values()) {
			final String colName = settingsModels.get(ct).get(Boolean.FALSE)
					.get(DialogType.name).getStringValue();
			if (!colName.isEmpty() && outTypes.get(ct) != null) {
				putOrAdd(newColumns, colName, ct);
			}
		}
		final Map<ColumnType, Pattern> patterns = new EnumMap<ColumnType, Pattern>(
				ColumnType.class);
		for (final ColumnType ct : ColumnType.values()) {
			patterns.put(ct, Pattern.compile(settingsModels.get(ct).get(
					Boolean.TRUE).get(DialogType.format).getStringValue()));
		}
		final Map<ColumnType, List<Token>> outFormats = new EnumMap<ColumnType, List<Token>>(
				ColumnType.class);
		for (final ColumnType ct : ColumnType.values()) {
			if (outTypes.get(ct) != null) {
				outFormats.put(ct, new TokenizerFactory()
						.createGroupingTokenizer(createContinueState(),
								acceptedTokens, false, groupStart, groupEnd, 0)
						.parse(
								settingsModels.get(ct).get(Boolean.FALSE).get(
										DialogType.format).getStringValue()));
			}
		}
		final EnumMap<ColumnType, Pattern> inPatterns = new EnumMap<ColumnType, Pattern>(
				ColumnType.class);
		for (final ColumnType ct : ColumnType.values()) {
			inPatterns.put(ct, Pattern.compile(settingsModels.get(ct).get(
					Boolean.TRUE).get(DialogType.format).getStringValue()));
		}
		final NavigableMap<String, ColumnType> dictionary = new TreeMap<String, ColumnType>(
				String.CASE_INSENSITIVE_ORDER);
		for (final ColumnType ct : ColumnType.values()) {
			dictionary.put(ct.getDisplayText(), ct);
		}
		final Map<String, Set<DataCell>> possibleValues = new HashMap<String, Set<DataCell>>();
		for (final Entry<String, List<ColumnType>> entry : newColumns
				.entrySet()) {
			final String newColumn = entry.getKey();
			possibleValues.put(newColumn, new HashSet<DataCell>());
			final ColumnType columnType = entry.getValue().iterator().next();// assert
			// same all
			final SingleCellFactory factory = new SingleCellFactory(
					new DataColumnSpecCreator(newColumn, outTypes
							.get(columnType)).createSpec()) {
				private final Map<ColumnType, Integer> origColumnIndices = new EnumMap<ColumnType, Integer>(
						ColumnType.class);

				private final Set<DataCell> possValues = possibleValues
						.get(newColumn);

				@SuppressWarnings("synthetic-access")
				private final Map<ColumnType, Map<Boolean, Map<DialogType, SettingsModelString>>> settings = settingsModels;
				{
					for (final ColumnType ct : ColumnType.values()) {
						origColumnIndices.put(ct, Integer.valueOf(dataTableSpec
								.findColumnIndex(settings.get(ct).get(
										Boolean.TRUE).get(DialogType.name)
										.getStringValue())));
					}

					if (outFormats.get(entry.getValue().iterator().next())
							.size() != 1
							&& outTypes.get(entry.getValue().iterator().next()) != StringCell.TYPE) {
						throw new IllegalStateException("Wrong format for "
								+ entry.getValue().iterator().next());
					}
				}

				private int plateCount = 0;
				int replicateCount = 0;
				private int columnCount = 1;
				private int rowCount = 1;

				private final Map<ColumnType, String> lastValues = new EnumMap<ColumnType, String>(
						ColumnType.class);

				@SuppressWarnings("synthetic-access")
				private final boolean genMissing = generateMissing
						.getBooleanValue();

				@Override
				public DataCell getCell(final DataRow row) {
					final Map<ColumnType, String> inputs = new EnumMap<ColumnType, String>(
							ColumnType.class);
					for (final ColumnType ct : ColumnType.values()) {
						final Pattern pattern = inPatterns.get(ct);
						final int index = origColumnIndices.get(ct).intValue();
						if (index != -1) {
							final DataCell cell = row.getCell(index);
							final Matcher matcher = pattern.matcher(cell
									.toString());
							inputs.put(ct, matcher.matches()
									&& matcher.groupCount() >= 1 ? matcher
									.group(1) : "");
						} else {
							inputs.put(ct, "");
						}
					}
					if (!inputs.equals(lastValues)) {
						final String c = inputs.get(ColumnType.WellColumn)
								.trim();
						final String r = inputs.get(ColumnType.WellRow).trim();
						int col = 0, rr = 0;
						try {
							col = Integer.parseInt(c);
							rr = Integer.parseInt(r);
						} catch (final NumberFormatException e) {
							// No problem, we do not care if cannot generate
							// default.
						}
						if (r.length() == 1 && rr == 0) {
							rr = Character.toLowerCase(r.charAt(0)) - 'a' + 1;
						}
						final String p = inputs.get(ColumnType.Plate);
						if (rr == 1 && col == 1) {
							if (p.isEmpty()) {
								++plateCount;
							} else {
								if (p.equals(lastValues.get(ColumnType.Plate))) {
									++replicateCount;
								} else {
									replicateCount = 1;
								}
							}
						}
						lastValues.putAll(inputs);
						// final String rep = inputs.get(ColumnType.Replicate);
					}
					final DataCell result;
					final DataType type = outTypes.get(columnType);
					final List<Token> tokenList = outFormats.get(columnType);
					final String formatText = tokenList.size() == 1 ? tokenList
							.iterator().next().getText() : "";
					final String value = formatText.isEmpty() ? inputs
							.get(columnType) : evaluate(formatText, inputs);
					if (type == IntCell.TYPE || type == DoubleCell.TYPE) {
						if (value.isEmpty()) {
							if (genMissing) {
								switch (columnType) {
								case Plate:
									result = type == IntCell.TYPE ? new IntCell(
											plateCount)
											: new DoubleCell(plateCount);
									break;
								case Replicate:
									result = type == IntCell.TYPE ? new IntCell(
											replicateCount)
											: new DoubleCell(replicateCount);
									break;
								case WellColumn:
									columnCount = columnCount <= 12 ? columnCount + 1
											: 1;
									result = type == IntCell.TYPE ? new IntCell(
											columnCount)
											: new DoubleCell(columnCount);
									break;
								case WellRow:
									rowCount = rowCount <= 8 ? rowCount + 1 : 1;
									result = type == IntCell.TYPE ? new IntCell(
											rowCount)
											: new DoubleCell(rowCount);
									break;
								case Experiment:
									result = type == IntCell.TYPE ? new IntCell(
											0)
											: new DoubleCell(0.0);
									break;
								default:
									throw new UnsupportedOperationException(
											"Not supported column: "
													+ columnType);
								}
							} else {
								result = DataType.getMissingCell();
							}
						} else {
							result = type == IntCell.TYPE ? new IntCell(Integer
									.parseInt(value)) : new DoubleCell(Double
									.parseDouble(value));
						}
					} else if (type == StringCell.TYPE) {
						final StringBuilder sb = new StringBuilder();
						for (final Token token : tokenList) {
							if (token instanceof CompoundToken<?>) {
								sb.append(evaluate(token.getText(), inputs));
							} else {
								sb.append(token.getText());
							}
						}
						result = new StringCell(sb.toString());
					} else {
						throw new IllegalStateException("Wrong type: " + type);
					}
					// lastValues.put(columnType, result.toString());
					if (type.equals(StringCell.TYPE)) {
						possValues.add(result);
					}
					return result;
				}

				private final Pattern leadingDigits = Pattern
						.compile("0(\\d)(.*)");
				private final Pattern functionApplication = Pattern
						.compile("^([\\w.]+)\\((.*)\\)$");
				private final Pattern integer = Pattern.compile("(\\d+)");
				private final Pattern string = Pattern.compile("\\\"(.*?)\\\"");

				private String evaluate(final String text,
						final Map<ColumnType, String> inputs) {
					if (dictionary.containsKey(text.trim())) {
						return inputs.get(dictionary.get(text));
					}
					final Matcher stringMatcher = string.matcher(text.trim());
					if (stringMatcher.matches()) {
						return stringMatcher.group(1);
					}
					final Matcher integerMatcher = integer.matcher(text.trim());
					if (integerMatcher.matches()) {
						return integerMatcher.group(1);
					}
					final Matcher leadingMatcher = leadingDigits.matcher(text
							.trim());
					if (leadingMatcher.matches()) {
						return ie.tcd.imm.hits.util.Misc.addTrailing(Integer
								.parseInt(evaluate(leadingMatcher.group(2)
										.trim(), inputs)), Integer
								.parseInt(leadingMatcher.group(1)));
					}
					final Matcher funcMatcher = functionApplication
							.matcher(text.trim());
					if (funcMatcher.matches()) {
						Class<?> cls = Misc.class;
						if (funcMatcher.group(1).contains(".")) {
							try {
								cls = Class.forName(funcMatcher.group(1)
										.substring(
												0,
												funcMatcher.group(1)
														.lastIndexOf('.')));
							} catch (final ClassNotFoundException e) {
								return text.trim();
							}
						}
						try {
							final Method m = cls.getMethod(funcMatcher.group(1)
									.substring(
											funcMatcher.group(1).lastIndexOf(
													'.') + 1), String.class);
							final Object o = m.invoke(null, evaluate(
									funcMatcher.group(2), inputs));
							return o == null ? "" : o.toString();
						} catch (final SecurityException e) {
							return text.trim();
						} catch (final NoSuchMethodException e) {
							return text.trim();
						} catch (final IllegalArgumentException e) {
							return text.trim();
						} catch (final IllegalAccessException e) {
							return text.trim();
						} catch (final InvocationTargetException e) {
							return text.trim();
						}
					}
					return text.trim();
				}
			};

			if (ret.createSpec().containsName(newColumn)) {
				ret.replace(factory, newColumn);
			} else {
				ret.append(factory);
			}
		}
		final Map<String, Integer> newPosition = new HashMap<String, Integer>();
		for (final Entry<String, List<ColumnType>> entry : newColumns
				.entrySet()) {
			final ColumnType columnType = entry.getValue().iterator().next();
			newPosition.put(entry.getKey(), Integer.valueOf(positionModels.get(
					columnType).getIntValue()));
		}
		final NavigableMap<Integer, String> reverseMap = new TreeMap<Integer, String>();
		for (final Entry<String, Integer> entry : newPosition.entrySet()) {
			reverseMap.put(entry.getValue(), entry.getKey());
		}
		for (final Entry<Integer, String> entry : reverseMap.tailMap(
				Integer.valueOf(0), false).entrySet()) {
			ret.move(entry.getValue(), entry.getKey().intValue() - 1);
		}
		for (final Entry<Integer, String> entry : reverseMap.headMap(
				Integer.valueOf(0), false).descendingMap().entrySet()) {
			ret.move(entry.getValue(), ret.createSpec().getNumColumns()
					+ entry.getKey().intValue());
		}
		return new Pair<ColumnRearranger, Map<String, Set<DataCell>>>(ret,
				possibleValues);
	}

	/**
	 * @return The initial state of the {@link Tokenizer}.
	 */
	@edu.umd.cs.findbugs.annotations.SuppressWarnings("NP")
	private Pair<Token, List<? extends Token>> createContinueState() {
		return new Pair<Token, List<? extends Token>>(null,
				new ArrayList<Token>());
	}

	/**
	 * @param typeName
	 *            A {@link String} with one of these values:
	 *            <ul>
	 *            <li>{@code Integer}</li>
	 *            <li>{@code Real}</li>
	 *            <li>{@code String}</li>
	 *            </ul>
	 * @return The associated DataType.
	 */
	@Nullable
	private static DataType findType(final String typeName) {
		if (BioConverterNodeDialog.INTEGER.equalsIgnoreCase(typeName)) {
			return IntCell.TYPE;
		}
		if (BioConverterNodeDialog.REAL.equalsIgnoreCase(typeName)) {
			return DoubleCell.TYPE;
		}
		if (BioConverterNodeDialog.STRING.equalsIgnoreCase(typeName)) {
			return StringCell.TYPE;
		}
		if (BioConverterNodeDialog.DO_NOT_GENERATE.equalsIgnoreCase(typeName)) {
			return null;
		}
		throw new IllegalArgumentException("Unsupported type: " + typeName);
	}

	/**
	 * Generates a configuration key based on the parameters.
	 * 
	 * @param colType
	 *            The {@link ColumnType}.
	 * @param input
	 *            The input ({@code true}), or output ({@code false}) kind.
	 * @param dialogType
	 *            The {@link DialogType}.
	 * @return A unique {@link String} for the combination of parameters.
	 */
	static final String generateKey(final ColumnType colType,
			final boolean input, final DialogType dialogType) {
		assert !input || dialogType != DialogType.type
				&& dialogType != DialogType.position;
		return colType.name() + "." + (input ? "in" : "out") + "."
				+ dialogType.name();
	}

	/**
	 * @return The possible keys to iterate over.
	 */
	static List<Pair<Pair<ColumnType, Boolean>, DialogType>> possibleKeys() {
		return possibleKeys;
	}

	/**
	 * Puts, or adds {@code value} to the {@code map} multimap.
	 * 
	 * @param <K>
	 *            Type of keys.
	 * @param <V>
	 *            Type of values.
	 * @param map
	 *            A multimap.
	 * @param key
	 *            The key.
	 * @param value
	 *            The value.
	 */
	private static <K, V> void putOrAdd(final Map<K, List<V>> map, final K key,
			final V value) {
		if (!map.containsKey(key)) {
			map.put(key, new ArrayList<V>());
		}
		map.get(key).add(value);
	}

	/**
	 * @param columnIndicesToRemove
	 *            The column indices to remove. (Starting from {@code 0}.)
	 * @param numColumns
	 *            The number of original columns.
	 * @return All of the original indices, except the {@code columnIndices}.
	 *         The others are shifted as necessary.
	 */
	private int[] allButMatched(final int[] columnIndicesToRemove,
			final int numColumns) {
		final Set<Integer> indicesToFilter = new HashSet<Integer>();
		for (final int colIndex : columnIndicesToRemove) {
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
	 *            A {@link DataTableSpec}.
	 * @return Might contain duplicates, these indices are present in {@code
	 *         dataTableSpec} (or not, because it might contain {@code -1}s).
	 */
	private int[] computeMatchedColumnIndices(final DataTableSpec dataTableSpec) {
		final int[] ret = new int[settingsModels.size()];
		for (int i = ret.length; i-- > 0;) {
			ret[i] = dataTableSpec.findColumnIndex(settingsModels.get(
					ColumnType.values()[i]).get(Boolean.TRUE).get(
					DialogType.name).getStringValue());
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
		for (final Pair<Pair<ColumnType, Boolean>, DialogType> outPair : possibleKeys()) {
			if (outPair.getRight() == DialogType.position) {
				positionModels.get(outPair.getLeft().getLeft()).saveSettingsTo(
						settings);
			} else {
				settingsModels.get(outPair.getLeft().getLeft()).get(
						outPair.getLeft().getRight()).get(outPair.getRight())
						.saveSettingsTo(settings);
			}
		}
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
		for (final Pair<Pair<ColumnType, Boolean>, DialogType> outPair : possibleKeys()) {
			if (outPair.getRight() == DialogType.position) {
				positionModels.get(outPair.getLeft().getLeft())
						.loadSettingsFrom(settings);
			} else {
				settingsModels.get(outPair.getLeft().getLeft()).get(
						outPair.getLeft().getRight()).get(outPair.getRight())
						.loadSettingsFrom(settings);
			}
		}
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
		for (final Pair<Pair<ColumnType, Boolean>, DialogType> outPair : possibleKeys()) {
			if (outPair.getRight() == DialogType.position) {
				positionModels.get(outPair.getLeft().getLeft())
						.validateSettings(settings);
			} else {
				settingsModels.get(outPair.getLeft().getLeft()).get(
						outPair.getLeft().getRight()).get(outPair.getRight())
						.validateSettings(settings);
			}
		}
	}

	/**
	 * @param xmlFilePath
	 *            The xml file path to load. It is relative to the model's class
	 *            path. It should conform to the xsd in
	 *            {@link BioConverterNodeModel#CONFIGURATION_XSD} (
	 *            {@value BioConverterNodeModel#CONFIGURATION_XSD}).
	 * @return The {@link Root} element read from the {@link #CONFIGURATION_XML}
	 *         ({@value #CONFIGURATION_XML}).
	 */
	static Root loadProperties(final String xmlFilePath) {
		try {
			final JAXBContext context = JAXBContext.newInstance(Root.class
					.getPackage().getName());
			final Unmarshaller unmarshaller = context.createUnmarshaller();
			final SchemaFactory schemaFactory = SchemaFactory
					.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
			final Schema schema = schemaFactory.newSchema(new StreamSource(
					BioConverterNodeModel.class
							.getResourceAsStream(CONFIGURATION_XSD)));
			unmarshaller.setSchema(schema);
			final Root ret = (Root) unmarshaller
					.unmarshal(BioConverterNodeModel.class
							.getResource(xmlFilePath));
			return ret;
		} catch (final SAXException e) {
			logger.fatal("Unable to load configuration: " + e.getMessage(), e);
			throw new RuntimeException(e);
		} catch (final JAXBException e) {
			logger.fatal("Unable to load configuration: " + e.getMessage(), e);
			throw new RuntimeException(e);
		}
	}

	/**
	 * Finds the default value for {@link SettingsModelString}s, based on the
	 * values present in {@code root}.
	 * <p>
	 * Note: This is an inefficient solution.
	 * 
	 * @param root
	 *            The configuration of defaults/profiles.
	 * @param columnType
	 *            The {@link ColumnType}.
	 * @param input
	 *            The input ({@code true}), or output ({@code false}) kind.
	 * @param dialogType
	 *            The {@link DialogType}.
	 * @return The found default value.
	 */
	public static String findDefault(final Root root,
			final ColumnType columnType, final boolean input,
			final DialogType dialogType) {
		final List<Default> defaults = root.getDefaults().getDefault();
		for (final Default d : defaults) {
			if (d.getColumn() == columnType && d.isInput() == input
					&& d.getDialog() == dialogType) {
				return d.getValue();
			}
		}
		throw new IllegalStateException("No defaults defined for " + columnType
				+ " " + (input ? "input" : "output") + " " + dialogType
				+ "\nCorrect in interop.xml");
	}
}

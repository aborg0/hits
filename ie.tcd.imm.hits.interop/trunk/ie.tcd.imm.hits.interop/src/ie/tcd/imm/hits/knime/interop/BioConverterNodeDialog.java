package ie.tcd.imm.hits.knime.interop;

import ie.tcd.imm.hits.common.PublicConstants;
import ie.tcd.imm.hits.knime.interop.BioConverterNodeModel.ConversionDefault;
import ie.tcd.imm.hits.util.Displayable;
import ie.tcd.imm.hits.util.Pair;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import java.lang.reflect.Array;

import org.knime.core.data.IntValue;
import org.knime.core.data.StringValue;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponent;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentWithDefaults;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelColumnName;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * <code>NodeDialog</code> for the "BioConverter" Node. Converts between
 * different kind of plate formats.
 * 
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more
 * complex dialog please derive directly from
 * {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author <a href="bakosg@tcd.ie">Gabor Bakos</a>
 */
public class BioConverterNodeDialog extends DefaultNodeSettingsPane {

	public static enum DialogType {
		group, name, format, type;

		private static final DialogType[] nonGroups = new DialogType[] { name,
				format, type };

		private static final DialogType[] inNonGroups = new DialogType[] {
				name, format };

		public static DialogType[] nonGroups(final boolean input) {
			return input ? inNonGroups : nonGroups;
		}
	}

	static enum ColumnType implements Displayable {
		Plate("Plate"), Replicate("Replicate");
		private final String displayText;

		private ColumnType(final String displayText) {
			this.displayText = displayText;
		}

		@Override
		public String getDisplayText() {
			return displayText;
		}
	}

	private static final Map<ColumnType, Map<Boolean, Map<DialogType, String>>> cfgKeys = createOuterEmptyMap();
	private static final Map<ColumnType, Map<Boolean, Map<DialogType, String>>> defaults = createOuterEmptyMap();
	private static final Map<ConversionDefault, Map<ColumnType, Map<Boolean, Map<DialogType, String>>>> patterns = new HashMap<ConversionDefault, Map<ColumnType, Map<Boolean, Map<DialogType, String>>>>();
	static {
		// TODO refactor!!!
		for (final ConversionDefault d : ConversionDefault.values()) {
			patterns.put(d, createOuterEmptyMap());
		}
		cfgKeys.get(ColumnType.Plate).get(Boolean.TRUE).put(DialogType.name,
				BioConverterNodeModel.CFGKEY_PLATE_IN);
		cfgKeys.get(ColumnType.Plate).get(Boolean.TRUE).put(DialogType.format,
				BioConverterNodeModel.CFGKEY_PLATE_IN_FORMAT);
		cfgKeys.get(ColumnType.Plate).get(Boolean.TRUE).put(DialogType.group,
				BioConverterNodeModel.CFGKEY_PLATE_IN_GROUP);
		cfgKeys.get(ColumnType.Plate).get(Boolean.FALSE).put(DialogType.name,
				BioConverterNodeModel.CFGKEY_PLATE_OUT);
		cfgKeys.get(ColumnType.Plate).get(Boolean.FALSE).put(DialogType.format,
				BioConverterNodeModel.CFGKEY_PLATE_OUT_FORMAT);
		cfgKeys.get(ColumnType.Plate).get(Boolean.FALSE).put(DialogType.type,
				BioConverterNodeModel.CFGKEY_PLATE_OUT_TYPE);
		cfgKeys.get(ColumnType.Plate).get(Boolean.FALSE).put(DialogType.group,
				BioConverterNodeModel.CFGKEY_PLATE_OUT_GROUP);
		defaults.get(ColumnType.Plate).get(Boolean.TRUE).put(DialogType.name,
				BioConverterNodeModel.DEFAULT_PLATE_IN);
		defaults.get(ColumnType.Plate).get(Boolean.TRUE).put(DialogType.format,
				BioConverterNodeModel.DEFAULT_PLATE_IN_FORMAT);
		defaults.get(ColumnType.Plate).get(Boolean.TRUE).put(DialogType.group,
				BioConverterNodeModel.DEFAULT_PLATE_IN_GROUP);
		defaults.get(ColumnType.Plate).get(Boolean.FALSE).put(DialogType.name,
				BioConverterNodeModel.DEFAULT_PLATE_OUT);
		defaults.get(ColumnType.Plate).get(Boolean.FALSE).put(
				DialogType.format,
				BioConverterNodeModel.DEFAULT_PLATE_OUT_FORMAT);
		defaults.get(ColumnType.Plate).get(Boolean.FALSE).put(DialogType.type,
				BioConverterNodeModel.DEFAULT_PLATE_OUT_TYPE);
		defaults.get(ColumnType.Plate).get(Boolean.FALSE).put(DialogType.group,
				BioConverterNodeModel.DEFAULT_PLATE_OUT_GROUP);
		cfgKeys.get(ColumnType.Replicate).get(Boolean.TRUE).put(
				DialogType.name, BioConverterNodeModel.CFGKEY_REPLICATE_IN);
		cfgKeys.get(ColumnType.Replicate).get(Boolean.TRUE).put(
				DialogType.format,
				BioConverterNodeModel.CFGKEY_REPLICATE_IN_FORMAT);
		cfgKeys.get(ColumnType.Replicate).get(Boolean.TRUE).put(
				DialogType.group,
				BioConverterNodeModel.CFGKEY_REPLICATE_IN_GROUP);
		cfgKeys.get(ColumnType.Replicate).get(Boolean.FALSE).put(
				DialogType.name, BioConverterNodeModel.CFGKEY_REPLICATE_OUT);
		cfgKeys.get(ColumnType.Replicate).get(Boolean.FALSE).put(
				DialogType.format,
				BioConverterNodeModel.CFGKEY_REPLICATE_OUT_FORMAT);
		cfgKeys.get(ColumnType.Replicate).get(Boolean.FALSE).put(
				DialogType.type,
				BioConverterNodeModel.CFGKEY_REPLICATE_OUT_TYPE);
		cfgKeys.get(ColumnType.Replicate).get(Boolean.FALSE).put(
				DialogType.group,
				BioConverterNodeModel.CFGKEY_REPLICATE_OUT_GROUP);
		defaults.get(ColumnType.Replicate).get(Boolean.TRUE).put(
				DialogType.name, BioConverterNodeModel.DEFAULT_REPLICATE_IN);
		defaults.get(ColumnType.Replicate).get(Boolean.TRUE).put(
				DialogType.format,
				BioConverterNodeModel.DEFAULT_REPLICATE_IN_FORMAT);
		defaults.get(ColumnType.Replicate).get(Boolean.TRUE).put(
				DialogType.group,
				BioConverterNodeModel.DEFAULT_REPLICATE_IN_GROUP);
		defaults.get(ColumnType.Replicate).get(Boolean.FALSE).put(
				DialogType.name, BioConverterNodeModel.DEFAULT_REPLICATE_OUT);
		defaults.get(ColumnType.Replicate).get(Boolean.FALSE).put(
				DialogType.format,
				BioConverterNodeModel.DEFAULT_REPLICATE_OUT_FORMAT);
		defaults.get(ColumnType.Replicate).get(Boolean.FALSE).put(
				DialogType.type,
				BioConverterNodeModel.DEFAULT_REPLICATE_OUT_TYPE);
		defaults.get(ColumnType.Replicate).get(Boolean.FALSE).put(
				DialogType.group,
				BioConverterNodeModel.DEFAULT_REPLICATE_OUT_GROUP);

		setPattern(ConversionDefault.cellHTS2Input, ColumnType.Plate, true,
				DialogType.name, PublicConstants.PLATE_COLUMN);
		setPattern(ConversionDefault.cellHTS2Input, ColumnType.Plate, true,
				DialogType.format, "\\d+");
		setPattern(ConversionDefault.cellHTS2Output, ColumnType.Plate, true,
				DialogType.name, PublicConstants.PLATE_COL_NAME);
		setPattern(ConversionDefault.cellHTS2Output, ColumnType.Plate, true,
				DialogType.format, "\\d+");
		setPattern(ConversionDefault.hcdc, ColumnType.Plate, true,
				DialogType.name, "barcode");
		setPattern(ConversionDefault.hcdc, ColumnType.Plate, true,
				DialogType.format, "(?:[^\\d]*)(\\d+)_(?:\\d+)");
		setPattern(ConversionDefault.cellHTS2Input, ColumnType.Plate, false,
				DialogType.name, PublicConstants.PLATE_COLUMN);
		setPattern(ConversionDefault.cellHTS2Input, ColumnType.Plate, false,
				DialogType.format, "${Plate}");
		setPattern(ConversionDefault.cellHTS2Input, ColumnType.Plate, false,
				DialogType.type, "Integer");
		setPattern(ConversionDefault.cellHTS2Output, ColumnType.Plate, false,
				DialogType.name, PublicConstants.PLATE_COL_NAME);
		setPattern(ConversionDefault.cellHTS2Output, ColumnType.Plate, false,
				DialogType.format, "${Plate}");
		setPattern(ConversionDefault.cellHTS2Output, ColumnType.Plate, false,
				DialogType.type, "Integer");
		setPattern(ConversionDefault.hcdc, ColumnType.Plate, false,
				DialogType.name, "barcode");
		setPattern(ConversionDefault.hcdc, ColumnType.Plate, false,
				DialogType.format, "${Experiment}${03Plate}_${03Replicate}");
		setPattern(ConversionDefault.hcdc, ColumnType.Plate, false,
				DialogType.type, "String");
		setPattern(ConversionDefault.cellHTS2Input, ColumnType.Replicate, true,
				DialogType.name, PublicConstants.REPLICATE_COLUMN);
		setPattern(ConversionDefault.cellHTS2Input, ColumnType.Replicate, true,
				DialogType.format, "\\d+");
		setPattern(ConversionDefault.cellHTS2Output, ColumnType.Replicate,
				true, DialogType.name, PublicConstants.REPLICATE_COL_NAME);
		setPattern(ConversionDefault.cellHTS2Output, ColumnType.Replicate,
				true, DialogType.format, "\\d+");
		setPattern(ConversionDefault.hcdc, ColumnType.Replicate, true,
				DialogType.name, "barcode");
		setPattern(ConversionDefault.hcdc, ColumnType.Replicate, true,
				DialogType.format, "(?:[^_]*_(\\d+)");
		setPattern(ConversionDefault.cellHTS2Input, ColumnType.Replicate,
				false, DialogType.name, PublicConstants.REPLICATE_COLUMN);
		setPattern(ConversionDefault.cellHTS2Input, ColumnType.Replicate,
				false, DialogType.format, "\\d+");
		setPattern(ConversionDefault.cellHTS2Input, ColumnType.Replicate,
				false, DialogType.type, "Integer");
		setPattern(ConversionDefault.cellHTS2Output, ColumnType.Replicate,
				false, DialogType.name, PublicConstants.REPLICATE_COL_NAME);
		setPattern(ConversionDefault.cellHTS2Output, ColumnType.Replicate,
				false, DialogType.format, "\\d+");
		setPattern(ConversionDefault.cellHTS2Output, ColumnType.Replicate,
				false, DialogType.type, "Integer");
		setPattern(ConversionDefault.hcdc, ColumnType.Replicate, false,
				DialogType.name, PublicConstants.REPLICATE_COL_NAME);
		setPattern(ConversionDefault.hcdc, ColumnType.Replicate, false,
				DialogType.format, "${Experiment}${03Plate}_${03Replicate}");
		setPattern(ConversionDefault.hcdc, ColumnType.Replicate, false,
				DialogType.type, "String");
	}

	private static Map<ColumnType, Map<Boolean, Map<DialogType, String>>> createOuterEmptyMap() {
		final Map<ColumnType, Map<Boolean, Map<DialogType, String>>> ret = new EnumMap<ColumnType, Map<Boolean, Map<DialogType, String>>>(
				ColumnType.class);
		for (final ColumnType c : ColumnType.values()) {
			ret.put(c, createEmptyMap());
		}
		return ret;
	}

	private static void setPattern(final ConversionDefault conv,
			final ColumnType col, final boolean input, final DialogType type,
			final String value) {
		patterns.get(conv).get(col).get(Boolean.valueOf(input))
				.put(type, value);
	}

	private static Map<Boolean, Map<DialogType, String>> createEmptyMap() {
		final Map<Boolean, Map<DialogType, String>> ret = new HashMap<Boolean, Map<DialogType, String>>();
		final Map<DialogType, String> left = new EnumMap<DialogType, String>(
				DialogType.class);
		final Map<DialogType, String> right = new EnumMap<DialogType, String>(
				DialogType.class);
		ret.put(Boolean.TRUE, left);
		ret.put(Boolean.FALSE, right);
		return ret;
	}

	/**
	 * New pane for configuring the BioConverter node.
	 */
	protected BioConverterNodeDialog() {
		super();
		setDefaultTabTitle("General");
		final Map<String, Boolean[]> genInEnablementMap = new LinkedHashMap<String, Boolean[]>();
		// key: name of concept
		final Map<ColumnType, Pair<Map<DialogType, DialogComponent>, Map<DialogType, DialogComponent>>> components = new LinkedHashMap<ColumnType, Pair<Map<DialogType, DialogComponent>, Map<DialogType, DialogComponent>>>();
		addSimpleComponents(components);
		addPatterns(components);
		for (final ConversionDefault v : ConversionDefault.values()) {
			genInEnablementMap.put(v.getDisplayText(), fill(Boolean.FALSE,
					components.size(), Boolean.class));
		}
		genInEnablementMap.put(ConversionDefault.custom.getDisplayText(), fill(
				Boolean.TRUE, components.size(), Boolean.class));
		final Map<String, Object[]> genInDefaultsMap = new LinkedHashMap<String, Object[]>();
		for (final ConversionDefault v : ConversionDefault.values()) {
			genInDefaultsMap.put(v.getDisplayText(), fill(v.getDisplayText(),
					components.size(), String.class));
		}
		final DialogComponentWithDefaults dialogGeneralIn = new DialogComponentWithDefaults(
				new SettingsModelString(
						BioConverterNodeModel.CFGKEY_GENERAL_IN_GROUP,
						BioConverterNodeModel.DEFAULT_GENERAL_IN_GROUP),
				"Input", genInEnablementMap, genInDefaultsMap, select(
						components, DialogType.group, true));
		final DialogComponentWithDefaults dialogGeneralOut = new DialogComponentWithDefaults(
				new SettingsModelString(
						BioConverterNodeModel.CFGKEY_GENERAL_OUT_GROUP,
						BioConverterNodeModel.DEFAULT_GENERAL_OUT_GROUP),
				"Output", genInEnablementMap, genInDefaultsMap, select(
						components, DialogType.group, false));
		createNewGroup("Conversion");
		addDialogComponent(dialogGeneralIn);
		addDialogComponent(dialogGeneralOut);
		createNewGroup("Other options");
		addDialogComponent(new DialogComponentBoolean(new SettingsModelBoolean(
				BioConverterNodeModel.CFGKEY_ADD_UNMATCHED,
				BioConverterNodeModel.DEFAULT_ADD_UNMATCHED),
				"Add unmatched columns?"));
		addDialogComponent(new DialogComponentBoolean(new SettingsModelBoolean(
				BioConverterNodeModel.CFGKEY_KEEP_ORIGINAL,
				BioConverterNodeModel.DEFAULT_KEEP_ORIGINAL),
				"Keep original columns?"));
		addDialogComponent(new DialogComponentBoolean(new SettingsModelBoolean(
				BioConverterNodeModel.CFGKEY_GENERATE_MISSING,
				BioConverterNodeModel.DEFAULT_GENERATE_MISSING),
				"Try generate missing column values?"));
		createTab(components, ColumnType.Plate);
		createTab(components, ColumnType.Replicate);
	}

	/**
	 * @param nonGroups
	 * @return
	 */
	private Map<String, Boolean[]> createEnablementMap(
			final DialogType[] nonGroups) {
		final Map<String, Boolean[]> enablementMap = new HashMap<String, Boolean[]>();
		for (final ConversionDefault cd : ConversionDefault.values()) {
			enablementMap.put(cd.getDisplayText(), fill(Boolean.FALSE,
					nonGroups.length, Boolean.class));
		}
		enablementMap.put(ConversionDefault.custom.getDisplayText(), fill(
				Boolean.TRUE, nonGroups.length, Boolean.class));
		return enablementMap;
	}

	/**
	 * @param components
	 * @param enablementMap
	 */
	private void addPatterns(
			final Map<ColumnType, Pair<Map<DialogType, DialogComponent>, Map<DialogType, DialogComponent>>> components) {
		for (final Entry<ColumnType, Map<Boolean, Map<DialogType, String>>> colEntry : cfgKeys
				.entrySet()) {
			final ColumnType columnType = colEntry.getKey();
			for (final Entry<Boolean, Map<DialogType, String>> inOutEntry : colEntry
					.getValue().entrySet()) {
				final Boolean input = inOutEntry.getKey();
				final DialogType[] nonGroups = DialogType.nonGroups(input
						.booleanValue());
				final DialogComponentWithDefaults groupComponent = new DialogComponentWithDefaults(
						new SettingsModelString(cfgKeys.get(columnType).get(
								input).get(DialogType.group), defaults.get(
								columnType).get(input).get(DialogType.group)),
						DialogType.group.name(),
						createEnablementMap(nonGroups), collectPatterns(
								columnType, input), select(components,
								columnType, Arrays.asList(nonGroups), input
										.booleanValue()));
				final Pair<Map<DialogType, DialogComponent>, Map<DialogType, DialogComponent>> pair = components
						.get(columnType);
				(input.booleanValue() ? pair.getLeft() : pair.getRight()).put(
						DialogType.group, groupComponent);
			}
		}
	}

	/**
	 * @param columnType
	 * @param input
	 * @return
	 */
	private Map<String, Object[]> collectPatterns(final ColumnType columnType,
			final Boolean input) {
		final DialogType[] nonGroups = DialogType.nonGroups(input);
		final Map<String, Object[]> ret = new LinkedHashMap<String, Object[]>();
		for (final Entry<ConversionDefault, Map<ColumnType, Map<Boolean, Map<DialogType, String>>>> entry : patterns
				.entrySet()) {
			final String displayText = entry.getKey().getDisplayText();
			final String[] value = new String[nonGroups.length];
			for (int i = nonGroups.length; i-- > 0;) {
				value[i] = entry.getValue().get(columnType).get(input).get(
						nonGroups[i]);
			}
			ret.put(displayText, value);
		}
		return ret;
	}

	/**
	 * @param components
	 */
	private void addSimpleComponents(
			final Map<ColumnType, Pair<Map<DialogType, DialogComponent>, Map<DialogType, DialogComponent>>> components) {
		for (final ColumnType colType : ColumnType.values()) {
			components
					.put(
							colType,
							new Pair<Map<DialogType, DialogComponent>, Map<DialogType, DialogComponent>>(
									new EnumMap<DialogType, DialogComponent>(
											DialogType.class),
									new EnumMap<DialogType, DialogComponent>(
											DialogType.class)));
			for (final boolean left : new boolean[] { true, false }) {
				for (final DialogType dialogType : DialogType.nonGroups(left)) {
					final Pair<Map<DialogType, DialogComponent>, Map<DialogType, DialogComponent>> pair = components
							.get(colType);
					final String configName = cfgKeys.get(colType).get(
							Boolean.valueOf(left)).get(dialogType);
					final String defaultValue = defaults.get(colType).get(
							Boolean.valueOf(left)).get(dialogType);
					final boolean colSelection = dialogType == DialogType.name
							&& left;
					final SettingsModelString stringModel = colSelection ? new SettingsModelColumnName(
							configName, defaultValue)
							: new SettingsModelString(configName, defaultValue);
					final String label = (left ? "input " : "output ")
							+ colType.getDisplayText() + " "
							+ dialogType.name() + ": ";
					DialogComponent dialog;
					switch (dialogType) {
					case name:
						dialog = colSelection ? new DialogComponentColumnNameSelection(
								stringModel, label, 0, IntValue.class,
								StringValue.class)
								: new DialogComponentString(stringModel, label,
										false, 15);
						break;
					case format:
						dialog = new DialogComponentString(stringModel, label,
								false, 30);
						break;
					case type:
						dialog = new DialogComponentStringSelection(
								stringModel, label, "Integer", "Real", "String");
						break;
					case group:
						throw new IllegalStateException(
								"group is not allowed here");
					default:
						throw new UnsupportedOperationException(
								"Not supported type: " + dialogType.getClass());
					}
					(left ? pair.getLeft() : pair.getRight()).put(dialogType,
							dialog);
				}
			}
		}
	}

	/**
	 * @param components
	 * @param columnType
	 */
	private void createTab(
			final Map<ColumnType, Pair<Map<DialogType, DialogComponent>, Map<DialogType, DialogComponent>>> components,
			final ColumnType columnType) {
		createNewTab(columnType.getDisplayText());
		createNewGroup("Input");
		setHorizontalPlacement(false);
		addDialogComponent(select(columnType, components, DialogType.group,
				true));
		setHorizontalPlacement(true);
		addDialogComponent(select(columnType, components, DialogType.name, true));
		addDialogComponent(select(columnType, components, DialogType.format,
				true));
		createNewGroup("Output");
		setHorizontalPlacement(false);
		addDialogComponent(select(columnType, components, DialogType.group,
				false));
		setHorizontalPlacement(true);
		addDialogComponent(select(columnType, components, DialogType.name,
				false));
		addDialogComponent(select(columnType, components, DialogType.format,
				false));
		addDialogComponent(select(columnType, components, DialogType.type,
				false));
	}

	/**
	 * @param colType
	 * @param components
	 * @param group
	 * @param left
	 * @return
	 */
	private DialogComponent select(
			final ColumnType colType,
			final Map<ColumnType, Pair<Map<DialogType, DialogComponent>, Map<DialogType, DialogComponent>>> components,
			final DialogType group, final boolean left) {
		final Pair<Map<DialogType, DialogComponent>, Map<DialogType, DialogComponent>> pair = components
				.get(colType);
		return (left ? pair.getLeft() : pair.getRight()).get(group);
	}

	/**
	 * @param components
	 * @param type
	 * @param left
	 * @return
	 */
	private static DialogComponent[] select(
			final Map<ColumnType, Pair<Map<DialogType, DialogComponent>, Map<DialogType, DialogComponent>>> components,
			final DialogType type, final boolean left) {
		final DialogComponent[] ret = new DialogComponent[components.size()];
		int i = 0;
		for (final Entry<ColumnType, Pair<Map<DialogType, DialogComponent>, Map<DialogType, DialogComponent>>> entry : components
				.entrySet()) {
			final Pair<Map<DialogType, DialogComponent>, Map<DialogType, DialogComponent>> value = entry
					.getValue();
			final Map<DialogType, DialogComponent> map = left ? value.getLeft()
					: value.getRight();
			ret[i++] = map.get(type);
		}
		return ret;
	}

	/**
	 * @param components
	 * @param columnType
	 * @param types
	 * @param left
	 * @return
	 */
	private static DialogComponent[] select(
			final Map<ColumnType, Pair<Map<DialogType, DialogComponent>, Map<DialogType, DialogComponent>>> components,
			final ColumnType columnType, final List<DialogType> types,
			final boolean left) {
		final DialogComponent[] ret = new DialogComponent[types.size()];
		int i = 0;
		final Pair<Map<DialogType, DialogComponent>, Map<DialogType, DialogComponent>> value = components
				.get(columnType);
		final Map<DialogType, DialogComponent> map = left ? value.getLeft()
				: value.getRight();
		for (final DialogType type : types) {
			ret[i++] = map.get(type);
		}
		return ret;
	}

	private static <T> T[] fill(final T init, final int length,
			final Class<T> cls) {
		final T[] ret = (T[]) Array.newInstance(cls, length);
		Arrays.fill(ret, init);
		return ret;
	}
}

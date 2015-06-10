package ie.tcd.imm.hits.knime.plate.format;

import ie.tcd.imm.hits.common.Format;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelEnum;
import org.knime.core.node.defaultnodesettings.SettingsModelEnumWithIcon;

/**
 * <code>NodeDialog</code> for the "Plate Format" Node. Converts between 96,
 * 384, 1536 format plates. It is also capable of mixing replicates in.
 * 
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more
 * complex dialog please derive directly from
 * {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author <a href="bakosg@tcd.ie">Gabor Bakos</a>
 */
public class PlateFormatNodeDialog extends DefaultNodeSettingsPane {

	/**
	 * New pane for configuring PlateFormat node dialog.
	 */
	protected PlateFormatNodeDialog() {
		super();

		final SettingsModelEnum<Format> fromWellCountModel = new SettingsModelEnum<Format>(
				PlateFormatNodeModel.CFGKEY_FROM_WELL_COUNT,
				PlateFormatNodeModel.DEFAULT_FROM_WELL_COUNT, Format.values());
		addDialogComponent(new DialogComponentStringSelection(
				fromWellCountModel, "Input well count:", fromWellCountModel
						.getDisplayTexts()));
		final SettingsModelEnum<Format> toWellCountModel = new SettingsModelEnum<Format>(
				PlateFormatNodeModel.CFGKEY_TO_WELL_COUNT,
				PlateFormatNodeModel.DEFAULT_TO_WELL_COUNT, Format.values());
		addDialogComponent(new DialogComponentStringSelection(toWellCountModel,
				"Output well count:", toWellCountModel.getDisplayTexts()));
		final SettingsModelEnumWithIcon<CombinationPattern> combinePatternModel = new SettingsModelEnumWithIcon<CombinationPattern>(
				PlateFormatNodeModel.CFGKEY_COMBINATION_PATTERN,
				CombinationPattern.LeftToRightThenDown,
				PlateFormatNodeModel.POSSIBLE_COMBINATION_PATTERN_VALUES);
		addDialogComponent(new DialogComponentStringSelection(
				combinePatternModel, "Layout: ", combinePatternModel
						.getStringIcons()));
	}
}

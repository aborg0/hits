package ie.tcd.imm.hits.knime.plate.format;

import ie.tcd.imm.hits.common.Format;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelEnum;

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
	 * New pane for configuring PlateFormat node dialog. This is just a
	 * suggestion to demonstrate possible default dialog components.
	 */
	protected PlateFormatNodeDialog() {
		super();

		final SettingsModelEnum<Format> fromWellCountModel = new SettingsModelEnum<Format>(
				PlateFormatNodeModel.CFGKEY_FROM_WELL_COUNT,
				PlateFormatNodeModel.DEFAULT_FROM_WELL_COUNT, Format.values());
		addDialogComponent(new DialogComponentStringSelection(
				fromWellCountModel, "From well:", fromWellCountModel
						.getDisplayTexts()));
		final SettingsModelEnum<Format> toWellCountModel = new SettingsModelEnum<Format>(
				PlateFormatNodeModel.CFGKEY_TO_WELL_COUNT,
				PlateFormatNodeModel.DEFAULT_TO_WELL_COUNT, Format.values());
		addDialogComponent(new DialogComponentStringSelection(toWellCountModel,
				"To well:", toWellCountModel.getDisplayTexts()));
		final SettingsModelEnum<CombinationPattern> combinePatternModel = new SettingsModelEnum<CombinationPattern>(
				PlateFormatNodeModel.CFGKEY_COMBINATION_PATTERN,
				CombinationPattern.LeftToRightThenDown, CombinationPattern
						.values());
		addDialogComponent(new DialogComponentStringSelection(
				combinePatternModel, "Layout: ", combinePatternModel
						.getDisplayTexts()));
	}
}

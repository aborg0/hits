/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package ie.tcd.imm.hits.knime.view.heatmap;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;

/**
 * <code>NodeDialog</code> for the "Heatmap" Node. Shows the heatmap of the
 * plates.
 * 
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more
 * complex dialog please derive directly from
 * {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
public class HeatmapNodeDialog extends DefaultNodeSettingsPane {

	/**
	 * New pane for configuring Heatmap node dialog. This is just a suggestion
	 * to demonstrate possible default dialog components.
	 */
	protected HeatmapNodeDialog() {
		super();
		final DialogComponentBoolean saveSettings = new DialogComponentBoolean(
				new SettingsModelBoolean(HeatmapNodeModel.CFGKEY_SAVE_SETTINGS,
						HeatmapNodeModel.DEFAULT_SAVE_SETTINGS),
				"Save settings of changes.");
		saveSettings.getModel().setEnabled(false);
		addDialogComponent(saveSettings);
	}
}

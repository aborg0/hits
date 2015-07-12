/*
 * All rights reserved. (C) Copyright 2011, Gabor Bakos
 */
package ie.tcd.imm.hits.knime.view.dendrogram.viewonly;

import ie.tcd.imm.hits.knime.view.ImageExportOption;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelEnum;

/**
 * Dialog options for the Dendrogram heatmap node.
 * 
 * @author Gabor Bakos
 */
class DendrogramNodeDialog extends DefaultNodeSettingsPane {
	/**
	 * The default constructor.
	 */
	DendrogramNodeDialog() {
		final SettingsModelEnum<ImageExportOption> exportImageModel = new SettingsModelEnum<ImageExportOption>(
				DendrogramNodeModel.CFGKEY_EXPORT_IMAGE,
				DendrogramNodeModel.DEFAULT_EXPORT_IMAGE,
				ImageExportOption.values());
		addDialogComponent(new DialogComponentStringSelection(exportImageModel,
				"Export image format", exportImageModel.getDisplayTexts()));
	}
}

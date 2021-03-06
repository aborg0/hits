/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package com.mind_era.knime.util.unpivot;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.defaultnodesettings.SettingsModelStringArray;

import com.mind_era.knime.common.util.HiLite;
import com.mind_era.knime.common.util.TransformingNodeModel;

/**
 * <code>NodeDialog</code> for the "Unpivot" Node. Introduces new rows (and
 * column(s)) based on the column name structure.
 * 
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more
 * complex dialog please derive directly from
 * {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
public class UnpivotNodeDialog extends DefaultNodeSettingsPane {

	/**
	 * New pane for configuring Unpivot node dialog. This is just a suggestion
	 * to demonstrate possible default dialog components.
	 */
	protected UnpivotNodeDialog() {
		super();
		final SettingsModelString patternModel = new SettingsModelString(
				UnpivotNodeModel.CFGKEY_PATTERN,
				UnpivotNodeModel.DEFAULT_PATTERN);
		addDialogComponent(new DialogComponentString(patternModel, "Pattern: ",
				true, 40));
		final DialogComponentTable table = new DialogComponentTable(
				new SettingsModelStringArray(
						UnpivotNodeModel.CFGKEY_NEW_COLUMNS,
						UnpivotNodeModel.DEFAULT_NEW_COLUMNS), patternModel, 0);
		patternModel.addChangeListener(table);
		addDialogComponent(table);
		addDialogComponent(new DialogComponentStringSelection(
				new SettingsModelString(TransformingNodeModel.CFGKEY_HILITE,
						TransformingNodeModel.DEFAULT_HILITE.getDisplayText()),
				"Enable HiLite support", HiLite
						.asDisplayTexts(TransformingNodeModel.supportedHiLites)));
	}
}

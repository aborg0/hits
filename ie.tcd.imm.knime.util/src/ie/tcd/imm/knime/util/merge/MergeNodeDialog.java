/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package ie.tcd.imm.knime.util.merge;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnFilter;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelFilterString;

/**
 * <code>NodeDialog</code> for the "Merge" Node. Resorts the rows. It is mostly
 * like an "anti-sort".
 * 
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more
 * complex dialog please derive directly from
 * {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
public class MergeNodeDialog extends DefaultNodeSettingsPane {

	/**
	 * New pane for configuring Merge node dialog. This is just a suggestion to
	 * demonstrate possible default dialog components.
	 */
	@SuppressWarnings("deprecation")
	protected MergeNodeDialog() {
		super();
		final DialogComponentColumnFilter mergeColumns = new DialogComponentColumnFilter(
				new SettingsModelFilterString(
						MergeNodeModel.CFGKEY_MERGE_COLUMNS), 0);
		mergeColumns.setExcludeTitle("Independent columns");
		mergeColumns.setIncludeTitle("Columns used to reorder");
		addDialogComponent(mergeColumns);
		final DialogComponentBoolean sortInMemory = new DialogComponentBoolean(
				new SettingsModelBoolean(MergeNodeModel.CFGKEY_SORT_IN_MEMORY,
						MergeNodeModel.DEFAULT_SORT_IN_MEMORY),
				"Sort in memory?");
		sortInMemory.setEnabled(false);
		sortInMemory.getModel().setEnabled(false);
		addDialogComponent(sortInMemory);
		addDialogComponent(new DialogComponentBoolean(new SettingsModelBoolean(
				MergeNodeModel.CFGKEY_SORT_ORDER_REVERSED,
				MergeNodeModel.DEFAULT_SORT_ORDER_REVERSED),
				"Reverse the order in a block?"));
		final DialogComponentBoolean haltOnErrorDialog = new DialogComponentBoolean(
				new SettingsModelBoolean(MergeNodeModel.CFGKEY_HALT_ON_ERROR,
						MergeNodeModel.DEFAULT_HALT_ON_ERROR), "Halt on error?");
		haltOnErrorDialog
				.setToolTipText("If not checked and there are missing combinations, it will report them as a warning, else stops with an error.");
		addDialogComponent(haltOnErrorDialog);
	}
}

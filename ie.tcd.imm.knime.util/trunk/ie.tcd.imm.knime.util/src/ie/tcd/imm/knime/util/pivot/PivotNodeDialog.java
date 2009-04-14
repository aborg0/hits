/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package ie.tcd.imm.knime.util.pivot;

import ie.tcd.imm.knime.util.pivot.PivotNodeModel.Behaviour;

import java.util.ArrayList;
import java.util.List;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnFilter;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelFilterString;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * <code>NodeDialog</code> for the "Pivot" Node. Converts some information
 * present in rows to columns.
 * 
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more
 * complex dialog please derive directly from
 * {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
public class PivotNodeDialog extends DefaultNodeSettingsPane {

	/**
	 * New pane for configuring Pivot node dialog. This is just a suggestion to
	 * demonstrate possible default dialog components.
	 */
	protected PivotNodeDialog() {
		super();
		final SettingsModelFilterString pivotModel = new SettingsModelFilterString(
				PivotNodeModel.CFGKEY_TO_COLUMNS);
		final DialogComponentColumnFilter pivotDialog = new DialogComponentColumnFilter(
				pivotModel, 0);
		addDialogComponent(pivotDialog);
		addDialogComponent(new DialogComponentString(new SettingsModelString(
				PivotNodeModel.CFGKEY_PATTERN, ""), "Pattern"));
		addDialogComponent(new DialogComponentStringSelection(
				new SettingsModelString(PivotNodeModel.CFGKEY_BEHAVIOUR,
						PivotNodeModel.DEFAULT_BEHAVIOUR), "Behaviour",
				Behaviour.fillEmpty.name(), Behaviour.signalError.name()));
		createNewTab("Value Columns");
		final SettingsModelFilterString valColModel = new SettingsModelFilterString(
				PivotNodeModel.CFGKEY_VALUES);
		final DialogComponentColumnFilter valuesDialog = new DialogComponentColumnFilter(
				valColModel, 0);
		addDialogComponent(valuesDialog);
		createNewTab("Key Columns");
		final SettingsModelFilterString keyColModel = new SettingsModelFilterString(
				PivotNodeModel.CFGKEY_KEYS);
		final DialogComponentColumnFilter keysDialog = new DialogComponentColumnFilter(
				keyColModel, 0);
		addDialogComponent(keysDialog);
		pivotModel.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(final ChangeEvent e) {
				final List<String> excludeList = pivotModel.getExcludeList();
				final List<String> includeList = pivotModel.getIncludeList();
				final List<String> valColExclude = new ArrayList<String>(
						valColModel.getExcludeList());
				final List<String> valColInclude = new ArrayList<String>(
						valColModel.getIncludeList());
				final List<String> keyColExclude = new ArrayList<String>(
						keyColModel.getExcludeList());
				final List<String> keyColInclude = new ArrayList<String>(
						keyColModel.getIncludeList());
				valColInclude.removeAll(includeList);
				valColExclude.removeAll(includeList);
				keyColInclude.removeAll(includeList);
				keyColExclude.removeAll(includeList);
				for (final String excluded : excludeList) {
					if (keyColInclude.contains(excluded)) {
						valColInclude.remove(excluded);
						valColExclude.add(excluded);
					} else if (valColInclude.contains(excluded)) {
						keyColInclude.remove(excluded);
						keyColExclude.add(excluded);
					} else {
						valColInclude.add(excluded);
						valColExclude.remove(excluded);
						keyColExclude.add(excluded);
					}
				}
				valColModel.setIncludeList(valColInclude);
				valColModel.setExcludeList(valColExclude);
				keyColModel.setIncludeList(keyColInclude);
				keyColModel.setExcludeList(keyColExclude);
			}
		});
	}
}
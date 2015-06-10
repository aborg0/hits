/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package ie.tcd.imm.knime.util.pivot;

import ie.tcd.imm.hits.knime.util.HiLite;
import ie.tcd.imm.hits.knime.util.TransformingNodeModel;
import ie.tcd.imm.knime.util.pivot.PivotNodeModel.Behaviour;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnFilter;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentTwoColumnStrings;
import org.knime.core.node.defaultnodesettings.SettingsModelFilterString;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.defaultnodesettings.UpdatableComponent;
import org.knime.core.node.port.PortObjectSpec;

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
				pivotModel, 0, true);
		pivotDialog.setIncludeTitle("Pivot columns");
		pivotDialog.setExcludeTitle("Key or value columns");
		addDialogComponent(pivotDialog);
		final SettingsModelString patternModel = new SettingsModelString(
				PivotNodeModel.CFGKEY_PATTERN, "");
		addDialogComponent(new DialogComponentString(patternModel, "Pattern",
				true, 70));
		pivotModel.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(final ChangeEvent e) {
				final StringBuilder sb = new StringBuilder("${}_");
				for (final String name : pivotModel.getIncludeList()) {
					sb.append("${").append(name).append("}_");
				}
				if (pivotModel.getIncludeList().size() > 0) {
					sb.setLength(sb.length() - 1);
				}
				patternModel.setStringValue(sb.toString());
			}
		});
		addDialogComponent(new DialogComponentStringSelection(
				new SettingsModelString(PivotNodeModel.CFGKEY_BEHAVIOUR,
						PivotNodeModel.DEFAULT_BEHAVIOUR), "Behaviour",
				Behaviour.fillEmpty.name(), Behaviour.signalError.name()));
		final UpdatableComponent updatableComponent = new UpdatableComponent();
		addDialogComponent(updatableComponent);
		addDialogComponent(new DialogComponentStringSelection(
				new SettingsModelString(TransformingNodeModel.CFGKEY_HILITE,
						TransformingNodeModel.DEFAULT_HILITE.getDisplayText()),
				"HiLite Strategy: ", HiLite
						.asDisplayTexts(TransformingNodeModel.supportedHiLites)));
		createNewTab("Other Columns");
		final SettingsModelFilterString keyColModel = new SettingsModelFilterString(
				PivotNodeModel.CFGKEY_KEYS);
		final DialogComponentTwoColumnStrings keysDialog = new DialogComponentTwoColumnStrings(
				keyColModel, "Keys", "Values");
		addDialogComponent(keysDialog);
		pivotModel.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(final ChangeEvent e) {
				final List<String> excludeList = pivotModel.getExcludeList();
				final List<String> includeList = pivotModel.getIncludeList();
				final List<String> keyColExclude = new ArrayList<String>(
						keyColModel.getExcludeList());
				final List<String> keyColInclude = new ArrayList<String>(
						keyColModel.getIncludeList());
				keyColInclude.removeAll(includeList);
				keyColExclude.removeAll(includeList);
				for (final String excluded : excludeList) {
					if (!keyColInclude.contains(excluded)
							&& !keyColExclude.contains(excluded)) {
						keyColExclude.add(excluded);
					}
				}
				final PortObjectSpec[] lastSpecs = updatableComponent
						.getLastPortObjectSpecs();
				if (lastSpecs == null) {
					return;
				}
				final DataTableSpec lastSpec = (DataTableSpec) lastSpecs[0];
				final DataColumnSpec[] cols = new DataColumnSpec[includeList
						.size()];
				{
					int i = 0;
					for (final String colName : includeList) {
						cols[i++] = lastSpec.getColumnSpec(colName);
					}
				}
				keyColModel.setIncludeList(keyColInclude);
				keyColModel.setExcludeList(keyColExclude);
				keysDialog.setAllPossibleValues(new LinkedHashSet<String>(
						excludeList));
			}
		});
	}
}

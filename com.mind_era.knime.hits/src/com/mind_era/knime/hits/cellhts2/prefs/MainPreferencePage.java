/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package com.mind_era.knime.hits.cellhts2.prefs;

import java.util.Arrays;

import javax.annotation.concurrent.NotThreadSafe;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PlatformUI;

import com.mind_era.knime.common.PossibleStatistics;
import com.mind_era.knime.hits.cellhts2.prefs.ui.ColumnSelectionFieldEditor;
import com.mind_era.knime.hits.internal.Activator;

/**
 * This class represents a preference page that is contributed to the
 * Preferences dialog. By subclassing <samp>FieldEditorPreferencePage</samp>, we
 * can use the field support built into JFace that allows us to create a page
 * that is small and knows how to save, restore and apply itself.
 * <p>
 * This page is used to modify preferences only. They are stored in the
 * preference store that belongs to the main plug-in class. That way,
 * preferences can be accessed directly via the preference store.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
@NotThreadSafe
public class MainPreferencePage extends FieldEditorPreferencePage implements
		IWorkbenchPreferencePage {

	/**
	 * Constructs the {@link MainPreferencePage}
	 */
	public MainPreferencePage() {
		super(GRID);
		setPreferenceStore(Activator.getInstance().getPreferenceStore());
		setDescription("You can adjust the HiTS related node to your needs.");
	}

	/**
	 * Creates the field editors. Field editors are abstractions of the common
	 * GUI blocks needed to manipulate various types of preferences. Each field
	 * editor knows how to save and restore itself.
	 */
	public void createFieldEditors() {
		final BooleanFieldEditor useNamesFieldEditor = new BooleanFieldEditor(
				PreferenceConstants.USE_NAMES_INSTEAD_OF_CHANNELS,
				"Use &names instead of numbers for channels",
				getFieldEditorParent());
		final BooleanFieldEditor useTCDExtensionsFieldEditor = new BooleanFieldEditor(
				PreferenceConstants.USE_TCD_EXTENSIONS, "Use &TCD extension?",
				getFieldEditorParent()); // {
		// boolean alreadyAdded = false;
		//
		// @Override
		// protected Button getChangeControl(Composite parent) {
		// final Button button = super.getChangeControl(parent);
		// if (!alreadyAdded) {
		// button.addSelectionListener(new SelectionAdapter() {
		//
		// @Override
		// public void widgetSelected(SelectionEvent e) {
		// useNamesFieldEditor.setEnabled(button
		// .getSelection(), getFieldEditorParent());
		// }
		// });
		// alreadyAdded = true;
		// }
		// return button;
		// }
		// };
		addField(useTCDExtensionsFieldEditor);
		addField(useNamesFieldEditor);
		useTCDExtensionsFieldEditor.load();
		// TODO handle the inconsistencies. (The enabledness of the node is not
		// always correct.)

		// useNamesFieldEditor.setEnabled(useTCDExtensionsFieldEditor
		// .getPreferenceStore().getBoolean(
		// useTCDExtensionsFieldEditor.getPreferenceName()),
		// getFieldEditorParent());

		final ColumnSelectionFieldEditor<PossibleStatistics> columnSelectionFieldEditor = new ColumnSelectionFieldEditor<PossibleStatistics>(
				PreferenceConstants.RESULT_COL_ORDER,
				"&Column order:",
				getFieldEditorParent(),
				Arrays
						.<PossibleStatistics> asList(PossibleStatistics
								.values()));
		PlatformUI.getWorkbench().getHelpSystem().setHelp(
				columnSelectionFieldEditor
						.getListControl(getFieldEditorParent()),
				"ie.tcd.imm.hits.help.pref.columnSelection");
		addField(columnSelectionFieldEditor);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
	 */
	public void init(final IWorkbench workbench) {
	}

}
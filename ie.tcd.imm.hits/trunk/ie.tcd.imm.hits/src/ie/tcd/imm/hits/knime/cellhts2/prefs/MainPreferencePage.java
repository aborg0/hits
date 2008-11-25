package ie.tcd.imm.hits.knime.cellhts2.prefs;

import ie.tcd.imm.hits.knime.cellhts2.prefs.ui.ColumnSelectionFieldEditor;
import ie.tcd.imm.hits.knime.xls.ImporterNodePlugin;

import java.util.Arrays;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

/**
 * This class represents a preference page that is contributed to the
 * Preferences dialog. By subclassing <samp>FieldEditorPreferencePage</samp>,
 * we can use the field support built into JFace that allows us to create a page
 * that is small and knows how to save, restore and apply itself.
 * <p>
 * This page is used to modify preferences only. They are stored in the
 * preference store that belongs to the main plug-in class. That way,
 * preferences can be accessed directly via the preference store.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */

public class MainPreferencePage extends FieldEditorPreferencePage implements
		IWorkbenchPreferencePage {

	public MainPreferencePage() {
		super(GRID);
		setPreferenceStore(ImporterNodePlugin.getDefault().getPreferenceStore());
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

		addField(new ColumnSelectionFieldEditor<PreferenceConstants.PossibleStatistics>(
				PreferenceConstants.RESULT_COL_ORDER,
				"&Column order:",
				getFieldEditorParent(),
				Arrays
						.<PreferenceConstants.PossibleStatistics> asList(PreferenceConstants.PossibleStatistics
								.values())));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
	 */
	public void init(final IWorkbench workbench) {
	}

}
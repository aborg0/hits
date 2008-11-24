package ie.tcd.imm.hits.knime.xls;

import ie.tcd.imm.hits.knime.util.DialogComponentFileChooserWithListener;
import ie.tcd.imm.hits.knime.util.DialogComponentMultiFileChooser;

import java.io.File;
import java.io.FilenameFilter;

import javax.swing.JFileChooser;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.defaultnodesettings.SettingsModelStringArray;

/**
 * <code>NodeDialog</code> for the "Importer" Node. Reads the data from xls
 * files
 * 
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more
 * complex dialog please derive directly from
 * {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author TCD
 */
public class ImporterNodeDialog extends DefaultNodeSettingsPane {

	static final String DEFAULT_DIR = System.getProperty("user.home");

	private static final FilenameFilter filenameFilter = new FilenameFilter() {
		@Override
		public boolean accept(final File dir, final String name) {
			return name.toLowerCase().endsWith(".xls");
		}
	};

	/**
	 * New pane for configuring Importer node dialog. This is just a suggestion
	 * to demonstrate possible default dialog components.
	 */
	protected ImporterNodeDialog() {
		super();

		final SettingsModelStringArray files = new SettingsModelStringArray(
				ImporterNodeModel.CFGKEY_FILES, new String[] {});
		// final SettingsModelString dirName = new SettingsModelString(
		// ImporterNodeModel.CFGKEY_DIR, DEFAULT_DIR);
		// final DialogComponentFileChooser_evopro dirChooser = new
		// DialogComponentFileChooser_evopro(
		// dirName, ImporterNodeModel.CFGKEY_DIR, true);
		final DialogComponentMultiFileChooser fileChooser = new DialogComponentMultiFileChooser(
				files, "XLS files", ImporterNodeModel.CFGKEY_FILES, 12, ".xls");
		fileChooser.setBorderTitle("Folder of xls files:");
		fileChooser
				.setToolTipText("Select the folder of the xls files from IN Cell Analyser 1000 outputs.");
		// final String[] fileNames = new File(dirName.getStringValue())
		// .list(filenameFilter);
		// final List<String> list = (fileNames == null) ? Collections
		// .<String> singletonList("") : Arrays.asList(fileNames);
		// final DialogComponentStringListSelection fileNamesComponent = new
		// DialogComponentStringListSelection(
		// files, "XLS files:", list,
		// ListSelectionModel.MULTIPLE_INTERVAL_SELECTION, true, 10);
		// dirChooser.getCombobox().addItemListener(new ItemListener() {
		//
		// @Override
		// public void itemStateChanged(final ItemEvent e) {
		// // final String selectedItem = (String) ((JComboBox)
		// // e.getSource())
		// // .getSelectedItem();
		// final String selectedItem = (String) e.getItem();
		// if (selectedItem == null) {
		// return;
		// }
		// final File dir = new File(selectedItem);
		// final String[] dirFiles = dir.list(filenameFilter);
		// final List<String> list;
		// if (dirFiles == null) {
		// list = Collections.<String> emptyList();
		// } else {
		// Arrays.sort(dirFiles);
		// list = new ArrayList<String>(dirFiles.length);
		// for (final String string : dirFiles) {
		// list.add(string);
		// }
		// }
		// fileNamesComponent.replaceListItems(list);
		// dirName.setStringValue(selectedItem);
		// }
		// });
		// fileNamesComponent
		// .setToolTipText("These files (in this alphabetical order) will be
		// representing the plates and replicates.");
		createNewGroup("Data files");
		// addDialogComponent(dirChooser);
		// addDialogComponent(fileNamesComponent);
		addDialogComponent(fileChooser);
		closeCurrentGroup();
		createNewGroup("Experiment parameters");
		final DialogComponentNumber wellCountDialog = new DialogComponentNumber(
				new SettingsModelIntegerBounded(
						ImporterNodeModel.CFGKEY_WELL_COUNT,
						ImporterNodeModel.DEFAULT_WELL_COUNT,
						ImporterNodeModel.DEFAULT_WELL_COUNT,
						ImporterNodeModel.MAX_WELL_COUNT), "Well count:", /* step */
				384 - 96, /* componentwidth */5);
		wellCountDialog
				.setToolTipText("Only 96, or 384 format wells are accepted.");
		addDialogComponent(wellCountDialog);
		final DialogComponentNumber plateDialog = new DialogComponentNumber(
				new SettingsModelIntegerBounded(
						ImporterNodeModel.CFGKEY_PLATE_COUNT,
						ImporterNodeModel.DEFAULT_PLATE_COUNT, 1,
						Integer.MAX_VALUE), "Plate count:", /* step */1, /* componentwidth */
				5);
		plateDialog.setToolTipText("Number of plates in the experiment.");
		addDialogComponent(plateDialog);
		final DialogComponentNumber replicatesDialog = new DialogComponentNumber(
				new SettingsModelIntegerBounded(
						ImporterNodeModel.CFGKEY_REPLICATE_COUNT,
						ImporterNodeModel.DEFAULT_REPLICATE_COUNT, 1,
						ImporterNodeModel.MAX_REPLICATE_COUNT),
				"Replicate count:", /* step */
				1, /* componentwidth */
				5);
		replicatesDialog
				.setToolTipText("Number of replicates in the experiment");
		addDialogComponent(replicatesDialog);
		closeCurrentGroup();
		createNewGroup("Gene annotations");
		final DialogComponentFileChooserWithListener annotFileChooser = new DialogComponentFileChooserWithListener(
				new SettingsModelString(
						ImporterNodeModel.CFGKEY_ANNOTATION_FILE,
						ImporterNodeModel.DEFAULT_ANNOTATION_FILE),
				ImporterNodeModel.CFGKEY_ANNOTATION_FILE,
				JFileChooser.OPEN_DIALOG, false, ".txt", ".TXT");
		annotFileChooser.setBorderTitle("Annotation file");
		annotFileChooser.setToolTipText("The file containing the annotations.");
		addDialogComponent(annotFileChooser);
		final DialogComponentBoolean addAnnotationsDialog = new DialogComponentBoolean(
				new SettingsModelBoolean(
						ImporterNodeModel.CFGKEY_COMBINE_ANNOTATIONS,
						ImporterNodeModel.DEFAULT_COMBINE_ANNOTATIONS),
				"Put annotations to the main output?");
		addAnnotationsDialog
				.setToolTipText("Add the gene annotations to the main outport too if checked.");
		addDialogComponent(addAnnotationsDialog);
		closeCurrentGroup();
	}
}

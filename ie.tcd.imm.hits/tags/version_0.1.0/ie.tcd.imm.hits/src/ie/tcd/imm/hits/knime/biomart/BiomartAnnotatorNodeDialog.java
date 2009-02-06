package ie.tcd.imm.hits.knime.biomart;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JComboBox;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.node.NodeLogger;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentMultiLineString;
import org.knime.core.node.defaultnodesettings.DialogComponentStringListSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.defaultnodesettings.SettingsModelStringArray;
import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPFactor;
import org.rosuda.REngine.REXPGenericVector;
import org.rosuda.REngine.REXPString;
import org.rosuda.REngine.RList;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;

/**
 * <code>NodeDialog</code> for the "BiomartAnnotator" Node. Adds some
 * annotations from the BioMart databases using the biomaRt R package.
 * 
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more
 * complex dialog please derive directly from
 * {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
public class BiomartAnnotatorNodeDialog extends DefaultNodeSettingsPane {
	private static final NodeLogger logger = NodeLogger
			.getLogger(BiomartAnnotatorNodeDialog.class);

	private final Map<String, String> attributes = new HashMap<String, String>();

	/**
	 * New pane for configuring the BiomartAnnotator node.
	 */
	protected BiomartAnnotatorNodeDialog() {
		super();
		final DialogComponentStringSelection biomartDatabaseDialog = new DialogComponentStringSelection(
				new SettingsModelString(
						BiomartAnnotatorNodeModel.CFGKEY_BIOMART_DATABASE,
						BiomartAnnotatorNodeModel.DEFAULT_BIOMART_DATABASE),
				"biomaRt database: ", new String[] { "" });
		final DialogComponentStringSelection biomartDatasetDialog = new DialogComponentStringSelection(
				new SettingsModelString(
						BiomartAnnotatorNodeModel.CFGKEY_BIOMART_DATASET,
						BiomartAnnotatorNodeModel.DEFAULT_BIOMART_DATASET),
				"biomaRt dataset:", new String[] { "" });
		final DialogComponentStringListSelection biomartAttributesDialog = new DialogComponentStringListSelection(
				new SettingsModelStringArray(
						BiomartAnnotatorNodeModel.CFGKEY_BIOMART_ATTRIBUTES,
						BiomartAnnotatorNodeModel.DEFAULT_BIOMART_ATTRIBUTES),
				"Attributes:", "");
		final DialogComponentMultiLineString selectedBiomartAttributesDialog = new DialogComponentMultiLineString(
				new SettingsModelString("selected_attributes_not_used", ""),
				"Selected: ", false, 70, 8);
		biomartDatabaseDialog.getModel().addChangeListener(
				new ChangeListener() {
					@Override
					public void stateChanged(final ChangeEvent e) {
						final JComboBox combobox = ((JComboBox) e.getSource());
						final Object selectedItem = combobox.getSelectedItem();
						if (selectedItem instanceof String) {
							final String dbName = (String) selectedItem;
							try {
								final RConnection conn = new RConnection();
								try {
									conn.voidEval("library(\"biomaRt\")");
									conn.voidEval("biomartDb = useMart(\""
											+ dbName + "\")");
									final REXP datasetsResult = conn
											.eval("listDatasets(biomartDb)");
									final RList table = ((REXPGenericVector) datasetsResult)
											.asList();

									final String[] shortNames = ((REXPString) table
											.get(0)).asStrings();
									biomartDatasetDialog.replaceListItems(
											Arrays.asList(shortNames), null);
								} finally {
									conn.close();
								}
							} catch (final RserveException e1) {
								logger.error(
										"Unable to select the datasets for "
												+ dbName, e1);
							}
						}
					}
				});
		biomartDatasetDialog.getModel().addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(final ChangeEvent e) {
				final JComboBox combobox = ((JComboBox) e.getSource());
				final Object object = ((JComboBox) biomartDatabaseDialog
						.getComponentPanel().getComponent(1)).getSelectedItem();
				if (object instanceof String) {
					final String dbName = (String) object;

					final Object selectedItem = combobox.getSelectedItem();
					if (selectedItem instanceof String) {
						final String dataset = (String) selectedItem;
						try {
							final RConnection conn = new RConnection();
							try {
								conn.voidEval("library(\"biomaRt\")");
								conn.voidEval("biomartDb = useMart(\"" + dbName
										+ "\")");

								final REXP datasetsResult = conn
										.eval("listAttributes(useDataset(\""
												+ dataset
												+ "\",mart=biomartDb))");
								final RList table = ((REXPGenericVector) datasetsResult)
										.asList();

								final String[] shortNames = ((REXPString) table
										.get(0)).asStrings();
								final String[] descriptions = ((REXPString) table
										.get(1)).asStrings();
								attributes.clear();
								for (int i = 0; i < shortNames.length; i++) {
									attributes.put(shortNames[i],
											descriptions[i]);
								}

								biomartAttributesDialog.replaceListItems(Arrays
										.asList(shortNames), new String[0]);
							} finally {
								conn.close();
							}
						} catch (final RserveException e1) {
							logger.error("Unable to select the attributes for "
									+ dbName + "/" + dataset, e1);
						}
					}
				}
			}
		});
		try {
			final RConnection conn = new RConnection();
			try {
				conn.voidEval("library(\"biomaRt\")");
				final REXPGenericVector martsList = (REXPGenericVector) conn
						.eval("listMarts()");
				biomartDatabaseDialog.replaceListItems(Arrays
						.asList(((REXPFactor) martsList.asList().get(0))
								.asStrings()), null);
			} finally {
				conn.close();
			}
		} catch (final RserveException e1) {
			logger.error("Problem loading the possible databases", e1);
		}

		addDialogComponent(biomartDatabaseDialog);
		addDialogComponent(biomartDatasetDialog);
		createNewGroup("Attributes");
		setHorizontalPlacement(true);
		addDialogComponent(biomartAttributesDialog);
		biomartAttributesDialog.getModel().addChangeListener(
				new ChangeListener() {
					// ((JList) ((JScrollPane)
					// biomartAttributesDialog.getComponentPanel()
					// .getComponent(1)).getViewport().getComponent(0))
					// .addListSelectionListener(new ListSelectionListener() {
					// @Override
					// public void valueChanged(final ListSelectionEvent e) {
					@Override
					public void stateChanged(final ChangeEvent e) {
						final String[] selectedValues = ((SettingsModelStringArray) e
								.getSource()).getStringArrayValue();
						final StringBuilder sb = new StringBuilder();
						for (final String selected : selectedValues) {
							sb.append(selected).append(" - ").append(
									attributes.get(selected)).append("\n");
						}
						((SettingsModelString) selectedBiomartAttributesDialog
								.getModel()).setStringValue(sb.toString());
					}
				});
		addDialogComponent(selectedBiomartAttributesDialog);
		setHorizontalPlacement(false);

	}

	// private static void addActionListenerTo(
	// final DialogComponentStringSelection dialog,
	// final ActionListener actionListener) {
	// final JComboBox combobox = ((JComboBox) dialog.getComponentPanel()
	// .getComponent(1));
	// combobox.addActionListener(actionListener);
	// }

}

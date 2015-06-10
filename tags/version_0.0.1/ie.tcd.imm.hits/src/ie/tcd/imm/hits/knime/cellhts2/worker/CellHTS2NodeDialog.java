package ie.tcd.imm.hits.knime.cellhts2.worker;

import ie.tcd.imm.hits.knime.util.SelectionMoverActionListener;
import ie.tcd.imm.hits.knime.xls.ImporterNodeModel;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;
import javax.swing.ButtonModel;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnFilter;
import org.knime.core.node.defaultnodesettings.DialogComponentDoubleRange;
import org.knime.core.node.defaultnodesettings.DialogComponentFileChooser;
import org.knime.core.node.defaultnodesettings.DialogComponentMultiLineString;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelDoubleBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelDoubleRange;
import org.knime.core.node.defaultnodesettings.SettingsModelFilterString;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.util.ColumnFilterPanel;

import edu.umd.cs.findbugs.annotations.DefaultAnnotation;

/**
 * <code>NodeDialog</code> for the "CellHTS2" Node. This node performs the
 * calculations using CellHTS2
 * 
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more
 * complex dialog please derive directly from
 * {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
@NotThreadSafe
@DefaultAnnotation(Nonnull.class)
public class CellHTS2NodeDialog extends DefaultNodeSettingsPane {

	/**
	 * New pane for configuring CellHTS2 node dialog.
	 */
	protected CellHTS2NodeDialog() {
		super();
		final DialogComponentMultiLineString sample = new DialogComponentMultiLineString(
				new SettingsModelString("sample", ""), "Samples:");

		final DialogComponentString experimentDialog = new DialogComponentString(
				new SettingsModelString(
						CellHTS2NodeModel.CFGKEY_EXPERIMENT_NAME,
						CellHTS2NodeModel.DEFAULT_EXPERIMENT_NAME),
				"experiment", true, 51);
		final JTextField experimentTextField = (JTextField) experimentDialog
				.getComponentPanel().getComponent(1);
		experimentDialog
				.setToolTipText("The short really description of the experiment (should not contain tab characters)");
		addDialogComponent(experimentDialog);
		createNewGroup("Normalization parameters");
		final DialogComponentStringSelection normalizationDialog = new DialogComponentStringSelection(
				new SettingsModelString(
						CellHTS2NodeModel.CFGKEY_NORMALISATION_METHOD,
						CellHTS2NodeModel.POSSIBLE_NORMALISATION_METHODS[1]),
				"Normalisation method: ",
				CellHTS2NodeModel.POSSIBLE_NORMALISATION_METHODS);
		final JComboBox normalizationCombobox = (JComboBox) normalizationDialog
				.getComponentPanel().getComponent(1);
		normalizationDialog
				.setToolTipText("The normalization method for the parameters. (For \"Z score\" select mean or median, and also some kind of variance adjust.)");
		addDialogComponent(normalizationDialog);
		setHorizontalPlacement(true);
		final DialogComponentBoolean isMultiplicativeDialog = new DialogComponentBoolean(
				new SettingsModelBoolean(
						CellHTS2NodeModel.CFGKEY_IS_MULTIPLICATIVE_NORMALISATION,
						CellHTS2NodeModel.DEFAULT_IS_MULTIPLICATIVE_NORMALISATION),
				"multiplicative?");
		final ButtonModel isMultiplicativeDialogModel = ((JCheckBox) isMultiplicativeDialog
				.getComponentPanel().getComponent(0)).getModel();
		isMultiplicativeDialog
				.setToolTipText("Using the additive, or the multiplicative normalization.");
		addDialogComponent(isMultiplicativeDialog);
		final DialogComponentBoolean logDialog = new DialogComponentBoolean(
				new SettingsModelBoolean(
						CellHTS2NodeModel.CFGKEY_LOG_TRANSFORM,
						CellHTS2NodeModel.DEFAULT_LOG_TRANSFORM),
				"log transform?");
		logDialog.setToolTipText("Log transform the normalized values?");
		addDialogComponent(logDialog);
		setHorizontalPlacement(false);
		final DialogComponentStringSelection varianceScaleDialog = new DialogComponentStringSelection(
				new SettingsModelString(CellHTS2NodeModel.CFGKEY_SCALE,
						CellHTS2NodeModel.POSSIBLE_SCALE[0]),
				"Variance adjustment: ", CellHTS2NodeModel.POSSIBLE_SCALE);
		varianceScaleDialog
				.setToolTipText("Plate-specific variance adjustment");
		addDialogComponent(varianceScaleDialog);
		closeCurrentGroup();
		createNewGroup("Replicate summary");
		final DialogComponentStringSelection scoreDialog = new DialogComponentStringSelection(
				new SettingsModelString(CellHTS2NodeModel.CFGKEY_SCORE,
						CellHTS2NodeModel.POSSIBLE_SCORE[1]), "Score: ",
				CellHTS2NodeModel.POSSIBLE_SCORE);
		scoreDialog.setToolTipText("Scoring of replicates.");
		addDialogComponent(scoreDialog);
		final DialogComponentStringSelection summarizeReplicatesDialog = new DialogComponentStringSelection(
				new SettingsModelString(CellHTS2NodeModel.CFGKEY_SUMMARISE,
						CellHTS2NodeModel.POSSIBLE_SUMMARISE[0]),
				"Summarise replicates with: ",
				CellHTS2NodeModel.POSSIBLE_SUMMARISE);
		summarizeReplicatesDialog
				.setToolTipText("Summarize the replicates with this method.");
		addDialogComponent(summarizeReplicatesDialog);
		closeCurrentGroup();
		createNewGroup("Result directory");
		final DialogComponentFileChooser fileChooser = new DialogComponentFileChooser(
				new SettingsModelString(CellHTS2NodeModel.CFGKEY_OUTPUT_DIR,
						CellHTS2NodeModel.DEFAULT_OUTPUT_DIR),
				CellHTS2NodeModel.CFGKEY_OUTPUT_DIR, JFileChooser.OPEN_DIALOG,
				true);
		fileChooser.setBorderTitle("Results: ");
		fileChooser
				.setToolTipText("The directory of the results. (Previous results will be overwritten.)");
		setHorizontalPlacement(true);
		addDialogComponent(fileChooser);
		final DialogComponentStringSelection patternDialog = new DialogComponentStringSelection(
				new SettingsModelString(
						CellHTS2NodeModel.CFGKEY_FOLDER_PATTERN,
						CellHTS2NodeModel.DEFAULT_FOLDER_PATTERN), "Pattern:",
				CellHTS2NodeModel.POSSIBLE_FOLDER_PATTERNS);
		final JComboBox patternCombobox = (JComboBox) patternDialog
				.getComponentPanel().getComponent(1);
		patternCombobox.setEditable(true);
		addDialogComponent(patternDialog);
		setHorizontalPlacement(false);
		addDialogComponent(sample);
		closeCurrentGroup();
		createNewTab("Parameters");
		@SuppressWarnings("unchecked")
		final DialogComponentColumnFilter parametersDialog = new DialogComponentColumnFilter(
				new SettingsModelFilterString(
						CellHTS2NodeModel.CFGKEY_PARAMETERS, new String[0],
						new String[] { ImporterNodeModel.PLATE_COL_NAME,
								ImporterNodeModel.REPLICATE_COL_NAME,
								ImporterNodeModel.WELL_COL_NAME,
								ImporterNodeModel.GENE_ID_COL_NAME,
								ImporterNodeModel.GENE_ANNOTATION_COL_NAME }),
				0, DoubleValue.class);
		final ColumnFilterPanel columnsFilter = (ColumnFilterPanel) parametersDialog
				.getComponentPanel().getComponent(0);
		final JPanel includePanel = (JPanel) columnsFilter.getComponent(1);
		final JList includeList = (JList) ((JScrollPane) includePanel
				.getComponent(1)).getViewport().getView();
		final JPanel center = (JPanel) columnsFilter.getComponent(0);
		final JPanel buttonPanel2 = (JPanel) center.getComponent(1);
		final JPanel buttonPanel = (JPanel) buttonPanel2.getComponent(0);
		final JButton upButton = new JButton("^");
		upButton.setMaximumSize(new Dimension(125, 5));
		buttonPanel.add(upButton);
		final DefaultListModel includeListModel = (DefaultListModel) includeList
				.getModel();
		upButton.addActionListener(new SelectionMoverActionListener(
				includeList, includeListModel, -1));
		buttonPanel.add(new JPanel());

		final JButton downButton = new JButton("v");
		downButton.setMaximumSize(new Dimension(125, 5));
		buttonPanel.add(downButton);
		downButton.addActionListener(new SelectionMoverActionListener(
				includeList, includeListModel, 1));
		buttonPanel.add(new JPanel());

		parametersDialog.setIncludeTitle("Selected parameters for analysis");
		parametersDialog
				.setToolTipText("You may select the parameters to analyse.");
		addDialogComponent(parametersDialog);
		createNewTab("Advanced");
		final DialogComponentDoubleRange scoreRangeDialog = new DialogComponentDoubleRange(
				new SettingsModelDoubleRange(
						CellHTS2NodeModel.CFGKEY_SCORE_RANGE,
						CellHTS2NodeModel.DEFAULT_SCORE_RANGE_MIN,
						CellHTS2NodeModel.DEFAULT_SCORE_RANGE_MAX), -200, 200,
				.1, "Score Range:");
		addDialogComponent(scoreRangeDialog);
		final DialogComponentNumber aspectRationDialog = new DialogComponentNumber(
				new SettingsModelDoubleBounded(
						CellHTS2NodeModel.CFGKEY_ASPECT_RATIO,
						CellHTS2NodeModel.DEFAULT_ASPECT_RATIO, 0.1, 200),
				"Aspect ratio of images", .1);
		aspectRationDialog.getModel().setEnabled(false);
		addDialogComponent(aspectRationDialog);
		includeListModel.addListDataListener(new ListDataListener() {

			@Override
			public void intervalRemoved(final ListDataEvent e) {
				// TODO Auto-generated method stub

			}

			@Override
			public void intervalAdded(final ListDataEvent e) {
				// TODO Auto-generated method stub

			}

			@Override
			public void contentsChanged(final ListDataEvent e) {
				updateSample(sample, experimentTextField,
						normalizationCombobox, isMultiplicativeDialogModel,
						fileChooser, patternCombobox, includeListModel);
			}

		});
		{
			updateSample(sample, experimentTextField, normalizationCombobox,
					isMultiplicativeDialogModel, fileChooser, patternCombobox,
					includeListModel);

		}
		patternCombobox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				updateSample(sample, experimentTextField,
						normalizationCombobox, isMultiplicativeDialogModel,
						fileChooser, patternCombobox, includeListModel);
			}
		});
		normalizationCombobox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				updateSample(sample, experimentTextField,
						normalizationCombobox, isMultiplicativeDialogModel,
						fileChooser, patternCombobox, includeListModel);
			}
		});
		isMultiplicativeDialogModel.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(final ChangeEvent e) {
				updateSample(sample, experimentTextField,
						normalizationCombobox, isMultiplicativeDialogModel,
						fileChooser, patternCombobox, includeListModel);
			}
		});
		experimentTextField.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				updateSample(sample, experimentTextField,
						normalizationCombobox, isMultiplicativeDialogModel,
						fileChooser, patternCombobox, includeListModel);
			}
		});
		fileChooser.getModel().addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(final ChangeEvent e) {
				updateSample(sample, experimentTextField,
						normalizationCombobox, isMultiplicativeDialogModel,
						fileChooser, patternCombobox, includeListModel);
			}
		});
	}

	private void updateSample(final DialogComponentMultiLineString sample,
			final JTextField experimentTextField,
			final JComboBox normalizationCombobox,
			final ButtonModel isMultiplicativeDialogModel,
			final DialogComponentFileChooser fileChooser,
			final JComboBox patternCombobox, final ListModel includeList) {
		final StringBuilder sb = new StringBuilder();
		final String outdirSelected = ((SettingsModelString) fileChooser
				.getModel()).getStringValue();
		final String outdir = outdirSelected == null ? "" : outdirSelected;
		final List<String> paramList = new ArrayList<String>(includeList
				.getSize());
		for (int i = 0; i < includeList.getSize(); ++i) {
			paramList.add(((DataColumnSpec) includeList.getElementAt(i))
					.getName());
		}
		final Map<String, String> dirs = CellHTS2NodeModel.computeOutDirs(
				CellHTS2NodeModel
						.computeNormMethods((String) normalizationCombobox
								.getSelectedItem()), patternCombobox.getModel()
						.getSelectedItem().toString(),
				outdir.endsWith("/") ? outdir : outdir + "/", paramList,
				experimentTextField.getText(), isMultiplicativeDialogModel
						.isSelected());
		for (final String dir : dirs.values()) {
			sb.append(dir.replace('/', File.separatorChar).trim()).append('\n');
		}
		if (sb.length() > 0) {
			sb.setLength(sb.length() - 1);
		}
		((SettingsModelString) sample.getModel()).setStringValue(sb.toString());
	}
}

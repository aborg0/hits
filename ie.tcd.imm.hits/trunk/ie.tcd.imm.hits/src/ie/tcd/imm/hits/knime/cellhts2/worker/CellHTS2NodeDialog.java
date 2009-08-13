/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package ie.tcd.imm.hits.knime.cellhts2.worker;

import ie.tcd.imm.hits.common.PublicConstants;
import ie.tcd.imm.hits.knime.cellhts2.worker.DialogComponentFormula.SettingsWrapper;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;
import javax.swing.JFileChooser;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.knime.core.data.DoubleValue;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentButton;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnFilter;
import org.knime.core.node.defaultnodesettings.DialogComponentDoubleRange;
import org.knime.core.node.defaultnodesettings.DialogComponentFileChooser;
import org.knime.core.node.defaultnodesettings.DialogComponentLabel;
import org.knime.core.node.defaultnodesettings.DialogComponentMultiLineString;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelDoubleBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelDoubleRange;
import org.knime.core.node.defaultnodesettings.SettingsModelFilterString;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.rosuda.REngine.REXPLogical;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;

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

	private final DialogComponentFormula normFormula;
	private final DialogComponentFormula summariseFormula;
	private final DialogComponentFormula scoreFormula;

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
		experimentDialog
				.setToolTipText("The short really description of the experiment (should not contain tab characters)");
		addDialogComponent(experimentDialog);
		createNewGroup("Normalization parameters");
		final DialogComponentStringSelection normalizationDialog = new DialogComponentStringSelection(
				new SettingsModelString(
						CellHTS2NodeModel.CFGKEY_NORMALISATION_METHOD,
						CellHTS2NodeModel.POSSIBLE_NORMALISATION_METHODS[1]),
				"Normalisation method: ",
				checkLocFit() ? checkExpert() ? CellHTS2NodeModel.POSSIBLE_NORMALISATION_METHODS_LOCFIT_EXPERIMENTAL
						: CellHTS2NodeModel.POSSIBLE_NORMALISATION_METHODS_LOCFIT
						: checkExpert() ? CellHTS2NodeModel.POSSIBLE_NORMALISATION_METHODS_EXPERIMENTAL
								: CellHTS2NodeModel.POSSIBLE_NORMALISATION_METHODS);
		normalizationDialog
				.setToolTipText("The normalization method for the parameters. (For \"Z score\" select mean or median, and also some kind of variance adjust.)");
		addDialogComponent(normalizationDialog);
		setHorizontalPlacement(true);
		final DialogComponentBoolean isMultiplicativeDialog = new DialogComponentBoolean(
				new SettingsModelBoolean(
						CellHTS2NodeModel.CFGKEY_IS_MULTIPLICATIVE_NORMALISATION,
						CellHTS2NodeModel.DEFAULT_IS_MULTIPLICATIVE_NORMALISATION),
				"multiplicative?");
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
		setHorizontalPlacement(true);
		varianceScaleDialog
				.setToolTipText("Plate-specific variance adjustment");
		addDialogComponent(varianceScaleDialog);
		final DialogComponentLabel normHelpDialog = new DialogComponentLabel(
				"Formula overview");
		addDialogComponent(normHelpDialog);
		closeCurrentGroup();
		createNewGroup("Replicate summary");
		setHorizontalPlacement(true);
		final DialogComponentStringSelection scoreDialog = new DialogComponentStringSelection(
				new SettingsModelString(CellHTS2NodeModel.CFGKEY_SCORE,
						CellHTS2NodeModel.POSSIBLE_SCORE[1]), "Score: ",
				checkExpert() ? CellHTS2NodeModel.POSSIBLE_SCORE_EXPERIMENTAL
						: CellHTS2NodeModel.POSSIBLE_SCORE);
		scoreDialog.setToolTipText("Scoring of replicates.");
		addDialogComponent(scoreDialog);
		final DialogComponentLabel scoreHelpDialog = new DialogComponentLabel(
				"Formula overview");
		addDialogComponent(scoreHelpDialog);
		setHorizontalPlacement(false);
		setHorizontalPlacement(true);
		final DialogComponentStringSelection summarizeReplicatesDialog = new DialogComponentStringSelection(
				new SettingsModelString(CellHTS2NodeModel.CFGKEY_SUMMARISE,
						CellHTS2NodeModel.POSSIBLE_SUMMARISE[0]),
				"Summarise replicates with: ",
				CellHTS2NodeModel.POSSIBLE_SUMMARISE);
		summarizeReplicatesDialog
				.setToolTipText("Summarize the replicates with this method.");
		addDialogComponent(summarizeReplicatesDialog);
		final DialogComponentLabel summariseHelpDialog = new DialogComponentLabel(
				"Formula overview");
		addDialogComponent(summariseHelpDialog);
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
		final DialogComponentString patternDialog = new DialogComponentString(
				new SettingsModelString(
						CellHTS2NodeModel.CFGKEY_FOLDER_PATTERN,
						CellHTS2NodeModel.DEFAULT_FOLDER_PATTERN), "Pattern:");
		addDialogComponent(patternDialog);
		final DialogComponentButton helpButton = new DialogComponentButton(
				"Help");
		helpButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				final Display display = Display.getDefault();
				display.asyncExec(new Runnable() {
					@Override
					public void run() {
						PlatformUI
								.getWorkbench()
								.getHelpSystem()
								.displayHelpResource(
										"/ie.tcd.imm.hits/help/cellHTS2.xhtml#params");
					}
				});
			}
		});
		addDialogComponent(helpButton);
		setHorizontalPlacement(false);
		final DialogComponentBoolean generateHTML = new DialogComponentBoolean(
				new SettingsModelBoolean(
						CellHTS2NodeModel.CFGKEY_GENERATE_HTML,
						CellHTS2NodeModel.DEFAULT_GENERATE_HTML),
				"Generate HTML reports?");
		generateHTML
				.setToolTipText("Generate HTML report increase the time to generate results.");
		generateHTML.getComponentPanel().setBackground(Color.CYAN);
		addDialogComponent(generateHTML);
		addDialogComponent(sample);
		closeCurrentGroup();
		createNewTab("Parameters");
		@SuppressWarnings("unchecked")
		final DialogComponentColumnFilter parametersDialog = new DialogComponentColumnFilter(
				new SettingsModelFilterString(
						CellHTS2NodeModel.CFGKEY_PARAMETERS, new String[0],
						new String[0]), 0, DoubleValue.class);
		((SettingsModelFilterString) parametersDialog.getModel())
				.setExcludeList(new String[] { PublicConstants.PLATE_COL_NAME,
						PublicConstants.REPLICATE_COL_NAME,
						PublicConstants.WELL_COL_NAME,
						PublicConstants.GENE_ID_COL_NAME,
						PublicConstants.GENE_ANNOTATION_COL_NAME });
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
				.1, "Score range:");
		addDialogComponent(scoreRangeDialog);
		final DialogComponentNumber aspectRationDialog = new DialogComponentNumber(
				new SettingsModelDoubleBounded(
						CellHTS2NodeModel.CFGKEY_ASPECT_RATIO,
						CellHTS2NodeModel.DEFAULT_ASPECT_RATIO, 0.1, 200),
				"Aspect ratio of images", .1);
		aspectRationDialog.getModel().setEnabled(false);
		addDialogComponent(aspectRationDialog);
		createNewTab("Help");
		normFormula = new DialogComponentFormula(new SettingsWrapper(
				normalizationDialog.getModel(), isMultiplicativeDialog
						.getModel(), logDialog.getModel(), varianceScaleDialog
						.getModel()));
		normFormula.setHelpComponent(normHelpDialog);
		addDialogComponent(normFormula);
		scoreFormula = new DialogComponentFormula(new SettingsWrapper(
				scoreDialog.getModel()));
		scoreFormula.setHelpComponent(scoreHelpDialog);
		addDialogComponent(scoreFormula);
		summariseFormula = new DialogComponentFormula(new SettingsWrapper(
				summarizeReplicatesDialog.getModel()));
		summariseFormula.setHelpComponent(summariseHelpDialog);
		addDialogComponent(summariseFormula);
		parametersDialog.getModel().addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(final ChangeEvent e) {
				updateSample(sample, experimentDialog, normalizationDialog,
						(SettingsModelBoolean) isMultiplicativeDialog
								.getModel(), fileChooser, patternDialog,
						parametersDialog, varianceScaleDialog, scoreDialog,
						summarizeReplicatesDialog, logDialog);
			}
		});
		{
			updateSample(sample, experimentDialog, normalizationDialog,
					(SettingsModelBoolean) isMultiplicativeDialog.getModel(),
					fileChooser, patternDialog, parametersDialog,
					varianceScaleDialog, scoreDialog,
					summarizeReplicatesDialog, logDialog);
		}
		patternDialog.getModel().addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(final ChangeEvent e) {
				updateSample(sample, experimentDialog, normalizationDialog,
						(SettingsModelBoolean) isMultiplicativeDialog
								.getModel(), fileChooser, patternDialog,
						parametersDialog, varianceScaleDialog, scoreDialog,
						summarizeReplicatesDialog, logDialog);
			}
		});
		normalizationDialog.getModel().addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(final ChangeEvent e) {
				updateSample(sample, experimentDialog, normalizationDialog,
						(SettingsModelBoolean) isMultiplicativeDialog
								.getModel(), fileChooser, patternDialog,
						parametersDialog, varianceScaleDialog, scoreDialog,
						summarizeReplicatesDialog, logDialog);
			}
		});
		isMultiplicativeDialog.getModel().addChangeListener(
				new ChangeListener() {
					@Override
					public void stateChanged(final ChangeEvent e) {
						updateSample(sample, experimentDialog,
								normalizationDialog,
								(SettingsModelBoolean) isMultiplicativeDialog
										.getModel(), fileChooser,
								patternDialog, parametersDialog,
								varianceScaleDialog, scoreDialog,
								summarizeReplicatesDialog, logDialog);
					}
				});
		experimentDialog.getModel().addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(final ChangeEvent e) {
				updateSample(sample, experimentDialog, normalizationDialog,
						(SettingsModelBoolean) isMultiplicativeDialog
								.getModel(), fileChooser, patternDialog,
						parametersDialog, varianceScaleDialog, scoreDialog,
						summarizeReplicatesDialog, logDialog);
			}
		});
		fileChooser.getModel().addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(final ChangeEvent e) {
				updateSample(sample, experimentDialog, normalizationDialog,
						(SettingsModelBoolean) isMultiplicativeDialog
								.getModel(), fileChooser, patternDialog,
						parametersDialog, varianceScaleDialog, scoreDialog,
						summarizeReplicatesDialog, logDialog);
			}
		});
		logDialog.getModel().addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(final ChangeEvent e) {
				if (((SettingsModelBoolean) logDialog.getModel())
						.getBooleanValue()) {
					isMultiplicativeDialog.getModel().setEnabled(false);
					((SettingsModelBoolean) isMultiplicativeDialog.getModel())
							.setBooleanValue(true);
				} else {
					isMultiplicativeDialog.getModel().setEnabled(true);
				}
			}
		});
	}

	private static boolean checkLocFit() {
		try {

			final RConnection conn;
			conn = new RConnection(/* "127.0.0.1", 1099, 10000 */);
			try {
				if (conn.isConnected()) {
					final REXPLogical hasLocfit = (REXPLogical) conn
							.eval("require('locfit')");
					return hasLocfit.isTrue()[0];
				}
			} finally {
				conn.close();
			}
		} catch (final RserveException e) {
			return false;
		}
		return false;
	}

	private static boolean checkExpert() {
		return Boolean.parseBoolean(System
				.getProperty(CellHTS2NodeModel.PROPERTY_EXPERT));
	}

	private void updateSample(final DialogComponentMultiLineString sample,
			final DialogComponentString experimentDialog,
			final DialogComponentStringSelection normalizationDialog,
			final SettingsModelBoolean isMultiplicativeDialogModel,
			final DialogComponentFileChooser fileChooser,
			final DialogComponentString patternDialog,
			final DialogComponentColumnFilter parametersDialog,
			final DialogComponentStringSelection varianceAdjustment,
			final DialogComponentStringSelection scoreDialog,
			final DialogComponentStringSelection summariseReplicatesDialog,
			final DialogComponentBoolean logDialog) {
		final StringBuilder sb = new StringBuilder();
		final String outdirSelected = ((SettingsModelString) fileChooser
				.getModel()).getStringValue();
		final String outdir = outdirSelected == null ? "" : outdirSelected;
		final SettingsModelFilterString parametersModel = (SettingsModelFilterString) parametersDialog
				.getModel();
		final List<String> paramList = new ArrayList<String>(parametersModel
				.getIncludeList().size());
		for (final String param : parametersModel.getIncludeList()) {
			paramList.add(param);
		}
		final Map<String, String> dirs = CellHTS2NodeModel
				.computeOutDirs(
						CellHTS2NodeModel
								.computeNormMethods(((SettingsModelString) normalizationDialog
										.getModel()).getStringValue()),
						((SettingsModelString) patternDialog.getModel())
								.getStringValue(),
						outdir.endsWith("/") ? outdir : outdir + "/",
						paramList, ((SettingsModelString) experimentDialog
								.getModel()).getStringValue(),
						isMultiplicativeDialogModel.getBooleanValue(),
						((SettingsModelString) varianceAdjustment.getModel())
								.getStringValue(),
						((SettingsModelString) scoreDialog.getModel())
								.getStringValue(),
						((SettingsModelString) summariseReplicatesDialog
								.getModel()).getStringValue(),
						((SettingsModelBoolean) logDialog.getModel())
								.getBooleanValue() ? "log" : "");
		for (final String dir : dirs.values()) {
			sb.append(dir.replace('/', File.separatorChar).trim()).append('\n');
		}
		if (sb.length() > 0) {
			sb.setLength(sb.length() - 1);
		}
		((SettingsModelString) sample.getModel()).setStringValue(sb.toString());
		normFormula.updateComponent();
		summariseFormula.updateComponent();
		scoreFormula.updateComponent();
	}
}

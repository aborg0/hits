/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package com.mind_era.knime.hits.xls;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;
import javax.swing.JFileChooser;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentFileChooser;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.defaultnodesettings.SettingsModelStringArray;

import com.mind_era.knime.common.util.DialogComponentMultiFileChooser;

/**
 * <code>NodeDialog</code> for the "Importer" Node. Reads the data from xls
 * files
 * 
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more
 * complex dialog please derive directly from
 * {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
@NotThreadSafe
@Nonnull
public class ImporterNodeDialog extends DefaultNodeSettingsPane {

	/** Default directory for the xls files. */
	static final String DEFAULT_DIR = System.getProperty("user.home");

	/**
	 * New pane for configuring Importer node dialog. This is just a suggestion
	 * to demonstrate possible default dialog components.
	 */
	protected ImporterNodeDialog() {
		super();

		final SettingsModelStringArray files = new SettingsModelStringArray(
				ImporterNodeModel.CFGKEY_FILES, new String[] {});
		final DialogComponentMultiFileChooser fileChooser = new DialogComponentMultiFileChooser(
				files, "XLS files", ImporterNodeModel.CFGKEY_FILES, 12, ".xls");
		fileChooser.setBorderTitle("Folder of xls files:");
		fileChooser
				.setToolTipText("Select the folder of the xls files from IN Cell Analyser 1000 outputs.");
		createNewGroup("Data files");
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
		final DialogComponentFileChooser annotFileChooser = new DialogComponentFileChooser(
				new SettingsModelString(
						ImporterNodeModel.CFGKEY_ANNOTATION_FILE,
						ImporterNodeModel.DEFAULT_ANNOTATION_FILE),
				ImporterNodeModel.CFGKEY_ANNOTATION_FILE,
				JFileChooser.OPEN_DIALOG, false, ".txt", ".TXT") {
			@Override
			protected void validateSettingsBeforeSave()
					throws InvalidSettingsException {
				try {
					super.validateSettingsBeforeSave();
				} catch (final InvalidSettingsException e) {
					if (!"Please specify a filename.".equals(e.getMessage())) {
						throw e;
					}
					// else OK, we allow empty file names.
				}
			}
		};
		annotFileChooser.setBorderTitle("Annotation file");
		annotFileChooser.setToolTipText("The file containing the annotations.");
		addDialogComponent(annotFileChooser);
		// final DialogComponentBoolean addAnnotationsDialog = new
		// DialogComponentBoolean(
		// new SettingsModelBoolean(
		// ImporterNodeModel.CFGKEY_COMBINE_ANNOTATIONS,
		// ImporterNodeModel.DEFAULT_COMBINE_ANNOTATIONS),
		// "Put annotations to the main output?");
		// addAnnotationsDialog
		// .setToolTipText("Add the gene annotations to the main outport too if
		// checked.");
		// addDialogComponent(addAnnotationsDialog);
		closeCurrentGroup();
	}
}

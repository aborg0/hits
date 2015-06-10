/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package ie.tcd.imm.hits.knime.cellhts2.prefs;

import ie.tcd.imm.hits.common.PossibleStatistics;
import ie.tcd.imm.hits.knime.cellhts2.prefs.ui.ColumnSelectionFieldEditor;
import ie.tcd.imm.hits.knime.xls.ImporterNodePlugin;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;

/**
 * Class used to initialize default preference values.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
public class PreferenceInitializer extends AbstractPreferenceInitializer {

	/*
	 * (non-Javadoc)
	 * 
	 * @seeorg.eclipse.core.runtime.preferences.AbstractPreferenceInitializer#
	 * initializeDefaultPreferences()
	 */
	public void initializeDefaultPreferences() {
		final IPreferenceStore store = ImporterNodePlugin.getDefault()
				.getPreferenceStore();
		store
				.setDefault(
						PreferenceConstants.RESULT_COL_ORDER,
						ColumnSelectionFieldEditor
								.createList(
										PossibleStatistics.PLATE,
										PossibleStatistics.POSITION,
										PossibleStatistics.WELL,
										PossibleStatistics.REPLICATE,
										PossibleStatistics.GROUP_BY_CHANNELS_START,
										PossibleStatistics.FINAL_WELL_ANNOTATION,
										PossibleStatistics.GROUP_BY_CHANNELS_END,
										PossibleStatistics.GROUP_BY_CHANNELS_START,
										PossibleStatistics.SCORE,
										PossibleStatistics.GROUP_BY_CHANNELS_END,
										PossibleStatistics.GENE_ID,
										PossibleStatistics.GENE_SYMBOL,
										PossibleStatistics.GENE_ANNOTATION,
										PossibleStatistics.GROUP_BY_REPLICATES_START,
										PossibleStatistics.GROUP_BY_CHANNELS_START,
										PossibleStatistics.RAW,
										PossibleStatistics.GROUP_BY_CHANNELS_END,
										PossibleStatistics.GROUP_BY_REPLICATES_END,
										PossibleStatistics.GROUP_BY_CHANNELS_START,
										PossibleStatistics.MEDIAN,
										PossibleStatistics.MEAN_OR_DIFF,
										PossibleStatistics.GROUP_BY_CHANNELS_END,
										PossibleStatistics.GROUP_BY_REPLICATES_START,
										PossibleStatistics.GROUP_BY_CHANNELS_START,
										PossibleStatistics.RAW_PER_PLATE_REPLICATE_MEAN,
										PossibleStatistics.GROUP_BY_CHANNELS_END,
										PossibleStatistics.GROUP_BY_REPLICATES_END,
										PossibleStatistics.GROUP_BY_REPLICATES_START,
										PossibleStatistics.GROUP_BY_CHANNELS_START,
										PossibleStatistics.NORMALISED,
										PossibleStatistics.GROUP_BY_CHANNELS_END,
										PossibleStatistics.GROUP_BY_REPLICATES_END));
		store.setDefault(PreferenceConstants.USE_TCD_EXTENSIONS, true);
		store.setDefault(PreferenceConstants.USE_NAMES_INSTEAD_OF_CHANNELS,
				true);
	}
}

package ie.tcd.imm.hits.knime.cellhts2.prefs;

import ie.tcd.imm.hits.knime.cellhts2.prefs.ui.ColumnSelectionFieldEditor;
import ie.tcd.imm.hits.knime.xls.ImporterNodePlugin;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;

/**
 * Class used to initialize default preference values.
 */
public class PreferenceInitializer extends AbstractPreferenceInitializer {

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer#initializeDefaultPreferences()
	 */
	public void initializeDefaultPreferences() {
		final IPreferenceStore store = ImporterNodePlugin.getDefault()
				.getPreferenceStore();
		store
				.setDefault(
						PreferenceConstants.RESULT_COL_ORDER,
						ColumnSelectionFieldEditor
								.createList(
										PreferenceConstants.PossibleStatistics.PLATE,
										PreferenceConstants.PossibleStatistics.POSITION,
										PreferenceConstants.PossibleStatistics.WELL,
										PreferenceConstants.PossibleStatistics.REPLICATE,
										PreferenceConstants.PossibleStatistics.GROUP_BY_REPLICATES_START,
										PreferenceConstants.PossibleStatistics.FINAL_WELL_ANNOTATION,
										PreferenceConstants.PossibleStatistics.GROUP_BY_REPLICATES_END,
										PreferenceConstants.PossibleStatistics.GROUP_BY_CHANNELS_START,
										PreferenceConstants.PossibleStatistics.SCORE,
										PreferenceConstants.PossibleStatistics.GROUP_BY_CHANNELS_END,
										PreferenceConstants.PossibleStatistics.GENE_ID,
										PreferenceConstants.PossibleStatistics.GENE_SYMBOL,
										PreferenceConstants.PossibleStatistics.GENE_ANNOTATION,
										PreferenceConstants.PossibleStatistics.GROUP_BY_REPLICATES_START,
										PreferenceConstants.PossibleStatistics.GROUP_BY_CHANNELS_START,
										PreferenceConstants.PossibleStatistics.RAW,
										PreferenceConstants.PossibleStatistics.GROUP_BY_CHANNELS_END,
										PreferenceConstants.PossibleStatistics.GROUP_BY_REPLICATES_END,
										PreferenceConstants.PossibleStatistics.GROUP_BY_CHANNELS_START,
										PreferenceConstants.PossibleStatistics.MEDIAN,
										PreferenceConstants.PossibleStatistics.MEAN_OR_DIFF,
										PreferenceConstants.PossibleStatistics.GROUP_BY_CHANNELS_END,
										PreferenceConstants.PossibleStatistics.GROUP_BY_REPLICATES_START,
										PreferenceConstants.PossibleStatistics.GROUP_BY_CHANNELS_START,
										PreferenceConstants.PossibleStatistics.RAW_PER_PLATE_REPLICATE_MEAN,
										PreferenceConstants.PossibleStatistics.GROUP_BY_CHANNELS_END,
										PreferenceConstants.PossibleStatistics.GROUP_BY_REPLICATES_END,
										PreferenceConstants.PossibleStatistics.GROUP_BY_REPLICATES_START,
										PreferenceConstants.PossibleStatistics.GROUP_BY_CHANNELS_START,
										PreferenceConstants.PossibleStatistics.NORMALIZED,
										PreferenceConstants.PossibleStatistics.GROUP_BY_CHANNELS_END,
										PreferenceConstants.PossibleStatistics.GROUP_BY_REPLICATES_END));
		store.setDefault(PreferenceConstants.USE_TCD_EXTENSIONS, true);
		store.setDefault(PreferenceConstants.USE_NAMES_INSTEAD_OF_CHANNELS,
				true);
	}
}

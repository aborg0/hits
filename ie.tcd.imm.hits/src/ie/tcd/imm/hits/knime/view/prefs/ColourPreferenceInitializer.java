/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package ie.tcd.imm.hits.knime.view.prefs;

import ie.tcd.imm.hits.knime.xls.ImporterNodePlugin;
import ie.tcd.imm.hits.util.swing.colour.ColourSelector.RangeType;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;

import edu.umd.cs.findbugs.annotations.DefaultAnnotation;

/**
 * Class used to initialise the colour related preference values.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
@DefaultAnnotation( { Nonnull.class, CheckReturnValue.class })
public class ColourPreferenceInitializer extends AbstractPreferenceInitializer {

	/**
	 * 
	 */
	public ColourPreferenceInitializer() {
		super();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seeorg.eclipse.core.runtime.preferences.AbstractPreferenceInitializer#
	 * initializeDefaultPreferences()
	 */
	@Override
	public void initializeDefaultPreferences() {
		final IPreferenceStore store = ImporterNodePlugin.getDefault()
				.getPreferenceStore();
		store.setDefault(ColourPreferenceConstants.UP_COLOUR, "255,0,0");
		store.setDefault(ColourPreferenceConstants.UP_VALUE, RangeType.max
				.getDisplayText());
		store
				.setDefault(ColourPreferenceConstants.MIDDLE_COLOUR,
						"255,255,128");
		store.setDefault(ColourPreferenceConstants.MIDDLE_VALUE,
				RangeType.median.getDisplayText());
		store.setDefault(ColourPreferenceConstants.DOWN_COLOUR, "0,255,0");
		store.setDefault(ColourPreferenceConstants.DOWN_VALUE, RangeType.min
				.getDisplayText());
	}
}

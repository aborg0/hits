/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package com.mind_era.knime.hits.cellhts2.prefs;


/**
 * Constant definitions for plug-in preferences
 */
public class PreferenceConstants {
	/** Configuration key for the column order property. */
	public static final String RESULT_COL_ORDER = "com.mind_era.knime.hits.resultColumnOrder";

	/** Key for usage of the TCD cellHTS2 extensions preference. */
	public static final String USE_TCD_EXTENSIONS = "com.mind_era.knime.hits.useTCDExtensions";

	/**
	 * Key for usage of the channel names instead of the original ch_n names.
	 * (Should be available only if used with TCD extensions.)
	 */
	public static final String USE_NAMES_INSTEAD_OF_CHANNELS = "com.mind_era.knime.hits.namesInsteadOfChannels";
}

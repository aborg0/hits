/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package ie.tcd.imm.hits.knime.cellhts2.prefs;


/**
 * Constant definitions for plug-in preferences
 */
public class PreferenceConstants {
	/** Configuration key for the column order property. */
	public static final String RESULT_COL_ORDER = "ie.tcd.imm.hits.knime.resultColumnOrder";

	/** Key for usage of the TCD cellHTS2 extensions preference. */
	public static final String USE_TCD_EXTENSIONS = "ie.tcd.imm.hits.knime.useTCDExtensions";

	/**
	 * Key for usage of the channel names instead of the original ch_n names.
	 * (Should be available only if used with TCD extensions.)
	 */
	public static final String USE_NAMES_INSTEAD_OF_CHANNELS = "ie.tcd.imm.hits.knime.namesInsteadOfChannels";
}

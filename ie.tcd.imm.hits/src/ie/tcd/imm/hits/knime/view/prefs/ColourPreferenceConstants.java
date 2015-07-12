/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package ie.tcd.imm.hits.knime.view.prefs;

import ie.tcd.imm.hits.util.swing.colour.ColourSelector.RangeType;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import edu.umd.cs.findbugs.annotations.DefaultAnnotation;

/**
 * The constants for the colour preferences.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
@DefaultAnnotation( { Nonnull.class, CheckReturnValue.class })
public class ColourPreferenceConstants {
	/** Key for colour of high colour values. */
	public static final String UP_COLOUR = "ie.tcd.imm.hits.knime.view.prefs.up.colour";
	/** Key for colour of medium colour values. */
	public static final String MIDDLE_COLOUR = "ie.tcd.imm.hits.knime.view.prefs.middle.colour";
	/** Key for colour of low colour values. */
	public static final String DOWN_COLOUR = "ie.tcd.imm.hits.knime.view.prefs.down.colour";

	/** Key for high values. */
	public static final String UP_VALUE = "ie.tcd.imm.hits.knime.view.prefs.up.value";
	/** Key for medium values. */
	public static final String MIDDLE_VALUE = "ie.tcd.imm.hits.knime.view.prefs.middle.value";
	/** Key for low values. */
	public static final String DOWN_VALUE = "ie.tcd.imm.hits.knime.view.prefs.down.value";
	/** The options for the up value. */
	static final RangeType[] DEFAULT_UP_OPTIONS = new RangeType[] { RangeType.max };
	/** The options for the medium value. */
	static final RangeType[] DEFAULT_MIDDLE_OPTIONS = new RangeType[] { RangeType.median };
	/** The options for the low value. */
	static final RangeType[] DEFAULT_DOWN_OPTIONS = new RangeType[] { RangeType.min };
}

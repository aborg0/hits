/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package ie.tcd.imm.hits.util;

import ie.tcd.imm.hits.util.swing.colour.ColourSelector.RangeType;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.nfunk.jep.JEP;

import edu.umd.cs.findbugs.annotations.DefaultAnnotation;

/**
 * Some utility methods to use {@link JEP}.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
@DefaultAnnotation( { Nonnull.class, CheckReturnValue.class })
public class JEPHelper {
	private JEPHelper() {
		super();
	}

	/**
	 * Adds the constants to {@code jep} with the proper {@code val} value.
	 * 
	 * @param jep
	 *            A {@link JEP}.
	 * @param type
	 *            The {@link RangeType}.
	 * @param val
	 *            The value to associate.
	 */
	public static void addConstant(final JEP jep, final RangeType type,
			final Double val) {
		switch (type) {
		case average:
			addCaseInsensitiveConstant(jep, "avg", val);
			addCaseInsensitiveConstant(jep, "average", val);
			addCaseInsensitiveConstant(jep, "mean", val);
			addCaseInsensitiveConstant(jep, "\u03bc", val);
			break;
		case median:
			addCaseInsensitiveConstant(jep, "median", val);
			break;
		case min:
			addCaseInsensitiveConstant(jep, "min", val);
			addCaseInsensitiveConstant(jep, "minimum", val);
			break;
		case max:
			addCaseInsensitiveConstant(jep, "max", val);
			addCaseInsensitiveConstant(jep, "maximum", val);
			break;
		case mad:
			addCaseInsensitiveConstant(jep, "mad", val);
			addCaseInsensitiveConstant(jep, "medianabsolutedeviation", val);
			break;
		case stdev:
			addCaseInsensitiveConstant(jep, "sd", val);
			addCaseInsensitiveConstant(jep, "stdev", val);
			addCaseInsensitiveConstant(jep, "standarddeviation", val);
			addCaseInsensitiveConstant(jep, "\u03c3", val);
			break;
		case q1:
			addCaseInsensitiveConstant(jep, "q1", val);
			break;
		case q3:
			addCaseInsensitiveConstant(jep, "q3", val);
			break;
		case iqr:
			addCaseInsensitiveConstant(jep, "iqr", val);
			break;
		default:
			throw new UnsupportedOperationException("Not supported yet: "
					+ type);
		}
	}

	/**
	 * Adds a constant with both uppercase and lowercase name.
	 * 
	 * @param jep
	 *            A {@link JEP}.
	 * @param string
	 *            A name to add.
	 * @param val
	 *            The value.
	 */
	private static void addCaseInsensitiveConstant(final JEP jep,
			final String string, final Double val) {
		jep.addConstant(string.toLowerCase(), val);
		jep.addConstant(string.toUpperCase(), val);
	}

}

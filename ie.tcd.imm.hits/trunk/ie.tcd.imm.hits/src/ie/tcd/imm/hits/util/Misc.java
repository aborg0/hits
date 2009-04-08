/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package ie.tcd.imm.hits.util;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;

import edu.umd.cs.findbugs.annotations.DefaultAnnotation;

/**
 * Some always useful methods.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
@DefaultAnnotation( { Nonnull.class, CheckReturnValue.class })
public class Misc {
	private Misc() {
		super();
	}

	/**
	 * Adds trailing zeroes to get at least {@code minDigits} digits in the
	 * result (sign does not count).
	 * 
	 * @param num
	 *            An integer number.
	 * @param minDigits
	 *            A non-negative number of the minimal digits to return.
	 * @return A {@link String} of {@code num} with at least {@code minDigits}
	 *         digits (except if {@code num} is {@link Integer#MIN_VALUE} and
	 *         {@code minDigits} is more than its digits).
	 */
	public static String addTrailing(final int num,
			@Nonnegative final int minDigits) {
		if (num < 0 && num != Integer.MIN_VALUE) {
			return "-" + addTrailing(-num, minDigits);
		}
		final String str = String.valueOf(num);
		if (str.length() >= minDigits) {
			return str;
		}
		final StringBuilder sb = new StringBuilder(minDigits);
		sb.setLength(minDigits);
		for (int i = minDigits - str.length(); i-- > 0;) {
			sb.setCharAt(i, '0');
		}
		for (int i = str.length(); i-- > 0;) {
			sb.setCharAt(i + minDigits - str.length(), str.charAt(i));
		}
		return sb.toString();
	}

	/**
	 * Rounds the {@code val} to minimise have an easier to read representation
	 * of if.
	 * 
	 * @param val
	 *            A finite double value.
	 * @return The rounded value as {@link String}.
	 */
	public static String round(final double val) {
		return Math.abs(val) > 100 ? Long.toString(Math.round(val)) : Math
				.abs(val) > 10 ? Double.toString(Math.round(val * 10) / 10.0)
				: Double.toString(Math.round(val * 100) / 100.0);
	}
}

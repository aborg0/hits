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

	/**
	 * Converts the integer value represented by {@code val} to an uppercase
	 * letter.
	 * 
	 * @param val
	 *            An integer number as {@link String}. (Between {@code 1} and
	 *            {@code 32}.)
	 * @return A single letter {@link String}. For {@code 1}, it returns {@code
	 *         A}.
	 * @throws NumberFormatException
	 *             The {@code val} is not a number.
	 * @throws NullPointerException
	 *             The {@code val} is {@code null}.
	 * @throws IllegalArgumentException
	 *             The {@code val} is out of accepted range.
	 */
	public static String toUpperLetter(final String val) {
		return toLetter(val, true);
	}

	/**
	 * Converts the integer value represented by {@code val} to a lowercase
	 * letter.
	 * 
	 * @param val
	 *            An integer number as {@link String}. (Between {@code 1} and
	 *            {@code 32}.)
	 * @return A single letter {@link String}. For {@code 1}, it returns {@code
	 *         a}.
	 * @throws NumberFormatException
	 *             The {@code val} is not a number.
	 * @throws NullPointerException
	 *             The {@code val} is {@code null}.
	 * @throws IllegalArgumentException
	 *             The {@code val} is out of accepted range.
	 */
	public static String toLowerLetter(final String val) {
		return toLetter(val, false);
	}

	/**
	 * Converts the integer value represented by {@code val} to an uppercase or
	 * a lowercase letter depending on the value of {@code upper}.
	 * 
	 * @param val
	 *            An integer number as {@link String}. (Between {@code 1} and
	 *            {@code 32}, inclusive.)
	 * @param upper
	 *            If {@code true} the returned letter is uppercase.
	 * @return A single letter {@link String}. For {@code 1}, it returns {@code
	 *         a}.
	 * @throws NumberFormatException
	 *             The {@code val} is not a number.
	 * @throws NullPointerException
	 *             The {@code val} is {@code null}.
	 * @throws IllegalArgumentException
	 *             The {@code val} is out of accepted range.
	 */
	public static String toLetter(final String val, final boolean upper) {
		final int v = Integer.parseInt(val.trim());
		if (v <= 0 || v > 32) {
			throw new IllegalArgumentException("Wrong number: " + val);
		}
		return v > 26 ? String.valueOf(new char[] {
				singleToLetter(upper, (v - 1) / 26),
				singleToLetter(upper, (v - 1) % 26 + 1) }) : String
				.valueOf(singleToLetter(upper, v));
	}

	/**
	 * Converts a number to a single letter.
	 * 
	 * @param upper
	 *            The result is uppercase or lowercase.
	 * @param v
	 *            The number to convert. ({@code 1} is {@code A} or {@code a})
	 * @return A letter from {@code A} or {@code a} to {@code Z} or {@code z}.
	 */
	private static char singleToLetter(final boolean upper, final int v) {
		return (char) ((upper ? 'A' : 'a') + v - 1);
	}

	/**
	 * Converts {@code letter} to an integer number. For {@code a}, or {@code A}
	 * it will return {@code 1}.
	 * 
	 * @param letter
	 *            A {@link String} containing a single letter.
	 * @return An integer represented as {@link String}, from {@code letter},
	 *         where {@code a}, {@code A} are {@code 0}. ({@code a} is {@code 1}
	 *         )
	 */
	public static String toNumber(final String letter) {
		final String trimmed = letter.trim();
		switch (trimmed.length()) {
		case 1:
			return String.valueOf(singleToNumber(trimmed.charAt(0)));
		case 2:
			return String.valueOf(singleToNumber(trimmed.charAt(0)) * 26
					+ singleToNumber(trimmed.charAt(1)));
		default:
			throw new IllegalArgumentException(
					"Only single letters accepted: \"" + letter + "\"");
		}
	}

	/**
	 * Converts a letter to a number.
	 * 
	 * @param c
	 *            A letter between {@code A} and {@code Z}, or {@code a} and
	 *            {@code z}.
	 * @return The index of letter. ({@code A}, or {@code a} is {@code 1})
	 */
	private static int singleToNumber(final char c) {
		return Character.toLowerCase(c) - 'a' + 1;
	}

	/**
	 * Increases {@code num} by {@code 1}.
	 * 
	 * @param num
	 *            A {@link String} representing an integer number.
	 * @return The increased number as a {@link String}.
	 * @throws NumberFormatException
	 *             The {@code num} is not a number.
	 * @throws NullPointerException
	 *             The {@code num} is {@code null}.
	 */
	public static String inc(final String num) {
		return inc(num, 1);
	}

	/**
	 * Decreases {@code num} by {@code 1}.
	 * 
	 * @param num
	 *            A {@link String} representing an integer number.
	 * @return The decreased number as a {@link String}.
	 * @throws NumberFormatException
	 *             The {@code num} is not a number.
	 * @throws NullPointerException
	 *             The {@code num} is {@code null}.
	 */
	public static String dec(final String num) {
		return inc(num, -1);
	}

	/**
	 * Increases {@code num} by {@code change}.
	 * 
	 * @param num
	 *            A {@link String} representing an integer number.
	 * @param change
	 *            The amount of change.
	 * @return The increased number as a {@link String}.
	 * @throws NumberFormatException
	 *             The {@code num} is not a number.
	 * @throws NullPointerException
	 *             The {@code num} is {@code null}.
	 */
	public static String inc(final String num, final int change) {
		final int v = Integer.parseInt(num);
		return String.valueOf(v + change);
	}

	/**
	 * Removes the invalid characters and replaces them with {@code _}
	 * 
	 * @param string
	 *            A non-{@code null}{@link String}.
	 * @return The new {@link String} without invalid parameters.
	 */
	public static String convertToFileName(final String string) {
		return string.replaceAll("[/\\\\\u0000-\u001f]", "_");
	}
}

/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package ie.tcd.imm.hits.knime.util;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

/**
 * An annotation that describes the possible behaviours of HiLites when the
 * generated rows are not in a one-to-(at most) one relationship.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
@Nonnull
@CheckReturnValue
public enum HiLite {
	/** HiLite is propagated only if all of the generated are selected */
	OnlyIfAllSelected("HiLite if all selected in a group"),
	/** HiLite is propagated if at least half of the generated rows are selected */
	AtLeastHalf("At least semi selection"),
	/** HiLite is propagated if at least on of the generated rows are selected */
	AtLeastOne("Any selection"),
	/** HiLite is not propagated. */
	NoHiLite("No HiLite support");

	private final String displayText;

	private static final Map<String, HiLite> reverse = new LinkedHashMap<String, HiLite>();

	private HiLite(final String displayText) {
		this.displayText = displayText;
	}

	/**
	 * @return The text to display as an alternative.
	 */
	public String getDisplayText() {
		return displayText;
	}

	/**
	 * Finds the first {@link HiLite} value with {@code displayText}.
	 * 
	 * @param displayText
	 *            A {@link String}.
	 * @return The found {@link HiLite} value.
	 * @throws IllegalArgumentException
	 *             If not found.
	 */
	public static HiLite valueOfDisplayText(final String displayText) {
		if (reverse.isEmpty()) {
			for (final HiLite hiLite : values()) {
				reverse.put(hiLite.getDisplayText(), hiLite);
			}
		}
		final HiLite ret = reverse.get(displayText);
		if (ret == null) {
			throw new IllegalArgumentException("Not found: " + displayText);
		}
		return ret;
	}

	/**
	 * @return The {@link #values()}s' {@link #getDisplayText()}s.
	 */
	@Deprecated
	public static String[] asDisplayTexts() {
		final HiLite[] values = values();
		final String[] ret = new String[values.length];
		for (int i = values.length; i-- > 0;) {
			ret[i] = values[i].getDisplayText();
		}
		return ret;
	}

	/**
	 * @param hiLites
	 *            Some {@link HiLite} values.
	 * @return The {@link #getDisplayText() display texts} of the {@code
	 *         hiLites}.
	 */
	public static String[] asDisplayTexts(final HiLite... hiLites) {
		final String[] ret = new String[hiLites.length];
		for (int i = hiLites.length; i-- > 0;) {
			ret[i] = hiLites[i].getDisplayText();
		}
		return ret;

	}
}

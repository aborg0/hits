/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package ie.tcd.imm.hits.common;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * Tests for {@link Format}'s {@link Format#convertWellToPosition(String) well
 * parsing method}.
 * 
 * @see TestFormatProblems
 * @see TestGoodWells
 * @author <a href="bakosg@tcd.ie">Gabor Bakos</a>
 */
@RunWith(Parameterized.class)
public class TestFormat {
	private final Format format;
	
	/**
	 * @param format
	 */
	public TestFormat(Format format) {
		super();
		this.format = format;
	}

	/**
	 * @return All possible values of {@link Format}.
	 */
	@Parameters
	public static List<Object[]> allFormatsProvider() {
		final Format[] values = Format.values();
		final Object[][] ret = new Object[values.length][1];
		for (int i = values.length; i-- > 0;) {
			ret[i][0] = values[i];
		}
		return Arrays.asList(ret);
	}

	/**
	 * Checks every {@link Format} with null input.
	 * 
	 * @param format
	 *            A {@link Format}.
	 */
	@Test(expected = NullPointerException.class)
	public void nullWell() {
		format.convertWellToPosition(null);
	}
}

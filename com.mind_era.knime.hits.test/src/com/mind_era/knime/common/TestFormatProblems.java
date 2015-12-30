/*
 * 
 */
package com.mind_era.knime.common;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.mind_era.knime.common.Format;

/**
 * Tests the bad formats for {@link Format}.
 *
 * @author Gabor Bakos
 */
@RunWith(Parameterized.class)
public class TestFormatProblems {
	private final String well;
	private final Format format;
	/**
	 * @param well
	 *            A well-like {@link String}.
	 * @param format
	 *            A {@link Format}.
	 */
	public TestFormatProblems(String well, Format format) {
		super();
		this.well = well;
		this.format = format;
	}


	/**
	 * @return Well-like values which are problematic.
	 */
	@Parameters
	public static List<Object[]> problemProvider() {
		return Arrays.asList(new Object[][] { { "", Format._96 }, { "", Format._384 },
				{ "A", Format._96 }, { "A", Format._384 }, { "J", Format._96 },
				{ "Q", Format._384 }, { "J1", Format._96 },
				{ "Q1", Format._384 }, { "A0", Format._96 },
				{ "A0", Format._384 }, { "A13", Format._96 },
				{ "A25", Format._384 }, });
	}


	/**
	 * Checks whether the method gives {@link Exception}s or not.
	 */
	@Test(expected = RuntimeException.class/*StringIndexOutOfBoundsException.class,
			IllegalArgumentException.class, NumberFormatProvider.class*/)
	public void badWellsStringIndex() {
		format.convertWellToPosition(well);
	}
}

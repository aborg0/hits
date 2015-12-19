/*
 * 
 */
package ie.tcd.imm.hits.common;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import org.junit.Assert;

/**
 * Test the good wells against {@link Format}. 
 *
 * @author Gabor Bakos
 */
@RunWith(Parameterized.class)
public class TestGoodWells {
	private final String well;
	private final Format format;
	private final int position;
	
	/**
	 * @param well
	 *            The well {@link String}.
	 * @param format
	 *            The {@link Format}.
	 * @param position
	 *            The expected position.
	 */
	public TestGoodWells(String well, Format format, int position) {
		super();
		this.well = well;
		this.format = format;
		this.position = position;
	}

	/**
	 * @return Good wells with the expected results.
	 */
	@SuppressWarnings("boxing")
	@Parameters()
	public static List<Object[]> goodWellsProvider() {
		return Arrays.asList(new Object[][] { { "A01", Format._96, 0 },
				{ "A01", Format._384, 0 }, { "B01", Format._96, 12 },
				{ "B01", Format._384, 24 }, { "H12", Format._96, 95 },
				{ "P24", Format._384, 383 }, { "A1", Format._96, 0 },
				{ "A1", Format._384, 0 }, { "B1", Format._96, 12 },
				{ "B1", Format._384, 24 }, { "A - 01", Format._96, 0 },
				{ "A - 01", Format._384, 0 }, { "B - 01", Format._96, 12 },
				{ "B - 01", Format._384, 24 }, { "H - 12", Format._96, 95 },
				{ "P - 24", Format._384, 383 }, { "A - 1", Format._96, 0 },
				{ "A - 1", Format._384, 0 }, { "B - 1", Format._96, 12 },
				{ "B - 1", Format._384, 24 }, });
	}

	/**
	 * Tests the good values.
	 */
	@Test
	public void goodWells() {
		final int pos = format.convertWellToPosition(well);
		Assert.assertEquals(pos, position);
		Assert.assertTrue("Too small: " + pos, pos >= 0);
		Assert.assertTrue("Too large: " + pos,
				pos < format.getRow() * format.getCol());
	}
}

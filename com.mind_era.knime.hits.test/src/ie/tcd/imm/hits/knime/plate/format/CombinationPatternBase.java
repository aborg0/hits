/*
 * 
 */
package ie.tcd.imm.hits.knime.plate.format;

import ie.tcd.imm.hits.common.Format;
import org.junit.Assert;

/**
 * Base class for combination patterns.
 *
 * @author Gabor Bakos
 */
abstract class CombinationPatternBase {

	/**
	 * 
	 */
	public CombinationPatternBase() {
		super();
	}

	protected void asserts(final CombinationPattern pattern, final int smallPlate, final int smallRow,
			final int smallCol, final int largePlate, final int largeRow, final int largeCol) {
				Assert.assertEquals("Plate problem, 96->384", pattern.plateCompute(smallPlate - 1, smallRow - 1,
						smallCol - 1, Format._96, Format._384), largePlate);
				Assert.assertEquals("Row problem, 96->384", pattern.rowCompute(smallPlate - 1, smallRow - 1,
						Format._96, Format._384), largeRow);
				Assert.assertEquals("Column problem, 96->384", pattern.colCompute(smallPlate - 1, smallCol - 1,
						Format._96, Format._384), largeCol);
				Assert.assertEquals("Plate problem, 384->96",pattern.plateCompute(largePlate - 1, largeRow - 1,
						largeCol - 1, Format._384, Format._96), smallPlate);
				Assert.assertEquals("Row problem, 384->96", pattern.rowCompute(largePlate - 1, largeRow - 1,
						Format._384, Format._96), smallRow);
				Assert.assertEquals("Column problem, 384->96", pattern.colCompute(largePlate - 1, largeCol - 1,
						Format._384, Format._96), smallCol);
			}

}
/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package ie.tcd.imm.hits.knime.plate.format;

import ie.tcd.imm.hits.common.Format;
import ie.tcd.imm.hits.knime.plate.format.PlateFormatNodeModel.PlateFormatCellFactory;

import java.util.Iterator;

import org.knime.core.data.DataColumnSpec;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * This class tests {@link PlateFormatNodeModel} for some values of
 * {@link CombinationPattern}.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
public class CombinationPatternTests {

	@DataProvider(name = "leftToRight")
	public static Object[][] testCasesLeftToRight() {
		@SuppressWarnings("boxing")
		final Object[][] ret = new Object[][] {// col=1
		{ 1, 1, 1, 1, 1, 1 }, { 2, 1, 1, 1, 1, 13 },//
				{ 3, 1, 1, 1, 9, 1 }, { 4, 1, 1, 1, 9, 13 },//
				{ 5, 1, 1, 2, 1, 1 }, { 6, 1, 1, 2, 1, 13 },//
				{ 7, 1, 1, 2, 9, 1 }, { 8, 1, 1, 2, 9, 13 },// 
				{ 1, 2, 1, 1, 2, 1 }, { 2, 2, 1, 1, 2, 13 },//
				{ 3, 2, 1, 1, 10, 1 }, { 4, 2, 1, 1, 10, 13 },//
				{ 5, 2, 1, 2, 2, 1 }, { 6, 2, 1, 2, 2, 13 },//
				{ 7, 2, 1, 2, 10, 1 }, { 8, 2, 1, 2, 10, 13 },// 
				{ 1, 5, 1, 1, 5, 1 }, { 2, 5, 1, 1, 5, 13 },//
				{ 3, 5, 1, 1, 13, 1 }, { 4, 5, 1, 1, 13, 13 },//
				{ 5, 5, 1, 2, 5, 1 }, { 6, 5, 1, 2, 5, 13 },//
				{ 7, 5, 1, 2, 13, 1 }, { 8, 5, 1, 2, 13, 13 },// 
				{ 1, 7, 1, 1, 7, 1 }, { 2, 7, 1, 1, 7, 13 },//
				{ 3, 7, 1, 1, 15, 1 }, { 4, 7, 1, 1, 15, 13 },//
				{ 5, 7, 1, 2, 7, 1 }, { 6, 7, 1, 2, 7, 13 },//
				{ 7, 7, 1, 2, 15, 1 }, { 8, 7, 1, 2, 15, 13 },//
				// col=2
				{ 1, 1, 2, 1, 1, 2 }, { 2, 1, 2, 1, 1, 14 },//
				{ 3, 1, 2, 1, 9, 2 }, { 4, 1, 2, 1, 9, 14 },//
				{ 5, 1, 2, 2, 1, 2 }, { 6, 1, 2, 2, 1, 14 },//
				{ 7, 1, 2, 2, 9, 2 }, { 8, 1, 2, 2, 9, 14 },// 
				{ 1, 2, 2, 1, 2, 2 }, { 2, 2, 2, 1, 2, 14 },//
				{ 3, 2, 2, 1, 10, 2 }, { 4, 2, 2, 1, 10, 14 },//
				{ 5, 2, 2, 2, 2, 2 }, { 6, 2, 2, 2, 2, 14 },//
				{ 7, 2, 2, 2, 10, 2 }, { 8, 2, 2, 2, 10, 14 },// 
				{ 1, 5, 2, 1, 5, 2 }, { 2, 5, 2, 1, 5, 14 },//
				{ 3, 5, 2, 1, 13, 2 }, { 4, 5, 2, 1, 13, 14 },//
				{ 5, 5, 2, 2, 5, 2 }, { 6, 5, 2, 2, 5, 14 },//
				{ 7, 5, 2, 2, 13, 2 }, { 8, 5, 2, 2, 13, 14 },// 
				{ 1, 7, 2, 1, 7, 2 }, { 2, 7, 2, 1, 7, 14 },//
				{ 3, 7, 2, 1, 15, 2 }, { 4, 7, 2, 1, 15, 14 },//
				{ 5, 7, 2, 2, 7, 2 }, { 6, 7, 2, 2, 7, 14 },//
				{ 7, 7, 2, 2, 15, 2 }, { 8, 7, 2, 2, 15, 14 },// 
				// col=12
				{ 1, 1, 12, 1, 1, 12 }, { 2, 1, 12, 1, 1, 24 },//
				{ 3, 1, 12, 1, 9, 12 }, { 4, 1, 12, 1, 9, 24 },//
				{ 5, 1, 12, 2, 1, 12 }, { 6, 1, 12, 2, 1, 24 },//
				{ 7, 1, 12, 2, 9, 12 }, { 8, 1, 12, 2, 9, 24 },// 
				{ 1, 2, 12, 1, 2, 12 }, { 2, 2, 12, 1, 2, 24 },//
				{ 3, 2, 12, 1, 10, 12 }, { 4, 2, 12, 1, 10, 24 },//
				{ 5, 2, 12, 2, 2, 12 }, { 6, 2, 12, 2, 2, 24 },//
				{ 7, 2, 12, 2, 10, 12 }, { 8, 2, 12, 2, 10, 24 },// 
				{ 1, 5, 12, 1, 5, 12 }, { 2, 5, 12, 1, 5, 24 },//
				{ 3, 5, 12, 1, 13, 12 }, { 4, 5, 12, 1, 13, 24 },//
				{ 5, 5, 12, 2, 5, 12 }, { 6, 5, 12, 2, 5, 24 },//
				{ 7, 5, 12, 2, 13, 12 }, { 8, 5, 12, 2, 13, 24 },// 
				{ 1, 7, 12, 1, 7, 12 }, { 2, 7, 12, 1, 7, 24 },//
				{ 3, 7, 12, 1, 15, 12 }, { 4, 7, 12, 1, 15, 24 },//
				{ 5, 7, 12, 2, 7, 12 }, { 6, 7, 12, 2, 7, 24 },//
				{ 7, 7, 12, 2, 15, 12 }, { 8, 7, 12, 2, 15, 24 },// 
		};
		return ret;
	}

	@DataProvider(name = "upToDown")
	public static Object[][] testCasesUpToDown() {
		@SuppressWarnings("boxing")
		final Object[][] ret = new Object[][] {// col=1
		{ 1, 1, 1, 1, 1, 1 }, { 2, 1, 1, 1, 9, 1 },//
				{ 3, 1, 1, 1, 1, 13 }, { 4, 1, 1, 1, 9, 13 },//
				{ 5, 1, 1, 2, 1, 1 }, { 6, 1, 1, 2, 9, 1 },//
				{ 7, 1, 1, 2, 1, 13 }, { 8, 1, 1, 2, 9, 13 },// 
				{ 1, 2, 1, 1, 2, 1 }, { 2, 2, 1, 1, 10, 1 },//
				{ 3, 2, 1, 1, 2, 13 }, { 4, 2, 1, 1, 10, 13 },//
				{ 5, 2, 1, 2, 2, 1 }, { 6, 2, 1, 2, 10, 1 },//
				{ 7, 2, 1, 2, 2, 13 }, { 8, 2, 1, 2, 10, 13 },// 
				{ 1, 5, 1, 1, 5, 1 }, { 2, 5, 1, 1, 13, 1 },//
				{ 3, 5, 1, 1, 5, 13 }, { 4, 5, 1, 1, 13, 13 },//
				{ 5, 5, 1, 2, 5, 1 }, { 6, 5, 1, 2, 13, 1 },//
				{ 7, 5, 1, 2, 5, 13 }, { 8, 5, 1, 2, 13, 13 },// 
				{ 1, 7, 1, 1, 7, 1 }, { 2, 7, 1, 1, 15, 1 },//
				{ 3, 7, 1, 1, 7, 13 }, { 4, 7, 1, 1, 15, 13 },//
				{ 5, 7, 1, 2, 7, 1 }, { 6, 7, 1, 2, 15, 1 },//
				{ 7, 7, 1, 2, 7, 13 }, { 8, 7, 1, 2, 15, 13 },//
				// col=2
				{ 1, 1, 2, 1, 1, 2 }, { 2, 1, 2, 1, 9, 2 },//
				{ 3, 1, 2, 1, 1, 14 }, { 4, 1, 2, 1, 9, 14 },//
				{ 5, 1, 2, 2, 1, 2 }, { 6, 1, 2, 2, 9, 2 },//
				{ 7, 1, 2, 2, 1, 14 }, { 8, 1, 2, 2, 9, 14 },// 
				{ 1, 2, 2, 1, 2, 2 }, { 2, 2, 2, 1, 10, 2 },//
				{ 3, 2, 2, 1, 2, 14 }, { 4, 2, 2, 1, 10, 14 },//
				{ 5, 2, 2, 2, 2, 2 }, { 6, 2, 2, 2, 10, 2 },//
				{ 7, 2, 2, 2, 2, 14 }, { 8, 2, 2, 2, 10, 14 },// 
				{ 1, 5, 2, 1, 5, 2 }, { 2, 5, 2, 1, 13, 2 },//
				{ 3, 5, 2, 1, 5, 14 }, { 4, 5, 2, 1, 13, 14 },//
				{ 5, 5, 2, 2, 5, 2 }, { 6, 5, 2, 2, 13, 2 },//
				{ 7, 5, 2, 2, 5, 14 }, { 8, 5, 2, 2, 13, 14 },// 
				{ 1, 7, 2, 1, 7, 2 }, { 2, 7, 2, 1, 15, 2 },//
				{ 3, 7, 2, 1, 7, 14 }, { 4, 7, 2, 1, 15, 14 },//
				{ 5, 7, 2, 2, 7, 2 }, { 6, 7, 2, 2, 15, 2 },//
				{ 7, 7, 2, 2, 7, 14 }, { 8, 7, 2, 2, 15, 14 },// 
				// col=12
				{ 1, 1, 12, 1, 1, 12 }, { 2, 1, 12, 1, 9, 12 },//
				{ 3, 1, 12, 1, 1, 24 }, { 4, 1, 12, 1, 9, 24 },//
				{ 5, 1, 12, 2, 1, 12 }, { 6, 1, 12, 2, 9, 12 },//
				{ 7, 1, 12, 2, 1, 24 }, { 8, 1, 12, 2, 9, 24 },// 
				{ 1, 2, 12, 1, 2, 12 }, { 2, 2, 12, 1, 10, 12 },//
				{ 3, 2, 12, 1, 2, 24 }, { 4, 2, 12, 1, 10, 24 },//
				{ 5, 2, 12, 2, 2, 12 }, { 6, 2, 12, 2, 10, 12 },//
				{ 7, 2, 12, 2, 2, 24 }, { 8, 2, 12, 2, 10, 24 },// 
				{ 1, 5, 12, 1, 5, 12 }, { 2, 5, 12, 1, 13, 12 },//
				{ 3, 5, 12, 1, 5, 24 }, { 4, 5, 12, 1, 13, 24 },//
				{ 5, 5, 12, 2, 5, 12 }, { 6, 5, 12, 2, 13, 12 },//
				{ 7, 5, 12, 2, 5, 24 }, { 8, 5, 12, 2, 13, 24 },// 
				{ 1, 7, 12, 1, 7, 12 }, { 2, 7, 12, 1, 15, 12 },//
				{ 3, 7, 12, 1, 7, 24 }, { 4, 7, 12, 1, 15, 24 },//
				{ 5, 7, 12, 2, 7, 12 }, { 6, 7, 12, 2, 15, 12 },//
				{ 7, 7, 12, 2, 7, 24 }, { 8, 7, 12, 2, 15, 24 },// 
		};
		return ret;
	}

	@DataProvider(name = "closePipettes")
	public static Iterator<Object[]> exhaustiveForClosePipettes() {
		return new Iterator<Object[]>() {
			private static final int MAX_SMALL_PLATES = 8;
			private static final int MAX_SMALL_ROWS = 8;
			private static final int MAX_SMALL_COLS = 12;
			private int smallPlate = 1;
			private int smallRow = 1;
			private int smallCol = 1;

			@Override
			public boolean hasNext() {
				return smallPlate <= MAX_SMALL_PLATES + 1;
			}

			@Override
			public Object[] next() {
				@SuppressWarnings("boxing")
				final Object[] ret = new Object[] { smallPlate, smallRow,
						smallCol, (smallPlate - 1) / 4 + 1,
						(smallRow - 1) * 2 + 1 + (smallPlate - 1) % 4 / 2,
						(smallCol - 1) * 2 + 1 + (smallPlate - 1) % 2 };
				if (++smallCol > MAX_SMALL_COLS) {
					smallCol = 1;
					if (++smallRow > MAX_SMALL_ROWS) {
						smallRow = 1;
						++smallPlate;
					}
				}
				return ret;
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}

		};
	}

	@Test(dataProvider = "leftToRight")
	public void leftToRightTests(final int smallPlate, final int smallRow,
			final int smallCol, final int largePlate, final int largeRow,
			final int largeCol) {
		final PlateFormatCellFactory cellFactoryToLarger = new PlateFormatCellFactory(
				new DataColumnSpec[] {}, 0, 1, 2, Format._96, Format._384,
				CombinationPattern.LeftToRightThenDown);
		final PlateFormatCellFactory cellFactoryToSmaller = new PlateFormatCellFactory(
				new DataColumnSpec[] {}, 0, 1, 2, Format._384, Format._96,
				CombinationPattern.LeftToRightThenDown);
		Assert.assertEquals(cellFactoryToLarger.plateCompute(smallPlate - 1,
				smallRow - 1, smallCol - 1), largePlate);
		Assert.assertEquals(cellFactoryToLarger.rowCompute(smallPlate - 1,
				smallRow - 1), largeRow);
		Assert.assertEquals(cellFactoryToLarger.colCompute(smallPlate - 1,
				smallCol - 1), largeCol);
		Assert.assertEquals(cellFactoryToSmaller.plateCompute(largePlate - 1,
				largeRow - 1, largeCol - 1), smallPlate);
		Assert.assertEquals(cellFactoryToSmaller.rowCompute(largePlate - 1,
				largeRow - 1), smallRow);
		Assert.assertEquals(cellFactoryToSmaller.colCompute(largePlate - 1,
				largeCol - 1), smallCol);
	}

	@Test(dataProvider = "upToDown")
	public void upToDownTests(final int smallPlate, final int smallRow,
			final int smallCol, final int largePlate, final int largeRow,
			final int largeCol) {
		final PlateFormatCellFactory cellFactoryToLarger = new PlateFormatCellFactory(
				new DataColumnSpec[] {}, 0, 1, 2, Format._96, Format._384,
				CombinationPattern.UpToDownThenRight);
		final PlateFormatCellFactory cellFactoryToSmaller = new PlateFormatCellFactory(
				new DataColumnSpec[] {}, 0, 1, 2, Format._384, Format._96,
				CombinationPattern.UpToDownThenRight);
		Assert.assertEquals(cellFactoryToLarger.plateCompute(smallPlate - 1,
				smallRow - 1, smallCol - 1), largePlate);
		Assert.assertEquals(cellFactoryToLarger.rowCompute(smallPlate - 1,
				smallRow - 1), largeRow);
		Assert.assertEquals(cellFactoryToLarger.colCompute(smallPlate - 1,
				smallCol - 1), largeCol);
		Assert.assertEquals(cellFactoryToSmaller.plateCompute(largePlate - 1,
				largeRow - 1, largeCol - 1), smallPlate);
		Assert.assertEquals(cellFactoryToSmaller.rowCompute(largePlate - 1,
				largeRow - 1), smallRow);
		Assert.assertEquals(cellFactoryToSmaller.colCompute(largePlate - 1,
				largeCol - 1), smallCol);
	}

	@Test(dataProvider = "upToDownMerge", enabled = false)
	public void upToDownMergeTests(final int smallPlate, final int smallRow,
			final int smallCol, final int largePlate, final int largeRow,
			final int largeCol) {
		final PlateFormatCellFactory cellFactoryToLarger = new PlateFormatCellFactory(
				new DataColumnSpec[] {}, 0, 1, 2, Format._96, Format._384,
				CombinationPattern.UpToDownThenRight8Pipettes);
		final PlateFormatCellFactory cellFactoryToSmaller = new PlateFormatCellFactory(
				new DataColumnSpec[] {}, 0, 1, 2, Format._384, Format._96,
				CombinationPattern.UpToDownThenRight8Pipettes);
		Assert.assertEquals(cellFactoryToLarger.plateCompute(smallPlate - 1,
				smallRow - 1, smallCol - 1), largePlate);
		Assert.assertEquals(cellFactoryToLarger.rowCompute(smallPlate - 1,
				smallRow - 1), largeRow);
		Assert.assertEquals(cellFactoryToLarger.colCompute(smallPlate - 1,
				smallCol - 1), largeCol);
		Assert.assertEquals(cellFactoryToSmaller.plateCompute(largePlate - 1,
				largeRow - 1, largeCol - 1), smallPlate);
		Assert.assertEquals(cellFactoryToSmaller.rowCompute(largePlate - 1,
				largeRow - 1), smallRow);
		Assert.assertEquals(cellFactoryToSmaller.colCompute(largePlate - 1,
				largeCol - 1), smallCol);
	}

	@Test(dataProvider = "closePipettes")
	public void leftToRightCloseMergeTests(final int smallPlate,
			final int smallRow, final int smallCol, final int largePlate,
			final int largeRow, final int largeCol) {
		final PlateFormatCellFactory cellFactoryToLarger = new PlateFormatCellFactory(
				new DataColumnSpec[] {}, 0, 1, 2, Format._96, Format._384,
				CombinationPattern.LeftToRightThenDown8PipettesClose);
		final PlateFormatCellFactory cellFactoryToSmaller = new PlateFormatCellFactory(
				new DataColumnSpec[] {}, 0, 1, 2, Format._384, Format._96,
				CombinationPattern.LeftToRightThenDown8PipettesClose);
		Assert.assertEquals(cellFactoryToLarger.plateCompute(smallPlate - 1,
				smallRow - 1, smallCol - 1), largePlate,
				"Plate problem, 96->384");
		Assert.assertEquals(cellFactoryToLarger.rowCompute(smallPlate - 1,
				smallRow - 1), largeRow, "Row problem, 96->384");
		Assert.assertEquals(cellFactoryToLarger.colCompute(smallPlate - 1,
				smallCol - 1), largeCol, "Column problem, 96->384");
		Assert.assertEquals(cellFactoryToSmaller.plateCompute(largePlate - 1,
				largeRow - 1, largeCol - 1), smallPlate,
				"Plate problem, 384->96");
		Assert.assertEquals(cellFactoryToSmaller.rowCompute(largePlate - 1,
				largeRow - 1), smallRow, "Row problem, 384->96");
		Assert.assertEquals(cellFactoryToSmaller.colCompute(largePlate - 1,
				largeCol - 1), smallCol, "Column problem, 384->96");
	}
}

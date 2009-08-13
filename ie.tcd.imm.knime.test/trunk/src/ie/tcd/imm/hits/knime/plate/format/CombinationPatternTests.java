/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package ie.tcd.imm.hits.knime.plate.format;

import ie.tcd.imm.hits.common.Format;
import ie.tcd.imm.hits.knime.plate.format.PlateFormatNodeModel.PlateFormatCellFactory;

import java.util.Iterator;

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

	/**
	 * Exhaustive (for 2 384 well plates) test data for
	 * {@link CombinationPattern#LeftToRightThenDown}.
	 * 
	 * @return An {@link Iterator} to the data.
	 */
	@DataProvider(name = "leftToRight")
	public static Iterator<Object[]> exhaustiveLeftToRight() {
		return new Iterator<Object[]>() {
			private static final int MAX_SMALL_PLATES = 8;
			private static final int MAX_SMALL_ROWS = 8;
			private static final int MAX_SMALL_COLS = 12;
			private int smallPlate = 1;
			private int smallRow = 1;
			private int smallCol = 1;

			@Override
			public boolean hasNext() {
				return smallPlate <= MAX_SMALL_PLATES;
			}

			@Override
			public Object[] next() {
				@SuppressWarnings("boxing")
				final Object[] ret = new Object[] { smallPlate, smallRow,
						smallCol, (smallPlate - 1) / 4 + 1,
						smallRow + (smallPlate - 1) % 4 / 2 * 8,
						smallCol + (smallPlate - 1) % 2 * 12 };
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

	/**
	 * Exhaustive (for 2 384 well plates) test data for
	 * {@link CombinationPattern#UpToDownThenRight}.
	 * 
	 * @return An {@link Iterator} to the data.
	 */
	@DataProvider(name = "upToDown")
	public static Iterator<Object[]> exhaustiveUpToDown() {
		return new Iterator<Object[]>() {
			private static final int MAX_SMALL_PLATES = 8;
			private static final int MAX_SMALL_ROWS = 8;
			private static final int MAX_SMALL_COLS = 12;
			private int smallPlate = 1;
			private int smallRow = 1;
			private int smallCol = 1;

			@Override
			public boolean hasNext() {
				return smallPlate <= MAX_SMALL_PLATES;
			}

			@Override
			public Object[] next() {
				@SuppressWarnings("boxing")
				final Object[] ret = new Object[] { smallPlate, smallRow,
						smallCol, (smallPlate - 1) / 4 + 1,
						smallRow + (smallPlate - 1) % 2 * 8,
						smallCol + (smallPlate - 1) % 4 / 2 * 12 };
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

	/**
	 * Exhaustive (for 2 384 well plates) test data for
	 * {@link CombinationPattern#UpToDownThenRight8Pipettes}.
	 * 
	 * @return An {@link Iterator} to the data.
	 */
	@DataProvider(name = "upToDownMerge")
	public static Iterator<Object[]> exhaustiveUpToDown8Pipettes() {
		return new Iterator<Object[]>() {
			private static final int MAX_SMALL_PLATES = 8;
			private static final int MAX_SMALL_ROWS = 8;
			private static final int MAX_SMALL_COLS = 12;
			private int smallPlate = 1;
			private int smallRow = 1;
			private int smallCol = 1;

			@Override
			public boolean hasNext() {
				return smallPlate <= MAX_SMALL_PLATES;
			}

			@Override
			public Object[] next() {
				@SuppressWarnings("boxing")
				final Object[] ret = new Object[] { smallPlate, smallRow,
						smallCol, (smallPlate - 1) / 4 + 1,
						(smallRow - 1) * 2 + 1 + (smallPlate - 1) % 2,
						smallCol + (smallPlate - 1) % 4 / 2 * 12 };
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

	/**
	 * Exhaustive (for 2 384 well plates) test data for
	 * {@link CombinationPattern#LeftToRightThenDown8PipettesClose}.
	 * 
	 * @return An {@link Iterator} to the data.
	 */
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

	/**
	 * Tests the {@link CombinationPattern#LeftToRightThenDown} version of
	 * {@link PlateFormatCellFactory}.
	 * 
	 * @param smallPlate
	 *            The plate number on small (96 well plate) (starting from
	 *            {@code 1}).
	 * @param smallRow
	 *            The row number on small (96 well plate) (starting from {@code
	 *            1}).
	 * @param smallCol
	 *            The column number on small (96 well plate) (starting from
	 *            {@code 1}).
	 * @param largePlate
	 *            The plate number on large (384 well plate) (starting from
	 *            {@code 1}).
	 * @param largeRow
	 *            The row number on large (384 well plate) (starting from
	 *            {@code 1}).
	 * @param largeCol
	 *            The column number on large (384 well plate) (starting from
	 *            {@code 1}).
	 */
	@Test(dataProvider = "leftToRight")
	public void leftToRightTests(final int smallPlate, final int smallRow,
			final int smallCol, final int largePlate, final int largeRow,
			final int largeCol) {
		// final PlateFormatCellFactory cellFactoryToLarger = new
		// PlateFormatCellFactory(
		// new DataColumnSpec[] {}, 0, 1, 2, Format._96, Format._384,
		// CombinationPattern.LeftToRightThenDown);
		// final PlateFormatCellFactory cellFactoryToSmaller = new
		// PlateFormatCellFactory(
		// new DataColumnSpec[] {}, 0, 1, 2, Format._384, Format._96,
		// CombinationPattern.LeftToRightThenDown);
		// asserts(cellFactoryToLarger, cellFactoryToSmaller, smallPlate,
		// smallRow, smallCol, largePlate, largeRow, largeCol);
		asserts(CombinationPattern.LeftToRightThenDown, smallPlate, smallRow,
				smallCol, largePlate, largeRow, largeCol);
	}

	/**
	 * Tests the {@link CombinationPattern#UpToDownThenRight} version of
	 * {@link PlateFormatCellFactory}.
	 * 
	 * @param smallPlate
	 *            The plate number on small (96 well plate) (starting from
	 *            {@code 1}).
	 * @param smallRow
	 *            The row number on small (96 well plate) (starting from {@code
	 *            1}).
	 * @param smallCol
	 *            The column number on small (96 well plate) (starting from
	 *            {@code 1}).
	 * @param largePlate
	 *            The plate number on large (384 well plate) (starting from
	 *            {@code 1}).
	 * @param largeRow
	 *            The row number on large (384 well plate) (starting from
	 *            {@code 1}).
	 * @param largeCol
	 *            The column number on large (384 well plate) (starting from
	 *            {@code 1}).
	 */
	@Test(dataProvider = "upToDown")
	public void upToDownTests(final int smallPlate, final int smallRow,
			final int smallCol, final int largePlate, final int largeRow,
			final int largeCol) {
		// final PlateFormatCellFactory cellFactoryToLarger = new
		// PlateFormatCellFactory(
		// new DataColumnSpec[] {}, 0, 1, 2, Format._96, Format._384,
		// CombinationPattern.UpToDownThenRight);
		// final PlateFormatCellFactory cellFactoryToSmaller = new
		// PlateFormatCellFactory(
		// new DataColumnSpec[] {}, 0, 1, 2, Format._384, Format._96,
		// CombinationPattern.UpToDownThenRight);
		// asserts(cellFactoryToLarger, cellFactoryToSmaller, smallPlate,
		// smallRow, smallCol, largePlate, largeRow, largeCol);
		asserts(CombinationPattern.UpToDownThenRight, smallPlate, smallRow,
				smallCol, largePlate, largeRow, largeCol);
	}

	/**
	 * Tests the {@link CombinationPattern#UpToDownThenRight8Pipettes} version
	 * of {@link PlateFormatCellFactory}.
	 * 
	 * @param smallPlate
	 *            The plate number on small (96 well plate) (starting from
	 *            {@code 1}).
	 * @param smallRow
	 *            The row number on small (96 well plate) (starting from {@code
	 *            1}).
	 * @param smallCol
	 *            The column number on small (96 well plate) (starting from
	 *            {@code 1}).
	 * @param largePlate
	 *            The plate number on large (384 well plate) (starting from
	 *            {@code 1}).
	 * @param largeRow
	 *            The row number on large (384 well plate) (starting from
	 *            {@code 1}).
	 * @param largeCol
	 *            The column number on large (384 well plate) (starting from
	 *            {@code 1}).
	 */
	@Test(dataProvider = "upToDownMerge")
	public void upToDownMergeTests(final int smallPlate, final int smallRow,
			final int smallCol, final int largePlate, final int largeRow,
			final int largeCol) {
		// final PlateFormatCellFactory cellFactoryToLarger = new
		// PlateFormatCellFactory(
		// new DataColumnSpec[] {}, 0, 1, 2, Format._96, Format._384,
		// CombinationPattern.UpToDownThenRight8Pipettes);
		// final PlateFormatCellFactory cellFactoryToSmaller = new
		// PlateFormatCellFactory(
		// new DataColumnSpec[] {}, 0, 1, 2, Format._384, Format._96,
		// CombinationPattern.UpToDownThenRight8Pipettes);
		// asserts(cellFactoryToLarger, cellFactoryToSmaller, smallPlate,
		// smallRow, smallCol, largePlate, largeRow, largeCol);
		asserts(CombinationPattern.UpToDownThenRight8Pipettes, smallPlate,
				smallRow, smallCol, largePlate, largeRow, largeCol);
	}

	/**
	 * Tests the {@link CombinationPattern#LeftToRightThenDown8PipettesClose}
	 * version of {@link PlateFormatCellFactory}.
	 * 
	 * @param smallPlate
	 *            The plate number on small (96 well plate) (starting from
	 *            {@code 1}).
	 * @param smallRow
	 *            The row number on small (96 well plate) (starting from {@code
	 *            1}).
	 * @param smallCol
	 *            The column number on small (96 well plate) (starting from
	 *            {@code 1}).
	 * @param largePlate
	 *            The plate number on large (384 well plate) (starting from
	 *            {@code 1}).
	 * @param largeRow
	 *            The row number on large (384 well plate) (starting from
	 *            {@code 1}).
	 * @param largeCol
	 *            The column number on large (384 well plate) (starting from
	 *            {@code 1}).
	 */
	@Test(dataProvider = "closePipettes")
	public void leftToRightCloseMergeTests(final int smallPlate,
			final int smallRow, final int smallCol, final int largePlate,
			final int largeRow, final int largeCol) {
		// final PlateFormatCellFactory cellFactoryToLarger = new
		// PlateFormatCellFactory(
		// new DataColumnSpec[] {}, 0, 1, 2, Format._96, Format._384,
		// CombinationPattern.LeftToRightThenDown8PipettesClose);
		// final PlateFormatCellFactory cellFactoryToSmaller = new
		// PlateFormatCellFactory(
		// new DataColumnSpec[] {}, 0, 1, 2, Format._384, Format._96,
		// CombinationPattern.LeftToRightThenDown8PipettesClose);
		// asserts(cellFactoryToLarger, cellFactoryToSmaller, smallPlate,
		// smallRow, smallCol, largePlate, largeRow, largeCol);
		asserts(CombinationPattern.LeftToRightThenDown8PipettesClose,
				smallPlate, smallRow, smallCol, largePlate, largeRow, largeCol);
	}

	// private void asserts(final PlateFormatCellFactory cellFactoryToLarger,
	// final PlateFormatCellFactory cellFactoryToSmaller,
	// final int smallPlate, final int smallRow, final int smallCol,
	// final int largePlate, final int largeRow, final int largeCol) {
	// Assert.assertEquals(cellFactoryToLarger.plateCompute(smallPlate - 1,
	// smallRow - 1, smallCol - 1), largePlate,
	// "Plate problem, 96->384");
	// Assert.assertEquals(cellFactoryToLarger.rowCompute(smallPlate - 1,
	// smallRow - 1), largeRow, "Row problem, 96->384");
	// Assert.assertEquals(cellFactoryToLarger.colCompute(smallPlate - 1,
	// smallCol - 1), largeCol, "Column problem, 96->384");
	// Assert.assertEquals(cellFactoryToSmaller.plateCompute(largePlate - 1,
	// largeRow - 1, largeCol - 1), smallPlate,
	// "Plate problem, 384->96");
	// Assert.assertEquals(cellFactoryToSmaller.rowCompute(largePlate - 1,
	// largeRow - 1), smallRow, "Row problem, 384->96");
	// Assert.assertEquals(cellFactoryToSmaller.colCompute(largePlate - 1,
	// largeCol - 1), smallCol, "Column problem, 384->96");
	// }

	private void asserts(final CombinationPattern pattern,
			final int smallPlate, final int smallRow, final int smallCol,
			final int largePlate, final int largeRow, final int largeCol) {
		Assert.assertEquals(pattern.plateCompute(smallPlate - 1, smallRow - 1,
				smallCol - 1, Format._96, Format._384), largePlate,
				"Plate problem, 96->384");
		Assert.assertEquals(pattern.rowCompute(smallPlate - 1, smallRow - 1,
				Format._96, Format._384), largeRow, "Row problem, 96->384");
		Assert.assertEquals(pattern.colCompute(smallPlate - 1, smallCol - 1,
				Format._96, Format._384), largeCol, "Column problem, 96->384");
		Assert.assertEquals(pattern.plateCompute(largePlate - 1, largeRow - 1,
				largeCol - 1, Format._384, Format._96), smallPlate,
				"Plate problem, 384->96");
		Assert.assertEquals(pattern.rowCompute(largePlate - 1, largeRow - 1,
				Format._384, Format._96), smallRow, "Row problem, 384->96");
		Assert.assertEquals(pattern.colCompute(largePlate - 1, largeCol - 1,
				Format._384, Format._96), smallCol, "Column problem, 384->96");
	}
}

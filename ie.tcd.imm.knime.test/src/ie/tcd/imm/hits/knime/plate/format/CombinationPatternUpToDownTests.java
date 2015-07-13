/*
 * 
 */
package ie.tcd.imm.hits.knime.plate.format;

import ie.tcd.imm.hits.knime.plate.format.PlateFormatNodeModel.PlateFormatCellFactory;

import java.util.AbstractCollection;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 *
 * @author Gabor Bakos
 */
@RunWith(Parameterized.class)
public class CombinationPatternUpToDownTests extends CombinationPatternBase {
	private final int smallPlate, smallRow, smallCol, largePlate, largeRow, largeCol;
	
	/**
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
	public CombinationPatternUpToDownTests(int smallPlate, int smallRow,
			int smallCol, int largePlate, int largeRow, int largeCol) {
		super();
		this.smallPlate = smallPlate;
		this.smallRow = smallRow;
		this.smallCol = smallCol;
		this.largePlate = largePlate;
		this.largeRow = largeRow;
		this.largeCol = largeCol;
	}
	/**
	 * Exhaustive (for 2 384 well plates) test data for
	 * {@link CombinationPattern#UpToDownThenRight}.
	 * 
	 * @return An {@link Iterator} to the data.
	 */
	@Parameters
	public static List<Object[]> exhaustiveUpToDown() {
		return Arrays.asList(new AbstractCollection<Object[]>() {
			private static final int MAX_SMALL_PLATES = 8;
			private static final int MAX_SMALL_ROWS = 8;
			private static final int MAX_SMALL_COLS = 12;
			@Override
			public Iterator<Object[]> iterator() {
				return new Iterator<Object[]>() {
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

			@Override
			public int size() {
				return MAX_SMALL_PLATES * MAX_SMALL_ROWS * MAX_SMALL_ROWS;
			}
			
		}.toArray(new Object[0][]));
	}
	/**
	 * Tests the {@link CombinationPattern#UpToDownThenRight} version of
	 * {@link PlateFormatCellFactory}.
	 */
	@Test
	public void upToDownTests(
			) {
		asserts(CombinationPattern.UpToDownThenRight, smallPlate, smallRow,
				smallCol, largePlate, largeRow, largeCol);
	}

}

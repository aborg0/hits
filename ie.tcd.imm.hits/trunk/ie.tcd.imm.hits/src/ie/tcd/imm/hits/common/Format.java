package ie.tcd.imm.hits.common;

/**
 * Plate format. Only {@code 96}, or {@code 384} are supported.
 */
public enum Format {
	/** 96 plate format */
	_96(8, 12),
	/** 384 plate format */
	_384(16, 24);

	private final int row;
	private final int col;

	private Format(final int row, final int col) {
		this.row = row;
		this.col = col;
	}

	/**
	 * @return The row count for the {@link Format plate format}.
	 */
	public int getRow() {
		return row;
	}

	/**
	 * @return The column count for the {@link Format plate format}.
	 */
	public int getCol() {
		return col;
	}

	/**
	 * Converts a {@code well} like {@code A01}, {@code A1}, or {@code A - 1}
	 * to a position between {@code [0} and {@code row * col)}.
	 * 
	 * @param well
	 *            A {@link String} like: a {@code well} like {@code A01},
	 *            {@code A1}, or {@code A - 1}. The letter represents the row,
	 *            the number represents the column.
	 * @return The position appropriate for the {@link Format} between
	 *         {@code [0} and {@code row * col)} ({@code rowPositionFromA * columnCount + columnPositionFrom1}).
	 * @throws IllegalArgumentException
	 *             If the column or row is out of the allowed values.
	 * @throws StringIndexOutOfBoundsException
	 *             If the length of {@code well} is not at least {@code 2}.
	 * @throws NumberFormatException
	 *             If the column is not a proper number.
	 * @throws NullPointerException
	 *             If {@code well} is {@code null}.
	 */
	public int convertWellToPosition(final String well)
			throws IllegalArgumentException, StringIndexOutOfBoundsException,
			NumberFormatException, NullPointerException {
		final int rowPos = Character.toLowerCase(well.charAt(0)) - 'a';
		if (rowPos < 0 || rowPos >= row) {
			throw new IllegalArgumentException("Wrong row: " + well.charAt(0)
					+ " (" + well + ")");
		}
		final String colString = well.substring(well.length()
				- (Character.isDigit(well.charAt(well.length() - 2)) ? 2 : 1),
				well.length());
		final int column = Integer.parseInt(colString);
		if (column <= 0 || column > col) {
			throw new IllegalArgumentException("Wrong column: " + colString
					+ " (" + well + ")");
		}
		return rowPos * col + column - 1;
	}
}
/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package com.mind_era.knime.common;

import com.mind_era.knime.common.util.Displayable;

/**
 * Plate format. Only {@code 96}, or {@code 384} are supported.
 */
public enum Format implements Displayable {
	/** 96 well plate format */
	_96(8, 12),
	/** 384 well plate format */
	_384(16, 24),
	/** 1536 well plate format */
	_1536(32, 48);

	private final int row;
	private final int col;
	private final int wellCount;

	private Format(final int row, final int col) {
		this.row = row;
		this.col = col;
		this.wellCount = this.row * this.col;
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
	 * Converts a {@code well} like {@code A01}, {@code A1}, or {@code A - 1} to
	 * a position between {@code [0} and {@code row * col)}.
	 * 
	 * @param well
	 *            A {@link String} like: a {@code well} like {@code A01},
	 *            {@code A1}, or {@code A - 1}. The letter represents the row,
	 *            the number represents the column.
	 * @return The position appropriate for the {@link Format} between {@code
	 *         [0} and {@code row * col)} ({@code rowPositionFromA * columnCount
	 *         + columnPositionFrom1}).
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

	/**
	 * @return The well count associated with this format.
	 */
	public int getWellCount() {
		return wellCount;
	}

	@Override
	public String getDisplayText() {
		return String.valueOf(wellCount);
	}

	/**
	 * @param row_
	 *            The row ({@code 0}-based) information of position. (Asserts
	 *            present.)
	 * @param col_
	 *            The column ({@code 0}-based) information of position. (Asserts
	 *            present.)
	 * @return The {@code 0}-based position on plate.
	 */
	public int getPos(final int row_, final int col_) {
		assert row_ >= 0;
		assert row_ < row;
		assert col_ >= 0;
		assert col_ < col;
		return row_ * this.col + col_;
	}

	/**
	 * Converts a position to {@code other} plate format.
	 * 
	 * @param pos
	 *            A position in <em>this</em> plate {@link Format}. ({@code 0}
	 *            -based)
	 * @param other
	 *            The other plate {@link Format}. (The plate {@link Format} to
	 *            convert to.)
	 * @return The position on {@code other} plate {@link Format}.
	 */
	public int convertPos(final int pos, final Format other) {
		final int newRow = other.getRow(pos);
		final int newCol = other.getCol(pos);
		return getPos(newRow, newCol);
	}

	/**
	 * Converts a position to {@code other} plate format without any checks on
	 * the input or output values.
	 * 
	 * @param pos
	 *            A position in <em>this</em> plate {@link Format}. ({@code 0}
	 *            -based)
	 * @param other
	 *            The other plate {@link Format}. (The plate {@link Format} to
	 *            convert to.)
	 * @return The position on {@code other} plate {@link Format}.
	 */
	public int unsafeConvertPos(final int pos, final Format other) {
		final int newRow = pos / other.col;
		final int newCol = pos % other.col;
		return newRow * col + newCol;
	}

	/**
	 * @param pos
	 *            A position on plate. ({@code 0}-based)
	 * @return The {@code 0}-based row value of {@code pos}.
	 */
	public int getRow(final int pos) {
		assert pos >= 0;
		assert pos < wellCount;
		return pos / col;
	}

	/**
	 * @param pos
	 *            A position on plate. ({@code 0}-based)
	 * @return The {@code 0}-based column value of {@code pos}.
	 */
	public int getCol(final int pos) {
		assert pos >= 0;
		assert pos < wellCount;
		return pos % col;
	}
}
/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package ie.tcd.imm.hits.knime.plate.format;

import ie.tcd.imm.hits.common.Format;
import ie.tcd.imm.hits.util.Displayable;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;

import javax.swing.Icon;

import org.knime.core.node.defaultnodesettings.HasIcon;

/**
 * This enum simple shows the possible options for the generation of the plates.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
public enum CombinationPattern implements Displayable, HasIcon {
	/** The normal 12|34 order, without any merge. */
	LeftToRightThenDown(true, true, true, 0, false, false, false) {
		@Override
		public int colComputeToLarger(final int plate, final int col,
				final Format from, final Format to) {
			return plate % (to.getCol() / from.getCol()) * from.getCol() + col
					+ 1;
		}

		@Override
		public int plateComputeToSmaller(final int plate, final int row,
				final int col, final Format from, final Format to) {
			return plate * (from.getWellCount() / to.getWellCount()) + col
					/ to.getCol() + row / to.getRow()
					* (from.getCol() / to.getCol()) + 1;
		}

		@Override
		public int rowCompute(final int plate, final int row,
				final Format from, final Format to) {
			return to.getWellCount() > from.getWellCount() ? plate
					% (to.getWellCount() / from.getWellCount())
					/ (to.getRow() / from.getRow()) * from.getRow() + row + 1
					: row % to.getRow() + 1;
		}
	},
	/** The normal 13|24 order without merging. */
	UpToDownThenRight(false, true, true, 0, false, false, false) {

		@Override
		public int colComputeToLarger(final int plate, final int col,
				final Format from, final Format to) {
			return plate % (to.getWellCount() / from.getWellCount())
					/ (to.getCol() / from.getCol()) * from.getCol() + col + 1;
		}

		@Override
		public int plateComputeToSmaller(final int plate, final int row,
				final int col, final Format from, final Format to) {
			return plate * (from.getWellCount() / to.getWellCount()) + col
					/ to.getCol() * (from.getRow() / to.getRow()) + row
					/ to.getRow() + 1;
		}

		@Override
		public int rowCompute(final int plate, final int row,
				final Format from, final Format to) {
			return to.getWellCount() > from.getWellCount() ? plate
					% (to.getWellCount() / from.getWellCount())
					% (to.getCol() / from.getCol()) * from.getRow() + row + 1
					: row % to.getRow() + 1;
		}
	},
	/** The merged view 13|24 with 8 pipettes. */
	UpToDownThenRight8Pipettes(false, true, true, 8, false, false, false) {

		@Override
		public int colComputeToLarger(final int plate, final int col,
				final Format from, final Format to) {
			return plate % (to.getWellCount() / from.getWellCount())
					/ (to.getCol() / from.getCol()) * from.getCol() + col + 1;
		}

		@Override
		public int plateComputeToSmaller(final int plate, final int row,
				final int col, final Format from, final Format to) {
			return plate * (from.getWellCount() / to.getWellCount()) + col
					/ to.getCol() * (from.getRow() / to.getRow()) + row
					% (from.getRow() / getPipettes()) + 1;
		}

		@Override
		public int rowCompute(final int plate, final int row,
				final Format from, final Format to) {
			return to.getWellCount() > from.getWellCount() ? plate
					% (to.getWellCount() / from.getWellCount())
					% (to.getCol() / from.getCol()) + row
					* (to.getRow() / getPipettes()) + 1 : row
					/ (from.getRow() / getPipettes()) % to.getRow() + 1;
		}
	},
	/** The merged view 13|24 with 8 pipettes with close to each other */
	LeftToRightThenDown8PipettesClose(true, true, true, 8, false, false, true) {

		@Override
		public int colComputeToLarger(final int plate, final int col,
				final Format from, final Format to) {
			return plate % (to.getCol() / from.getCol()) + col
					* (to.getCol() / from.getCol()) + 1;
		}

		@Override
		public int plateComputeToSmaller(final int plate, final int row,
				final int col, final Format from, final Format to) {
			return plate * (from.getWellCount() / to.getWellCount()) + col
					% (from.getCol() / to.getCol()) + row
					% (from.getRow() / to.getRow())
					* (from.getCol() / to.getCol()) + 1;
		}

		@Override
		public int rowCompute(final int plate, final int row,
				final Format from, final Format to) {
			return to.getWellCount() > from.getWellCount() ? plate
					% (to.getWellCount() / from.getWellCount())
					/ (to.getCol() / from.getCol()) + row
					* (to.getCol() / from.getCol()) + 1 : row
					/ (from.getRow() / getPipettes()) % to.getRow() + 1;
		}
	};

	private final boolean firstHorizontal;
	private final boolean verticalToDown;
	private final boolean horizontalToRight;
	private final int pipettes;
	private final boolean combineReplicates;
	private final boolean keepEmptyReplicates;
	private final boolean closeWells;

	private CombinationPattern(final boolean firstHorizontal,
			final boolean verticalToDown, final boolean horizontalToRight,
			final int pipettes, final boolean combineReplicates,
			final boolean keepEmptyReplicates, final boolean closeWells) {
		this.firstHorizontal = firstHorizontal;
		this.verticalToDown = verticalToDown;
		this.horizontalToRight = horizontalToRight;
		this.pipettes = pipettes;
		this.combineReplicates = combineReplicates;
		this.keepEmptyReplicates = keepEmptyReplicates;
		this.closeWells = closeWells;
	}

	/**
	 * @return The first direction is horizontal.
	 */
	public final boolean isFirstHorizontal() {
		return firstHorizontal;
	}

	/**
	 * @return The vertical to down.
	 */
	public final boolean isVerticalToDown() {
		return verticalToDown;
	}

	/**
	 * @return The horizontal to right.
	 */
	public final boolean isHorizontalToRight() {
		return horizontalToRight;
	}

	/**
	 * @return The number of pipettes.
	 */
	public final int getPipettes() {
		return pipettes;
	}

	/**
	 * @return The combine replicates to the tables.
	 */
	public final boolean isCombineReplicates() {
		return combineReplicates;
	}

	/**
	 * @return The keep empty replicates, or fill them with values form the next
	 *         plate's first replicate.
	 */
	public final boolean isKeepEmptyReplicates() {
		return keepEmptyReplicates;
	}

	/**
	 * @return The closeWells.
	 */
	public boolean isCloseWells() {
		return closeWells;
	}

	/**
	 * @param plate
	 *            The original plate number ({@code 0} based).
	 * @param col
	 *            The original column number ({@code 0} based).
	 * @param from
	 *            The source's {@link Format}.
	 * @param to
	 *            The destination's {@link Format}.
	 * @return The computed column number ({@code 1} based).
	 */
	public int colCompute(final int plate, final int col, final Format from,
			final Format to) {
		return to.getWellCount() > from.getWellCount() ? colComputeToLarger(
				plate, col, from, to) : (isCloseWells() ? col
				/ (from.getCol() / to.getCol()) : col % to.getCol()) + 1;
	}

	/**
	 * @param plate
	 *            The original plate number ({@code 0} based).
	 * @param col
	 *            The original column number ({@code 0} based).
	 * @param from
	 *            The source's {@link Format}.
	 * @param to
	 *            The destination's {@link Format}.
	 * @return The computed column number, when {@code to} is larger ({@code 1}
	 *         based).
	 */
	protected abstract int colComputeToLarger(final int plate, final int col,
			Format from, Format to);

	/**
	 * @param plate
	 *            The original plate number ({@code 0} based).
	 * @param row
	 *            The original row number ({@code 0} based).
	 * @param from
	 *            The source's {@link Format}.
	 * @param to
	 *            The destination's {@link Format}.
	 * @return The computed row number ({@code 1} based).
	 */
	public abstract int rowCompute(final int plate, final int row, Format from,
			Format to);

	/**
	 * @param plate
	 *            The original plate number ({@code 0} based).
	 * @param col
	 *            The original column number ({@code 0} based).
	 * @param row
	 *            The original row number ({@code 0} based).
	 * @param from
	 *            The source's {@link Format}.
	 * @param to
	 *            The destination's {@link Format}.
	 * @return The computed column number ({@code 1} based).
	 */
	public int plateCompute(final int plate, final int row, final int col,
			final Format from, final Format to) {
		return to.getWellCount() > from.getWellCount() ? plate
				/ (to.getWellCount() / from.getWellCount()) + 1
				: plateComputeToSmaller(plate, row, col, from, to);
	}

	/**
	 * @param plate
	 *            The original plate number ({@code 0} based).
	 * @param col
	 *            The original column number ({@code 0} based).
	 * @param row
	 *            The original row number ({@code 0} based).
	 * @param from
	 *            The source's {@link Format}.
	 * @param to
	 *            The destination's {@link Format}.
	 * @return The new plate value when {@code to} is smaller ({@code 1} based).
	 */
	protected abstract int plateComputeToSmaller(int plate, int row, int col,
			Format from, Format to);

	/** {@inheritDoc} */
	@Override
	public String getDisplayText() {
		return (firstHorizontal ? (horizontalToRight ? "left to right"
				: "right to left")
				+ " then " + (verticalToDown ? "up to down" : "down to up")
				: (verticalToDown ? "up to down" : "down to up")
						+ " then "
						+ (horizontalToRight ? "left to right"
								: "right to left"))
				+ " "
				+ (pipettes > 0 ? pipettes + " pipettes " : "")
				+ (closeWells ? "wells are close " : "")
				+ (combineReplicates ? keepEmptyReplicates ? "keeping empty spaces"
						: "filling empty spaces with replicates"
						: "without replicates");
	}

	/** {@inheritDoc} */
	@Override
	public Icon getIcon() {
		return new Icon() {
			private static final int PLATE_HEIGHT = 16;
			private static final int PLATE_WIDTH = 24;
			private static final int GAP = 2;

			private final Color[] colours = new Color[] { Color.RED,
					Color.BLACK, Color.WHITE, Color.BLUE };

			@Override
			public int getIconHeight() {
				return PLATE_HEIGHT * 2;
			}

			@Override
			public int getIconWidth() {
				return (PLATE_WIDTH + GAP + PLATE_WIDTH / 4) * 2;
			}

			@Override
			public void paintIcon(final Component c, final Graphics g,
					final int x, final int y) {
				for (int j = PLATE_HEIGHT; j-- > 0;) {
					for (int i = PLATE_WIDTH; i-- > 0;) {
						final int row = j % (PLATE_HEIGHT + GAP);
						final int col = i % (PLATE_WIDTH + GAP);
						if (row < PLATE_HEIGHT && col < PLATE_WIDTH) {
							final int plate = i / (PLATE_WIDTH + GAP);
							final int plateCompute = plateCompute(plate, row,
									col, Format._384, Format._96);
							final int newPlate = plateCompute - 1;
							g.setColor(colours[newPlate % colours.length]);
							g.fillRect(i * 2, j * 2, 2, 2);
						}
					}
				}
				g.setColor(Color.LIGHT_GRAY);
				g.fillRect(PLATE_WIDTH * 2 + 1, 0, 8, getIconHeight());
				for (int i = colours.length; i-- > 0;) {
					g.setColor(colours[i]);
					g.drawString(String.valueOf(i + 1), PLATE_WIDTH * 2 + 2,
							(i + 1) * (getIconHeight() / colours.length));
				}
			}
		};
	}
}

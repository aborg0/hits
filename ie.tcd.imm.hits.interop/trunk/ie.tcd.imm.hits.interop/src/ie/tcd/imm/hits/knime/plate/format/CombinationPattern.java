/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package ie.tcd.imm.hits.knime.plate.format;

import ie.tcd.imm.hits.util.Displayable;

/**
 * This enum simple shows the possible options for the generation of the plates.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
public enum CombinationPattern implements Displayable {
	/** The normal 12|34 order, without any merge. */
	LeftToRightThenDown(true, true, true, 0, false, false, false),
	/** The normal 13|24 order without merging. */
	UpToDownThenRight(false, true, true, 0, false, false, false),
	/** The merged view 13|24 with 8 pipettes. */
	UpToDownThenRight8Pipettes(false, true, true, 8, false, false, false),
	/** The merged view 13|24 with 8 pipettes with close to each other */
	LeftToRightThenDown8PipettesClose(true, true, true, 8, false, false, true);

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

	/*
	 * (non-Javadoc)
	 * 
	 * @see ie.tcd.imm.hits.util.Displayable#getDisplayText()
	 */
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
}
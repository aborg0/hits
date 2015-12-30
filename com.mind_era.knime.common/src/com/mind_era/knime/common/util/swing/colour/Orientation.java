/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package com.mind_era.knime.common.util.swing.colour;

/** The orientation of the sample (text). */
public enum Orientation {
	/** At the up */
	North(false),
	/** Left */
	West(true),
	/** Down */
	South(false),
	/** Right */
	East(true);
	private final boolean isVertical;

	private Orientation(final boolean isVertical) {
		this.isVertical = isVertical;
	}

	/**
	 * @return the isVertical
	 */
	public boolean isVertical() {
		return isVertical;
	}
}
/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package com.mind_era.knime.common.util.swing.colour;

import java.awt.Color;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

/**
 * Implementations compute the {@link Color} value for a {@code double} value.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
@Nonnull
@CheckReturnValue
public interface ColourComputer {
	/**
	 * Computes a {@link Color} for the {@code val}.
	 * 
	 * @param val
	 *            A {@code double} value.
	 * @return The {@link Color} belonging to {@code val}.
	 */
	public Color compute(double val);

	/**
	 * @return The tooltip for that {@link ColourComputer}.
	 */
	public String getTooltip();
}

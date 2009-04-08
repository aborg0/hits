/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package ie.tcd.imm.hits.util.swing.colour;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import edu.umd.cs.findbugs.annotations.DefaultAnnotation;

/**
 * A legend for a {@link ColourComputer}.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 * @param <T>
 *            The type of the model.
 */
@DefaultAnnotation( { Nonnull.class, CheckReturnValue.class })
public interface ColourLegend<T extends ColourComputer> {
	/**
	 * Sets the actual {@link ColourComputer} and {@link Orientation}.
	 * 
	 * @param model
	 *            The new {@link ColourComputer}.
	 * @param orientation
	 *            The new {@link Orientation}.
	 */
	public void setModel(T model, Orientation orientation);

	/** @return The actual {@link ColourComputer}. */
	public T getModel();
}

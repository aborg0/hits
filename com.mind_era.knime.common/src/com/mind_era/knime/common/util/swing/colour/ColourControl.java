/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package com.mind_era.knime.common.util.swing.colour;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

/**
 * Interface of the controls for {@link ColourComputer}s.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 * @param <T>
 *            The controlled {@link ColourComputer}.
 */
@Nonnull
@CheckReturnValue
public interface ColourControl<T extends ColourComputer> {
	/**
	 * Changes the actual model.
	 * 
	 * @param model
	 *            The new {@link ColourComputer}.
	 */
	public void setModel(T model);

	/**
	 * @return The actual {@link ColourComputer}.
	 */
	public T getColourModel();
}

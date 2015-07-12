/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package ie.tcd.imm.hits.util.swing.colour;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import edu.umd.cs.findbugs.annotations.DefaultAnnotation;

/**
 * Interface of the controls for {@link ColourComputer}s.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 * @param <T>
 *            The controlled {@link ColourComputer}.
 */
@DefaultAnnotation( { Nonnull.class, CheckReturnValue.class })
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

/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package com.mind_era.knime.common.util;

import java.util.concurrent.Callable;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

/**
 * Something like {@link Iterable}, but it is more like a visitor.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 * @param <Type>
 *            The type of the interesting elements.
 * @param <State>
 *            The associated state for the interesting elements.
 */
@Nonnull
@CheckReturnValue
public interface Traversable<Type, State> {

	/**
	 * Visits all interesting elements, and calls {@code callable} in those
	 * places.
	 * 
	 * @param callable
	 *            The method to call.
	 */
	void traverse(Callable<?> callable);

	/**
	 * @return The associated state of the interesting element.
	 */
	State getState();

	/**
	 * @return The current interesting element.
	 */
	Type getElement();
}

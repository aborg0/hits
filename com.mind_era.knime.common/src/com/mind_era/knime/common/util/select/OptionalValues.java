/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package com.mind_era.knime.common.util.select;

import java.util.Collection;
import java.util.Set;

/**
 * An interface that allows you to set the actual selection of available values.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 * @param <T>
 *            The type of the possible values.
 */
public interface OptionalValues<T> {
	/**
	 * @return The active values. (Not modifiable, might be empty.)
	 */
	public Set<T> getActiveValues();

	/**
	 * @param activeValues
	 *            The new set of active values.
	 */
	public void setActiveValues(Collection<T> activeValues);
}

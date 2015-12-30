/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package com.mind_era.knime.common.view;

import java.util.List;
import java.util.Set;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

/**
 * This interface is to select a set of values from a list of values.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 * @param <Type>
 *            The type of the values.
 */
@Nonnull
@CheckReturnValue
public interface ListSelection<Type> {

	/**
	 * @return The possible values. (Not modifiable, not {@code null}.)
	 */
	public List<Type> getPossibleValues();

	/**
	 * @return The actual selection of selected values.
	 */
	public Set<Type> getSelection();

	/**
	 * Sets the possible values to {@code possibleValues}. It will preserve the
	 * selections if those are available.
	 * <p>
	 * The {@code possibleValues} may not be {@code null}.
	 * <p>
	 * This should notify the listeners if there were a change.
	 * 
	 * @param possibleValues
	 *            The new possible values of the list.
	 */
	public void setPossibleValues(List<? extends Type> possibleValues);

	/**
	 * Sets the selections to {@code selection}. If it contains values not in
	 * {@link #getPossibleValues() possible values} those (and only those) will
	 * be not selected. It may contain duplications.
	 * <p>
	 * This should notify the listeners if there were a change.
	 * 
	 * @param selection
	 *            The new selected values. (Not {@code null})
	 */
	public void setSelection(Iterable<? extends Type> selection);
}

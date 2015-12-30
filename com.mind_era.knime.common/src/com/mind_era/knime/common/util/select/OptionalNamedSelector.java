/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package com.mind_era.knime.common.util.select;

import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A {@link NamedSelector} with option to have active values.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 * @param <T>
 *            The type of the contained values.
 */
public class OptionalNamedSelector<T> extends NamedSelector<T> implements
		OptionalValues<Integer> {
	private Set<Integer> activeValues = new HashSet<Integer>();

	private Collection<MouseListener> listeners = new ArrayList<MouseListener>();

	/**
	 * @param name
	 *            The name of the object.
	 * @param valueMapping
	 *            The mapping for {@link Selectable#getValueMapping()}.
	 * @param selections
	 *            The initial selection.
	 */
	public OptionalNamedSelector(final String name,
			final Map<Integer, T> valueMapping, final Set<Integer> selections) {
		super(name, valueMapping, selections);
		activeValues.addAll(valueMapping.keySet());
	}

	/**
	 * Selects the first option.
	 * 
	 * @param name
	 *            The name of the object.
	 * @param valueMapping
	 *            The mapping for {@link Selectable#getValueMapping()}.
	 * @see #OptionalNamedSelector(String, Map, Set)
	 */
	protected OptionalNamedSelector(final String name,
			final Map<Integer, T> valueMapping) {
		super(name, valueMapping);
		activeValues.addAll(valueMapping.keySet());
	}

	/**
	 * Creates an {@link OptionalNamedSelector} with a single selection (the
	 * first one). This is a factory method.
	 * 
	 * @param <T>
	 *            Type of the contained values.
	 * @param name
	 *            The name of the selector.
	 * @param valueMapping
	 *            The mapping for {@link Selectable#getValueMapping()}.
	 * @return A new {@link OptionalNamedSelector}.
	 */
	public static <T> OptionalNamedSelector<T> createSingle(final String name,
			final Map<Integer, T> valueMapping) {
		return new OptionalNamedSelector<T>(name, valueMapping);
	}

	/**
	 * Creates a {@link OptionalNamedSelector} with a single selection (the
	 * first one). This is a factory method.
	 * 
	 * @param <T>
	 *            Type of the contained values.
	 * @param name
	 *            The name of the selector.
	 * @param vals
	 *            The values for {@link Selectable#getValueMapping()}.
	 * @return A new {@link OptionalNamedSelector}.
	 */
	public static <T> OptionalNamedSelector<T> createSingle(final String name,
			final Iterable<T> vals) {
		return OptionalNamedSelector.createSingle(name, NamedSelector
				.createValues(vals));
	}

	@Override
	public Set<Integer> getActiveValues() {
		return Collections.unmodifiableSet(activeValues);
	}

	@Override
	public void setActiveValues(final Collection<Integer> activeValues) {
		this.activeValues.clear();
		this.activeValues.addAll(activeValues);
		for (final Integer selection : getSelections()) {
			if (!this.activeValues.contains(selection - 1)) {
				deselect(selection);
			}
		}
		notifyListeners();
	}

	@Override
	public void notifyListeners() {
		super.notifyListeners();
		// Just making this public.
	}

	/**
	 * Adds {@code listener} to the controls.
	 * 
	 * @param listener
	 *            A {@link MouseListener}.
	 */
	public void addControlListener(final MouseListener listener) {
		if (!listeners.contains(listener)) {
			listeners.add(listener);
		}
	}

	/**
	 * Removes all control listeners.
	 */
	public void removeControlListeners() {
		listeners.clear();
	}

	/**
	 * Removes a specified control listener.
	 * 
	 * @param listener
	 *            A {@link MouseListener}.
	 * @return <code>true</code> iff {@code listener} was previously contained.
	 */
	public boolean removeControlListener(final MouseListener listener) {
		final boolean ret = listeners.remove(listener);
		return ret;
	}

	/**
	 * @return An unmodifiable {@link Collection} of control listeners.
	 */
	public Collection<MouseListener> getControlListeners() {
		return Collections.unmodifiableCollection(listeners);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int result = super.hashCode();
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (!super.equals(obj)) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final OptionalNamedSelector<?> other = (OptionalNamedSelector<?>) obj;
		if (activeValues == null) {
			if (other.activeValues != null) {
				return false;
			}
		} else if (!activeValues.equals(other.activeValues)) {
			return false;
		}
		return true;
	}
}

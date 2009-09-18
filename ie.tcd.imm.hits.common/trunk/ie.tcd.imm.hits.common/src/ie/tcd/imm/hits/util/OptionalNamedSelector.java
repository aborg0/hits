/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package ie.tcd.imm.hits.util;

import ie.tcd.imm.hits.util.select.OptionalValues;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * TODO Javadoc!
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
public class OptionalNamedSelector<T> extends NamedSelector<T> implements
		OptionalValues<Integer> {
	private Set<Integer> activeValues = new HashSet<Integer>();

	/**
	 * @param name
	 * @param valueMapping
	 * @param selections
	 */
	public OptionalNamedSelector(final String name,
			final Map<Integer, T> valueMapping, final Set<Integer> selections) {
		super(name, valueMapping, selections);
		activeValues.addAll(valueMapping.keySet());
	}

	protected OptionalNamedSelector(final String name,
			final Map<Integer, T> valueMapping) {
		super(name, valueMapping);
		activeValues.addAll(valueMapping.keySet());
	}

	public static <T> OptionalNamedSelector<T> createSingle(final String name,
			final Map<Integer, T> valueMapping) {
		return new OptionalNamedSelector<T>(name, valueMapping);
	}

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

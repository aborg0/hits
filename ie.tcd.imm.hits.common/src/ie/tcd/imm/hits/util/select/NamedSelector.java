/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package ie.tcd.imm.hits.util.select;

import ie.tcd.imm.hits.util.HasName;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

/**
 * A {@link HasName}, {@link Selectable} implementation.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 * @param <T>
 *            The type of the contained values.
 */
public class NamedSelector<T> extends Selector<T> implements HasName {

	private final String name;

	/**
	 * @param name
	 *            The name of the object.
	 * @param valueMapping
	 *            The mapping for {@link Selectable#getValueMapping()}.
	 * @param selections
	 *            The initial selection.
	 */
	public NamedSelector(final String name, final Map<Integer, T> valueMapping,
			final Set<Integer> selections) {
		super(valueMapping, selections);
		this.name = name;
	}

	/**
	 * Selects the first option.
	 * 
	 * @param name
	 *            The name of the object.
	 * @param valueMapping
	 *            The mapping for {@link Selectable#getValueMapping()}.
	 * @see #NamedSelector(String, Map, Set)
	 */
	protected NamedSelector(final String name,
			final Map<Integer, T> valueMapping) {
		this(name, valueMapping, valueMapping.isEmpty() ? Collections
				.<Integer> emptySet() : Collections
				.<Integer> singleton(valueMapping.keySet().iterator().next()));
	}

	/**
	 * Creates a {@link NamedSelector} with a single selection (the first one).
	 * This is a factory method.
	 * 
	 * @param <T>
	 *            Type of the contained values.
	 * @param name
	 *            The name of the selector.
	 * @param valueMapping
	 *            The mapping for {@link Selectable#getValueMapping()}.
	 * @return A new {@link NamedSelector}.
	 */
	public static <T> NamedSelector<T> createSingle(final String name,
			final Map<Integer, T> valueMapping) {
		return new NamedSelector<T>(name, valueMapping);
	}

	/**
	 * Creates a {@link NamedSelector} with a single selection (the first one).
	 * This is a factory method.
	 * 
	 * @param <T>
	 *            Type of the contained values.
	 * @param name
	 *            The name of the selector.
	 * @param values
	 *            The values for {@link Selectable#getValueMapping()}.
	 * @return A new {@link NamedSelector}.
	 */
	public static <T> NamedSelector<T> createSingle(final String name,
			final Iterable<T> values) {
		return createSingle(name, createValues(values));
	}

	/** {@inheritDoc} */
	@Override
	public String getName() {
		return name;
	}

	/**
	 * Constructs the {@link Selectable#getValueMapping() valueMapping} for
	 * {@link Selector}s, based on {@code values}.
	 * 
	 * @param <V>
	 *            Type of value objects.
	 * @param values
	 *            The values.
	 * @return A mapping from {@code 1}..{@code n} to the {@code values}.
	 */
	public static <V> LinkedHashMap<Integer, V> createValues(
			final Iterable<V> values) {
		final LinkedHashMap<Integer, V> ret = new LinkedHashMap<Integer, V>();
		int i = 1;
		for (final V v : values) {
			ret.put(Integer.valueOf(i++), v);
		}
		return ret;
	}

	/**
	 * @return The only selected value, or {@code null} if none is selected.
	 * @throws IllegalStateException
	 *             If more than {@code 1} values are selected.
	 */
	@Nullable
	public T getSelected() throws IllegalStateException {
		if (getSelections().size() > 1) {
			throw new IllegalStateException("More than one thing is selected.");
		}
		return getSelections().isEmpty() ? null : getValueMapping().get(
				getSelections().iterator().next());
	}

	/** {@inheritDoc} */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + (name == null ? 0 : name.hashCode());
		return result;
	}

	/** {@inheritDoc} */
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
		final NamedSelector<?> other = (NamedSelector<?>) obj;
		if (name == null) {
			if (other.name != null) {
				return false;
			}
		} else if (!name.equals(other.name)) {
			return false;
		}
		return true;
	}
}

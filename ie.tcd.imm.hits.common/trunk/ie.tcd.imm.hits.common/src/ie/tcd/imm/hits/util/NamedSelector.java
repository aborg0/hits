/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package ie.tcd.imm.hits.util;

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

	protected NamedSelector(final String name,
			final Map<Integer, T> valueMapping) {
		this(name, valueMapping, valueMapping.isEmpty() ? Collections
				.<Integer> emptySet() : Collections
				.<Integer> singleton(valueMapping.keySet().iterator().next()));
	}

	public static <T> NamedSelector<T> createSingle(final String name,
			final Map<Integer, T> valueMapping) {
		return new NamedSelector<T>(name, valueMapping);
	}

	public static <T> NamedSelector<T> createSingle(final String name,
			final Iterable<T> values) {
		return createSingle(name, createValues(values));
	}

	/** {@inheritDoc} */
	@Override
	public String getName() {
		return name;
	}

	public static <V> LinkedHashMap<Integer, V> createValues(
			final Iterable<V> set) {
		final LinkedHashMap<Integer, V> ret = new LinkedHashMap<Integer, V>();
		int i = 0;
		for (final V v : set) {
			ret.put(Integer.valueOf(i++), v);
		}
		return ret;
	}

	@Nullable
	public T getSelected() {
		if (getSelections().size() > 1) {
			throw new IllegalStateException("More than one thing is selected.");
		}
		return getSelections().isEmpty() ? null : getValueMapping().get(
				getSelections().iterator().next());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + (name == null ? 0 : name.hashCode());
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

/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package ie.tcd.imm.hits.util;

import java.util.Map;
import java.util.Set;

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

	/** {@inheritDoc} */
	@Override
	public String getName() {
		return name;
	}

}

/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package ie.tcd.imm.hits.util.select;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.Map.Entry;

/**
 * A simple implementation of {@link Selectable}.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 * @param <MappedValues>
 *            The type of contained values.
 */
public class Selector<MappedValues> implements Selectable<MappedValues> {

	private final Map<Integer, MappedValues> valueMapping = new HashMap<Integer, MappedValues>();
	private final Set<Integer> selections = new HashSet<Integer>();
	private final Map<ActionListener, Boolean> listeners = new WeakHashMap<ActionListener, Boolean>();

	/**
	 * A no-arg constructor for serialisation.
	 */
	protected Selector() {
		super();
	}

	/**
	 * @param valueMapping
	 *            The mapping for {@link Selectable#getValueMapping()}.
	 * @param selections
	 *            The actual selections.
	 */
	public Selector(final Map<Integer, MappedValues> valueMapping,
			final Set<Integer> selections) {
		super();
		this.valueMapping.putAll(valueMapping);
		this.selections.addAll(selections);
	}

	/**
	 * @return A {@link Map} from the constants to the values. It is
	 *         <em>not modifiable</em>!
	 */
	@Override
	public Map<Integer, MappedValues> getValueMapping() {
		return Collections.unmodifiableMap(valueMapping);
	}

	/** {@inheritDoc} */
	@Override
	public Set<Integer> getSelections() {
		return Collections.unmodifiableSet(selections);
	}

	/** {@inheritDoc} */
	@Override
	public void select(final Integer val) {
		final boolean add = selections.add(val);
		if (add) {
			notifyListeners();
		}
	}

	/** {@inheritDoc} */
	@Override
	public void selectSingle(final Integer val) {
		if (selections.size() == 1 && selections.contains(val)) {
			return;
		}
		selections.clear();
		selections.add(val);
		notifyListeners();

	}

	/** {@inheritDoc} */
	@Override
	public void deselect(final Integer val) {
		final boolean remove = selections.remove(val);
		if (remove) {
			notifyListeners();
		}
	}

	/**
	 * Notifies listeners about a change.
	 */
	protected void notifyListeners() {
		final ActionEvent event = new ActionEvent(this, (int) (System
				.currentTimeMillis() & 0xFFFFFFFF), "selectionChange");
		notifyListeners(event);
	}

	/**
	 * Notifies the listeners about a change.
	 * 
	 * @param event
	 *            The {@link ActionEvent} to use.
	 */
	protected void notifyListeners(final ActionEvent event) {
		for (final ActionListener listener : listeners.keySet()) {
			listener.actionPerformed(event);
		}
	}

	/** {@inheritDoc} */
	@Override
	public void addActionListener(final ActionListener actionListener) {
		listeners.put(actionListener, Boolean.TRUE);
	}

	/** {@inheritDoc} */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		// result = prime * result
		// + (selections == null ? 0 : selections.hashCode());
		result = prime * result
				+ (valueMapping == null ? 0 : valueMapping.hashCode());
		return result;
	}

	/** {@inheritDoc} */
	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final Selector<?> other = (Selector<?>) obj;
		if (selections == null) {
			if (other.selections != null) {
				return false;
			}
		} else if (!selections.equals(other.selections)) {
			return false;
		}
		if (valueMapping == null) {
			if (other.valueMapping != null) {
				return false;
			}
		} else if (!valueMapping.equals(other.valueMapping)) {
			return false;
		}
		return true;
	}

	/** {@inheritDoc} */
	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		for (final Entry<Integer, MappedValues> entry : valueMapping.entrySet()) {
			if (selections.contains(entry.getKey())) {
				sb.append('[').append(entry.getValue()).append(']');
			} else {
				sb.append(entry.getValue());
			}
			sb.append(", ");
		}
		if (sb.length() > 0) {
			sb.setLength(sb.length() - 2);
		}
		return sb.toString();
	}
}

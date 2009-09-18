/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package ie.tcd.imm.hits.util;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * TODO Javadoc!
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
public class Selector<MappedValues> implements Selectable<MappedValues> {

	private final Map<Integer, MappedValues> valueMapping = new HashMap<Integer, MappedValues>();
	private final Set<Integer> selections = new HashSet<Integer>();
	private final Map<ActionListener, Boolean> listeners = new WeakHashMap<ActionListener, Boolean>();

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

	@Override
	public Set<Integer> getSelections() {
		return Collections.unmodifiableSet(selections);
	}

	@Override
	public void select(final Integer val) {
		final boolean add = selections.add(val);
		if (add) {
			notifyListeners();
		}
	}

	@Override
	public void selectSingle(final Integer val) {
		if (selections.size() == 1 && selections.contains(val)) {
			return;
		}
		selections.clear();
		selections.add(val);
		notifyListeners();

	}

	@Override
	public void deselect(final Integer val) {
		final boolean remove = selections.remove(val);
		if (remove) {
			notifyListeners();
		}
	}

	protected void notifyListeners() {
		for (final ActionListener listener : listeners.keySet()) {
			listener.actionPerformed(new ActionEvent(this, (int) (System
					.currentTimeMillis() & 0xFFFFFFFF), "selectionChange"));
		}
	}

	@Override
	public void addActionListener(final ActionListener actionListener) {
		listeners.put(actionListener, Boolean.TRUE);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
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

}

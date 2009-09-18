/* All rights reserved. (C) Copyright 2009, Trinity College Dublin */
package ie.tcd.imm.hits.util;

import java.awt.event.ActionListener;
import java.util.Map;
import java.util.Set;

/**
 * TODO Javadoc!
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 * @param <MappedValues>
 *            The type of values mapped to integer values.
 */
public interface Selectable<MappedValues> {

	/**
	 * @return The selected (to view) values of the {@link Selectable}. (
	 *         <em>Not modifiable!</em>)
	 */
	public abstract Set<Integer> getSelections();

	/**
	 * Selects the value with key {@code val}.
	 * 
	 * @param val
	 *            The value key to select.
	 * @see #getSelections()
	 * @see #deselect(Integer)
	 * @see #getValueMapping()
	 */
	public abstract void select(final Integer val);

	/**
	 * Selects a single value with key: {@code val}.
	 * 
	 * @param val
	 *            A possible value. (Starting from {@code 1}).
	 */
	public abstract void selectSingle(final Integer val);

	/**
	 * Deselects the value with key {@code val}.
	 * 
	 * @param val
	 *            The value key to deselect.
	 * @see #getSelections()
	 * @see #select(Integer)
	 * @see #getValueMapping()
	 */
	public abstract void deselect(final Integer val);

	/**
	 * @return A {@link Map} from the constants to the values. It is
	 *         <em>not modifiable</em>!
	 */
	public Map<Integer, MappedValues> getValueMapping();

	/**
	 * Adds an {@link ActionListener}.
	 * 
	 * @param actionListener
	 *            An {@link ActionListener} to notify the listeners about
	 *            changes.
	 */
	public void addActionListener(ActionListener actionListener);

}
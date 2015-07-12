/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package ie.tcd.imm.hits.view.util;

import ie.tcd.imm.hits.common.Format;
import ie.tcd.imm.hits.util.Pair;
import ie.tcd.imm.hits.util.swing.VariableControl;

import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import javax.swing.AbstractButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.border.BevelBorder;
import javax.swing.border.EmptyBorder;

import org.knime.core.data.property.ColorAttr;

/**
 * A {@link VariableControl} that allows to select well(s) on a plate.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
public class SimpleWellSelection extends JPanel {
	private static final long serialVersionUID = 7969161552427878759L;
	private static final boolean INCLUDE_INDICATOR = true;
	private final Map<String, Map<Integer, AbstractButton>> buttons = new LinkedHashMap<String, Map<Integer, AbstractButton>>();
	// private final Format format;
	private String selection = "A1";

	private Set<String> hilites = new HashSet<String>();

	private final List<ActionListener> listeners = new ArrayList<ActionListener>();

	/**
	 * @param format
	 *            The {@link Format} of plate.
	 */
	public SimpleWellSelection(final Format format) {
		super();
		// this.format = format;
		removeAll();
		setLayout(new GridLayout(format.getRow() + 1, format.getCol() + 1));
		add(new JLabel("\u00a0"));
		for (int j = 0; j < format.getCol(); j++) {
			add(new JLabel(Integer.toString(j + 1)));
		}
		for (int i = 0; i < format.getRow(); i++) {
			final String row = ie.tcd.imm.hits.util.Misc.toUpperLetter(Integer
					.toString(i + 1));
			buttons.put(row, new LinkedHashMap<Integer, AbstractButton>());
			add(new JLabel(row));
			for (int j = 0; j < format.getCol(); j++) {
				final AbstractButton button = new JToggleButton("\u00a0");
				button
						.addActionListener(new ButtonActionListener(row
								+ (j + 1)));
				buttons.get(row).put(Integer.valueOf(j), button);
				add(button);
			}
		}

		updateComponent();
	}

	/**
	 * 
	 */
	public void updateComponent() {
		for (final Entry<String, Map<Integer, AbstractButton>> entry : buttons
				.entrySet()) {
			final Map<Integer, AbstractButton> map = entry.getValue();
			for (final Entry<Integer, AbstractButton> buttonEntry : map
					.entrySet()) {
				final String key = entry.getKey()
						+ (buttonEntry.getKey().intValue() + 1);
				final boolean selected = selection.equals(key);
				buttonEntry.getValue()
						.setSelected(INCLUDE_INDICATOR ^ selected);
				final boolean hilited = hilites.contains(key);
				final Color color = selected ? (hilited ? ColorAttr.SELECTED_HILITE
						: ColorAttr.SELECTED)
						: hilited ? ColorAttr.HILITE : ColorAttr.DEFAULT
								.getColor();
				buttonEntry.getValue().setForeground(color);
				buttonEntry.getValue().setBackground(color);
				buttonEntry.getValue().setBorder(
						hilited ? new BevelBorder(BevelBorder.RAISED, color,
								color) : new EmptyBorder(1, 1, 1, 1));
			}
		}
	}

	//
	// protected void setEnabledComponents(final boolean enabled) {
	// for (final Map<Integer, ? extends AbstractButton> outerEntry : buttons
	// .values()) {
	// for (final AbstractButton button : outerEntry.values()) {
	// button.setEnabled(enabled);
	// }
	// }
	// }

	/**
	 * @return The selection.
	 */
	public String getSelection() {
		return selection;
	}

	/**
	 * @param selection
	 *            The selection to set.
	 */
	protected void setSelection(final String selection) {
		this.selection = selection;
		updateComponent();
		notifyListeners();
	}

	/**
	 * Add a new {@link ActionListener}.
	 * 
	 * @param listener
	 *            An {@link ActionListener}.
	 */
	public void addActionListener(final ActionListener listener) {
		listeners.add(listener);
	}

	/**
	 * Removes the selected {@link ActionListener}.
	 * 
	 * @param listener
	 *            An {@link ActionListener}.
	 * @return {@code true}, if it was previously contained.
	 */
	public boolean removeActionListener(final ActionListener listener) {
		return listeners.remove(listener);
	}

	/**
	 * Removes all {@link ActionListener}s.
	 */
	public void removeAllActionListeners() {
		listeners.clear();
	}

	/**
	 * The listener for the buttons.
	 */
	private class ButtonActionListener implements ActionListener {

		private final String value;

		/**
		 * @param value
		 *            The associated well name.
		 */
		private ButtonActionListener(final String value) {
			this.value = value;
		}

		@Override
		public void actionPerformed(final ActionEvent e) {
			final AbstractButton button = (AbstractButton) e.getSource();
			final HashSet<String> newSel = new HashSet<String>(Collections
					.singleton(selection));
			if (button.isSelected() ^ !INCLUDE_INDICATOR) {
				newSel.add(value);
			} else {
				newSel.remove(value);
			}
			if (!newSel.isEmpty()) {
				setSelection(value);
			} else if (newSel.isEmpty()) {
				button.setSelected(INCLUDE_INDICATOR);
			}
		}
	}

	/**
	 * Notifies all {@link ActionListener}s.
	 */
	public void notifyListeners() {
		for (final ActionListener listener : listeners) {
			listener.actionPerformed(null);
		}
	}

	/**
	 * Updates the component reflecting the new selection of HiLites.
	 * 
	 * @param select
	 *            The new selection of HiLites.
	 */
	public void updateHiLites(final Set<Pair<String, Integer>> select) {
		hilites.clear();
		for (final Pair<String, Integer> pair : select) {
			hilites.add(pair.getLeft() + pair.getRight());
		}
		updateComponent();
	}
}

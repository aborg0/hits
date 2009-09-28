/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package ie.tcd.imm.hits.view.util;

import ie.tcd.imm.hits.common.Format;
import ie.tcd.imm.hits.util.swing.VariableControl;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.AbstractButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

/**
 * A {@link VariableControl} that allows to select well(s) on a plate.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
public class SimpleWellSelection extends JPanel {
	private static final long serialVersionUID = 7969161552427878759L;
	private static final boolean INCLUDE_INDICATOR = true;
	private final Map<String, Map<Integer, AbstractButton>> buttons = new LinkedHashMap<String, Map<Integer, AbstractButton>>();
	private final Format format;
	private String selection = "A1";

	private final List<ActionListener> listeners = new ArrayList<ActionListener>();

	/**
	 * @param format
	 * @param model
	 * @param selectionType
	 * @param controlsHandler
	 * @param changeListener
	 * 
	 */
	public SimpleWellSelection(final Format format) {
		super();
		this.format = format;
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
				buttonEntry.getValue().setSelected(
						INCLUDE_INDICATOR
								^ selection
										.equals(entry.getKey()
												+ (buttonEntry.getKey()
														.intValue() + 1)));
			}
		}
	}

	protected void setEnabledComponents(final boolean enabled) {
		for (final Map<Integer, ? extends AbstractButton> outerEntry : buttons
				.values()) {
			for (final AbstractButton button : outerEntry.values()) {
				button.setEnabled(enabled);
			}
		}
	}

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

	public void addActionListener(final ActionListener listener) {
		listeners.add(listener);
	}

	public boolean removeActionListener(final ActionListener listener) {
		return listeners.remove(listener);
	}

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
		 * @param listSelection
		 * @param selectionType
		 */
		private ButtonActionListener(final String value) {
			this.value = value;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent
		 * )
		 */
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
				// buttons.get(String.valueOf(selection.charAt(0))).get(
				// Integer.valueOf(selection.substring(1)) - 1)
				// .setSelected(!INCLUDE_INDICATOR);
				// button.setSelected(INCLUDE_INDICATOR);
				setSelection(value);
			} else if (newSel.isEmpty()) {
				button.setSelected(INCLUDE_INDICATOR);
			}
		}
	}

	/**
	 * 
	 */
	public void notifyListeners() {
		for (final ActionListener listener : listeners) {
			listener.actionPerformed(null);
		}
	}
}

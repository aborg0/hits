/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package com.mind_era.knime.common.util;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.Serializable;
import java.lang.reflect.Method;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JList;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.defaultnodesettings.SettingsModelFilterString;
import org.knime.core.node.defaultnodesettings.SettingsModelStringArray;

/**
 * When attached to an {@link ActionEvent} emitter (like a {@link JButton}), it
 * will move the selection(s) of the {@link #list referenced} {@link JList} up
 * or down (by {@link #move}).
 * <p>
 * The {@link #list} must has the {@link DefaultListModel}, {@link #model}.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
@NotThreadSafe
@Nonnull
public class SelectionMoverActionListener<T> implements ActionListener,
		Serializable {
	private static final long serialVersionUID = -2883537212024563886L;
	private final JList<T> list;
	private final DefaultListModel<T> model;
	private final int move;
	private final SettingsModel settingsModel;

	/**
	 * 
	 * @param list
	 *            A {@link JList} for which selections we listen to.
	 * @param model
	 *            The {@link DefaultListModel} of {@code list}.
	 * @param move
	 *            We move the selection by this amount.
	 * @param settingsModel
	 *            The {@link SettingsModelFilterString}, or
	 *            {@link SettingsModelStringArray} for the list of values.
	 */
	public SelectionMoverActionListener(final JList<T> list,
			final DefaultListModel<T> model, final int move,
			final SettingsModel settingsModel) {
		super();
		this.list = list;
		this.model = model;
		this.move = move;
		this.settingsModel = settingsModel;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	/**
	 * Moves the selection up or down.
	 * 
	 * {@inheritDoc}
	 */
	@Override
	public void actionPerformed(final ActionEvent e) {
		final int[] selectedIndices = list.getSelectedIndices();
		final int max = model.getSize();
		if (selectedIndices.length > 0
				&& (move > 0
						&& selectedIndices[selectedIndices.length - 1] + move < max || move < 0
						&& selectedIndices[0] + move >= 0)) {
			final int sgn = move > 0 ? 1 : -1;
			final int start = sgn == -1 ? 0 : selectedIndices.length - 1;
			final int end = sgn == -1 ? selectedIndices.length : -1;
			// for (final int selected : selectedIndices) {
			for (int j = start; j != end; j -= sgn) {
				final int selected = selectedIndices[j];
				final T tmp = model.get(selected);
				for (int i = move; i != 0; i -= sgn) {
					model.setElementAt(model.get(selected + move), selected
							+ move - sgn);
				}
				model.setElementAt(tmp, selected + move);
			}
			final String[] newValues = new String[model.size()];
			for (int i = model.size(); i-- > 0;) {
				final Object elem = model.getElementAt(i);
				if (elem instanceof DataColumnSpec) {
					final DataColumnSpec spec = (DataColumnSpec) elem;
					newValues[i] = spec.getName();
				} else if (elem instanceof String) {
					final String str = (String) elem;
					newValues[i] = str;
				} else {
					throw new UnsupportedOperationException(
							"Not supported type: " + elem.getClass());
				}
			}
			if (settingsModel instanceof SettingsModelFilterString) {
				final SettingsModelFilterString m = (SettingsModelFilterString) settingsModel;
				m.setIncludeList(newValues);
			} else if (settingsModel instanceof SettingsModelStringArray) {
				final SettingsModelStringArray m = (SettingsModelStringArray) settingsModel;
				m.setStringArrayValue(newValues);
			} else {
				// Do nothing, no change will be notified.
			}
			try {
				final Method method = SettingsModel.class
						.getDeclaredMethod("notifyChangeListeners");
				method.setAccessible(true);
				final Object result = method.invoke(settingsModel);
				assert result == null;
			} catch (final ReflectiveOperationException e1) {
				// Do nothing, no change will be notified.
			}
			for (int i = 0; i < selectedIndices.length; i++) {
				selectedIndices[i] += move;
			}
			list.setSelectedIndices(selectedIndices);
		}
	}
}

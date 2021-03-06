/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package com.mind_era.knime.common.view.impl;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseListener;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.event.ChangeListener;

import org.knime.core.node.defaultnodesettings.SettingsModel;

import com.mind_era.knime.common.util.select.Selectable;
import com.mind_era.knime.common.util.swing.SelectionType;
import com.mind_era.knime.common.util.swing.VariableControl;
import com.mind_era.knime.common.view.ControlsHandler;
import com.mind_era.knime.common.view.ListSelection;

/**
 * A {@link VariableControl} with {@link VariableControl.ControlTypes#ComboBox}.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 * @param <Model>
 *            Type of the model for values.
 * @param <Sel>
 *            The type of the container of {@code Model}s.
 */
@Nonnull
@CheckReturnValue
public class ComboBoxControl<Model, Sel extends Selectable<Model>> extends
		AbstractVariableControl<Model, Sel> {
	private final JComboBox<String> combobox = new JComboBox<String>(new DefaultComboBoxModel<String>());

	/**
	 * @param model
	 *            The {@link SettingsModelListSelection}.
	 * @param selectionType
	 *            The initial {@link SelectionType}.
	 * @param controlsHandler
	 *            The {@link ControlsHandler} instance.
	 * @param changeListener
	 *            The {@link ChangeListener} associated to the {@code model}.
	 * @param domainModel
	 *            The model for possible parameters and selections.
	 */
	public ComboBoxControl(final SettingsModelListSelection model,
			final SelectionType selectionType,
			final ControlsHandler<SettingsModel, Model, Sel> controlsHandler,
			final ChangeListener changeListener, final Sel domainModel) {
		super(model, selectionType, controlsHandler, changeListener,
				domainModel);
		switch (selectionType) {
		case MultipleAtLeastOne:
		case MultipleOrNone:
			if (model.getSelection().size() > 1) {
				model.setSelection(Collections.singleton(model.getSelection()
						.iterator().next()));
			}
			break;
		// throw new UnsupportedOperationException(
		// "Not supported selection type: " + selectionType);
		case Single:
			if (model.getSelection().size() > 1) {
				model.setSelection(Collections.singleton(model.getSelection()
						.iterator().next()));
			}
			break;
		case Unmodifiable:
			break;
		default:
			throw new UnsupportedOperationException(
					"Not supported selection type: " + selectionType);
		}
		combobox.setName(model.getConfigName());
		updateComponent();
		combobox.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(final ItemEvent e) {
				final Set<String> newSelection = Collections
						.singleton((String) combobox.getSelectedItem());
				if (selectionType != SelectionType.Unmodifiable) {
					if (!newSelection.equals(model.getSelection())) {
						model.setSelection(newSelection);
					}
				}
				// updateComponent();
			}
		});
		getPanel().add(combobox);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.knime.core.node.defaultnodesettings.DialogComponent#setEnabledComponents
	 * (boolean)
	 */
	@Override
	protected void setEnabledComponents(final boolean enabled) {
		combobox.setEnabled(enabled);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * ie.tcd.imm.hits.knime.view.impl.AbstractVariableControl#updateComponent()
	 */
	@Override
	protected void updateComponent() {
		@SuppressWarnings("unchecked")
		final ListSelection<String> model = (ListSelection<String>) getModel();
		final List<String> possibleValues = model.getPossibleValues();
		final DefaultComboBoxModel<String> comboBoxModel = (DefaultComboBoxModel<String>) combobox
				.getModel();
		final LinkedList<String> comboElements = new LinkedList<String>();
		for (int i = comboBoxModel.getSize(); i-- > 0;) {
			comboElements.addFirst((String) comboBoxModel.getElementAt(i));
		}
		if (!possibleValues.equals(comboElements)) {
			comboBoxModel.removeAllElements();
			for (final String value : possibleValues) {
				combobox.addItem(value);
			}
		}
		final Set<String> selection = model.getSelection();
		assert selection.size() == 1;
		if (combobox.getSelectedItem() != selection.iterator().next()) {
			combobox.setSelectedItem(selection.iterator().next());
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ie.tcd.imm.hits.knime.view.impl.AbstractVariableControl#getType()
	 */
	@Override
	public ControlTypes getType() {
		return ControlTypes.ComboBox;
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
		result = prime * result + (combobox == null ? 0 : combobox.hashCode());
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
		final ComboBoxControl<?, ?> other = (ComboBoxControl<?, ?>) obj;
		if (combobox == null) {
			if (other.combobox != null) {
				return false;
			}
		} else if (combobox != other.combobox) {
			return false;
		}
		return true;
	}

	@Override
	protected void notifyChange(final MouseListener listener,
			final Change change) {
		switch (change) {
		case add:
			combobox.addMouseListener(listener);
			break;
		case remove:
			combobox.removeMouseListener(listener);
			break;
		default:
			throw new IllegalStateException("Not supported change type.");
		}
		super.notifyChange(listener, change);
	}
}

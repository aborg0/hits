/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package ie.tcd.imm.hits.knime.view.impl;

import ie.tcd.imm.hits.knime.view.ControlsHandler;
import ie.tcd.imm.hits.knime.view.ListSelection;
import ie.tcd.imm.hits.util.swing.SelectionType;
import ie.tcd.imm.hits.util.swing.VariableControl;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JToggleButton;
import javax.swing.event.ChangeListener;

import org.knime.core.node.defaultnodesettings.SettingsModel;

/**
 * A {@link VariableControl} with {@link VariableControl.ControlTypes#Buttons}.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 * @param <Model>
 *            Type of the model for values.
 */
public class ButtonsControl<Model> extends AbstractVariableControl<Model> {
	/**
	 * The listener for the buttons.
	 */
	private static class ButtonActionListener implements ActionListener {

		private final String value;
		private final ListSelection<String> listSelection;
		private final SelectionType selectionType;

		private ButtonActionListener(final String value,
				final ListSelection<String> listSelection,
				final SelectionType selectionType) {
			this.value = value;
			this.listSelection = listSelection;
			this.selectionType = selectionType;
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
			final JToggleButton button = (JToggleButton) e.getSource();
			final HashSet<String> newSel = new HashSet<String>(listSelection
					.getSelection());
			switch (selectionType) {
			case Unmodifiable:
				return;
			case Single:
				listSelection.setSelection(Collections.singletonList(value));
				return;
			case MultipleAtLeastOne:
			case MultipleOrNone:
			default:
				break;
			}
			if (button.isSelected() ^ !INCLUDE_INDICATOR) {
				newSel.add(value);
			} else {
				newSel.remove(value);
			}
			if (!newSel.isEmpty()
					|| selectionType == SelectionType.MultipleOrNone) {
				listSelection.setSelection(newSel);
			} else if (newSel.isEmpty()) {
				button.setSelected(INCLUDE_INDICATOR);
			}
		}
	}

	private static final boolean INCLUDE_INDICATOR = true;

	private final List<JToggleButton> buttons = new ArrayList<JToggleButton>();
	// private ButtonGroup group = new ButtonGroup();

	/**
	 * The actual focus is on the button with this label. May be {@code null}.
	 */
	private String currentFocus;

	/**
	 * @param model
	 *            A {@link SettingsModelListSelection } to store the preferences.
	 * @param selectionType
	 *            The {@link SelectionType} for this control.
	 * @param controlsHandler
	 *            The {@link ControlsHandler} for the possible transformations.
	 * @param changeListener
	 *            The {@link ChangeListener} associated to the {@code model}.
	 */
	public ButtonsControl(final SettingsModelListSelection model,
			final SelectionType selectionType,
			final ControlsHandler<SettingsModel, Model> controlsHandler,
			final ChangeListener changeListener) {
		super(model, selectionType, controlsHandler, changeListener);
		updateComponent();
		// getModel().addChangeListener(new ChangeListener() {
		//
		// @Override
		// public void stateChanged(final ChangeEvent e) {
		// updateComponent();
		// }
		// });
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
		getModel().setEnabled(enabled);
		updateComponent();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.knime.core.node.defaultnodesettings.DialogComponent#updateComponent()
	 */
	@Override
	protected void updateComponent() {
		// for (final JToggleButton button : buttons) {
		// group.remove(button);
		// }
		buttons.clear();
		final List<String> values = ((SettingsModelListSelection) getModel())
				.getPossibleValues();
		final Set<String> selections = ((SettingsModelListSelection) getModel())
				.getSelection();
		for (final String value : values) {
			final JToggleButton toggleButton = new JToggleButton(value,
					selections.contains(value) ^ !INCLUDE_INDICATOR);
			toggleButton.setName(value);
			buttons.add(toggleButton);
			// group.add(toggleButton);
		}
		super.updateComponent();
		@SuppressWarnings("unchecked")
		final ListSelection<String> model = (ListSelection<String>) getModel();
		for (final JToggleButton button : buttons) {
			getPanel().add(button);
			button.setEnabled(getModel().isEnabled());
			final String value = button.getText();
			button.addActionListener(new ButtonActionListener(value, model,
					getSelectionType()));
			if (value.equals(currentFocus)) {
				button.grabFocus();
			}
			button.addFocusListener(new FocusListener() {
				@Override
				public void focusLost(final FocusEvent e) {
					if (currentFocus.equals(value)) {
						currentFocus = null;
					}
				}

				@Override
				public void focusGained(final FocusEvent e) {
					currentFocus = value;
				}
			});
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ie.tcd.imm.hits.knime.view.impl.AbstractVariableControl#getType()
	 */
	@Override
	public ControlTypes getType() {
		return ControlTypes.Buttons;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int result = super.hashCode();
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
		final ButtonsControl<?> other = (ButtonsControl<?>) obj;
		if (buttons == null) {
			if (other.buttons != null) {
				return false;
			}
		} else if (buttons != other.buttons) {
			return false;
		}
		if (currentFocus == null) {
			if (other.currentFocus != null) {
				return false;
			}
		} else if (!currentFocus.equals(other.currentFocus)) {
			return false;
		}
		return true;
	}
}

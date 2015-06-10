/**
 * 
 */
package ie.tcd.imm.hits.knime.view.impl;

import ie.tcd.imm.hits.knime.view.ControlsHandler;
import ie.tcd.imm.hits.knime.view.ListSelection;
import ie.tcd.imm.hits.util.swing.SelectionType;
import ie.tcd.imm.hits.util.swing.VariableControl;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
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

import edu.umd.cs.findbugs.annotations.DefaultAnnotation;

/**
 * A {@link VariableControl} with {@link VariableControl.ControlTypes#ComboBox}.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
@DefaultAnnotation( { Nonnull.class, CheckReturnValue.class })
class ComboBoxControl extends AbstractVariableControl {
	private final JComboBox combobox = new JComboBox(new DefaultComboBoxModel());

	/**
	 * @param model
	 * @param selectionType
	 * @param controlsHandler
	 * @param changeListener
	 *            The {@link ChangeListener} associated to the {@code model}.
	 */
	public ComboBoxControl(final SettingsModelListSelection model,
			final SelectionType selectionType,
			final ControlsHandler<SettingsModel> controlsHandler,
			final ChangeListener changeListener) {
		super(model, selectionType, controlsHandler, changeListener);
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
	 * @see org.knime.core.node.defaultnodesettings.DialogComponent#setEnabledComponents(boolean)
	 */
	@Override
	protected void setEnabledComponents(final boolean enabled) {
		combobox.setEnabled(enabled);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ie.tcd.imm.hits.knime.view.impl.AbstractVariableControl#updateComponent()
	 */
	@Override
	protected void updateComponent() {
		@SuppressWarnings("unchecked")
		final ListSelection<String> model = (ListSelection<String>) getModel();
		final List<String> possibleValues = model.getPossibleValues();
		final DefaultComboBoxModel comboBoxModel = (DefaultComboBoxModel) combobox
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
		result = prime * result
				+ ((combobox == null) ? 0 : combobox.hashCode());
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
		final ComboBoxControl other = (ComboBoxControl) obj;
		if (combobox == null) {
			if (other.combobox != null) {
				return false;
			}
		} else if (combobox != other.combobox) {
			return false;
		}
		return true;
	}

}
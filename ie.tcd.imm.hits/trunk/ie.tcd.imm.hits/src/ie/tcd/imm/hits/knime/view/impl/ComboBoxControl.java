/**
 * 
 */
package ie.tcd.imm.hits.knime.view.impl;

import ie.tcd.imm.hits.knime.view.ListSelection;
import ie.tcd.imm.hits.util.swing.SelectionType;
import ie.tcd.imm.hits.util.swing.VariableControl;
import ie.tcd.imm.hits.util.swing.VariableControl.ControlTypes;

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

import edu.umd.cs.findbugs.annotations.DefaultAnnotation;

/**
 * A {@link VariableControl} with {@link ControlTypes#ComboBox}.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
@DefaultAnnotation( { Nonnull.class, CheckReturnValue.class })
class ComboBoxControl extends AbstractVariableControl {
	private static final long serialVersionUID = -9025497242849755048L;

	private final JComboBox combobox = new JComboBox(new DefaultComboBoxModel());

	/**
	 * @param model
	 * @param selectionType
	 */
	public ComboBoxControl(final SettingsModelListSelection model,
			final SelectionType selectionType) {
		super(model, selectionType);
		switch (selectionType) {
		case MultipleAtLeastOne:
		case MultipleOrNone:
			throw new UnsupportedOperationException(
					"Not supported selection type: " + selectionType);
		case Single:
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
				if (selectionType != SelectionType.Unmodifiable) {
					model.setSelection(Collections.singleton((String) combobox
							.getSelectedItem()));
				}
				updateComponent();
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
		combobox.setSelectedItem(selection.iterator().next());
	}
}

/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package ie.tcd.imm.hits.view.util;

import ie.tcd.imm.hits.common.Format;
import ie.tcd.imm.hits.knime.view.ControlsHandler;
import ie.tcd.imm.hits.knime.view.ListSelection;
import ie.tcd.imm.hits.knime.view.impl.AbstractVariableControl;
import ie.tcd.imm.hits.knime.view.impl.SettingsModelListSelection;
import ie.tcd.imm.hits.util.select.Selectable;
import ie.tcd.imm.hits.util.select.Selector;
import ie.tcd.imm.hits.util.swing.SelectionType;
import ie.tcd.imm.hits.util.swing.VariableControl;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseListener;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.AbstractButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.event.ChangeListener;

import org.knime.core.node.defaultnodesettings.SettingsModel;

/**
 * A {@link VariableControl} that allows to select well(s) on a plate.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 * @param <Model>
 *            The type of model used for values.
 * @param <Sel>
 *            The type of model of selections for those values.
 */
public class WellSelectionWidget<Model, Sel extends Selectable<Model>> extends
		AbstractVariableControl<Model, Sel> {
	private static final long serialVersionUID = 7969161552427878759L;
	private final Map<String, Map<Integer, AbstractButton>> buttons = new LinkedHashMap<String, Map<Integer, AbstractButton>>();

	// private final Format format;

	/**
	 * @param format
	 *            The {@link Format} to use.
	 * @param model
	 *            The inner {@link SettingsModelListSelection}.
	 * @param selectionType
	 *            The {@link SelectionType}.
	 * @param controlsHandler
	 *            The {@link ControlsHandler} where it will be registered.
	 * @param changeListener
	 *            The {@link ChangeListener} to add first.
	 * @param domainModel
	 *            The {@link Selector} model.
	 */
	public WellSelectionWidget(final Format format,
			final SettingsModelListSelection model,
			final SelectionType selectionType,
			final ControlsHandler<SettingsModel, Model, Sel> controlsHandler,
			final ChangeListener changeListener, final Sel domainModel) {
		super(model, selectionType, controlsHandler, changeListener,
				domainModel);
		// this.format = format;
		final JPanel view = getView();
		view.removeAll();
		view
				.setLayout(new GridLayout(format.getRow() + 1,
						format.getCol() + 1));
		view.add(new JLabel("\u00a0"));
		for (int j = 0; j < format.getCol(); j++) {
			view.add(new JLabel(Integer.toString(j + 1)));
		}
		for (int i = 0; i < format.getRow(); i++) {
			final String row = ie.tcd.imm.hits.util.Misc.toUpperLetter(Integer
					.toString(i + 1));
			buttons.put(row, new LinkedHashMap<Integer, AbstractButton>());
			view.add(new JLabel(row));
			for (int j = 0; j < format.getCol(); j++) {
				final AbstractButton button = new JToggleButton("\u00a0");
				button.addActionListener(new ButtonActionListener(
						row + (j + 1), (SettingsModelListSelection) getModel(),
						selectionType));
				for (final MouseListener l : getListeners()) {
					button.addMouseListener(l);
				}
				buttons.get(row).put(Integer.valueOf(j), button);
				view.add(button);
			}
		}

		updateComponent();
	}

	@Override
	protected void setEnabledComponents(final boolean enabled) {
		for (final Map<Integer, ? extends AbstractButton> outerEntry : buttons
				.values()) {
			for (final AbstractButton button : outerEntry.values()) {
				button.setEnabled(enabled);
			}
		}
	}

	@Override
	public ie.tcd.imm.hits.util.swing.VariableControl.ControlTypes getType() {
		return ControlTypes.Buttons;
	}

	@Override
	protected void updateComponent() {
	}

	private static final boolean INCLUDE_INDICATOR = true;

	/**
	 * The listener for the buttons.
	 */
	private static class ButtonActionListener implements ActionListener {

		private final String value;
		private final ListSelection<String> listSelection;
		private final SelectionType selectionType;

		/**
		 * @param value
		 *            The associated value.
		 * @param listSelection
		 *            The {@link ListSelection} model.
		 * @param selectionType
		 *            The {@link SelectionType}.
		 */
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
			final AbstractButton button = (AbstractButton) e.getSource();
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
}

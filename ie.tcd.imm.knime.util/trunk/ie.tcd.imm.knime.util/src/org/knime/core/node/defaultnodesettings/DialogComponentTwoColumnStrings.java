/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package org.knime.core.node.defaultnodesettings;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;

/**
 * A {@link DialogComponent} to handle two disjoint lists. It is designed for
 * {@link SettingsModelFilterString}s.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
public class DialogComponentTwoColumnStrings extends DialogComponent {

	private final JList includeList = new JList(new DefaultListModel());
	private final JList excludeList = new JList(new DefaultListModel());

	/**
	 * The action to move the selection from a {@link JList} to the other.
	 */
	private final class ChangeAction extends AbstractAction {
		private static final long serialVersionUID = 5388523081226805353L;
		private final JList fromList;
		private final JList toList;

		/**
		 * Constructs {@link ChangeAction}.
		 * 
		 * @param name
		 *            The name of the action.
		 * @param fromList
		 *            The list to move from the selected elements.
		 * @param toList
		 *            The list to move to the elements.
		 */
		public ChangeAction(final String name, final JList fromList,
				final JList toList) {
			super(name);
			this.fromList = fromList;
			this.toList = toList;
		}

		@Override
		public void actionPerformed(final ActionEvent e) {
			final Object[] selectedValues = fromList.getSelectedValues();
			if (selectedValues.length > 0) {
				for (final Object obj : selectedValues) {
					((DefaultListModel) toList.getModel()).addElement(obj);
					((DefaultListModel) fromList.getModel()).removeElement(obj);
				}
			}
			updateModel();
		}
	}

	/**
	 * Constructs a {@link DialogComponentTwoColumnStrings} with {@code model}
	 * and the preferred titles.
	 * 
	 * @param model
	 *            A {@link SettingsModelFilterString}.
	 * @param includeTitle
	 *            The title of the right column (the {@code model}'s include
	 *            list).
	 * @param excludeTitle
	 *            The title of the left column (the {@code model}'s exclude
	 *            list).
	 */
	public DialogComponentTwoColumnStrings(
			final SettingsModelFilterString model, final String includeTitle,
			final String excludeTitle) {
		super(model);
		getComponentPanel().setLayout(new GridLayout(1, 3));
		getComponentPanel().add(excludeList);
		final JPanel buttonPanel = new JPanel();
		final ChangeAction rightToLeftAction = new ChangeAction(
				"<html>&lArr;</html>", includeList, excludeList);
		final ChangeAction leftToRightAction = new ChangeAction(
				"<html>&rArr;</html>", excludeList, includeList);
		final JButton toRightButton = new JButton(leftToRightAction);
		final JButton toLeftButton = new JButton(rightToLeftAction);
		buttonPanel.add(toLeftButton);
		buttonPanel.add(toRightButton);
		getComponentPanel().add(buttonPanel);
		includeList.setBorder(new TitledBorder(includeTitle));
		getComponentPanel().add(includeList);
		excludeList.setBorder(new TitledBorder(excludeTitle));
		updateComponent();
	}

	@Override
	protected void checkConfigurabilityBeforeLoad(final PortObjectSpec[] specs)
			throws NotConfigurableException {
		// TODO Auto-generated method stub

	}

	@Override
	protected void setEnabledComponents(final boolean enabled) {
		includeList.setEnabled(enabled);
		excludeList.setEnabled(enabled);
	}

	@Override
	public void setToolTipText(final String text) {
		getComponentPanel().setToolTipText(text);
		includeList.setToolTipText(text);
		excludeList.setToolTipText(text);
	}

	@Override
	protected void updateComponent() {
		{
			int i = 0;
			((DefaultListModel) includeList.getModel()).clear();
			for (final String include : ((SettingsModelFilterString) getModel())
					.getIncludeList()) {
				((DefaultListModel) includeList.getModel()).add(i++, include);
			}
		}
		{
			((DefaultListModel) excludeList.getModel()).clear();
			int i = 0;
			for (final String exclude : ((SettingsModelFilterString) getModel())
					.getExcludeList()) {
				((DefaultListModel) excludeList.getModel()).add(i++, exclude);
			}
		}
		includeList.repaint();
		excludeList.repaint();

	}

	/**
	 * Updates the model based on the actual values of the UI.
	 */
	protected void updateModel() {
		final SettingsModelFilterString model = (SettingsModelFilterString) getModel();
		final DefaultListModel includeModel = (DefaultListModel) includeList
				.getModel();
		final ArrayList<String> include = new ArrayList<String>(includeModel
				.getSize());
		fill(includeModel, include);
		final DefaultListModel excludeModel = (DefaultListModel) excludeList
				.getModel();
		final ArrayList<String> exclude = new ArrayList<String>(excludeModel
				.getSize());
		fill(excludeModel, exclude);
		model.setIncludeList(include);
		model.setExcludeList(exclude);
	}

	/**
	 * Gets the values from {@code model} to {@code list}.
	 * 
	 * @param model
	 *            A {@link DefaultListModel}.
	 * @param list
	 *            The result list (should be modifiable).
	 */
	private void fill(final DefaultListModel model, final List<String> list) {
		for (int i = 0; i < model.size(); ++i) {
			list.add((String) model.get(i));
		}
	}

	@Override
	protected void validateSettingsBeforeSave() throws InvalidSettingsException {
		// TODO Auto-generated method stub

	}

	/**
	 * Sets the values that are included in one of the two columns. The values
	 * previously not present will be added to the left/excluded list.
	 * 
	 * @param possibleValues
	 *            The possible values of the columns.
	 */
	public void setAllPossibleValues(final Set<String> possibleValues) {
		filterModel(possibleValues, (DefaultListModel) includeList.getModel());
		final DefaultListModel model = (DefaultListModel) excludeList
				.getModel();
		filterModel(possibleValues, model);
		for (final String string : possibleValues) {
			if (!((DefaultListModel) includeList.getModel()).contains(string)
					&& !model.contains(string)) {
				model.add(model.getSize(), string);
			}
		}
		updateModel();
	}

	/**
	 * Filters the values of {@code listModel} to contain values only from
	 * {@code possibleValues}.
	 * 
	 * @param possibleValues
	 *            A {@link Set} of possible values.
	 * @param listModel
	 *            A {@link DefaultListModel}.
	 */
	private void filterModel(final Set<String> possibleValues,
			final DefaultListModel listModel) {
		for (int i = listModel.size(); i-- > 0;) {
			if (!possibleValues.contains(listModel.get(i))) {
				listModel.remove(i);
			}
		}
	}
}

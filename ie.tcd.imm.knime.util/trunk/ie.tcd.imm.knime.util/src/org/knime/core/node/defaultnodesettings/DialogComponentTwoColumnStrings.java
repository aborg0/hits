/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package org.knime.core.node.defaultnodesettings;

import java.util.ArrayList;
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
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
public class DialogComponentTwoColumnStrings extends DialogComponent {

	private final JList includeList = new JList(new DefaultListModel());
	private final JList excludeList = new JList(new DefaultListModel());

	private final class ChangeAction extends AbstractAction {

		/**  */
		private static final long serialVersionUID = 5388523081226805353L;
		private JButton toButton;
		private JButton fromButton;
		private final JList fromList;
		private final JList toList;

		/**
		 * @param name
		 * @param excludeList
		 * @param includeList
		 */
		public ChangeAction(final String name, final JList fromList,
				final JList toList) {
			super(name);
			this.fromList = fromList;
			this.toList = toList;
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
			final Object[] selectedValues = fromList.getSelectedValues();
			if (selectedValues.length > 0) {
				for (final Object obj : selectedValues) {
					((DefaultListModel) toList.getModel()).addElement(obj);
					((DefaultListModel) fromList.getModel()).removeElement(obj);
				}
			}
			updateModel();
			updateButtons();
		}

		/**
		 * 
		 */
		private void updateButtons() {
			// toButton.setEnabled(toList.getModel().getSize() > 0);
			// fromButton.setEnabled(fromList.getModel().getSize() > 0);
		}

		/**
		 * @param toRightButton
		 */
		public void setFromButton(final JButton fromButton) {
			this.fromButton = fromButton;
		}

		/**
		 * @param toLeftButton
		 */
		public void setToButton(final JButton toButton) {
			this.toButton = toButton;
		}

	}

	/**
	 * @param model
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
		rightToLeftAction.setFromButton(toRightButton);
		rightToLeftAction.setToButton(toLeftButton);
		leftToRightAction.setFromButton(toLeftButton);
		leftToRightAction.setToButton(toRightButton);
		buttonPanel.add(toLeftButton);
		buttonPanel.add(toRightButton);
		getComponentPanel().add(buttonPanel);
		includeList.setBorder(new TitledBorder(includeTitle));
		getComponentPanel().add(includeList);
		excludeList.setBorder(new TitledBorder(excludeTitle));
		updateComponent();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seeorg.knime.core.node.defaultnodesettings.DialogComponent#
	 * checkConfigurabilityBeforeLoad(org.knime.core.node.port.PortObjectSpec[])
	 */
	@Override
	protected void checkConfigurabilityBeforeLoad(final PortObjectSpec[] specs)
			throws NotConfigurableException {
		// TODO Auto-generated method stub

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
		includeList.setEnabled(enabled);
		excludeList.setEnabled(enabled);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.knime.core.node.defaultnodesettings.DialogComponent#setToolTipText
	 * (java.lang.String)
	 */
	@Override
	public void setToolTipText(final String text) {
		getComponentPanel().setToolTipText(text);
		includeList.setToolTipText(text);
		excludeList.setToolTipText(text);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.knime.core.node.defaultnodesettings.DialogComponent#updateComponent()
	 */
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
	 * @param model
	 * @param list
	 */
	private void fill(final DefaultListModel model, final ArrayList<String> list) {
		for (int i = 0; i < model.size(); ++i) {
			list.add((String) model.get(i));
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seeorg.knime.core.node.defaultnodesettings.DialogComponent#
	 * validateSettingsBeforeSave()
	 */
	@Override
	protected void validateSettingsBeforeSave() throws InvalidSettingsException {
		// TODO Auto-generated method stub

	}

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
	 * @param possibleValues
	 * @param listModel
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

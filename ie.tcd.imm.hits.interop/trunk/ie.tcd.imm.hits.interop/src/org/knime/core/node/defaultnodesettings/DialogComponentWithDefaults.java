/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package org.knime.core.node.defaultnodesettings;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;

import edu.umd.cs.findbugs.annotations.DefaultAnnotation;

/**
 * This {@link DialogComponent} allows to combine various
 * {@link DialogComponent}s and set their defaults.
 * <p>
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
// TODO save last used values. <br/>
// TODO add option to guess the initial value.
@DefaultAnnotation( { Nonnull.class, CheckReturnValue.class })
public class DialogComponentWithDefaults extends DialogComponent {

	private final JComboBox selectionBox = new JComboBox();
	private final Map<String, Boolean[]> enablementOptions;
	private final Map<String, Object[]> defaultValues;
	private final DialogComponent[] components;

	/**
	 * @param model
	 * @param label
	 * @param enablementOptions
	 * @param defaultValues
	 * @param components
	 */
	public DialogComponentWithDefaults(final SettingsModelString model,
			final String label, final Map<String, Boolean[]> enablementOptions,
			final Map<String, Object[]> defaultValues,
			final DialogComponent... components) {
		super(model);
		this.components = components;
		this.enablementOptions = clone(enablementOptions);
		this.defaultValues = clone(defaultValues);
		checkMaps(enablementOptions, components.length);
		checkMaps(defaultValues, components.length);
		getComponentPanel().add(new JLabel(label));
		getComponentPanel().add(selectionBox);
		for (final String option : defaultValues.keySet()) {
			selectionBox.addItem(option);
		}
		selectionBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				((SettingsModelString) getModel())
						.setStringValue((String) selectionBox.getSelectedItem());
				updateComponent();
			}
		});
		selectionBox.setEditable(false);
		getModel().addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(final ChangeEvent e) {
				updateComponent();
			}
		});
		updateComponent();
	}

	/**
	 * @param map
	 * @param <T>
	 * @return The cloned (not deep, just the array is cloned) map.
	 */
	private <T> Map<String, T[]> clone(final Map<String, T[]> map) {
		final Map<String, T[]> ret = new LinkedHashMap<String, T[]>();
		for (final Entry<String, T[]> entry : map.entrySet()) {
			ret.put(entry.getKey(), entry.getValue().clone());
		}
		return ret;
	}

	/**
	 * @param values
	 * @param valueCount
	 */
	private void checkMaps(final Map<String, ? extends Object[]> values,
			final int valueCount) {
		for (final Entry<String, ? extends Object[]> entry : values.entrySet()) {
			if (entry.getValue().length != valueCount) {
				throw new IllegalArgumentException("Wrong number of values: "
						+ entry.getValue().length + " expected: " + valueCount
						+ " at option: " + entry.getKey());
			}
		}
	}

	@Override
	protected void updateComponent() {
		final String newSelection = ((SettingsModelString) getModel())
				.getStringValue();
		selectionBox.setSelectedItem(newSelection);
		final String selection = (String) selectionBox.getSelectedItem();
		final Boolean[] enablements = enablementOptions.get(selection);
		final Object[] defaults = defaultValues.get(selection);
		for (int i = components.length; i-- > 0;) {
			components[i].getModel().setEnabled(enablements[i].booleanValue());
			if (defaults[i] instanceof String) {
				final String newValue = (String) defaults[i];
				((SettingsModelString) components[i].getModel())
						.setStringValue(newValue);
			} else if (defaults[i] instanceof String[]) {
				final String[] newValue = (String[]) defaults[i];
				((SettingsModelStringArray) components[i].getModel())
						.setStringArrayValue(newValue);
			} else if (defaults[i] instanceof Integer) {
				final Integer newValue = (Integer) defaults[i];
				((SettingsModelInteger) components[i].getModel())
						.setIntValue(newValue.intValue());
			} else if (defaults[i] instanceof Double) {
				final Double newValue = (Double) defaults[i];
				((SettingsModelDouble) components[i].getModel())
						.setDoubleValue(newValue.doubleValue());
			} else {
				if (defaults[i] != null) {
					throw new UnsupportedOperationException(
							"Not supported value type: "
									+ (defaults[i] == null ? null : defaults[i]
											.getClass()));
				}
			}
		}
	}

	@Override
	protected void validateSettingsBeforeSave() throws InvalidSettingsException {
		// Done in construction.
		final String selection = ((SettingsModelString) getModel())
				.getStringValue();
		if (!defaultValues.containsKey(selection)) {
			throw new InvalidSettingsException("No valid defaults found for "
					+ selection);
		}
	}

	@Override
	protected void checkConfigurabilityBeforeLoad(final PortObjectSpec[] specs)
			throws NotConfigurableException {
		// Should be OK.
	}

	@Override
	protected void setEnabledComponents(final boolean enabled) {
		selectionBox.setEnabled(enabled);
	}

	@Override
	public void setToolTipText(final String text) {
		getComponentPanel().setToolTipText(text);
	}
}

/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package ie.tcd.imm.hits.knime.view.impl;

import ie.tcd.imm.hits.knime.view.ListSelection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.port.PortObjectSpec;

import edu.umd.cs.findbugs.annotations.DefaultAnnotation;

/**
 * A {@link SettingsModel} for {@link ListSelection}.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
@DefaultAnnotation( { Nonnull.class, CheckReturnValue.class })
class SettingsModelListSelection extends SettingsModel implements
		ListSelection<String> {

	/**
	 * The model type ID.
	 * 
	 * @see #getModelTypeID()
	 */
	public static final String modelId = ListSelection.class.getCanonicalName()
			+ "<String>";

	/** The key for the values. */
	protected static final String CFGKEY_VALUES = modelId + "_values";

	/** The key for the selections. */
	protected static final String CFGKEY_SELECTIONS = modelId + "_selections";
	private final String configName;
	private final List<String> possibleValues = new ArrayList<String>();
	private final Set<String> selections = new HashSet<String>();

	/**
	 * Constructs a {@link SettingsModelListSelection}.
	 * 
	 * @param configName
	 *            The associated configuration name.
	 * @param initialPossibleValues
	 *            The initial possible values.
	 * @param selection
	 *            The selected values.
	 * @see #getConfigName()
	 * @see ListSelection
	 * @see #SettingsModelListSelection(String, List, Iterable)
	 */
	public SettingsModelListSelection(final String configName,
			final List<String> initialPossibleValues, final String... selection) {
		this(configName, initialPossibleValues, Arrays.asList(selection));
	}

	/**
	 * Constructs a {@link SettingsModelListSelection}.
	 * 
	 * @param configName
	 *            The associated configuration name.
	 * @param initialPossibleValues
	 *            The initial possible values.
	 * @param selection
	 *            The selected values.
	 * @see #getConfigName()
	 * @see ListSelection
	 */
	public SettingsModelListSelection(final String configName,
			final List<String> initialPossibleValues,
			final Iterable<String> selection) {
		super();
		this.configName = configName;
		possibleValues.addAll(initialPossibleValues);
		final boolean notified = updateSelection(selection);
		if (!notified) {
			notifyChangeListeners();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.knime.core.node.defaultnodesettings.SettingsModel#createClone()
	 */
	@Override
	@SuppressWarnings("unchecked")
	protected SettingsModelListSelection createClone() {
		return new SettingsModelListSelection(configName, possibleValues,
				selections);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.knime.core.node.defaultnodesettings.SettingsModel#getConfigName()
	 */
	@Override
	protected String getConfigName() {
		return configName;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.knime.core.node.defaultnodesettings.SettingsModel#getModelTypeID()
	 */
	@Override
	protected String getModelTypeID() {
		return modelId;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.knime.core.node.defaultnodesettings.SettingsModel#loadSettingsForDialog
	 * (org.knime.core.node.NodeSettingsRO,
	 * org.knime.core.node.port.PortObjectSpec[])
	 */
	@Override
	protected void loadSettingsForDialog(final NodeSettingsRO settings,
			final PortObjectSpec[] specs) throws NotConfigurableException {
		try {
			updateModel(settings);
		} catch (final InvalidSettingsException e) {
			if (isEnabled()) {
				throw new NotConfigurableException("Problem loading settings.",
						e);
			}
		}
	}

	/**
	 * @param newValues
	 * @param newSelections
	 */
	private void updateModel(final List<String> newValues,
			final Iterable<String> newSelections) {
		final boolean toNotify = !this.possibleValues.equals(newValues);
		possibleValues.clear();
		if (newValues.size() != new HashSet<String>(newValues).size()) {
			throw new IllegalArgumentException("Duplicate value.");
		}
		possibleValues.addAll(newValues);
		final boolean notified = updateSelection(newSelections);
		if (toNotify && !notified) {
			notifyChangeListeners();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.knime.core.node.defaultnodesettings.SettingsModel#loadSettingsForModel
	 * (org.knime.core.node.NodeSettingsRO)
	 */
	@Override
	protected void loadSettingsForModel(final NodeSettingsRO settings)
			throws InvalidSettingsException {
		updateModel(settings);
	}

	/**
	 * @param settings
	 * @throws InvalidSettingsException
	 */
	private void updateModel(final NodeSettingsRO settings)
			throws InvalidSettingsException {
		final String[] newValues = settings.getStringArray(CFGKEY_VALUES);
		final String[] newSelections = settings
				.getStringArray(CFGKEY_SELECTIONS);
		updateModel(Arrays.asList(newValues), Arrays.asList(newSelections));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.knime.core.node.defaultnodesettings.SettingsModel#saveSettingsForDialog
	 * (org.knime.core.node.NodeSettingsWO)
	 */
	@Override
	protected void saveSettingsForDialog(final NodeSettingsWO settings)
			throws InvalidSettingsException {
		settings.addStringArray(CFGKEY_VALUES, possibleValues
				.toArray(new String[possibleValues.size()]));
		settings.addStringArray(CFGKEY_SELECTIONS, selections
				.toArray(new String[selections.size()]));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.knime.core.node.defaultnodesettings.SettingsModel#saveSettingsForModel
	 * (org.knime.core.node.NodeSettingsWO)
	 */
	@Override
	protected void saveSettingsForModel(final NodeSettingsWO settings) {
		settings.addStringArray(CFGKEY_VALUES, possibleValues
				.toArray(new String[possibleValues.size()]));
		settings.addStringArray(CFGKEY_SELECTIONS, selections
				.toArray(new String[selections.size()]));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.knime.core.node.defaultnodesettings.SettingsModel#toString()
	 */
	@Override
	public String toString() {
		return getClass().getSimpleName() + " ('" + configName + "')";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seeorg.knime.core.node.defaultnodesettings.SettingsModel#
	 * validateSettingsForModel(org.knime.core.node.NodeSettingsRO)
	 */
	@Override
	protected void validateSettingsForModel(final NodeSettingsRO settings)
			throws InvalidSettingsException {
		final String[] vals = settings.getStringArray(CFGKEY_VALUES);
		if (vals == null) {
			throw new InvalidSettingsException("Values are missing.");
		}
		final HashSet<String> values = new HashSet<String>();
		for (final String val : vals) {
			if (val == null) {
				throw new InvalidSettingsException("Missing value");
			}
			if (!values.add(val)) {
				throw new InvalidSettingsException("Duplicate value: " + val);
			}
		}
		final String[] selections = settings.getStringArray(CFGKEY_SELECTIONS);
		if (selections == null) {
			throw new InvalidSettingsException("Selections are missing.");
		}
		final HashSet<String> set = new HashSet<String>();
		for (final String selection : selections) {
			if (selection == null) {
				throw new InvalidSettingsException("Missing selection");
			}
			if (!set.add(selection)) {
				throw new InvalidSettingsException("Duplicate selection: "
						+ selection);
			}
			if (!values.contains(selection)) {
				throw new InvalidSettingsException(
						"Selection is not in possible values: " + selection
								+ " {" + values + "}");
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ie.tcd.imm.hits.knime.view.ListSelection#getPossibleValues()
	 */
	@Override
	public List<String> getPossibleValues() {
		return Collections.unmodifiableList(possibleValues);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ie.tcd.imm.hits.knime.view.ListSelection#getSelection()
	 */
	@Override
	public Set<String> getSelection() {
		return Collections.unmodifiableSet(selections);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * ie.tcd.imm.hits.knime.view.ListSelection#setPossibleValues(java.util.
	 * List)
	 */
	@Override
	public void setPossibleValues(final List<? extends String> possibleValues) {
		final boolean toNotify = !this.possibleValues.equals(possibleValues);
		this.possibleValues.clear();
		this.possibleValues.addAll(possibleValues);
		final HashSet<String> copy = new HashSet<String>(selections);
		final boolean notified = updateSelection(copy);
		if (toNotify && !notified) {
			notifyChangeListeners();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * ie.tcd.imm.hits.knime.view.ListSelection#setSelection(java.lang.Iterable)
	 */
	@Override
	public void setSelection(final Iterable<? extends String> selection) {
		final boolean notified = updateSelection(selection);
		assert notified || !notified;
	}

	/**
	 * Updates the current selections.
	 * 
	 * @param selection
	 *            The new selection values.
	 * @return Whether or not notified the listeners.
	 */
	private boolean updateSelection(final Iterable<? extends String> selection) {
		final HashSet<String> copy = new HashSet<String>(selections);
		selections.clear();
		for (final String sel : selection) {
			if (this.possibleValues.contains(sel)) {
				selections.add(sel);
			}
		}
		if (!copy.equals(selections)) {
			notifyChangeListeners();
			return true;
		}
		return false;
	}
}

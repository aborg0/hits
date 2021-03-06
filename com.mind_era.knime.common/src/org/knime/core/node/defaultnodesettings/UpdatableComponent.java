/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package org.knime.core.node.defaultnodesettings;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;

/**
 * This {@link DialogComponent} is for updating other components. Please
 * override the {@link #updateComponent()} method.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
public class UpdatableComponent extends DialogComponent {

	/**
	 * Constructs an {@link UpdatableComponent}.
	 */
	public UpdatableComponent() {
		super(new EmptySettingsModel());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void checkConfigurabilityBeforeLoad(final PortObjectSpec[] specs)
			throws NotConfigurableException {
		// Do nothing
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void setEnabledComponents(final boolean enabled) {
		// TODO Auto-generated method stub

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setToolTipText(final String text) {
		// TODO Auto-generated method stub

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void updateComponent() {
		// Please override
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void validateSettingsBeforeSave() throws InvalidSettingsException {
		// TODO Auto-generated method stub

	}

	/**
	 * @return The last {@link PortObjectSpec}s, or {@code null}.
	 * @see DialogComponent#getLastTableSpecs()
	 */
	public PortObjectSpec[] getLastPortObjectSpecs() {
		return super.getLastTableSpecs();
	}
}

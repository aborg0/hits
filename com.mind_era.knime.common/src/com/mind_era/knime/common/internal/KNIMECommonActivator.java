/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package com.mind_era.knime.common.internal;

import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Activator of com.mind_era.knime.common.common plugin.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
public class KNIMECommonActivator extends AbstractUIPlugin {
	/** Plugin name */
	public static final String PLUGIN_ID = "com.mind_era.knime.common.common";

	private static KNIMECommonActivator instance;

	/**
	 * Constructs the {@link KNIMECommonActivator}, and assigns the
	 * {@link #getInstance() instance}.
	 */
	@SuppressFBWarnings("ST")
	public KNIMECommonActivator() {
		super();
		instance = this;
	}

	@Override
	public void stop(final BundleContext context) throws Exception {
		super.stop(context);
		instance = null;
	}

	/**
	 * @return the instance
	 */
	public static KNIMECommonActivator getInstance() {
		return instance;
	}
}

/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package com.mind_era.knime.util.internal;

import org.eclipse.core.runtime.Plugin;
import org.osgi.framework.BundleContext;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends Plugin {

	//TODO I think this is no longer necessary.
	/** Plugin identifier */
	public static final String PLUGIN_ID = "com.mind_era.knime.util";

	// The shared instance
	private static Activator plugin;

	/**
	 * The constructor
	 */
	@SuppressFBWarnings("ST")
	public Activator() {
		super();
		plugin = this;
	}

	/** {@inheritDoc} */
	@Override
	public void start(final BundleContext context) throws Exception {
		super.start(context);
	}

	/** {@inheritDoc} */
	@Override
	public void stop(final BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 * 
	 * @return the shared instance
	 */
	public static Activator getDefault() {
		return plugin;
	}
}

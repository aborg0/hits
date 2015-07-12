/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package ie.tcd.imm.hits.common.internal;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Activator of ie.tcd.imm.hits.common plugin.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
public class KNIMECommonActivator implements BundleActivator {
	/** Plugin name */
	public static final String PLUGIN_ID = "ie.tcd.imm.hits.common";

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
	public void start(final BundleContext context) throws Exception {
		// Do nothing
	}

	@Override
	public void stop(final BundleContext context) throws Exception {
		instance = null;
	}

	/**
	 * @return the instance
	 */
	public static KNIMECommonActivator getInstance() {
		return instance;
	}
}

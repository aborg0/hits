/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package ie.tcd.imm.hits.image;

import org.eclipse.core.runtime.Plugin;
import org.osgi.framework.BundleContext;

import edu.umd.cs.findbugs.annotations.SuppressWarnings;

/**
 * TODO Javadoc!
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
public class INCellImageReaderNodePlugin extends Plugin {

	/** Make sure that this *always* matches the ID in plugin.xml. */
	public static final String PLUGIN_ID = "ie.tcd.imm.hits.knime.xls";

	// The shared instance.
	private static INCellImageReaderNodePlugin plugin;

	/**
	 * 
	 */
	@SuppressWarnings("ST")
	public INCellImageReaderNodePlugin() {
		super();
		plugin = this;
	}

	/**
	 * This method is called upon plug-in activation.
	 * 
	 * @param context
	 *            The OSGI bundle context
	 * @throws Exception
	 *             If this plugin could not be started
	 */
	@Override
	public void start(final BundleContext context) throws Exception {
		super.start(context);

	}

	/**
	 * This method is called when the plug-in is stopped.
	 * 
	 * @param context
	 *            The OSGI bundle context
	 * @throws Exception
	 *             If this plugin could not be stopped
	 */
	@Override
	@SuppressWarnings("ST")
	public void stop(final BundleContext context) throws Exception {
		super.stop(context);
		plugin = null;
	}

	/**
	 * Returns the shared instance.
	 * 
	 * @return Singleton instance of the Plugin
	 */
	public static INCellImageReaderNodePlugin getDefault() {
		return plugin;
	}
}

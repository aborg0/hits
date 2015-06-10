/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package ie.tcd.imm.hits.image.internal;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Plugin;
import org.osgi.framework.BundleContext;

import edu.umd.cs.findbugs.annotations.SuppressWarnings;

/**
 * TODO Javadoc!
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
public class ImagePlugin extends Plugin {

	/** Make sure that this *always* matches the ID in plugin.xml. */
	public static final String PLUGIN_ID = "ie.tcd.imm.hits.knime.xls";

	// The shared instance.
	private static ImagePlugin plugin;

	/**
	 * 
	 */
	@SuppressWarnings("ST")
	public ImagePlugin() {
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
	public static ImagePlugin getDefault() {
		return plugin;
	}

	/**
	 * @return A {@link File} with absolute path pointing to the plugin's root
	 *         folder.
	 * @throws IOException
	 *             If the root folder cannot be expressed as a {@link URL}.
	 */
	public File getPluginPath() throws IOException {
		final URL url = FileLocator.find(ImagePlugin.getDefault().getBundle(),
				new Path(""), null);
		URI uri;
		try {
			uri = url == null ? null : FileLocator.toFileURL(url).toURI();
		} catch (final URISyntaxException e) {
			throw new IOException(e);
		}
		return new File(uri).getAbsoluteFile();

	}
}

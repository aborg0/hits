/**
 * 
 */
package ie.tcd.imm.hits.knime.internal;

import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * Activator for this plugin, necessary for the preferences.
 * 
 * @author Gabor Bakos
 */
public class Activator extends AbstractUIPlugin {
	private static Activator instance; 

	/**
	 * The activator constructor.
	 */
	public Activator() {
		super();
		instance = this;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	@Override
	public void stop(BundleContext context) throws Exception {
		super.stop(context);
		instance = null;
	}
	
	/**
	 * @return the instance
	 */
	public static Activator getInstance() {
		return instance;
	}
}

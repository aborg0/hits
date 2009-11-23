package ie.tcd.imm.hits.image.omero.read;

import ie.tcd.imm.hits.image.internal.ImagePlugin;

import java.io.File;
import java.io.IOException;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.openmicroscopy.shoola.env.Container;
import org.openmicroscopy.shoola.env.init.BatchLoginServiceInit;
import org.openmicroscopy.shoola.env.init.FakeInitializer;
import org.openmicroscopy.shoola.env.init.LoginServiceInit;
import org.openmicroscopy.shoola.env.init.NullSplashScreenInit;
import org.openmicroscopy.shoola.env.init.SplashScreenInit;

/**
 * This is the model implementation of OMEROReader. Allows to import data from
 * OMERO servers.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
public class OMEROReaderNodeModel extends NodeModel {
	static {
		final String pluginPath = ((org.eclipse.osgi.baseadaptor.BaseData) ((org.eclipse.osgi.framework.internal.core.BundleHost) ImagePlugin
				.getDefault().getBundle()).getBundleData()).getBundleFile()
				.getBaseFile().getAbsolutePath();
		FakeInitializer.replaceInitTask(LoginServiceInit.class,
				BatchLoginServiceInit.class);
		FakeInitializer.replaceInitTask(SplashScreenInit.class,
				NullSplashScreenInit.class);
		// Container.startup(pluginPath, "container.xml");

		final Container container = Container.startupInTestMode(pluginPath);
		// final LoginService svc = (LoginService)
		// container.getRegistry().lookup(
		// LookupNames.LOGIN);
		// final UserCredentials uc = new UserCredentials(userName, password,
		// hostName, speedLevel);
		// uc.setPort(port);
		// svc.login(uc);
	}

	/**
	 * Constructor for the node model.
	 */
	protected OMEROReaderNodeModel() {
		super(0, 2);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
			final ExecutionContext exec) throws Exception {
		// TODO: Return a BufferedDataTable for each output port
		return new BufferedDataTable[] { null, null };
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void reset() {
		// TODO: generated method stub
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
			throws InvalidSettingsException {

		// TODO: generated method stub
		return new DataTableSpec[] { null, null };
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveSettingsTo(final NodeSettingsWO settings) {
		// TODO: generated method stub
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
			throws InvalidSettingsException {
		// TODO: generated method stub
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void validateSettings(final NodeSettingsRO settings)
			throws InvalidSettingsException {
		// TODO: generated method stub
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadInternals(final File internDir,
			final ExecutionMonitor exec) throws IOException,
			CanceledExecutionException {
		// TODO: generated method stub
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveInternals(final File internDir,
			final ExecutionMonitor exec) throws IOException,
			CanceledExecutionException {
		// TODO: generated method stub
	}
}

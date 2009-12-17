package ie.tcd.imm.hits.image.omero.read;

import ie.tcd.imm.hits.image.internal.ImagePlugin;
import ie.tcd.imm.hits.util.Displayable;

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
import org.knime.core.node.defaultnodesettings.SettingsModelEnum;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
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
		String pluginPath;
		try {
			pluginPath = ImagePlugin.getDefault().getPluginPath()
					.getAbsolutePath();
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
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

	static enum ConnectionSpeed implements Displayable {
		high {
			@Override
			public String getDisplayText() {
				return "high";
			}
		},
		medium {
			@Override
			public String getDisplayText() {
				return "medium";
			}
		},
		low {
			@Override
			public String getDisplayText() {
				return "low";
			}
		};
	}

	static final String CFGKEY_SERVER_NAME = "server.name";
	static final String DEFAULT_SERVER_NAME = "ome2-copy.fzk.de";
	static final String CFGKEY_SERVER_PORT = "server.name";
	static final int DEFAULT_SERVER_PORT = 4063;
	static final String CFGKEY_CONNECTION_SPEED = "connection.speed";
	static final ConnectionSpeed DEFAULT_CONNECTION_SPEED = ConnectionSpeed.low;
	static final String CFGKEY_USERNAME = "user.name";
	static final String DEFAULT_USERNAME = "";
	static final String CFGKEY_PASSWORD = "password";
	static final String DEFAULT_PASSWORD = "";

	private final SettingsModelString serverName = new SettingsModelString(
			CFGKEY_SERVER_NAME, DEFAULT_SERVER_NAME);
	private final SettingsModelInteger serverPort = new SettingsModelIntegerBounded(
			CFGKEY_SERVER_PORT, DEFAULT_SERVER_PORT, 0, 65535);
	private final SettingsModelEnum<ConnectionSpeed> connectionSpeed = new SettingsModelEnum<ConnectionSpeed>(
			CFGKEY_CONNECTION_SPEED, DEFAULT_CONNECTION_SPEED, ConnectionSpeed
					.values());
	private final SettingsModelString userName = new SettingsModelString(
			CFGKEY_USERNAME, DEFAULT_USERNAME);
	private final SettingsModelString password = new SettingsModelString(
			CFGKEY_PASSWORD, DEFAULT_PASSWORD);

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
		serverName.saveSettingsTo(settings);
		serverPort.saveSettingsTo(settings);
		connectionSpeed.saveSettingsTo(settings);
		userName.saveSettingsTo(settings);
		password.saveSettingsTo(settings);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
			throws InvalidSettingsException {
		serverName.loadSettingsFrom(settings);
		serverPort.loadSettingsFrom(settings);
		connectionSpeed.loadSettingsFrom(settings);
		userName.loadSettingsFrom(settings);
		password.loadSettingsFrom(settings);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void validateSettings(final NodeSettingsRO settings)
			throws InvalidSettingsException {
		serverName.validateSettings(settings);
		serverPort.validateSettings(settings);
		connectionSpeed.validateSettings(settings);
		userName.validateSettings(settings);
		password.validateSettings(settings);
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

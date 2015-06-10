/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package ie.tcd.imm.hits.image.omero.read;

import ie.tcd.imm.hits.util.Displayable;
import ij.ImagePlus;

import java.awt.Image;
import java.io.File;
import java.io.IOException;
import java.util.Collections;

import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelEnum;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.defaultnodesettings.SettingsModelStringArray;
import org.knime.core.util.KnimeEncryption;
import org.knime.exp.imaging.data.def.DefaultImageCell;

import pojos.ImageData;
import pojos.PixelsData;

/**
 * This is the model implementation of OMEROReader. Allows to import data from
 * OMERO servers.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
public class OMEROReaderNodeModel extends NodeModel {
	private static final String IMAGE_NAME = "image name";
	private static final String IMAGE = "Image";
	private static final String ID = "id";
	static final NodeLogger logger = NodeLogger
			.getLogger(OMEROReaderNodeModel.class);

	// static final Container container;
	// static {
	// String pluginPath;
	// try {
	// pluginPath = ImagePlugin.getDefault().getPluginPath()
	// .getAbsolutePath();
	// } catch (final IOException e) {
	// throw new RuntimeException(e);
	// }
	// FakeInitializer.replaceInitTask(LoginServiceInit.class,
	// BatchLoginServiceInit.class);
	// FakeInitializer.replaceInitTask(SplashScreenInit.class,
	// NullSplashScreenInit.class);
	// // Container.startup(pluginPath, "container.xml");
	//
	// container = Container.startupInTestMode(pluginPath);
	// // final LoginService svc = (LoginService)
	// // container.getRegistry().lookup(
	// // LookupNames.LOGIN);
	// // final UserCredentials uc = new UserCredentials(userName, password,
	// // hostName, speedLevel);
	// // uc.setPort(port);
	// // svc.login(uc);
	// }

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
	static final String CFGKEY_SERVER_PORT = "server.port";
	static final int DEFAULT_SERVER_PORT = 4063;
	static final String CFGKEY_CONNECTION_SPEED = "connection.speed";
	static final ConnectionSpeed DEFAULT_CONNECTION_SPEED = ConnectionSpeed.low;
	static final ConnectionSpeed[] POSSIBLE_CONNECTION_SPEEDS = ConnectionSpeed
			.values();
	static final String CFGKEY_USERNAME = "user.name";
	static final String DEFAULT_USERNAME = "";
	static final String CFGKEY_PASSWORD = "password";
	static final String DEFAULT_PASSWORD = "";
	static final String CFGKEY_SELECTION = "selection";
	static final String[] DEFAULT_SELECTION = new String[] { "" };

	private final SettingsModelString serverName = new SettingsModelString(
			CFGKEY_SERVER_NAME, DEFAULT_SERVER_NAME);
	private final SettingsModelInteger serverPort = new SettingsModelIntegerBounded(
			CFGKEY_SERVER_PORT, DEFAULT_SERVER_PORT, 0, 65535);
	private final SettingsModelEnum<ConnectionSpeed> connectionSpeed = new SettingsModelEnum<ConnectionSpeed>(
			CFGKEY_CONNECTION_SPEED, DEFAULT_CONNECTION_SPEED,
			POSSIBLE_CONNECTION_SPEEDS);
	private final SettingsModelString userName = new SettingsModelString(
			CFGKEY_USERNAME, DEFAULT_USERNAME);
	private final SettingsModelString password = new SettingsModelString(
			CFGKEY_PASSWORD, DEFAULT_PASSWORD);
	private final SettingsModelStringArray selection = new SettingsModelStringArray(
			CFGKEY_SELECTION, DEFAULT_SELECTION);

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
		final DataTableSpec[] specs = configure(new DataTableSpec[0]);
		final BufferedDataContainer images = exec.createDataContainer(specs[0]);
		final OMEROHandler handler = OMEROHandler.getInstance();
		handler.reconnect(serverName.getStringValue(),
				serverPort.getIntValue(), userName.getStringValue(),
				KnimeEncryption.decrypt(password.getStringValue()),
				connectionSpeed.getEnumValue());
		final long userId = handler.getExperimenterData().getId();
		for (final String selectedString : selection.getStringArrayValue()) {
			if (!selectedString.isEmpty()) {
				assert selectedString.contains("\u00a0") : selectedString;
				final long id = Long.parseLong(selectedString.substring(0,
						selectedString.indexOf('\u00a0')));
				handler.getDataService().getImages(ImageData.class,
						Collections.singletonList(id), userId);
				final PixelsData pixels = handler.getImageService().loadPixels(
						id);
				System.out.println(pixels.getPixelType());
				System.out.println(pixels.getSizeC());
				System.out.println(pixels.getSizeT());
				System.out.println(pixels.getSizeX());
				System.out.println(pixels.getSizeY());
				System.out.println(pixels.getSizeZ());
				images.addRowToTable(new DefaultRow(new RowKey(Long
						.toString(id)), new IntCell((int) id), new StringCell(
						selectedString.substring(selectedString
								.indexOf('\u00a0') + 1)), new DefaultImageCell(
						new ImagePlus((String) null, (Image) null))));
			}
		}
		images.close();
		final BufferedDataContainer metadata = exec
				.createDataContainer(specs[1]);
		metadata.close();
		return new BufferedDataTable[] { images.getTable(), metadata.getTable() };
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
		return new DataTableSpec[] {
				new DataTableSpec(new DataColumnSpecCreator(ID, IntCell.TYPE)
						.createSpec(), new DataColumnSpecCreator(IMAGE_NAME,
						StringCell.TYPE).createSpec(),
						new DataColumnSpecCreator(IMAGE, DefaultImageCell.TYPE)
								.createSpec()),
				new DataTableSpec(new DataColumnSpecCreator(ID, IntCell.TYPE)
						.createSpec()) };
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
		selection.saveSettingsTo(settings);
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
		selection.loadSettingsFrom(settings);
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
		selection.validateSettings(settings);
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

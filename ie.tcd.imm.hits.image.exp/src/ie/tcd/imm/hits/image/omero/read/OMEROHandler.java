/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package ie.tcd.imm.hits.image.omero.read;

import ie.tcd.imm.hits.image.internal.ImagePlugin;
import ie.tcd.imm.hits.image.omero.read.OMEROReaderNodeModel.ConnectionSpeed;

import java.io.IOException;

import org.knime.core.node.NodeLogger;
import org.openmicroscopy.shoola.env.Container;
import org.openmicroscopy.shoola.env.LookupNames;
import org.openmicroscopy.shoola.env.config.Registry;
import org.openmicroscopy.shoola.env.data.OmeroDataService;
import org.openmicroscopy.shoola.env.data.OmeroImageService;
import org.openmicroscopy.shoola.env.data.login.LoginService;
import org.openmicroscopy.shoola.env.data.login.UserCredentials;
import org.openmicroscopy.shoola.env.init.BatchLoginServiceInit;
import org.openmicroscopy.shoola.env.init.FakeInitializer;
import org.openmicroscopy.shoola.env.init.LoginServiceInit;
import org.openmicroscopy.shoola.env.init.NullSplashScreenInit;
import org.openmicroscopy.shoola.env.init.SplashScreenInit;

import pojos.ExperimenterData;

/**
 * A helper class to handle OMERO server related tasks.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
public class OMEROHandler {
	static final NodeLogger logger = NodeLogger.getLogger(OMEROHandler.class);
	static final OMEROHandler instance = new OMEROHandler();

	final Container container;

	private OMEROHandler() {
		super();
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

		container = Container.startupInTestMode(pluginPath);
	}

	protected void reconnect(final String server, final int port,
			final String user, final String password,
			final ConnectionSpeed speed) {
		final LoginService svc = getLoginService();
		final UserCredentials uc = new UserCredentials(user, password, server,
				speed.ordinal());
		uc.setPort(port);
		final int state = svc.login(uc);
		switch (state) {
		case LoginService.CONNECTED:
			OMEROReaderNodeModel.logger.info("Connected to " + server);
			break;
		// case LoginService.NOT_CONNECTED:
		// break;
		default:
			OMEROReaderNodeModel.logger.warn("Connection failed.");
			break;
		}
	}

	LoginService getLoginService() {
		final LoginService svc = (LoginService) getRegistry().lookup(
				LookupNames.LOGIN);
		return svc;
	}

	ExperimenterData getExperimenterData() {
		return (ExperimenterData) getRegistry().lookup(
				LookupNames.CURRENT_USER_DETAILS);
	}

	Registry getRegistry() {
		return container.getRegistry();
	}

	OmeroDataService getDataService() {
		return getRegistry().getDataService();
	}

	OmeroImageService getImageService() {
		return getRegistry().getImageService();
	}

	public static OMEROHandler getInstance() {
		return instance;
	}
}

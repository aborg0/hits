/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package ie.tcd.imm.hits.image.omero.read;

import ie.tcd.imm.hits.image.omero.read.OMEROReaderNodeModel.ConnectionSpeed;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.util.Collections;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentButton;
import org.knime.core.node.defaultnodesettings.DialogComponentNumberEdit;
import org.knime.core.node.defaultnodesettings.DialogComponentPasswordField;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.DialogComponentStringListSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelEnum;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.defaultnodesettings.SettingsModelStringArray;
import org.knime.core.node.defaultnodesettings.UpdatableComponent;
import org.knime.core.util.KnimeEncryption;
import org.openmicroscopy.shoola.env.data.DSAccessException;
import org.openmicroscopy.shoola.env.data.DSOutOfServiceException;
import org.openmicroscopy.shoola.env.data.OmeroDataService;

import pojos.ExperimenterData;
import pojos.ImageData;

/**
 * <code>NodeDialog</code> for the "OMEROReader" Node. Allows to import data
 * from OMERO servers.
 * 
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more
 * complex dialog please derive directly from
 * {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
public class OMEROReaderNodeDialog extends DefaultNodeSettingsPane {

	/**
	 * New pane for configuring the OMEROReader node.
	 */
	protected OMEROReaderNodeDialog() {
		super();
		createNewGroup("server");
		setHorizontalPlacement(true);
		final SettingsModelString serverNameModel = new SettingsModelString(
				OMEROReaderNodeModel.CFGKEY_SERVER_NAME,
				OMEROReaderNodeModel.DEFAULT_SERVER_NAME);
		addDialogComponent(new DialogComponentString(serverNameModel,
				"Server: "));
		final SettingsModelIntegerBounded portModel = new SettingsModelIntegerBounded(
				OMEROReaderNodeModel.CFGKEY_SERVER_PORT,
				OMEROReaderNodeModel.DEFAULT_SERVER_PORT, 0, 65535);
		addDialogComponent(new DialogComponentNumberEdit(portModel, "Port: "));
		final SettingsModelEnum<ConnectionSpeed> connectionSpeedModel = new SettingsModelEnum<ConnectionSpeed>(
				OMEROReaderNodeModel.CFGKEY_CONNECTION_SPEED,
				OMEROReaderNodeModel.DEFAULT_CONNECTION_SPEED,
				OMEROReaderNodeModel.POSSIBLE_CONNECTION_SPEEDS);
		addDialogComponent(new DialogComponentStringSelection(
				connectionSpeedModel, "Connection speed: ",
				connectionSpeedModel.getDisplayTexts()));
		createNewGroup("user");
		setHorizontalPlacement(false);
		final SettingsModelString usernameModel = new SettingsModelString(
				OMEROReaderNodeModel.CFGKEY_USERNAME,
				OMEROReaderNodeModel.DEFAULT_USERNAME);
		addDialogComponent(new DialogComponentString(usernameModel,
				"User name: "));
		final SettingsModelString passwordModel = new SettingsModelString(
				OMEROReaderNodeModel.CFGKEY_PASSWORD,
				OMEROReaderNodeModel.DEFAULT_PASSWORD);
		addDialogComponent(new DialogComponentPasswordField(passwordModel,
				"Password: "));
		final SettingsModelStringArray optionsModel = new SettingsModelStringArray(
				OMEROReaderNodeModel.CFGKEY_SELECTION,
				OMEROReaderNodeModel.DEFAULT_SELECTION);
		createNewGroup("Plates");
		final DialogComponentStringListSelection optionComponent = new DialogComponentStringListSelection(
				optionsModel, "Dataset", Collections.singletonList(""), true,
				10);
		addDialogComponent(optionComponent);
		addDialogComponent(new UpdatableComponent() {
			@Override
			protected void updateComponent()/* => */{
				try {
					reconnect(serverNameModel.getStringValue(), portModel
							.getIntValue(), usernameModel.getStringValue(),
							KnimeEncryption.decrypt(passwordModel
									.getStringValue()), connectionSpeedModel
									.getEnumValue(), optionComponent);
				} catch (final InvalidKeyException e) {
					OMEROReaderNodeModel.logger.error(e);
				} catch (final BadPaddingException e) {
					OMEROReaderNodeModel.logger.error(e);
				} catch (final IllegalBlockSizeException e) {
					OMEROReaderNodeModel.logger.error(e);
				} catch (final UnsupportedEncodingException e) {
					OMEROReaderNodeModel.logger.error(e);
				} catch (final IOException e) {
					OMEROReaderNodeModel.logger.error(e);
				}
			}
		});
		final ActionListener listener = new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				try {
					reconnect(serverNameModel.getStringValue(), portModel
							.getIntValue(), usernameModel.getStringValue(),
							KnimeEncryption.decrypt(passwordModel
									.getStringValue()), connectionSpeedModel
									.getEnumValue(), optionComponent);
				} catch (final InvalidKeyException e1) {
					OMEROReaderNodeModel.logger.error(e1);
				} catch (final BadPaddingException e1) {
					OMEROReaderNodeModel.logger.error(e1);
				} catch (final IllegalBlockSizeException e1) {
					OMEROReaderNodeModel.logger.error(e1);
				} catch (final UnsupportedEncodingException e1) {
					OMEROReaderNodeModel.logger.error(e1);
				} catch (final IOException e1) {
					OMEROReaderNodeModel.logger.error(e1);
				}
			}
		};
		// serverNameModel.addChangeListener(listener);
		// portModel.addChangeListener(listener);
		// connectionSpeedModel.addChangeListener(listener);
		// usernameModel.addChangeListener(listener);
		// passwordModel.addChangeListener(listener);
		final DialogComponentButton connectButton = new DialogComponentButton(
				"Connect");
		connectButton.addActionListener(listener);
		addDialogComponent(connectButton);
	}

	protected static void reconnect(final String server, final int port,
			final String user, final String password,
			final ConnectionSpeed speed,
			final DialogComponentStringListSelection optionComponent) {
		final OMEROHandler handler = OMEROHandler.getInstance();
		handler.reconnect(server, port, user, password, speed);
		final OmeroDataService dataService = handler.getDataService();
		try {
			final ExperimenterData experimenterData = handler
					.getExperimenterData();
			final long userId = experimenterData.getId();
			final Set<?> experimenterImages = dataService
					.getExperimenterImages(userId);
			final SortedSet<String> names = new TreeSet<String>();
			for (final Object object : experimenterImages) {
				if (object instanceof ImageData) {
					final ImageData data = (ImageData) object;
					// System.out.println("name: " + data.getName());
					// final Collection<?> containerPaths = dataService
					// .findContainerPaths(PlateData.class, data.getId(),
					// -1);
					// System.out.println(containerPaths);
					// for (final Object o : containerPaths) {
					// if (o instanceof DatasetData) {
					// final DatasetData dataset = (DatasetData) o;
					// names.add(String.format("%d\u00a0%s", dataset
					// .getId(), dataset.getName()));
					// }
					// }
					// System.out.println("acqu: " + data.getAcquisitionDate());
					names.add(String.format("%5d\u00a0%s", data.getId(), data
							.getName()));
				}
			}
			optionComponent.replaceListItems(names);
			// final Object advancedSearchFor = dataService
			// .advancedSearchFor(new SearchDataContext(Collections
			// .<Integer> emptyList(), Collections
			// .<Class> singletonList(Dataset.class),
			// new String[0], new String[0], new String[0]));
			// System.out.println(advancedSearchFor);
			System.out.println();
		} catch (final DSOutOfServiceException e) {
			OMEROReaderNodeModel.logger.info(e.getMessage(), e);
		} catch (final DSAccessException e) {
			OMEROReaderNodeModel.logger.info(e.getMessage(), e);
		}
	}
}

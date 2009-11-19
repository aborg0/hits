/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package ie.tcd.imm.hits.knime.biomart;

import ie.tcd.imm.hits.util.RUtil;
import ie.tcd.imm.hits.util.file.OpenStream;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JComboBox;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.eclipse.core.net.proxy.IProxyData;
import org.eclipse.core.net.proxy.IProxyService;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponent;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentMultiLineString;
import org.knime.core.node.defaultnodesettings.DialogComponentNumberEdit;
import org.knime.core.node.defaultnodesettings.DialogComponentPasswordField;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.DialogComponentStringListSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.defaultnodesettings.SettingsModelStringArray;
import org.knime.core.node.defaultnodesettings.UpdatableComponent;
import org.knime.core.node.util.StringIconOption;
import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPFactor;
import org.rosuda.REngine.REXPGenericVector;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.REXPString;
import org.rosuda.REngine.RList;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;

/**
 * <code>NodeDialog</code> for the "BiomartAnnotator" Node. Adds some
 * annotations from the BioMart databases using the biomaRt R package.
 * 
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more
 * complex dialog please derive directly from
 * {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
public class BiomartAnnotatorNodeDialog extends DefaultNodeSettingsPane {
	/**  */
	private static final NodeLogger logger = NodeLogger
			.getLogger(BiomartAnnotatorNodeDialog.class);
	private static final String PROXY_TAB_NAME = "Proxy";

	private final Map<String, String> attributes = new HashMap<String, String>();

	/**
	 * New pane for configuring the BiomartAnnotator node.
	 */
	protected BiomartAnnotatorNodeDialog() {
		super();
		final DialogComponentStringSelection biomartDatabaseDialog = new DialogComponentStringSelection(
				new SettingsModelString(
						BiomartAnnotatorNodeModel.CFGKEY_BIOMART_DATABASE,
						BiomartAnnotatorNodeModel.DEFAULT_BIOMART_DATABASE),
				"biomaRt database: ", new String[] { "" });
		final DialogComponentStringSelection biomartDatasetDialog = new DialogComponentStringSelection(
				new SettingsModelString(
						BiomartAnnotatorNodeModel.CFGKEY_BIOMART_DATASET,
						BiomartAnnotatorNodeModel.DEFAULT_BIOMART_DATASET),
				"biomaRt dataset:", new String[] { "" });
		final DialogComponentStringListSelection biomartAttributesDialog = new DialogComponentStringListSelection(
				new SettingsModelStringArray(
						BiomartAnnotatorNodeModel.CFGKEY_BIOMART_ATTRIBUTES,
						BiomartAnnotatorNodeModel.DEFAULT_BIOMART_ATTRIBUTES),
				"Attributes:", "");
		final DialogComponentMultiLineString selectedBiomartAttributesDialog = new DialogComponentMultiLineString(
				new SettingsModelString("selected_attributes_not_used", ""),
				"Selected: ", false, 70, 8);
		final DialogComponentBoolean proxyFromEclipse = new DialogComponentBoolean(
				new SettingsModelBoolean(
						BiomartAnnotatorNodeModel.CFGKEY_PROXY_FROM_ECLIPSE,
						BiomartAnnotatorNodeModel.DEFAULT_PROXY_FROM_ECLIPSE),
				"Use KNIME proxy settings?");
		final DialogComponentString proxyHost = new DialogComponentString(
				new SettingsModelString(
						BiomartAnnotatorNodeModel.CFGKEY_PROXY_HOST,
						BiomartAnnotatorNodeModel.DEFAULT_PROXY_HOST),
				"Proxy server", false, 80);
		final DialogComponentNumberEdit proxyPort = new DialogComponentNumberEdit(
				new SettingsModelIntegerBounded(
						BiomartAnnotatorNodeModel.CFGKEY_PROXY_PORT,
						BiomartAnnotatorNodeModel.DEFAULT_PROXY_PORT, -1, 65535),
				"Port number", 8);
		final DialogComponentString proxyUser = new DialogComponentString(
				new SettingsModelString(
						BiomartAnnotatorNodeModel.CFGKEY_PROXY_USER,
						BiomartAnnotatorNodeModel.DEFAULT_PROXY_USER),
				"Proxy username", false, 40);
		final DialogComponentPasswordField proxyPassword = new DialogComponentPasswordField(
				new SettingsModelString(
						BiomartAnnotatorNodeModel.CFGKEY_PROXY_PASSWORD,
						BiomartAnnotatorNodeModel.DEFAULT_PROXY_PASSWORD),
				"Proxy password", 25);
		biomartDatabaseDialog.getModel().addChangeListener(
				new ChangeListener() {
					@Override
					public void stateChanged(final ChangeEvent e)/* => */{
						// final JComboBox combobox = (JComboBox) e.getSource();
						final Object selectedItem = e.getSource() instanceof JComboBox ? ((JComboBox) e
								.getSource()).getSelectedItem()
								: ((SettingsModelString) e.getSource())
										.getStringValue();
						if (selectedItem instanceof String) {
							final REXP datasetsResult;
							final String dbName = (String) selectedItem;
							try {
								final RConnection conn = new RConnection();
								try {
									conn.voidEval("library(\"biomaRt\")");
									setProxy(conn, proxyHost, proxyPort,
											proxyUser, proxyPassword);
									conn.voidEval("biomartDb = useMart(\""
											+ dbName + "\")");
									datasetsResult = RUtil.eval(conn,
											"listDatasets(biomartDb)");
								} finally {
									conn.close();
								}
								final RList table = ((REXPGenericVector) datasetsResult)
										.asList();

								final String[] shortNames = ((REXPString) table
										.get(0)).asStrings();
								biomartDatasetDialog.replaceListItems(Arrays
										.asList(shortNames), null);
							} catch (final RserveException e1) {
								logger.error(
										"Unable to select the datasets for "
												+ dbName, e1);
								selectTab(PROXY_TAB_NAME);
							} catch (final REXPMismatchException e1) {
								logger.error(
										"Unable to select the datasets for "
												+ dbName, e1);
							}
						}
					}
				});
		biomartDatasetDialog.getModel().addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(final ChangeEvent e) /* => */{
				final Object object = ((JComboBox) biomartDatabaseDialog
						.getComponentPanel().getComponent(1)).getSelectedItem();
				final String dbName = object instanceof String ? (String) object
						: object instanceof StringIconOption ? ((StringIconOption) object)
								.getText()
								: object.toString();

				final Object selectedItem = e.getSource() instanceof JComboBox ? ((JComboBox) e
						.getSource()).getSelectedItem()
						: ((SettingsModelString) e.getSource())
								.getStringValue();
				if (selectedItem instanceof String) {
					final REXP datasetsResult;
					final String dataset = (String) selectedItem;
					try {
						final RConnection conn = new RConnection();
						try {
							conn.voidEval("library(\"biomaRt\")");
							setProxy(conn, proxyHost, proxyPort, proxyUser,
									proxyPassword);
							conn.voidEval("biomartDb = useMart(\"" + dbName
									+ "\")");

							datasetsResult = RUtil.eval(conn,
									"listAttributes(useDataset(\"" + dataset
											+ "\",mart=biomartDb))");
						} finally {
							conn.close();
						}
						final RList table = ((REXPGenericVector) datasetsResult)
								.asList();

						final String[] shortNames = ((REXPString) table.get(0))
								.asStrings();
						final String[] descriptions = ((REXPString) table
								.get(1)).asStrings();
						attributes.clear();
						for (int i = 0; i < shortNames.length; i++) {
							attributes.put(shortNames[i], descriptions[i]);
						}

						biomartAttributesDialog.replaceListItems(Arrays
								.asList(shortNames), new String[0]);
					} catch (final RserveException e1) {
						logger.error("Unable to select the attributes for "
								+ dbName + "/" + dataset, e1);
					} catch (final REXPMismatchException e1) {
						logger.error("Unable to select the attributes for "
								+ dbName + "/" + dataset, e1);
					}
				}
			}
		});

		addDialogComponent(biomartDatabaseDialog);
		addDialogComponent(biomartDatasetDialog);
		createNewGroup("Attributes");
		setHorizontalPlacement(true);
		addDialogComponent(biomartAttributesDialog);
		biomartAttributesDialog.getModel().addChangeListener(
				new ChangeListener() {
					@Override
					public void stateChanged(final ChangeEvent e) /* => */{
						final String[] selectedValues = ((SettingsModelStringArray) e
								.getSource()).getStringArrayValue();
						final StringBuilder sb = new StringBuilder();
						for (final String selected : selectedValues) {
							sb.append(selected).append(" - ").append(
									attributes.get(selected)).append("\n");
						}
						((SettingsModelString) selectedBiomartAttributesDialog
								.getModel()).setStringValue(sb.toString());
					}
				});
		addDialogComponent(selectedBiomartAttributesDialog);
		setHorizontalPlacement(false);
		createNewTab(PROXY_TAB_NAME);
		addDialogComponent(proxyFromEclipse);
		createNewGroup("Custom proxy settings");
		final ChangeListener proxyFromEclipseChangeListener = new ChangeListener() {
			@Override
			public void stateChanged(final ChangeEvent e) /* => */{
				final boolean settingsFromEclipse = ((SettingsModelBoolean) proxyFromEclipse
						.getModel()).getBooleanValue();
				for (final DialogComponent component : new DialogComponent[] {
						proxyHost, proxyPort, proxyUser, proxyPassword }) {
					component.getModel().setEnabled(!settingsFromEclipse);
				}
				if (settingsFromEclipse) {
					final IProxyService proxyService = OpenStream
							.getProxyService();
					// TODO update when KNIME is based on eclipse 3.5
					final IProxyData proxyDataForHost = proxyService
							.getProxyDataForHost("biomart.org",
									IProxyData.HTTP_PROXY_TYPE);
					((SettingsModelString) proxyHost.getModel())
							.setStringValue(proxyDataForHost.getHost() == null ? ""
									: proxyDataForHost.getHost());
					((SettingsModelInteger) proxyPort.getModel())
							.setIntValue(proxyDataForHost.getPort());
					((SettingsModelString) proxyUser.getModel())
							.setStringValue(proxyDataForHost.getUserId() == null ? ""
									: proxyDataForHost.getUserId());
					((SettingsModelString) proxyPassword.getModel())
							.setStringValue(proxyDataForHost.getPassword() == null ? ""
									: proxyDataForHost.getPassword());
				}
				try {
					final REXPGenericVector martsList;
					final RConnection conn = new RConnection();
					try {
						conn.voidEval("library(\"biomaRt\")");
						setProxy(conn, proxyHost, proxyPort, proxyUser,
								proxyPassword);
						martsList = RUtil.<REXPGenericVector> eval(conn,
								"listMarts()");
					} finally {
						conn.close();
					}
					biomartDatabaseDialog.replaceListItems(Arrays
							.asList(((REXPFactor) martsList.asList().get(0))
									.asStrings()), null);
				} catch (final RserveException e1) {
					logger.error("Problem loading the possible databases", e1);
				} catch (final REXPMismatchException e1) {
					logger.error("Unable to list the datasets ", e1);
				}
			}
		};
		proxyFromEclipse.getModel().addChangeListener(
				proxyFromEclipseChangeListener);
		addDialogComponent(proxyHost);
		addDialogComponent(proxyPort);
		addDialogComponent(proxyUser);
		addDialogComponent(proxyPassword);
		addDialogComponent(new UpdatableComponent() {
			@Override
			protected void updateComponent() /* => */{
				proxyFromEclipseChangeListener.stateChanged(null);
			}
		});
	}

	private void setProxy(final RConnection conn,
			final DialogComponentString proxyHost,
			final DialogComponentNumberEdit proxyPort,
			final DialogComponentString proxyUser,
			final DialogComponentPasswordField proxyPassword)
			throws RserveException, REXPMismatchException {
		final String host = ((SettingsModelString) proxyHost.getModel())
				.getStringValue();
		if (!host.isEmpty()) {
			// http://username:password@proxy.server:8080
			final StringBuilder proxyString = new StringBuilder("http://");
			final String user = ((SettingsModelString) proxyUser.getModel())
					.getStringValue();
			if (!user.isEmpty()) {
				proxyString.append(user);
				final String password = ((SettingsModelString) proxyPassword
						.getModel()).getStringValue();
				if (!password.isEmpty()) {
					proxyString.append(':').append(password);
				}
				proxyString.append("@");
			}
			proxyString.append(host).append(':')
					.append(
							((SettingsModelInteger) proxyPort.getModel())
									.getIntValue());
			RUtil.voidEval(conn, "Sys.setenv(\"http_proxy\" = \"" + proxyString
					+ "\")");
		}
	}
}

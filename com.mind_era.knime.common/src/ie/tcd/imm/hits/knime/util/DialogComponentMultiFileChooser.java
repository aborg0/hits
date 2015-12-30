/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package ie.tcd.imm.hits.knime.util;

import ie.tcd.imm.hits.util.FilenameFilterWrapper;
import ie.tcd.imm.hits.util.file.ListContents;
import ie.tcd.imm.hits.util.file.OpenStream;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponent;
import org.knime.core.node.defaultnodesettings.SettingsModelStringArray;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.StringHistory;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * A {@link DialogComponent} for {@link SettingsModelStringArray} models. It is
 * able to select some files in a directory/folder.
 * <p>
 * (Subclassing, change listener friendly.)
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
@NotThreadSafe
@Nonnull
public class DialogComponentMultiFileChooser extends DialogComponent {
	private static final NodeLogger logger = NodeLogger
			.getLogger(DialogComponentMultiFileChooser.class);

	/** This selects the folder. */
	protected final JComboBox<String> dirNameComboBox = new JComboBox<>();
	private final StringHistory stringHistory;
	private final JButton browseButton = new JButton("Browse");
	private final FilenameFilter possibleExtensions;
	private final DefaultListModel<String> fileNameModel = new DefaultListModel<>();
	/** This is where the filenames are shown. */
	protected final JList<String> fileNameList = new JList<>(fileNameModel);

	private final TitledBorder border = new TitledBorder("");

	private ListContents contentsLister = new ListContents();

	/**
	 * @param model
	 *            The {@link SettingsModelStringArray model} holding the full
	 *            filenames. (Not {@code null}.)
	 * @param fileNameLabel
	 *            This will be shown on the left of {@link #fileNameList}.
	 * @param historyId
	 *            This identifies the file history.
	 * @param visibleRowCount
	 *            Shows these many lines.
	 * @param validExtensions
	 *            Only files with these extensions are shown.
	 */
	@SuppressFBWarnings("RCN")
	public DialogComponentMultiFileChooser(
			final SettingsModelStringArray model, final String fileNameLabel,
			final String historyId, @Nonnegative final int visibleRowCount,
			final String... validExtensions) {
		super(model);
		dirNameComboBox.setEditable(true);
		stringHistory = StringHistory.getInstance(historyId);
		final HashSet<String> extensions = new HashSet<String>(
				validExtensions == null ? 1 : validExtensions.length * 2);
		if (validExtensions == null) {
			extensions.add("");
		} else {
			for (final String extension : validExtensions) {
				extensions.add(extension.toLowerCase());
			}
		}
		possibleExtensions = new FilenameFilter() {
			@Override
			public boolean accept(final File dir, final String name) {
				for (final String extension : extensions) {
					if (name.toLowerCase().endsWith(extension)) {
						return true;
					}
				}
				return false;
			}

			/*
			 * (non-Javadoc)
			 * 
			 * @see java.lang.Object#toString()
			 */
			@Override
			public String toString() {
				final StringBuilder ret = new StringBuilder();
				for (final String extension : extensions) {
					ret.append("*").append(extension).append("; ");
				}
				if (ret.length() > 2) {
					ret.setLength(ret.length() - "; ".length());
				}
				return ret.toString();
			}
		};
		dirNameComboBox.setPreferredSize(new Dimension(320, dirNameComboBox
				.getPreferredSize().height));
		dirNameComboBox.removeAllItems();
		for (final String string : stringHistory.getHistory()) {
			dirNameComboBox.addItem(string);
		}
		final JPanel dirPanel = new JPanel();
		dirPanel.add(dirNameComboBox);
		dirPanel.add(browseButton);
		browseButton.addActionListener(new ActionListener() {
			@java.lang.SuppressWarnings("synthetic-access")
			@Override
			public void actionPerformed(final ActionEvent e)/* => */{
				final String selectedDir = getCurrentSelection();
				final JFileChooser fileChooser = new JFileChooser(selectedDir);
				fileChooser
						.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
				fileChooser.setFileFilter(new FilenameFilterWrapper(
						possibleExtensions, true));
				fileChooser.setDialogType(JFileChooser.OPEN_DIALOG);
				final int returnVal = fileChooser.showDialog(
						getComponentPanel().getParent(), null);
				if (returnVal == JFileChooser.APPROVE_OPTION) {
					final File selectedFile = fileChooser.getSelectedFile();
					final String newDir = (selectedFile.isDirectory()
							|| selectedFile.isFile()
							&& selectedFile.getName().toLowerCase().endsWith(
									"zip") ? selectedFile : selectedFile
							.getParentFile()).toURI().toString();
					dirNameComboBox.removeItem(newDir);
					dirNameComboBox.addItem(newDir);
					dirNameComboBox.setSelectedItem(newDir);
					updateList(newDir);
				}
			}
		});
		dirNameComboBox.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(final ItemEvent e) /* => */{
				updateList(getCurrentSelection());
			}
		});
		getComponentPanel().setLayout(new BorderLayout());
		getComponentPanel().add(dirPanel, BorderLayout.NORTH);
		final JPanel fileNamePanel = new JPanel(new BorderLayout());
		final JPanel labelAndButtonsPanel = new JPanel(new BorderLayout());

		final JButton moveUpButton = new JButton("^");
		labelAndButtonsPanel.add(moveUpButton, BorderLayout.NORTH);
		// final int size = moveUpButton.getPreferredSize().height;
		// moveUpButton.setPreferredSize(new Dimension(size, size));
		moveUpButton.addActionListener(new SelectionMoverActionListener<String>(
				fileNameList, fileNameModel, -1, getModel()));
		labelAndButtonsPanel
				.add(new JLabel(fileNameLabel), BorderLayout.CENTER);
		final JButton moveDownButton = new JButton("v");
		moveDownButton.addActionListener(new SelectionMoverActionListener<String>(
				fileNameList, fileNameModel, 1, getModel()));
		// moveDownButton.setPreferredSize(new Dimension(size, size));
		labelAndButtonsPanel.add(moveDownButton, BorderLayout.SOUTH);
		fileNameList.setVisibleRowCount(visibleRowCount);
		fileNamePanel.add(labelAndButtonsPanel, BorderLayout.WEST);
		fileNamePanel.add(new JScrollPane(fileNameList), BorderLayout.CENTER);
		getComponentPanel().add(fileNamePanel, BorderLayout.CENTER);
		getComponentPanel().setBorder(border);
		updateList(getCurrentSelection());
		fileNameList.repaint();
		getComponentPanel().validate();
	}

	/**
	 * Updates the list of the file names.
	 * 
	 * @param newDir
	 *            A folder name. (Not {@code null}.)
	 */
	void updateList(final String newDir) {
		fileNameModel.clear();
		if (newDir != null) {
			try {
				final Future<Map<String, URI>> contents = contentsLister
						.asyncFindContents(OpenStream.convertURI(newDir), 2);
				final List<String> newContents = new ArrayList<String>();
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run()/* => */{
						try {
							fileNameModel.clear();
							for (final Entry<String, URI> entry : contents
									.get().entrySet()) {
								if (possibleExtensions.accept(null, entry
										.getValue().toString())) {
									newContents.add(entry.getKey());
								}
							}
						} catch (final InterruptedException e) {
							getComponentPanel().revalidate();
							return;
						} catch (final ExecutionException e) {
							logger.debug(e.getMessage(), e);
							getComponentPanel().revalidate();
							return;
						}
						Collections.sort(newContents);
						for (final String string : newContents) {
							fileNameModel.addElement(string);
						}
						getComponentPanel().revalidate();
					}
				});
			} catch (final URISyntaxException e) {
				// do nothing.
			} catch (final RuntimeException e) {
				// do nothing.
			}
			// final File dir = new File(newDir);
			// final File[] files = dir.listFiles(possibleExtensions);
			// if (files != null) {
			// Arrays.sort(files);
			// for (final File file : files) {
			// fileNameModel.addElement(file.getName());
			// }
			// }
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seeorg.knime.core.node.defaultnodesettings.DialogComponent#
	 * checkConfigurabilityBeforeLoad(org.knime.core.data.DataTableSpec[])
	 */
	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void checkConfigurabilityBeforeLoad(final PortObjectSpec[] specs)
			throws NotConfigurableException {
		// No check needed.
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.knime.core.node.defaultnodesettings.DialogComponent#setEnabledComponents
	 * (boolean)
	 */
	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void setEnabledComponents(final boolean enabled) {
		browseButton.setEnabled(enabled);
		dirNameComboBox.setEnabled(enabled);
		fileNameList.setEnabled(enabled);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.knime.core.node.defaultnodesettings.DialogComponent#setToolTipText
	 * (java.lang.String)
	 */
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setToolTipText(final String text) {
		if (text != null) {
			getComponentPanel().setToolTipText(text);
			fileNameList.setToolTipText(text);
			dirNameComboBox.setToolTipText(text);
			browseButton.setToolTipText(text);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.knime.core.node.defaultnodesettings.DialogComponent#updateComponent()
	 */
	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void updateComponent() {
		final SettingsModelStringArray model = (SettingsModelStringArray) getModel();
		final String[] strings = model.getStringArrayValue();
		final String[] fileNames = new String[strings.length];
		final URI[] uris = new URI[strings.length];
		for (int i = strings.length; i-- > 0;) {
			try {
				uris[i] = new URI(strings[i]);
			} catch (final URISyntaxException e) {
				throw new IllegalStateException(e);
			}
		}
		URI baseUri = OpenStream.findBaseUri(uris);
		for (int i = 0; i < strings.length; ++i) {
			final String fileName = strings[i];
			try {
				final URI uri = new URI(fileName);
				uris[i] = uri;
				fileNames[i] = baseUri.relativize(uri).getPath();
			} catch (final URISyntaxException e) {
				throw new IllegalStateException(e);
			}
		}
		final String homeDirName = System.getProperty("user.home");
		if (baseUri == null) {
			final File dir = new File(homeDirName);
			baseUri = dir.toURI();
		}
		dirNameComboBox.getModel().setSelectedItem(baseUri.toString());
		try {
			final Map<String, URI> contents = ListContents.findContents(
					baseUri, 2);
			fileNameModel.clear();
			final SortedSet<String> names = new TreeSet<String>(contents
					.keySet());
			final List<String> toRemove = new ArrayList<String>();
			for (final String name : names) {
				if (!possibleExtensions.accept(null, name)) {
					toRemove.add(name);
				}
			}
			names.removeAll(toRemove);
			for (final String fileName : fileNames) {
				if (!names.contains(fileName)) {
					logger.warn("The file: " + fileName
							+ " is no longer available.",
							new IllegalStateException("The file: " + fileName
									+ " is no longer available."));
				}
				fileNameModel.addElement(fileName);
				names.remove(fileName);
			}
			final int[] indices = new int[fileNames.length];
			for (int i = indices.length; i-- > 0;) {
				indices[i] = i;
			}
			fileNameList.setSelectedIndices(indices);
			for (final String name : names) {
				fileNameModel.addElement(name);
			}
			getComponentPanel().validate();
		} catch (final IOException e) {
			return;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void validateSettingsBeforeSave() throws InvalidSettingsException {
		final List<String> values = fileNameList.getSelectedValuesList();
		if (values == null || values.size() < 1) {
			((SettingsModelStringArray) getModel())
					.setStringArrayValue(new String[0]);
		} else {
			// final File dir = new File(getCurrentSelection());
			// if (!dir.isDirectory()) {
			// throw new InvalidSettingsException(
			// "The selected directory is not directory: "
			// + dir.getAbsolutePath());
			// }
			final String[] selectedValues = new String[values.size()];
			for (int i = 0, length = values.size(); i < length; i++) {
				// final File file = new File(dir, values[i].toString());
				// if (!file.canRead()) {
				// throw new InvalidSettingsException(
				// "The selected file is not readable: "
				// + file.getAbsolutePath());
				// }
				try {
					selectedValues[i] = OpenStream.convertURI(
							getCurrentSelection())
							.resolve(values.get(i)).toString();
				} catch (final URISyntaxException e) {
					throw new InvalidSettingsException(
							"Wrong file name or folder: " + e.getMessage(), e);
				}
				// file.getAbsolutePath();
			}
			// we transfer the value from the field into the model
			((SettingsModelStringArray) getModel())
					.setStringArrayValue(selectedValues);
		}
		stringHistory.add(getCurrentSelection());
	}

	/**
	 * @return The currently selected {@link String} in {@link #dirNameComboBox}
	 *         .
	 */
	protected String getCurrentSelection() {
		final String select = dirNameComboBox.getEditor().getItem().toString();
		return select == null || select.length() == 0 ? (String) dirNameComboBox
				.getSelectedItem()
				: select;
	}

	/**
	 * Sets the title of the component to {@code title}.
	 * 
	 * @param title
	 *            A non-{@code null} {@link String}.
	 */
	public void setBorderTitle(final String title) {
		border.setTitle(title);
	}
}

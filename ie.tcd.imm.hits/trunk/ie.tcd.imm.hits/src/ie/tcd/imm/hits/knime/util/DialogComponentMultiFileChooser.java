/**
 * 
 */
package ie.tcd.imm.hits.knime.util;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.io.FilenameFilter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.SortedSet;
import java.util.TreeSet;

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
import javax.swing.border.TitledBorder;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponent;
import org.knime.core.node.defaultnodesettings.SettingsModelStringArray;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.StringHistory;

import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.SuppressWarnings;

/**
 * A {@link DialogComponent} for {@link SettingsModelStringArray} models. It is
 * able to select some files in a directory/folder.
 * <p>
 * (Subclassing, change listener friendly.)
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
@NotThreadSafe
@DefaultAnnotation(Nonnull.class)
public class DialogComponentMultiFileChooser extends DialogComponent {
	private static final NodeLogger logger = NodeLogger
			.getLogger(DialogComponentMultiFileChooser.class);

	/** This selects the folder. */
	protected final JComboBox dirNameComboBox = new JComboBox();
	private final StringHistory stringHistory;
	private final JButton browseButton = new JButton("Browse");
	private final FilenameFilter possibleExtensions;
	private final DefaultListModel fileNameModel = new DefaultListModel();
	/** This is where the filenames are shown. */
	protected final JList fileNameList = new JList(fileNameModel);

	private final TitledBorder border = new TitledBorder("");

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
	@SuppressWarnings("RCN")
	public DialogComponentMultiFileChooser(
			final SettingsModelStringArray model, final String fileNameLabel,
			final String historyId, @Nonnegative
			final int visibleRowCount, final String... validExtensions) {
		super(model);
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
			@Override
			public void actionPerformed(final ActionEvent e) {
				final String selectedDir = getCurrentSelection();
				final JFileChooser fileChooser = new JFileChooser(selectedDir);
				fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				fileChooser.setDialogType(JFileChooser.OPEN_DIALOG);
				final int returnVal = fileChooser.showDialog(
						getComponentPanel().getParent(), null);
				if (returnVal == JFileChooser.APPROVE_OPTION) {
					final String newDir = fileChooser.getSelectedFile()
							.getAbsoluteFile().toString();
					dirNameComboBox.removeItem(newDir);
					dirNameComboBox.addItem(newDir);
					dirNameComboBox.setSelectedItem(newDir);
					updateList(newDir);
				}
			}
		});
		dirNameComboBox.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(final ItemEvent e) {
				updateList(getCurrentSelection());
			}
		});
		getComponentPanel().setLayout(new BorderLayout());
		getComponentPanel().add(dirPanel, BorderLayout.NORTH);
		final JPanel fileNamePanel = new JPanel(new BorderLayout());
		final JPanel labelAndButtonsPanel = new JPanel(new BorderLayout());

		final JButton moveUpButton = new JButton("^");
		labelAndButtonsPanel.add(moveUpButton, BorderLayout.NORTH);
		final int size = moveUpButton.getPreferredSize().height;
		moveUpButton.setPreferredSize(new Dimension(size, size));
		moveUpButton.addActionListener(new SelectionMoverActionListener(
				fileNameList, fileNameModel, -1));
		labelAndButtonsPanel
				.add(new JLabel(fileNameLabel), BorderLayout.CENTER);
		final JButton moveDownButton = new JButton("v");
		moveDownButton.addActionListener(new SelectionMoverActionListener(
				fileNameList, fileNameModel, 1));
		moveDownButton.setPreferredSize(new Dimension(size, size));
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
	private void updateList(final String newDir) {
		fileNameModel.clear();
		if (newDir != null) {
			final File dir = new File(newDir);
			final File[] files = dir.listFiles(possibleExtensions);
			if (files != null) {
				Arrays.sort(files);
				for (final File file : files) {
					fileNameModel.addElement(file.getName());
				}
			}
		}
		getComponentPanel().revalidate();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.knime.core.node.defaultnodesettings.DialogComponent#checkConfigurabilityBeforeLoad(org.knime.core.data.DataTableSpec[])
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
	 * @see org.knime.core.node.defaultnodesettings.DialogComponent#setEnabledComponents(boolean)
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
	 * @see org.knime.core.node.defaultnodesettings.DialogComponent#setToolTipText(java.lang.String)
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
	 * @see org.knime.core.node.defaultnodesettings.DialogComponent#updateComponent()
	 */
	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void updateComponent() {
		final SettingsModelStringArray model = (SettingsModelStringArray) getModel();
		final String[] strings = model.getStringArrayValue();
		String dirName = null;
		final String[] fileNames = new String[strings.length];
		for (int i = 0; i < strings.length; ++i) {
			final String fileName = strings[i];
			final File file = new File(fileName);
			if (file.isDirectory()) {
				throw new IllegalStateException(
						"Cannot put directories to files.: "
								+ file.getAbsolutePath());
			}
			if (dirName == null) {
				dirName = file.getParent();
			}
			if (!file.getParent().equals(dirName)) {
				throw new IllegalStateException(
						"Files from only one directory are accepted.");
			}
			fileNames[i] = file.getName();
		}
		final String currentSelection = getCurrentSelection();
		final String homeDirName = System.getProperty("user.home");
		final File dir = new File(
				dirName == null || !new File(dirName).isDirectory() ? (currentSelection == null ? homeDirName
						: new File(currentSelection).isDirectory() ? currentSelection
								: homeDirName)
						: dirName);
		dirNameComboBox.setSelectedItem(dir.getAbsolutePath());
		final File[] files = dir.listFiles(possibleExtensions);
		final SortedSet<String> names = new TreeSet<String>();
		if (files != null) {
			for (final File file : files) {
				names.add(file.getName());
			}
		}
		fileNameModel.clear();
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
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.knime.core.node.defaultnodesettings.DialogComponent#validateSettingsBeforeSave()
	 */
	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void validateSettingsBeforeSave() throws InvalidSettingsException {
		final Object[] values = fileNameList.getSelectedValues();
		if (values == null || values.length < 1) {
			((SettingsModelStringArray) getModel())
					.setStringArrayValue(new String[0]);
		} else {
			final File dir = new File(getCurrentSelection());
			if (!dir.isDirectory()) {
				throw new InvalidSettingsException(
						"The selected directory is not directory: "
								+ dir.getAbsolutePath());
			}
			final String[] selectedValues = new String[values.length];
			for (int i = 0, length = values.length; i < length; i++) {
				final File file = new File(dir, values[i].toString());
				if (!file.canRead()) {
					throw new InvalidSettingsException(
							"The selected file is not readable: "
									+ file.getAbsolutePath());
				}
				selectedValues[i] = file.getAbsolutePath();
			}
			// we transfer the value from the field into the model
			((SettingsModelStringArray) getModel())
					.setStringArrayValue(selectedValues);
		}
		stringHistory.add(getCurrentSelection());
	}

	/**
	 * @return The currently selected {@link String} in {@link #dirNameComboBox}.
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

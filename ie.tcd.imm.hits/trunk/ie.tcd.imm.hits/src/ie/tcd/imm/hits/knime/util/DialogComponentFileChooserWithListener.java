/**
 * 
 */
package ie.tcd.imm.hits.knime.util;

import java.awt.event.ActionListener;
import java.awt.event.ItemListener;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JPanel;

import org.knime.core.node.defaultnodesettings.DialogComponentFileChooser;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

import edu.umd.cs.findbugs.annotations.DefaultAnnotation;

/**
 * This component allows you to check the value on changes, and add
 * {@link ItemListener}s, or {@link ActionListener}s.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
@NotThreadSafe
@DefaultAnnotation(Nonnull.class)
public class DialogComponentFileChooserWithListener extends
		DialogComponentFileChooser {

	private final JComboBox combobox;

	/**
	 * Creates a {@link DialogComponentFileChooserWithListener} with minimal
	 * number of parameters.
	 * <p>
	 * Creates an {@link JFileChooser#OPEN_DIALOG open dialog} for files..
	 * 
	 * @param stringModel
	 *            The model holding the value
	 * @param historyID
	 *            Identifies the file history
	 * @param validExtensions
	 *            Only show files with these extensions
	 * @see DialogComponentFileChooserWithListener#DialogComponentFileChooserWithListener(SettingsModelString,
	 *      String, int, boolean, String...)
	 */
	public DialogComponentFileChooserWithListener(
			final SettingsModelString stringModel, final String historyID,
			final String... validExtensions) {
		this(stringModel, historyID, JFileChooser.OPEN_DIALOG, false,
				validExtensions);
	}

	/**
	 * Creates a {@link DialogComponentFileChooserWithListener}.
	 * 
	 * @param stringModel
	 *            The model holding the value
	 * @param historyID
	 *            Identifies the file history
	 * @param dialogType
	 *            The dialog type, see {@link JFileChooser#OPEN_DIALOG},
	 *            {@link JFileChooser#SAVE_DIALOG},
	 *            {@link JFileChooser#CUSTOM_DIALOG}.
	 * @param directoryOnly
	 *            Show only folders, or files too.
	 * @see DialogComponentFileChooserWithListener#DialogComponentFileChooserWithListener(SettingsModelString,
	 *      String, int, boolean, String...)
	 */
	public DialogComponentFileChooserWithListener(
			final SettingsModelString stringModel, final String historyID,
			final int dialogType, final boolean directoryOnly) {
		this(stringModel, historyID, dialogType, directoryOnly, new String[0]);
	}

	/**
	 * Creates a {@link DialogComponentFileChooserWithListener}. It shows files
	 * and folders too.
	 * 
	 * @param stringModel
	 *            The model holding the value
	 * @param historyID
	 *            Identifies the file history
	 * @param dialogType
	 *            The dialog type, see {@link JFileChooser#OPEN_DIALOG},
	 *            {@link JFileChooser#SAVE_DIALOG},
	 *            {@link JFileChooser#CUSTOM_DIALOG}.
	 * @param validExtensions
	 *            Only show files with these extensions
	 * @see DialogComponentFileChooserWithListener#DialogComponentFileChooserWithListener(SettingsModelString,
	 *      String, int, boolean, String...)
	 */
	public DialogComponentFileChooserWithListener(
			final SettingsModelString stringModel, final String historyID,
			final int dialogType, final String... validExtensions) {
		this(stringModel, historyID, dialogType, false, validExtensions);
	}

	/**
	 * Creates a {@link DialogComponentFileChooserWithListener}.
	 * 
	 * @param stringModel
	 *            The model holding the value
	 * @param historyID
	 *            Identifies the file history
	 * @param dialogType
	 *            The dialog type, see {@link JFileChooser#OPEN_DIALOG},
	 *            {@link JFileChooser#SAVE_DIALOG},
	 *            {@link JFileChooser#CUSTOM_DIALOG}.
	 * @param directoryOnly
	 *            Show only folders, or files too.
	 * @param validExtensions
	 *            Only show files with these extensions
	 */
	public DialogComponentFileChooserWithListener(
			final SettingsModelString stringModel, final String historyID,
			final int dialogType, final boolean directoryOnly,
			final String... validExtensions) {
		super(stringModel, historyID, dialogType, directoryOnly,
				validExtensions);
		final JPanel panel = getComponentPanel();
		combobox = (JComboBox) ((JPanel) panel.getComponent(0)).getComponent(0);
	}

	/**
	 * @return The currently selected item in the {@link JComboBox}.
	 */
	public String getCurrentSelection() {
		final String select = (String) combobox.getEditor().getItem();
		return select == null || select.length() == 0 ? (String) combobox
				.getSelectedItem() : select;
	}

	/**
	 * 
	 * @see JComboBox#addItemListener(ItemListener)
	 * @param listener
	 *            An {@link ItemListener}
	 */
	public void addItemListener(final ItemListener listener) {
		combobox.addItemListener(listener);
	}

	/**
	 * 
	 * @see JComboBox#removeItemListener(ItemListener)
	 * @param listener
	 *            An {@link ItemListener}
	 */
	public void removeItemListener(final ItemListener listener) {
		combobox.removeItemListener(listener);
	}

	/**
	 * @see JComboBox#addActionListener(ActionListener)
	 * 
	 * @param listener
	 *            An {@link ActionListener}.
	 */
	public void addActionListener(final ActionListener listener) {
		combobox.addActionListener(listener);
	}

	/**
	 * @see JComboBox#removeActionListener(ActionListener)
	 * 
	 * @param listener
	 *            An {@link ActionListener}.
	 */
	public void removeActionListener(final ActionListener listener) {
		combobox.removeActionListener(listener);
	}
}

/**
 * 
 */
package ie.tcd.imm.hits.knime.util;

import java.awt.event.ActionListener;
import java.awt.event.ItemListener;

import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JPanel;

import org.knime.core.node.defaultnodesettings.DialogComponentFileChooser;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * This component allows you to check the value on changes.
 * 
 * @author bakosg@tcd.ie
 */
public class DialogComponentFileChooserWithListener extends
		DialogComponentFileChooser {

	private final JComboBox combobox;

	/**
	 * {@inheritDoc}
	 */
	public DialogComponentFileChooserWithListener(
			final SettingsModelString stringModel, final String historyID,
			final String... validExtensions) {
		this(stringModel, historyID, JFileChooser.OPEN_DIALOG, false,
				validExtensions);
	}

	/**
	 * {@inheritDoc}
	 */
	public DialogComponentFileChooserWithListener(
			final SettingsModelString stringModel, final String historyID,
			final int dialogType, final boolean directoryOnly) {
		this(stringModel, historyID, dialogType, directoryOnly, new String[0]);
	}

	/**
	 * {@inheritDoc}
	 */
	public DialogComponentFileChooserWithListener(
			final SettingsModelString stringModel, final String historyID,
			final int dialogType, final String... validExtensions) {
		this(stringModel, historyID, dialogType, false, validExtensions);
	}

	/**
	 * {@inheritDoc}
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

	public void addActionListener(final ActionListener listener) {
		combobox.addActionListener(listener);
	}

	public void removeActionListener(final ActionListener listener) {
		combobox.removeActionListener(listener);
	}
}

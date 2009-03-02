/**
 * 
 */
package ie.tcd.imm.hits.knime.view.heatmap;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;

/**
 * This panel allows the user to save/load/delete view configurations ({@link ViewModel}s).
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
public class SettingsPanel extends JPanel {
	private static final long serialVersionUID = -8855376941992295369L;
	private final JComboBox list = new JComboBox(new Object[] { "default" });
	private final JButton load = new JButton("load");
	private final JButton save = new JButton("save");
	private final JButton delete = new JButton("delete");

	/**
	 * Constructs a {@link SettingsPanel}.
	 */
	public SettingsPanel() {
		super();
		final JPanel panel = new JPanel();
		list.setEnabled(false);
		panel.add(list);
		load.setEnabled(false);
		panel.add(load);
		delete.setEnabled(false);
		panel.add(delete);
		save.setEnabled(false);
		panel.add(save);
		add(panel);
	}
}

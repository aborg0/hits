package ie.tcd.imm.hits.gui;

import java.awt.Component;

import javax.swing.JFrame;
import javax.swing.WindowConstants;

/**
 * Wraps a JComponent with a JFrame for analyse with Abbot.
 * 
 * @author <a href="bakosg@tcd.ie">Gabor Bakos</a>
 */
public class WrapComponent {
	/**
	 * Test class for automated testing.
	 * 
	 * @param component
	 *            The component to wrap.
	 */
	public static void tester(final Component component) {
		// final ColourSelector selector = new ColourSelector(Collections
		// .singletonList("param"), Collections
		// .singletonList(StatTypes.score));
		final JFrame frame = new JFrame("colour selector tester");
		frame.getContentPane().add(component);
		frame.pack();
		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		frame.setVisible(true);
	}
}

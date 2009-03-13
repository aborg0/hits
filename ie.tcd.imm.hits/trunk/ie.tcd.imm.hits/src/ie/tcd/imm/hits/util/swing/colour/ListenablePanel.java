/**
 * 
 */
package ie.tcd.imm.hits.util.swing.colour;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.swing.JPanel;

import edu.umd.cs.findbugs.annotations.DefaultAnnotation;

/**
 * A {@link JPanel} with listeners.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
@DefaultAnnotation( { Nonnull.class, CheckReturnValue.class })
public abstract class ListenablePanel extends JPanel {
	private final List<ActionListener> listeners = new ArrayList<ActionListener>();

	/**
	 * Adds the {@link ActionListener} to the listeners if not previously
	 * contained.
	 * 
	 * @param actionListener
	 *            An {@link ActionListener}.
	 */
	public void addActionListener(final ActionListener actionListener) {
		listeners.add(actionListener);
	}

	/**
	 * Notifies the listeners about an action.
	 */
	protected void fireModelChange() {
		final ActionEvent actionEvent = new ActionEvent(this, (int) (System
				.currentTimeMillis() & 0xffffffff), "modelReplaced");
		for (final ActionListener listener : listeners) {
			listener.actionPerformed(actionEvent);
		}
	}
}

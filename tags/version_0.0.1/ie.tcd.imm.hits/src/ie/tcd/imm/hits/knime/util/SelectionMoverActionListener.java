/**
 * 
 */
package ie.tcd.imm.hits.knime.util;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.Serializable;
import java.util.Arrays;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JList;

import edu.umd.cs.findbugs.annotations.DefaultAnnotation;

/**
 * When attached to an {@link ActionEvent} emitter (like a {@link JButton}), it
 * will move the selection(s) of the {@link #list referenced} {@link JList} up
 * or down (by {@link #move}).
 * <p>
 * The {@link #list} must has the {@link DefaultListModel}, {@link #model}.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
@NotThreadSafe
@DefaultAnnotation(Nonnull.class)
public class SelectionMoverActionListener implements ActionListener,
		Serializable {
	private static final long serialVersionUID = -2883537212024563886L;
	private final JList list;
	private final DefaultListModel model;
	private final int move;

	/**
	 * 
	 * @param list
	 *            A {@link JList} for which selections we listen to.
	 * @param model
	 *            The {@link DefaultListModel} of {@code list}.
	 * @param move
	 *            We move the selection by this amount.
	 */
	public SelectionMoverActionListener(final JList list,
			final DefaultListModel model, final int move) {
		super();
		this.list = list;
		this.model = model;
		this.move = move;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	/**
	 * Moves the selection up or down.
	 * 
	 * {@inheritDoc}
	 */
	@Override
	public void actionPerformed(final ActionEvent e) {
		final int[] selectedIndices = list.getSelectedIndices();
		Arrays.sort(selectedIndices);
		final int max = model.getSize();
		if (selectedIndices.length > 0
				&& (move > 0 && selectedIndices[selectedIndices.length - 1]
						+ move < max)
				|| (move < 0 && selectedIndices[0] + move >= 0)) {
			final int sgn = move > 0 ? 1 : -1;
			final int start = sgn == -1 ? 0 : selectedIndices.length - 1;
			final int end = sgn == -1 ? selectedIndices.length : -1;
			// for (final int selected : selectedIndices) {
			for (int j = start; j != end; j -= sgn) {
				final int selected = selectedIndices[j];
				final Object tmp = model.get(selected);
				for (int i = move; i != 0; i -= sgn) {
					model.setElementAt(model.get(selected + move), selected
							+ move - sgn);
				}
				model.setElementAt(tmp, selected + move);
			}
			for (int i = 0; i < selectedIndices.length; i++) {
				selectedIndices[i] += move;
			}
			list.setSelectedIndices(selectedIndices);
		}
	}
}

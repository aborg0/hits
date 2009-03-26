package ie.tcd.imm.hits.util.swing;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JPopupMenu;

public class PopupListener extends MouseAdapter {
	private final JPopupMenu popup;

	public PopupListener(final JPopupMenu popupMenu) {
		popup = popupMenu;
	}

	public void mousePressed(final MouseEvent e) {
		maybeShowPopup(e);
	}

	public void mouseReleased(final MouseEvent e) {
		maybeShowPopup(e);
	}

	private void maybeShowPopup(final MouseEvent e) {
		if (e.isPopupTrigger()) {
			popup.show(e.getComponent(), e.getX(), e.getY());
		}
	}
}

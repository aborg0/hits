/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package ie.tcd.imm.hits.util.swing;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JPopupMenu;

/**
 * A simple popup listener. (Like in the tutorial.)
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
public class PopupListener extends MouseAdapter {
	private final JPopupMenu popup;

	/**
	 * Constructs the listener.
	 * 
	 * @param popupMenu
	 *            The popup menu to show.
	 */
	public PopupListener(final JPopupMenu popupMenu) {
		popup = popupMenu;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void mousePressed(final MouseEvent e) {
		maybeShowPopup(e);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void mouseReleased(final MouseEvent e) {
		maybeShowPopup(e);
	}

	private void maybeShowPopup(final MouseEvent e) {
		if (e.isPopupTrigger()) {
			popup.show(e.getComponent(), e.getX(), e.getY());
		}
	}
}

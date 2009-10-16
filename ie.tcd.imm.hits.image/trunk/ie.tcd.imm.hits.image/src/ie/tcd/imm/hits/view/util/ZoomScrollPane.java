/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package ie.tcd.imm.hits.view.util;

import ie.tcd.imm.hits.view.util.Zoomable.ZoomListener.ZoomEvent;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;
import javax.swing.BoundedRangeModel;
import javax.swing.JScrollPane;

/**
 * A {@link JScrollPane} which supports nofitication on zoom changes (Ctrl+mouse
 * wheel) and dragging of content.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
public class ZoomScrollPane extends JScrollPane implements Zoomable {
	private static final long serialVersionUID = -1342681994932246945L;
	private final BoundedRangeModel zoomModel;

	private List<ZoomListener> listeners = new ArrayList<ZoomListener>();

	/**
	 * @param zoomModel
	 *            A {@link BoundedRangeModel} to get the actual zoom factor in
	 *            percentages.
	 */
	public ZoomScrollPane(final BoundedRangeModel zoomModel) {
		this(null, VERTICAL_SCROLLBAR_AS_NEEDED,
				HORIZONTAL_SCROLLBAR_AS_NEEDED, zoomModel);
	}

	/**
	 * @param view
	 *            The component to display in the viewport.
	 * @param zoomModel
	 *            A {@link BoundedRangeModel} to get the actual zoom factor in
	 *            percentages.
	 */
	public ZoomScrollPane(final Component view,
			final BoundedRangeModel zoomModel) {
		this(view, VERTICAL_SCROLLBAR_AS_NEEDED,
				HORIZONTAL_SCROLLBAR_AS_NEEDED, zoomModel);
	}

	/**
	 * @param vsbPolicy
	 *            Vertical scrollbar policy
	 * @param hsbPolicy
	 *            Horizontal scrollbar policy
	 * @param zoomModel
	 *            A {@link BoundedRangeModel} to get the actual zoom factor in
	 *            percentages.
	 */
	public ZoomScrollPane(final int vsbPolicy, final int hsbPolicy,
			final BoundedRangeModel zoomModel) {
		this(null, vsbPolicy, hsbPolicy, zoomModel);
	}

	/**
	 * @param view
	 *            The component to display in the viewport.
	 * @param vsbPolicy
	 *            Vertical scrollbar policy
	 * @param hsbPolicy
	 *            Horizontal scrollbar policy
	 * @param zoomModel
	 *            A {@link BoundedRangeModel} to get the actual zoom factor in
	 *            percentages.
	 */
	public ZoomScrollPane(@Nullable final Component view, final int vsbPolicy,
			final int hsbPolicy, final BoundedRangeModel zoomModel) {
		super(view, vsbPolicy, hsbPolicy);
		this.zoomModel = zoomModel;
		init();
	}

	private void init() {
		final MouseAdapter mouseAdapter = new MouseAdapter() {
			private boolean dragStarted = true;
			private int startX, startY;
			int origViewX;
			int origViewY;
			private Cursor origCursor;

			@Override
			public void mousePressed(final MouseEvent e) {
				if (e.getButton() == MouseEvent.BUTTON1) {
					dragStarted = true;
					startX = e.getX();
					startY = e.getY();
					origViewX = getViewport().getViewPosition().x;
					origViewY = getViewport().getViewPosition().y;
					origCursor = getViewport().getView().getCursor();
					getViewport().getView().setCursor(
							Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
				}
			}

			@Override
			public void mouseDragged(final MouseEvent e) {
				if (!e.isAltDown() && !e.isControlDown() && !e.isShiftDown()
						&& !e.isMetaDown() && dragStarted) {
					final int maxX = Math.max(0, getViewport().getView()
							.getWidth()
							- getViewport().getWidth());

					final int maxY = Math.max(0, getViewport().getView()
							.getHeight()
							- getViewport().getHeight());
					final int deltaX = e.getX() - startX;
					final int deltaY = e.getY() - startY;
					getViewport().setViewPosition(
							new Point(Math.min(Math.max(0, origViewX - deltaX),
									maxX), Math.min(Math.max(0, origViewY
									- deltaY), maxY)));
				}
			}

			@Override
			public void mouseReleased(final MouseEvent e) {
				dragStarted = e.getButton() == MouseEvent.BUTTON1 ? false
						: dragStarted;
				if (origCursor != null) {
					getViewport().getView().setCursor(origCursor);
				}
			}

			@Override
			public void mouseWheelMoved(final MouseWheelEvent e) {
				if (e.isControlDown()) {
					notifyListeners(new ZoomEvent.Impl(zoomModel.getValue()
							+ e.getWheelRotation()));
					// zoomModel.setValue();
				} else {
					final int maxY = Math.max(0, getViewport().getView()
							.getHeight()
							- getViewport().getHeight());
					final int origY = getViewport().getViewPosition().y;
					final int y = Math.min(Math.max(0, origY
							+ e.getUnitsToScroll() * 3), maxY);
					getViewport().setViewPosition(
							new Point(getViewport().getViewPosition().x, y));
				}
			}

		};
		addMouseListener(mouseAdapter);
		addMouseMotionListener(mouseAdapter);
		addMouseWheelListener(mouseAdapter);
	}

	/**
	 * Sends {@code event} to each listener.
	 * 
	 * @param event
	 *            The event to send.
	 */
	protected void notifyListeners(final ZoomEvent event) {
		for (final ZoomListener listener : listeners) {
			listener.zoom(event);
		}
	}

	/** {@inheritDoc} */
	@Override
	public void addZoomListener(final ZoomListener listener) {
		listeners.add(listener);
	}

	/** {@inheritDoc} */
	@Override
	public void removeAllZoomListerners() {
		listeners.clear();
	}
}

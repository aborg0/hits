/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package ie.tcd.imm.hits.view.util;

/**
 * An interface to zoom make a container zoomable.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
public interface Zoomable {
	/**
	 * Listens to {@link ZoomListener.ZoomEvent}s.
	 */
	public interface ZoomListener {
		/** A container for the new zoom factor in percentages. */
		public interface ZoomEvent {
			/** @return the new zoom factor in percentages. */
			public int zoomFactor();

			/** Default implementation of {@link ZoomEvent}. */
			public static class Impl implements ZoomEvent {

				private int factor;

				/**
				 * @param factor
				 *            The new zoom factor.
				 */
				public Impl(final int factor) {
					super();
					this.factor = factor;
				}

				@Override
				public int zoomFactor() {
					return factor;
				}
			}
		}

		/**
		 * Performs zooming.
		 * 
		 * @param event
		 *            A {@link ZoomEvent}.
		 */
		public void zoom(ZoomEvent event);
	}

	/**
	 * Adds a {@link ZoomListener} to the container.
	 * 
	 * @param listener
	 *            A ZoomListener.
	 */
	public void addZoomListener(ZoomListener listener);

	/**
	 * Removes all registered {@link ZoomListener}s from the container.
	 */
	public void removeAllZoomListerners();
}

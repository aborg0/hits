/*
 * All rights reserved. (C) Copyright 2011, Gabor Bakos
 */
package ie.tcd.imm.hits.knime.view;

import javax.swing.JComponent;

import org.knime.core.data.image.ImageContent;

/**
 * An interface to paint a {@link JComponent} to an {@link ImageContent}
 * 
 * @author Gabor Bakos
 */
public interface ComponentPainter {
	/**
	 * Draws the component to {@link ImageContent}.
	 * 
	 * @param component
	 *            A {@link JComponent}.
	 * @return The content of {@code component} in {@link ImageContent} format.
	 */
	public ImageContent paint(final JComponent component);
}

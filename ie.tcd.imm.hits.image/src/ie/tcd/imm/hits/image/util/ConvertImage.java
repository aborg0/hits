/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package ie.tcd.imm.hits.image.util;

import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;

/**
 * Helper methods for image conversions.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
public class ConvertImage {

	/** Hide constructor */
	private ConvertImage() {
		super();
	}

	/**
	 * Converts an {@link Image} to a {@link BufferedImage}.
	 * 
	 * @param image
	 *            An {@link Image}.
	 * @return The {@code image} as a {@link BufferedImage}.
	 */
	@Deprecated
	public static BufferedImage toBufferedImage(final Image image) {
		final BufferedImage bi = new BufferedImage(image.getWidth(null), image
				.getHeight(null), BufferedImage.TYPE_INT_RGB);
		final Graphics g = bi.getGraphics();
		g.drawImage(image, 0, 0, null);
		g.dispose();
		return bi;
	}
}

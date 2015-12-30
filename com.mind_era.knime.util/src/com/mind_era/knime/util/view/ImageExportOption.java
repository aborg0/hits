/*
 * All rights reserved. (C) Copyright 2011, Bakos Gabor
 */
package com.mind_era.knime.util.view;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.JComponent;
import javax.swing.JLabel;

import org.apache.batik.dom.svg.SVGDOMImplementation;
import org.apache.batik.svggen.SVGGraphics2D;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.knime.base.data.xml.SvgImageContent;
import org.knime.core.data.image.ImageContent;
import org.knime.core.data.image.png.PNGImageContent;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.svg.SVGDocument;

import com.mind_era.knime.common.util.Displayable;

/**
 * The possible image export options.
 * 
 * @author Gabor Bakos
 */
public enum ImageExportOption implements Displayable, ComponentPainter {
	None("Do not export") {
		@Override
		public ImageContent paint(final JComponent component) {
			return Empty;
		}
	},
	Svg("SVG") {
		@Override
		public ImageContent paint(final JComponent comp) {
			Dimension size = comp.getPreferredSize();
			if (size.width <= 0 || size.width <= 0) {
				size = new Dimension(300, 300);
			}
			comp.setSize(size);

			final DOMImplementation domImpl = new SVGDOMImplementation();
			final String svgNS = "http://www.w3.org/2000/svg";
			final Document myFactory = domImpl.createDocument(svgNS, "svg",
					null);
			final SVGGraphics2D g = new SVGGraphics2D(myFactory);
			g.setColor(Color.GREEN);
			g.setSVGCanvasSize(size);

			comp.update(g);

			myFactory.replaceChild(g.getRoot(), myFactory.getDocumentElement());
			return new SvgImageContent((SVGDocument) myFactory);
		}
	},
	Png("PNG") {
		public ImageContent paint(final JComponent component) {
			Dimension size = component.getPreferredSize();
			if (size.width <= 0 || size.width <= 0) {
				size = new Dimension(300, 300);
			}
			component.setSize(size);
			final BufferedImage image = new BufferedImage(size.width,
					size.height, BufferedImage.TYPE_INT_ARGB);
			final Graphics2D g = image.createGraphics();
			component.update(g);
			final ByteArrayOutputStream baos = new ByteArrayOutputStream();
			try {
				ImageIO.write(image, "PNG", baos);
			} catch (final IOException e) {
				// Could not happen
				throw new RuntimeException(e);
			}
			return new PNGImageContent(baos.toByteArray());
		}
	};

	private final String displayText;

	/**
	 * @return The displayText.
	 */
	@Override
	public String getDisplayText() {
		return displayText;
	}

	private ImageExportOption(final String text) {
		this.displayText = text;
	}

	public static final SvgImageContent Empty = createEmpty();

	private static final SvgImageContent createEmpty() {
		// try {
		final DOMImplementation domImpl = new SVGDOMImplementation();
		final String svgNS = "http://www.w3.org/2000/svg";
		final Document myFactory = domImpl.createDocument(svgNS, "svg", null);
		final SVGGraphics2D g = new SVGGraphics2D(myFactory);
		final JLabel label = new JLabel("X");
		label.setSize(200, 200);
		label.update(g);
		g.setSVGCanvasSize(new Dimension(300, 300));
		myFactory.replaceChild(g.getRoot(), myFactory.getDocumentElement());
		return new SvgImageContent((SVGDocument) myFactory);
		// return new SvgImageContent(
		// new ByteArrayInputStream(
		// "<?xml version=\"1.0\" standalone=\"no\"?>\n<!DOCTYPE svg PUBLIC \"-//W3C//DTD SVG 20000802//EN\"\n\"http://www.w3.org/TR/2000/CR-SVG-20000802/DTD/svg-20000802.dtd\">\n<svg width=\"0\" height=\"0\"/>"
		// .getBytes()));
		// } catch (final IOException e) {
		// // should not happen
		// throw new RuntimeException(e);
		// }
	}
}

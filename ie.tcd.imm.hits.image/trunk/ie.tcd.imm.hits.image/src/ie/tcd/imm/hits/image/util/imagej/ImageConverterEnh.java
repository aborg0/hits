/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package ie.tcd.imm.hits.image.util.imagej;

import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;
import ij.process.LUT;

/**
 * Adds option to use convert with custom limits.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
public class ImageConverterEnh {

	private final ImagePlus imp;
	private final int type;

	/**
	 * @param imp
	 */
	public ImageConverterEnh(final ImagePlus imp) {
		super();
		this.imp = imp;
		type = imp.getType();
	}

	public synchronized void convertToGray8(final double min, final double max) {
		if (imp.getStackSize() > 1) {
			throw new IllegalArgumentException("Unsupported conversion");
		}
		final ImageProcessor ip = imp.getProcessor();
		switch (type) {
		case ImagePlus.GRAY8:
			imp.setProcessor(null, new TypeConverterEnh(ip).convertByteToByte(
					min, max));
			break;
		case ImagePlus.GRAY16:
			imp.setProcessor(null, new TypeConverterEnh(ip).convertShortToByte(
					min, max));
			break;
		case ImagePlus.GRAY32:
			throw new UnsupportedOperationException();
			// imp.setProcessor(null, new TypeConverterEnh(ip).convertIntToByte(
			// min, max));
		default:
			throw new UnsupportedOperationException();
		}
		imp.setCalibration(imp.getCalibration()); // update calibration
	}

	public synchronized void convertToRGB(final LUT lut) {
		if (imp.getStackSize() > 1) {
			throw new IllegalArgumentException("Unsupported conversion");
		}
		convertToGray8(lut.min, lut.max);
		final ColorProcessor colorProcessor = new ColorProcessor(imp.getImage());
		final int[] rgb = new int[256];
		lut.getRGBs(rgb);
		colorProcessor.applyTable(rgb, 1);
		imp.setProcessor(null, colorProcessor);
	}

	public synchronized void convertStackToRGB() {
		final ImageStack stack = imp.getStack();
		final ColorProcessor[] processors = new ColorProcessor[stack.getSize()];
		final ColorProcessor colorProcessor = new ColorProcessor(
				imp.getWidth(), imp.getHeight());
		for (int i = stack.getSize(); i-- > 0;) {
			processors[i] = (ColorProcessor) stack.getProcessor(i + 1);
		}
		final int[] pixels = new int[processors[0].getPixelCount()];
		for (final ColorProcessor cp : processors) {
			final int[] px = (int[]) cp.getPixels();
			for (int i = px.length; i-- > 0;) {
				final int b = pixels[i] & 0xff;
				final int g = (pixels[i] & 0xff00) >> 8;
				final int r = (pixels[i] & 0xff0000) >> 16;
				final int a = (pixels[i] & 0xff000000) >>> 24;
				final int ob = px[i] & 0xff;
				final int og = (px[i] & 0xff00) >> 8;
				final int or = (px[i] & 0xff0000) >> 16;
				final int oa = (px[i] & 0xff000000) >>> 24;
				final int l0 = cut(b + ob);
				final int l1 = cut(g + og);
				final int l2 = cut(r + or);
				final int l3 = cut(a + oa);
				pixels[i] = (l3 << 24) + (l2 << 16) + (l1 << 8) + l0;
			}
		}
		colorProcessor.setPixels(pixels);
		imp.setProcessor(null, colorProcessor);
	}

	/**
	 * @param i
	 *            converts to a byte value.
	 * @return A value between {@code 0} (inclusive) and {@code 255}
	 *         (inclusive).
	 */
	private int cut(final int i) {
		return i < 0 ? 0 : i > 255 ? 255 : i;
	}
}

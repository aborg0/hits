/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package ie.tcd.imm.hits.image.util.imagej;

import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ByteProcessor;
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
	 *            An {@link ImagePlus} object.
	 */
	public ImageConverterEnh(final ImagePlus imp) {
		super();
		this.imp = imp;
		type = imp.getType();
	}

	/**
	 * Converts to {@link ByteProcessor} with linear scaling between {@code min,
	 * max}. Only single grey scale images are supported.
	 * 
	 * @param min
	 *            The lower bound of the range. (Inclusive)
	 * @param max
	 *            The higher bound of the range. (Inclusive)
	 */
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

	/**
	 * Converts a single stacked image to the coloured image using {@code lut}.
	 * First converts to 8 bit representation with the {@code lut} min/max
	 * bounds.
	 * 
	 * @param lut
	 *            A {@link LUT}.
	 */
	public synchronized void convertToRGB(final LUT lut) {
		if (imp.getStackSize() > 1) {
			throw new IllegalArgumentException("Unsupported conversion");
		}
		convertToGray8(lut.min, lut.max);
		// final ColorProcessor colorProcessor = new
		// ColorProcessor(imp.getImage());
		final int[] rgb = new int[256];
		lut.getRGBs(rgb);
		applyTable(rgb);
		// colorProcessor.applyTable(rgb, 1);
		// imp.setProcessor(null, colorProcessor);
	}

	/**
	 * Applies a mapping from {@code 256} values to the specified rgb values
	 * coded in the {@code rgb} array.
	 * 
	 * @param rgb
	 *            A {@code 256} length in array.
	 */
	private void applyTable(final int[] rgb) {
		final int[] pixels = new int[imp.getProcessor().getPixelCount()];
		final byte[] p = (byte[]) imp.getProcessor().getPixels();
		final ColorProcessor cp = new ColorProcessor(imp.getWidth(), imp
				.getHeight(), pixels);
		for (int i = pixels.length; i-- > 0;) {
			final int idx = p[i] + 256 & 0xff;
			pixels[i] = rgb[idx];
		}
		imp.setProcessor(null, cp);

	}

	private interface FunctionIntIntInt {
		int convert(int i, int j);
	}

	/** The possible strategies to combine two rgba coloured values. */
	public static enum ConversionStrategy implements FunctionIntIntInt {
		/**
		 * Simply adds the values for each channel with {@code 0}, {@code 255}
		 * cutoffs.
		 */
		Additive {
			public int convert(final int rgb1, final int rgb2) {
				final int b = rgb1 & 0xff;
				final int g = (rgb1 & 0xff00) >> 8;
				final int r = (rgb1 & 0xff0000) >> 16;
				final int a = (rgb1 & 0xff000000) >>> 24;
				final int ob = rgb2 & 0xff;
				final int og = (rgb2 & 0xff00) >> 8;
				final int or = (rgb2 & 0xff0000) >> 16;
				final int oa = (rgb2 & 0xff000000) >>> 24;
				final int l0 = cut(b + ob);
				final int l1 = cut(g + og);
				final int l2 = cut(r + or);
				final int l3 = cut(a + oa);
				return (l3 << 24) + (l2 << 16) + (l1 << 8) + l0;
			}

		},
		/** Selects the minimal values for each channel. */
		Minimum {
			public int convert(final int rgb1, final int rgb2) {
				final int b = rgb1 & 0xff;
				final int g = (rgb1 & 0xff00) >> 8;
				final int r = (rgb1 & 0xff0000) >> 16;
				final int a = (rgb1 & 0xff000000) >>> 24;
				final int ob = rgb2 & 0xff;
				final int og = (rgb2 & 0xff00) >> 8;
				final int or = (rgb2 & 0xff0000) >> 16;
				final int oa = (rgb2 & 0xff000000) >>> 24;
				final int l0 = Math.min(b, ob);
				final int l1 = Math.min(g, og);
				final int l2 = Math.min(r, or);
				final int l3 = Math.min(a, oa);
				return (l3 << 24) + (l2 << 16) + (l1 << 8) + l0;
			}

		},
		/** Selects the maximal values for each channel. */
		Maximum {
			public int convert(final int rgb1, final int rgb2) {
				final int b = rgb1 & 0xff;
				final int g = (rgb1 & 0xff00) >> 8;
				final int r = (rgb1 & 0xff0000) >> 16;
				final int a = (rgb1 & 0xff000000) >>> 24;
				final int ob = rgb2 & 0xff;
				final int og = (rgb2 & 0xff00) >> 8;
				final int or = (rgb2 & 0xff0000) >> 16;
				final int oa = (rgb2 & 0xff000000) >>> 24;
				final int l0 = Math.max(b, ob);
				final int l1 = Math.max(g, og);
				final int l2 = Math.max(r, or);
				final int l3 = Math.max(a, oa);
				return (l3 << 24) + (l2 << 16) + (l1 << 8) + l0;
			}

		};

		/**
		 * @param i
		 *            a pixel value
		 * @param j
		 *            a pixel value
		 * @return combined pixel value
		 */
		@Override
		public abstract int convert(final int i, int j);

		/**
		 * @param i
		 *            converts to a byte value.
		 * @return A value between {@code 0} (inclusive) and {@code 255}
		 *         (inclusive).
		 */
		private static int cut(final int i) {
			return i < 0 ? 0 : i > 255 ? 255 : i;
		}
	}

	/**
	 * Converts a stack with {@link ColorProcessor}s to an RGB image, by adding
	 * the values (unsigned, bounded).
	 * 
	 * @param f
	 *            The function describing how to combine the two pixel values.
	 */
	public synchronized void convertStackToRGB(final FunctionIntIntInt f) {
		final ImageStack stack = imp.getStack();
		final ColorProcessor[] processors = new ColorProcessor[stack.getSize() - 1];
		final ColorProcessor colorProcessor = new ColorProcessor(
				imp.getWidth(), imp.getHeight());
		for (int i = stack.getSize(); i-- > 1;) {
			processors[i - 1] = (ColorProcessor) stack.getProcessor(i + 1);
		}
		final int[] pixels = new int[stack.getProcessor(1).getPixelCount()];
		System.arraycopy(stack.getProcessor(1).getPixelsCopy(), 0, pixels, 0,
				pixels.length);
		for (final ColorProcessor cp : processors) {
			final int[] px = (int[]) cp.getPixels();
			for (int i = px.length; i-- > 0;) {
				final int rgb1 = pixels[i];
				final int rgb2 = px[i];
				pixels[i] = f.convert(rgb1, rgb2);
			}
		}
		colorProcessor.setPixels(pixels);
		imp.setProcessor(null, colorProcessor);
	}

	/**
	 * Inverts the values for each channel.
	 */
	public synchronized void invert() {
		final ColorProcessor colorProcessor = new ColorProcessor(
				imp.getWidth(), imp.getHeight());
		final int[] px = (int[]) imp.getProcessor().getPixelsCopy();
		for (int i = px.length; i-- > 0;) {
			px[i] = invert(px[i]);
		}
		colorProcessor.setPixels(px);
		imp.setProcessor(null, colorProcessor);
	}

	/**
	 * @param val
	 *            Converts a pixel to the opposite value
	 * @return The new pixel value.
	 */
	private int invert(final int val) {
		final int b = val & 0xff;
		final int g = (val & 0xff00) >> 8;
		final int r = (val & 0xff0000) >> 16;
		final int a = (val & 0xff000000) >>> 24;
		final int l0 = 255 - b;
		final int l1 = 255 - g;
		final int l2 = 255 - r;
		final int l3 = 255 - a;
		return (l3 << 24) + (l2 << 16) + (l1 << 8) + l0;
	}
}

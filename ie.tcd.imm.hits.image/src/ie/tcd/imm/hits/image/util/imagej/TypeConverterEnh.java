/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package ie.tcd.imm.hits.image.util.imagej;

import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;
import ij.process.TypeConverter;

/**
 * A {@link TypeConverter} with a different strategy (you can adjust the min and
 * max values).
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
public class TypeConverterEnh {

	private final ImageProcessor ip;

	/**
	 * @param ip
	 *            An {@link ImageProcessor}.
	 */
	public TypeConverterEnh(final ImageProcessor ip) {
		super();
		this.ip = ip;
	}

	/**
	 * Converts a {@link ByteProcessor} to a {@link ByteProcessor}.
	 * 
	 * @param min
	 *            Value of the lower cutoff point.
	 * @param max
	 *            Value of the higher cutoff point.
	 * @return A {@link ByteProcessor} with the properly scaled values.
	 */
	public ByteProcessor convertByteToByte(final double min, final double max) {
		final int size = ip.getWidth() * ip.getHeight();
		final byte[] pixels = (byte[]) ip.getPixels();
		final byte[] pixels8 = new byte[size];
		int value;
		// final int min = (int) ip.getMin(), max = (int) ip.getMax();
		final int mini = (int) min;
		final double scale = 256.0 / (max - min + 1);
		for (int i = 0; i < size; i++) {
			value = (pixels[i] & 0xff) - mini;
			if (value < 0) {
				value = 0;
			}
			value = (int) (value * scale + 0.5);
			if (value > 255) {
				value = 255;
			}
			pixels8[i] = (byte) value;
		}
		return new ByteProcessor(ip.getWidth(), ip.getHeight(), pixels8, ip
				.getCurrentColorModel());
	}

	/**
	 * Converts a {@link ShortProcessor} to a {@link ByteProcessor}.
	 * 
	 * @param min
	 *            Value of the lower cutoff point.
	 * @param max
	 *            Value of the higher cutoff point.
	 * @return A {@link ByteProcessor} with the properly scaled values.
	 */
	public ByteProcessor convertShortToByte(final double min, final double max) {
		final int size = ip.getWidth() * ip.getHeight();
		final short[] pixels16 = (short[]) ip.getPixels();
		final byte[] pixels8 = new byte[size];
		int value;
		// final int min = (int) ip.getMin(), max = (int) ip.getMax();
		final int mini = (int) min;
		final double scale = 256.0 / (max - min + 1);
		for (int i = 0; i < size; i++) {
			value = (pixels16[i] & 0xffff) - mini;
			if (value < 0) {
				value = 0;
			}
			value = (int) (value * scale + 0.5);
			if (value > 255) {
				value = 255;
			}
			pixels8[i] = (byte) value;
		}
		return new ByteProcessor(ip.getWidth(), ip.getHeight(), pixels8, ip
				.getCurrentColorModel());
	}
}

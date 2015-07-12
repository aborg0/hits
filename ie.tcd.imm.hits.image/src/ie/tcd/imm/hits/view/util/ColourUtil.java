/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package ie.tcd.imm.hits.view.util;

import ij.process.LUT;

/**
 * Defines some constant {@link LUT}s.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
public class ColourUtil {
	private static int RANGE = 256;
	private static final byte[] ZERO = new byte[RANGE];
	/** From black to white */
	public static LUT WHITE = new LUT(makeLinear(), makeLinear(), makeLinear());
	/** From black to red. */
	public static LUT RED = new LUT(makeLinear(), ZERO, ZERO);
	/** From black to green. */
	public static LUT GREEN = new LUT(ZERO, makeLinear(), ZERO);
	/** From black to blue. */
	public static LUT BLUE = new LUT(ZERO, ZERO, makeLinear());
	/** From non-red to white. */
	public static LUT INV_RED = new LUT(invert(makeLinear()), invert(ZERO),
			invert(ZERO));
	/** From red to black. */
	public static LUT INV_ONLY_RED = new LUT(invert(makeLinear()), ZERO, ZERO);
	/** From white to black. */
	public static LUT INV_WHITE = new LUT(invert(makeLinear()),
			invert(makeLinear()), invert(makeLinear()));
	/** From non-red to white. */
	public static LUT INV_RED_KEEP_GREEN = new LUT(invert(makeLinear()), ZERO,
			invert(ZERO));
	/** From non-red to white. */
	public static LUT INV_RED_KEEP_BLUE = new LUT(invert(makeLinear()),
			invert(ZERO), ZERO);
	/** From non-green to white. */
	public static LUT INV_GREEN = new LUT(invert(ZERO), invert(makeLinear()),
			invert(ZERO));
	/** From green to black. */
	public static LUT INV_ONLY_GREEN = new LUT(ZERO, invert(makeLinear()), ZERO);
	/** From non-green to white. */
	public static LUT INV_GREEN_KEEP_RED = new LUT(ZERO, invert(makeLinear()),
			invert(ZERO));
	/** From non-green to white. */
	public static LUT INV_GREEN_KEEP_BLUE = new LUT(invert(ZERO),
			invert(makeLinear()), ZERO);
	/** From non-blue to white. */
	public static LUT INV_BLUE = new LUT(invert(ZERO), invert(ZERO),
			invert(makeLinear()));
	/** From blue to black. */
	public static LUT INV_ONLY_BLUE = new LUT(ZERO, ZERO, invert(makeLinear()));
	/** From non-blue to white. */
	public static LUT INV_BLUE_KEEP_RED = new LUT(ZERO, invert(ZERO),
			invert(makeLinear()));
	/** From non-blue to white. */
	public static LUT INV_BLUE_KEEP_GREEN = new LUT(invert(ZERO), ZERO,
			invert(makeLinear()));

	/**
	 * The predefined {@link LUT}s in {@link #RED}, {@link #BLUE},
	 * {@link #GREEN}, {@link #INV_RED}, {@link #INV_BLUE}, {@link #INV_GREEN}
	 * order.
	 */
	public static LUT[] LUTS = new LUT[] { RED, BLUE, GREEN, INV_RED, INV_BLUE,
			INV_GREEN };

	/**
	 * @return A new byte array with linear changes.
	 */
	private static byte[] makeLinear() {
		final byte[] ret = new byte[RANGE];
		for (int i = RANGE; i-- > 0;) {
			ret[i] = (byte) (i & 0xff);
		}
		return ret;
	}

	// private static byte[] flip(final byte[] vals) {
	// final byte[] ret = new byte[vals.length];
	// for (int i = vals.length; i-- > 0;) {
	// ret[vals.length - i] = vals[i];
	// }
	// return ret;
	// }

	private static byte[] invert(final byte[] vals) {
		final byte[] ret = new byte[vals.length];
		for (int i = vals.length; i-- > 0;) {
			ret[i] = (byte) (RANGE - 1 - vals[i] & 0xff);
		}
		return ret;
	}
}

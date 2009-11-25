/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package ie.tcd.imm.hits.view.util;

//import ij.process.LUT;

/**
 * TODO Javadoc!
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
public class ColourUtil {
	private static int RANGE = 256;
	private static final byte[] ZERO = new byte[RANGE];

//	public static LUT RED = new LUT(makeLinear(), ZERO, ZERO);
//	public static LUT GREEN = new LUT(ZERO, makeLinear(), ZERO);
//	public static LUT BLUE = new LUT(ZERO, ZERO, makeLinear());
//	public static LUT INV_RED = new LUT(invert(makeLinear()), invert(ZERO),
//			invert(ZERO));
//	public static LUT INV_GREEN = new LUT(invert(ZERO), invert(makeLinear()),
//			invert(ZERO));
//	public static LUT INV_BLUE = new LUT(invert(ZERO), invert(ZERO),
//			invert(makeLinear()));
//
//	public static LUT[] LUTS = new LUT[] { RED, BLUE, GREEN, INV_RED, INV_BLUE,
//			INV_GREEN };

	/**
	 * @return
	 */
	private static byte[] makeLinear() {
		final byte[] ret = new byte[RANGE];
		for (int i = RANGE; i-- > 0;) {
			ret[i] = (byte) (i & 0xff);
		}
		return ret;
	}

	private static byte[] flip(final byte[] vals) {
		final byte[] ret = new byte[vals.length];
		for (int i = vals.length; i-- > 0;) {
			ret[vals.length - i] = vals[i];
		}
		return ret;
	}

	private static byte[] invert(final byte[] vals) {
		final byte[] ret = new byte[vals.length];
		for (int i = vals.length; i-- > 0;) {
			ret[i] = (byte) (RANGE - 1 - vals[i] & 0xff);
		}
		return ret;
	}
}

/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package ie.tcd.imm.hits.util;

import java.io.Serializable;

import javax.annotation.CheckReturnValue;

import edu.umd.cs.findbugs.annotations.DefaultAnnotation;

/**
 * A ({@link Serializable}) triple of {@link Serializable} objects.
 * 
 * @author <a href="bakosg@tcd.ie">Gabor Bakos</a>
 * @param <Type1>
 *            The type of the first object.
 * @param <Type2>
 *            The type of the second object.
 * @param <Type3>
 *            The type of the third object.
 */
@DefaultAnnotation( { CheckReturnValue.class })
public class SerializableTriple<Type1 extends Serializable, Type2 extends Serializable, Type3 extends Serializable>
		extends Triple<Type1, Type2, Type3> implements Serializable {
	private static final long serialVersionUID = 346626861413922725L;

	/**
	 * Constructs a {@link Triple} of the supplied objects.
	 * 
	 * @param o1
	 *            The first object.
	 * @param o2
	 *            The second object.
	 * @param o3
	 *            The third object.
	 */
	public SerializableTriple(final Type1 o1, final Type2 o2, final Type3 o3) {
		super(o1, o2, o3);
	}

	/**
	 * Factory method for creating {@link Triple}s.
	 * 
	 * @param <Type1>
	 *            Type of the first object.
	 * @param <Type2>
	 *            Type of the second object.
	 * @param <Type3>
	 *            Type of the third object.
	 * @param o1
	 *            The first object.
	 * @param o2
	 *            The second object.
	 * @param o3
	 *            The third object.
	 * @return The generated {@link Triple}.
	 */
	public static <Type1 extends Serializable, Type2 extends Serializable, Type3 extends Serializable> SerializableTriple<Type1, Type2, Type3> apply(
			final Type1 o1, final Type2 o2, final Type3 o3) {
		return new SerializableTriple<Type1, Type2, Type3>(o1, o2, o3);
	}

	/** {@inheritDoc} */
	@Override
	public Triple<Type3, Type1, Type2> shiftRight() {
		return SerializableTriple.apply(getO3(), getO1(), getO2());
	}

	/** {@inheritDoc} */
	@Override
	public Triple<Type2, Type3, Type1> shiftLeft() {
		return SerializableTriple.apply(getO2(), getO3(), getO1());
	}

	/** {@inheritDoc} */
	@Override
	public Triple<Type3, Type2, Type1> reverse() {
		return SerializableTriple.apply(getO3(), getO2(), getO1());
	}
}

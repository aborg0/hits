/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package com.mind_era.knime.common.util;

import java.io.Serializable;

import javax.annotation.CheckReturnValue;

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
@CheckReturnValue
public class SerializableTriple<Type1 extends Serializable, Type2 extends Serializable, Type3 extends Serializable>
		implements Serializable, ITriple<Type1, Type2, Type3> {
	private static final long serialVersionUID = 346626861413922725L;
	private final Type1 o1;
	private final Type2 o2;
	private final Type3 o3;

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
		super();
		this.o1 = o1;
		this.o2 = o2;
		this.o3 = o3;
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
	public SerializableTriple<Type3, Type1, Type2> shiftRight() {
		return SerializableTriple.apply(getO3(), getO1(), getO2());
	}

	/** {@inheritDoc} */
	@Override
	public SerializableTriple<Type2, Type3, Type1> shiftLeft() {
		return SerializableTriple.apply(getO2(), getO3(), getO1());
	}

	/** {@inheritDoc} */
	@Override
	public SerializableTriple<Type3, Type2, Type1> reverse() {
		return SerializableTriple.apply(getO3(), getO2(), getO1());
	}

	/** {@inheritDoc} */
	public final Type1 getO1() {
		return o1;
	}

	/** {@inheritDoc} */
	public final Type2 getO2() {
		return o2;
	}

	/** {@inheritDoc} */
	public final Type3 getO3() {
		return o3;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (o1 == null ? 0 : o1.hashCode());
		result = prime * result + (o2 == null ? 0 : o2.hashCode());
		result = prime * result + (o3 == null ? 0 : o3.hashCode());
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final SerializableTriple<?, ?, ?> other = (SerializableTriple<?, ?, ?>) obj;
		if (o1 == null) {
			if (other.o1 != null) {
				return false;
			}
		} else if (!o1.equals(other.o1)) {
			return false;
		}
		if (o2 == null) {
			if (other.o2 != null) {
				return false;
			}
		} else if (!o2.equals(other.o2)) {
			return false;
		}
		if (o3 == null) {
			if (other.o3 != null) {
				return false;
			}
		} else if (!o3.equals(other.o3)) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return "(" + o1 + ", " + o2 + ", " + o3 + ")";
	}
}

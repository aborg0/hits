/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package ie.tcd.imm.hits.util;

import javax.annotation.CheckReturnValue;

import edu.umd.cs.findbugs.annotations.DefaultAnnotation;

/**
 * @author <a href="bakosg@tcd.ie">Gabor Bakos</a>
 * @param <Type1>
 * @param <Type2>
 * @param <Type3>
 */
@DefaultAnnotation( { CheckReturnValue.class })
public class Triple<Type1, Type2, Type3> {

	private final Type1 o1;
	private final Type2 o2;
	private final Type3 o3;

	/**
	 * 
	 * @param o1
	 * @param o2
	 * @param o3
	 */
	public Triple(final Type1 o1, final Type2 o2, final Type3 o3) {
		super();
		this.o1 = o1;
		this.o2 = o2;
		this.o3 = o3;
	}

	/**
	 * 
	 * @param <Type1>
	 * @param <Type2>
	 * @param <Type3>
	 * @param o1
	 * @param o2
	 * @param o3
	 * @return
	 */
	public static <Type1, Type2, Type3> Triple<Type1, Type2, Type3> apply(
			final Type1 o1, final Type2 o2, final Type3 o3) {
		return new Triple<Type1, Type2, Type3>(o1, o2, o3);
	}

	/**
	 * @return the o1
	 */
	public final Type1 getO1() {
		return o1;
	}

	/**
	 * @return the o2
	 */
	public final Type2 getO2() {
		return o2;
	}

	/**
	 * @return the o3
	 */
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
		final Triple<?, ?, ?> other = (Triple<?, ?, ?>) obj;
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
		return "(" /* + o1 */+ ", " + o2 + ", " + o3 + ")";
	}
}

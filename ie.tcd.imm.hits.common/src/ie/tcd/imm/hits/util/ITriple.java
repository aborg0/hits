/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package ie.tcd.imm.hits.util;

import javax.annotation.CheckReturnValue;

/**
 * A triple of objects.
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
public interface ITriple<Type1, Type2, Type3> {
	/**
	 * @return the o1
	 */
	public Type1 getO1();

	/**
	 * @return the o2
	 */
	public Type2 getO2();

	/**
	 * @return the o3
	 */
	public Type3 getO3();

	/**
	 * @return The object shifted to right.
	 */
	public ITriple<Type3, Type1, Type2> shiftRight();

	/**
	 * @return The object shifted to left.
	 */
	public ITriple<Type2, Type3, Type1> shiftLeft();

	/**
	 * @return The object are in reversed order.
	 */
	public ITriple<Type3, Type2, Type1> reverse();
}

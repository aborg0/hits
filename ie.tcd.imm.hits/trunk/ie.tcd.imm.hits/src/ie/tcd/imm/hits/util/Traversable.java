/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package ie.tcd.imm.hits.util;

import java.util.concurrent.Callable;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import edu.umd.cs.findbugs.annotations.DefaultAnnotation;

/**
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
@DefaultAnnotation( { Nonnull.class, CheckReturnValue.class })
public interface Traversable<Type, State> {

	/**
	 * @param callable
	 */
	void traverse(Callable<?> callable);

	/**
	 * @return
	 */
	State getState();

	/**
	 * @return
	 */
	Type getType();
}

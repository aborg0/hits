/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package ie.tcd.imm.util.template;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import edu.umd.cs.findbugs.annotations.DefaultAnnotation;

/**
 * A common implementation of the {@link Token} interface.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
@DefaultAnnotation( { Nonnull.class, CheckReturnValue.class })
public abstract class AbstractToken implements Token {
	private static final long serialVersionUID = -4749109768887755365L;

	private final String content;
	private final int startPosition;
	private final int endPosition;

	/**
	 * @param startPosition
	 *            The first position of the {@link Token}.
	 * @param endPosition
	 *            The position after the last character of the {@link Token}.
	 * @param content
	 *            The content of the token.
	 * 
	 */
	public AbstractToken(final int startPosition, final int endPosition,
			final String content) {
		super();
		this.startPosition = startPosition;
		this.endPosition = endPosition;
		this.content = content;
	}

	@Override
	public int getEndPosition() {
		return endPosition;
	}

	@Override
	public int getStartPosition() {
		return startPosition;
	}

	@Override
	public String toString() {
		return content;
	}
}

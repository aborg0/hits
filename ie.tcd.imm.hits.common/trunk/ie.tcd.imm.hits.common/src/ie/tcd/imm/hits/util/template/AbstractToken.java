/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package ie.tcd.imm.hits.util.template;

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

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (content == null ? 0 : content.hashCode());
		result = prime * result + endPosition;
		result = prime * result + startPosition;
		return result;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof AbstractToken)) {
			return false;
		}
		final AbstractToken other = (AbstractToken) obj;
		if (content == null) {
			if (other.content != null) {
				return false;
			}
		} else if (!content.equals(other.content)) {
			return false;
		}
		if (endPosition != other.endPosition) {
			return false;
		}
		if (startPosition != other.startPosition) {
			return false;
		}
		return true;
	}
}

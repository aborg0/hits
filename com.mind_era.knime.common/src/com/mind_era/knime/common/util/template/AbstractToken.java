/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package com.mind_era.knime.common.util.template;

import java.util.WeakHashMap;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

/**
 * A common implementation of the {@link Token} interface.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
@Nonnull
@CheckReturnValue
public abstract class AbstractToken implements Token {
	private static final long serialVersionUID = -4749109768887755365L;

	/**
	 * A {@link Token} without content.
	 */
	public static final class EmptyToken extends AbstractToken {
		private static final long serialVersionUID = -7916615759761665791L;

		/**
		 * @param position
		 *            The position where the gap is.
		 */
		private EmptyToken(final int position) {
			super(position, position, "");
		}

		@Override
		public boolean equals(final Object other) {
			return other != null && other.getClass().equals(EmptyToken.class)
					&& super.equals(other);
		}

		private static WeakHashMap<Integer, EmptyToken> cache = new WeakHashMap<Integer, EmptyToken>();

		/**
		 * Factory method to find a proper {@link EmptyToken}.
		 * 
		 * @param position
		 *            The position of the gap.
		 * @return A cached, or a new instance of {@link EmptyToken}.
		 */
		public synchronized static EmptyToken get(final int position) {
			final Integer pos = Integer.valueOf(position);
			final EmptyToken emptyToken = new EmptyToken(position);
			if (!cache.containsKey(pos)) {
				cache.put(pos, emptyToken);
			}
			return cache.get(pos);
		}
	}

	private final String text;
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
		this.text = content;
	}

	@Override
	public int getEndPosition() {
		return endPosition;
	}

	@Override
	public int getStartPosition() {
		return startPosition;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ie.tcd.imm.hits.util.template.Token#getText()
	 */
	@Override
	public String getText() {
		return text;
	}

	@Override
	public String toString() {
		return "#" + text + "#(" + startPosition + ")";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (text == null ? 0 : text.hashCode());
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
		if (text == null) {
			if (other.text != null) {
				return false;
			}
		} else if (!text.equals(other.text)) {
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

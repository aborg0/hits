/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package ie.tcd.imm.hits.util.template;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

/**
 * A simple implementation of the {@link Token} interface.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
@Nonnull
@CheckReturnValue
public final class SimpleToken extends AbstractToken {
	private static final long serialVersionUID = -4055509769254551505L;

	/**
	 * @param startPosition
	 *            The first position of the {@link Token}.
	 * @param endPosition
	 *            The position after the last character of the {@link Token}.
	 * @param content
	 *            The content of the token.
	 */
	public SimpleToken(final int startPosition, final int endPosition,
			final String content) {
		super(startPosition, endPosition, content);
	}

	@Override
	public boolean equals(final Object obj) {
		return this == obj || obj != null
				&& obj.getClass().equals(SimpleToken.class)
				&& super.equals(obj);
	}
}

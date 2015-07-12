/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package ie.tcd.imm.hits.util.template;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

/**
 * This interface is a marker for compound {@link Token}s.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 * @param <TokenType>
 *            The type of the contained tokens.
 */
@Nonnull
@CheckReturnValue
public interface CompoundToken<TokenType extends Token> extends
		Iterable<TokenType>, Token {
	// Just a marker no other methods.
}

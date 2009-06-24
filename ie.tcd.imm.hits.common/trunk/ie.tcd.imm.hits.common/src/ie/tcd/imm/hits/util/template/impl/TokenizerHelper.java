/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package ie.tcd.imm.hits.util.template.impl;

import ie.tcd.imm.hits.util.template.CompoundToken;
import ie.tcd.imm.hits.util.template.Token;

import java.util.ArrayList;
import java.util.List;

/**
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
public class TokenizerHelper {

	/**
	 * s
	 * 
	 */
	public TokenizerHelper() {
		super();
	}

	/**
	 * Selects a proper elements from {@code tokens} with type {@code
	 * tokenClass}.
	 * 
	 * @param <T>
	 *            Type of the filtered {@link Token}s.
	 * @param tokens
	 *            A list of tokens.
	 * @param tokenClass
	 *            A class of a {@link Token}.
	 * @return A {@link List} of {@link Token}s with type of {@code T}.
	 */
	protected <T extends Token> List<T> filter(
			final Iterable<? extends Token> tokens, final Class<T> tokenClass) {
		final List<T> ret = new ArrayList<T>();
		for (final Token token : tokens) {
			if (tokenClass.isInstance(token)) {
				final boolean b = ret.add(tokenClass.cast(token));
				assert b;
			} else if (token instanceof CompoundToken) {
				final CompoundToken<? extends Token> compound = (CompoundToken<?>) token;
				ret.addAll(filter(compound, tokenClass));
			}
		}
		return ret;
	}

	/**
	 * Selects a proper elements from {@code tokens} with type {@code
	 * tokenClass}. (Depending on {@code instance} value it will be included, or
	 * excluded with that type of tokens.)
	 * 
	 * @param tokens
	 *            A list of tokens.
	 * @param tokenClass
	 *            A class of a {@link Token}.
	 * @param instance
	 *            If {@code true} the instance of {@code tokenClass} included,
	 *            else those will be excluded.
	 * @return The list of filtered elements. Does not contain {@code null}
	 *         values.
	 */
	protected List<Token> filter(final Iterable<? extends Token> tokens,
			final Class<? extends Token> tokenClass, final boolean instance) {
		final List<Token> ret = new ArrayList<Token>();
		for (final Token token : tokens) {
			if (token != null && tokenClass.isInstance(token) ^ !instance) {
				final boolean b = ret.add(token);
				assert b;
			} else if (token instanceof CompoundToken) {
				final CompoundToken<? extends Token> compound = (CompoundToken<?>) token;
				ret.addAll(filter(compound, tokenClass, instance));
			}
		}
		return ret;
	}

}
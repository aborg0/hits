/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package com.mind_era.knime.common.util.template.impl;

import java.util.List;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import com.mind_era.knime.common.util.template.CompoundToken;
import com.mind_era.knime.common.util.template.SimpleToken;
import com.mind_era.knime.common.util.template.Token;
import com.mind_era.knime.common.util.template.TokenizeException;
import com.mind_era.knime.common.util.template.Tokenizer;

/**
 * Selects only certain classes of {@link Token}s from the results. 
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
@Nonnull
@CheckReturnValue
public class FilterTokenizer extends TokenizerHelper implements Tokenizer {

	private final Tokenizer tokenizer;
	private final Class<? extends Token>[] acceptedTokenClasses;
	private final boolean goIntoCompounds;

	/**
	 * @param tokenizer
	 *            The {@link Tokenizer} to wrap.
	 */
	public FilterTokenizer(final Tokenizer tokenizer) {
		this(tokenizer, true, SimpleToken.class);
	}

	/**
	 * @param tokenizer
	 *            The {@link Tokenizer} to wrap.
	 * @param goIntoCompounds
	 *            If {@code true} it will go inside {@link CompoundToken}s.
	 * @param acceptedTokenClasses
	 *            Instances of these classes are accepted
	 */
	@SafeVarargs
	public FilterTokenizer(final Tokenizer tokenizer,
			final boolean goIntoCompounds,
			final Class<? extends Token>... acceptedTokenClasses) {
		super();
		this.tokenizer = tokenizer;
		this.goIntoCompounds = goIntoCompounds;
		this.acceptedTokenClasses = acceptedTokenClasses.clone();
	}

	@Override
	public List<Token> parse(final String text) throws TokenizeException {
		return filter(tokenizer.parse(text), goIntoCompounds,
				acceptedTokenClasses);
	}
}

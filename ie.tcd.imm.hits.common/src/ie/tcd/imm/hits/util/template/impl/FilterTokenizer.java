/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package ie.tcd.imm.hits.util.template.impl;

import ie.tcd.imm.hits.util.template.CompoundToken;
import ie.tcd.imm.hits.util.template.SimpleToken;
import ie.tcd.imm.hits.util.template.Token;
import ie.tcd.imm.hits.util.template.TokenizeException;
import ie.tcd.imm.hits.util.template.Tokenizer;

import java.util.List;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import edu.umd.cs.findbugs.annotations.DefaultAnnotation;

/**
 * Selects only th
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
@DefaultAnnotation( { Nonnull.class, CheckReturnValue.class })
public class FilterTokenizer extends TokenizerHelper implements Tokenizer {

	private final Tokenizer tokenizer;
	private final Class<? extends Token>[] acceptedTokenClasses;
	private final boolean goIntoCompounds;

	/**
	 * @param tokenizer
	 *            The {@link Tokenizer} to wrap.
	 */
	@SuppressWarnings("unchecked")
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

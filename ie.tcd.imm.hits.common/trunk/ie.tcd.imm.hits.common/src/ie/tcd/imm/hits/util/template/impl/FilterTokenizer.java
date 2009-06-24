/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package ie.tcd.imm.hits.util.template.impl;

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

	/**
	 * @param tokenizer
	 *            The {@link Tokenizer} to wrap.
	 */
	public FilterTokenizer(final Tokenizer tokenizer) {
		super();
		this.tokenizer = tokenizer;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ie.tcd.imm.hits.util.template.Tokenizer#parse(java.lang.String)
	 */
	@Override
	public List<Token> parse(final String text) throws TokenizeException {
		return filter(tokenizer.parse(text), SimpleToken.class, true);
	}
}

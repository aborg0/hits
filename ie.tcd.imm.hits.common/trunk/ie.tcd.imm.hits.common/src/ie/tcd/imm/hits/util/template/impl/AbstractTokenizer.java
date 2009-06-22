/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package ie.tcd.imm.hits.util.template.impl;

import ie.tcd.imm.hits.util.template.AbstractToken;
import ie.tcd.imm.hits.util.template.SimpleToken;
import ie.tcd.imm.hits.util.template.Token;
import ie.tcd.imm.hits.util.template.TokenizeException;
import ie.tcd.imm.hits.util.template.Tokenizer;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.io.Serializable;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import edu.umd.cs.findbugs.annotations.DefaultAnnotation;

/**
 * A simple, {@link Pattern#split(CharSequence) split}-based {@link Tokenizer}.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
@DefaultAnnotation( { Nonnull.class, CheckReturnValue.class })
public class AbstractTokenizer implements Tokenizer, Serializable {
	private static final long serialVersionUID = 7828487000830931865L;

	/**
	 * A simple class for the {@link Token}s of splits.
	 */
	protected class SplitToken extends AbstractToken {
		private static final long serialVersionUID = -8082977893154016447L;

		/**
		 * @param startPosition
		 *            The first position of the {@link Token}.
		 * @param endPosition
		 *            The position after the last character of the {@link Token}
		 *            .
		 * @param content
		 *            The content of the token.
		 */
		public SplitToken(final int startPosition, final int endPosition,
				final String content) {
			super(startPosition, endPosition, content);
		}

		@Override
		public boolean equals(final Object obj) {
			return this == obj || obj != null
					&& obj.getClass().equals(SplitToken.class)
					&& super.equals(obj);
		}
	}

	private final String splitExpression;
	private final int offset;

	/**
	 * @param offset
	 *            The start offset of the {@link Tokenizer}. ({@code 0}-based.)
	 * @param splitExpression
	 *            The regular expression to split the text (from {@code offset}
	 *            ).
	 * 
	 */
	public AbstractTokenizer(final int offset, final String splitExpression) {
		super();
		this.offset = offset;
		this.splitExpression = splitExpression;
	}

	/**
	 * @param text
	 *            The text to analyse.
	 * @return A mixture of the {@link SimpleToken}s and {@link SplitToken}s.
	 * @throws TokenizeException
	 */
	protected List<Token> splitter(final String text) throws TokenizeException {
		final Pattern pattern = Pattern.compile(splitExpression);
		final Matcher matcher = pattern.matcher(text.substring(offset));
		final String[] parts = text.substring(offset).split(splitExpression);
		final List<Token> ret = new ArrayList<Token>();
		int beginOffset = offset;
		for (final String part : parts) {
			if (!part.isEmpty()) {
				ret.add(new SimpleToken(beginOffset, part.length(), part));
				beginOffset += part.length();
			}
			final boolean found = matcher.find(beginOffset - offset);
			if (!found)// Only empty strings
			{
				break;
			}
			ret.add(new SplitToken(matcher.start() - offset, matcher.end()
					- offset, matcher.group()));
			beginOffset += matcher.group().length();
		}
		return ret;
	}

	@Override
	public List<Token> parse(final String text) throws TokenizeException {
		return filter(splitter(text), SimpleToken.class, true);
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
	protected <T extends Token> List<T> filter(final Iterable<Token> tokens,
			final Class<T> tokenClass) {
		final List<T> ret = new ArrayList<T>();
		for (final Token token : tokens) {
			if (tokenClass.isInstance(token)) {
				final boolean b = ret.add(tokenClass.cast(token));
				assert b;
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
	protected List<Token> filter(final Iterable<Token> tokens,
			final Class<? extends Token> tokenClass, final boolean instance) {
		final List<Token> ret = new ArrayList<Token>();
		for (final Token token : tokens) {
			if (token != null && tokenClass.isInstance(token) ^ !instance) {
				final boolean b = ret.add(token);
				assert b;
			}
		}
		return ret;
	}
}

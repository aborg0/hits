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
public abstract class AbstractTokenizer implements Tokenizer, Serializable {
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
			final boolean found = matcher.find(beginOffset - offset);
			assert found;
			beginOffset += matcher.group().length();
			ret.add(new SplitToken(beginOffset + matcher.start() - offset,
					beginOffset + matcher.end() - offset, matcher.group()));
			if (!part.isEmpty()) {
				ret.add(new SimpleToken(beginOffset, part.length(), part));
				beginOffset += part.length();
			}
		}
		return ret;
	}
}

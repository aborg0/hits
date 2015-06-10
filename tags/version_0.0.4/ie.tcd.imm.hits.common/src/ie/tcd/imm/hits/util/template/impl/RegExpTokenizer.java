/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package ie.tcd.imm.hits.util.template.impl;

import ie.tcd.imm.hits.util.template.AbstractToken;
import ie.tcd.imm.hits.util.template.SimpleToken;
import ie.tcd.imm.hits.util.template.Token;
import ie.tcd.imm.hits.util.template.TokenizeException;
import ie.tcd.imm.hits.util.template.Tokenizer;
import ie.tcd.imm.hits.util.template.AbstractToken.EmptyToken;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.RegEx;

import edu.umd.cs.findbugs.annotations.DefaultAnnotation;

/**
 * A simple, {@link Pattern#split(CharSequence) split}-based {@link Tokenizer}.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
@DefaultAnnotation( { Nonnull.class, CheckReturnValue.class })
public class RegExpTokenizer extends TokenizerHelper implements Tokenizer,
		Serializable {
	private static final long serialVersionUID = 7828487000830931865L;

	/**
	 * A simple class for the {@link Token}s of splits.
	 */
	public static final class SplitToken extends AbstractToken {
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

	private final int offset;
	private final Pattern pattern;

	/**
	 * @param offset
	 *            The start offset of the {@link Tokenizer}. ({@code 0}-based.)
	 * @param splitExpression
	 *            The regular expression to split the text (from {@code offset}
	 *            ).
	 * 
	 */
	public RegExpTokenizer(final int offset, @RegEx final String splitExpression) {
		super();
		this.offset = offset;
		pattern = Pattern.compile(splitExpression);
	}

	/**
	 * @param offset
	 *            The start offset of the {@link Tokenizer}. ({@code 0}-based.)
	 * @param splitExpression
	 *            The regular expression to split the text (from {@code offset}
	 *            ).
	 * 
	 */
	public RegExpTokenizer(final int offset, final Pattern splitExpression) {
		super();
		this.offset = offset;
		pattern = splitExpression;
	}

	/**
	 * @param text
	 *            The text to analyse.
	 * @return A mixture of the {@link SimpleToken}s and {@link SplitToken}s.
	 */
	protected List<Token> splitter(final String text) {
		final Matcher matcher = pattern.matcher(text.substring(offset));
		final String[] parts = pattern.split(text.substring(offset), -1);
		final List<Token> ret = new ArrayList<Token>();
		int beginOffset = offset;
		for (final String part : parts) {
			if (!part.isEmpty()) {
				ret.add(new SimpleToken(beginOffset, beginOffset
						+ part.length(), part));
				beginOffset += part.length();
			} else {
				ret.add(EmptyToken.get(beginOffset));
			}
			final boolean found = matcher.find(beginOffset - offset);
			if (!found)// Only empty strings
			{
				break;
			}
			ret.add(new SplitToken(matcher.start() + offset, matcher.end()
					+ offset, matcher.group()));
			beginOffset += matcher.group().length();
		}
		return ret;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<Token> parse(final String text) throws TokenizeException {
		return filter(splitter(text), SimpleToken.class, true);
	}
}

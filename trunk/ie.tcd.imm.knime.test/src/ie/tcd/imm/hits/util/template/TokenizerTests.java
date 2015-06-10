/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package ie.tcd.imm.hits.util.template;

import ie.tcd.imm.hits.util.template.AbstractToken.EmptyToken;
import ie.tcd.imm.hits.util.template.impl.GroupingTokenizer.GroupToken;
import ie.tcd.imm.hits.util.template.impl.RegExpTokenizer.SplitToken;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import edu.umd.cs.findbugs.annotations.DefaultAnnotation;

/**
 * A base class for the tests of {@link Tokenizer} implementations.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
@SuppressWarnings("restriction")
@DefaultAnnotation( { Nonnull.class, CheckReturnValue.class })
public abstract class TokenizerTests {

	/**
	 * @param first
	 * @param last
	 * @param start
	 * @return A {@link GroupToken}.
	 */
	protected static GroupToken<SplitToken, EmptyToken> group(
			final String first, final String last, final int start) {
		return new GroupToken<SplitToken, EmptyToken>(new SplitToken(start,
				start + first.length(), first), EmptyToken.get(start
				+ first.length()), new SplitToken(start + first.length(), start
				+ (first + last).length(), last));
	}

	/**
	 * @param first
	 * @param last
	 * @param start
	 * @param content
	 * @return A {@link GroupToken}.
	 */
	protected static GroupToken<SplitToken, SimpleToken> group(
			final String first, final String last, final int start,
			final String content) {
		return new GroupToken<SplitToken, SimpleToken>(new SplitToken(start,
				start + first.length(), first), new SimpleToken(start
				+ first.length(), start + (first + content).length(), content),
				new SplitToken(start + (first + content).length(), start
						+ (first + content + last).length(), last));
	}

	/**
	 * 
	 * @param content
	 * @param start
	 * @return A {@link SimpleToken}.
	 */
	protected static SimpleToken simple(final String content, final int start) {
		return new SimpleToken(start, start + content.length(), content);
	}

	/**
	 * @param token
	 * @param amount
	 * @return Shifted token.
	 */
	protected static Token shift(final Token token, final int amount) {
		if (token instanceof EmptyToken) {
			final EmptyToken t = (EmptyToken) token;
			return EmptyToken.get(t.getStartPosition() + amount);
		}
		if (token instanceof SimpleToken) {
			final SimpleToken s = (SimpleToken) token;
			return new SimpleToken(s.getStartPosition() + amount, s
					.getEndPosition()
					+ amount, s.getText());
		}
		if (token instanceof SplitToken) {
			final SplitToken s = (SplitToken) token;
			return new SplitToken(s.getStartPosition() + amount, s
					.getEndPosition()
					+ amount, s.getText());
		}
		if (token instanceof GroupToken<?, ?>) {
			final GroupToken<? extends Token, ? extends Token> g = (GroupToken<?, ?>) token;
			return new GroupToken<Token, Token>(
					shift(g.getGroupStart(), amount), shift(g.getContent(),
							amount), shift(g.getGroupEnd(), amount));
		}
		throw new UnsupportedOperationException("Not yet supported: "
				+ token.getClass());
	}

	/**
	 * @param tokens
	 * @param amount
	 * @return Shifted tokens.
	 */
	protected static List<Token> shift(final Iterable<? extends Token> tokens,
			final int amount) {
		final List<Token> ret = new ArrayList<Token>();
		for (final Token token : tokens) {
			ret.add(shift(token, amount));
		}
		return ret;
	}

	/**
	 * @return Creates a {@link Tokenizer} for simple tests.
	 */
	protected abstract Tokenizer create();

	/**
	 * 
	 */
	public TokenizerTests() {
		// TODO Auto-generated constructor stub
	}
}

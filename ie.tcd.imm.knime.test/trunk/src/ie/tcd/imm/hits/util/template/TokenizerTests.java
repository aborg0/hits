/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package ie.tcd.imm.hits.util.template;

import ie.tcd.imm.hits.util.template.AbstractToken.EmptyToken;
import ie.tcd.imm.hits.util.template.impl.AbstractTokenizer.SplitToken;
import ie.tcd.imm.hits.util.template.impl.GroupingTokenizer.GroupToken;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import edu.umd.cs.findbugs.annotations.DefaultAnnotation;

/**
 * A base class for the tests of {@link Tokenizer} implementations.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
@DefaultAnnotation( { Nonnull.class, CheckReturnValue.class })
public abstract class TokenizerTests {

	protected static GroupToken<SplitToken, EmptyToken> group(final String first, final String last, final int start) {
		return new GroupToken<SplitToken, EmptyToken>(new SplitToken(start,
				start + first.length(), first), EmptyToken.get(start
				+ first.length()), new SplitToken(start + first.length(), start
				+ (first + last).length(), last));
	}

	protected static GroupToken<SplitToken, SimpleToken> group(final String first, final String last, final int start,
			final String content) {
				return new GroupToken<SplitToken, SimpleToken>(new SplitToken(start,
						start + first.length(), first), new SimpleToken(start
						+ first.length(), start + (first + content).length(), content),
						new SplitToken(start + (first + content).length(), start
								+ (first + content + last).length(), last));
			}

	protected static SimpleToken simple(final String content, final int start) {
		return new SimpleToken(start, start + content.length(), content);
	}

	protected abstract Tokenizer create();

	/**
	 * 
	 */
	public TokenizerTests() {
		// TODO Auto-generated constructor stub
	}
}

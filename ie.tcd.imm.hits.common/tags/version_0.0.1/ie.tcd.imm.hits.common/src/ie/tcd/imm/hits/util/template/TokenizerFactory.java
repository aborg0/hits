/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package ie.tcd.imm.hits.util.template;

import ie.tcd.imm.hits.util.Pair;
import ie.tcd.imm.hits.util.template.impl.FilterTokenizer;
import ie.tcd.imm.hits.util.template.impl.GroupingTokenizer;
import ie.tcd.imm.hits.util.template.impl.RegExpTokenizer;

import java.util.List;
import java.util.regex.Pattern;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.RegEx;

import edu.umd.cs.findbugs.annotations.DefaultAnnotation;

/**
 * This class provides some methods to create {@link Tokenizer} implementations
 * to your needs.
 * <p>
 * The returned {@link Tokenizer} implementations are -if not stated otherwise-
 * once usable implementations.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
@DefaultAnnotation( { Nonnull.class, CheckReturnValue.class })
public class TokenizerFactory {

	/**
	 * Constructs a simple {@link TokenizerFactory}.
	 */
	public TokenizerFactory() {
		super();
		// No implementation selection options yet.
	}

	/**
	 * @param splitExpression
	 *            The regular expression to split the values.
	 * @return A simple splitting {@link Tokenizer}.
	 * @see RegExpTokenizer#RegExpTokenizer(int, String)
	 */
	public Tokenizer createSplitTokenizer(@RegEx final String splitExpression) {
		return createSplitTokenizer(splitExpression, 0);
	}

	/**
	 * @param splitExpression
	 *            The regular expression to split the values.
	 * @param offset
	 *            The start position. ({@code 0}-based.)
	 * @return A simple splitting {@link Tokenizer}.
	 * @see RegExpTokenizer#RegExpTokenizer(int, String)
	 */
	public Tokenizer createSplitTokenizer(final String splitExpression,
			final int offset) {
		return new RegExpTokenizer(offset, splitExpression);
	}

	/**
	 * @param splitExpression
	 *            The {@link Pattern} to split the values.
	 * @param offset
	 *            The start position. ({@code 0}-based.)
	 * @return A simple splitting {@link Tokenizer}.
	 * @see RegExpTokenizer#RegExpTokenizer(int, String)
	 */
	public Tokenizer createSplitTokenizer(final Pattern splitExpression,
			final int offset) {
		return new RegExpTokenizer(offset, splitExpression);
	}

	/**
	 * @param groupStart
	 *            A {@link Pattern} for start group.
	 * @param groupEnd
	 *            A {@link Pattern} for close group.
	 * @return A {@link Tokenizer} with groups.
	 */
	Tokenizer createGroupingTokenizer(final Pattern groupStart,
			final Pattern groupEnd) {
		return new GroupingTokenizer(0, groupStart, groupEnd);
	}

	/**
	 * @param groupStart
	 *            A {@link Pattern} for start group.
	 * @param groupEnd
	 *            A {@link Pattern} for close group.
	 * @param offset
	 *            The start position. ({@code 0}-based.)
	 * @return A {@link Tokenizer} with groups.
	 */
	Tokenizer createGroupingTokenizer(final Pattern groupStart,
			final Pattern groupEnd, final int offset) {
		return new GroupingTokenizer(offset, groupStart, groupEnd);
	}

	/**
	 * @param continueState
	 *            The state to start the {@link Tokenizer}.
	 * @param groupStart
	 *            A {@link Pattern} for start group.
	 * @param groupEnd
	 *            A {@link Pattern} for close group.
	 * @param offset
	 *            The start position. ({@code 0}-based.)
	 * @return A {@link Tokenizer} with only {@link SimpleToken} results.
	 */
	public Tokenizer createGroupingTokenizer(
			final Pair<? extends Token, ? extends List<? extends Token>> continueState,
			final Pattern groupStart, final Pattern groupEnd, final int offset) {
		return new FilterTokenizer(new GroupingTokenizer(offset, groupStart,
				groupEnd, continueState.getLeft(), continueState.getRight()));
	}

	/**
	 * @param continueState
	 *            The state to start the {@link Tokenizer}.
	 * @param acceptedClasses
	 *            These are the classes that are returned.
	 * @param goIntoCompounds
	 *            If {@code true} it will go and select inside
	 *            {@link CompoundToken}s.
	 * @param groupStart
	 *            A {@link Pattern} for start group.
	 * @param groupEnd
	 *            A {@link Pattern} for close group.
	 * @param offset
	 *            The start position. ({@code 0}-based.)
	 * @return A {@link Tokenizer} with only {@link SimpleToken} results.
	 */
	@SuppressWarnings("unchecked")
	public Tokenizer createGroupingTokenizer(
			final Pair<? extends Token, ? extends List<? extends Token>> continueState,
			final List<Class<? extends Token>> acceptedClasses,
			final boolean goIntoCompounds, final Pattern groupStart,
			final Pattern groupEnd, final int offset) {
		return new FilterTokenizer(new GroupingTokenizer(offset, groupStart,
				groupEnd, continueState.getLeft(), continueState.getRight()),
				goIntoCompounds, acceptedClasses
						.toArray(new Class[acceptedClasses.size()]));
	}
}

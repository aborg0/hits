/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package com.mind_era.knime.common.util.template.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.RegEx;

import org.knime.core.util.Pair;

import com.mind_era.knime.common.util.template.AbstractToken;
import com.mind_era.knime.common.util.template.CompoundToken;
import com.mind_era.knime.common.util.template.Token;
import com.mind_era.knime.common.util.template.TokenizeException;
import com.mind_era.knime.common.util.template.Tokenizer;
import com.mind_era.knime.common.util.template.AbstractToken.EmptyToken;

/**
 * This {@link Tokenizer} implementation allows to handle groups like:
 * <code>${}</code>
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
@Nonnull
@CheckReturnValue
public class GroupingTokenizer extends RegExpTokenizer {
	private static final long serialVersionUID = -1774408671215139193L;

	/**
	 * A simple {@link CompoundToken}.
	 * 
	 * @param <GroupTokenType>
	 *            The type of the boundaries.
	 * @param <ContentTokenType>
	 *            The type of the contents.
	 */
	public static class GroupToken<GroupTokenType extends Token, ContentTokenType extends Token>
			extends AbstractToken implements CompoundToken<Token> {

		private static final long serialVersionUID = 795301323098532142L;
		private final GroupTokenType groupStart;
		private final ContentTokenType content;
		private final GroupTokenType groupEnd;

		/**
		 * @param groupStart
		 *            The start boundary {@link Token}.
		 * @param content
		 *            The content {@link Token}.
		 * @param groupEnd
		 *            The end boundary {@link Token}.
		 */
		public GroupToken(final GroupTokenType groupStart,
				final ContentTokenType content, final GroupTokenType groupEnd) {
			super(content.getStartPosition(), content.getEndPosition(), content
					.getText());
			this.groupStart = groupStart;
			this.content = content;
			this.groupEnd = groupEnd;
		}

		/**
		 * @return the groupStart
		 */
		public GroupTokenType getGroupStart() {
			return groupStart;
		}

		/**
		 * @return the content
		 */
		public ContentTokenType getContent() {
			return content;
		}

		/**
		 * @return the groupEnd
		 */
		public GroupTokenType getGroupEnd() {
			return groupEnd;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result
					+ (content == null ? 0 : content.hashCode());
			result = prime * result
					+ (groupEnd == null ? 0 : groupEnd.hashCode());
			result = prime * result
					+ (groupStart == null ? 0 : groupStart.hashCode());
			return result;
		}

		@Override
		public boolean equals(final Object obj) {
			if (this == obj) {
				return true;
			}
			if (!super.equals(obj)) {
				return false;
			}
			if (!(obj instanceof GroupToken<?, ?>)) {
				return false;
			}
			final GroupToken<?, ?> other = (GroupToken<?, ?>) obj;
			if (content == null) {
				if (other.content != null) {
					return false;
				}
			} else if (!content.equals(other.content)) {
				return false;
			}
			if (groupEnd == null) {
				if (other.groupEnd != null) {
					return false;
				}
			} else if (!groupEnd.equals(other.groupEnd)) {
				return false;
			}
			if (groupStart == null) {
				if (other.groupStart != null) {
					return false;
				}
			} else if (!groupStart.equals(other.groupStart)) {
				return false;
			}
			return true;
		}

		@Override
		public Iterator<Token> iterator() {
			return Arrays.asList(groupStart, content, groupEnd).listIterator();
		}
	}

	private final Pattern groupStart;
	private final Pattern groupEnd;
	@Nullable
	private final SplitToken origStartToken;
	private final Collection<? extends Token> origPuffer;

	private static final Pair<SplitToken, List<Token>> startState = Pair
			.create(null, Collections.<Token> emptyList());

	/**
	 * @param offset
	 *            The start position. ({@code 0}-based.)
	 * @param groupStart
	 *            A regular expression for start group.
	 * @param groupEnd
	 *            A regular expression for close group.
	 */
	public GroupingTokenizer(final int offset, @RegEx final String groupStart,
			@RegEx final String groupEnd) {
		this(offset, groupStart, groupEnd, null, Collections
				.<Token> emptyList());
	}

	/**
	 * @param offset
	 *            The start position. ({@code 0}-based.)
	 * @param groupStart
	 *            A regular expression for start group.
	 * @param groupEnd
	 *            A regular expression for close group.
	 * @param origStartToken
	 *            The start state's token.
	 * @param origPuffer
	 *            The already matched {@link Token}s.
	 */
	public GroupingTokenizer(final int offset, @RegEx final String groupStart,
			@RegEx final String groupEnd, @Nullable final Token origStartToken,
			final Collection<? extends Token> origPuffer) {
		this(offset, Pattern.compile(groupStart), Pattern.compile(groupEnd),
				origStartToken, origPuffer);
	}

	/**
	 * @param offset
	 *            The start position. ({@code 0}-based.)
	 * @param groupStart
	 *            A {@link Pattern} for start group.
	 * @param groupEnd
	 *            A {@link Pattern} for close group.
	 */
	public GroupingTokenizer(final int offset, final Pattern groupStart,
			final Pattern groupEnd) {
		this(offset, groupStart, groupEnd, null, Collections
				.<Token> emptyList());
	}

	/**
	 * @param offset
	 *            The start position. ({@code 0}-based.)
	 * @param groupStart
	 *            A {@link Pattern} for start group.
	 * @param groupEnd
	 *            A {@link Pattern} for close group.
	 * @param origStartToken
	 *            The start state's token.
	 * @param origPuffer
	 *            The already matched {@link Token}s.
	 */
	public GroupingTokenizer(final int offset, final Pattern groupStart,
			final Pattern groupEnd, @Nullable final Token origStartToken,
			final Collection<? extends Token> origPuffer) {
		super(offset, "(?:" + groupStart.pattern() + ")|(?:"
				+ groupEnd.pattern() + ")");
		this.groupStart = groupStart;
		this.groupEnd = groupEnd;
		if (origStartToken instanceof SplitToken || origStartToken == null) {
			final SplitToken split = (SplitToken) origStartToken;
			this.origStartToken = split;
		} else {
			throw new IllegalArgumentException("Wrong type: "
					+ origStartToken.getClass());
		}
		this.origPuffer = new ArrayList<Token>(origPuffer);
	}

	@Override
	public List<Token> parse(final String text) throws TokenizeException {
		final List<Token> splittedTokens = splitter(text);
		final List<Token> ret = new ArrayList<Token>(splittedTokens.size());
		SplitToken startToken = origStartToken;
		final List<Token> puffer = new ArrayList<Token>(origPuffer);
		for (final Token token : splittedTokens) {
			if (token instanceof SplitToken) {
				final SplitToken splitToken = (SplitToken) token;
				if (groupStart.matcher(splitToken.getText()).matches()
						&& (!groupEnd.matcher(splitToken.getText()).matches() || startToken == null)) {
					if (startToken != null) {
						throw new<Pair<SplitToken, List<Token>>> TokenizeException(
								"This part does not support embedded groups.",
								token.getStartPosition(),
								new Pair<SplitToken, List<Token>>(startToken,
										puffer), token.getStartPosition(),
								startState);
					}
					assert puffer.isEmpty() : puffer;
					startToken = splitToken;
				} else if (groupEnd.matcher(splitToken.getText()).matches()) {
					if (startToken != null) {
						switch (puffer.size()) {
						case 0:
							ret.add(new GroupToken<SplitToken, Token>(
									startToken, EmptyToken.get(splitToken
											.getStartPosition()), splitToken));
							break;
						case 1:
							ret.add(new GroupToken<SplitToken, Token>(
									startToken, puffer.get(0), splitToken));
							break;
						default:
							throw new UnsupportedOperationException(
									"Not supported multiple content in a group.");
						}
						startToken = null;
						puffer.clear();
					} else {
						throw new<Pair<SplitToken, List<Token>>> TokenizeException(
								"Group end without group start. Please review you expressions.",
								token.getStartPosition(), startState, token
										.getEndPosition(), startState);
					}
				}
			} else if (!(token instanceof EmptyToken)) {
				(startToken == null ? ret : puffer).add(token);
			}
		}
		if (startToken != null) {
			final int errorPos = puffer.isEmpty() ? startToken.getEndPosition()
					: puffer.get(0).getEndPosition();
			throw new<Pair<SplitToken, List<Token>>> TokenizeException(
					"Not closed group: " + startToken + " @ "
							+ (startToken.getStartPosition() + 1), errorPos,
					new Pair<SplitToken, List<Token>>(startToken, puffer),
					errorPos, startState);
		}
		return ret;
	}
}

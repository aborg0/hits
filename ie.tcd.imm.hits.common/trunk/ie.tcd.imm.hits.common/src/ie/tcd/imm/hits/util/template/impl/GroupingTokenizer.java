/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package ie.tcd.imm.hits.util.template.impl;

import ie.tcd.imm.hits.util.Pair;
import ie.tcd.imm.hits.util.template.AbstractToken;
import ie.tcd.imm.hits.util.template.Token;
import ie.tcd.imm.hits.util.template.TokenizeException;
import ie.tcd.imm.hits.util.template.Tokenizer;
import ie.tcd.imm.hits.util.template.AbstractToken.EmptyToken;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.RegEx;

import edu.umd.cs.findbugs.annotations.DefaultAnnotation;

/**
 * This {@link Tokenizer} implementation allows to handle groups like:
 * <code>${}</code>
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
@DefaultAnnotation( { Nonnull.class, CheckReturnValue.class })
public class GroupingTokenizer extends AbstractTokenizer {
	private static final long serialVersionUID = -1774408671215139193L;

	/**
	 * 
	 * @param <GroupTokenType>
	 * @param <ContentTokenType>
	 */
	public static class GroupToken<GroupTokenType extends Token, ContentTokenType extends Token>
			extends AbstractToken {

		private static final long serialVersionUID = 795301323098532142L;
		private final GroupTokenType groupStart;
		private final ContentTokenType content;
		private final GroupTokenType groupEnd;

		/**
		 * @param groupStart
		 * @param content
		 * @param groupEnd
		 */
		public GroupToken(final GroupTokenType groupStart,
				final ContentTokenType content, final GroupTokenType groupEnd) {
			super(content.getStartPosition(), content.getEndPosition(), content
					.toString());
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
			if (!(obj instanceof GroupToken)) {
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
	}

	private final Pattern groupStart;
	private final Pattern groupEnd;

	/**
	 * @param offset
	 * @param groupStart
	 * @param groupEnd
	 */
	public GroupingTokenizer(final int offset, @RegEx final String groupStart,
			@RegEx final String groupEnd) {
		super(offset, "(?:" + groupStart + ")|(?:" + groupEnd + ")");
		this.groupStart = Pattern.compile(groupStart);
		this.groupEnd = Pattern.compile(groupEnd);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * ie.tcd.imm.hits.util.template.impl.AbstractTokenizer#parse(java.lang.
	 * String)
	 */
	@Override
	public List<Token> parse(final String text) throws TokenizeException {
		final List<Token> splittedTokens = splitter(text);
		final List<Token> ret = new ArrayList<Token>(splittedTokens.size());
		SplitToken startToken = null;
		final List<Token> puffer = new ArrayList<Token>();
		for (final Token token : splittedTokens) {
			if (token instanceof SplitToken) {
				final SplitToken splitToken = (SplitToken) token;
				if (groupStart.matcher(splitToken.toString()).matches()) {
					if (startToken != null) {
						throw new<Pair<SplitToken, List<Token>>> TokenizeException(
								"This part does not support embedded groups.",
								token.getStartPosition(),
								new Pair<SplitToken, List<Token>>(startToken,
										puffer), token.getStartPosition(),
								new Pair<SplitToken, List<Token>>(null,
										new ArrayList<Token>()));
					}
					assert puffer.isEmpty() : puffer;
					startToken = splitToken;
				}
				if (groupEnd.matcher(splitToken.toString()).matches()) {
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
					} else {
						throw new<Pair<SplitToken, List<Token>>> TokenizeException(
								"Group end without group start. Please review you expressions.",
								token.getStartPosition(),
								new Pair<SplitToken, List<Token>>(null,
										Collections.<Token> emptyList()), token
										.getEndPosition(),
								new Pair<SplitToken, List<Token>>(null,
										Collections.<Token> emptyList()));
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
					errorPos, new Pair<SplitToken, List<Token>>(null,
							new ArrayList<Token>()));
		}
		return ret;
	}
}

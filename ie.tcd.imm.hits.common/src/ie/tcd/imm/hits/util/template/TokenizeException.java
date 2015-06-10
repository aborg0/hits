/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package ie.tcd.imm.hits.util.template;

import java.text.ParseException;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import edu.umd.cs.findbugs.annotations.DefaultAnnotation;

/**
 * A {@link ParseException} with some additional data.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
@DefaultAnnotation( { Nonnull.class, CheckReturnValue.class })
public class TokenizeException extends ParseException {
	private static final long serialVersionUID = -419497436622379860L;
	@Nullable
	private final Object errorState;
	private final int continueOffset;
	@Nullable
	private final Object continueState;

	/**
	 * @param <T>
	 *            Type of {@link Tokenizer} state.
	 * @param s
	 *            Error message.
	 * @param errorOffset
	 *            The position where the error occurred.
	 * @param errorState
	 *            The actual state of the {@link Tokenizer}.
	 * @param continueOffset
	 *            From this position the {@link Tokenizer} can be continued with
	 *            {@code continueState}.
	 * @param continueState
	 *            The state where the parsing should continue. Might be {@code
	 *            null}.
	 */
	public <T> TokenizeException(final String s, final int errorOffset,
			@Nullable final T errorState, final int continueOffset,
			@Nullable final T continueState) {
		super(s, errorOffset);
		this.errorState = errorState;
		this.continueOffset = continueOffset;
		this.continueState = continueState;
	}

	/**
	 * @param <T>
	 *            The type of {@link Tokenizer} state. (Should be type
	 *            parameter, but not possible.)
	 * @return The actual {@link Tokenizer} state when the exception occurred.
	 */
	@Nullable
	@SuppressWarnings("unchecked")
	public <T> T getErrorState() {
		return (T) errorState;
	}

	/**
	 * @return the continueOffset
	 */
	public int getContinueOffset() {
		return continueOffset;
	}

	/**
	 * @param <T>
	 *            The type of {@link Tokenizer} state. (Should be type
	 *            parameter, but not possible.)
	 * @return The {@link Tokenizer} state which is allows to continue creating
	 *         {@link Token}s. Might be {@code null}.
	 */
	@SuppressWarnings("unchecked")
	@Nullable
	public <T> T getContinueState() {
		return (T) continueState;
	}
}

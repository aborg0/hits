/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package ie.tcd.imm.hits.util.template;

import java.util.List;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import edu.umd.cs.findbugs.annotations.DefaultAnnotation;

/**
 * Creates parts/Tokens from a {@link String}.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
@DefaultAnnotation( { Nonnull.class, CheckReturnValue.class })
public interface Tokenizer {
	/**
	 * Creates {@link Token}s from {@code text}.
	 * 
	 * @param text
	 *            A {@link String}.
	 * @return A {@link List} of {@link Token}s.
	 * @throws TokenizeException
	 *             If there were an error.
	 */
	public List<Token> parse(String text) throws TokenizeException;
}

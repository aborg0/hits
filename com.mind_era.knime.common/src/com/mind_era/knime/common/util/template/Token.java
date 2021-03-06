/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package com.mind_era.knime.common.util.template;

import java.io.Serializable;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

/**
 * A part of a {@link String}.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
@Nonnull
@CheckReturnValue
public interface Token extends Serializable {
	/** @return the position from {@code 0}, start of the token */
	int getStartPosition();

	/** @return the position from {@code 0}, end of the token {@code + 1} */
	int getEndPosition();

	/** @return the text matched */
	public String getText();
}
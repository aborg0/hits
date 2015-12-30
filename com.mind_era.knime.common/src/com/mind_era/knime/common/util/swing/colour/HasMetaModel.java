/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package com.mind_era.knime.common.util.swing.colour;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import com.mind_era.knime.common.util.swing.colour.ColourSelector.Line.ComplexMetaModel;

/**
 * An interface for those classes that has {@link ComplexMetaModel}.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
@Nonnull
@CheckReturnValue
interface HasMetaModel {
	/**
	 * @return The actual {@link ComplexMetaModel}.
	 */
	public ComplexMetaModel getMetaModel();
}
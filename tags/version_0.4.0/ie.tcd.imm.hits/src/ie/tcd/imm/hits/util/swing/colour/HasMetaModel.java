/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package ie.tcd.imm.hits.util.swing.colour;

import ie.tcd.imm.hits.util.swing.colour.ColourSelector.Line.ComplexMetaModel;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import edu.umd.cs.findbugs.annotations.DefaultAnnotation;

/**
 * An interface for those classes that has {@link ComplexMetaModel}.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
@DefaultAnnotation( { Nonnull.class, CheckReturnValue.class })
interface HasMetaModel {
	/**
	 * @return The actual {@link ComplexMetaModel}.
	 */
	public ComplexMetaModel getMetaModel();
}
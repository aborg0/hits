/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package ie.tcd.imm.hits.knime.util;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

/**
 * This enum enumerates all the possible HiLite strategies.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
@Nonnull
@CheckReturnValue
public enum HiliteType {
	/** Yellow HiLite, normal not HiLited */
	Normal,
	/** Normal HiLite, others faded. */
	FadeUnHilit,
	/** Normal HiLite, others not visible */
	HideUnHilit;
}

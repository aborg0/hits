/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package org.knime.core.node.defaultnodesettings;

import javax.annotation.Nullable;
import javax.swing.Icon;

/**
 * An interface to get {@link Icon} for the instances.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
public interface HasIcon {
	/**
	 * @return The associated {@link Icon}.
	 */
	@Nullable
	public Icon getIcon();
}

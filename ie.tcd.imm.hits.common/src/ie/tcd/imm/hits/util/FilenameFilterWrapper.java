/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package ie.tcd.imm.hits.util;

import java.io.File;
import java.io.FilenameFilter;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.swing.filechooser.FileFilter;

/**
 * Wraps a {@link FilenameFilter} to a {@link FileFilter}.
 */
@Nonnull
@CheckReturnValue
public class FilenameFilterWrapper extends FileFilter {

	private final FilenameFilter filenameFilter;
	private final boolean allowDirs;

	/**
	 * @param filenameFilter
	 *            The {@link FilenameFilter} to wrap.
	 * @param allowDirs
	 *            Allows folders with any extension if this is {@code true}.
	 */
	public FilenameFilterWrapper(final FilenameFilter filenameFilter,
			final boolean allowDirs) {
		super();
		this.filenameFilter = filenameFilter;
		this.allowDirs = allowDirs;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.swing.filechooser.FileFilter#accept(java.io.File)
	 */
	@Override
	public boolean accept(final File f) {
		return f.isDirectory() && allowDirs
				|| filenameFilter.accept(f.getParentFile(), f.getName());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.swing.filechooser.FileFilter#getDescription()
	 */
	@Override
	public String getDescription() {
		return filenameFilter.toString();
	}

}

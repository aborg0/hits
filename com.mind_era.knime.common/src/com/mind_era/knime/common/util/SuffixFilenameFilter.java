package com.mind_era.knime.common.util;

import java.io.File;
import java.io.FilenameFilter;

/**
 * Simple {@link FilenameFilter} with selected suffix.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
public final class SuffixFilenameFilter implements FilenameFilter {
	private final String suffix;

	/**
	 * @param suffix
	 *            The suffix to match. (<em>Not</em> regular expression.)
	 */
	public SuffixFilenameFilter(final String suffix) {
		super();
		this.suffix = suffix;
	}

	@Override
	public boolean accept(final File dir, final String name) {
		return name.toLowerCase().endsWith(suffix.toLowerCase());
	}
}
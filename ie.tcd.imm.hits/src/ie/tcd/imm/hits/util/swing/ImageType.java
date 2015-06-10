/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package ie.tcd.imm.hits.util.swing;

/** Supported image export types. */
public enum ImageType {
	/** Portable network graphics */
	png("PNG files", "png"),
	/** Scalable vector graphics */
	svg("SVG files", "svg");

	private final String description;
	private final String[] extensions;

	private ImageType(final String description, final String... extensions) {
		this.description = description;
		this.extensions = extensions;
	}

	/**
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * @return the extensions
	 */
	public String[] getExtensions() {
		return extensions.clone();
	}
}
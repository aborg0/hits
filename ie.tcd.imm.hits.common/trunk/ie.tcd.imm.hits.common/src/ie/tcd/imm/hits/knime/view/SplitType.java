/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package ie.tcd.imm.hits.knime.view;

import ie.tcd.imm.hits.util.swing.SelectionType;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * These are the possible splits for the arrangement.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
@Nonnull
@CheckReturnValue
public enum SplitType {
	/**
	 * Mostly for horisontal layout on scollpanes.
	 * <p>
	 * Currently not supported.
	 */
	ParalelSplitHorisontal(SelectionType.MultipleAtLeastOne),
	/**
	 * Mostly for vertical layout on scollpanes.
	 * <p>
	 * Currently not supported.
	 */
	ParalelSplitVertical(SelectionType.MultipleAtLeastOne),
	/**
	 * Mostly for tabs.
	 * <p>
	 * Currently not supported.
	 */
	HierachicalTabs(SelectionType.MultipleAtLeastOne, true),
	/**
	 * Split inside the circle or the rectangle.
	 */
	PrimarySplit(SelectionType.MultipleAtLeastOne),
	/**
	 * Split inside the circle or the rectangle - other direction.
	 */
	SeconderSplit(SelectionType.MultipleAtLeastOne),
	/**
	 * Some optional additional information.
	 * <p>
	 * It is too hard to implement correctly, postponed.
	 */
	AdditionalInfo(SelectionType.MultipleOrNone),
	/**
	 * Selecting single values.
	 */
	SingleSelect(SelectionType.Single);

	private final SelectionType selection;
	private final boolean allowHierarchy;
	private final boolean allowAggregate;

	private SplitType(final SelectionType selection) {
		this(selection, false);
	}

	private SplitType(final SelectionType selection,
			final boolean allowHierarchy) {
		this(selection, allowHierarchy, true);
	}

	private SplitType(final SelectionType selection,
			final boolean allowHierarchy, final boolean allowAggregate) {
		this.selection = selection;
		this.allowHierarchy = allowHierarchy;
		this.allowAggregate = allowAggregate;
	}

	/**
	 * @return the selection type
	 */
	public SelectionType getSelection() {
		return selection;
	}

	/**
	 * @return the allow hierarchy (like a direct product)
	 */
	public boolean isAllowHierarchy() {
		return allowHierarchy;
	}

	/**
	 * @return the allow aggregate (like avg, sd, mad, median, ...)
	 */
	public boolean isAllowAggregate() {
		return allowAggregate;
	}

	/**
	 * Implementations of this interface tells which kind of controls it is.
	 */
	@Deprecated
	interface Positioned {
		/**
		 * @return The associated {@link SplitType} of the control.
		 */
		public SplitType getSplitType();

		/**
		 * @return An optional name for that control.
		 */
		public @Nullable
		String getPositionName();

		/**
		 * Sets the {@code splitType} property.
		 * 
		 * @param splitType
		 *            the new {@link SplitType}.
		 */
		public void setSplitType(SplitType splitType);
	}
}
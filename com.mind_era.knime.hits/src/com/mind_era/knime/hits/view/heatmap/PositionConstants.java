/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package com.mind_era.knime.hits.view.heatmap;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

/**
 * These constants are for the arrangement of possible position to the
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
@Nonnull
@CheckReturnValue
enum PositionConstants {
	/** The default for "hidden" parts. */
	control,
	/** The first part of the control part. */
	primary,
	/** The second part of the control part. */
	secondary,
	/** The third part of the control part. */
	additional,
	/** The first (uppermost) part of the sidebar. */
	sidebarPrimary,
	/** The second part of the sidebar. */
	sidebarSecondary,
	/** The 3rd part of the sidebar. */
	sidebarAdditional,
	/** The lowest part of the sidebar. */
	sidebarOther,
	/** The upper part of the UI, before the heatmaps. */
	upper;
}

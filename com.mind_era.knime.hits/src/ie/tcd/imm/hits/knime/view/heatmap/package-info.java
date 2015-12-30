/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
/**
 * Some specific constraints:
 * <ul>
 * <li>There must be at least one {@link StatTypes#plate} slider for the {@link ControlPanel.ArrangementModel}, and for the first entry it must not be part of an aggregation.</li>
 * <li>The following types have to be on there own, could not merge them with other types:
 * 	<ul>
 * 		<li>{@link StatTypes#plate}</li>
 * 		<li>{@link StatTypes#replicate}</li>
 * 	</ul>
 * </li>
 * </ul>
 * <p>
 * The most important classes:
 * <ul>
 * 	<li>{@link HeatmapNodeModel} is the data model for the values of the wells.</li>
 * 	<li>{@link ViewModel} is responsible for the visual representation of the {@link Heatmap}s.</li>
 * 	<li>{@link Heatmap} is a class holding some {@link WellViewPanel wells}.</li>
 * 	<li></li>
 * </ul>
 */
@Nonnull
@CheckReturnValue
package ie.tcd.imm.hits.knime.view.heatmap;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;


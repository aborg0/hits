/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package com.mind_era.knime.common.util.swing.colour;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import com.mind_era.knime.common.util.swing.colour.ColourSelector.ColourModel;
import com.mind_era.knime.common.view.StatTypes;

/**
 * Factory for the controls and the legends of a {@link ColourComputer} type.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 * @param <Computer>
 *            The actual type of {@link ColourComputer}.
 */
@Nonnull
@CheckReturnValue
public interface ColourFactory<Computer extends ColourComputer> {

	/**
	 * Creates a control to the {@code computer}.
	 * <p>
	 * Should add a listener to the control which updates {@code colourModel}.
	 * 
	 * @param colourModel
	 *            The parent {@link ColourModel}.
	 * @param stat
	 *            The {@link StatTypes} of the control.
	 * @param parameter
	 *            The parameter of the control.
	 * @param computer
	 *            A {@link ColourComputer}.
	 * 
	 * @return A control for {@code computer} with {@code colourModel} parent at
	 *         position {@code parameter, stat}.
	 */
	public ColourControl<Computer> createControl(ColourModel colourModel,
			String parameter, StatTypes stat, Computer computer);

	/**
	 * Creates a legend to {@code computer}.
	 * 
	 * @param computer
	 *            A {@link ColourComputer}.
	 * @return The legend of the {@code computer}.
	 */
	public ColourLegend<Computer> createLegend(Computer computer);

	/**
	 * @return The default model for that type of {@link ColourModel}.
	 */
	public Computer getDefaultModel();
}

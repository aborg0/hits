/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package com.mind_era.knime.common.util.swing.colour;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Map;
import java.util.TreeMap;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.eclipse.jface.preference.ColorSelector;
import org.knime.core.util.Pair;

import com.mind_era.knime.common.internal.KNIMECommonActivator;
import com.mind_era.knime.common.util.interval.Interval;
import com.mind_era.knime.common.util.interval.Interval.DefaultInterval;
import com.mind_era.knime.common.util.swing.colour.ColourSelector.ColourModel;
import com.mind_era.knime.common.view.StatTypes;
import com.mind_era.knime.common.view.prefs.ColourPreferenceConstants;

/**
 * A {@link ColourFactory} for {@link ComplexModel}.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
@Nonnull
@CheckReturnValue
public class ComplexModelFactory implements ColourFactory<ComplexModel> {

	@Override
	public ColourControl<ComplexModel> createControl(
			final ColourModel colourModel, final String parameter,
			final StatTypes stat, final ComplexModel computer) {
		final ComplexControl ret = new ComplexControl(parameter, stat);
		ret.setModel(computer);
		ret.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				final ComplexModel newModel = ((ComplexControl) e.getSource())
						.getColourModel();
				colourModel.setModel(parameter, stat, newModel);
			}
		});
		return ret;
	}

	@Override
	public ColourLegend<ComplexModel> createLegend(final ComplexModel computer) {
		final ColourLegend<ComplexModel> ret = new ComplexLegend();
		ret.setModel(computer, Orientation.South);
		return ret;
	}

	@Override
	public ComplexModel getDefaultModel() {
		final Map<Interval<Double>, Color> discretes = new TreeMap<Interval<Double>, Color>();
		discretes.put(new DefaultInterval<Double>(Double.valueOf(-1.5), Double
				.valueOf(1.5), true, false), ComplexModelFactory
				.getColour(ColourPreferenceConstants.MIDDLE_COLOUR));
		final Map<Interval<Double>, Pair<Color, Color>> continuouses = new TreeMap<Interval<Double>, Pair<Color, Color>>();
		continuouses.put(new DefaultInterval<Double>(Double.valueOf(1.5),
				Double.valueOf(2.5), false, true), new Pair<Color, Color>(
				ComplexModelFactory
						.getColour(ColourPreferenceConstants.MIDDLE_COLOUR),
				ComplexModelFactory
						.getColour(ColourPreferenceConstants.UP_COLOUR)));
		continuouses.put(new DefaultInterval<Double>(Double.valueOf(-2.5),
				Double.valueOf(-1.5), false, true), new Pair<Color, Color>(
				ComplexModelFactory
						.getColour(ColourPreferenceConstants.DOWN_COLOUR),
				ComplexModelFactory
						.getColour(ColourPreferenceConstants.MIDDLE_COLOUR)));
		return new ComplexModel(continuouses, discretes);
	}

	/**
	 * @param key
	 *            A {@link String} in <code>\d{1,3},\d{1,3},\d{1,3}</code>
	 *            format. The numbers should be between {@code 0} (inclusive)
	 *            and {@code 255} (inclusive).
	 * @return The colour belonging to {@code key}.
	 * @see ColorSelector#getColorValue()
	 */
	public static Color getColour(final String key) {
		final String rgbString = KNIMECommonActivator.getInstance()
				.getPreferenceStore().getString(key);
		final int r = Integer.parseInt(rgbString.substring(0, rgbString
				.indexOf(',')));
		final int g = Integer.parseInt(rgbString.substring(rgbString
				.indexOf(',') + 1, rgbString.lastIndexOf(',')));
		final int b = Integer.parseInt(rgbString.substring(rgbString
				.lastIndexOf(',') + 1));
		final float[] hsb = Color.RGBtoHSB(r, g, b, null);
		return Color.getHSBColor(hsb[0], hsb[1], hsb[2]);
	}
}

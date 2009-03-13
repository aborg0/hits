/**
 * 
 */
package ie.tcd.imm.hits.util.swing.colour;

import ie.tcd.imm.hits.knime.view.heatmap.HeatmapNodeModel.StatTypes;
import ie.tcd.imm.hits.knime.view.prefs.ColourPreferenceConstants;
import ie.tcd.imm.hits.knime.xls.ImporterNodePlugin;
import ie.tcd.imm.hits.util.Pair;
import ie.tcd.imm.hits.util.interval.Interval;
import ie.tcd.imm.hits.util.interval.Interval.DefaultInterval;
import ie.tcd.imm.hits.util.swing.colour.ColourSelector.ColourModel;
import ie.tcd.imm.hits.util.swing.colour.ColourSelector.SampleWithText.Orientation;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Map;
import java.util.TreeMap;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.eclipse.jface.preference.ColorSelector;

import edu.umd.cs.findbugs.annotations.DefaultAnnotation;

/**
 * A {@link ColourFactory} for {@link ComplexModel}.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
@DefaultAnnotation( { Nonnull.class, CheckReturnValue.class })
public class ComplexModelFactory implements ColourFactory<ComplexModel> {

	/*
	 * (non-Javadoc)
	 * 
	 * @see ie.tcd.imm.hits.util.swing.colour.ColourFactory#createControl(ie.tcd.imm.hits.util.swing.colour.ColourSelector.ColourModel,
	 *      java.lang.String,
	 *      ie.tcd.imm.hits.knime.view.heatmap.HeatmapNodeModel.StatTypes,
	 *      ie.tcd.imm.hits.util.swing.colour.ColourComputer)
	 */
	@Override
	public ColourControl<ComplexModel> createControl(
			final ColourModel colourModel, final String parameter,
			final StatTypes stat, final ComplexModel computer) {
		final ComplexControl ret = new ComplexControl();
		ret.setModel(computer);
		ret.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				final ComplexModel newModel = ((ComplexControl) e.getSource())
						.getColourModel();
				colourModel.setModel(parameter, stat, newModel);
				// ret.setModel(newModel);
			}
		});
		return ret;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ie.tcd.imm.hits.util.swing.colour.ColourFactory#createLegend(ie.tcd.imm.hits.util.swing.colour.ColourComputer)
	 */
	@Override
	public ColourLegend<ComplexModel> createLegend(final ComplexModel computer) {
		final ColourLegend<ComplexModel> ret = new ComplexLegend();
		ret.setModel(computer, Orientation.South);
		return ret;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ie.tcd.imm.hits.util.swing.colour.ColourFactory#getDefaultModel()
	 */
	@Override
	public ComplexModel getDefaultModel() {
		final Map<Interval<Double>, Color> discretes = new TreeMap<Interval<Double>, Color>();
		// discretes.put(new
		// DefaultInterval<Double>(Double.valueOf(.6666666666),
		// Double.valueOf(1.5), true, true), Color.BLACK);
		discretes.put(new DefaultInterval<Double>(Double.valueOf(-1.5), Double
				.valueOf(1.5), true, false),
				getColour(ColourPreferenceConstants.MIDDLE_COLOUR));
		final Map<Interval<Double>, Pair<Color, Color>> continuouses = new TreeMap<Interval<Double>, Pair<Color, Color>>();
		// return new ComplexModel(2.5, Double.valueOf(1.5), Double
		// .valueOf(.6666666666), .4, Color.RED, Color.DARK_GRAY,
		// Color.DARK_GRAY, Color.GREEN, discretes);
		// continuouses.put(new DefaultInterval<Double>(Double.valueOf(1.5),
		// Double.valueOf(2.5), false, true), new Pair<Color, Color>(
		// getColour(ColourPreferenceConstants.MIDDLE_COLOUR),
		// getColour(ColourPreferenceConstants.UP_COLOUR)));
		// continuouses.put(new DefaultInterval<Double>(Double.valueOf(.4),
		// Double
		// .valueOf(.6666666666), false, true), new Pair<Color, Color>(
		// getColour(ColourPreferenceConstants.DOWN_COLOUR),
		// getColour(ColourPreferenceConstants.MIDDLE_COLOUR)));
		continuouses.put(new DefaultInterval<Double>(Double.valueOf(1.5),
				Double.valueOf(2.5), false, true), new Pair<Color, Color>(
				getColour(ColourPreferenceConstants.MIDDLE_COLOUR),
				getColour(ColourPreferenceConstants.UP_COLOUR)));
		continuouses.put(new DefaultInterval<Double>(Double.valueOf(-2.5),
				Double.valueOf(-1.5), false, true), new Pair<Color, Color>(
				getColour(ColourPreferenceConstants.DOWN_COLOUR),
				getColour(ColourPreferenceConstants.MIDDLE_COLOUR)));
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
		final String rgbString = ImporterNodePlugin.getDefault()
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

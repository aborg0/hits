/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package ie.tcd.imm.hits.image.loci.view;

import ij.process.LUT;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;

import org.jfree.chart.axis.AxisState;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.data.Range;
import org.jfree.ui.RectangleEdge;

/**
 * A {@link NumberAxis} wrapper to show the selected colouring.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
public class ColouredAxis extends NumberAxis {
	private static final long serialVersionUID = -4332077591456672816L;
	private final NumberAxis axis;
	private final LUT lut;

	/**
	 * Creates the wrapper.
	 * 
	 * @param axis
	 *            A {@link NumberAxis} to wrap.
	 * @param lut
	 *            The {@link LUT} to show.
	 */
	public ColouredAxis(final NumberAxis axis, final LUT lut) {
		super();
		this.axis = axis;
		this.lut = lut;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public AxisState draw(final Graphics2D g2, final double cursor,
			final Rectangle2D plotArea, final Rectangle2D dataArea,
			final RectangleEdge edge, final PlotRenderingInfo plotState) {
		final AxisState state = new AxisState(cursor);
		if (!isVisible()) {
			return state;
		}
		final Range r = axis.getRange();
		final double viewMin = axis.valueToJava2D(r.getLowerBound(), dataArea,
				edge);
		final double min = axis.valueToJava2D(lut.min, dataArea, edge);
		final double max = axis.valueToJava2D(lut.max, dataArea, edge);
		final double viewMax = axis.valueToJava2D(r.getUpperBound(), dataArea,
				edge);
		final Color orig = g2.getColor();
		final int[] rgbs = new int[256];
		lut.getRGBs(rgbs);
		g2.setColor(new Color(rgbs[0]));

		final int height = (int) (plotArea.getHeight() - dataArea.getHeight());
		g2.fillRect((int) viewMin, 0, (int) (min - viewMin), height);
		final double scale = (max - min) / 256;
		for (int i = rgbs.length; i-- > 0;) {
			g2.setColor(new Color(rgbs[i]));
			g2.fillRect((int) (min + (i - .5) * scale), 0,
					(int) (scale + .5) + 1, height);
		}
		g2.setColor(new Color(rgbs[255]));
		g2.fillRect((int) (max - .5), 0, (int) (viewMax - max), height);
		g2.setColor(orig);
		return state;
	}
}
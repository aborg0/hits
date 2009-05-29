/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package ie.tcd.imm.hits.util.swing.colour;

import ie.tcd.imm.hits.util.interval.Interval;

import java.util.SortedMap;
import java.util.SortedSet;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.swing.BoxLayout;
import javax.swing.JPanel;

import edu.umd.cs.findbugs.annotations.DefaultAnnotation;

/**
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 * 
 */
@DefaultAnnotation( { Nonnull.class, CheckReturnValue.class })
public class ComplexLegend extends JPanel implements ColourLegend<ComplexModel> {
	private static final long serialVersionUID = 8900759711033022991L;

	/**
	 * A heatmap for the {@link ComplexModel}.
	 */
	static class ComplexSample extends JPanel {
		private static final long serialVersionUID = -9172518248262733408L;
		private ComplexModel model;

		/**
		 * Sets the actual model.
		 * 
		 * @param model
		 *            The new {@link ComplexModel}.
		 */
		protected void setModel(final ComplexModel model) {
			this.model = model;
			repaint();
		}

		private static final class VerticalSample extends ComplexSample {
			private static final long serialVersionUID = -6846329287245896832L;

			/*
			 * (non-Javadoc)
			 * 
			 * @see javax.swing.JComponent#paintComponent(java.awt.Graphics)
			 */
			@Override
			protected void paintComponent(final Graphics g) {
				super.paintComponent(g);
				final Rectangle bounds = getBounds();
				final double low = ComplexSample.getOrElse(super.model
						.getContinuouses(), true, Double.POSITIVE_INFINITY);
				final double high = ComplexSample.getOrElse(super.model
						.getContinuouses(), false, Double.NEGATIVE_INFINITY);
				final double min = Math.min(low, ComplexSample.getOrElse(
						super.model.getDiscretes(), true,
						Double.POSITIVE_INFINITY));
				final double max = Math.max(high, ComplexSample.getOrElse(
						super.model.getDiscretes(), false,
						Double.NEGATIVE_INFINITY));
				for (int i = bounds.height; i-- > 0;) {
					g.setColor(super.model.compute(min + (max - min)
							* (bounds.height - i) / bounds.height));
					g.drawLine(0, i, bounds.width, i);
				}
			}

		}

		private static final class HorizontalSample extends ComplexSample {

			private static final long serialVersionUID = 8874559536310256415L;

			/*
			 * (non-Javadoc)
			 * 
			 * @see javax.swing.JComponent#paintComponent(java.awt.Graphics)
			 */
			@Override
			protected void paintComponent(final Graphics g) {
				super.paintComponent(g);
				final Rectangle bounds = getBounds();
				final double low = ComplexSample.getOrElse(super.model
						.getContinuouses(), true, Double.POSITIVE_INFINITY);
				final double high = ComplexSample.getOrElse(super.model
						.getContinuouses(), false, Double.NEGATIVE_INFINITY);
				final double min = Math.min(low, ComplexSample.getOrElse(
						super.model.getDiscretes(), true,
						Double.POSITIVE_INFINITY));
				final double max = Math.max(high, ComplexSample.getOrElse(
						super.model.getDiscretes(), false,
						Double.NEGATIVE_INFINITY));
				for (int i = bounds.width; i-- > 0;) {
					g.setColor(super.model.compute(min + (max - min)
							* (bounds.width - i) / bounds.width));
					g.drawLine(i, 0, i, bounds.height);
				}
			}
		}

		/**
		 * @param isVertical
		 *            The orientation of the returned sample. (vertical: |,
		 *            horizontal: -)
		 * @return A vertical or horizontal sample.
		 */
		public static ComplexSample create(final boolean isVertical) {
			return isVertical ? new VerticalSample() : new HorizontalSample();
		}

		/**
		 * @param sortedMap
		 *            A {@link SortedMap} with {@link Interval} keys.
		 * @param low
		 *            The lowest value, or the highest is interesting?
		 * @param elseVal
		 *            If the {@code sortedMap} is empty this will be returned.
		 * @return The lowest or the highest value from {@code sortedMap} (or
		 *         {@code elseVal} if not exists).
		 */
		protected static double getOrElse(
				final SortedMap<Interval<Double>, ?> sortedMap,
				final boolean low, final double elseVal) {
			return sortedMap.isEmpty() ? elseVal : low ? sortedMap.firstKey()
					.getLow() : sortedMap.lastKey().getHigh();
		}
	}

	private ComplexSample sample;
	private TextPanel textPanel;

	private static final class TextPanel extends JPanel {
		private static final long serialVersionUID = -1195603392371111053L;
		private final Orientation orientation;
		private final SortedSet<Double> values;

		TextPanel(final Orientation orientation, final ComplexModel model) {
			super();
			this.orientation = orientation;
			values = model.getValues();
		}

		@Override
		protected void paintComponent(final Graphics g) {
			super.paintComponent(g);
			if (values.isEmpty()) {
				return;
			}
			final Rectangle bounds = getBounds();
			final double smallest = values.first().doubleValue();
			final double largest = values.last().doubleValue();
			g.setColor(Color.BLACK);
			g.setFont(g.getFont().deriveFont(
					orientation.isVertical() ? 8.5f : 8.0f));
			int i = 0;
			if (values.size() == 0) {

			}
			final int n = values.size() == 1 ? 2 : values.size();
			for (final Double value : values) {
				final String val = Double.toString(Math.round(value
						.doubleValue() * 100) / 100.0);
				final double range = largest - smallest;
				final int strWidth = g.getFontMetrics().getStringBounds(val, g)
						.getBounds().width;
				final int fontHeight = getFontMetrics(getFont()).getHeight();
				switch (orientation) {
				case East:
					g.drawString(val, gap(bounds),
							(bounds.height - fontHeight * 2 / 3) * (n - 1 - i)
									/ (n - 1) + fontHeight * 2 / 3);
					g.drawLine(0, (int) ((largest - value.doubleValue())
							/ range * bounds.height), gap(bounds) - 2,
							(bounds.height - fontHeight * 2 / 3) * (n - 1 - i)
									/ (n - 1) + fontHeight / 3);
					break;
				case West: {
					g.drawString(val, 6, (bounds.height - fontHeight * 2 / 3)
							* (n - 1 - i) / (n - 1) + fontHeight * 2 / 3);
					g
							.drawLine(strWidth + 7,
									(bounds.height - fontHeight * 2 / 3)
											* (n - 1 - i) / (n - 1)
											+ fontHeight / 3, bounds.width,
									(int) ((largest - value.doubleValue())
											/ range * bounds.height));
					break;
				}
				case North: {
					if (i == n - 1) {
						g.drawString(val, 0, fontHeight * 2 / 3 + (n - i + 1)
								% 2 * bounds.height / 2);
						g
								.drawLine(strWidth / 2, fontHeight * 2 / 3
										+ (n - i + 1) % 2 * bounds.height / 2,
										(int) ((largest - value.doubleValue())
												/ range * bounds.width),
										bounds.height);
					} else {
						g.drawString(val,
								bounds.width * (n - i) / n - strWidth,
								fontHeight * 2 / 3 + (n - i + 1) % 2
										* bounds.height / 2);
						g
								.drawLine(bounds.width * (n - i) / n - strWidth
										/ 2, fontHeight * 2 / 3 + (n - i + 1)
										% 2 * bounds.height / 2,
										(int) ((largest - value.doubleValue())
												/ range * bounds.width),
										bounds.height);
					}
					break;
				}
				case South: {
					if (i == n - 1) {
						g.drawString(val, 0, bounds.height - (n - i + 1) % 2
								* bounds.height / 2 - fontHeight / 4);
						g
								.drawLine(strWidth / 2, bounds.height
										- (n - i + 1) % 2 * bounds.height / 2
										- fontHeight * 5 / 6,
										(int) ((largest - value.doubleValue())
												/ range * bounds.width), 0);
					} else {
						g.drawString(val,
								bounds.width * (n - i) / n - strWidth,
								bounds.height - (n - i + 1) % 2 * bounds.height
										/ 2 - fontHeight / 4);
						g
								.drawLine(bounds.width * (n - i) / n - strWidth
										/ 2, bounds.height - (n - i + 1) % 2
										* bounds.height / 2 - fontHeight * 5
										/ 6, (int) ((largest - value
										.doubleValue())
										/ range * bounds.width), 0);
					}
					break;
				}
				default:
					break;
				}
				++i;
			}
		}

		/**
		 * @param bounds
		 *            The bounds of the whole {@link TextPanel}.
		 * @return The gap between the {@link TextPanel} and the
		 *         {@link ComplexSample}.
		 */
		private int gap(final Rectangle bounds) {
			return bounds.width / 3;
		}

	}

	@Override
	public void setModel(final ComplexModel model, final Orientation orientation) {
		if (sample != null) {
			remove(sample);
		}
		if (textPanel != null) {
			remove(textPanel);
		}
		final int sizeOfBar = 10;
		switch (orientation) {
		case East:
			add(sample = ComplexSample.create(orientation.isVertical()));
			sample.setPreferredSize(new Dimension(sizeOfBar,
					getPreferredSize().height));
			textPanel = new TextPanel(orientation, model);
			textPanel.setPreferredSize(new Dimension(getPreferredSize().width
					- sizeOfBar, getPreferredSize().height));
			add(textPanel);
			setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
			break;
		case North:
			textPanel = new TextPanel(orientation, model);
			textPanel.setPreferredSize(new Dimension(getPreferredSize().width,
					getPreferredSize().height - sizeOfBar));
			add(textPanel);
			add(sample = ComplexSample.create(orientation.isVertical()));
			sample.setPreferredSize(new Dimension(getPreferredSize().width,
					sizeOfBar));
			setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
			break;
		case West:
			textPanel = new TextPanel(orientation, model);
			add(textPanel);
			add(sample = ComplexSample.create(orientation.isVertical()));
			sample.setPreferredSize(new Dimension(sizeOfBar,
					getPreferredSize().height));
			textPanel.setPreferredSize(new Dimension(getPreferredSize().width
					- sizeOfBar, getPreferredSize().height));
			setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
			break;
		case South: {
			textPanel = new TextPanel(orientation, model);
			add(sample = ComplexSample.create(orientation.isVertical()));
			sample.setPreferredSize(new Dimension(getPreferredSize().width,
					sizeOfBar));
			add(textPanel);
			textPanel.setPreferredSize(new Dimension(getPreferredSize().width,
					getPreferredSize().height - sizeOfBar));
			setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
			break;
		}
		default:
			break;
		}
		sample.setModel(model);
		revalidate();
	}

	@Override
	public ComplexModel getModel() {
		return sample.model;
	}
}

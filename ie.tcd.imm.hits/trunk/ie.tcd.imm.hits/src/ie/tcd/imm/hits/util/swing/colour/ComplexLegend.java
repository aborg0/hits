/**
 * 
 */
package ie.tcd.imm.hits.util.swing.colour;

import ie.tcd.imm.hits.util.interval.Interval;
import ie.tcd.imm.hits.util.swing.colour.ColourSelector.SampleWithText.Orientation;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.util.SortedMap;

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
				// final Interval<Double> firstKey = super.model.getDiscretes()
				// .isEmpty() ? null : super.model.getDiscretes()
				// .firstKey();
				// final Interval<Double> lastKey = super.model.getDiscretes()
				// .isEmpty() ? null : super.model.getDiscretes()
				// .lastKey();
				final double low = getOrElse(super.model.getContinuouses(),
						true, Double.POSITIVE_INFINITY);
				final double high = getOrElse(super.model.getContinuouses(),
						false, Double.NEGATIVE_INFINITY);
				final double min = Math.min(low, getOrElse(super.model
						.getDiscretes(), true, Double.POSITIVE_INFINITY));
				final double max = Math.max(high, getOrElse(super.model
						.getDiscretes(), false, Double.NEGATIVE_INFINITY));
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
				final double low = getOrElse(super.model.getContinuouses(),
						true, Double.POSITIVE_INFINITY);
				final double high = getOrElse(super.model.getContinuouses(),
						false, Double.NEGATIVE_INFINITY);
				final double min = Math.min(low, getOrElse(super.model
						.getDiscretes(), true, Double.POSITIVE_INFINITY));
				final double max = Math.max(high, getOrElse(super.model
						.getDiscretes(), false, Double.NEGATIVE_INFINITY));
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

		TextPanel(final Orientation orientation, final ComplexModel model) {
			super();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ie.tcd.imm.hits.util.swing.colour.ColourLegend#setModel(ie.tcd.imm.hits.util.swing.colour.ColourComputer,
	 *      ie.tcd.imm.hits.util.swing.colour.ColourSelector.SampleWithText.Orientation)
	 */
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see ie.tcd.imm.hits.util.swing.colour.ColourLegend#getModel()
	 */
	@Override
	public ComplexModel getModel() {
		return sample.model;
	}
}

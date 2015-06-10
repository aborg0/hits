package ie.tcd.imm.hits.knime.view.heatmap;

import ie.tcd.imm.hits.knime.util.VisualUtils;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;

import javax.swing.JPanel;

/**
 * This class is for selecting the colours for different parameters and its
 * statistical, or normal values.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
public class ColourSelector extends JPanel {
	private static final long serialVersionUID = 1927466055122883656L;

	/**
	 * A sample for the continuous colouring.
	 */
	public static class Sample extends JPanel {
		private Color up = Color.RED;

		private Color down = Color.GREEN, middle = Color.BLACK;

		private double upVal = 1.0;

		private double downVal = -1.0, middleVal = 0.0;

		private final class VerticalSample extends Sample {
			private static final long serialVersionUID = 9076573632082832266L;

			private VerticalSample() {
				super();
				setPreferredSize(new Dimension(20, 50));
			}

			@Override
			protected void paintComponent(final Graphics g) {
				super.paintComponent(g);
				final Rectangle bounds = getBounds();
				for (int i = bounds.height; i-- > 0;) {
					g.setColor(VisualUtils.colourOf(
							super.downVal + (super.upVal - super.downVal) * i
									/ bounds.height, super.down, super.middle,
							super.up, super.downVal, super.middleVal,
							super.upVal));
					g.drawLine(0, i, bounds.width, i);
				}
			}
		}

		private final class HorizontalSample extends Sample {
			private static final long serialVersionUID = -470116411480038777L;

			private HorizontalSample() {
				super();
				setPreferredSize(new Dimension(50, 20));
			}
		}

		/**
		 * @param isVertical
		 *            The orientation of the {@link Sample}.
		 * @return A horizontal if not {@code isVertical}, else a vertical
		 *         {@link Sample}.
		 */
		public static Sample create(final boolean isVertical) {
			return isVertical ? new Sample().new VerticalSample()
					: new Sample().new HorizontalSample();
		}

		private static final long serialVersionUID = 1230862889150618654L;

		public void setDown(final Color down) {
			this.down = down;
			repaint();
		}

		public void setDown(final double downVal) {
			this.downVal = downVal;
			repaint();
		}

		public void setMiddle(final Color middle) {
			this.middle = middle;
			repaint();
		}

		public void setMiddle(final double middleVal) {
			this.middleVal = middleVal;
			repaint();
		}

		public void setUp(final Color up) {
			this.up = up;
			repaint();
		}

		public void setUp(final double upVal) {
			this.upVal = upVal;
			repaint();
		}
	}

	private static class DoubleValueSelector extends JPanel {
		private static final long serialVersionUID = -6389541195312535553L;

		private final Sample sample = Sample.create(true);
	}
}

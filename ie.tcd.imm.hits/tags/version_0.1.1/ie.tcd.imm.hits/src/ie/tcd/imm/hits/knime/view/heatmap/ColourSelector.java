package ie.tcd.imm.hits.knime.view.heatmap;

import ie.tcd.imm.hits.knime.util.VisualUtils;
import ie.tcd.imm.hits.knime.view.heatmap.ColourSelector.DoubleValueSelector.Model;
import ie.tcd.imm.hits.knime.view.heatmap.HeatmapNodeModel.StatTypes;
import ie.tcd.imm.hits.knime.view.prefs.ColourPreferenceConstants;
import ie.tcd.imm.hits.knime.xls.ImporterNodePlugin;
import ie.tcd.imm.hits.util.Displayable;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.Serializable;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.eclipse.jface.preference.ColorSelector;

import edu.umd.cs.findbugs.annotations.DefaultAnnotation;

/**
 * This class is for selecting the colours for different parameters and its
 * statistical, or normal values.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
@DefaultAnnotation( { Nonnull.class, CheckReturnValue.class })
public class ColourSelector extends JPanel {
	private static final long serialVersionUID = 1927466055122883656L;

	/** The default {@link Model} for real valued parameters. */
	public static final Model DEFAULT_MODEL = new Model(-2.0, Double
			.valueOf(0.0), 2.0, Color.GREEN, Color.YELLOW, Color.RED);

	/**
	 * The possible ranges of an interval.
	 */
	public static enum RangeType implements Displayable {
		/** The minimum value. */
		min("Minimum"),
		/** The maximum value */
		max("Maximum"),
		/** The median value */
		median("Median"),
		/** The mean/average value */
		average("Average or Mean"),
		/** The standard deviation value */
		stdev("Standard Deviation"),
		/** The median absolute deviation value */
		mad("Median Absolute Deviation");

		private final String displayText;

		private RangeType(final String displayText) {
			this.displayText = displayText;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see ie.tcd.imm.hits.util.Displayable#getDisplayText()
		 */
		@Override
		public String getDisplayText() {
			return displayText;
		}
	}

	/**
	 * This is a {@link ColourModel} for each parameter and {@link StatTypes}
	 * with real valued parameters.
	 */
	public static class ColourModel implements Serializable {
		private static final long serialVersionUID = -6758031469463423849L;
		private final Map<String, Map<StatTypes, Model>> models = new TreeMap<String, Map<StatTypes, Model>>();
		private final List<ActionListener> listeners = new ArrayList<ActionListener>();

		/**
		 * Sets the {@link Model} for the selected {@code parameter} and
		 * {@code stat}.
		 * 
		 * @param parameter
		 *            A parameter.
		 * @param stat
		 *            A {@link StatTypes}.
		 * @param model
		 *            The new {@link Model}.
		 */
		protected void setModel(final String parameter, final StatTypes stat,
				final Model model) {
			if (!models.containsKey(parameter)) {
				models.put(parameter, new EnumMap<StatTypes, Model>(
						StatTypes.class));
			}
			final Map<StatTypes, Model> map = models.get(parameter);
			map.put(stat, model);
			fireModelChanged();
		}

		/**
		 * Removes {@code listener}.
		 * 
		 * @param listener
		 *            An {@link ActionListener}.
		 */
		public void removeActionListener(final ActionListener listener) {
			listeners.remove(listener);
		}

		/**
		 * Adds the {@link ActionListener} to the listeners if not previously
		 * contained.
		 * 
		 * @param listener
		 *            An {@link ActionListener}.
		 */
		public void addActionListener(final ActionListener listener) {
			if (!listeners.contains(listener)) {
				listeners.add(listener);
			}
		}

		private void fireModelChanged(final ActionEvent e) {
			for (final ActionListener listener : listeners) {
				listener.actionPerformed(e);
			}
		}

		private void fireModelChanged() {
			fireModelChanged(new ActionEvent(this, (int) (System
					.currentTimeMillis() & 0xffffffff), "modelChanged"));
		}

		/**
		 * Gets the associated {@link Model} for {@code parameter} and
		 * {@code stat}. It may return {@code null}.
		 * 
		 * @param parameter
		 *            A parameter.
		 * @param stat
		 *            A {@link StatTypes} (with non-discrete values).
		 * @return The associated {@link Model} or {@code null}.
		 */
		public @Nullable
		Model getModel(final String parameter, final StatTypes stat) {
			final Map<StatTypes, Model> map = models.get(parameter);
			if (map != null) {
				return map.get(stat);
			}
			return null;
		}

		/**
		 * 
		 */
		void notifyListeners() {
			fireModelChanged();
		}
	}

	/**
	 * A sample for the continuous colouring.
	 */
	public static abstract class Sample extends JPanel {
		private Color up = Color.RED;

		private Color down = Color.GREEN;
		private @Nullable
		Color middle = Color.BLACK;

		private double upVal = 1.0;

		private double downVal = -1.0, middleVal = 0.0;

		private static final class VerticalSample extends Sample {
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
					g.setColor(VisualUtils.colourOf(super.downVal
							+ (super.upVal - super.downVal)
							* (bounds.height - i) / bounds.height, super.down,
							super.middle, super.up, super.downVal,
							super.middleVal, super.upVal));
					g.drawLine(0, i, bounds.width/* / 3 */, i);
				}
			}
		}

		private static final class HorizontalSample extends Sample {
			private static final long serialVersionUID = -470116411480038777L;

			private HorizontalSample() {
				super();
				setPreferredSize(new Dimension(50, 20));
			}

			@Override
			protected void paintComponent(final Graphics g) {
				super.paintComponent(g);
				final Rectangle bounds = getBounds();
				for (int i = bounds.width; i-- > 0;) {
					g.setColor(VisualUtils.colourOf(super.downVal
							+ (super.upVal - super.downVal) * i / bounds.width,
							super.down, super.middle, super.up, super.downVal,
							super.middleVal, super.upVal));
					g.drawLine(i, 0, i, bounds.height);
				}
			}
		}

		/**
		 * @param isVertical
		 *            The orientation of the {@link Sample}.
		 * @return A horizontal if not {@code isVertical}, else a vertical
		 *         {@link Sample}.
		 */
		public static Sample create(final boolean isVertical) {
			return isVertical ? new VerticalSample() : new HorizontalSample();
		}

		private static final long serialVersionUID = 1230862889150618654L;

		private Model model;

		/**
		 * Sets the colour for the lower values.
		 * 
		 * @param down
		 *            A {@link Color colour} for the lower values.
		 */
		public void setDown(final Color down) {
			this.down = down;
			repaint();
		}

		/**
		 * Sets the value for the lower interval.
		 * 
		 * @param downVal
		 *            The new value for the lower interval.
		 */
		public void setDown(final double downVal) {
			this.downVal = downVal;
			repaint();
		}

		/**
		 * Sets the colour for the middle values.
		 * 
		 * @param middle
		 *            A {@link Color colour} for the middle values. May be
		 *            {@code null}, which means it is not specified, the
		 *            automatically the same as should be between the two
		 *            extreme colours.
		 */
		public void setMiddle(@Nullable
		final Color middle) {
			this.middle = middle;
			repaint();
		}

		/**
		 * Sets the value for the middle part of the interval.
		 * 
		 * @param middleVal
		 *            The new value for the middle part of the interval.
		 */
		public void setMiddle(final double middleVal) {
			this.middleVal = middleVal;
			repaint();
		}

		/**
		 * Sets the colour for the upper values.
		 * 
		 * @param up
		 *            A {@link Color colour} for the upper values.
		 */
		public void setUp(final Color up) {
			this.up = up;
			repaint();
		}

		/**
		 * Sets the value for the upper interval.
		 * 
		 * @param upVal
		 *            The new value for the upper interval.
		 */
		public void setUp(final double upVal) {
			this.upVal = upVal;
			repaint();
		}

		/**
		 * Sets the model to the samples.
		 * 
		 * @param model
		 *            A {@link Model} of colours.
		 */
		protected void setModel(final Model model) {
			this.model = model;
			this.downVal = model.getDownVal();
			this.middleVal = model.getMiddleVal() == null ? 0.0 : model
					.getMiddleVal().doubleValue();
			this.upVal = model.getUpVal();
			this.down = model.getDown();
			this.middle = model.getMiddleVal() == null ? null : model
					.getMiddle();
			this.up = model.getUp();
			repaint();
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.awt.Component#toString()
		 */
		@Override
		public String toString() {
			return down.toString()
					+ " ("
					+ downVal
					+ ")-> "
					+ (middle != null ? middle.toString() + " (" + middleVal
							+ ")->" : "") + up.toString() + " (" + upVal + ")";
		}

		/**
		 * @return The associate {@link Model}.
		 */
		public Model getModel() {
			return model;
		}
	}

	/**
	 * This class shows the sample of the heatmap legend with the proper values.
	 */
	public static class SampleWithText extends JPanel {
		private static final long serialVersionUID = 8745641973568977951L;
		private Sample sample;
		private TextPanel textPanel;

		/**
		 * Creates the {@link SampleWithText} with {@link FlowLayout}.
		 */
		public SampleWithText() {
			super();
			setLayout(/* new FlowLayout(FlowLayout.CENTER) */new BoxLayout(
					this, BoxLayout.X_AXIS));
		}

		/** The orientation of the sample (text). */
		public static enum Orientation {
			/** At the up */
			North(false),
			/** Left */
			West(true),
			/** Down */
			South(false),
			/** Right */
			East(true);
			private final boolean isVertical;

			private Orientation(final boolean isVertical) {
				this.isVertical = isVertical;
			}

			/**
			 * @return the isVertical
			 */
			public boolean isVertical() {
				return isVertical;
			}
		}

		private final class TextPanel extends JPanel {
			private static final long serialVersionUID = -8128412170298608805L;
			private final Orientation orientation;
			private final double upVal;
			private final Double middleVal;
			private final double downVal;

			/**
			 * Constructs the text for a {@link Sample}.
			 * 
			 * @param orientation
			 *            The orientation of the {@link TextPanel}.
			 * @param upVal
			 *            The high value.
			 * @param middleVal
			 *            The middle value or {@code null}.
			 * @param downVal
			 *            The low value.
			 */
			TextPanel(final Orientation orientation, final double upVal,
					final Double middleVal, final double downVal) {
				super();
				this.orientation = orientation;
				this.upVal = upVal;
				this.middleVal = middleVal;
				this.downVal = downVal;
			}

			/*
			 * (non-Javadoc)
			 * 
			 * @see javax.swing.JComponent#paintComponent(java.awt.Graphics)
			 */
			@Override
			protected void paintComponent(final Graphics g) {
				// super.paintComponent(g);
				final Rectangle bounds = getBounds();
				final String upStr = Double
						.toString(Math.round(upVal * 100) / 100.0);
				final String middleStr = middleVal == null ? ""
						: Double.toString(Math
								.round(middleVal.doubleValue() * 100) / 100.0);
				final String downStr = Double.toString(Math
						.round(downVal * 100) / 100.0);
				g.setColor(Color.BLACK);
				g.setFont(g.getFont().deriveFont(8.5f));
				switch (orientation) {
				case East:
					g.drawString(upStr, gap(bounds), getFontMetrics(getFont())
							.getHeight() * 2 / 3);
					if (middleVal != null) {
						g.drawString(middleStr, gap(bounds), bounds.height / 2);
					}
					g.drawString(downStr, gap(bounds), bounds.height);
					g.drawLine(0, 0, gap(bounds) - 2, getFontMetrics(getFont())
							.getHeight() / 3);
					if (middleVal != null) {
						g.drawLine(0, (int) ((upVal - middleVal.doubleValue())
								/ (upVal - downVal) * bounds.height),
								gap(bounds) - 2, bounds.height / 2
										- getFontMetrics(getFont()).getHeight()
										/ 3);
					}
					g.drawLine(0, bounds.height, gap(bounds) - 2, bounds.height
							- getFontMetrics(getFont()).getHeight() / 3);
					break;
				case West: {
					g.drawString(upStr, 5, getFontMetrics(getFont())
							.getHeight() * 2 / 3);
					if (middleVal != null) {
						g.drawString(middleStr, 5, bounds.height / 2);
					}
					g.drawString(downStr, 5, bounds.height);
					g.drawLine(g.getFontMetrics().getStringBounds(downStr, g)
							.getBounds().width + 7, getFontMetrics(getFont())
							.getHeight() / 3, bounds.width, 0);
					if (middleVal != null) {
						g.drawLine(g.getFontMetrics().getStringBounds(
								middleStr, g).getBounds().width + 7,
								bounds.height / 2
										- getFontMetrics(getFont()).getHeight()
										/ 3, bounds.width,
								(int) ((upVal - middleVal.doubleValue())
										/ (upVal - downVal) * bounds.height));
					}
					g.drawLine(g.getFontMetrics().getStringBounds(upStr, g)
							.getBounds().width + 7, bounds.height
							- getFontMetrics(getFont()).getHeight() / 3,
							bounds.width, bounds.height);
					break;
				}
				case North: {
					g.drawString(upStr, bounds.width
							- g.getFontMetrics().getStringBounds(upStr, g)
									.getBounds().width, getFontMetrics(
							getFont()).getHeight() * 2 / 3);
					if (middleVal != null) {
						g
								.drawString(middleStr, bounds.width
										/ 2
										- g.getFontMetrics().getStringBounds(
												middleStr, g).getBounds().width
										/ 2, bounds.height / 2
										+ getFontMetrics(getFont()).getHeight()
										/ 3);
					}
					g.drawString(downStr, 0, getFontMetrics(getFont())
							.getHeight() * 2 / 3);
					g.drawLine(g.getFontMetrics().getStringBounds(downStr, g)
							.getBounds().width / 2, getFontMetrics(getFont())
							.getHeight() / 3, 0, bounds.height);
					if (middleVal != null) {
						g
								.drawLine(
										bounds.width / 2,
										bounds.height
												/ 2
												+ getFontMetrics(getFont())
														.getHeight() / 3,
										bounds.width
												- (int) ((upVal - middleVal
														.doubleValue())
														/ (upVal - downVal) * bounds.width),
										bounds.height);
					}
					g.drawLine(bounds.width
							- g.getFontMetrics().getStringBounds(upStr, g)
									.getBounds().width / 2, getFontMetrics(
							getFont()).getHeight() * 2 / 3, bounds.width,
							bounds.height);
					break;
				}
				case South: {
					g.drawString(upStr, bounds.width
							- g.getFontMetrics().getStringBounds(upStr, g)
									.getBounds().width, bounds.height
							- getFontMetrics(getFont()).getHeight() * 2 / 3);
					if (middleVal != null) {
						g
								.drawString(middleStr, bounds.width
										/ 2
										- g.getFontMetrics().getStringBounds(
												middleStr, g).getBounds().width
										/ 2, bounds.height / 2
										- getFontMetrics(getFont()).getHeight()
										/ 4);
					}
					g.drawString(downStr, 0, bounds.height
							- getFontMetrics(getFont()).getHeight() * 2 / 3);
					g.drawLine(g.getFontMetrics().getStringBounds(downStr, g)
							.getBounds().width / 2, bounds.height
							- getFontMetrics(getFont()).getHeight() * 4 / 3, 0,
							0);
					if (middleVal != null) {
						g
								.drawLine(
										bounds.width / 2,
										bounds.height
												/ 2
												- getFontMetrics(getFont())
														.getHeight() * 5 / 6,
										bounds.width
												- (int) ((upVal - middleVal
														.doubleValue())
														/ (upVal - downVal) * bounds.width),
										0);
					}
					g.drawLine(bounds.width
							- g.getFontMetrics().getStringBounds(upStr, g)
									.getBounds().width / 2, bounds.height
							- getFontMetrics(getFont()).getHeight() * 4 / 3,
							bounds.width, 0);
					break;
				}
				default:
					break;
				}
			}

			/**
			 * @param bounds
			 *            The bounds of the whole {@link TextPanel}.
			 * @return The gap between the {@link TextPanel} and the
			 *         {@link Sample}.
			 */
			private int gap(final Rectangle bounds) {
				return bounds.width / 3;
			}

			/**
			 * @return The associated model to the sample.
			 */
			public Model getModel() {
				return sample.getModel();
			}

			/*
			 * (non-Javadoc)
			 * 
			 * @see java.awt.Component#toString()
			 */
			@Override
			public String toString() {
				return "" + downVal + " -> "
						+ (middleVal != null ? middleVal + " -> " : "") + upVal;
			}
		}

		/**
		 * Updates the model of this legend.
		 * 
		 * @param model
		 *            A colour {@link Model}.
		 * @param orientation
		 *            The new {@link Orientation} of the {@link SampleWithText}.
		 */
		public void setModel(final Model model, final Orientation orientation) {
			if (sample != null) {
				remove(sample);
			}
			if (textPanel != null) {
				remove(textPanel);
			}
			final int sizeOfBar = 10;
			switch (orientation) {
			case East:
				add(sample = Sample.create(orientation.isVertical()));
				sample.setPreferredSize(new Dimension(sizeOfBar,
						getPreferredSize().height));
				textPanel = new TextPanel(orientation, model.getUpVal(), model
						.getMiddle() == null ? null : model.getMiddleVal(),
						model.getDownVal());
				textPanel.setPreferredSize(new Dimension(
						getPreferredSize().width - sizeOfBar,
						getPreferredSize().height));
				add(textPanel);
				setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
				break;
			case North:
				textPanel = new TextPanel(orientation, model.getUpVal(), model
						.getMiddle() == null ? null : model.getMiddleVal(),
						model.getDownVal());
				textPanel.setPreferredSize(new Dimension(
						getPreferredSize().width, getPreferredSize().height
								- sizeOfBar));
				add(textPanel);
				add(sample = Sample.create(orientation.isVertical()));
				sample.setPreferredSize(new Dimension(getPreferredSize().width,
						sizeOfBar));
				setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
				break;
			case West:
				textPanel = new TextPanel(orientation, model.getUpVal(), model
						.getMiddle() == null ? null : model.getMiddleVal(),
						model.getDownVal());
				add(textPanel);
				add(sample = Sample.create(orientation.isVertical()));
				sample.setPreferredSize(new Dimension(sizeOfBar,
						getPreferredSize().height));
				textPanel.setPreferredSize(new Dimension(
						getPreferredSize().width - sizeOfBar,
						getPreferredSize().height));
				setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
				break;
			case South: {
				textPanel = new TextPanel(orientation, model.getUpVal(), model
						.getMiddle() == null ? null : model.getMiddleVal(),
						model.getDownVal());
				add(sample = Sample.create(orientation.isVertical()));
				sample.setPreferredSize(new Dimension(getPreferredSize().width,
						sizeOfBar));
				add(textPanel);
				textPanel.setPreferredSize(new Dimension(
						getPreferredSize().width, getPreferredSize().height
								- sizeOfBar));
				setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
				break;
			}
			default:
				break;
			}
			sample.setModel(model);
			revalidate();
		}

		/**
		 * @return debug information about the orientation and the colour
		 *         {@link Model}.
		 */
		@Override
		public String toString() {
			return textPanel.orientation.name() + sample;
		}

		/**
		 * @return The associated colour {@link Model}.
		 */
		public Model getModel() {
			return sample.getModel();
		}
	}

	/**
	 * This class helps to select the colouring for a double valued parameter.
	 */
	public static class DoubleValueSelector extends JPanel {
		private static final long serialVersionUID = -6389541195312535553L;

		private final Sample sample = Sample.create(true);
		private final JSpinner up = new JSpinner(new SpinnerNumberModel(Double
				.valueOf(2.0), null, null, Double.valueOf(.1)));
		private final JTextField middle = new JTextField("0.0", 4);
		private final JSpinner down = new JSpinner(new SpinnerNumberModel(
				Double.valueOf(-2.0), null, null, Double.valueOf(.1)));
		private final JButton upButton = new JButton();
		private final JButton middleButton = new JButton();
		private final JButton downButton = new JButton();

		private Model model;

		private final List<ActionListener> listeners = new ArrayList<ActionListener>();

		private static final class ButtonListener implements ActionListener,
				Serializable {
			private static final long serialVersionUID = 1188532264819715469L;
			private final DoubleValueSelector parent;
			private final String text;
			private final Positions pos;
			private final JButton button;

			/**
			 * Constructs a {@link ButtonListener}.
			 * 
			 * @param parent
			 *            The parent {@link DoubleValueSelector}.
			 * @param pos
			 *            The position on that.
			 * @param button
			 *            The associated button.
			 */
			public ButtonListener(final DoubleValueSelector parent,
					final Positions pos, final JButton button) {
				this.parent = parent;
				this.pos = pos;
				switch (pos) {
				case Up:
					text = "upper limit";
					break;
				case Middle:
					text = "middle value";
					break;
				case Down:
					text = "lower limit";
					break;
				default:
					throw new UnsupportedOperationException("Not supported: "
							+ pos);
				}
				this.button = button;
			}

			@Override
			public void actionPerformed(final ActionEvent e) {
				final Color newColour = JColorChooser.showDialog(parent,
						"Select the color for " + text, button.getBackground());
				if (newColour != null) {
					parent.setModel(new Model(parent.model, pos, newColour));
				}
			}
		}

		private static class ValueListener implements ChangeListener,
				ActionListener, Serializable {
			private static final long serialVersionUID = 4990485633527905794L;
			private final Positions pos;
			private final DoubleValueSelector parent;

			/**
			 * @param parent
			 * @param pos
			 *            The position of the component we listen to.
			 */
			public ValueListener(final DoubleValueSelector parent,
					final Positions pos) {
				super();
				this.parent = parent;
				this.pos = pos;
			}

			@Override
			public void stateChanged(final ChangeEvent e) {
				final Double value = ((Double) ((SpinnerModel) e.getSource())
						.getValue());
				parent.setModel(new Model(parent.model, pos, value));
			}

			@Override
			public void actionPerformed(final ActionEvent e) {
				final String value = ((JTextField) e.getSource()).getText();
				try {
					final double val = NumberFormat.getInstance().parse(value)
							.doubleValue();
					parent.setModel(new Model(parent.model, pos, val));
				} catch (final ParseException ex) {
					parent
							.setModel(new Model(parent.model, pos,
									(Double) null));
				}
			}
		}

		/**
		 * The positions of the possible colours.
		 */
		public static enum Positions {
			/** The left/down position */
			Down,
			/** The middle position */
			Middle,
			/** The right/up position */
			Up;
		}

		/**
		 * The colour model for the double values.
		 */
		public static class Model implements Serializable {
			private static final long serialVersionUID = 8613456651113117411L;
			private final double downVal, upVal;
			private @Nullable
			final Double middleVal;
			private final Color down, middle, up;

			/**
			 * Constructs a new {@link Model} with the raw parameters.
			 * 
			 * @param downVal
			 *            down/left value
			 * @param middleVal
			 *            middle value, or {@code null}
			 * @param upVal
			 *            up/right value
			 * @param down
			 *            down colour
			 * @param middle
			 *            middle colour
			 * @param up
			 *            up colour
			 */
			public Model(final double downVal, @Nullable
			final Double middleVal, final double upVal, final Color down,
					final Color middle, final Color up) {
				super();
				this.downVal = downVal;
				this.middleVal = middleVal;
				this.upVal = upVal;
				this.down = down;
				this.middle = middle;
				this.up = up;
			}

			/**
			 * Creates a new {@link Model} based on a previous one with possibly
			 * new {@link Color colour} at {@link Positions position}
			 * {@code pos}.
			 * 
			 * @param model
			 *            A {@link Model}.
			 * @param pos
			 *            A {@link Positions}.
			 * @param col
			 *            The new {@link Color}.
			 */
			public Model(final Model model, final Positions pos, final Color col) {
				this(model.getDownVal(), model.getMiddleVal(),
						model.getUpVal(), pos == Positions.Down ? col : model
								.getDown(), pos == Positions.Middle ? col
								: model.getMiddle(), pos == Positions.Up ? col
								: model.getUp());
			}

			/**
			 * Creates a new {@link Model} based on a previous one with possibly
			 * new {@code value} at {@link Positions position} {@code pos}.
			 * 
			 * @param model
			 *            A {@link Model}.
			 * @param pos
			 *            A {@link Positions}.
			 * @param val
			 *            The new value.
			 */
			public Model(final Model model, final Positions pos, @Nullable
			final Double val) {
				this(pos == Positions.Down ? val.doubleValue() : model
						.getDownVal(), pos == Positions.Middle ? val : model
						.getMiddleVal(), pos == Positions.Up ? val
						.doubleValue() : model.getUpVal(), model.getDown(),
						model.getMiddle(), model.getUp());
			}

			/**
			 * {@inheritDoc}
			 */
			@Override
			public String toString() {
				return getDownVal() + " (" + getDown() + ") -> "
						+ getMiddleVal() + " (" + getMiddle() + ") -> "
						+ getUpVal() + "(" + getUp() + ")";
			}

			/**
			 * @return The {@link Color} for the lower value.
			 */
			public Color getDown() {
				return down;
			}

			/**
			 * @return The {@link Color} for the middle value. (Maybe
			 *         {@code null}.)
			 */
			public Color getMiddle() {
				return middle;
			}

			/**
			 * @return The {@link Color} for the higher value.
			 */
			public Color getUp() {
				return up;
			}

			/**
			 * @return The lower value.
			 */
			public double getDownVal() {
				return downVal;
			}

			/**
			 * @return The middle value.
			 */
			public @Nullable
			Double getMiddleVal() {
				return middleVal;
			}

			/**
			 * @return The higher value.
			 */
			public double getUpVal() {
				return upVal;
			}
		}

		/**
		 * Constructs a {@link DoubleValueSelector} with the defaults.
		 */
		public DoubleValueSelector() {
			this(-2.0, 0.0, 2.0, Color.GREEN, Color.BLACK, Color.RED);
		}

		/**
		 * Constructs a {@link DoubleValueSelector}.
		 * 
		 * @param downVal
		 *            The down limit.
		 * @param middleVal
		 *            The middle value. Maybe {@code null}.
		 * @param upVal
		 *            The up limit.
		 * @param green
		 *            The colour associated to the down value.
		 * @param black
		 *            The colour associated to the middle value.
		 * @param red
		 *            The colour associated to the up value.
		 */
		public DoubleValueSelector(final double downVal,
				final Double middleVal, final double upVal, final Color green,
				final Color black, final Color red) {
			super();
			model = new Model(downVal, middleVal, upVal, green, black, red);
			final GridBagLayout gbl = new GridBagLayout();
			setLayout(gbl);
			setModel(model);
			final GridBagConstraints sampleConstraint = new GridBagConstraints();
			sampleConstraint.gridheight = 3;
			add(sample, sampleConstraint);
			final GridBagConstraints upConstraint = new GridBagConstraints();
			upConstraint.gridx = 1;
			up
					.setPreferredSize(new Dimension(40,
							up.getPreferredSize().height));
			up.getModel().addChangeListener(
					new ValueListener(this, Positions.Up));
			add(up, upConstraint);
			final GridBagConstraints middleConstraint = new GridBagConstraints();
			middleConstraint.gridx = 1;
			middleConstraint.gridy = 1;
			middle.addActionListener(new ValueListener(this, Positions.Middle));
			add(middle, middleConstraint);
			final GridBagConstraints downConstraint = new GridBagConstraints();
			downConstraint.gridx = 1;
			downConstraint.gridy = 2;
			down.getModel().addChangeListener(
					new ValueListener(this, Positions.Down));
			down.setPreferredSize(up.getPreferredSize());
			add(down, downConstraint);
			final GridBagConstraints upButtonConstraint = new GridBagConstraints();
			upButtonConstraint.gridx = 2;
			add(upButton, upButtonConstraint);
			upButton.addActionListener(new ButtonListener(this, Positions.Up,
					upButton));
			final GridBagConstraints middleButtonConstraint = new GridBagConstraints();
			middleButtonConstraint.gridx = 2;
			middleButtonConstraint.gridy = 1;
			add(middleButton, middleButtonConstraint);
			middleButton.addActionListener(new ButtonListener(this,
					Positions.Middle, middleButton));
			final GridBagConstraints downButtonConstraint = new GridBagConstraints();
			downButtonConstraint.gridx = 2;
			downButtonConstraint.gridy = 2;
			add(downButton, downButtonConstraint);
			downButton.addActionListener(new ButtonListener(this,
					Positions.Down, downButton));
		}

		/**
		 * Changes the model.
		 * 
		 * @param model
		 *            The new {@link Model}.
		 */
		public void setModel(final Model model) {
			this.model = model;
			update();
			fireModelChange();
		}

		private void update() {
			down.setValue(Double.valueOf(model.getDownVal()));
			middle.setText(model.getMiddleVal() == null ? "" : String
					.valueOf(model.getMiddleVal().doubleValue()));
			up.setValue(Double.valueOf(model.getUpVal()));
			sample.setDown(model.getDownVal());
			sample.setMiddle(model.getMiddleVal() == null ? 0.0 : model
					.getMiddleVal().doubleValue());
			sample.setUp(model.getUpVal());
			sample.setDown(model.getDown());
			sample.setMiddle(model.getMiddleVal() == null ? null : model
					.getMiddle());
			sample.setUp(model.getUp());
			downButton.setBackground(model.getDown());
			middleButton.setBackground(model.getMiddle() == null ? Color.BLACK
					: model.getMiddle());
			upButton.setBackground(model.getUp());
			sample.setModel(model);
		}

		/**
		 * Adds the {@link ActionListener} to the listeners if not previously
		 * contained.
		 * 
		 * @param actionListener
		 *            An {@link ActionListener}.
		 */
		public void addActionListener(final ActionListener actionListener) {
			listeners.add(actionListener);
		}

		private void fireModelChange() {
			for (final ActionListener listener : listeners) {
				listener.actionPerformed(new ActionEvent(this, (int) (System
						.currentTimeMillis() & 0xffffffff), "modelReplaced"));
			}
		}
	}

	private static final class Line extends JPanel {
		private static final long serialVersionUID = -50401089349519532L;

		private Line(final ColourSelector parent, final StatTypes stat,
				final Iterable<String> parameters) {
			super();
			setLayout(new GridLayout(1, 0));
			add(new JLabel(stat.name()));
			add(new JButton());
			for (final String parameter : parameters) {
				final DoubleValueSelector doubleValueSelector = new DoubleValueSelector();
				doubleValueSelector.setModel(parent.model.getModel(parameter,
						stat));
				doubleValueSelector.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(final ActionEvent e) {
						parent.model.setModel(parameter, stat,
								((DoubleValueSelector) e.getSource()).model);
					}
				});
				add(doubleValueSelector);
			}
		}
	}

	private final JPanel doublePanel;

	/**
	 * Creates a {@link ColourSelector} based on {@code parameters} and
	 * {@code stats}.
	 * 
	 * @param parameters
	 *            Some parameter names.
	 * @param stats
	 *            Some {@link StatTypes}.
	 */
	public ColourSelector(final Iterable<String> parameters,
			final Iterable<StatTypes> stats) {
		super();
		doublePanel = new JPanel();
		doublePanel.setLayout(new GridLayout(0, 1));
		doublePanel.setBorder(new TitledBorder("Colours for heatmap values"));
		update(parameters, stats, Collections
				.<String, Map<StatTypes, Map<RangeType, Double>>> emptyMap());
		add(doublePanel);
	}

	private final ColourModel model = new ColourModel();

	/**
	 * Updates the panel based on {@code parameters} and {@code stats}.
	 * 
	 * @param parameters
	 *            Some parameter names.
	 * @param stats
	 *            Some {@link StatTypes}.
	 * @param ranges
	 *            The ranges of the parameter/statistics.
	 */
	public void update(final Iterable<String> parameters,
			final Iterable<StatTypes> stats,
			final Map<String, Map<StatTypes, Map<RangeType, Double>>> ranges) {
		doublePanel.removeAll();
		final JPanel titles = new JPanel();
		titles.setLayout(new GridLayout(1, 0));
		final Color defaultLowColor = getColour(ColourPreferenceConstants.DOWN_COLOUR);
		final Color defaultMiddleColor = getColour(ColourPreferenceConstants.MIDDLE_COLOUR);
		final Color defaultHighColor = getColour(ColourPreferenceConstants.UP_COLOUR);
		titles.add(new JLabel("Statistics"));
		titles.add(new JLabel(""));
		for (final String parameter : parameters) {
			titles.add(new JLabel(parameter));
		}
		doublePanel.add(titles);
		for (final String parameter : parameters) {
			if (!model.models.containsKey(parameter)) {
				model.models.put(parameter, new EnumMap<StatTypes, Model>(
						StatTypes.class));
			}
			final Map<StatTypes, Model> map = model.models.get(parameter);
			for (final StatTypes stat : stats) {
				if (!map.containsKey(stat)) {
					final Map<StatTypes, Map<RangeType, Double>> possMap = ranges
							.get(parameter);
					if (possMap == null || !possMap.containsKey(stat)) {
						map.put(stat, DEFAULT_MODEL);
					} else {
						final Map<RangeType, Double> rangeMap = possMap
								.get(stat);
						final Double defaultLowValue = rangeMap
								.get(Displayable.Util
										.findByDisplayText(
												ImporterNodePlugin
														.getDefault()
														.getPreferenceStore()
														.getString(
																ColourPreferenceConstants.DOWN_VALUE),
												RangeType.values()));
						final Double defaultMiddleValue = rangeMap
								.get(Displayable.Util
										.findByDisplayText(
												ImporterNodePlugin
														.getDefault()
														.getPreferenceStore()
														.getString(
																ColourPreferenceConstants.MIDDLE_VALUE),
												RangeType.values()));
						final Double defaultHighValue = rangeMap
								.get(Displayable.Util
										.findByDisplayText(
												ImporterNodePlugin
														.getDefault()
														.getPreferenceStore()
														.getString(
																ColourPreferenceConstants.UP_VALUE),
												RangeType.values()));
						map.put(stat, new Model(
								defaultLowValue == null ? DEFAULT_MODEL.downVal
										: defaultLowValue.doubleValue(),
								defaultMiddleValue,
								defaultHighValue == null ? DEFAULT_MODEL.upVal
										: defaultHighValue.doubleValue(),
								defaultLowColor, defaultMiddleColor,
								defaultHighColor));
					}
				}
			}
		}
		for (final StatTypes stat : stats) {
			if (!stat.isDiscrete()) {
				doublePanel.add(new Line(this, stat, parameters));
			}
		}
	}

	/**
	 * @param key
	 *            A {@link String} in <code>\d{1,3},\d{1,3},\d{1,3}</code>
	 *            format. The numbers should be between {@code 0} (inclusive)
	 *            and {@code 255} (inclusive).
	 * @return The colour belonging to {@code key}.
	 * @see ColorSelector#getColorValue()
	 */
	private Color getColour(final String key) {
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

	/**
	 * @return The current {@link ColourModel}.
	 */
	public ColourModel getModel() {
		return model;
	}
}
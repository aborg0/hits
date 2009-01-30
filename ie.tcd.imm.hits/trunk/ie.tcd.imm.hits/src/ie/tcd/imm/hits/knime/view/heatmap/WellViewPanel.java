package ie.tcd.imm.hits.knime.view.heatmap;

import ie.tcd.imm.hits.knime.view.heatmap.ViewModel.ParameterModel;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.util.Collection;
import java.util.List;

import javax.annotation.Nullable;
import javax.swing.JPanel;

import org.knime.base.node.mine.sota.view.interaction.Hiliteable;
import org.knime.core.data.property.ColorAttr;

/**
 * This panel shows the values as colour codes.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
public class WellViewPanel extends JPanel implements Hiliteable,
		MouseMotionListener {
	private static final String[] NO_LABELS = new String[0];
	private static final long serialVersionUID = -5609225166570164016L;
	private final boolean isSelectable;
	private ViewModel model;
	private final Color[] sampleColors = new Color[] {
			Color.BLUE/* new Color(30, 30, 255, 70) */, Color.GREEN,
			Color.RED, Color.ORANGE, Color.PINK, Color.CYAN, Color.MAGENTA };
	private final Color borderColor = Color.BLACK;
	private Color[] colors;
	private String[] labels;
	private boolean hilitedAll;
	private boolean isSelected;
	private final int positionOnPlate;

	/**
	 * Constructs a {@link WellViewPanel} object.
	 * 
	 * @param isSelectable
	 *            If {@code true}, it will be selectable.
	 * @param model
	 *            The model for the layout of the well.
	 * @param positionOnPlate
	 *            A {@code 0}-based position on the plate. It is
	 *            {@code row*12+column}. If may be less then {@code 0}
	 *            indicating that it is not belonging to a plate.
	 */
	public WellViewPanel(final boolean isSelectable, final ViewModel model,
			final int positionOnPlate) {
		super();
		this.isSelectable = isSelectable;
		this.model = model;
		this.positionOnPlate = positionOnPlate;
		this.colors = sampleColors;
		this.labels = NO_LABELS;
		hilitedAll = false;
		isSelected = false;
		addMouseMotionListener(this);
	}

	@Override
	protected void paintComponent(final Graphics g) {
		super.paintComponent(g);
		final Rectangle bounds = getBounds();
		final int radius = Math.min(bounds.width / 2, bounds.height / 2) - 1;
		if (hilitedAll) {
			g.setColor(isSelected ? ColorAttr.SELECTED_HILITE
					: ColorAttr.HILITE);
			g.fillRect(0, 0, bounds.width, bounds.height);
		}
		final List<ParameterModel> primerParameters = model.getMain()
				.getPrimerParameters();
		final Collection<SliderModel> sliders = model.getMain()
				.getArrangementModel().getSliderModels();// .getSliders().get(Type.Splitter);
		final int primerParamCount = Math.max(1, selectValueCount(
				primerParameters, sliders));
		final int secCount = Math.max(1, selectValueCount(model.getMain()
				.getSeconderParameters(), sliders));
		int colorPos = 0;
		switch (model.getShape()) {
		case Circle: {
			final int[] radiuses = getRadiuses(radius, secCount);
			final int additionalParamCount = selectValueCount(model.getMain()
					.getAdditionalParameters(), sliders);
			assert additionalParamCount <= 4 : "Additional parameters: "
					+ additionalParamCount;
			switch (additionalParamCount) {
			case 0:
				break;
			case 1:
				g.setColor(colors[colorPos++]);
				colorPos %= colors.length;
				g.fillRect(bounds.width / 2 - radius, bounds.height / 2
						- radius, 2 * radius, 2 * radius);
				break;
			case 2:
				g.setColor(colors[colorPos++]);
				colorPos %= colors.length;
				g.fillRect(bounds.width / 2 - radius, bounds.height / 2
						- radius, 2 * radius, radius);
				g.setColor(colors[colorPos++]);
				colorPos %= colors.length;
				g.fillRect(bounds.width / 2 - radius, bounds.height / 2,
						2 * radius, radius);
				break;
			case 3:
				g.setColor(colors[colorPos++]);
				colorPos %= colors.length;
				g.fillRect(bounds.width / 2 - radius, bounds.height / 2
						- radius, 2 * radius, radius);
				g.setColor(colors[colorPos++]);
				colorPos %= colors.length;
				g.fillRect(bounds.width / 2 - radius, bounds.height / 2,
						radius, radius);
				g.setColor(colors[colorPos++]);
				colorPos %= colors.length;
				g.fillRect(bounds.width / 2, bounds.height / 2, radius, radius);
				break;
			case 4:
				g.setColor(colors[colorPos++]);
				colorPos %= colors.length;
				g.fillRect(bounds.width / 2 - radius, bounds.height / 2
						- radius, radius, radius);
				g.setColor(colors[colorPos++]);
				colorPos %= colors.length;
				g.fillRect(bounds.width / 2, bounds.height / 2 - radius,
						radius, radius);
				g.setColor(colors[colorPos++]);
				colorPos %= colors.length;
				g.fillRect(bounds.width / 2 - radius, bounds.height / 2,
						radius, radius);
				g.setColor(colors[colorPos++]);
				colorPos %= colors.length;
				g.fillRect(bounds.width / 2, bounds.height / 2, radius, radius);
				break;
			}
			for (int j = 0; j < secCount; ++j) {
				for (int i = 0; i < primerParamCount; ++i) {
					g.setColor(colors[colorPos++]);
					colorPos %= colors.length;
					g.fillArc(bounds.width / 2 - radiuses[j], bounds.height / 2
							- radiuses[j], 2 * radiuses[j], 2 * radiuses[j],
							model.getMain().getStartAngle() + i * 360
									/ primerParamCount, 360 / primerParamCount);
				}
			}
			if (model.getMain().isDrawBorder()) {
				g.setColor(borderColor);
				drawBorderOval(g, bounds, radius);
			}
			if (isSelected) {
				g.setColor(hilitedAll ? ColorAttr.SELECTED_HILITE
						: ColorAttr.SELECTED);
				drawBorderOval(g, bounds, radius);
				drawBorderOval(g, bounds, radius + 1);
				drawBorderOval(g, bounds, radius - 1);
			}
			if (hilitedAll & !isSelected) {
				g.setColor(ColorAttr.HILITE);
				drawBorderOval(g, bounds, radius);
				// drawBorderOval(g, bounds, radius - 1);
				// drawBorderOval(g, bounds, radius + 1);
				// drawBorderOval(g, bounds, radius + 2);
			}
			if (model.getMain().isDrawAdditionalBorders()) {
				g.setColor(borderColor);
				g.drawRect(bounds.width / 2 - (radius + 1), bounds.height / 2
						- (radius + 1), 2 * radius + 1, 2 * radius + 1);
			}
			if (model.getMain().isDrawPrimaryBorders()) {
				g.setColor(borderColor);
				if (primerParamCount > 1) {
					for (int i = primerParamCount; i-- > 0;) {
						final double angle = (model.getMain().getStartAngle() + i
								* 360 / primerParamCount)
								/ 180.0 * Math.PI;
						g.drawLine(bounds.width / 2, bounds.height / 2,
								bounds.width / 2
										+ (int) (Math.cos(angle) * radius),
								bounds.height / 2
										- (int) (Math.sin(angle) * radius));
					}
				}
			}
			if (model.getMain().isDrawSecondaryBorders()) {
				g.setColor(borderColor);
				if (secCount > 0) {
					for (int i = radiuses.length; i-- > 1;) {
						final int r = radiuses[i];
						g.drawOval(bounds.width / 2 - r, bounds.height / 2 - r,
								2 * r, 2 * r);
					}
				}
			}
			break;
		}
		case Rectangle: {
			for (int i = secCount; i-- > 0;) {
				for (int j = primerParamCount; j-- > 0;) {
					g.setColor(colors[(i * primerParamCount + j)
							% colors.length /* colorPos++ */]);
					// colorPos %= colors.length;
					g.fillRect(bounds.width / 2 - radius + j * 2 * radius
							/ primerParamCount, bounds.height / 2 - radius + i
							* 2 * radius / secCount, (int) (radius * 2.0
							/ primerParamCount + .5), (int) (radius * 2.0
							/ secCount + .5));
				}
			}
			if (model.getMain().isDrawBorder()) {
				g.setColor(borderColor);
				drawBorderRect(g, bounds, radius);
			}
			if (isSelected) {
				g.setColor(hilitedAll ? ColorAttr.SELECTED_HILITE
						: ColorAttr.SELECTED);
				drawBorderRect(g, bounds, radius);
				drawBorderRect(g, bounds, radius + 1);
			}
			if (hilitedAll & !isSelected) {
				g.setColor(ColorAttr.HILITE);
				drawBorderRect(g, bounds, radius);
			}
			if (model.getMain().isDrawPrimaryBorders() && primerParamCount > 1) {
				g.setColor(borderColor);
				for (int i = primerParamCount - 1; i-- > 0;) {
					g.drawLine(bounds.width / 2 - radius + (i + 1) * 2 * radius
							/ primerParamCount, bounds.height / 2 - radius,
							bounds.width / 2 - radius + (i + 1) * 2 * radius
									/ primerParamCount, bounds.height / 2
									+ radius);
				}
			}
			if (model.getMain().isDrawSecondaryBorders() && secCount > 1) {
				g.setColor(borderColor);
				for (int j = secCount - 1; j-- > 0;) {
					g.drawLine(bounds.width / 2 - radius, bounds.height / 2
							- radius + (j + 1) * 2 * radius / secCount,
							bounds.width / 2 + radius, bounds.height / 2
									- radius + (j + 1) * 2 * radius / secCount);
				}
			}
			break;
		}
		default:
			throw new UnsupportedOperationException("Not supported yet.");
		}
	}

	private void drawBorderRect(final Graphics g, final Rectangle bounds,
			final int radius) {
		g.drawRect(bounds.width / 2 - radius, bounds.height / 2 - radius,
				radius * 2, radius * 2);
	}

	private void drawBorderOval(final Graphics g, final Rectangle bounds,
			final int radius) {
		g.drawOval(bounds.width / 2 - radius - 1, bounds.height / 2 - radius
				- 1, 2 * (radius + 1), 2 * (radius + 1));
		g.drawOval(bounds.width / 2 - radius, bounds.height / 2 - radius,
				2 * (radius), 2 * (radius));
		// g.drawOval(bounds.width / 2 - radius + 1, bounds.height / 2 - radius
		// + 1, 2 * (radius - 1), 2 * (radius - 1));
	}

	/**
	 * @return The actual {@link ViewModel}.
	 */
	protected ViewModel getModel() {
		return model;
	}

	/**
	 * Sets the current colours to show. This call will repaint the well. Any
	 * change to the {@code colors} array will take affect on next
	 * {@link #repaint()}.
	 * 
	 * @param colors
	 *            Some colours. It should be as many as primary times secondary
	 *            parameter values are.
	 */
	public void setColors(final Color... colors) {
		this.colors = colors == null ? sampleColors : colors;
		repaint();
	}

	/**
	 * Sets the labels for the different parts of well representation.
	 * (Currently only the first is used if set.)
	 * 
	 * @param labels
	 *            The new labels. Any change on these has affect later.
	 */
	public void setLabels(@Nullable
	final String... labels) {
		this.labels = labels == null ? NO_LABELS : labels;
	}

	/**
	 * This method computes the radiuses of the circles with approximately the
	 * same areas for the rings.
	 * 
	 * @param radius
	 *            The radius of outermost circle.
	 * @param count
	 *            This many circles will be. This must be strictly larger than
	 *            {@code 0}.
	 * @return The radiuses for the well circles. ({@code count} length)
	 */
	protected static int[] getRadiuses(final int radius, final int count) {
		final int[] ret = new int[count];
		double prev = radius / Math.sqrt(count);
		ret[count - 1] = (int) (prev);
		for (int i = count - 1; i-- > 1;) {
			prev = Math.sqrt(prev * prev + (double) radius * radius / count);
			ret[i] = (int) prev;
		}
		ret[0] = radius;
		return ret;
	}

	/**
	 * Computes how many different values have to be represented in a single
	 * well by the primary or the secondary parameters.
	 * 
	 * @param parameters
	 *            The parameters of the splits.
	 * @param sliders
	 *            The {@link SliderModel}s splitting the wells.
	 * @return The number of different values to represent.
	 */
	protected static int selectValueCount(
			final List<ParameterModel> parameters,
			final Collection<SliderModel> sliders) {
		int ret = 0;
		SliderModel current = null;
		for (final SliderModel slider : sliders) {
			if (slider.getParameters().equals(parameters)) {
				current = slider;
				break;
			}
		}
		for (final ParameterModel parameterModel : parameters) {
			ret += parameterModel.getAggregateType() == null ? (current == null ? (parameterModel
					.getColorLegend().size() > 0 ? parameterModel
					.getColorLegend().size() : 1)
					: current.getSelections().size())
					: 1;
		}
		return ret;
	}

	/**
	 * Sets a new {@link ViewModel}, {@code model}.
	 * 
	 * @param model
	 */
	public void setModel(final ViewModel model) {
		this.model = model;
		repaint();
	}

	@Override
	public boolean isHilited() {
		return hilitedAll;
	}

	@Override
	public void setHilited(final boolean hilit) {
		hilitedAll = hilit;
		repaint();
	}

	@Override
	public void mouseDragged(final MouseEvent e) {
		// TODO Auto-generated method stub
		// Currently do nothing.
	}

	@Override
	public void mouseMoved(final MouseEvent e) {
		// TODO Auto-generated method stub
		assert e.getSource() == this;
		if (labels.length > 0) {
			setToolTipText(labels[0]);
		}
		final Point point = e.getPoint();
	}

	/**
	 * If {@link #isSelectable} selects or deselects the well, and
	 * {@link #repaint()}s it.
	 * 
	 * @param select
	 *            If the well is {@link #isSelectable} and this value is
	 *            {@code true} and , than it will be selected, else if still
	 *            {@link #isSelectable}, but the value is {@code false} if will
	 *            be deselected.
	 */
	public void setSelected(final boolean select) {
		if (isSelectable) {
			isSelected = select;
			repaint();
		}
	}

	/**
	 * @return If and only if {@code true} this well is selected.
	 */
	public boolean isSelected() {
		return isSelected;
	}

	/**
	 * @return The position on plate.
	 */
	public int getPositionOnPlate() {
		return positionOnPlate;
	}

	@Override
	public String toString() {
		return (isSelected ? "[x]" : "[ ]") + (isHilited() ? "[[x]]" : "[[ ]]")
				+ " " + positionOnPlate;
	}
}
/**
 * 
 */
package ie.tcd.imm.hits.knime.view.heatmap;

import ie.tcd.imm.hits.knime.view.heatmap.ViewModel.ParameterModel;
import ie.tcd.imm.hits.knime.view.heatmap.ViewModel.OverviewModel.Places;
import ie.tcd.imm.hits.util.Pair;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * This panel shows the legend of the heatmap's circles/rectangles.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
public class LegendPanel extends JPanel implements ActionListener {
	/**
	 * This panel shows the layout of different parameters.
	 */
	public static class ParametersPanel extends JPanel {
		private static final long serialVersionUID = -2271852289205726654L;
		private final boolean isSelectable;
		private ViewModel model;

		private final JLabel label = new JLabel();
		private final Set<ActionListener> listeners = new HashSet<ActionListener>();
		private final Places places;

		/**
		 * Constructs a {@link ParametersPanel}.
		 * 
		 * @param isSelectable
		 *            The value of selectable property
		 * @param model
		 *            The model of the parameters
		 * @param horizontal
		 *            The {@link ParameterModel} is horizontal or vertical
		 * @param places
		 *            This is where it will be placed.
		 */
		public ParametersPanel(final boolean isSelectable,
				final ViewModel model, final boolean horizontal,
				final Places places) {
			super();
			this.isSelectable = isSelectable;
			this.model = model;
			// model.getOverviewModel().
			this.places = places;
			internalSetModel();
			add(label);
		}

		/**
		 * Sets and updates the model.
		 * 
		 * @param model
		 *            The new {@link ViewModel}.
		 */
		public void setModel(final ViewModel model) {
			this.model = model;
			internalSetModel();
		}

		private void internalSetModel() {
			switch (places) {
			case Choices:
				final List<ParameterModel> choiceModel = model.getOverview()
						.getChoiceModel();
				setText(choiceModel);
				break;
			case Columns:
				setText(model.getOverview().getRowModel());
				break;
			case Rows:
				setText(model.getOverview().getColModel());
				break;
			default:
				break;
			}
			repaint();
		}

		private void setText(final List<ParameterModel> paramModel) {
			if (paramModel.isEmpty()) {
				label.setVisible(false);
			} else {
				label.setVisible(true);
				final StringBuilder sb = new StringBuilder();
				for (final ParameterModel parameterModel : paramModel) {
					sb.append(parameterModel.getShortName()).append(" | ");
				}
				sb.setLength(sb.length() - 3);
				label.setText(sb.toString());
			}
		}

		/**
		 * Adds an {@link ActionListener} if {@link #isSelectable selectable}.
		 * 
		 * @param listener
		 *            An {@link ActionListener}.
		 * @return If not added {@code true}, else {@code false}.
		 */
		public boolean addActionListener(final ActionListener listener) {
			if (isSelectable) {
				return listeners.add(listener);
			}
			return false;
		}

		/**
		 * Removes {@code listener} if {@link #isSelectable selectable}.
		 * 
		 * @param listener
		 *            An {@link ActionListener}.
		 * @return If previously contained {@code true}, else {@code false}.
		 */
		public boolean removeActionListener(final ActionListener listener) {
			if (isSelectable) {
				return listeners.remove(listener);
			}
			return false;
		}
		//
		// private void actionPerformed() {
		// for (final ActionListener listener : listeners) {
		// listener.actionPerformed(new ActionEvent(this, (int) (System
		// .currentTimeMillis() & 0xffffffffL), ""));
		// }
		// }
	}

	private static final long serialVersionUID = 9129522729091802767L;
	private ViewModel model;
	private final LayoutLegendPanel layoutLegendPanel;

	/**
	 * This class shows a legend of a sample well.
	 */
	private static class ShapeLegendPanel extends WellViewPanel {
		private static final long serialVersionUID = -1068470342510750276L;

		private boolean showColors = true;
		private boolean showLabels = true;

		private final Color borderColor = Color.BLACK;

		/**
		 * Constructs a {@link ShapeLegendPanel}.
		 * 
		 * @param isSelectable
		 *            The parts are selectable or not.
		 * @param model
		 *            The {@link ViewModel} to influence the view of the sample.
		 */
		public ShapeLegendPanel(final boolean isSelectable,
				final ViewModel model) {
			super(isSelectable, model, -1);
		}

		/**
		 * Draws a sample node, and some additional informations, like labels,
		 * colour ranges.
		 */
		@Override
		protected void paintComponent(final Graphics g) {
			super.paintComponent(g);
			final Rectangle bounds = getBounds();
			final int radius = Math.min(bounds.width, bounds.height) / 2;
			final Collection<SliderModel> sliders = getModel().getMain()
					.getArrangementModel().getSliderModels();// .getSliders().get(Type.Splitter);
			final int primaryCount = selectValueCount(getModel().getMain()
					.getPrimerParameters(), sliders);
			final int secundaryCount = selectValueCount(getModel().getMain()
					.getSeconderParameters(), sliders);
			switch (getModel().getShape()) {
			case Circle:
				if (showLabels) {
					final int startAngle = getModel().getMain().getStartAngle();
					int angle = startAngle;
					if (primaryCount == 0) {
						return;
					}
					g.setFont(g.getFont().deriveFont(15.0f));
					angle += 180 / primaryCount;
					for (final ParameterModel model : getModel().getMain()
							.getPrimerParameters()) {
						final SliderModel currentSlider = getCurrentSlider(
								sliders, model);
						if (currentSlider != null) {
							for (final Entry<Integer, Pair<ParameterModel, Object>> entry : currentSlider
									.getValueMapping().entrySet()) {
								if (!currentSlider.getSelections().contains(
										entry.getKey())) {
									continue;
								}
								g.setColor(Color.WHITE);
								final Font origFont = g.getFont();
								g.setFont(origFont.deriveFont(Font.BOLD));
								g.setColor(borderColor);
								g
										.drawString(
												model.getShortName(),
												bounds.width
														/ 2
														- 40
														+ (int) (Math.cos(angle
																/ 180.0
																* Math.PI) * 60),
												bounds.height
														/ 2
														- (int) (Math.sin(angle
																/ 180.0
																* Math.PI) * 60)/*-bounds.width / 4, bounds.height / 4*/);
								g
										.drawString(
												entry.getValue().getRight()
														.toString(),
												bounds.width
														/ 2
														- 40
														+ (int) (Math.cos(angle
																/ 180.0
																* Math.PI) * 60),
												bounds.height
														/ 2
														- (int) (Math.sin(angle
																/ 180.0
																* Math.PI) * 60 - 15)/*-bounds.width / 4, bounds.height / 4*/);
								g.setFont(origFont);
								angle += 360 / primaryCount;
							}
						}
					}
					((Graphics2D) g).rotate(-startAngle / 180.0 * Math.PI);
					g.setFont(getFont().deriveFont(Font.BOLD));
					final List<ParameterModel> secunderParameters = getModel()
							.getMain().getSeconderParameters();
					if (secunderParameters.size() > 0) {
						final ParameterModel paramModel = secunderParameters
								.iterator().next();
						g.drawString(paramModel.getShortName(), radius / 2,
								(int) (radius * 1.2));
						final SliderModel slider = getCurrentSlider(sliders,
								paramModel);
						final Map<Integer, Pair<ParameterModel, Object>> valueMapping = slider
								.getValueMapping();
						final int[] radiuses = getRadiuses(radius, slider
								.getSelections().size());
						int i = 0;
						for (final Entry<Integer, Pair<ParameterModel, Object>> entry : valueMapping
								.entrySet()) {
							if (slider.getSelections().contains(entry.getKey())) {
								g.drawString(entry.getValue().getRight()
										.toString(), radius / 2 - 30
										+ radiuses[i], (int) (radius * 1.35)
										+ (i % 2) * 15);
								++i;
							}
						}
					}
					((Graphics2D) g).rotate(startAngle / 180.0 * Math.PI);
				}
				break;
			case Rectangle:
				if (showLabels) {
					final Font origFont = g.getFont();
					g.setFont(origFont.deriveFont(Font.BOLD));
					g.setColor(borderColor);
					for (final ParameterModel model : getModel().getMain()
							.getPrimerParameters()) {
						final SliderModel slider = getCurrentSlider(sliders,
								model);
						g.drawString(model.getShortName(), 10, 15);
						int i = 0;
						for (final Entry<Integer, Pair<ParameterModel, Object>> entry : slider
								.getValueMapping().entrySet()) {
							if (slider.getSelections().contains(entry.getKey())) {
								g.drawString(entry.getValue().getRight()
										.toString(), i * bounds.width
										/ primaryCount, 30 + (i % 2) * 15);
								++i;
							}
						}
					}
					((Graphics2D) g).rotate(-Math.PI / 2);
					for (final ParameterModel model : getModel().getMain()
							.getSeconderParameters()) {
						g.drawString(model.getShortName(), -bounds.height + 5,
								15);
						int i = 0;
						final SliderModel slider = getCurrentSlider(sliders,
								model);
						for (final Entry<Integer, Pair<ParameterModel, Object>> entry : slider
								.getValueMapping().entrySet()) {
							if (slider.getSelections().contains(entry.getKey())) {
								g.drawString(entry.getValue().getRight()
										.toString(), 5 - (i + 1) * bounds.width
										/ secundaryCount, 30 + (i % 2) * 15);
								++i;
							}
						}
					}
					g.setFont(origFont);
				}
				break;
			default:
				break;
			}
		}

		private SliderModel getCurrentSlider(
				final Collection<SliderModel> sliders,
				final ParameterModel model) {
			SliderModel currentSlider = null;
			for (final SliderModel slider : sliders) {
				for (final ParameterModel pm : slider.getParameters()) {
					if (pm.equals(model)) {
						currentSlider = slider;
					}
				}
			}
			return currentSlider;
		}

		/**
		 * @param showLabels
		 *            Changes the labels' visibility.
		 */
		public void setShowLabels(final boolean showLabels) {
			this.showLabels = showLabels;
			repaint();
		}

		/**
		 * @param showColors
		 *            Changes the colourmaps' visibility.
		 */
		public void setShowColors(final boolean showColors) {
			this.showColors = showColors;
			repaint();
		}
	}

	/**
	 * This panel shows the layout of the {@link SliderModel}s.
	 */
	private static class LayoutLegendPanel extends JPanel {
		private static final long serialVersionUID = 4132948016212511178L;

		private final ParametersPanel horizontalParametersPanel;
		private final ParametersPanel verticalParametersPanel;
		private final ParametersPanel choiceParametersPanel;
		private final ShapeLegendPanel shapeLegendPanel;

		/**
		 * Constructs a {@link LayoutLegendPanel}.
		 * 
		 * @param isSelectable
		 *            The parameters will be selectable or not depending on the
		 *            value of this parameter.
		 * @param model
		 *            The {@link ViewModel} for the {@link ParameterModel}s.
		 */
		LayoutLegendPanel(final boolean isSelectable, final ViewModel model) {
			super();
			horizontalParametersPanel = new ParametersPanel(isSelectable,
					model, true, ViewModel.OverviewModel.Places.Columns);
			verticalParametersPanel = new ParametersPanel(isSelectable, model,
					false, ViewModel.OverviewModel.Places.Rows);
			choiceParametersPanel = new ParametersPanel(isSelectable, model,
					true, ViewModel.OverviewModel.Places.Choices);
			shapeLegendPanel = new ShapeLegendPanel(isSelectable, model);
			shapeLegendPanel.setPreferredSize(new Dimension(200, 200));
			final BorderLayout layout = new BorderLayout();
			setLayout(layout);
			add(horizontalParametersPanel, BorderLayout.SOUTH);
			add(verticalParametersPanel, BorderLayout.EAST);
			add(choiceParametersPanel, BorderLayout.NORTH);
			add(shapeLegendPanel, BorderLayout.CENTER);
		}

		/**
		 * Updates the samples.
		 * 
		 * @param model
		 *            The new {@link ViewModel}.
		 */
		public void setModel(final ViewModel model) {
			horizontalParametersPanel.setModel(model);
			verticalParametersPanel.setModel(model);
			choiceParametersPanel.setModel(model);
			shapeLegendPanel.setModel(model);
		}
	}

	/**
	 * Constructs a new {@link LegendPanel}.
	 * 
	 * @param isSelectable
	 *            The {@link LayoutLegendPanel} will be selectable or not.
	 * @param model
	 *            The {@link ViewModel} for this sample.
	 */
	public LegendPanel(final boolean isSelectable, final ViewModel model) {
		super();
		this.model = model;
		layoutLegendPanel = new LayoutLegendPanel(isSelectable, model);
		add(layoutLegendPanel);
	}

	@Override
	public void actionPerformed(final ActionEvent e) {
		revalidate();
		repaint();
	}

	/**
	 * Updates the sample using {@code currentViewModel}.
	 * 
	 * @param currentViewModel
	 *            This holds the new parameters to visualize the sample.
	 */
	public void setViewModel(final ViewModel currentViewModel) {
		model.getMain().getArrangementModel().removeListener(this);
		model = currentViewModel;
		layoutLegendPanel.setModel(model);
		model.getMain().getArrangementModel().addListener(this);
		repaint();
	}
}

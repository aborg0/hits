/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package ie.tcd.imm.hits.knime.view.heatmap;

import ie.tcd.imm.hits.knime.view.StatTypes;
import ie.tcd.imm.hits.knime.view.heatmap.HeatmapNodeView.VolatileModel;
import ie.tcd.imm.hits.knime.view.heatmap.ViewModel.ParameterModel;
import ie.tcd.imm.hits.knime.view.heatmap.ViewModel.OverviewModel.Places;
import ie.tcd.imm.hits.util.select.Selectable;
import ie.tcd.imm.hits.util.swing.colour.ColourComputer;
import ie.tcd.imm.hits.util.swing.colour.ColourFactory;
import ie.tcd.imm.hits.util.swing.colour.ColourLegend;
import ie.tcd.imm.hits.util.swing.colour.FactoryRegistry;
import ie.tcd.imm.hits.util.swing.colour.Orientation;
import ie.tcd.imm.hits.util.swing.colour.ColourSelector.ColourModel;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.knime.core.util.Pair;

/**
 * This panel shows the legend of the heatmap's circles/rectangles.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
public class LegendPanel extends JPanel implements ActionListener {
	private static final long serialVersionUID = 9129522729091802767L;
	private ViewModel model;
	private final LayoutLegendPanel layoutLegendPanel;
	private boolean showColors = true;

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
				// final List<ParameterModel> choiceModel = model.getOverview()
				// .getChoiceModel();
				// setText(choiceModel);
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
	}

	/**
	 * This class shows a legend of a sample well.
	 */
	private static class ShapeLegendPanel extends WellViewPanel {
		private static final long serialVersionUID = -1068470342510750276L;

		private boolean showLabels = true;

		private final Color borderColor = Color.BLACK;

		private BufferedImage image;

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
			addComponentListener(new ComponentAdapter() {
				@Override
				public void componentResized(final ComponentEvent e) {
					updateImage();
				}

			});
			addMouseMotionListener(new MouseAdapter() {
				@Override
				public void mouseMoved(final MouseEvent e) {
					final Collection<SliderModel> sliders = getModel()
							.getMain().getArrangementModel().getSliderModels();
					final List<ParameterModel> seconderParameters = getModel()
							.getMain().getSeconderParameters();
					int secondaryCount = WellViewPanel.selectValueCount(
							seconderParameters, sliders);
					secondaryCount = secondaryCount == 0 ? 1 : secondaryCount;
					final int index = image.getRGB(e.getX(), e.getY()) - 1 & 0x00ffffff;
					final int prim = index / secondaryCount;
					final int sec = index == (-1 & 0xffffff) ? -1 : index
							% secondaryCount;
					final List<ParameterModel> primerParameters = getModel()
							.getMain().getPrimerParameters();
					final ParameterModel primary = primerParameters.isEmpty() ? null
							: primerParameters.iterator().next();
					final ParameterModel secondary = seconderParameters
							.isEmpty() ? null : seconderParameters.iterator()
							.next();
					final Selectable<Pair<ParameterModel, Object>> primSlider = LegendPanel
							.getCurrentSlider(sliders, primary);
					final Selectable<Pair<ParameterModel, Object>> secSlider = LegendPanel
							.getCurrentSlider(sliders, secondary);
					final Pair<ParameterModel, Object> primPair = selectPair(
							prim, primSlider);
					final Pair<ParameterModel, Object> secPair = selectPair(
							sec, secSlider);
					setToolTipText("<html>"
							+ prettyPrint(primPair)
							+ (secPair == null ? "" : "<br>"
									+ prettyPrint(secPair)) + "</html>");
				}

				/**
				 * @param pair
				 *            A {@link Pair} of {@link ParameterModel} and
				 *            something else.
				 * @return Selects the short name from the
				 *         {@link ParameterModel}, and appends the
				 *         {@link String} representation of the associated
				 *         object.
				 */
				private String prettyPrint(
						final Pair<ParameterModel, Object> pair) {
					return pair == null ? "" : (pair.getFirst() == null ? ""
							: pair.getFirst().getShortName())
							+ ": "
							+ (pair.getSecond() == null ? "" : pair.getSecond()
									.toString());
				}

				/**
				 * Selects the {@code index}<sup>th</sup> {@link ParameterModel}
				 * from {@code slider} if available.
				 * 
				 * @param index
				 *            The ({@code 0}-based) index in the {@code slider}.
				 * @param slider
				 *            A {@link SliderModel}.
				 * @return The found {@link ParameterModel} with the associated
				 *         value. {@code null} if not found.
				 */
				@Nullable
				private Pair<ParameterModel, Object> selectPair(
						final int index,
						final Selectable<Pair<ParameterModel, Object>> slider) {
					if (slider == null) {
						return null;
					}
					int i = 0;
					for (final Entry<Integer, Pair<ParameterModel, Object>> entry : slider
							.getValueMapping().entrySet()) {
						if (slider.getSelections().contains(entry.getKey())) {
							if (i++ == index) {
								return entry.getValue();
							}
						}
					}
					return null;
				}
			});
		}

		private void updateImage() {
			if (getSize().height == 0 || getSize().width == 0) {
				return;
			}
			image = new BufferedImage(getSize().width, getSize().height,
					BufferedImage.TYPE_INT_ARGB);
			final Color[] origColors = getColors();
			final boolean origShowLabels = showLabels;
			final Collection<SliderModel> sliders = getModel().getMain()
					.getArrangementModel().getSliderModels();
			final int primaryCount = Math.max(1, WellViewPanel
					.selectValueCount(getModel().getMain()
							.getPrimerParameters(), sliders));
			final int secondaryCount = Math.max(1, WellViewPanel
					.selectValueCount(getModel().getMain()
							.getSeconderParameters(), sliders));
			final Color[] tmpColors = new Color[primaryCount * secondaryCount];
			for (int i = primaryCount; i-- > 0;) {
				for (int j = secondaryCount; j-- > 0;) {
					final int index = i * secondaryCount + j;
					tmpColors[j * primaryCount + i] = new Color(index + 1);
				}
			}
			showLabels = false;
			setColorsButNotRepaint(tmpColors);
			paintComponent(image.getGraphics());
			showLabels = origShowLabels;
			setColorsButNotRepaint(origColors);
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
					.getArrangementModel().getSliderModels();
			final int primaryCount = WellViewPanel.selectValueCount(getModel()
					.getMain().getPrimerParameters(), sliders);
			final int secondaryCount = WellViewPanel.selectValueCount(
					getModel().getMain().getSeconderParameters(), sliders);
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
						final Selectable<Pair<ParameterModel, Object>> currentSlider = LegendPanel
								.getCurrentSlider(sliders, model);
						if (currentSlider != null) {
							for (final Entry<Integer, Pair<ParameterModel, Object>> entry : currentSlider
									.getValueMapping().entrySet()) {
								if (!currentSlider.getSelections().contains(
										entry.getKey())) {
									continue;
								}
								g.setColor(Color.WHITE);
								final Font origFont = g.getFont();
								g.setFont(origFont.deriveFont(Font.BOLD)
										.deriveFont(8.5f));
								g.setColor(borderColor);
								g
										.drawString(model.getShortName(),
												bounds.width
														/ 2
														- 25
														+ (int) (Math.cos(angle
																/ 180.0
																* Math.PI)
																* radius * .8),
												bounds.height
														/ 2
														- (int) (Math.sin(angle
																/ 180.0
																* Math.PI)
																* radius * .8)/*-bounds.width / 4, bounds.height / 4*/);
								g
										.drawString(
												entry.getValue().getSecond()
														.toString(),
												bounds.width
														/ 2
														- 25
														+ (int) (Math.cos(angle
																/ 180.0
																* Math.PI)
																* radius * .8),
												bounds.height
														/ 2
														- (int) (Math.sin(angle
																/ 180.0
																* Math.PI)
																* radius * .8 - 15)/*-bounds.width / 4, bounds.height / 4*/);
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
						final Selectable<Pair<ParameterModel, Object>> slider = LegendPanel
								.getCurrentSlider(sliders, paramModel);
						if (slider == null) {
							return;
						}
						final Map<Integer, Pair<ParameterModel, Object>> valueMapping = slider
								.getValueMapping();
						final int[] radiuses = WellViewPanel.getRadiuses(
								radius, slider.getSelections().size());
						int i = 0;
						for (final Entry<Integer, Pair<ParameterModel, Object>> entry : valueMapping
								.entrySet()) {
							if (slider.getSelections().contains(entry.getKey())) {
								g.drawString(entry.getValue().getSecond()
										.toString(), radius / 2 - 30
										+ radiuses[i], (int) (radius * 1.35)
										+ i % 2 * 15);
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
						final Selectable<Pair<ParameterModel, Object>> slider = LegendPanel
								.getCurrentSlider(sliders, model);
						g.drawString(model.getShortName(), 10, 15);
						int i = 0;
						for (final Entry<Integer, Pair<ParameterModel, Object>> entry : slider
								.getValueMapping().entrySet()) {
							if (slider.getSelections().contains(entry.getKey())) {
								g.drawString(entry.getValue().getSecond()
										.toString(), i * bounds.width
										/ primaryCount, 30 + i % 2 * 15);
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
						final Selectable<Pair<ParameterModel, Object>> slider = LegendPanel
								.getCurrentSlider(sliders, model);
						for (final Entry<Integer, Pair<ParameterModel, Object>> entry : slider
								.getValueMapping().entrySet()) {
							if (slider.getSelections().contains(entry.getKey())) {
								g.drawString(entry.getValue().getSecond()
										.toString(), 5 - (i + 1) * bounds.width
										/ secondaryCount, 30 + i % 2 * 15);
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

		// /**
		// * @param showLabels
		// * Changes the labels' visibility.
		// */
		// public void setShowLabels(final boolean showLabels) {
		// this.showLabels = showLabels;
		// repaint();
		// }

		@Override
		public void setModel(final ViewModel model) {
			super.setModel(model);
			updateImage();
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
			horizontalParametersPanel.setOpaque(false);
			verticalParametersPanel = new ParametersPanel(isSelectable, model,
					false, ViewModel.OverviewModel.Places.Rows);
			verticalParametersPanel.setOpaque(false);
			choiceParametersPanel = new ParametersPanel(isSelectable, model,
					true, ViewModel.OverviewModel.Places.Choices);
			choiceParametersPanel.setOpaque(false);
			shapeLegendPanel = new ShapeLegendPanel(isSelectable, model);
			shapeLegendPanel.setOpaque(false);
			shapeLegendPanel.setPreferredSize(new Dimension(250, 250));
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
		layoutLegendPanel.setOpaque(false);
		setLayout(new LayoutManager() {
			private final Pattern pattern = Pattern.compile("(\\d+)_(\\d+)");

			@Override
			public void removeLayoutComponent(final Component comp) {
				// No need to change.
			}

			@Override
			public Dimension preferredLayoutSize(final Container parent) {
				return new Dimension(250, 250);
			}

			@Override
			public Dimension minimumLayoutSize(final Container parent) {
				return new Dimension(200, 200);
			}

			@Override
			public void layoutContainer(final Container parent) {
				// TODO Auto-generated method stub
				// Do nothing?
				// parent.setBounds(11, 11, 200, 200);
			}

			@Override
			public void addLayoutComponent(final String name,
					final Component comp) {
				if (name != null) {
					final Matcher matcher = pattern.matcher(name);
					if (matcher.matches() && showColors) {
						final int radius = Math.min(getWidth(), getHeight()) / 2;
						final Set<SliderModel> sliders = LegendPanel.this.model
								.getMain().getArrangementModel()
								.getSliderModels();
						final int primaryCount = SliderModel.findSlider(
								sliders,
								LegendPanel.this.model.getMain()
										.getPrimerParameters().iterator()
										.next().getType()).getSelections()
								.size();
						final int secondaryCount = LegendPanel.this.model
								.getMain().getSeconderParameters().isEmpty() ? 1
								: SliderModel.findSlider(
										sliders,
										LegendPanel.this.model.getMain()
												.getSeconderParameters()
												.iterator().next().getType())
										.getSelections().size();
						switch (LegendPanel.this.model.getShape()) {
						case Circle: {
							int angle = LegendPanel.this.model.getMain()
									.getStartAngle();
							angle += 180 / primaryCount;
							angle += 360 * Integer.parseInt(matcher.group(1))
									/ primaryCount;
							final int second = Integer.parseInt(matcher
									.group(2)) + 1;
							final int x = 100 + (int) (Math.cos(angle / 180.0
									* Math.PI)
									* radius / second * .85);

							final int y = 100 + 15 - (int) (Math.sin(angle
									/ 180.0 * Math.PI)
									* radius / second * .89);
							final Orientation orientation = Orientation
									.values()[(angle - 45) / 90 % 4];
							final int width = !orientation.isVertical() ? comp
									.getPreferredSize().width : comp
									.getPreferredSize().width - 0;
							final int height = !orientation.isVertical() ? comp
									.getPreferredSize().height - 15 : comp
									.getPreferredSize().height;
							comp.setBounds(Math.max(0, Math.min(x, getWidth()
									- width)), Math.max(0, Math.min(y,
									getHeight() - height)), width, height);
							comp.setPreferredSize(new Dimension(width, height));
							if (comp instanceof ColourLegend<?>) {
								@SuppressWarnings("unchecked")
								final ColourLegend<ColourComputer> sample = (ColourLegend<ColourComputer>) comp;
								sample.setModel(sample.getModel(), orientation);
							}
							break;
						}
						case Rectangle: {
							if (comp instanceof ColourLegend<?>) {
								@SuppressWarnings("unchecked")
								final ColourLegend<ColourComputer> sample = (ColourLegend<ColourComputer>) comp;
								final int colWidth = layoutLegendPanel
										.getWidth()
										/ primaryCount;
								final int colHeight = layoutLegendPanel
										.getHeight()
										/ secondaryCount;
								final boolean alternate = colWidth < comp
										.getPreferredSize().width;
								final int idx = Integer.parseInt(matcher
										.group(1));
								final int idx2 = Integer.parseInt(matcher
										.group(2));
								final boolean south = alternate && idx % 2 != 0;
								final int x = idx != 0 ? idx * colWidth + 40
										: 0;
								final int y = idx != 0 ? (south && idx2 == 0 ? getHeight()
										- comp.getPreferredSize().height
										* 2
										/ 3
										: 10)
										+ (idx2 > 0 ? idx2 * colHeight : 0)
										: 40 + idx2 * Math.max(30, colHeight);
								final int width = idx == 0 ? 40 : Math.max(45,
										colWidth - 5);
								final int height = idx == 0 ? Math.max(30,
										colHeight - 3) : 40;
								comp.setBounds(Math.max(0, Math.min(x,
										getWidth() - width)), Math.max(0, Math
										.min(y, getHeight() - height)), width,
										height);
								sample.setModel(sample.getModel(),
										idx != 0 ? south ? Orientation.South
												: Orientation.North
												: Orientation.West);
								comp.setPreferredSize(new Dimension(Math.max(
										45, colWidth - 5), 40));
							}
							break;
						}
						}
					} else {
						switch (LegendPanel.this.model.getShape()) {
						case Circle:
							comp.setBounds(40, 40,
									comp.getPreferredSize().width - 80, comp
											.getPreferredSize().height - 80);
							break;
						case Rectangle:
							// comp.setBounds(40, 40,
							// comp.getPreferredSize().width - 80, comp
							// .getPreferredSize().height - 80);
							// comp.setBounds(0, 50, getWidth(), getHeight() -
							// 50);
							break;
						default:
							break;
						}
					}
				} else {
					switch (LegendPanel.this.model.getShape()) {
					case Circle:
						comp.setBounds(0, 0, comp.getPreferredSize().width,
								comp.getPreferredSize().height);
						// comp.setBounds(80, 60,
						// comp.getPreferredSize().width - 80, comp
						// .getPreferredSize().height - 80);
						break;
					case Rectangle:
						comp.setBounds(0, 50, getWidth(), getHeight() - 50);
						break;
					default:
						break;
					}
				}
			}
		});
		add("legend", layoutLegendPanel);
	}

	/**
	 * @param showColors
	 *            Changes the colourmaps' visibility.
	 */
	public void setShowColors(final boolean showColors) {
		this.showColors = showColors;
		repaint();
	}

	@Override
	public void actionPerformed(final ActionEvent e) {
		if (e.getActionCommand().startsWith(
				VolatileModel.SHOW_COLOUR_LEGEND_PREFIX)) {
			setShowColors(Boolean.parseBoolean(e.getActionCommand().substring(
					VolatileModel.SHOW_COLOUR_LEGEND_PREFIX.length())));
		}
		setViewModel(model);
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
		for (final Component component : getComponents()) {
			if (component instanceof ColourLegend<?>) {
				remove(component);
			}
		}
		remove(layoutLegendPanel);
		if (showColors) {
			try {
				final Set<SliderModel> sliders = model.getMain()
						.getArrangementModel().getSliderModels();
				final ParameterModel primary = model.getMain()
						.getPrimerParameters().iterator().next();
				final Selectable<Pair<ParameterModel, Object>> primarySlider = SliderModel
						.findSlider(sliders, primary.getType());
				final ParameterModel secondary = model.getMain()
						.getSeconderParameters().size() == 0 ? null : model
						.getMain().getSeconderParameters().iterator().next();
				final Selectable<Pair<ParameterModel, Object>> secondarySlider = SliderModel
						.findSlider(sliders,
								secondary == null ? StatTypes.experimentName
										: secondary.getType());
				final Selectable<Pair<ParameterModel, Object>> statSlider = SliderModel
						.findSlider(sliders, StatTypes.metaStatType);
				final Selectable<Pair<ParameterModel, Object>> paramSlider = SliderModel
						.findSlider(sliders, StatTypes.parameter);
				final ColourModel cm = currentViewModel.getMain()
						.getColourModel();
				switch (primary.getType()) {
				case metaStatType:
					switch (secondary == null ? StatTypes.experimentName
							: secondary.getType()) {
					case parameter: {
						int i = 0;
						for (final Integer statSelect : primarySlider
								.getSelections()) {
							final StatTypes stat = (StatTypes) primarySlider
									.getValueMapping().get(statSelect)
									.getSecond();
							int j = 0;
							for (final Integer paramSelect : secondarySlider
									.getSelections()) {
								final String param = (String) secondarySlider
										.getValueMapping().get(paramSelect)
										.getSecond();
								addSample(cm, i, j++, stat, param);
							}
							++i;
						}
						break;
					}
					default: {
						final Set<Integer> paramSelect = paramSlider
								.getSelections();
						assert paramSelect.size() == 1;
						final String param = (String) paramSlider
								.getValueMapping().get(
										paramSelect.iterator().next())
								.getSecond();
						int i = 0;
						for (final Integer primSelect : primarySlider
								.getSelections()) {
							final StatTypes stat = (StatTypes) primarySlider
									.getValueMapping().get(primSelect)
									.getSecond();
							addSample(cm, i++, 0, stat, param);
						}
						break;
					}
					}
					break;
				case parameter:
					switch (secondary == null ? StatTypes.experimentName
							: secondary.getType()) {
					case metaStatType: {
						int i = 0;
						for (final Integer paramSelect : primarySlider
								.getSelections()) {
							final String param = (String) primarySlider
									.getValueMapping().get(paramSelect)
									.getSecond();
							int j = 0;
							for (final Integer statSelect : secondarySlider
									.getSelections()) {
								final StatTypes stat = (StatTypes) secondarySlider
										.getValueMapping().get(statSelect)
										.getSecond();
								addSample(cm, i, j++, stat, param);
							}
							++i;
						}
						break;
					}
					default: {
						final Set<Integer> statSelects = statSlider
								.getSelections();
						assert statSelects.size() == 1;
						int i = 0;
						final StatTypes stat = (StatTypes) statSlider
								.getValueMapping().get(
										statSelects.iterator().next())
								.getSecond();
						for (final Integer primSelect : primarySlider
								.getSelections()) {
							final String param = (String) primarySlider
									.getValueMapping().get(primSelect)
									.getSecond();
							addSample(cm, i++, 0, stat, param);
						}
						break;
					}
					}
				default:
					switch (secondary == null ? StatTypes.experimentName
							: secondary.getType()) {
					case metaStatType: {
						final String param = (String) paramSlider
								.getValueMapping().get(
										paramSlider.getSelections().iterator()
												.next()).getSecond();
						int j = 0;
						for (final Integer select : statSlider.getSelections()) {
							final StatTypes stat = (StatTypes) statSlider
									.getValueMapping().get(select).getSecond();
							addSample(cm, 0, j++, stat, param);
						}
						break;
					}
					case parameter: {
						final StatTypes stat = (StatTypes) statSlider
								.getValueMapping().get(
										statSlider.getSelections().iterator()
												.next()).getSecond();
						int j = 0;
						for (final Integer select : paramSlider.getSelections()) {
							final String param = (String) paramSlider
									.getValueMapping().get(select).getSecond();
							addSample(cm, 0, j++, stat, param);
						}
						break;
					}
					default:
						addSample(cm, 0, 0, (StatTypes) statSlider
								.getValueMapping().get(
										statSlider.getSelections().iterator()
												.next()).getSecond(),
								(String) paramSlider.getValueMapping().get(
										paramSlider.getSelections().iterator()
												.next()).getSecond());
						break;
					}
					break;
				}
			} catch (final Exception e) {
				// The exceptions are not critical.
			}
		}
		add("legend", layoutLegendPanel);
		revalidate();
		repaint();
	}

	/**
	 * @param cm
	 *            A {@link ColourModel}.
	 * @param i
	 *            The main index.
	 * @param j
	 *            The secondary index.
	 * @param stat
	 *            The {@link StatTypes}.
	 * @param param
	 *            The parameter.
	 */
	private void addSample(final ColourModel cm, final int i, final int j,
			final StatTypes stat, final String param) {
		final ColourComputer m = cm.getModel(param, stat);
		final ColourFactory<ColourComputer> factory = FactoryRegistry
				.getInstance().getFactory(m);
		final ColourLegend<ColourComputer> legend = factory.createLegend(m);
		legend.setModel(m, Orientation.South);
		if (legend instanceof JComponent) {
			final JComponent sample = (JComponent) legend;
			sample.setOpaque(false);
			sample.setPreferredSize(new Dimension(50, 50));
			sample.setToolTipText(m.getTooltip());
			add(i + "_" + j, sample);
		}
	}

	private static Selectable<Pair<ParameterModel, Object>> getCurrentSlider(
			final Collection<SliderModel> sliders,
			@Nullable final ParameterModel model) {
		Selectable<Pair<ParameterModel, Object>> currentSlider = null;
		for (final SliderModel slider : sliders) {
			for (final ParameterModel pm : slider.getParameters()) {
				if (pm.equals(model)) {
					currentSlider = slider;
				}
			}
		}
		return currentSlider;
	}
}

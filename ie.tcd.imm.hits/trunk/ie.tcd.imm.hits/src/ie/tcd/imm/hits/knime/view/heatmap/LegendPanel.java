/**
 * 
 */
package ie.tcd.imm.hits.knime.view.heatmap;

import ie.tcd.imm.hits.knime.view.heatmap.ControlPanel.Slider;
import ie.tcd.imm.hits.knime.view.heatmap.ControlPanel.Slider.Type;
import ie.tcd.imm.hits.knime.view.heatmap.ViewModel.ParameterModel;
import ie.tcd.imm.hits.knime.view.heatmap.ViewModel.OverviewModel.Places;

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
	public static class ParametersPanel extends JPanel {
		private static final long serialVersionUID = -2271852289205726654L;
		private final boolean isSelectable;
		private ViewModel model;

		private final JLabel label = new JLabel();
		private final Set<ActionListener> listeners = new HashSet<ActionListener>();
		private final Places places;

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

		public boolean addActionListener(final ActionListener listener) {
			if (isSelectable) {
				return listeners.add(listener);
			}
			return false;
		}

		public boolean removeActionListener(final ActionListener listener) {
			if (isSelectable) {
				return listeners.remove(listener);
			}
			return false;
		}

		private void actionPerformed() {
			for (final ActionListener listener : listeners) {
				listener.actionPerformed(new ActionEvent(this, (int) (System
						.currentTimeMillis() & 0xffffffffL), ""));
			}
		}
	}

	private static final long serialVersionUID = 9129522729091802767L;
	private ViewModel model;
	private final LayoutLegendPanel layoutLegendPanel;

	private static class ShapeLegendPanel extends WellViewPanel {
		private static final long serialVersionUID = -1068470342510750276L;

		boolean showColors = true;
		boolean showLabels = true;

		private final Color borderColor = Color.BLACK;

		public ShapeLegendPanel(final boolean isSelectable,
				final ViewModel model) {
			super(isSelectable, model, -1);
		}

		@Override
		protected void paintComponent(final Graphics g) {
			super.paintComponent(g);
			final Rectangle bounds = getBounds();
			final int radius = Math.min(bounds.width, bounds.height) / 2;
			final Collection<Slider> sliders = getModel().getMain()
					.getArrangementModel().getSliders().get(Type.Splitter);
			final int primaryCount = selectValueCount(getModel().getMain()
					.getPrimerParameters(), sliders);
			final int secundaryCount = selectValueCount(getModel().getMain()
					.getSecunderParameters(), sliders);
			switch (getModel().getShape()) {
			case Circle:
				if (showLabels) {
					final int startAngle = getModel().getMain().getStartAngle();
					int angle = startAngle;
					if (primaryCount == 0) {
						return;
					}
					// ((Graphics2D) g).rotate(angle / Math.PI / 2);
					// for (int i = 0; i < 5; ++i) {
					// final double a = (0 + i * 360 / 5) / 180.0 * Math.PI;
					// final int origX = 0, origY = bounds.width / 2;
					// g.drawLine(origX, origY, origX
					// + (int) (Math.cos(a) * 10 * (i + 1)), origY
					// - (int) (Math.sin(a) * 10 * (i + 1)));
					// }
					g.setFont(g.getFont().deriveFont(15.0f));
					angle += 180 / primaryCount;
					for (final ParameterModel model : getModel().getMain()
							.getPrimerParameters()) {
						final Slider currentSlider = getCurrentSlider(sliders,
								model);
						if (currentSlider != null) {
							for (final Entry<Integer, Map<ParameterModel, Object>> entry : currentSlider
									.getValueMapping().entrySet()) {
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
												entry.getValue().get(model)
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
						// for (final Entry<Object, Color> entry : model
						// .getColorLegend().entrySet()) {
						// // for (int i = model.getValueCount() + 1; i-- > 1;)
						// // {
						// g.setColor(borderColor);
						// angle += 360 / primaryCount;
						// g
						// .drawString(
						// model.getShortName(),
						// bounds.width
						// / 2
						// - 40
						// + (int) (Math.cos(angle
						// / 180.0 * Math.PI) * 60),
						// bounds.height
						// / 2
						// - (int) (Math.sin(angle
						// / 180.0 * Math.PI) * 60)/*-bounds.width / 4,
						// bounds.height / 4*/);
						// g
						// .drawString(
						// entry.getKey().toString(),
						// bounds.width
						// / 2
						// - 40
						// + (int) (Math.cos(angle
						// / 180.0 * Math.PI) * 60),
						// bounds.height
						// / 2
						// - (int) (Math.sin(angle
						// / 180.0 * Math.PI) * 60 - 15)/*-bounds.width / 4,
						// bounds.height / 4*/);
						// // ((Graphics2D) g).rotate(2 * Math.PI /
						// // primaryCount);
						// //
						// // for (int j = 0; j < 7; ++j) {
						// // final double a = (0 + j * 360 / 7) / 180.0
						// // * Math.PI;
						// // final int origX = 0, origY = bounds.width / 2;
						// // g.setColor(Color.ORANGE);
						// // g
						// // .drawLine(
						// // origX,
						// // origY,
						// // origX
						// // + (int) (Math.cos(a) * 10 * (j + 1)),
						// // origY
						// // - (int) (Math.sin(a) * 10 * (j + 1)));
						// // }
						// }
					}
					// ((Graphics2D) g).rotate(-Math.PI / 2);
					// for (final ParameterModel model : getModel().getMain()
					// .getSecunderParameters()) {
					// g.drawString(model.getShortName(), -bounds.height / 2,
					// 15);
					// }
					((Graphics2D) g).rotate(-startAngle / 180.0 * Math.PI);
					g.setFont(getFont().deriveFont(Font.BOLD));
					final List<ParameterModel> secunderParameters = getModel()
							.getMain().getSecunderParameters();
					if (secunderParameters.size() > 0) {
						final ParameterModel paramModel = secunderParameters
								.iterator().next();
						g.drawString(paramModel.getShortName(), radius / 2,
								(int) (radius * 1.2));
						final Slider slider = getCurrentSlider(sliders,
								paramModel);
						final Map<Integer, Map<ParameterModel, Object>> valueMapping = slider
								.getValueMapping();
						final int[] radiuses = getRadiuses(radius, valueMapping
								.size());
						int i = 0;
						for (final Entry<Integer, Map<ParameterModel, Object>> entry : valueMapping
								.entrySet()) {
							g.drawString(entry.getValue().get(paramModel)
									.toString(), radius / 2 - 30 + radiuses[i],
									(int) (radius * 1.35) + (i % 2) * 15);
							++i;
						}
					}
					// for (int i = primaryCount; i-- > 0;) {
					// ((Graphics2D) g).rotate(primaryCount / Math.PI / 2);
					// g.drawString("______" + i, 0, 0);
					// }
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
						final Slider slider = getCurrentSlider(sliders, model);
						g.drawString(model.getShortName(), 10, 15);
						int i = 0;
						for (final Entry<Integer, Map<ParameterModel, Object>> entry : slider
								.getValueMapping().entrySet()) {
							g.drawString(
									entry.getValue().get(model).toString(), i
											* bounds.width / primaryCount,
									30 + (i % 2) * 15);
							++i;
						}
						// final int count = model.getColorLegend().entrySet()
						// .size();
						// for (final Entry<Object, Color> entry : model
						// .getColorLegend().entrySet()) {
						// g.drawString(entry.getKey().toString(), i
						// * bounds.width / count, 30 + (i % 2) * 15);
						// ++i;
						// }
					}
					((Graphics2D) g).rotate(-Math.PI / 2);
					for (final ParameterModel model : getModel().getMain()
							.getSecunderParameters()) {
						g.drawString(model.getShortName(), -bounds.height + 5,
								15);
						int i = 0;
						final Slider slider = getCurrentSlider(sliders, model);
						for (final Entry<Integer, Map<ParameterModel, Object>> entry : slider
								.getValueMapping().entrySet()) {
							g
									.drawString(entry.getValue().get(model)
											.toString(), 5 - (i + 1)
											* bounds.width / secundaryCount,
											30 + (i % 2) * 15);
							++i;
						}
					}
					g.setFont(origFont);
				}
				break;
			default:
				break;
			}
		}

		private Slider getCurrentSlider(final Collection<Slider> sliders,
				final ParameterModel model) {
			Slider currentSlider = null;
			for (final Slider slider : sliders) {
				for (final ParameterModel pm : slider.getParameters()) {
					if (pm.equals(model)) {
						currentSlider = slider;
					}
				}
			}
			return currentSlider;
		}

		public void setShowLabels(final boolean showLabels) {
			this.showLabels = showLabels;
			repaint();
		}

		public void setShowColors(final boolean showColors) {
			this.showColors = showColors;
			repaint();
		}
	}

	private static class LayoutLegendPanel extends JPanel {
		private static final long serialVersionUID = 4132948016212511178L;

		private final ParametersPanel horizontalParametersPanel;
		private final ParametersPanel verticalParametersPanel;
		private final ParametersPanel choiceParametersPanel;
		private final ShapeLegendPanel shapeLegendPanel;

		private ViewModel model;

		LayoutLegendPanel(final boolean isSelectable, final ViewModel model) {
			super();
			this.model = model;
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

		public void setModel(final ViewModel model) {
			this.model = model;
			horizontalParametersPanel.setModel(model);
			verticalParametersPanel.setModel(model);
			choiceParametersPanel.setModel(model);
			shapeLegendPanel.setModel(model);
		}
	}

	public LegendPanel(final boolean isSelectable, final ViewModel model) {
		super();
		this.model = model;
		layoutLegendPanel = new LayoutLegendPanel(isSelectable, model);
		add(layoutLegendPanel);
	}

	@Override
	public void actionPerformed(final ActionEvent e) {
		repaint();
	}

	@Override
	protected void paintComponent(final Graphics g) {
		// TODO Auto-generated method stub
		super.paintComponent(g);
	}

	public void setViewModel(final ViewModel currentViewModel) {
		model.getMain().getArrangementModel().removeListener(this);
		model = currentViewModel;
		layoutLegendPanel.setModel(model);
		model.getMain().getArrangementModel().addListener(this);
		repaint();
	}
}

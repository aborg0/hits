/**
 * 
 */
package ie.tcd.imm.hits.knime.view.heatmap;

import ie.tcd.imm.hits.knime.view.heatmap.ControlPanel.ArrangementModel;
import ie.tcd.imm.hits.knime.view.heatmap.ControlPanel.Slider;
import ie.tcd.imm.hits.knime.view.heatmap.ControlPanel.Slider.Type;
import ie.tcd.imm.hits.knime.view.heatmap.HeatmapNodeModel.StatTypes;
import ie.tcd.imm.hits.knime.view.heatmap.HeatmapNodeView.VolatileModel;
import ie.tcd.imm.hits.knime.view.heatmap.ViewModel.Format;
import ie.tcd.imm.hits.knime.view.heatmap.ViewModel.ParameterModel;
import ie.tcd.imm.hits.util.Pair;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import javax.swing.JComponent;

import org.knime.core.data.DataCell;
import org.knime.core.node.property.hilite.HiLiteListener;
import org.knime.core.node.property.hilite.KeyEvent;

/**
 * Shows a heatmap of values.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
public class Heatmap extends JComponent implements HiLiteListener {
	private static final long serialVersionUID = 7832090816939923780L;
	private ViewModel viewModel;
	private final WellViewPanel[] wells = new WellViewPanel[384];
	private Map<DataCell, Pair<Integer, Integer>> keyToPlateAndPosition;
	private VolatileModel volatileModel;
	/** From 0. */
	private int plate;

	/**
	 * Constructs a {@link Heatmap} with the {@code viewModel} and
	 * {@code dataModel}.
	 * 
	 * @param viewModel
	 *            The {@link ViewModel} to constrain the well layout.
	 * @param dataModel
	 *            The {@link HeatmapNodeModel} to set the initial values of the
	 *            wells.
	 */
	public Heatmap(final ViewModel viewModel, final HeatmapNodeModel dataModel) {
		super();
		this.viewModel = viewModel;
		internalUpdateViewModel();
	}

	@Override
	protected void paintComponent(final Graphics g) {
		super.paintComponent(g);
		// final Rectangle bounds = getBounds();
		// final int rowCount = viewModel.getFormat().getRow();
		// final int colCount = viewModel.getFormat().getCol();
		// final int radius = Math.min(bounds.height / rowCount, bounds.width
		// / colCount);
		// final Random random = new Random(12);
		// for (int i = 0; i < rowCount; ++i) {
		// for (int j = 0; j < colCount; ++j) {
		// g.setColor(new Color(random.nextInt(), false));
		// g.fillOval(bounds.x + j * radius, bounds.y + i * radius,
		// radius, radius);
		// }
		// }
	}

	/**
	 * Updates the values of the {@link Heatmap} using the parameters.
	 * 
	 * @param nodeModel
	 *            This {@link HeatmapNodeModel} should contain all values for
	 *            the experiment.
	 * @param volatileModel
	 *            This contains the current values of the layout.
	 * @param row
	 *            We want to see the {@link Heatmap} from this row.
	 * @param col
	 *            We want to see the {@link Heatmap} from this column.
	 */
	public void setModel(final HeatmapNodeModel nodeModel,
			final VolatileModel volatileModel, final int row, final int col) {
		// this.volatileModel.removeActionListener(this);
		this.volatileModel = volatileModel;
		// volatileModel.addActionListener(this);
		keyToPlateAndPosition = nodeModel.keyToPlateAndPosition;
		setHilites();
		replicates(nodeModel);
		repaint();
	}

	private void setHilites() {
		final Map<Slider, Integer> sliderPositions = volatileModel
				.getSliderPositions();
		final Collection<Slider> sliders = viewModel.getMain()
				.getArrangementModel().getSliders().get(Type.Selector);
		final int currentPlate = (sliders.size() > 0 ? sliderPositions.get(
				sliders.iterator().next()).intValue() : 1) - 1;
		final boolean[] hiliteValues = volatileModel
				.getHiliteValues(currentPlate);
		final boolean[] selections = volatileModel
				.getSelectionValues(currentPlate);
		for (int i = viewModel.getFormat().getCol()
				* viewModel.getFormat().getRow(); i-- > 0;) {
			wells[i].setHilited(hiliteValues[i]);
			wells[i].setSelected(selections[i]);
		}
	}

	private void replicates(final HeatmapNodeModel nodeModel) {
		final int cols = viewModel.getFormat().getCol();
		final int rows = viewModel.getFormat().getRow();
		final Map<Slider, Integer> sliderPositions = volatileModel
				.getSliderPositions();
		final int[] platePos = new int[Slider.MAX_INDEPENDENT_FACTORS];
		for (final Entry<Slider, Integer> entry : sliderPositions.entrySet()) {
			final Slider slider = entry.getKey();
			for (final ParameterModel model : slider.getParameters()) {
				if (model.getType() == StatTypes.plate) {
					platePos[slider.getSubId()] = entry.getValue().intValue();
				}
			}
		}
		final Map<Integer, Map<String, EnumMap<StatTypes, double[]>>> replicateMap = nodeModel.replicateValues
				.get(platePos[0]);
		final LinkedHashMap<ParameterModel, Collection<Slider>> mainArrangement = viewModel
				.getMain().getArrangementModel().getMainArrangement();
		final Slider aPlateSlider = ArrangementModel.selectNth(mainArrangement,
				0, StatTypes.plate);
		final Collection<Slider> splitterSliders = viewModel.getMain()
				.getArrangementModel().getSliders().get(Type.Splitter);
		assert splitterSliders != null;
		assert splitterSliders.size() > 0;
		int size = 0;
		for (final Entry<ParameterModel, Collection<Slider>> entry : mainArrangement
				.entrySet()) {
			if (entry.getKey().getAggregateType() != null) {
				size += computeContributedValuesCount(entry.getValue());
			} else {
				size += computeSplitterCount(splitterSliders);
			}
		}
		plate = volatileModel.getSliderPositions().get(aPlateSlider).intValue() - 1;
		// replicateMap.entrySet().iterator().next()
		// .getValue().keySet().size();//
		// nodeModel.scoreValues.get(platePos[0]).entrySet().size()
		final Slider replicateSlider = ArrangementModel.selectNth(
				mainArrangement, 0, StatTypes.replicate);
		final int replicateCount = replicateSlider.getValueMapping().size();// getParameters().iterator()
		// .next().getColorLegend().size();
		final Color[][] colors = new Color[rows * cols][size/* * replicateCount */];
		for (final Entry<ParameterModel, Collection<Slider>> mainEntry : mainArrangement
				.entrySet()) {
			final Collection<Slider> currentSliders = mainEntry.getValue();
			String selectedParameter = null;
			Slider parameterSlider = null;
			StatTypes selected = null;// StatTypes.normalized;
			Slider statSlider = null;
			Integer platePosition = null;
			Slider plateSlider = null;
			for (final Slider currentSlider : currentSliders) {
				final List<ParameterModel> parameters = currentSlider
						.getParameters();
				for (final ParameterModel parameterModel : parameters) {
					final Integer currentSliderValue = sliderPositions
							.get(currentSlider);
					// FIXME if problem with null
					switch (parameterModel.getType()) {
					case metaStatType: {
						assert parameters.size() == 1;
						final Pair<ParameterModel, Object> value = currentSlider
								.getValueMapping().get(currentSliderValue);
						assert value != null;
						final Object object = value.getRight();
						assert object instanceof StatTypes : object.getClass();
						selected = currentSlider.getType() == Type.Splitter ? null
								: (StatTypes) object;
						statSlider = currentSlider;
						break;
					}
					case parameter: {
						assert parameters.size() == 1;
						if (currentSliderValue != null) {
							final Pair<ParameterModel, Object> value = currentSlider
									.getValueMapping().get(currentSliderValue);
							assert value != null;
							final Object object = value.getRight();
							assert object instanceof String : object.getClass();
							selectedParameter = currentSlider.getType() == Type.Splitter ? null
									: (String) object;
						}
						parameterSlider = currentSlider;
						break;
					}
					case plate: {
						assert parameters.size() == 1;
						final Pair<ParameterModel, Object> value = currentSlider
								.getValueMapping().get(currentSliderValue);
						assert value != null;
						final Object object = value.getRight();
						assert object instanceof Integer : object.getClass();
						platePosition = currentSlider.getType() == Type.Splitter ? null
								: (Integer) object;
						plateSlider = currentSlider;
						break;
					}
					default:// Not handled yet.
						break;
					}
				}
			}
			final int paramCount = computeSplitterCount(Collections
					.singleton(parameterSlider));
			final Set<String> currentParameters = new HashSet<String>();
			for (final Pair<ParameterModel, Object> m : parameterSlider
					.getValueMapping().values()) {
				currentParameters.add((String) m.getRight());
			}
			final Set<Integer> currentReplicates = new HashSet<Integer>();
			for (final Pair<ParameterModel, Object> m : replicateSlider
					.getValueMapping().values()) {
				currentReplicates.add((Integer) m.getRight());
			}
			if (selected.isUseReplicates()) {
				int replicateValue = 0;
				for (final Entry<Integer, Map<String, EnumMap<StatTypes, double[]>>> replicateEntry : replicateMap
						.entrySet()) {
					if (!currentReplicates.contains(replicateEntry.getKey())) {
						continue;
					}
					final Map<String, EnumMap<StatTypes, double[]>> paramMap = replicateEntry
							.getValue();
					int param = 0;
					for (final Map.Entry<String, EnumMap<StatTypes, double[]>> entry : paramMap
							.entrySet()) {
						if (currentParameters.contains(entry.getKey())) {
							for (int i = entry.getValue().get(selected).length; i-- > 0;) {
								final Color colorI = colorOf(
								/*
								 * paramMap.get( "Nuc Displacement")
								 */entry.getValue().get(selected)[i],
										Color.GREEN, Color.YELLOW, Color.RED,
										-1, 0, 1);
								switch (viewModel.getFormat()) {
								case _96:
									colors[i][replicateValue * paramCount
											+ param] = colorI;
									break;
								case _384:
									colors[i / 12 * cols + i % 12][/*
									 * (replicateEntry
									 * .getKey().intValue() -
									 * 1)
									 */replicateValue * paramCount + param] = colorI;
								default:
									break;
								}
							}
							++param;
						}
					}
					++replicateValue;
				}
			} else {
				if (selectedParameter == null) {
					int param = 0;
					for (final Entry<Integer, Pair<ParameterModel, Object>> entry : parameterSlider
							.getValueMapping().entrySet()) {
						// final int param = entry.getKey().intValue() - 1;
						final String paramName = (String) entry.getValue()
								.getRight();
						// TODO handle selected == null.
						// TODO handle plateposition == null
						final double[] values = nodeModel.scoreValues.get(
								platePosition).get(paramName).get(selected);
						for (int i = values.length; i-- > 0;) {
							final Color colorI = colorOf(values[i],
									Color.GREEN, Color.YELLOW, Color.RED, -1,
									0, 1);
							for (int replicate = replicateCount; replicate-- > 0;) {
								switch (viewModel.getFormat()) {
								case _96:
									colors[i][replicate * paramCount + param] = colorI;
									break;
								case _384:
									colors[i / 12 * cols + i % 12][replicate
											* paramCount + param] = colorI;
								default:
									break;
								}
							}
						}
						++param;
					}
				} else {
					// TODO handle platePosition == null
					final double[] values = nodeModel.scoreValues.get(
							Integer.valueOf(platePosition)).get(
							selectedParameter).get(selected);
					for (int i = values.length; i-- > 0;) {
						final Color colorI = colorOf(values[i], Color.GREEN,
								Color.YELLOW, Color.RED, -1, 0, 1);
						for (int replicate = replicateCount; replicate-- > 0;) {
							for (int param = paramCount; param-- > 0;) {
								switch (viewModel.getFormat()) {
								case _96:
									colors[i][replicate * paramCount + param] = colorI;
									break;
								case _384:
									colors[i / 12 * cols + i % 12][replicate
											* paramCount + param] = colorI;
								default:
									break;
								}
							}
						}
					}
				}
			}
		}
		for (int i = rows * cols; i-- > 0;) {
			wells[i].setColors(colors[i]);
		}
		for (int i = rows * cols; i-- > 0;) {
			final String[] pantherMolFunction = nodeModel.texts
					.get(platePos[0]).get("PantherMolecularFunction");
			final String molFunc = pantherMolFunction == null ? ""
					: pantherMolFunction[i];
			final String[] geneSymbols = nodeModel.texts.get(platePos[0]).get(
					"gene symbol");
			final String[] pathway = nodeModel.texts.get(platePos[0]).get(
					"Pathway");
			final StringBuilder label = new StringBuilder("<html>").append(
					geneSymbols == null ? "" : geneSymbols[i]).append(" - ")
					.append(
							molFunc == null ? "" : molFunc.replaceAll("\\;",
									"<br>").replaceAll("\\n", "<br>")).append(
							"<br/>").append(pathway == null ? "" : pathway[i])
					.append("<br>");
			final Map<String, EnumMap<StatTypes, double[]>> map = nodeModel.scoreValues
					.get(platePos[0]);
			label.append("<br><table><tr><td>Plate</td><td>").append(
					platePos[0]).append("</td></tr><tr><td>Well</td><td>")
					.append((char) ((i / 12) + 'A')).append(1 + (i % 12))
					.append("</td></tr><tr><td><b>Scores</b></td></tr>");
			for (final Entry<String, EnumMap<StatTypes, double[]>> entry : map
					.entrySet()) {
				label
						.append("<tr><td>")
						.append(entry.getKey())
						.append("</td><td>")
						.append(
								Math.round(entry.getValue()
										.get(StatTypes.score)[i] * 100.0) / 100.0)
						.append("</td></tr>");
			}
			label.append("</table>");
			label.append("</html>");
			wells[i].setLabels(label.toString());
		}
	}

	private int computeSplitterCount(final Collection<Slider> sliders) {
		int ret = 1;
		for (final Slider slider : sliders) {
			// final List<ParameterModel> parameters = slider.getParameters();
			// assert parameters.size() == 1 : slider;
			// final ParameterModel parameter = parameters.iterator().next();
			ret *= slider.getValueMapping().size();// parameter.getColorLegend().size();
		}
		return ret;
	}

	private int computeContributedValuesCount(final Collection<Slider> value) {
		// TODO Auto-generated method stub
		return 0;
	}

	private Color colorOf(final double d, final Color blue, final Color black,
			final Color red, final double low, final double mid,
			final double high) {
		if (d < low) {
			return blue;
		}
		if (d > high) {
			return red;
		}
		if (black == null) {
			return new Color((float) (blue.getRed() / 256.0f + (d - low)
					/ (high - low) * (blue.getRed() - red.getRed()) / 256.0f),
					(float) (blue.getGreen() / 256.0f + (d - low)
							/ (high - low) * (blue.getGreen() - red.getGreen())
							/ 256.0f),
					(float) (blue.getBlue() / 256.0f + (d - low) / (high - low)
							* (blue.getBlue() - red.getBlue()) / 256.0f));
		}
		if (d < mid) {
			return new Color((float) (black.getRed() / 256.0f + (mid - d)
					/ (mid - low) * (blue.getRed() - black.getRed()) / 256.0f),
					(float) (black.getGreen() / 256.0f + (mid - d)
							/ (mid - low)
							* (blue.getGreen() - black.getGreen()) / 256.0f),
					(float) (black.getBlue() / 256.0f + (mid - d) / (mid - low)
							* (blue.getBlue() - black.getBlue()) / 256.0f));
		}
		return new Color((float) (black.getRed() / 256.0f + (d - mid)
				/ (high - mid) * (red.getRed() - black.getRed()) / 256.0f),
				(float) (black.getGreen() / 256.0f + (d - mid) / (high - mid)
						* (red.getGreen() - black.getGreen()) / 256.0f),
				(float) (black.getBlue() / 256.0f + (d - mid) / (high - mid)
						* (red.getBlue() - black.getBlue()) / 256.0f));
	}

	/**
	 * Updates the {@link Heatmap} using the values from {@code viewModel}.
	 * 
	 * @param viewModel
	 *            The new arrangement of the wells.
	 */
	public void setViewModel(final ViewModel viewModel) {
		this.viewModel = viewModel;
		internalUpdateViewModel();
		repaint();
	}

	private void internalUpdateViewModel() {
		removeAll();
		final Format format = viewModel.getFormat();
		final int rows = format.getRow();
		final int cols = format.getCol();
		setLayout(new GridLayout(rows, cols));
		for (int j = 0; j < cols; ++j) {
			for (int i = 0; i < rows; ++i) {
				final WellViewPanel shapeLegendPanel = new WellViewPanel(true,
						viewModel, j * rows + i);
				shapeLegendPanel.setPreferredSize(new Dimension(
						getBounds().width / cols, getBounds().height / rows));
				shapeLegendPanel.addMouseListener(new MouseAdapter() {
					@Override
					public void mouseClicked(final MouseEvent e) {
						super.mouseClicked(e);
						final WellViewPanel source = (WellViewPanel) e
								.getSource();
						final boolean oldSelection = source.isSelected();
						if ((e.getModifiersEx() & InputEvent.CTRL_DOWN_MASK) == 0) {
							volatileModel.clearSelection();
							for (final WellViewPanel well : wells) {
								if (well != null) {
									well.setSelected(false);
								}
							}
						}
						source.setSelected(!oldSelection);
						volatileModel.setSelection(plate, source
								.getPositionOnPlate(), source.isSelected());
						repaint();
					}
				});
				wells[j * rows + i] = shapeLegendPanel;
				// shapeLegendPanel.setHilited(volatileModel
				// .getHiliteValues(plate)[j * rows + i]);
				add(shapeLegendPanel);
			}
		}
		validate();
	}

	@Override
	public void hiLite(final KeyEvent event) {
		hilite(event, true);
		System.out.println(event.keys());
	}

	private void hilite(final KeyEvent event, final boolean hilite) {
		for (final DataCell key : event.keys()) {
			final Pair<Integer, Integer> pair = keyToPlateAndPosition.get(key);
			if (pair != null) {
				assert pair != null;
				final int plate = pair.getLeft().intValue() - 1;
				final int pos = pair.getRight().intValue();
				volatileModel.setHilite(plate, pos, hilite);
			}
		}
		setHilites();
	}

	@Override
	public void unHiLite(final KeyEvent event) {
		hilite(event, false);
	}

	@Override
	public void unHiLiteAll() {
		for (final WellViewPanel well : wells) {
			if (well != null) {
				well.setHilited(false);
			}
		}
		volatileModel.unHiliteAll();
	}

	/**
	 * This method adds a {@link MouseListener} for each well of the
	 * {@link Heatmap}.
	 * 
	 * @param listener
	 *            A non-{@code null} {@link MouseListener}.
	 */
	public void addClickListenerForEveryWell(final MouseListener listener) {
		for (final WellViewPanel well : wells) {
			if (well != null) {
				well.addMouseListener(listener);
			}
		}
	}
}

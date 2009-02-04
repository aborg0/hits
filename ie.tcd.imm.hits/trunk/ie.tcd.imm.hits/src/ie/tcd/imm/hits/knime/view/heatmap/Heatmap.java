/**
 * 
 */
package ie.tcd.imm.hits.knime.view.heatmap;

import ie.tcd.imm.hits.common.Format;
import ie.tcd.imm.hits.knime.util.ModelBuilder;
import ie.tcd.imm.hits.knime.util.VisualUtils;
import ie.tcd.imm.hits.knime.view.heatmap.ColourSelector.ColourModel;
import ie.tcd.imm.hits.knime.view.heatmap.ColourSelector.DoubleValueSelector.Model;
import ie.tcd.imm.hits.knime.view.heatmap.HeatmapNodeModel.StatTypes;
import ie.tcd.imm.hits.knime.view.heatmap.HeatmapNodeView.VolatileModel;
import ie.tcd.imm.hits.knime.view.heatmap.SliderModel.Type;
import ie.tcd.imm.hits.knime.view.heatmap.ViewModel.ParameterModel;
import ie.tcd.imm.hits.util.Pair;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;
import javax.swing.JComponent;

import org.knime.core.data.RowKey;
import org.knime.core.data.property.ColorAttr;
import org.knime.core.node.property.hilite.HiLiteListener;
import org.knime.core.node.property.hilite.KeyEvent;

/**
 * Shows a heatmap of values.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
public class Heatmap extends JComponent implements HiLiteListener {
	private static final long serialVersionUID = 7832090816939923780L;
	private static final Integer ZERO = Integer.valueOf(0);
	private ViewModel viewModel;
	private final WellViewPanel[] wells = new WellViewPanel[384];
	private Map<String, Pair<Integer, Integer>> keyToPlateAndPosition;
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
		keyToPlateAndPosition = nodeModel.getModelBuilder()
				.getKeyToPlateAndPosition();
		setHilites();
		replicates(nodeModel);
		repaint();
	}

	private void setHilites() {
		final Collection<SliderModel> sliders = viewModel.getMain()
				.getArrangementModel().getSliders().get(Type.Selector);
		final int currentPlate = (sliders.size() > 0 ? sliders.iterator()
				.next().getSelections().iterator().next() : 1) - 1;
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
		/* 0-MAX_INDEPENDENT_FACTORS -> selected plate values. */
		final Map<Integer, List<Integer>> platePos = new HashMap<Integer, List<Integer>>();
		/* 0-MAX_INDEPENDENT_FACTORS -> selected experiment values. */
		final Map<Integer, List<String>> experimentPos = new HashMap<Integer, List<String>>();
		/* 0-MAX_INDEPENDENT_FACTORS -> selected normalisation values. */
		final Map<Integer, List<String>> normalisationPos = new HashMap<Integer, List<String>>();
		/* 0-MAX_INDEPENDENT_FACTORS -> selected parameter values. */
		final Map<Integer, List<String>> parameterPos = new HashMap<Integer, List<String>>();
		/* 0-MAX_INDEPENDENT_FACTORS -> selected statistics values. */
		final Map<Integer, List<StatTypes>> statPos = new HashMap<Integer, List<StatTypes>>();
		/* 0-MAX_INDEPENDENT_FACTORS -> selected replicate values. */
		final Map<Integer, List<Integer>> replicatePos = new HashMap<Integer, List<Integer>>();
		for (int i = SliderModel.MAX_INDEPENDENT_FACTORS; i-- > 0;) {
			platePos.put(Integer.valueOf(i), new ArrayList<Integer>());
			experimentPos.put(Integer.valueOf(i), new ArrayList<String>());
			normalisationPos.put(Integer.valueOf(i), new ArrayList<String>());
			parameterPos.put(Integer.valueOf(i), new ArrayList<String>());
			statPos.put(Integer.valueOf(i), new ArrayList<StatTypes>());
			replicatePos.put(Integer.valueOf(i), new ArrayList<Integer>());
		}
		SliderModel experimentSlider = null;
		SliderModel normalisationSlider = null;
		SliderModel parameterSlider = null;
		SliderModel statSlider = null;
		SliderModel plateSlider = null;
		SliderModel replicateSlider = null;
		for (final SliderModel slider : viewModel.getMain()
				.getArrangementModel().getSliderModels()) {

			for (final ParameterModel model : slider.getParameters()) {
				switch (model.getType()) {
				case plate:
					selectionToPos(platePos, slider, Integer.class);
					plateSlider = slider;
					break;
				case experimentName:
					selectionToPos(experimentPos, slider, String.class);
					experimentSlider = slider;
					break;
				case normalisation:
					selectionToPos(normalisationPos, slider, String.class);
					normalisationSlider = slider;
					break;
				case parameter:
					selectionToPos(parameterPos, slider, String.class);
					parameterSlider = slider;
					break;
				case metaStatType: {
					selectionToPos(statPos, slider, StatTypes.class);
					statSlider = slider;
					break;
				}
				case replicate: {
					selectionToPos(replicatePos, slider, Integer.class);
					replicateSlider = slider;
					break;
				}

				default:
					// Do nothing.
					break;
				}
			}
		}
		final SliderModel firstParamSlider = findSlider(viewModel.getMain()
				.getArrangementModel().getSliderModels(), viewModel.getMain()
				.getPrimerParameters());
		final SliderModel secondParamSlider = findSlider(viewModel.getMain()
				.getArrangementModel().getSliderModels(), viewModel.getMain()
				.getSeconderParameters());
		final int size = computeSplitterCount(Arrays.asList(firstParamSlider,
				secondParamSlider));
		plate = plateSlider.getSelections().size() == 1 ? ((Number) plateSlider
				.getValueMapping().get(
						plateSlider.getSelections().iterator().next())
				.getRight()).intValue() - 1 : -1;
		final Color[][] colors = new Color[rows * cols][size];
		final ColourModel colourModel = volatileModel.getColourModel();
		final ModelBuilder modelBuilder = nodeModel.getModelBuilder();
		final Map<String, Map<String, Map<Integer, Map<String, Map<StatTypes, double[]>>>>> scores = modelBuilder
				.getScores();
		// ENH use Berkeley DB
		final List<SliderModel> scoreSliderList = Arrays.asList(
				experimentSlider, normalisationSlider, plateSlider,
				parameterSlider, statSlider);
		if (secondParamSlider != null
				&& secondParamSlider.equals(replicateSlider)) {
			for (int i = replicateSlider.getSelections().size(); i-- > 0;) {
				findColourValues(Format._96, colors, scores, colourModel,
						firstParamSlider, null, 0, secondParamSlider, null, i,
						scoreSliderList, 0);
			}
		} else if (firstParamSlider.equals(replicateSlider)) {
			for (int i = replicateSlider.getSelections().size(); i-- > 0;) {
				findColourValues(Format._96, colors, scores, colourModel,
						firstParamSlider, null, i, secondParamSlider, null, i,
						scoreSliderList, 0);
			}
		} else {
			findColourValues(Format._96, colors, scores, colourModel,
					firstParamSlider, null, 0, secondParamSlider, null, 0,
					scoreSliderList, 0);
		}
		final Map<String, Map<String, Map<Integer, Map<Integer, Map<String, Map<StatTypes, double[]>>>>>> replicates = modelBuilder
				.getReplicates();
		final List<SliderModel> replicateSliderList = Arrays.asList(
				experimentSlider, normalisationSlider, plateSlider,
				replicateSlider, parameterSlider, statSlider);
		findColourValues(Format._96, colors, replicates, colourModel,
				firstParamSlider, null, 0, secondParamSlider, null, 0,
				replicateSliderList, 0);
		// exp, norm, plate, col
		final Map<String, Map<String, Map<Integer, Color[]>>> colours = modelBuilder
				.getColours();
		assert colours != null;
		// norm, plate, col
		final Map<String, Map<Integer, Color[]>> normColors = colours == null ? null
				: experimentPos.get(ZERO).size() > 0 ? colours
						.get(experimentPos.get(ZERO).get(0)) : null;
		if (normColors != null) {
			final Map<Integer, Color[]> plateColours = normalisationPos.get(
					ZERO).size() > 0 ? normColors.get(normalisationPos
					.get(ZERO).get(0)) : null;
			if (plateColours != null) {
				final Color[] array = plateColours.get(Integer
						.valueOf(plate + 1));
				if (array != null) {
					for (int i = rows * cols; i-- > 0;) {
						final Color color = array[i];
						wells[i]
								.setBackground(color == null ? ColorAttr.BACKGROUND
										: color);
					}
				}
			}
		}
		for (int i = rows * cols; i-- > 0;) {
			wells[i].setColors(colors[i]);
		}
		for (int i = rows * cols; i-- > 0;) {
			final String l = InfoParser.parse(experimentPos.get(ZERO),
					normalisationPos.get(ZERO), viewModel.getLabelPattern(), /*
					 * plate +
					 * 1
					 */
					platePos.get(ZERO), i / 12, i % 12, nodeModel);
			wells[i].setLabels(l);
		}
	}

	/**
	 * Finds the proper colour values for the selected parameters.
	 * <p>
	 * Recursive implementation.
	 * 
	 * @param format
	 *            The {@link Format} is used to compute the positions.
	 * @param colors
	 *            The results will be in this array. The first dimension is the
	 *            position, the second is the selected primary & secondary
	 *            parameter.
	 * @param scores
	 *            The values associated to the parameters.
	 * @param colourModel
	 *            The colourModel to have the colours from.
	 * @param firstParamSlider
	 *            The primary parameter {@link SliderModel}.
	 * @param firstSelection
	 *            The actual selection of primary {@link SliderModel}.
	 * @param firstIndex
	 *            The position in the second dimension of {@code colors}. This
	 *            is for the primary parameter.
	 * @param secondParamSlider
	 *            The secondary parameter {@link SliderModel}.
	 * @param secondSelection
	 *            The actual selection of secondary {@link SliderModel}.
	 * @param secondIndex
	 *            The position in the second dimension of {@code colors}. This
	 *            is for the secondary parameter.
	 * @param sliderList
	 *            This list describes the layout of {@code scores}.
	 * @param sliderIndex
	 *            The actual position in the {@code sliderList} (the recursion
	 *            depth).
	 */
	private void findColourValues(final Format format, final Color[][] colors,
			final Object scores, final ColourModel colourModel,
			final SliderModel firstParamSlider, @Nullable
			final Integer firstSelection, final int firstIndex,
			final SliderModel secondParamSlider, @Nullable
			final Integer secondSelection, final int secondIndex,
			final List<SliderModel> sliderList, final int sliderIndex) {
		final int firstParamCount = firstParamSlider == null ? 1
				: firstParamSlider.getSelections().size();
		double[] array = null;
		if (sliderList.size() > sliderIndex) {
			Map<?, ?> map = (Map<?, ?>) scores;

			for (int i = sliderIndex; i < sliderList.size(); ++i) {
				final SliderModel sliderModel = sliderList.get(i);
				if (map == null) {
					return;
				}
				if (sliderModel == firstParamSlider) {
					int j = 0;
					for (final Integer selected : sliderModel.getSelections()) {
						findColourValues(format, colors, map.get(sliderModel
								.getValueMapping().get(selected).getRight()),
								colourModel, firstParamSlider, selected, j,
								secondParamSlider, secondSelection,
								secondIndex, sliderList, i + 1);
						++j;
					}
					return;
				}
				if (sliderModel == secondParamSlider) {
					int j = 0;
					for (final Integer selected : sliderModel.getSelections()) {
						findColourValues(format, colors, map.get(sliderModel
								.getValueMapping().get(selected).getRight()),
								colourModel, firstParamSlider, firstSelection,
								firstIndex, secondParamSlider, selected, j,
								sliderList, i + 1);
						++j;
					}
					return;
				} else {
					assert sliderModel.getSelections().size() == 1;
					final Object obj = map.get(sliderModel.getValueMapping()
							.get(sliderModel.getSelections().iterator().next())
							.getRight());
					if (i < sliderList.size() - 1) {
						map = (Map<?, ?>) obj;
					} else {
						array = (double[]) obj;
					}
				}
			}
		} else {
			array = (double[]) scores;
		}
		SliderModel paramSlider = null;
		SliderModel statSlider = null;
		for (final SliderModel sliderModel : sliderList) {
			switch (sliderModel.getParameters().get(0).getType()) {
			case parameter:
				paramSlider = sliderModel;
				break;
			case metaStatType:
				statSlider = sliderModel;
				break;
			default:
				break;
			}
		}
		final Model model0 = colourModel
				.getModel(
						(String) (paramSlider.getSelections().size() == 1 ? paramSlider
								.getValueMapping().get(
										paramSlider.getSelections().iterator()
												.next()).getRight()
								: firstParamSlider == paramSlider ? firstParamSlider
										.getValueMapping().get(firstSelection)
										.getRight()
										: secondParamSlider.getValueMapping()
												.get(secondSelection)
												.getRight()),
						(StatTypes) (statSlider.getSelections().size() == 1 ? statSlider
								.getValueMapping().get(
										statSlider.getSelections().iterator()
												.next()).getRight()
								: firstParamSlider == statSlider ? firstParamSlider
										.getValueMapping().get(firstSelection)
										.getRight()
										: secondParamSlider.getValueMapping()
												.get(secondSelection)
												.getRight()));
		final Model model = model0 == null ? ColourSelector.DEFAULT_MODEL
				: model0;
		if (array != null) {
			for (int p = format.getCol() * format.getRow(); p-- > 0;) {
				colors[p][secondIndex * firstParamCount + firstIndex] = VisualUtils
						.colourOf(array[p], model.getDown(), model
								.getMiddleVal() == null ? null : model
								.getMiddle(), model.getUp(),
								model.getDownVal(),
								model.getMiddleVal() == null ? 0.0 : model
										.getMiddleVal().doubleValue(), model
										.getUpVal());

			}
		}
	}

	/**
	 * Finds a {@link SliderModel} with similar
	 * {@link SliderModel#getParameters() parameters}.
	 * 
	 * @param sliderModels
	 *            Some {@link SliderModel}s.
	 * @param parameters
	 *            Some {@link ParameterModel}s. Only the fist will be used.
	 * @return The {@link SliderModel} with fist
	 *         {@link SliderModel#getParameters() parameter} same as the first
	 *         {@code parameters}, or {@code null} if not found in
	 *         {@code sliderModels}.
	 */
	private @Nullable
	SliderModel findSlider(final Set<SliderModel> sliderModels,
			final List<ParameterModel> parameters) {
		if (parameters.size() == 0) {
			return null;
		}
		for (final SliderModel sliderModel : sliderModels) {
			if (sliderModel.getParameters().iterator().next().equals(
					parameters.get(0))) {
				return sliderModel;
			}
		}
		return null;
	}

	/**
	 * Converts the selections to the proper values for the
	 * {@link SliderModel#getSubId()}s.
	 * 
	 * @param <T>
	 *            The type of the values in {@code posMap}.
	 * @param posMap
	 *            The map of the positions ({@code 0} (inclusive) to
	 *            {@link SliderModel#MAX_INDEPENDENT_FACTORS}{@code} exclusive)
	 *            to the values. The values will be added to the lists. It is a
	 *            good choice to have these lists modifiable and empty at the
	 *            beginning.
	 * @param slider
	 *            A {@link SliderModel}.
	 * @param cls
	 *            The class of the types.
	 */
	private <T> void selectionToPos(final Map<Integer, List<T>> posMap,
			final SliderModel slider, final Class<? extends T> cls) {
		final List<T> list = posMap.get(Integer.valueOf(slider.getSubId()));
		for (final Integer selection : slider.getSelections()) {
			list.add(cls.cast(slider.getValueMapping().get(selection)
					.getRight()));
		}
	}

	private int computeSplitterCount(final Collection<SliderModel> sliders) {
		int ret = 1;
		for (final SliderModel slider : sliders) {
			if (slider != null) {
				ret *= slider.getSelections().size();
			}
		}
		return ret;
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
				final WellViewPanel well = new WellViewPanel(true, viewModel, j
						* rows + i);
				well.setPreferredSize(new Dimension(getBounds().width / cols,
						getBounds().height / rows));
				well.addMouseListener(new MouseAdapter() {
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
						if (plate == -1) {
							final Collection<Number> selectedPlates = new ArrayList<Number>();
							for (final SliderModel slider : viewModel.getMain()
									.getArrangementModel().getSliderModels()) {
								if (slider.getParameters().iterator().next()
										.getType() == StatTypes.plate) {
									final Set<Integer> selections = slider
											.getSelections();
									for (final Integer integer : selections) {
										selectedPlates.add((Number) slider
												.getValueMapping().get(integer)
												.getRight());
									}
									break;
								}
							}
							for (final Number selectedPlate : selectedPlates) {
								volatileModel.setSelection(selectedPlate
										.intValue() - 1, source
										.getPositionOnPlate(), source
										.isSelected());
							}
						} else {
							volatileModel.setSelection(plate, source
									.getPositionOnPlate(), source.isSelected());
						}
						repaint();
					}
				});
				wells[j * rows + i] = well;
				add(well);
			}
		}
		validate();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void hiLite(final KeyEvent event) {
		hilite(event, true);
	}

	private void hilite(final KeyEvent event, final boolean hilite) {
		for (final RowKey key : event.keys()) {
			final Pair<Integer, Integer> pair = keyToPlateAndPosition.get(key
					.getString());
			if (pair != null) {
				assert pair != null;
				final int plate = pair.getLeft().intValue() - 1;
				final int pos = pair.getRight().intValue();
				volatileModel.setHilite(plate, pos, hilite);
			}
		}
		setHilites();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void unHiLite(final KeyEvent event) {
		hilite(event, false);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void unHiLiteAll(final KeyEvent event) {
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

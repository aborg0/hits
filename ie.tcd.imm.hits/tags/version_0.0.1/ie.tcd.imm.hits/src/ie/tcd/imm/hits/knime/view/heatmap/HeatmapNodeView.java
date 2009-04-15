package ie.tcd.imm.hits.knime.view.heatmap;

import ie.tcd.imm.hits.knime.view.heatmap.ControlPanel.ArrangementModel;
import ie.tcd.imm.hits.knime.view.heatmap.ControlPanel.Slider;
import ie.tcd.imm.hits.knime.view.heatmap.ControlPanel.Slider.Type;
import ie.tcd.imm.hits.knime.view.heatmap.HeatmapNodeModel.StatTypes;
import ie.tcd.imm.hits.knime.view.heatmap.ViewModel.Format;
import ie.tcd.imm.hits.knime.view.heatmap.ViewModel.OverviewModel;
import ie.tcd.imm.hits.knime.view.heatmap.ViewModel.ParameterModel;
import ie.tcd.imm.hits.knime.view.heatmap.ViewModel.Shape;
import ie.tcd.imm.hits.util.Pair;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.HierarchyBoundsAdapter;
import java.awt.event.HierarchyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.data.DataCell;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeView;
import org.knime.core.node.property.hilite.HiLiteHandler;
import org.knime.core.node.property.hilite.HiLiteListener;
import org.knime.core.node.property.hilite.KeyEvent;
import org.knime.core.node.tableview.TableView;

import edu.umd.cs.findbugs.annotations.DefaultAnnotation;

/**
 * <code>NodeView</code> for the "Heatmap" Node. Shows the heatmap of the
 * plates.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
@DefaultAnnotation( { Nonnull.class, CheckReturnValue.class })
@NotThreadSafe
public class HeatmapNodeView extends NodeView {
	private final LegendPanel legendPanel;
	private final LegendPanel legendPanel2;
	// private final JTable infoTable = new JTable(1, 1);
	private final JLabel infoTable = new JLabel("Click on a well.");
	private final TableView table = new TableView();
	private final SettingsPanel settingsPanel;
	private final InfoControl infoControl;
	private final JCheckBoxMenuItem showHilite;
	private final JCheckBoxMenuItem showOnlyHilited;
	private final JMenuItem unHiliteSelected;
	private final JMenuItem unHiliteAll;
	private final JMenuItem hiliteSelected;
	private final JCheckBoxMenuItem showColorsLegend;
	private final JCheckBoxMenuItem showTooltipsLegend;
	private final JCheckBoxMenuItem testingForSelected;
	private final HeatmapPanel heatmapPanel;

	/**
	 * This {@link HiLiteListener} updates the {@link HeatmapPanel} on changes
	 * of {@link HeatmapPanel}.
	 */
	private static class HeatmapPanel extends JPanel implements ActionListener {
		private static final class HeatmapHiLiteListener implements
				HiLiteListener, Serializable {
			private static final long serialVersionUID = -1134588773984128388L;
			private final HeatmapPanel heatmap;

			/**
			 * Constructs the {@link HiLiteListener} using the {@code heatmap}.
			 * 
			 * @param heatmap
			 *            The {@link HeatmapPanel} to update on change.
			 */
			public HeatmapHiLiteListener(final HeatmapPanel heatmap) {
				super();
				this.heatmap = heatmap;
			}

			/**
			 * {@inheritDoc}
			 */
			@Override
			public void unHiLiteAll() {
				for (final Map<Integer, Heatmap> map : heatmap.heatmaps
						.values()) {
					for (final Heatmap heatmap : map.values()) {
						heatmap.unHiLiteAll();
					}
				}
			}

			/**
			 * {@inheritDoc}
			 */
			@Override
			public void unHiLite(final KeyEvent event) {
				for (final Map<Integer, Heatmap> map : heatmap.heatmaps
						.values()) {
					for (final Heatmap heatmap : map.values()) {
						heatmap.unHiLite(event);
					}
				}
			}

			/**
			 * {@inheritDoc}
			 */
			@Override
			public void hiLite(final KeyEvent event) {
				for (final Map<Integer, Heatmap> map : heatmap.heatmaps
						.values()) {
					for (final Heatmap heatmap : map.values()) {
						heatmap.hiLite(event);
					}
				}
			}
		}

		private static final long serialVersionUID = 6589091201002870311L;
		private final Map<Integer, Map<Integer, Heatmap>> heatmaps = new HashMap<Integer, Map<Integer, Heatmap>>();
		private ViewModel model;
		private @Nullable
		HeatmapNodeModel dataModel;
		private final HeatmapHiLiteListener hiliteListener;
		private final VolatileModel volatileModel;

		/**
		 * Constructs the {@link HeatmapPanel}.
		 * 
		 * @param model
		 *            The layout model of the {@link Heatmap}.
		 * @param dataModel
		 *            The data to show.
		 * @param volatileModel
		 *            The actual values of the visualisation parameters.
		 */
		public HeatmapPanel(final ViewModel model, @Nullable
		final HeatmapNodeModel dataModel, final VolatileModel volatileModel) {
			super();
			this.model = model;
			this.dataModel = dataModel;
			this.volatileModel = volatileModel;
			internalSetModel();
			hiliteListener = new HeatmapHiLiteListener(this);
		}

		private void internalSetModel() {
			removeAll();
			heatmaps.clear();
			final OverviewModel overview = model.getOverview();
			// final List<ParameterModel> choiceModel =
			// overview.getChoiceModel();
			// TODO use the proper value assigned to
			final Map<Type, Collection<Slider>> sliders = model.getMain()
					.getArrangementModel().getSliders();
			final int allChoiceCount = volatileModel.count(StatTypes.plate);
			final int selectorCount = possibleValueCount(sliders
					.get(Type.Selector));
			Container currentContainer = this;
			final Collection<Slider> possSelectors = model.getMain()
					.getArrangementModel().getSliders().get(Type.Selector);
			for (final Slider selector : possSelectors) {
				final Color[] possColors = new Color[] { Color.DARK_GRAY,
						Color.LIGHT_GRAY };
				final Slider selectorSlider = possSelectors.size() > 0 ? possSelectors
						.iterator().next()
						: null;
				final int sliderPos = !volatileModel.getSliderPositions()
						.containsKey(selectorSlider) ? 1 : volatileModel
						.getSliderPositions().get(selectorSlider).intValue();
				final JSlider slider = new JSlider(SwingConstants.HORIZONTAL,
						1, allChoiceCount, sliderPos);
				slider.setMinorTickSpacing(1);
				slider.setMajorTickSpacing(5);
				slider.setSnapToTicks(true);
				slider.setPaintLabels(true);
				final Dictionary<Integer, JComponent> labels = new Hashtable<Integer, JComponent>();
				final Set<ParameterModel> paramSet = new HashSet<ParameterModel>();
				for (final Entry<Integer, Pair<ParameterModel, Object>> entry : selector
						.getValueMapping().entrySet()) {
					final JLabel label = new JLabel(entry.getValue().getRight()
							.toString());
					final ParameterModel key = entry.getValue().getLeft();
					paramSet.add(key);
					label.setBackground(possColors[paramSet.size()
							% possColors.length]);
					labels.put(entry.getKey(), label);
				}
				slider.setLabelTable(labels);
				slider.setBorder(new TitledBorder(selector.getParameters().get(
						0).getShortName()));
				// if (allChoiceCount > 1) {
				currentContainer.setLayout(new BorderLayout());
				currentContainer.add(slider, BorderLayout.NORTH);
				currentContainer.add(currentContainer = new JPanel(),
						BorderLayout.CENTER);
				slider.addChangeListener(new ChangeListener() {
					@Override
					public void stateChanged(final ChangeEvent e) {
						final int selected = slider.getModel().getValue();
						volatileModel.setSliderPosition(selectorSlider, Integer
								.valueOf(selected));
						setModel(dataModel);
					}
				});
			}
			final int rowCount = Math.max(1, possibleValueCount(sliders
					.get(Type.ScrollVertical)));
			final int colCount = Math.max(1, possibleValueCount(sliders
					.get(Type.ScrollHorisontal)));
			if (rowCount > 1 || colCount > 1) {
				final JPanel panel = new JPanel();
				panel.setPreferredSize(new Dimension(800, 600));
				final JScrollPane scrollPane = new JScrollPane(panel,
						ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
						ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
				currentContainer.add(scrollPane);
				scrollPane.add(panel);
				currentContainer = panel;
				currentContainer
						.addHierarchyBoundsListener(new HierarchyBoundsAdapter() {
							@Override
							public void ancestorResized(final HierarchyEvent e) {
								final Dimension dim = new Dimension(
										HeatmapPanel.this.getBounds().width,
										HeatmapPanel.this.getBounds().height);
								scrollPane.setSize(dim);
								scrollPane.getViewport().setPreferredSize(dim);
								scrollPane.getViewport().setViewSize(dim);
								scrollPane.repaint();
							}
						});
			}
			currentContainer.setLayout(new GridLayout(rowCount, colCount));

			// final Map<Object, Map<Object, Heatmap>> currentHeatMaps =
			// heatmaps
			// .get(allChoiceCount > 1 ? Integer.valueOf(slider.getModel()
			// .getValue() - 1) : null);
			for (int i = 0; i < rowCount; ++i) {
				final Integer iVal = Integer.valueOf(i);
				if (!heatmaps.containsKey(iVal)) {
					heatmaps.put(iVal, new HashMap<Integer, Heatmap>());
				}
				for (int j = 0; j < colCount; ++j) {
					final Map<Integer, Heatmap> heatmapCol = heatmaps.get(iVal);
					final Integer jVal = Integer.valueOf(j);
					final Heatmap heatmap = new Heatmap(model, dataModel);
					if (dataModel != null) {
						heatmap.setModel(dataModel, volatileModel, i, j);
					}
					heatmapCol.put(jVal, heatmap);
					currentContainer.add(heatmap);
				}
			}
		}

		private int possibleValueCount(final Collection<Slider> sliders) {
			int allCount = 0;
			for (final Slider slider : sliders) {
				// assert choice.getAggregateType() == null;
				// assert choice.getType().isDiscrete() : choice;
				allCount += slider.getValueMapping().size();
			}
			return allCount;
		}

		/**
		 * Updates the {@link HeatmapPanel} using the new {@link ViewModel}.
		 * 
		 * @param model
		 *            The new layout model.
		 */
		public void setViewModel(final ViewModel model) {
			model.getMain().getArrangementModel().removeListener(this);
			this.model = model;
			model.getMain().getArrangementModel().addListener(this);
			internalSetModel();
			revalidate();
		}

		private void setModel(final HeatmapNodeModel nodeModel,
				final VolatileModel volatileModel) {
			// FIXME the heatmaps should need only the selected values.
			volatileModel.addActionListener(this);
			this.dataModel = nodeModel;
			for (final Map.Entry<Integer, Map<Integer, Heatmap>> rowEntry : heatmaps
					.entrySet()) {
				for (final Map.Entry<Integer, Heatmap> heatmapEntry : rowEntry
						.getValue().entrySet()) {
					heatmapEntry.getValue().setModel(nodeModel, volatileModel,
							rowEntry.getKey().intValue(),
							heatmapEntry.getKey().intValue());
				}
			}
		}

		/**
		 * Sets the new data model. Updates the actual values of the
		 * visualisation parameters.
		 * 
		 * @param nodeModel
		 *            The new {@link HeatmapNodeModel}.
		 */
		public void setModel(final HeatmapNodeModel nodeModel) {
			volatileModel.setKeyToPlateAndPosition(nodeModel.getModelBuilder()
					.getKeyToPlateAndPosition());
			setModel(nodeModel, volatileModel);
		}

		/**
		 * @return The current {@link HiLiteListener}.
		 */
		public HiLiteListener getHiLiteListener() {
			return hiliteListener;
		}

		@Override
		public void actionPerformed(final ActionEvent e) {
			// internalSetModel();
			setModel(dataModel);
		}

		/**
		 * Adds a {@link MouseListener} to every {@link Heatmap}'s every well.
		 * 
		 * @param listener
		 *            A {@link MouseListener}.
		 */
		void addClickListenerToEveryWell(final MouseListener listener) {
			for (final Map<Integer, Heatmap> outer : heatmaps.values()) {
				for (final Heatmap heatmap : outer.values()) {
					heatmap.addClickListenerForEveryWell(listener);
				}
			}
		}
	}

	private static final ParameterModel defaultParamModel = new ParameterModel(
			"score", StatTypes.score, null, Collections.singletonList("score"), /* Collections.singletonList("plate") */
			Collections.<String> emptyList());
	private static final ParameterModel parameterParamModel = new ParameterModel(
			"parameters", StatTypes.parameter, null, Collections
					.singletonList("parameters"), Collections
					.<String> emptyList());
	private static final ParameterModel replicateParamModel = new ParameterModel(
			"replicate", StatTypes.replicate, null, Collections
					.singletonList("replicate"), Collections
					.<String> emptyList());
	private static final ParameterModel plateParamModel = new ParameterModel(
			"plate", StatTypes.plate, null, Collections.singletonList("plate"),
			Collections.singletonList("plate"));
	static {
		defaultParamModel.setEndColor(Color.GREEN);
		defaultParamModel.setStartColor(Color.RED);
		defaultParamModel.setMiddleColor(Color.BLACK);
		// defaultParamModel.setValueCount(3);
		// plateParamModel.setValueCount(8);
		// parameterParamModel.setValueCount(3);
		// replicateParamModel.setValueCount(3);
	}

	private final EnumMap<Format, EnumMap<Shape, ViewModel>> possibleViewModels = new EnumMap<Format, EnumMap<Shape, ViewModel>>(
			Format.class);
	private ViewModel currentViewModel = new ViewModel(Format._96,
			Shape.Circle, new ViewModel.OverviewModel(Collections
					.<ParameterModel> emptyList(), Collections
					.<ParameterModel> emptyList(), /*
													 * Collections .<ParameterModel>
													 * emptyList()
													 */Collections.singletonList(plateParamModel)),
			new ViewModel.ShapeModel(new ArrangementModel(), Collections
					.singletonList(parameterParamModel), Collections
					.singletonList(replicateParamModel), Collections
					.<ParameterModel> emptyList(), true));
	private final ViewModel currentViewModel_orig = new ViewModel(Format._96,
			Shape.Circle, new ViewModel.OverviewModel(Collections
					.<ParameterModel> emptyList(), Collections
					.<ParameterModel> emptyList(), /*
													 * Collections .<ParameterModel>
													 * emptyList()
													 */Collections.singletonList(plateParamModel)),
			new ViewModel.ShapeModel(new ArrangementModel(), Arrays.asList(
					defaultParamModel, defaultParamModel, defaultParamModel),
					Arrays.asList(defaultParamModel, defaultParamModel,
							defaultParamModel)/*
												 * Collections .<ParameterModel>
												 * emptyList()
												 */, Arrays.asList(defaultParamModel, defaultParamModel,
							defaultParamModel, defaultParamModel)/*
																	 * Collections.<ParameterModel>
																	 * emptyList()
																	 */, true));
	{
		for (final Format format : Format.values()) {
			possibleViewModels.put(format, new EnumMap<Shape, ViewModel>(
					Shape.class));
			for (final Shape shape : Shape.values()) {
				possibleViewModels.get(format).put(
						shape,
						new ViewModel(new ViewModel(currentViewModel, shape),
								format));
			}
		}
	}
	private final ControlPanel controlPanel = new ControlPanel(this);

	/** This class is responsible to hold the changing parameters. */
	static class VolatileModel implements Serializable, ActionListener {
		private static final long serialVersionUID = 6675568415910804477L;

		/**
		 * This enum describes the possible strategies to layout the
		 * {@link Heatmap}s.
		 */
		static enum MagnificationStrategy {
			/**
			 * The {@link Heatmap}s are all shown horizontally, till they fill
			 * the horizontal space.
			 */
			autoHorizontalAll,
			/** Only one {@link Heatmap} is shown horizontally. */
			autoHorizontalOne,
			/**
			 * All {@link Heatmap}s are shown vertivally, till they fill the
			 * vertical space
			 */
			autoVerticalAll,
			/** Only one {@link Heatmap} is shown vertically. */
			autoVerticalOne,
			/** The size is set with some buttons. */
			manual;
		};

		/** The magnification factor for the {@link Heatmap}s. */
		private double magnification;

		/** The strategy to find the {@link #magnification} value. */
		private MagnificationStrategy magStrategy;

		/** The plate's row and column positions at the upper left corner. */
		private int leftX, upperY;
		/** Plate/position hilite. (indices start from 0.) */
		private boolean[][] hilites;
		/** Plate/position selections. (indices start from 0.) */
		private boolean[][] selections;
		/** The current {@link Slider} positions. */
		private final Map<Slider, Integer> sliderPositions = new HashMap<Slider, Integer>();

		private final List<ActionListener> listeners = new ArrayList<ActionListener>();

		/** The actual {@link ArrangementModel} */
		private ArrangementModel arrangementModel;

		/**
		 * This {@link Map} tells that which {@link DataCell} represents which
		 * row's ids (rowId &Rarr; (plate, position)). The plate and position
		 * values are {@code 0}-based.
		 */
		private Map<DataCell, Pair<Integer, Integer>> keyToPlateAndPosition;

		/** The {@link HiLiteHandler} used. */
		private HiLiteHandler hiliteHandler;

		/**
		 * Constructs a {@link VolatileModel}.
		 */
		public VolatileModel() {
			super();
		}

		/**
		 * Sets the new row key &Rarr; (plate, position) {@link Map}. Every
		 * change will have effect on the {@link VolatileModel}, so please
		 * <b>do not</b> modify.
		 * 
		 * @param keyToPlateAndPosition
		 *            The new key &Rarr; (plate, position) {@link Map}.
		 */
		public void setKeyToPlateAndPosition(
				final Map<DataCell, Pair<Integer, Integer>> keyToPlateAndPosition) {
			this.keyToPlateAndPosition = keyToPlateAndPosition;
		}

		/**
		 * @return An unmodifiable {@link Map} of {@link Slider} positions.
		 */
		public Map<Slider, Integer> getSliderPositions() {
			return Collections.unmodifiableMap(sliderPositions);
		}

		/**
		 * Updates the {@code slider} to the new {@code value}.
		 * 
		 * @param slider
		 *            A {@link Slider}
		 * @param value
		 *            The new value of it.
		 */
		public void setSliderPosition(final Slider slider, final Integer value) {
			sliderPositions.put(slider, value);
			actionPerformed(new ActionEvent(slider, (int) (System
					.currentTimeMillis() & 0xffffffff), "slider value changed"));
		}

		/**
		 * Changes the actual values according to the new
		 * {@link ArrangementModel} and {@link HeatmapNodeModel data model}.
		 * 
		 * @param arrangementModel
		 *            The new {@link ArrangementModel}.
		 * @param nodeModel
		 *            The new {@link HeatmapNodeModel}.
		 */
		public void mutateValues(final ArrangementModel arrangementModel,
				final HeatmapNodeModel nodeModel) {
			// TODO Auto-generated method stub
			this.arrangementModel = arrangementModel;
			for (final Collection<Slider> sliderColl : arrangementModel
					.getSliders().values()) {
				for (final Slider slider : sliderColl) {
					setSliderPosition(slider, Integer.valueOf(1));
				}
			}
			final int plateCount = count(StatTypes.plate);
			hilites = new boolean[plateCount][384];
			final Set<DataCell> hilitKeys = nodeModel.getInHiLiteHandler(0)
					.getHiLitKeys();
			keyToPlateAndPosition = nodeModel.getModelBuilder()
					.getKeyToPlateAndPosition();
			setHilites(hilitKeys);
			selections = new boolean[plateCount][384];
		}

		/**
		 * HiLites or deHiLites a well on {@code plate} ({@code 0}-based) at
		 * {@code position} ({@code 0}-based).
		 * <p>
		 * This call <b>does not </b> repaint the well!
		 * 
		 * @param plate
		 *            A ({@code 0}-based) plate number.
		 * @param position
		 *            A ({@code 0}-based) position number.
		 * @param value
		 *            The new HiLite value for that well.
		 */
		void setHilite(final int plate, final int position, final boolean value) {
			hilites[plate][position] = value;
		}

		/**
		 * @param plate
		 *            A ({@code 0}-based) plate number.
		 * @return The HiLite values for that plate. This is modifiable, but
		 *         please <b>do not</b> modify it.
		 */
		boolean[] getHiliteValues(final int plate) {
			return hilites[plate];
		}

		/**
		 * Selects, or deselects a well on {@code plate} ({@code 0}-based) at
		 * {@code position} ({@code 0}-based).
		 * <p>
		 * This call <b>does not </b> repaint the well!
		 * 
		 * @param plate
		 *            A ({@code 0}-based) plate number.
		 * @param position
		 *            A ({@code 0}-based) position number.
		 * @param value
		 *            The new selection value for that well.
		 */
		void setSelection(final int plate, final int position,
				final boolean value) {
			selections[plate][position] = value;
		}

		/**
		 * UnHiLites every well. (Clears all HiLite.)
		 */
		public void unHiliteAll() {
			final int plateCount = count(StatTypes.plate);
			hilites = new boolean[plateCount][384];
			if (hiliteHandler != null) {
				hiliteHandler.fireClearHiLiteEvent();
			}
		}

		/**
		 * @param param
		 *            A {@link StatTypes}.
		 * @return The number of possible values for the {@code param}
		 *         {@link StatTypes} .
		 */
		private int count(final StatTypes param) {
			final Collection<Slider> sliders = arrangementModel
					.getMainArrangement().entrySet().iterator().next()
					.getValue();
			for (final Slider slider : sliders) {
				for (final ParameterModel parameters : slider.getParameters()) {
					if (parameters.getType() == param) {
						return slider.getValueMapping().size();
					}
				}
			}
			throw new IllegalStateException("Not found: " + param);
		}

		@Override
		public void actionPerformed(final ActionEvent e) {
			boolean hilite = false;
			boolean hiliteRelated = false;
			if (e.getActionCommand().equals(HiLiteHandler.UNHILITE_SELECTED)) {
				hiliteRelated = true;
			}
			if (e.getActionCommand().equals(HiLiteHandler.HILITE_SELECTED)) {
				hiliteRelated = hilite = true;
			}
			if (e.getActionCommand().equals(HiLiteHandler.CLEAR_HILITE)) {
				unHiliteAll();
			}
			if (hiliteRelated) {
				final Set<Pair<Integer, Integer>> hilitesChange = new HashSet<Pair<Integer, Integer>>();
				for (int i = 0; i < selections.length; i++) {
					for (int j = 0; j < selections[i].length; j++) {
						if (selections[i][j] && hilites[i][j] != hilite) {
							hilitesChange.add(new Pair<Integer, Integer>(i + 1,
									j));
							hilites[i][j] = hilite;
						}
					}
				}
				if (hiliteHandler != null) {
					final Set<DataCell> change = new HashSet<DataCell>();
					for (final Entry<DataCell, Pair<Integer, Integer>> entry : keyToPlateAndPosition
							.entrySet()) {
						if (hilitesChange.contains(entry.getValue())) {
							change.add(entry.getKey());
						}
					}
					if (hilite) {
						hiliteHandler.fireHiLiteEvent(change);
					} else {
						hiliteHandler.fireUnHiLiteEvent(change);
					}
				}
			}
			for (final ActionListener listener : listeners) {
				listener.actionPerformed(e);
			}
		}

		/**
		 * Adds a new {@link ActionListener} (if not yet contained).
		 * 
		 * @param listener
		 *            An {@link ActionListener}.
		 */
		public void addActionListener(final ActionListener listener) {
			if (!listeners.contains(listener)) {
				listeners.add(listener);
			}
		}

		/**
		 * Removes the {@link ActionListener}.
		 * 
		 * @param listener
		 *            An {@link ActionListener}.
		 */
		public void removeActionListener(final ActionListener listener) {
			listeners.remove(listener);
		}

		/**
		 * Clears all selections. (No repaint.)
		 */
		public void clearSelection() {
			selections = new boolean[selections.length][selections[0].length];
		}

		/**
		 * @param plate
		 *            A ({@code 0}-based) plate number.
		 * @return The selection values for that plate. This is modifiable, but
		 *         please <b>do not</b> modify it.
		 */
		public boolean[] getSelectionValues(final int plate) {
			return selections[plate];
		}

		/**
		 * @return The {@link HiLiteHandler}.
		 */
		public HiLiteHandler getHiliteHandler() {
			return hiliteHandler;
		}

		/**
		 * Sets the new {@link HiLiteHandler}.
		 * 
		 * @param hiliteHandler
		 *            A {@link HiLiteHandler}.
		 */
		public void setHiliteHandler(final HiLiteHandler hiliteHandler) {
			this.hiliteHandler = hiliteHandler;
		}

		/**
		 * HiLites the wells with {@code hiLitKeys} keys.
		 * 
		 * @param hiLitKeys
		 *            A {@link Set} of row ids.
		 */
		public void setHilites(final Set<DataCell> hiLitKeys) {
			for (final DataCell dataCell : hiLitKeys) {
				final Pair<Integer, Integer> pair = keyToPlateAndPosition
						.get(dataCell);
				hilites[pair.getLeft().intValue() - 1][pair.getRight()] = true;
			}
		}
	}

	private final VolatileModel volatileModel = new VolatileModel();

	/**
	 * Creates a new view.
	 * 
	 * @param nodeModel
	 *            The model (class: {@link HeatmapNodeModel})
	 */
	protected HeatmapNodeView(final NodeModel nodeModel) {
		super(nodeModel);
		final JMenu hiliteMenu = new JMenu(HiLiteHandler.HILITE);
		showHilite = new JCheckBoxMenuItem("Show HiLite visuals", true);
		hiliteMenu.add(showHilite);
		showOnlyHilited = new JCheckBoxMenuItem("Show only HiLited values",
				false);
		hiliteMenu.add(showOnlyHilited);
		hiliteMenu.add(new JMenuItem());
		hiliteSelected = new JMenuItem(HiLiteHandler.HILITE_SELECTED);
		hiliteMenu.add(hiliteSelected);
		unHiliteSelected = new JMenuItem(HiLiteHandler.UNHILITE_SELECTED);
		hiliteMenu.add(unHiliteSelected);
		unHiliteAll = new JMenuItem(HiLiteHandler.CLEAR_HILITE);
		hiliteMenu.add(unHiliteAll);
		getJMenuBar().add(hiliteMenu);
		table.getContentTable().addMouseListener(new MouseAdapter() {

			@Override
			public void mouseReleased(final MouseEvent e) {
				handle(e);
			}

			private void handle(final MouseEvent e) {
				if (e.isPopupTrigger()) {
					final JPopupMenu popupMenu = new JPopupMenu(
							HiLiteHandler.HILITE);
					final JMenu hiLiteMenu2 = table.createHiLiteMenu();
					for (final Component comp : hiLiteMenu2.getMenuComponents()) {
						popupMenu.add((JMenuItem) comp);
					}
					popupMenu.show(e.getComponent(), e.getX(), e.getY());
				}
			}

			@Override
			public void mousePressed(final MouseEvent e) {
				handle(e);
			}

		});

		final JMenu legendMenu = new JMenu("Legend");
		showColorsLegend = new JCheckBoxMenuItem("Show colors legend", true);
		legendMenu.add(showColorsLegend);
		showTooltipsLegend = new JCheckBoxMenuItem("Show tooltips legend", true);
		legendMenu.add(showTooltipsLegend);
		getJMenuBar().add(legendMenu);
		final JMenu testingMenu = new JMenu("Testing");
		testingForSelected = new JCheckBoxMenuItem(
				"Show only selected parameters", false);
		testingMenu.add(testingForSelected);
		getJMenuBar().add(testingMenu);

		final JSplitPane mainSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
				true);
		final JSplitPane upperSplit = new JSplitPane(
				JSplitPane.HORIZONTAL_SPLIT, false);
		mainSplit.setTopComponent(upperSplit);
		mainSplit.setOneTouchExpandable(true);

		// /Init the defaults.
		currentViewModel.getMain().getArrangementModel().mutate(
				((HeatmapNodeModel) nodeModel).getPossibleParameters());
		volatileModel.mutateValues(currentViewModel.getMain()
				.getArrangementModel(), (HeatmapNodeModel) nodeModel);
		controlPanel.updateControl(currentViewModel);

		heatmapPanel = new HeatmapPanel(getCurrentViewModel(), null,
				volatileModel);
		upperSplit.setTopComponent(heatmapPanel);
		legendPanel = new LegendPanel(false, getCurrentViewModel());
		legendPanel.setPreferredSize(new Dimension(200, 200));
		showColorsLegend.addActionListener(legendPanel);
		showTooltipsLegend.addActionListener(legendPanel);
		upperSplit.setBottomComponent(legendPanel);
		upperSplit.setOneTouchExpandable(true);
		heatmapPanel.setPreferredSize(new Dimension(600, 400));
		final JTabbedPane bottomTabs = new JTabbedPane();
		mainSplit.setBottomComponent(bottomTabs);
		settingsPanel = new SettingsPanel();
		bottomTabs.setPreferredSize(new Dimension(600, 200));
		bottomTabs.add("Controls", new JScrollPane(controlPanel));
		final ViewModel[] models = new ViewModel[Shape.values().length
				* Format.values().length];
		{
			int i = 0;
			for (final EnumMap<Shape, ViewModel> shapeModels : possibleViewModels
					.values()) {
				for (final ViewModel model : shapeModels.values()) {
					models[i++] = model;
				}
			}
		}
		infoControl = new InfoControl();
		infoControl.setViewModel(currentViewModel);
		bottomTabs.add("Info controls", infoControl);
		final JScrollPane infoScrollPane = new JScrollPane(infoTable);
		final JSplitPane infoSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
				infoScrollPane, table);
		infoSplit.setOneTouchExpandable(true);
		bottomTabs.add("Info", infoSplit);
		bottomTabs.add("Settings", settingsPanel);
		legendPanel2 = new LegendPanel(false, getCurrentViewModel());
		legendPanel2.setPreferredSize(new Dimension(200, 200));
		showColorsLegend.addActionListener(legendPanel2);
		showTooltipsLegend.addActionListener(legendPanel2);
		bottomTabs.add("Legend", legendPanel2);
		setComponent(mainSplit);
		nodeModel.getInHiLiteHandler(0).addHiLiteListener(
				heatmapPanel.getHiLiteListener());
		// heatmapPanel.hiliteListener.hiLite(new KeyEvent(this, hilitKeys));
		hiliteSelected.addActionListener(heatmapPanel);
		hiliteSelected.addActionListener(volatileModel);
		unHiliteSelected.addActionListener(heatmapPanel);
		unHiliteSelected.addActionListener(volatileModel);
		unHiliteAll.addActionListener(heatmapPanel);
		unHiliteAll.addActionListener(volatileModel);
		volatileModel.addActionListener(legendPanel);
		volatileModel.addActionListener(legendPanel2);
		setCurrentViewModel(currentViewModel);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void modelChanged() {
		// TODO retrieve the new model from your nodemodel and
		// update the view.
		final HeatmapNodeModel nodeModel = getNodeModel();
		assert nodeModel != null;
		volatileModel.setHiliteHandler(nodeModel.getInHiLiteHandler(0));

		for (final EnumMap<Shape, ViewModel> shapeBasedModels : possibleViewModels
				.values()) {
			for (final Entry<Shape, ViewModel> entry : shapeBasedModels
					.entrySet()) {
				final ParameterModel parameterModel = new ParameterModel(
						"plate", StatTypes.plate, null, Collections
								.<String> emptyList(), Collections
								.<String> emptyList());
				shapeBasedModels.put(entry.getKey(), new ViewModel(entry
						.getValue(), new OverviewModel(Collections
						.<ParameterModel> emptyList(), Collections
						.<ParameterModel> emptyList(), Collections
						.singletonList(parameterModel))));
			}
		}
		controlPanel.setModel(nodeModel);
		heatmapPanel.setModel(nodeModel);
		table.setDataTable(nodeModel.getTable());
		table.setHiLiteHandler(nodeModel.getInHiLiteHandler(0));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onClose() {
		// TODO things to do when closing the view
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onOpen() {
		// TODO things to do when opening the view
		// heatmapPanel.applyHiLiting();
		// final Set<DataCell> hilitKeys = heatmapPanel.dataModel
		// .getInHiLiteHandler(0).getHiLitKeys();
		// heatmapPanel.hiliteListener.hiLite(new KeyEvent(this, hilitKeys));
		modelChanged();
	}

	/**
	 * @return The actual {@link ViewModel}.
	 */
	public ViewModel getCurrentViewModel() {
		return currentViewModel;
	}

	/**
	 * Updates the current {@link ViewModel}.
	 * 
	 * @param currentViewModel
	 *            The new {@link ViewModel}.
	 */
	public void setCurrentViewModel(final ViewModel currentViewModel) {
		this.currentViewModel = currentViewModel;
		infoControl.setViewModel(currentViewModel);
		controlPanel.setViewModel(currentViewModel);
		heatmapPanel.setViewModel(currentViewModel);
		heatmapPanel.addClickListenerToEveryWell(new MouseAdapter() {
			@Override
			public void mouseClicked(final MouseEvent e) {
				infoTable.setText(((WellViewPanel) e.getSource())
						.getToolTipText());
			}
		});
		currentViewModel.addActionListener(heatmapPanel);
		legendPanel.setViewModel(currentViewModel);
		legendPanel2.setViewModel(currentViewModel);
	}

	/**
	 * Changes the actual {@link ViewModel} according to the parameters.
	 * 
	 * @param format
	 *            The plate format.
	 * @param shape
	 *            The shape of wells.
	 */
	public void changeView(final Format format, final Shape shape) {
		setCurrentViewModel(possibleViewModels.get(format).get(shape));
	}

	/**
	 * @return The current model for frequently changing parameters.
	 */
	public VolatileModel getVolatileModel() {
		return volatileModel;
	}

	/**
	 * @return The actual data model.
	 */
	@Override
	public HeatmapNodeModel getNodeModel() {
		return (HeatmapNodeModel) super.getNodeModel();
	}
}
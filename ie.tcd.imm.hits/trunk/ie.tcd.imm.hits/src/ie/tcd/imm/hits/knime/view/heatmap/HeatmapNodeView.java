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

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
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

/**
 * <code>NodeView</code> for the "Heatmap" Node. Shows the heatmap of the
 * plates.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
public class HeatmapNodeView extends NodeView {
	private final LegendPanel legendPanel;
	private final LegendPanel legendPanel2;
	// private final JTable infoTable = new JTable(1, 1);
	private final JLabel infoTable = new JLabel("Click on a well.");
	private final SettingsPanel settingsPanel;
	private final InfoControl infoControl = new InfoControl();
	private final JCheckBoxMenuItem showHilite;
	private final JCheckBoxMenuItem showOnlyHilited;
	private final JMenuItem unHiliteSelected;
	private final JMenuItem unHiliteAll;
	private final JMenuItem hiliteSelected;
	private final JCheckBoxMenuItem showColorsLegend;
	private final JCheckBoxMenuItem showTooltipsLegend;
	private final JCheckBoxMenuItem testingForSelected;
	private final HeatmapPanel heatmapPanel;

	private static class HeatmapPanel extends JPanel implements ActionListener {
		private static final long serialVersionUID = 6589091201002870311L;
		private final Map<Integer, Map<Integer, Heatmap>> heatmaps = new HashMap<Integer, Map<Integer, Heatmap>>();
		private ViewModel model;
		private HeatmapNodeModel dataModel;
		private final HiLiteListener hiliteListener;
		private final VolatileModel volatileModel;

		public HeatmapPanel(final ViewModel model,
				final HeatmapNodeModel dataModel,
				final VolatileModel volatileModel) {
			super();
			this.model = model;
			this.dataModel = dataModel;
			this.volatileModel = volatileModel;
			internalSetModel();
			hiliteListener = new HiLiteListener() {

				@Override
				public void unHiLiteAll() {
					for (final Map<Integer, Heatmap> map : heatmaps.values()) {
						for (final Heatmap heatmap : map.values()) {
							heatmap.unHiLiteAll();
						}
					}
				}

				@Override
				public void unHiLite(final KeyEvent event) {
					for (final Map<Integer, Heatmap> map : heatmaps.values()) {
						for (final Heatmap heatmap : map.values()) {
							heatmap.unHiLite(event);
						}
					}
				}

				@Override
				public void hiLite(final KeyEvent event) {
					for (final Map<Integer, Heatmap> map : heatmaps.values()) {
						for (final Heatmap heatmap : map.values()) {
							heatmap.hiLite(event);
						}
					}
				}

			};
		}

		private void internalSetModel() {
			removeAll();
			heatmaps.clear();
			final OverviewModel overview = model.getOverview();
			final List<ParameterModel> choiceModel = overview.getChoiceModel();
			// TODO use the proper value assigned to
			final Map<Type, Collection<Slider>> sliders = model.getMain()
					.getArrangementModel().getSliders();
			// final boolean notEmpty = sliders.containsKey(Type.Selector);
			final int allChoiceCount = volatileModel.count(StatTypes.plate);
			// notEmpty
			// && sliders.get(Type.Selector).iterator().hasNext() ?
			// possibleValueCount(sliders
			// .get(Type.Selector).iterator().next().getParameters())
			// : 1;// model.getMain().getArrangementModel()
			// .getTypeValuesMap().get(StatTypes.plate).size(); //
			possibleValueCount(choiceModel);
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
				for (final Entry<Integer, Map<ParameterModel, Object>> entry : selector
						.getValueMapping().entrySet()) {
					final JLabel label = new JLabel(entry.getValue().values()
							.iterator().next().toString());
					final ParameterModel key = entry.getValue().entrySet()
							.iterator().next().getKey();
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
			// for (int i = 0; i < allChoiceCount; ++i) {
			// heatmaps.put(Integer.valueOf(i),
			// new HashMap<Object, Map<Object, Heatmap>>());
			// }
			// for (final ParameterModel parameterModel : choiceModel) {
			// for (final Object key : parameterModel.getColorLegend()
			// .keySet()) {
			// heatmaps.put(Collections.singletonMap(parameterModel,
			// key),
			// new HashMap<Object, Map<Object, Heatmap>>());
			// }
			// }
			// } else {
			// heatmaps.put(null, new HashMap<Object, Map<Object,
			// Heatmap>>());
			// }
			final int rowCount = Math.max(1, possibleValueCount(overview
					.getRowModel()));
			final int colCount = Math.max(1, possibleValueCount(overview
					.getColModel()));
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

		private int possibleValueCount(final List<ParameterModel> model) {
			int allCount = 0;
			for (final ParameterModel choice : model) {
				assert choice.getAggregateType() == null;
				assert choice.getType().isDiscrete() : choice;
				allCount += choice.getValueCount();
			}
			return allCount;
		}

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

		public void setModel(final HeatmapNodeModel nodeModel) {
			volatileModel
					.setKeyToPlateAndPosition(nodeModel.keyToPlateAndPosition);
			setModel(nodeModel, volatileModel);
		}

		public HiLiteListener getHiLiteListener() {
			return hiliteListener;
		}

		@Override
		public void actionPerformed(final ActionEvent e) {
			// internalSetModel();
			setModel(dataModel);
		}

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
		plateParamModel.setValueCount(8);
		parameterParamModel.setValueCount(3);
		replicateParamModel.setValueCount(3);
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

		static enum MagnificationStrategy {
			autoHorisontalAll, autoHorisontalOne, autoVerticalAll, autoVerticalOne, manual;
		};

		private double magnification;

		private MagnificationStrategy magStrategy;

		private double leftX, upperY;
		private boolean[][] hilites;// plate/position???
		private boolean[][] selections; // plate/position???
		private final Map<Slider, Integer> sliderPositions = new HashMap<Slider, Integer>();

		private final List<ActionListener> listeners = new ArrayList<ActionListener>();

		private ArrangementModel arrangementModel;

		private Map<DataCell, Pair<Integer, Integer>> keyToPlateAndPosition;

		private HiLiteHandler hiliteHandler;

		public VolatileModel() {
			super();
			// TODO Auto-generated constructor stub
		}

		public void setKeyToPlateAndPosition(
				final Map<DataCell, Pair<Integer, Integer>> keyToPlateAndPosition) {
			this.keyToPlateAndPosition = keyToPlateAndPosition;
		}

		public Map<Slider, Integer> getSliderPositions() {
			return Collections.unmodifiableMap(sliderPositions);
		}

		public void setSliderPosition(final Slider slider, final Integer value) {
			sliderPositions.put(slider, value);
			actionPerformed(new ActionEvent(slider, (int) (System
					.currentTimeMillis() & 0xffffffff), "slider value changed"));
		}

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
			keyToPlateAndPosition = nodeModel.keyToPlateAndPosition;
			setHilites(hilitKeys);
			selections = new boolean[plateCount][384];
		}

		void setHilite(final int plate, final int position, final boolean value) {
			hilites[plate][position] = value;
		}

		boolean[] getHiliteValues(final int plate) {
			return hilites[plate];
		}

		void setSelection(final int plate, final int position,
				final boolean value) {
			selections[plate][position] = value;
		}

		public void unHiliteAll() {
			final int plateCount = count(StatTypes.plate);
			hilites = new boolean[plateCount][384];
			if (hiliteHandler != null) {
				hiliteHandler.fireClearHiLiteEvent();
			}
		}

		private int count(final StatTypes param) {
			final Collection<Slider> sliders = arrangementModel
					.getMainArrangement().entrySet().iterator().next()
					.getValue();
			for (final Slider slider : sliders) {
				for (final ParameterModel parameters : slider.getParameters()) {
					if (parameters.getType() == param) {
						return parameters.getValueCount();
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

		public void addActionListener(final ActionListener listener) {
			if (!listeners.contains(listener)) {
				listeners.add(listener);
			}
		}

		public void removeActionListener(final ActionListener listener) {
			listeners.remove(listener);
		}

		public void clearSelection() {
			selections = new boolean[selections.length][selections[0].length];
		}

		public boolean[] getSelectionValues(final int plate) {
			return selections[plate];
		}

		public HiLiteHandler getHiliteHandler() {
			return hiliteHandler;
		}

		public void setHiliteHandler(final HiLiteHandler hiliteHandler) {
			this.hiliteHandler = hiliteHandler;
		}

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
				((HeatmapNodeModel) nodeModel).possibleParameters);
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
		bottomTabs.add("Controls", controlPanel);
		bottomTabs.add("Info controls", infoControl);
		bottomTabs.add("Info", new JScrollPane(infoTable));
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
				parameterModel.setValueCount(nodeModel.scoreValues.size());
				shapeBasedModels.put(entry.getKey(), new ViewModel(entry
						.getValue(), new OverviewModel(Collections
						.<ParameterModel> emptyList(), Collections
						.<ParameterModel> emptyList(), Collections
						.singletonList(parameterModel))));
				// model.getOverview().getChoiceModel().set(0, parameterModel);
			}
		}
		controlPanel.setModel(nodeModel);
		heatmapPanel.setModel(nodeModel);
		heatmapPanel.addClickListenerToEveryWell(new MouseAdapter() {

			@Override
			public void mouseClicked(final MouseEvent e) {
				infoTable.setText(((WellViewPanel) e.getSource())
						.getToolTipText());
			}
		});
		// final Set<DataCell> hilitKeys = nodeModel.getInHiLiteHandler(0)
		// .getHiLitKeys();
		// heatmapPanel.hiliteListener.hiLite(new KeyEvent(this, hilitKeys));
		// be aware of a possibly not executed nodeModel! The data you retrieve
		// from your nodemodel could be null, emtpy, or invalid in any kind.

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

	}

	public ViewModel getCurrentViewModel() {
		return currentViewModel;
	}

	public void setCurrentViewModel(final ViewModel currentViewModel) {
		this.currentViewModel = currentViewModel;
		controlPanel.setViewModel(currentViewModel);
		heatmapPanel.setViewModel(currentViewModel);
		legendPanel.setViewModel(currentViewModel);
		legendPanel2.setViewModel(currentViewModel);
	}

	public void changeView(final Format format, final Shape shape) {
		setCurrentViewModel(possibleViewModels.get(format).get(shape));
	}

	public VolatileModel getVolatileModel() {
		return volatileModel;
	}

	@Override
	public HeatmapNodeModel getNodeModel() {
		return (HeatmapNodeModel) super.getNodeModel();
	}
}

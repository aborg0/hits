/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package ie.tcd.imm.hits.knime.view.heatmap;

import ie.tcd.imm.hits.common.Format;
import ie.tcd.imm.hits.knime.util.ModelBuilder;
import ie.tcd.imm.hits.knime.util.SimpleModelBuilder;
import ie.tcd.imm.hits.knime.view.ControlsHandler;
import ie.tcd.imm.hits.knime.view.ExportImages;
import ie.tcd.imm.hits.knime.view.SplitType;
import ie.tcd.imm.hits.knime.view.heatmap.ControlPanel.ArrangementModel;
import ie.tcd.imm.hits.knime.view.heatmap.HeatmapNodeModel.StatTypes;
import ie.tcd.imm.hits.knime.view.heatmap.SliderModel.Type;
import ie.tcd.imm.hits.knime.view.heatmap.ViewModel.OverviewModel;
import ie.tcd.imm.hits.knime.view.heatmap.ViewModel.ParameterModel;
import ie.tcd.imm.hits.knime.view.heatmap.ViewModel.Shape;
import ie.tcd.imm.hits.knime.view.impl.ControlsHandlerKNIMEFactory;
import ie.tcd.imm.hits.knime.view.impl.ControlsHandlerKNIMEFactory.ArrangementEvent;
import ie.tcd.imm.hits.util.Pair;
import ie.tcd.imm.hits.util.Traversable;
import ie.tcd.imm.hits.util.swing.ImageType;
import ie.tcd.imm.hits.util.swing.VariableControl.ControlTypes;
import ie.tcd.imm.hits.util.swing.colour.ColourSelector;
import ie.tcd.imm.hits.util.swing.colour.ComplexModel;
import ie.tcd.imm.hits.util.swing.colour.ColourSelector.RangeType;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.HierarchyBoundsAdapter;
import java.awt.event.HierarchyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.Callable;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JViewport;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.data.DataCell;
import org.knime.core.data.RowKey;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeView;
import org.knime.core.node.defaultnodesettings.SettingsModel;
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
public class HeatmapNodeView extends NodeView<HeatmapNodeModel> {
	/**
	 * An action to save all possible views of plates in the experiment.
	 */
	public class ExportAllAction extends ExportImages {
		private static final long serialVersionUID = 7782954593253996267L;
		/** The default action description. */
		public static final String EXPORT_ALL = "Export all...";

		private JViewport viewport;
		private String fileName;

		/**
		 * Creates the action with value of {@link #EXPORT_ALL} default
		 * description ({@value #EXPORT_ALL}).
		 * 
		 * @see ExportAllAction#ExportAllAction(String)
		 */
		public ExportAllAction() {
			this(EXPORT_ALL);
		}

		/**
		 * @param name
		 *            The description of the action.
		 * @see AbstractAction#AbstractAction(String)
		 */
		public ExportAllAction(final String name) {
			this(name, null);
		}

		/**
		 * @param name
		 *            The description of the action.
		 * @param icon
		 *            The {@link Icon} associated to the {@link ExportAllAction}
		 *            instance.
		 */
		public ExportAllAction(final String name, @Nullable final Icon icon) {
			this(name, icon, ImageType.png);
		}

		/**
		 * @param name
		 *            The description of the action.
		 * @param icon
		 *            The {@link Icon} associated to the {@link ExportAllAction}
		 *            instance.
		 * @param type
		 *            Format of saved images.
		 */
		ExportAllAction(final String name, @Nullable final Icon icon,
				final ImageType type) {
			super(name, icon, type);
		}

		@Override
		protected JComponent createAdditionalControls() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		protected Traversable<JComponent, String> createTraversable(
				final String folderName, final int width, final int height) {
			return new Traversable<JComponent, String>() {

				@Override
				public void traverse(final Callable<?> callable) {
					final List<ParameterModel> primerParameters = getCurrentViewModel()
							.getMain().getPrimerParameters();
					final List<ParameterModel> seconderParameters = getCurrentViewModel()
							.getMain().getSeconderParameters();
					final Set<SliderModel> sliderModels = getCurrentViewModel()
							.getMain().getArrangementModel().getSliderModels();
					final List<SliderModel> others = new ArrayList<SliderModel>(
							sliderModels.size());
					final Map<StatTypes, Integer> origSelections = new EnumMap<StatTypes, Integer>(
							StatTypes.class);
					for (final SliderModel sliderModel : sliderModels) {
						if (primerParameters.get(0).getType() != sliderModel
								.getParameters().get(0).getType()
								&& (seconderParameters.size() == 0 || seconderParameters
										.get(0).getType() != sliderModel
										.getParameters().get(0).getType())) {
							others.add(sliderModel);
							assert sliderModel.getSelections().size() == 1 : sliderModel
									.getSelections();
							origSelections.put(sliderModel.getParameters().get(
									0).getType(), sliderModel.getSelections()
									.iterator().next());
						}
					}
					final boolean[] stopped = new boolean[1];
					final boolean madeDirs = new File(folderName).mkdirs();
					assert madeDirs || !madeDirs;
					saveImages(stopped, callable, width, height, folderName,
							others, 0);
				}

				@Override
				public String getState() {
					return fileName;
				}

				@Override
				public JComponent getElement() {
					return viewport;
				}
			};
		}

		@Override
		protected void setupComponent(
				final Traversable<JComponent, String> traversable) {
			scrollPane.setViewport((JViewport) traversable.getElement());
		}

		/**
		 * Saves the images with all possible different parameter values.
		 * 
		 * @param stopped
		 *            The stopped pointer
		 * @param callable
		 *            The {@link Callable} for the drawing.
		 * @param w
		 *            The width of the image.
		 * @param h
		 *            The height of the image.
		 * @param folderName
		 *            The name of the destination folder.
		 * @param others
		 *            The non-primary, non-secondary {@link SliderModel}s.
		 * @param actual
		 *            The actually modified {@link SliderModel} in {@code
		 *            others}.
		 */
		private void saveImages(final boolean[] stopped,
				final Callable<?> callable, final int w, final int h,
				final String folderName, final List<SliderModel> others,
				final int actual) {
			if (stopped[0]) {
				return;
			}
			if (actual < others.size()) {
				final SliderModel slider = others.get(actual);
				final Integer original = slider.getSelections().iterator()
						.next();
				for (final Integer key : slider.getValueMapping().keySet()) {
					if (!stopped[0]) {
						slider.selectSingle(key);
						saveImages(stopped, callable, w, h, folderName, others,
								actual + 1);
					}
				}
				slider.selectSingle(original);
			} else {
				final Runnable paint = new Runnable() {
					@Override
					public void run() {
						final HeatmapPanel hp = new HeatmapPanel(
								HeatmapNodeView.this.getCurrentViewModel(),
								HeatmapNodeView.this.getNodeModel(),
								HeatmapNodeView.this.getVolatileModel());
						scrollPane.setViewportView(hp.heatmapsPanel);
						viewport = scrollPane.getViewport();
						viewport.setPreferredSize(new Dimension(w, h));
						viewport.setMinimumSize(new Dimension(w, h));
						viewport.setMaximumSize(new Dimension(w, h));
						hp.setModel(getNodeModel());
						hp.heatmapsPanel.setSize(w, h);
						hp.heatmapsPanel.setBounds(0, 0, w, h);
					}
				};
				if (SwingUtilities.isEventDispatchThread()) {
					paint.run();
				} else {
					try {
						SwingUtilities.invokeAndWait(paint);
					} catch (final InterruptedException e1) {
						JOptionPane.showMessageDialog(null, "Interrupted",
								"Interrupted", JOptionPane.ERROR_MESSAGE);
					} catch (final InvocationTargetException e1) {
						JOptionPane.showMessageDialog(null, "Interrupted",
								"Interrupted", JOptionPane.ERROR_MESSAGE);
					}
				}
				final StringBuilder fileNameBuilder = new StringBuilder();
				for (final SliderModel sliderModel : others) {
					fileNameBuilder.append(sliderModel.getParameters().get(0)
							.getShortName());
					fileNameBuilder.append("-").append(
							sliderModel.getValueMapping().get(
									sliderModel.getSelections().iterator()
											.next()).getRight().toString()
									.replaceAll("[^\\d\\w]+", "_")).append(" ");
				}
				// fileNameBuilder.append(".png");
				fileName = fileNameBuilder.toString();
				try {
					callable.call();
				} catch (final RuntimeException e) {
					logger.warn("Problem: " + e.getMessage(), e);
					throw e;
				} catch (final Exception e) {
					logger.warn("Problem: " + e.getMessage(), e);
					throw new RuntimeException(e);
				}
			}
		}
	}

	private static final NodeLogger logger = NodeLogger
			.getLogger(HeatmapNodeView.class);

	// private static final ExecutorService executor = new ThreadPoolExecutor(1,
	// 1, 1000, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(1));
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
	private final HeatmapPanel heatmapPanel;
	private final ColourSelector colourSelector = new ColourSelector(
			Collections.<String> emptyList(), Collections
					.<StatTypes> emptyList());

	private final ControlsHandler<? extends SettingsModel, SliderModel> controlsHandler = new ControlsHandlerKNIMEFactory();

	/**
	 * This {@link HiLiteListener} updates the {@link HeatmapPanel} on changes
	 * of {@link HeatmapPanel}.
	 */
	private static class HeatmapPanel extends JPanel implements ActionListener {
		private static final long serialVersionUID = 6589091201002870311L;

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
			public void unHiLiteAll(final KeyEvent event) {
				for (final Map<Integer, Heatmap> map : heatmap.heatmaps
						.values()) {
					for (final Heatmap heatmap : map.values()) {
						heatmap.unHiLiteAll(event);
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

		private final Map<Integer, Map<Integer, Heatmap>> heatmaps = new HashMap<Integer, Map<Integer, Heatmap>>();
		private ViewModel model;
		private @Nullable
		HeatmapNodeModel dataModel;
		private final HeatmapHiLiteListener hiliteListener;
		private final VolatileModel volatileModel;
		private final JPanel controlPanel = new JPanel();
		private final JPanel heatmapsPanel = new JPanel();

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
		public HeatmapPanel(final ViewModel model,
				@Nullable final HeatmapNodeModel dataModel,
				final VolatileModel volatileModel) {
			super();
			controlPanel.setName(PositionConstants.upper.name());
			setLayout(new BorderLayout());
			add(controlPanel, BorderLayout.NORTH);
			add(heatmapsPanel, BorderLayout.CENTER);
			this.model = model;
			this.dataModel = dataModel;
			this.volatileModel = volatileModel;
			internalSetModel();
			hiliteListener = new HeatmapHiLiteListener(this);
		}

		private void internalSetModel() {
			heatmapsPanel.removeAll();
			heatmaps.clear();
			final Map<Type, Collection<SliderModel>> sliders = model.getMain()
					.getArrangementModel().getSliders();
			Container currentContainer = heatmapsPanel;
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

		private int possibleValueCount(final Collection<SliderModel> sliders) {
			int allCount = 0;
			for (final SliderModel slider : sliders) {
				allCount += slider.getSelections().size();
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
			"score", StatTypes.score, null, Collections.singletonList("score"), /*
																				 * Collections.
																				 * singletonList
																				 * (
																				 * "plate"
																				 * )
																				 */
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
	}

	private final EnumMap<Format, EnumMap<Shape, ViewModel>> possibleViewModels = new EnumMap<Format, EnumMap<Shape, ViewModel>>(
			Format.class);
	private ViewModel currentViewModel = new ViewModel(Format._96,
			Shape.Circle, new ViewModel.OverviewModel(Collections
					.<ParameterModel> emptyList(), Collections
					.<ParameterModel> emptyList(), Collections
					.singletonList(plateParamModel)), new ViewModel.ShapeModel(
					new ArrangementModel(), Collections
							.singletonList(parameterParamModel), Collections
							.singletonList(replicateParamModel), Collections
							.<ParameterModel> emptyList(), true));
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
	private final VolatileModel volatileModel = new VolatileModel();

	private final ChangeListener changeListenerForControlsHandler;

	private final ControlPanel controlPanel = new ControlPanel(this);

	private final JSplitPane mainSplit;

	/** This class is responsible to hold the changing parameters. */
	static class VolatileModel implements Serializable, ActionListener {
		private static final long serialVersionUID = 6675568415910804477L;
		/** The prefix for the show colour legend action. */
		public static final String SHOW_COLOUR_LEGEND_PREFIX = "show colour legend: ";
		/** The prefix for the show only HiLites action. */
		public static final String SHOW_ONLY_HILITES_PREFIX = "show only HiLites: ";
		/** The prefix for the show HiLites action. */
		public static final String SHOW_HILITES_PREFIX = "show HiLites: ";

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

		/** Shows the hiliting background. */
		private boolean showHiLites = true;

		/** Shows only the hilited wells (if any). */
		private boolean showOnlyHiLites = false;

		/** The plate's row and column positions at the upper left corner. */
		private int leftX, upperY;
		/** Plate/position hilite. (indices start from 0.) */
		private boolean[][] hilites;
		/** Plate/position selections. (indices start from 0.) */
		private boolean[][] selections;
		/** The current {@link SliderModel} positions. */
		private final Map<SliderModel, Integer> sliderPositions = new HashMap<SliderModel, Integer>();

		private final List<ActionListener> listeners = new ArrayList<ActionListener>();

		/** The actual {@link ArrangementModel} */
		private ArrangementModel arrangementModel;

		/**
		 * This {@link Map} tells that which {@link DataCell} represents which
		 * row's ids (rowId &Rarr; (plate, position)). The plate and position
		 * values are {@code 0}-based.
		 */
		private Map<String, Pair<Integer, Integer>> keyToPlateAndPosition;

		/** The {@link HiLiteHandler} used. */
		private HiLiteHandler hiliteHandler;

		/** The visibility of colour legend property. */
		private boolean showColourLegend = true;

		/**
		 * Constructs a {@link VolatileModel}.
		 */
		public VolatileModel() {
			super();
		}

		/**
		 * Sets the new row key &Rarr; (plate, position) {@link Map}. Every
		 * change will have effect on the {@link VolatileModel}, so please <b>do
		 * not</b> modify.
		 * 
		 * @param keyToPlateAndPosition
		 *            The new key &Rarr; (plate, position) {@link Map}.
		 */
		public void setKeyToPlateAndPosition(
				final Map<String, Pair<Integer, Integer>> keyToPlateAndPosition) {
			this.keyToPlateAndPosition = keyToPlateAndPosition;
		}

		/**
		 * @return An unmodifiable {@link Map} of {@link SliderModel} positions.
		 */
		public Map<SliderModel, Integer> getSliderPositions() {
			return Collections.unmodifiableMap(sliderPositions);
		}

		/**
		 * Updates the {@code slider} to the new {@code value}.
		 * 
		 * @param slider
		 *            A {@link SliderModel}
		 * @param value
		 *            The new value of it.
		 */
		public void setSliderPosition(final SliderModel slider,
				final Integer value) {
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
			this.arrangementModel = arrangementModel;
			for (final Entry<Type, Collection<SliderModel>> entry : arrangementModel
					.getSliders().entrySet()) {
				switch (entry.getKey()) {
				case ScrollHorisontal:
				case ScrollVertical:
				case Splitter:
					// Leave the selections.
					break;
				case Hidden:
				case Selector:
					for (final SliderModel m : entry.getValue()) {
						if (m.getSelections().size() > 1) {
							m.selectSingle(m.getSelections().iterator().next());
						}
					}
					break;
				default:
					throw new UnsupportedOperationException(
							"Not supported type: " + entry.getKey());
				}
			}
			final int plateCount = count(StatTypes.plate);
			hilites = new boolean[plateCount][384];
			final Set<RowKey> hilitKeys = nodeModel.getInHiLiteHandler(0)
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
			final Collection<SliderModel> sliders = arrangementModel
					.getMainArrangement().entrySet().iterator().next()
					.getValue();
			for (final SliderModel slider : sliders) {
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
					final Set<RowKey> change = new HashSet<RowKey>();
					for (final Entry<String, Pair<Integer, Integer>> entry : keyToPlateAndPosition
							.entrySet()) {
						if (hilitesChange.contains(entry.getValue())) {
							change.add(new RowKey(entry.getKey()));
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
		public void setHilites(final Set<RowKey> hiLitKeys) {
			for (final RowKey dataCell : hiLitKeys) {
				final String string = dataCell.getString();
				final Pair<Integer, Integer> pair = keyToPlateAndPosition
						.containsKey(string) ? keyToPlateAndPosition
						.get(string)
						: string.contains("_") ? keyToPlateAndPosition
								.get(string.substring(0, string.indexOf('_')))
								: null;
				if (pair != null) {
					hilites[pair.getLeft().intValue() - 1][pair.getRight()] = true;
				}
			}
		}

		/**
		 * @return the showColourLegend
		 */
		public boolean isShowColourLegend() {
			return showColourLegend;
		}

		/**
		 * @param showColourLegend
		 *            the showColourLegend to set
		 */
		public void setShowColourLegend(final boolean showColourLegend) {
			if (this.showColourLegend != showColourLegend) {
				this.showColourLegend = showColourLegend;
				actionPerformed(new ActionEvent(this, (int) (System
						.currentTimeMillis() & 0xffffffffL),
						SHOW_COLOUR_LEGEND_PREFIX + showColourLegend));
			}
		}

		/**
		 * @return Whether or not show the HiLite background.
		 */
		public boolean isShowHiLites() {
			return showHiLites;
		}

		/**
		 * Sets the showHiLites property to {@code showHiLites}.
		 * 
		 * @param showHiLites
		 *            The new value of showHiLites property.
		 */
		public void setShowHiLites(final boolean showHiLites) {
			if (showHiLites != this.showHiLites) {
				this.showHiLites = showHiLites;
				actionPerformed(new ActionEvent(this, (int) (System
						.currentTimeMillis() & 0xffffffffL),
						SHOW_HILITES_PREFIX + showHiLites));
			}
		}

		/**
		 * @return if {@code true} only the HiLited values are shown.
		 */
		public boolean isShowOnlyHiLites() {
			return showOnlyHiLites;
		}

		/**
		 * Sets the showOnlyHiLites property to {@code showOnlyHiLites}.
		 * 
		 * @param showOnlyHiLites
		 *            The new value of showOnlyHiLites property.
		 */
		public void setShowOnlyHiLites(final boolean showOnlyHiLites) {
			if (showOnlyHiLites != this.showOnlyHiLites) {
				this.showOnlyHiLites = showOnlyHiLites;
				actionPerformed(new ActionEvent(this, (int) (System
						.currentTimeMillis() & 0xffffffffL),
						SHOW_ONLY_HILITES_PREFIX + showOnlyHiLites));
			}
		}
	}

	/**
	 * Creates a new view.
	 * 
	 * @param nodeModel
	 *            The model (class: {@link HeatmapNodeModel})
	 */
	protected HeatmapNodeView(final HeatmapNodeModel nodeModel) {
		super(nodeModel);
		final JMenu hiliteMenu = new JMenu(HiLiteHandler.HILITE);
		showHilite = new JCheckBoxMenuItem("Show HiLite visuals", true);
		showHilite.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				getVolatileModel().setShowHiLites(
						showHilite.getModel().isSelected());
			}
		});
		// hiliteMenu.add(showHilite);
		showOnlyHilited = new JCheckBoxMenuItem("Show only HiLited values",
				false);
		showOnlyHilited.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				getVolatileModel().setShowOnlyHiLites(
						showOnlyHilited.getModel().isSelected());
			}
		});
		// hiliteMenu.add(showOnlyHilited);
		// hiliteMenu.add(new JMenuItem());
		hiliteSelected = new JMenuItem(HiLiteHandler.HILITE_SELECTED);
		hiliteMenu.add(hiliteSelected);
		unHiliteSelected = new JMenuItem(HiLiteHandler.UNHILITE_SELECTED);
		hiliteMenu.add(unHiliteSelected);
		unHiliteAll = new JMenuItem(HiLiteHandler.CLEAR_HILITE);
		hiliteMenu.add(unHiliteAll);
		final JMenu fileMenu = getJMenuBar().getMenu(0);
		final Action exportAllAction = new ExportAllAction();
		fileMenu.add(new JMenuItem(exportAllAction));
		final Action exportLegendAction = new ExportLegendAction<ComplexModel>(
				"Export colour legend", colourSelector, ImageType.png);
		fileMenu.add(new JMenuItem(exportLegendAction));
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
		showColorsLegend = new JCheckBoxMenuItem("Show colours legend", true);
		legendMenu.add(showColorsLegend);
		showColorsLegend.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(final ActionEvent e) {
				volatileModel.setShowColourLegend(showColorsLegend.getModel()
						.isSelected());
			}
		});
		showTooltipsLegend = new JCheckBoxMenuItem("Show tooltips legend", true);
		// legendMenu.add(showTooltipsLegend);
		getJMenuBar().add(legendMenu);

		mainSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, true);
		final JSplitPane upperSplit = new JSplitPane(
				JSplitPane.HORIZONTAL_SPLIT, false);
		mainSplit.setTopComponent(upperSplit);
		mainSplit.setOneTouchExpandable(true);

		// Initialise the defaults.
		currentViewModel.getMain().getArrangementModel().mutate(
				nodeModel.getPossibleParameters());
		volatileModel.mutateValues(currentViewModel.getMain()
				.getArrangementModel(), nodeModel);
		((ControlsHandlerKNIMEFactory) controlsHandler)
				.setArrangement(currentViewModel.getMain());
		controlPanel.updateControl(currentViewModel);

		heatmapPanel = new HeatmapPanel(getCurrentViewModel(), null,
				volatileModel);
		upperSplit.setTopComponent(heatmapPanel);
		legendPanel = new LegendPanel(false, getCurrentViewModel());
		legendPanel.setPreferredSize(new Dimension(250, 250));
		legendPanel.setMinimumSize(new Dimension(200, 200));
		showColorsLegend.addActionListener(legendPanel);
		showTooltipsLegend.addActionListener(legendPanel);
		final GridBagLayout gridBagLayout = new GridBagLayout();
		final JPanel rightPanel = new JPanel(gridBagLayout);
		rightPanel.add(legendPanel);
		final GridBagConstraints legendConstraint = new GridBagConstraints();
		legendConstraint.gridy = 0;
		legendConstraint.gridheight = 4;
		final JScrollPane scrollPane = new JScrollPane(rightPanel);
		scrollPane.getViewport().setPreferredSize(new Dimension(250, 600));
		gridBagLayout.addLayoutComponent(legendPanel, legendConstraint);
		// scrollPane.setPreferredSize(new Dimension(250, 800));
		upperSplit.setBottomComponent(scrollPane);
		upperSplit.setOneTouchExpandable(true);
		// heatmapPanel.setPreferredSize(new Dimension(600, 800));
		final JTabbedPane bottomTabs = new JTabbedPane();
		mainSplit.setBottomComponent(bottomTabs);
		settingsPanel = new SettingsPanel();
		// bottomTabs.setPreferredSize(new Dimension(600, 200));
		final JScrollPane controlScrollPane = new JScrollPane(controlPanel);
		// controlScrollPane.getViewport().setPreferredSize(
		// new Dimension(controlScrollPane.getPreferredSize().width, 200));
		bottomTabs.add("Controls", controlScrollPane);
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
		bottomTabs.add("Colours", new JScrollPane(getColourSelector()));
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
		getColourSelector().getModel().addActionListener(heatmapPanel);
		getColourSelector().getModel().addActionListener(legendPanel);
		getColourSelector().getModel().addActionListener(legendPanel2);
		currentViewModel.getMain().setColourModel(
				getColourSelector().getModel());
		volatileModel.addActionListener(legendPanel);
		volatileModel.addActionListener(legendPanel2);
		setCurrentViewModel(currentViewModel);
		controlsHandler.setContainer(heatmapPanel.controlPanel,
				SplitType.SingleSelect, PositionConstants.upper.name());
		final JPanel rightPrim = new JPanel(new FlowLayout());
		final JPanel rightSec = new JPanel(new FlowLayout());
		final JPanel rightOther = new JPanel();
		rightOther.setLayout(new GridLayout(0, 1));
		rightPanel.add(rightPrim);
		final GridBagConstraints primConstaints = new GridBagConstraints();
		primConstaints.gridy = 4;
		gridBagLayout.addLayoutComponent(rightPrim, primConstaints);
		rightPanel.add(rightSec);
		final GridBagConstraints secContstraints = new GridBagConstraints();
		secContstraints.gridy = 5;
		gridBagLayout.addLayoutComponent(rightSec, secContstraints);
		final GridBagConstraints otherContraints = new GridBagConstraints();
		otherContraints.gridy = 6;
		rightPanel.add(rightOther);
		gridBagLayout.addLayoutComponent(rightOther, otherContraints);

		controlsHandler.setContainer(rightPrim, SplitType.PrimarySplit,
				PositionConstants.sidebarPrimary.name());
		controlsHandler.setContainer(rightSec, SplitType.SeconderSplit,
				PositionConstants.sidebarSecondary.name());
		controlsHandler.setContainer(rightOther, SplitType.SingleSelect,
				PositionConstants.sidebarOther.name());
		changeListenerForControlsHandler = new ChangeListener() {
			@Override
			public void stateChanged(final ChangeEvent e) {
				if (e instanceof ArrangementEvent) {
					final ArrangementEvent event = (ArrangementEvent) e;
					setCurrentViewModel(new ViewModel(getCurrentViewModel(),
							event.getNewArrangement()));
					for (final EnumMap<Shape, ViewModel> shapes : possibleViewModels
							.values()) {
						for (final Entry<Shape, ViewModel> shapeEntry : shapes
								.entrySet()) {
							final ViewModel viewModel = new ViewModel(
									shapeEntry.getValue(), event
											.getNewArrangement());
							viewModel.getMain().setColourModel(
									getCurrentViewModel().getMain()
											.getColourModel());
							shapeEntry.setValue(viewModel);
						}

					}
				}
			}
		};
		// rightPanel.setPreferredSize(new Dimension(200, 800));
		// heatmapPanel.setPreferredSize(new Dimension(heatmapPanel
		// .getPreferredSize().width, 800));
		// upperSplit.setPreferredSize(new Dimension(
		// upperSplit.getPreferredSize().width, 800));
		controlsHandler.addChangeListener(changeListenerForControlsHandler);
		mainSplit.setDividerLocation(500);
		upperSplit.setDividerLocation(650);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void modelChanged() {
		if (getNodeModel() == null || getNodeModel().getTable() == null) {
			mainSplit.setVisible(false);
			return;
		}
		mainSplit.setVisible(true);
		logger.debug("Model change started.");
		for (final Entry<Type, Collection<SliderModel>> entry : getVolatileModel().arrangementModel
				.getSliders().entrySet()) {
			for (final SliderModel model : entry.getValue()) {
				controlsHandler.deregister(model);
			}
		}
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
		for (final Entry<Type, Collection<SliderModel>> entry : getVolatileModel().arrangementModel
				.getSliders().entrySet()) {
			for (final SliderModel model : entry.getValue()) {
				SplitType splitType;
				final ControlTypes controlType;
				switch (type(model)) {
				case experimentName:
				case normalisation:
				case metaStatType:
					controlType = ControlTypes.ComboBox;
					splitType = SplitType.SingleSelect;
					break;
				case plate:
					controlType = ControlTypes.Slider;
					splitType = SplitType.SingleSelect;
					break;
				case parameter:
					splitType = SplitType.PrimarySplit;
					controlType = ControlTypes.List;
					break;
				case replicate:
					splitType = SplitType.SeconderSplit;
					controlType = ControlTypes.Buttons;
					break;
				case meanOrDiff:
				case median:
				case normalised:
				case otherEnumeration:
				case otherNumeric:
				case rankNonReplicates:
				case rankReplicates:
				case raw:
				case rawPerMedian:
				case score:
				default:
					throw new UnsupportedOperationException("Not supported: "
							+ type(model));
				}
				controlsHandler
						.register(
								model,
								splitType,
								splitType == SplitType.SeconderSplit ? PositionConstants.sidebarSecondary
										.name()
										: getDefaultPosition(entry.getKey()),
								controlType);
				model.addActionListener(volatileModel);
			}
		}
		getColourSelector().getModel().addActionListener(volatileModel);
		getColourSelector().update(
				nodeModel.getModelBuilder().getSpecAnalyser().getParameters(),
				nodeModel.getModelBuilder().getSpecAnalyser().getStatistics(),
				findRanges(nodeModel.getModelBuilder()));
		heatmapPanel.setModel(nodeModel);
		table.setDataTable(nodeModel.getTable());
		table.setHiLiteHandler(nodeModel.getInHiLiteHandler(0));
		logger.debug("Model change successfully finished.");
		getColourSelector().getModel().notifyListeners();
	}

	/**
	 * @param modelBuilder
	 *            A {@link ModelBuilder}.
	 * @return The map containing the {@link RangeType}s for different
	 *         parameter/statistics types.
	 */
	private Map<String, Map<StatTypes, Map<RangeType, Double>>> findRanges(
			final ModelBuilder modelBuilder) {
		final Map<String, Map<StatTypes, Map<RangeType, Double>>> ret = new HashMap<String, Map<StatTypes, Map<RangeType, Double>>>();
		final Map<String, Map<StatTypes, List<Double>>> vals = new HashMap<String, Map<StatTypes, List<Double>>>();
		for (final Entry<String, Map<String, Map<Integer, Map<String, Map<StatTypes, double[]>>>>> scoresEntry : modelBuilder
				.getScores().entrySet()) {
			for (final Entry<String, Map<Integer, Map<String, Map<StatTypes, double[]>>>> expEntry : scoresEntry
					.getValue().entrySet()) {
				for (final Entry<Integer, Map<String, Map<StatTypes, double[]>>> plateEntry : expEntry
						.getValue().entrySet()) {
					for (final Entry<String, Map<StatTypes, double[]>> paramEntry : plateEntry
							.getValue().entrySet()) {
						if (!ret.containsKey(paramEntry.getKey())) {
							ret
									.put(
											paramEntry.getKey(),
											new EnumMap<StatTypes, Map<RangeType, Double>>(
													StatTypes.class));
							vals.put(paramEntry.getKey(),
									new EnumMap<StatTypes, List<Double>>(
											StatTypes.class));
						}
						for (final Entry<StatTypes, double[]> statEntry : paramEntry
								.getValue().entrySet()) {
							final Map<StatTypes, Map<RangeType, Double>> map = ret
									.get(paramEntry.getKey());
							if (!map.containsKey(statEntry.getKey())) {
								map.put(statEntry.getKey(),
										new EnumMap<RangeType, Double>(
												RangeType.class));
								vals.get(paramEntry.getKey()).put(
										statEntry.getKey(),
										new ArrayList<Double>());
							}
							final List<Double> list = vals.get(
									paramEntry.getKey())
									.get(statEntry.getKey());
							for (final double val : statEntry.getValue()) {
								if (!Double.isNaN(val)
										&& !Double.isInfinite(val)) {
									list.add(Double.valueOf(val));
								}
							}
						}
					}
				}
			}
		}
		for (final Entry<String, Map<String, Map<Integer, Map<Integer, Map<String, Map<StatTypes, double[]>>>>>> replicatesEntry : modelBuilder
				.getReplicates().entrySet()) {
			for (final Entry<String, Map<Integer, Map<Integer, Map<String, Map<StatTypes, double[]>>>>> expEntry : replicatesEntry
					.getValue().entrySet()) {
				for (final Entry<Integer, Map<Integer, Map<String, Map<StatTypes, double[]>>>> plateEntry : expEntry
						.getValue().entrySet()) {
					for (final Entry<Integer, Map<String, Map<StatTypes, double[]>>> replicateEntry : plateEntry
							.getValue().entrySet()) {
						for (final Entry<String, Map<StatTypes, double[]>> paramEntry : replicateEntry
								.getValue().entrySet()) {

							if (!ret.containsKey(paramEntry.getKey())) {
								ret
										.put(
												paramEntry.getKey(),
												new EnumMap<StatTypes, Map<RangeType, Double>>(
														StatTypes.class));
								vals.put(paramEntry.getKey(),
										new EnumMap<StatTypes, List<Double>>(
												StatTypes.class));
							}
							for (final Entry<StatTypes, double[]> statEntry : paramEntry
									.getValue().entrySet()) {
								final Map<StatTypes, Map<RangeType, Double>> map = ret
										.get(paramEntry.getKey());
								if (!map.containsKey(statEntry.getKey())) {
									map.put(statEntry.getKey(),
											new EnumMap<RangeType, Double>(
													RangeType.class));
									vals.get(paramEntry.getKey()).put(
											statEntry.getKey(),
											new ArrayList<Double>());
								}
								final List<Double> list = vals.get(
										paramEntry.getKey()).get(
										statEntry.getKey());
								for (final double val : statEntry.getValue()) {
									if (!Double.isNaN(val)
											&& !Double.isInfinite(val)) {
										list.add(Double.valueOf(val));
									}
								}
							}
						}
					}
				}
			}
		}
		SimpleModelBuilder.computeStatistics(ret, vals);
		return ret;
	}

	/**
	 * @param model
	 *            A {@link SliderModel}.
	 * @return The {@link StatTypes} of the first {@link ParameterModel}.
	 */
	private StatTypes type(final SliderModel model) {
		return model.getParameters().iterator().next().getType();
	}

	/**
	 * @param type
	 *            A {@link Type}.
	 * @return The default position name for {{@code type}.
	 */
	private String getDefaultPosition(final Type type) {
		switch (type) {
		case Hidden:
			return PositionConstants.sidebarOther.name();
		case Selector:
			return PositionConstants.upper.name();
		case Splitter:
			return PositionConstants.sidebarPrimary.name();
		default:
			throw new UnsupportedOperationException("Not supported type: "
					+ type);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onClose() {
		// TODO things to do when closing the view
		if (getNodeModel().isSaveSettings()) {
			// try {
			// final JAXBContext context = JAXBContext
			// .newInstance(ViewModel.class);
			// final Marshaller marshaller = context.createMarshaller();
			// final FileOutputStream os = new FileOutputStream(new File(
			// getNodeModel().getInternDir(),
			// HeatmapNodeModel.SAVE_SETTINGS_FILE_NAME));
			// try {
			// // FIXME it is not working: no 0-arg constructor.
			// marshaller.marshal(currentViewModel, os);
			// } finally {
			// os.close();
			// }
			// } catch (final JAXBException e) {
			// logger.info("Unable to save state.", e);
			// } catch (final IOException e) {
			// logger.info("Unable to save state.", e);
			// }
		}
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
		final File settingsFile = new File(getNodeModel().getInternDir(),
				HeatmapNodeModel.SAVE_SETTINGS_FILE_NAME);
		if (getNodeModel().isSaveSettings() && settingsFile.canRead()) {
			// try {
			// final JAXBContext context = JAXBContext
			// .newInstance(ViewModel.class);
			// final Unmarshaller unmarshaller = context.createUnmarshaller();
			// unmarshaller.setSchema(null);
			// currentViewModel = ViewModel.class.cast(unmarshaller
			// .unmarshal(settingsFile));
			// } catch (final JAXBException e) {
			// logger.info("Unable to load state.", e);
			// }
		}
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
		return super.getNodeModel();
	}

	/**
	 * @return The {@link ControlsHandler} of the view.
	 */
	public ControlsHandler<? extends SettingsModel, SliderModel> getControlsHandler() {
		return controlsHandler;
	}

	/**
	 * @return the colourSelector
	 */
	public ColourSelector getColourSelector() {
		return colourSelector;
	}
}

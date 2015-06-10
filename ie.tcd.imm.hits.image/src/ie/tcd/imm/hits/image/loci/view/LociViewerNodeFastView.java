package ie.tcd.imm.hits.image.loci.view;

import ie.tcd.imm.hits.common.Format;
import ie.tcd.imm.hits.image.loci.view.AutoContrast.ContrastActionEvent;
import ie.tcd.imm.hits.image.util.imagej.ImageConverterEnh;
import ie.tcd.imm.hits.image.util.imagej.ImageConverterEnh.ConversionStrategy;
import ie.tcd.imm.hits.knime.view.ControlsHandler;
import ie.tcd.imm.hits.knime.view.SplitType;
import ie.tcd.imm.hits.util.ITriple;
import ie.tcd.imm.hits.util.Pair;
import ie.tcd.imm.hits.util.select.NamedSelector;
import ie.tcd.imm.hits.util.select.OptionalNamedSelector;
import ie.tcd.imm.hits.util.swing.VariableControl.ControlTypes;
import ie.tcd.imm.hits.view.impl.ControlsHandlerFactory;
import ie.tcd.imm.hits.view.util.ColourUtil;
import ie.tcd.imm.hits.view.util.SimpleWellSelection;
import ie.tcd.imm.hits.view.util.ZoomScrollPane;
import ie.tcd.imm.hits.view.util.Zoomable.ZoomListener;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ImageProcessor;
import ij.process.LUT;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import javax.annotation.Nullable;
import javax.media.jai.InterpolationNearest;
import javax.media.jai.JAI;
import javax.media.jai.RenderedOp;
import javax.media.jai.util.ImagingListener;
import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.BoundedRangeModel;
import javax.swing.ButtonGroup;
import javax.swing.DefaultBoundedRangeModel;
import javax.swing.Icon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.JSpinner.DefaultEditor;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import loci.formats.FormatException;
import loci.formats.FormatReader;
import loci.formats.FormatTools;
import loci.plugins.util.ImagePlusReader;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartMouseEvent;
import org.jfree.chart.ChartMouseListener;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.LogarithmicAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.entity.ChartEntity;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.Layer;
import org.jfree.ui.RectangleEdge;
import org.knime.core.data.RowKey;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeView;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.property.hilite.HiLiteHandler;
import org.knime.core.node.property.hilite.HiLiteListener;
import org.knime.core.node.property.hilite.KeyEvent;

import com.sun.media.jai.widget.DisplayJAI;

/**
 * <code>NodeView</code> for the "Loci Viewer" Node. Shows images based on
 * Bio-Formats.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
public class LociViewerNodeFastView extends NodeView<LociViewerNodeModel> {
	/**
	 * A simple {@link AbstractAction} that sets the
	 * {@link LociViewerNodeFastView#contrastStrategy} to the defined in the
	 * constructor {@link AutoContrast} object.
	 */
	private class SetAutoContrast extends AbstractAction {
		private static final long serialVersionUID = -1716780980886699511L;
		private final String message;
		private final AutoContrastStrategy strat;
		private final double left;
		private final double right;

		/**
		 * @param message
		 *            The name of the action.
		 * @param strat
		 *            The {@link AutoContrastStrategy} to use in the
		 *            {@link AutoContrast}.
		 * @param left
		 *            The first additional parameter.
		 * @param right
		 *            The second additional parameter.
		 */
		public SetAutoContrast(final String message,
				final AutoContrastStrategy strat, final double left,
				final double right) {
			super(message);
			this.message = message;
			this.strat = strat;
			this.left = left;
			this.right = right;
		}

		/**
		 * Sets the new strategy, regenerates the image.
		 */
		@Override
		public void actionPerformed(final ActionEvent e) /* => */{
			contrastStrategy = new AutoContrast(message, strat, left, right);
			regenerateImage();
		}

	}

	/**
	 * A {@link HiLiteListener} for the well control.
	 */
	private final class HiLiteListenerWells implements HiLiteListener {
		private Set<ITriple<String, String, Integer>> hilites = new HashSet<ITriple<String, String, Integer>>();

		@Override
		public void unHiLiteAll(final KeyEvent event) {
			hilites.clear();
			updateHilites();
		}

		@Override
		public void unHiLite(final KeyEvent event) {
			final Set<ITriple<?, ?, ?>> toRemove = new HashSet<ITriple<?, ?, ?>>();
			final Set<RowKey> keysToRemove = event.keys();
			for (final ITriple<?, ?, ?> triple : hilites) {
				if (keysToRemove.contains(triple.getO1())) {
					toRemove.add(triple);
				}
			}
			for (final ITriple<?, ?, ?> iTriple : toRemove) {
				hilites.remove(iTriple);
			}
			updateHilites();
		}

		@Override
		public void hiLite(final KeyEvent event) {
			for (final RowKey row : event.keys()) {
				final ITriple<String, String, Integer> triple = getNodeModel()
						.getRowsToWells().get(row);
				hilites.add(triple);
			}
			updateHilites();
		}

		/**
		 * 
		 */
		protected void updateHilites() {
			wellSel.updateHiLites(select(hilites, plateSelector.getSelected()));
		}

		// /**
		// * Removes all HiLites.
		// */
		// protected void clear() {
		// hilites.clear();
		// }
	}

	private static final String GENERAL = "general";
	private static final String PLATE = "plate";
	private static final String FIELD = "field";
	private static final String TIME = "time";
	private static final String Z = "Z";
	private static final String CHANNEL = "channel";

	private static final NodeLogger logger = NodeLogger
			.getLogger(LociViewerNodeFastView.class);

	private JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER));

	private ControlsHandler<SettingsModel, String, OptionalNamedSelector<String>> controlsHandlerFactory;
	@Nullable
	private OptionalNamedSelector<String> plateSelector;
	@Nullable
	private OptionalNamedSelector<String> fieldSelector;
	@Nullable
	private OptionalNamedSelector<String> timeSelector;
	@Nullable
	private OptionalNamedSelector<String> zSelector;
	@Nullable
	private OptionalNamedSelector<String> channelSelector;
	/** Plate, row, column, field, time, z, image id (series), LOCI data */
	private Map<String, Map<String, Map<Integer, Map<Integer, Map<Double, Map<Double, Map<Integer, FormatReader>>>>>>> joinTable;
	private DisplayJAI imagePanel = new DisplayJAI();
	private List<ActionListener> listenersToNotGCd = new ArrayList<ActionListener>();
	private ZoomScrollPane imageScrollPane;
	private ImagePlus imagePlus;
	private ImageProcessor[] imageProcessors;

	private BoundedRangeModel zoomModel = new DefaultBoundedRangeModel(100, 0,
			10, 400);
	private JSlider zoomSlider = new JSlider(zoomModel);
	private SpinnerModel secondZoomModel = new SpinnerNumberModel(100, 10, 400,
			1);
	private JSpinner zoomSpinner = new JSpinner(secondZoomModel);
	private Map<String, LUT> luts = new HashMap<String, LUT>();
	private JPanel otherPanel;
	/** The width of the current image */
	protected int sizeX;
	/** The height of the current image */
	protected int sizeY;
	private SimpleWellSelection wellSel;
	private HiLiteListenerWells hiLiteListener;
	private ConversionStrategy strategy = ConversionStrategy.Maximum;
	private boolean inverse = false;
	private AutoContrast contrastStrategy = AutoContrast.keepLast;
	{
		zoomModel.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(final ChangeEvent e) /* => */{
				if (!zoomModel.getValueIsAdjusting()
						&& Math.abs(((Number) secondZoomModel.getValue())
								.doubleValue()
								- zoomModel.getValue()) > 1E-5) {
					secondZoomModel.setValue(Integer.valueOf(zoomModel
							.getValue()));
				}
			}
		});
		secondZoomModel.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(final ChangeEvent e) /* => */{
				if (Math.abs(((Number) secondZoomModel.getValue())
						.doubleValue()
						- zoomModel.getValue()) > 1E-5) {
					zoomModel.setValue(Integer
							.valueOf(((Number) secondZoomModel.getValue())
									.intValue()));
				}
			}
		});
		zoomSlider.setOrientation(SwingConstants.VERTICAL);
		zoomSlider.setMajorTickSpacing(100);
		// zoomSlider.setPaintTicks(true);
		zoomSlider.setPaintLabels(true);
		zoomSlider.setPaintTrack(true);
		zoomSlider.setLabelTable(zoomSlider.createStandardLabels(50, 50));
		((DefaultEditor) zoomSpinner.getEditor()).getTextField().setColumns(5);
	}

	static {
		JAI.getDefaultInstance().setImagingListener(new ImagingListener() {

			@Override
			public boolean errorOccurred(final String message,
					final Throwable thrown, final Object where,
					final boolean isRetryable) /* => */throws RuntimeException {
				// suppress error messages
				return false;
			}
		});
	}

	/**
	 * Creates a new view.
	 * 
	 * @param nodeModel
	 *            The model (class: {@link LociViewerNodeModel})
	 */
	protected LociViewerNodeFastView(final LociViewerNodeModel nodeModel) {
		super(nodeModel);
		controlsHandlerFactory = new ControlsHandlerFactory<String>();
		final JPanel controls = new JPanel();
		controlsHandlerFactory.setContainer(controls, SplitType.SingleSelect,
				GENERAL);
		controlsHandlerFactory.setContainer(controls, SplitType.PrimarySplit,
				GENERAL);
		imageScrollPane = new ZoomScrollPane(imagePanel, zoomModel);
		imagePanel.setAlignmentX(Component.CENTER_ALIGNMENT);
		imagePanel.setAlignmentY(Component.CENTER_ALIGNMENT);
		imageScrollPane.addZoomListener(new ZoomListener() {

			@Override
			public void zoom(final ZoomEvent event)/* => */{
				zoomModel.setValue(event.zoomFactor());
			}
		});
		final JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		otherPanel = new JPanel();
		final JPanel jPanel = new JPanel();
		splitPane.setLeftComponent(imageScrollPane);
		jPanel.add(controls);
		jPanel.add(otherPanel);
		splitPane.setResizeWeight(1.0);
		splitPane.setRightComponent(new JScrollPane(jPanel));
		splitPane.getLeftComponent().setPreferredSize(new Dimension(1000, 600));
		splitPane.getRightComponent()
				.setPreferredSize(new Dimension(1000, 250));
		splitPane.setOneTouchExpandable(true);
		splitPane.setDividerLocation(600);
		splitPane.revalidate();
		panel.add(splitPane);
		joinTable = Collections.emptyMap();
		otherPanel.add(zoomSlider);
		otherPanel.add(zoomSpinner);
		zoomModel.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(final ChangeEvent e)/* => */{
				repaintImage();
			}
		});
		final Format format = Format._96;

		wellSel = new SimpleWellSelection(format);
		hiLiteListener = new HiLiteListenerWells();
		otherPanel.add(wellSel);
		final JMenu hiliteMenu = new JMenu(HiLiteHandler.HILITE);
		getJMenuBar().add(hiliteMenu);
		hiliteMenu.add(new AbstractAction(HiLiteHandler.HILITE_SELECTED) {
			private static final long serialVersionUID = -6054254350021725863L;

			@Override
			public void actionPerformed(final ActionEvent e)/* => */{
				final Set<RowKey> selections = findSelectedRowKeys();
				getNodeModel().getInHiLiteHandler(0)
						.fireHiLiteEvent(selections);
			}
		});
		hiliteMenu.add(new AbstractAction(HiLiteHandler.UNHILITE_SELECTED) {
			private static final long serialVersionUID = 8360935725278415300L;

			@Override
			public void actionPerformed(final ActionEvent e)/* => */{
				final Set<RowKey> selections = findSelectedRowKeys();
				getNodeModel().getInHiLiteHandler(0).fireUnHiLiteEvent(
						selections);
			}
		});
		hiliteMenu.add(new AbstractAction(HiLiteHandler.CLEAR_HILITE) {
			private static final long serialVersionUID = 7449582397283093888L;

			@Override
			public void actionPerformed(final ActionEvent e)/* => */{
				getNodeModel().getInHiLiteHandler(0).fireClearHiLiteEvent();
			}
		});
		setComponent(splitPane);
		final JMenu coloursMenu = new JMenu("Colours");
		final JCheckBoxMenuItem invert = new JCheckBoxMenuItem(
				new AbstractAction("Invert") {
					private static final long serialVersionUID = -3192154966624677909L;

					@Override
					public void actionPerformed(final ActionEvent e) /* => */{
						inverse = !inverse;
						regenerateImage(AutoContrast.keepLast);
					}
				});
		invert.setState(inverse);
		coloursMenu.add(invert);
		coloursMenu.add(new JSeparator());
		final EnumMap<ConversionStrategy, JRadioButtonMenuItem> strategies = new EnumMap<ConversionStrategy, JRadioButtonMenuItem>(
				ConversionStrategy.class);
		strategies.put(ConversionStrategy.Maximum, new JRadioButtonMenuItem(
				new ColourCombinationAction("Maximum",
						ConversionStrategy.Maximum)));
		strategies.put(ConversionStrategy.Minimum, new JRadioButtonMenuItem(
				new ColourCombinationAction("Minimum",
						ConversionStrategy.Minimum)));
		strategies.put(ConversionStrategy.Additive, new JRadioButtonMenuItem(
				new ColourCombinationAction("Additive",
						ConversionStrategy.Additive)));
		final ButtonGroup buttonGroup = new ButtonGroup();
		for (final JRadioButtonMenuItem menu : strategies.values()) {
			coloursMenu.add(menu);
			buttonGroup.add(menu);
		}
		buttonGroup.clearSelection();
		buttonGroup.setSelected(strategies.get(strategy).getModel(), true);
		getJMenuBar().add(coloursMenu);
		final JMenu contrastMenu = new JMenu("Contrast");
		final ButtonGroup contrastButtons = new ButtonGroup();
		final JRadioButtonMenuItem keepLast = new JRadioButtonMenuItem(
				new SetAutoContrast(AutoContrastStrategy.KeepLast
						.getDisplayText(), AutoContrastStrategy.KeepLast, 1.0,
						1.0));
		contrastButtons.add(keepLast);
		contrastMenu.add(keepLast);
		final double[] options = new double[] { 1.0, .9, .75, .5, .25, .1 };
		for (final AutoContrastStrategy strat : new AutoContrastStrategy[] {
				AutoContrastStrategy.MinModeMax,
				AutoContrastStrategy.FirstHighest,
				AutoContrastStrategy.SecondHighest }) {
			add(strat, contrastMenu, contrastButtons, options);
		}
		if (Boolean.parseBoolean(System.getProperty("knime.expert.mode"))) {
			getJMenuBar().add(contrastMenu);
		}
	}

	/**
	 * Adds for {@code strat} some menus to {@code contrastMenu}, the leaves are
	 * {@link JRadioButtonMenuItem}s, added also to the {@code contrastButtons}.
	 * 
	 * @param strat
	 *            An {@link AutoContrastStrategy}.
	 * @param contrastMenu
	 *            The "Contrast" menu.
	 * @param contrastButtons
	 *            A {@link ButtonGroup} for the contrast
	 *            {@link JRadioButtonMenuItem}s.
	 * @param options
	 *            The optional values for the actions, combination of these will
	 *            be used to generate the menus.
	 */
	private void add(final AutoContrastStrategy strat,
			final JMenu contrastMenu, final ButtonGroup contrastButtons,
			final double[] options) {
		final double d = options[0];
		final JMenu menu = new JMenu(new SetAutoContrast(strat.getDisplayText()
				+ "+/-" + d, strat, d, d));
		for (int i = 0; i < options.length; ++i) {
			final double o = options[i];
			final JMenu subMenu = new JMenu(new SetAutoContrast(strat
					.getDisplayText()
					+ "+/-" + o, strat, o, o));
			menu.add(subMenu);
			for (int j = i; j < options.length; ++j) {
				final double o2 = options[j];
				final JRadioButtonMenuItem menuItem = new JRadioButtonMenuItem(
						new SetAutoContrast(strat.getDisplayText() + "+" + o
								+ "/-" + o2, strat, o2, o));
				subMenu.add(menuItem);
				contrastButtons.add(menuItem);
			}
			for (int j = i; j < options.length; ++j) {
				final double o2 = options[j];
				if (o2 == o) {
					continue;
				}
				final JRadioButtonMenuItem menuItem = new JRadioButtonMenuItem(
						new AutoContrast(strat.getDisplayText() + "+" + o2
								+ "/-" + o, strat, o, o2));
				subMenu.add(menuItem);
				contrastButtons.add(menuItem);
			}
		}
		contrastMenu.add(menu);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void modelChanged() {
		deregisterPreviousSelectors();
		joinTable = getNodeModel().getJoinTable();
		recreatePlateSelector();
		getNodeModel().getInHiLiteHandler(0).addHiLiteListener(hiLiteListener);
		((JComponent) getComponent()).revalidate();
		getComponent().repaint();
	}

	/**
	 * @param hilites
	 *            The actual set of hilited triples.
	 * @param plate
	 *            The plate to select.
	 * @return A {@link Set} of {@link Pair} of row, column information on the
	 *         selected {@code plate}.
	 */
	protected static Set<Pair<String, Integer>> select(
			final Set<ITriple<String, String, Integer>> hilites,
			final String plate) {
		final Set<Pair<String, Integer>> ret = new HashSet<Pair<String, Integer>>();
		for (final ITriple<String, String, Integer> iTriple : hilites) {
			if (iTriple.getO1().equals(plate)) {
				ret.add(Pair.apply(iTriple.getO2(), iTriple.getO3()));
			}
		}
		return ret;
	}

	/**
	 * 
	 */
	private void recreatePlateSelector() {
		listenersToNotGCd.clear();
		deregister(plateSelector);
		plateSelector = OptionalNamedSelector.createSingle(PLATE, joinTable
				.keySet());
		updateWellSelection();
		final ActionListener actionListener = new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e)/* => */{
				wellSel.notifyListeners();
				hiLiteListener.updateHilites();
			}
		};
		plateSelector.addActionListener(actionListener);
		listenersToNotGCd.add(actionListener);
		controlsHandlerFactory.register(plateSelector, SplitType.SingleSelect,
				GENERAL, ControlTypes.List);
	}

	/**
	 * 
	 */
	private void updateWellSelection() {
		recreateFieldSelector();
		wellSel.removeAllActionListeners();
		wellSel.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e)/* => */{
				fieldSelector.notifyListeners();
			}
		});
	}

	private void recreateFieldSelector() {
		deregister(fieldSelector);
		fieldSelector = OptionalNamedSelector.createSingle(FIELD,
				asStringSet(increase(getPlateRowColMap().keySet(), 0)));
		recreateTimeSelector();
		final ActionListener actionListener = new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e)/* => */{
				timeSelector.notifyListeners();
			}
		};
		fieldSelector.addActionListener(actionListener);
		listenersToNotGCd.add(actionListener);
		controlsHandlerFactory.register(fieldSelector, SplitType.SingleSelect,
				GENERAL, ControlTypes.Buttons);
	}

	private void recreateTimeSelector() {
		deregister(timeSelector);
		timeSelector = OptionalNamedSelector.createSingle(TIME + " "
				+ getNodeModel().getTimeUnit(),
				asStringSet(getPlateRowColFieldMap().keySet()));
		recreateZSelector();
		final ActionListener actionListener = new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e)/* => */{
				zSelector.notifyListeners();
			}
		};
		timeSelector.addActionListener(actionListener);
		listenersToNotGCd.add(actionListener);
		controlsHandlerFactory.register(timeSelector, SplitType.SingleSelect,
				GENERAL, ControlTypes.Buttons);
	}

	private void recreateZSelector() {
		deregister(zSelector);
		zSelector = OptionalNamedSelector.createSingle(Z + " "
				+ getNodeModel().getZUnit(),
				asStringSet(getPlateRowColFieldTimeMap().keySet())// ,
				// "\u00B5m")
				);
		recreateChannelSelector();
		final ActionListener actionListener = new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e)/* => */{
				channelSelector.notifyListeners();
			}
		};
		zSelector.addActionListener(actionListener);
		listenersToNotGCd.add(actionListener);
		controlsHandlerFactory.register(zSelector, SplitType.SingleSelect,
				GENERAL, ControlTypes.Buttons);
	}

	private static Iterable<Integer> increase(final Collection<Integer> col,
			final int inc) {
		final List<Integer> ret = new ArrayList<Integer>(col.size());
		for (final Integer i : col) {
			ret.add(Integer.valueOf(i.intValue() + inc));
		}
		return ret;
	}

	private void recreateChannelSelector() {
		deregister(channelSelector);
		final Collection<FormatReader> readers = getPlateRowColFieldTimeZMap()
				.values();
		List<String> channelNames;
		if (readers.isEmpty()) {
			channelNames = Collections.singletonList("Channel");
		} else {
			final FormatReader reader = readers.iterator().next();
			channelNames = new ArrayList<String>(reader.getSizeC());
			final Class<? extends FormatReader> readerClass = reader.getClass();
			try {
				final Field channelNamesField = readerClass
						.getDeclaredField("channelNames");

				if (channelNamesField != null) {
					channelNamesField.setAccessible(true);
					goThroughInterfaces(channelNames, reader,
							channelNamesField, channelNamesField.getType(),
							reader.getSizeC());
				}
				if (channelNames.isEmpty()) {
					channelNames.addAll(createSampleChannelNames(reader
							.getSizeC()));
				}
			} catch (final NoSuchFieldException e) {
				channelNames
						.addAll(createSampleChannelNames(reader.getSizeC()));
			} catch (final IllegalArgumentException e) {
				channelNames
						.addAll(createSampleChannelNames(reader.getSizeC()));
			} catch (final IllegalAccessException e) {
				channelNames
						.addAll(createSampleChannelNames(reader.getSizeC()));
			}
		}
		initLuts(channelNames);
		channelSelector = new OptionalNamedSelector<String>(CHANNEL,
				NamedSelector.createValues(channelNames), Collections
						.singleton(Integer.valueOf(1)));
		final MouseListener controlListener = new MouseAdapter() {
			@Override
			public void mouseClicked(final MouseEvent e)/* => */{
				if (e != null && e.getButton() != MouseEvent.BUTTON3) {
					return;
				}
				final JPopupMenu popupMenu = createPopupMenu(e);
				popupMenu.show(e.getComponent(), e.getX(), e.getY());
			}
		};
		final ActionListener actionListener = new ActionListener() {

			@Override
			public void actionPerformed(final ActionEvent e)/* => */{
				try {
					final Entry<Integer, FormatReader> entry = getPlateRowColFieldTimeZMap()
							.entrySet().iterator().next();
					final FormatReader formatReader = entry.getValue();
					final ImagePlusReader imagePlusReader = ImagePlusReader
							.makeImagePlusReader(formatReader);
					imagePlusReader.setSeries(entry.getKey().intValue());
					imageScrollPane.getViewport().setSize(
							sizeX = imagePlusReader.getSizeX(),
							sizeY = imagePlusReader.getSizeY());
					imageProcessors = new ImageProcessor[channelSelector
							.getValueMapping().size()];
					for (final Integer channel : channelSelector
							.getActiveValues()) {
						final int index = FormatTools.getIndex(imagePlusReader,
								zSelector.getSelections().iterator().next()
										.intValue() - 1,
								channel.intValue() - 1, timeSelector
										.getSelections().iterator().next()
										.intValue() - 1);
						imageProcessors[channel.intValue() - 1] = imagePlusReader
								.openProcessors(index/* channel.intValue() - 1 */)[0];
					}
					regenerateImage();
				} catch (final FormatException ex) {
					imagePlus = new ImagePlus();
					logger.error("", ex);
				} catch (final IOException ex) {
					imagePlus = new ImagePlus();
					logger.error("", ex);
				}
			}

		};
		listenersToNotGCd.add(actionListener);
		channelSelector.addActionListener(actionListener);
		channelSelector.addControlListener(controlListener);
		controlsHandlerFactory.register(channelSelector,
				SplitType.PrimarySplit, GENERAL, ControlTypes.Buttons);
		if (getPlateRowColFieldMap() != null) {
			actionListener.actionPerformed(null);
		}
	}

	/**
	 * Regenerates the {@link #imagePlus} object of zooming.
	 */
	protected void regenerateImage() {
		imagePlus = generateImagePlus(channelSelector.getSelections(),
				contrastStrategy);
		repaintImage();
	}

	/**
	 * Regenerates the {@link #imagePlus} object of zooming, based on the
	 * {@code autoContrast} parameter and the state of the object.
	 * 
	 * @param autoContrast
	 *            The {@link AutoContrast} object to regenerate the image.
	 */
	protected void regenerateImage(final AutoContrast autoContrast) {
		imagePlus = generateImagePlus(channelSelector.getSelections(),
				autoContrast);
		repaintImage();
	}

	/**
	 * Initialise the {@link LUT}s for each channel.
	 * 
	 * @param channelNames
	 *            The the name of the channels.
	 */
	private void initLuts(final List<String> channelNames) {
		int i = 0;
		for (final String channel : channelNames) {
			if (!luts.containsKey(channelNames)) {
				final LUT lut = (LUT) ColourUtil.LUTS[i++
						% ColourUtil.LUTS.length].clone();
				lut.min = Double.NaN;
				lut.max = Double.NaN;
				luts.put(channel, lut);
			}
		}
	}

	/**
	 * If finds an {@link Iterable} interface it will compute the channel names
	 * to {@code channelNames}. If not found it does noting.
	 * 
	 * @param channelNames
	 *            The result channel names list.
	 * @param reader
	 *            The {@link FormatReader} object.
	 * @param channelNamesField
	 *            The field of channel names.
	 * @param cls
	 *            The class/interface's {@link Class}.
	 * @param numberOfChannels
	 *            The number of channels.
	 * @throws IllegalAccessException
	 *             If the field is not accessible.
	 */
	private void goThroughInterfaces(final List<String> channelNames,
			final FormatReader reader, final Field channelNamesField,
			final Class<?> cls, final int numberOfChannels)
			throws IllegalAccessException {
		for (final Class<?> class1 : cls.getInterfaces()) {
			if (class1.equals(Iterable.class)) {
				final Object object = channelNamesField.get(reader);
				int i = 0;
				for (final Object content : (Iterable<?>) object) {
					channelNames.add(content.toString());

					if (++i == numberOfChannels) {
						return;
					}
				}
				return;
			}
			goThroughInterfaces(channelNames, reader, channelNamesField,
					class1, numberOfChannels);
		}
	}

	/**
	 * @param numberOfChannels
	 *            Number of channels.
	 * @return A list of {@code Channel n} values (starting from {@code Channel
	 *         1}).
	 */
	private Collection<String> createSampleChannelNames(
			final int numberOfChannels) {
		final Collection<String> ret = new ArrayList<String>(numberOfChannels);
		for (int i = 1; i <= numberOfChannels; ++i) {
			ret.add("Channel " + i);
		}
		return ret;
	}

	/**
	 * Repaint the computed image with the current scaling. (Uses the currently
	 * selected channels.)
	 */
	protected void repaintImage() {
		scaleAndSetImage(imagePlus.getBufferedImage());
		// final Set<Integer> selectedChannels =
		// channelSelector.getSelections();

		// repaintImage(selectedChannels);
	}

	/**
	 * Scales the previously generated image with {@link JAI}.
	 * 
	 * @param origImage
	 *            A {@link RenderedImage}, tipically got from {@link #imagePlus}
	 *            .
	 */
	private void scaleAndSetImage(final RenderedImage origImage) {
		final float scale = zoomModel.getValue() / 100.0f;
		final ParameterBlock pb = new ParameterBlock();
		pb.addSource(origImage);
		pb.add(scale);
		pb.add(scale);
		pb.add(0.0f);
		pb.add(0.0f);
		pb.add(new InterpolationNearest());
		final RenderedOp planarImage = JAI.create("scale", pb);
		final boolean fitWidth = planarImage.getWidth() < imageScrollPane
				.getViewportBorderBounds().width;
		final boolean fitHeight = planarImage.getHeight() < imageScrollPane
				.getViewportBorderBounds().height;
		imagePanel
				.set(planarImage, fitWidth ? (imageScrollPane
						.getViewportBorderBounds().width - planarImage
						.getWidth()) / 2 : 0, fitHeight ? (imageScrollPane
						.getViewportBorderBounds().height - planarImage
						.getHeight()) / 2 : 0);
		imagePanel.setAlignmentX(Component.CENTER_ALIGNMENT);
		imagePanel.setAlignmentY(Component.CENTER_ALIGNMENT);
	}

	/**
	 * Generates the {@link ImagePlus} image based on the current settings and
	 * {@code selectedChannels}.
	 * 
	 * @param selectedChannels
	 *            The selected channels ({@code 1}-based).
	 * @param autoContrast
	 *            The {@link AutoContrast} object to use for enhancing the
	 *            contrast of each channel.
	 * @return The generated {@link ImagePlus} object.
	 */
	private ImagePlus generateImagePlus(final Set<Integer> selectedChannels,
			final AutoContrast autoContrast) {
		final ImageStack stack = new ImageStack(sizeX, sizeY);
		for (final Integer channelInt : selectedChannels) {
			final int channel = channelInt.intValue() - 1;
			final String channelName = channelSelector.getValueMapping().get(
					channelInt);
			final LUT lut = luts.get(channelName);
			final ImagePlus imp = new ImagePlus("", imageProcessors[channel]);
			autoContrast.actionPerformed(new ContrastActionEvent(this, 1, null,
					0L, 0, imageProcessors[channel].getHistogram(), lut));
			// if (Double.isNaN(lut.min)) {
			// lut.min = findMin(imageProcessors[channel].getHistogram());
			// }
			// if (Double.isNaN(lut.max)) {
			// lut.max = findMax(imageProcessors[channel].getHistogram());
			// }
			new ImageConverterEnh(imp).convertToRGB(lut);
			stack.addSlice(null, imp.getProcessor());
		}
		final ImagePlus ret = new ImagePlus("", stack);
		final ImageConverterEnh conv = new ImageConverterEnh(ret);
		conv.convertStackToRGB(strategy);
		if (inverse) {
			conv.invert();
		}
		return ret;
	}

	private static <T> Set<String> asStringSet(final Iterable<T> vals) {
		final Set<String> ret = new LinkedHashSet<String>();
		for (final T t : vals) {
			ret.add(t.toString());
		}
		return ret;
	}

	// private static <T extends Number> Set<String> asStringSetWithUnit(
	// final Iterable<T> vals, final String unit) {
	// final Set<String> ret = new LinkedHashSet<String>();
	// for (final T t : vals) {
	// ret.add(NumberFormat.getNumberInstance(Locale.getDefault()).format(
	// t.doubleValue())
	// + " " + unit);
	// }
	// return ret;
	// }

	private void deregisterPreviousSelectors() {
		deregister(plateSelector);
		plateSelector = null;
		deregister(fieldSelector);
		fieldSelector = null;
		deregister(timeSelector);
		timeSelector = null;
		deregister(zSelector);
		zSelector = null;
		deregister(channelSelector);
		channelSelector = null;
	}

	/**
	 * @param selector
	 *            The selector to deregister from
	 *            {@link #controlsHandlerFactory} .
	 */
	private void deregister(final OptionalNamedSelector<String> selector) {
		if (selector != null) {
			controlsHandlerFactory.deregister(selector);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onClose() {
		// Do nothing
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onOpen() {
		// Do nothing
	}

	/**
	 * @return The {@link Map} for the current plate. (Row, Column, Field, time,
	 *         z, id &rArr; reader).
	 */
	private Map<String, Map<Integer, Map<Integer, Map<Double, Map<Double, Map<Integer, FormatReader>>>>>> getPlateMap() {
		return joinTable.get(plateSelector.getSelected());
	}

	/**
	 * @return The {@link Map} for the current plate and row. (Column, Field,
	 *         time, z, id &rArr; reader).
	 */
	private Map<Integer, Map<Integer, Map<Double, Map<Double, Map<Integer, FormatReader>>>>> getPlateRowMap() {
		// return getPlateMap().get(rowSelector.getSelected());
		return getPlateMap().get(wellSel.getSelection().substring(0, 1));
	}

	/**
	 * @return The {@link Map} for the current plate, row, column. (Field, time,
	 *         z, id &rArr; reader).
	 */
	private Map<Integer, Map<Double, Map<Double, Map<Integer, FormatReader>>>> getPlateRowColMap() {
		return getPlateRowMap().get(
				Integer.valueOf(Integer.parseInt(wellSel.getSelection()
						.substring(1))));
	}

	/**
	 * @return The {@link Map} for the current plate, row, column, field. (time,
	 *         z, id &rArr; reader).
	 */
	private Map<Double, Map<Double, Map<Integer, FormatReader>>> getPlateRowColFieldMap() {
		return getPlateRowColMap().get(
				Integer.valueOf(fieldSelector.getSelected()));
	}

	/**
	 * @return The {@link Map} for the current plate, row, column, field. ( z,
	 *         id &rArr; reader).
	 */
	private Map<Double, Map<Integer, FormatReader>> getPlateRowColFieldTimeMap() {
		return getPlateRowColFieldMap().get(
				Double.valueOf(timeSelector.getSelected()));
	}

	/**
	 * @return The {@link Map} for the current plate, row, column, field. ( id
	 *         &rArr; reader).
	 */
	private Map<Integer, FormatReader> getPlateRowColFieldTimeZMap() {
		final Double val = Double.valueOf(zSelector.getSelected());
		final Map<Double, Map<Integer, FormatReader>> map = getPlateRowColFieldTimeMap();
		final Map<Integer, FormatReader> ret = map.get(val);
		if (ret != null) {
			return ret;
		}
		double minDiff = Double.MAX_VALUE;
		Map<Integer, FormatReader> possRet = null;
		for (final Entry<Double, Map<Integer, FormatReader>> entry : map
				.entrySet()) {
			final double diff = Math.abs(entry.getKey().doubleValue()
					- val.doubleValue());
			if (diff < minDiff && entry.getValue() != null) {
				minDiff = diff;
				possRet = entry.getValue();
			}
		}
		if (possRet != null) {
			return possRet;
		}
		return Collections.emptyMap();// TODO replace with empty FormatReader
	}

	/**
	 * @return The {@link RowKey}s associated to the selected well.
	 */
	private Set<RowKey> findSelectedRowKeys() {
		final Set<RowKey> selections = new HashSet<RowKey>();
		for (final Entry<RowKey, ? extends ITriple<String, String, Integer>> entry : getNodeModel()
				.getRowsToWells().entrySet()) {
			if (entry.getValue().getO1().equals(plateSelector.getSelected())
					&& entry.getValue().getO2().equals(
							wellSel.getSelection().substring(0, 1))
					&& entry.getValue().getO3().equals(
							Integer
									.valueOf(wellSel.getSelection()
											.substring(1)))) {
				selections.add(entry.getKey());
			}
		}
		return selections;
	}

	private static int findChannelIndex(@Nullable final MouseEvent event,
			final OptionalNamedSelector<String> channelSelector) {
		final Object source = event == null ? null : event.getSource();
		if (source instanceof AbstractButton) {
			final AbstractButton button = (AbstractButton) source;
			final String channelName = button.getText();
			return findChannelIndexByName(channelSelector, channelName);
		}
		if (source instanceof JList) {
			final JList list = (JList) source;
			final int index = list.locationToIndex(event.getPoint());
			if (index == -1) {
				return 0;
			}
			final Object element = list.getModel().getElementAt(index);
			final String channelName = element.toString();
			return findChannelIndexByName(channelSelector, channelName);
		}
		final Set<Integer> selections = channelSelector.getSelections();
		return selections.isEmpty() ? 0 : selections.iterator().next()
				.intValue();
	}

	/**
	 * Selects the {@code 0} based index from the {@code channelSelector,
	 * channelName} parameters. {@code 0} if not found.
	 * 
	 * @param channelSelector
	 *            An {@link OptionalNamedSelector}.
	 * @param channelName
	 *            A value from the mapping.
	 * @return The index of the channel ({@code 0}-based), or {@code 0} if not
	 *         found.
	 */
	private static int findChannelIndexByName(
			final OptionalNamedSelector<String> channelSelector,
			final String channelName) {
		for (final Entry<Integer, String> entry : channelSelector
				.getValueMapping().entrySet()) {
			if (channelName.equals(entry.getValue())) {
				return entry.getKey().intValue();
			}
		}
		return 0;
	}

	private static XYDataset createHistogram(
			final ImageProcessor[] imageProcessors,
			final String... channelNames) {
		final XYSeriesCollection xySeriesCollection = new XYSeriesCollection();
		assert imageProcessors.length == channelNames.length;
		for (int p = 0; p < imageProcessors.length; ++p) {
			final String channelName = channelNames[p];
			final ImageProcessor imageProcessor = imageProcessors[p];
			final XYSeries ret = createHistogram(imageProcessor, channelName);
			xySeriesCollection.addSeries(ret);
		}
		return xySeriesCollection;
	}

	/**
	 * @param imageProcessor
	 *            An {@link ImageProcessor} to compute the histogram.
	 * @param channelName
	 *            The name of the channel.
	 * @return {@link XYSeries} of {@code imageProcessor}'s histogram.
	 */
	private static XYSeries createHistogram(
			final ImageProcessor imageProcessor, final String channelName) {
		final XYSeries ret = new XYSeries(channelName);
		// final DefaultXYDataset ret = new DefaultXYDataset();// new
		// HistogramDataset();
		// ret.setType(HistogramType.FREQUENCY);
		final int[] histogram = imageProcessor.getHistogram();
		final double[] dh = new double[histogram.length];
		for (int i = dh.length; i-- > 0;) {
			dh[i] = histogram[i];
		}
		int min = 0;
		for (int i = 0; i < histogram.length; ++i) {
			if (histogram[i] == 0) {
				min = i;
			} else {
				break;
			}
		}
		int max = histogram.length - 1;
		for (int i = histogram.length; i-- > 0;) {
			if (histogram[i] == 0) {
				max = i;
			} else {
				break;
			}
		}
		// final double[][] data = new double[2][max - min + 1];
		for (int i = max - min; i-- > 0;) {
			// data[0][i] = min + i;
			// data[1][i] = histogram[i + min];
			ret.add(min + i, histogram[i + min]);
		}
		return ret;
	}

	/**
	 * @param lineChart
	 *            A {@link JFreeChart} to wrap in a {@link ChartPanel}.
	 * @param lut
	 *            The {@link LUT} belonging to the {@code lineChart}.
	 * @return The {@link ChartPanel} with listener to markers.
	 */
	private ChartPanel createChartPanel(final JFreeChart lineChart,
			final LUT lut) {
		final ChartPanel chartPanel = new ChartPanel(lineChart);
		// lut.min = Math.max(lut.min, lineChart.getXYPlot().getDomainAxis()
		// .getRange().getLowerBound());
		// lut.max = Math.min(lut.max, lineChart.getXYPlot().getDomainAxis()
		// .getRange().getUpperBound());
		chartPanel.getChart().getXYPlot().addDomainMarker(
				new ValueMarker(lut.min), Layer.FOREGROUND);
		chartPanel.getChart().getXYPlot().addDomainMarker(
				new ValueMarker(lut.max), Layer.FOREGROUND);
		final ValueAxis origDomain = chartPanel.getChart().getXYPlot()
				.getDomainAxis();
		final ColouredAxis colouredAxis = new ColouredAxis(
				(NumberAxis) origDomain, lut);
		chartPanel.addChartMouseListener(new ChartMouseListener() {
			private ValueMarker selectedMarker = null;

			@Override
			public void chartMouseMoved(final ChartMouseEvent event)/* => */{
				// Do nothing
			}

			@Override
			public void chartMouseClicked(final ChartMouseEvent event)/* => */{
				final ChartEntity entity = event.getEntity();
				if (entity == null) {
					return;
				}
				final XYPlot xyPlot = lineChart.getXYPlot();
				final Collection<?> domainMarkers = xyPlot
						.getDomainMarkers(Layer.FOREGROUND);
				boolean clickedOnMarker = false;
				final ValueAxis domainAxis = xyPlot.getDomainAxis();
				final RectangleEdge domainAxisLocation = Plot
						.resolveDomainAxisLocation(xyPlot
								.getDomainAxisLocation(), xyPlot
								.getOrientation());
				for (final Object object : domainMarkers) {
					if (object instanceof ValueMarker) {
						final ValueMarker marker = (ValueMarker) object;
						final double valueToJava2D = domainAxis.valueToJava2D(
								marker.getValue(), chartPanel
										.getScreenDataArea(),
								domainAxisLocation);
						if (Math.abs(event.getTrigger().getX() - valueToJava2D) < 3) {
							selectedMarker = marker;
							clickedOnMarker = true;
							break;
						}
					}
				}
				if (!clickedOnMarker) {
					if (selectedMarker != null) {
						final double value = domainAxis.java2DToValue(event
								.getTrigger().getX(), chartPanel
								.getScreenDataArea(), domainAxisLocation);
						selectedMarker.setValue(value);
					}
					final List<Double> vals = new ArrayList<Double>();
					for (final Object object : domainMarkers) {
						if (object instanceof ValueMarker) {
							final ValueMarker marker = (ValueMarker) object;
							vals.add(Double.valueOf(marker.getValue()));
						}
					}
					Collections.sort(vals);
					lut.min = vals.get(0).doubleValue();
					lut.max = vals.get(vals.size() - 1).doubleValue();
					regenerateImage(AutoContrast.keepLast);
					selectedMarker = null;
				}
			}
		});
		chartPanel.getChart().getXYPlot().setDomainAxes(
				new ValueAxis[] { origDomain, colouredAxis });
		return chartPanel;
	}

	/**
	 * Performs a change in the selected {@link LUT}.
	 */
	protected class LUTChangeAction extends AbstractAction {
		private static final long serialVersionUID = 985037332491541636L;
		private final LUT lut;
		private final String channelName;

		/**
		 * Creates the proper action.
		 * 
		 * @param lut
		 *            The new lut values.
		 * @param text
		 *            The text of the {@link Action}.
		 * @param channelName
		 *            The channel name it belongs to.
		 */
		public LUTChangeAction(final LUT lut, final String text,
				final String channelName) {
			super(text);
			this.lut = lut;
			this.channelName = channelName;
		}

		/**
		 * Changes the {@link LUT} bounds for the defined channel and regenerate
		 * the the image.
		 * 
		 * @param e
		 *            Not used, may be {@code null}.
		 */
		@Override
		public void actionPerformed(final ActionEvent e)/* => */{
			final LUT orig = luts.get(channelName);
			final LUT newLut = (LUT) lut.clone();
			newLut.min = orig.min;
			newLut.max = orig.max;
			luts.put(channelName, newLut);
			regenerateImage(AutoContrast.keepLast);
		}
	}

	/**
	 * Creates a popup menu to modify the ranges or change the colours.
	 * 
	 * @param e
	 *            The {@link MouseEvent} for the action.
	 * @return A new popup menu containing the previously mentioned actions.
	 */
	private JPopupMenu createPopupMenu(final MouseEvent e) {
		final JPopupMenu popupMenu = new JPopupMenu("Colours");
		popupMenu.add(new AbstractAction("modify ranges") {
			private static final long serialVersionUID = 2203912915467547415L;

			public void actionPerformed(final ActionEvent action)/* => */{
				final int channelIndex = findChannelIndex(e, channelSelector);
				final String channelName = channelSelector.getValueMapping()
						.get(Integer.valueOf(channelIndex));
				final Entry<Integer, FormatReader> entry = getPlateRowColFieldTimeZMap()
						.entrySet().iterator().next();
				final FormatReader formatReader = entry.getValue();
				final ImagePlusReader imagePlusReader = ImagePlusReader
						.makeImagePlusReader(formatReader);
				imagePlusReader.setSeries(entry.getKey().intValue());
				try {
					showHistogram(channelIndex, channelName, imagePlusReader);
				} catch (final FormatException e1) {
					JOptionPane.showMessageDialog(controlsHandlerFactory
							.getContainer(SplitType.AdditionalInfo, CHANNEL),
							"Unable to handle image: " + e1.getMessage());
				} catch (final IOException e1) {
					JOptionPane.showMessageDialog(controlsHandlerFactory
							.getContainer(SplitType.AdditionalInfo, CHANNEL),
							"Unable to handle image: " + e1.getMessage());
				}
			}
		});
		final JMenu colours = new JMenu("change colours");
		popupMenu.add(colours);
		final int channelIndex = findChannelIndex(e, channelSelector);
		final String channelName = channelSelector.getValueMapping().get(
				Integer.valueOf(channelIndex));
		final ButtonGroup buttonGroup = new ButtonGroup();
		addMenuItem(buttonGroup, colours, new LUTChangeAction(ColourUtil.WHITE,
				"<html>Black &Rarr; White</html>", channelName));
		addMenuItem(buttonGroup, colours, new LUTChangeAction(
				ColourUtil.INV_WHITE, "<html>White &Rarr; Black</html>",
				channelName));
		addMenuItem(buttonGroup, colours, new LUTChangeAction(ColourUtil.RED,
				"<html>Black &Rarr; Red</html>", channelName));
		addMenuItem(buttonGroup, colours, new LUTChangeAction(
				ColourUtil.INV_ONLY_RED, "<html>Red &Rarr; Black</html>",
				channelName));
		addMenuItem(buttonGroup, colours, new LUTChangeAction(
				ColourUtil.INV_RED_KEEP_GREEN,
				"<html>Purple &Rarr; Blue</html>", channelName));
		addMenuItem(buttonGroup, colours, new LUTChangeAction(
				ColourUtil.INV_RED_KEEP_BLUE,
				"<html>Yellow &Rarr; Green</html>", channelName));
		addMenuItem(buttonGroup, colours, new LUTChangeAction(ColourUtil.BLUE,
				"<html>Black &Rarr; Blue</html>", channelName));
		addMenuItem(buttonGroup, colours, new LUTChangeAction(
				ColourUtil.INV_ONLY_BLUE, "<html>Blue &Rarr; Black</html>",
				channelName));
		addMenuItem(buttonGroup, colours, new LUTChangeAction(ColourUtil.GREEN,
				"<html>Black &Rarr; Green</html>", channelName));
		addMenuItem(buttonGroup, colours, new LUTChangeAction(
				ColourUtil.INV_ONLY_GREEN, "<html>Green &Rarr; Black</html>",
				channelName));
		addMenuItem(buttonGroup, colours, new LUTChangeAction(
				ColourUtil.INV_RED, "<html>White &Rarr; Cyan</html>",
				channelName));
		addMenuItem(buttonGroup, colours, new LUTChangeAction(
				ColourUtil.INV_BLUE, "<html>White &Rarr; Yellow</html>",
				channelName));
		addMenuItem(buttonGroup, colours, new LUTChangeAction(
				ColourUtil.INV_GREEN, "<html>White &Rarr; Purple</html>",
				channelName));
		buttonGroup.clearSelection();
		findSelected(buttonGroup, luts.get(channelName));
		return popupMenu;
	}

	/**
	 * Finds the matching (similar to {@code lut}) menu entry from {@code
	 * buttonGroup}, and selects the first.
	 * 
	 * @param buttonGroup
	 *            A {@link ButtonGroup} with the {@link JRadioButtonMenuItem}s.
	 * @param lut
	 *            A reference {@link LUT}.
	 */
	private void findSelected(final ButtonGroup buttonGroup, final LUT lut) {
		final int[] refRGBs = new int[256], oRGBs = new int[256];
		lut.getRGBs(refRGBs);
		for (final Enumeration<AbstractButton> iterator = buttonGroup
				.getElements(); iterator.hasMoreElements();) {
			final AbstractButton b = iterator.nextElement();
			final LUT lut2 = ((LUTChangeAction) b.getAction()).lut;
			lut2.getRGBs(oRGBs);
			if (Arrays.equals(refRGBs, oRGBs)) {
				buttonGroup.setSelected(b.getModel(), true);
				return;
			}
		}
	}

	/**
	 * Adds {@code lutChangeAction} to the {@code buttonGroup} and {@code
	 * colours} as a {@link JRadioButtonMenuItem}.
	 * 
	 * @param buttonGroup
	 *            A {@link ButtonGroup}.
	 * @param colours
	 *            A {@link JMenu}.
	 * @param lutChangeAction
	 *            The action to add.
	 */
	private void addMenuItem(final ButtonGroup buttonGroup,
			final JMenu colours, final LUTChangeAction lutChangeAction) {
		final JRadioButtonMenuItem menuItem = new JRadioButtonMenuItem(
				lutChangeAction);
		colours.add(menuItem);
		buttonGroup.add(menuItem);
	}

	/**
	 * Shows a histogram dialog for the referenced image.
	 * 
	 * @param channelIndex
	 *            The channel index for the histogram ({@code 1}-based).
	 * @param channelName
	 *            The name of the channel.
	 * @param imagePlusReader
	 *            An {@link ImagePlusReader} of the image.
	 * @throws FormatException
	 *             Problem opening the image.
	 * @throws IOException
	 *             Problem opening the image.
	 */
	private void showHistogram(final int channelIndex,
			final String channelName, final ImagePlusReader imagePlusReader)
			throws FormatException, IOException {
		final XYDataset histogram = createHistogram(
				new ImageProcessor[] { imagePlusReader
						.openProcessors(channelIndex - 1)[0] }, channelName);
		final JFreeChart lineChart = ChartFactory.createXYLineChart(
				"Histogram", null, null, histogram, PlotOrientation.VERTICAL,
				true, true, false);

		lineChart.getXYPlot().getDomainAxis().setRange(
				histogram.getXValue(0, 0),
				histogram.getXValue(0, histogram.getItemCount(0) - 1));
		final LogarithmicAxis rangeAxis = new LogarithmicAxis("Frequency");
		// rangeAxis.setAutoRangeIncludesZero(true);
		rangeAxis.setAllowNegativesFlag(true);
		lineChart.getXYPlot().setRangeAxis(rangeAxis);
		final LUT lut = luts.get(channelName);
		final double lutMin = lut.min, lutMax = lut.max;
		final ChartPanel chartPanel = createChartPanel(lineChart, lut);
		final int selectedOption = JOptionPane.showOptionDialog(
				controlsHandlerFactory.getContainer(SplitType.AdditionalInfo,
						CHANNEL), chartPanel, "", JOptionPane.OK_CANCEL_OPTION,
				JOptionPane.PLAIN_MESSAGE, null, null, null);
		switch (selectedOption) {
		case JOptionPane.OK_OPTION:
			// Do nothing, we have done the changes previously
			break;
		case JOptionPane.CANCEL_OPTION:
		case JOptionPane.CLOSED_OPTION:
			lut.min = lutMin;
			lut.max = lutMax;
			regenerateImage(AutoContrast.keepLast);
			break;
		default:
			throw new IllegalStateException("Unexpected option: "
					+ selectedOption);
		}
	}

	/**
	 * Changes the combination strategy of the current image.
	 */
	private class ColourCombinationAction extends AbstractAction {
		private static final long serialVersionUID = -6219194742481791076L;
		private final ConversionStrategy strategy;

		/**
		 * @param name
		 *            The name of the action.
		 * @param strategy
		 *            The {@link ConversionStrategy} to select in the action.
		 */
		public ColourCombinationAction(final String name,
				final ConversionStrategy strategy)/* => */{
			this(name, strategy, null);
		}

		/**
		 * @param name
		 *            The name of the action.
		 * @param strategy
		 *            The {@link ConversionStrategy} to select in the action.
		 * @param icon
		 *            The icon to use for the selection.
		 */
		public ColourCombinationAction(final String name,
				final ConversionStrategy strategy, final Icon icon) {
			super(name, icon);
			this.strategy = strategy;
		}

		/**
		 * Sets the strategy to the one declared in the constructor.
		 * 
		 * @param e
		 *            Not used, can be {@code null}.
		 */
		@Override
		public void actionPerformed(final ActionEvent e) {
			if (LociViewerNodeFastView.this.strategy != strategy) {
				LociViewerNodeFastView.this.strategy = strategy;
				regenerateImage(AutoContrast.keepLast);
			}
		}

	}

}

package ie.tcd.imm.hits.image.loci.view;

import ie.tcd.imm.hits.common.Format;
import ie.tcd.imm.hits.knime.view.ControlsHandler;
import ie.tcd.imm.hits.knime.view.SplitType;
import ie.tcd.imm.hits.util.ITriple;
import ie.tcd.imm.hits.util.NamedSelector;
import ie.tcd.imm.hits.util.OptionalNamedSelector;
import ie.tcd.imm.hits.util.Pair;
import ie.tcd.imm.hits.util.swing.VariableControl.ControlTypes;
import ie.tcd.imm.hits.view.impl.ControlsHandlerFactory;
import ie.tcd.imm.hits.view.util.SimpleWellSelection;
import ie.tcd.imm.hits.view.util.WellSelectionWidget;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ImageConverter;
import ij.process.ImageProcessor;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.awt.image.renderable.ParameterBlock;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedHashMap;
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
import javax.swing.BoundedRangeModel;
import javax.swing.DefaultBoundedRangeModel;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
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
import loci.plugins.util.ImagePlusReader;

import org.knime.core.data.RowKey;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeView;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.property.hilite.HiLiteHandler;
import org.knime.core.node.property.hilite.HiLiteListener;
import org.knime.core.node.property.hilite.KeyEvent;

import com.sun.media.jai.widget.DisplayJAI;

/**
 * <code>NodeView</code> for the "OMEViewer" Node. Shows images based on OME.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
public class LociViewerNodeFastView extends NodeView<LociViewerNodeModel> {
	/**
	 * TODO Javadoc!
	 * 
	 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
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

		protected void clear() {
			hilites.clear();
		}
	}

	private static final String GENERAL = "general";
	private static final String PLATE = "plate";
	private static final String ROW = "row";
	private static final String COLUMN = "column";
	private static final String FIELD = "field";
	private static final String CHANNEL = "channel";

	private static final NodeLogger logger = NodeLogger
			.getLogger(LociViewerNodeFastView.class);

	private JPanel panel = new JPanel();

	private ControlsHandler<SettingsModel, String, NamedSelector<String>> controlsHandlerFactory;
	@Nullable
	private OptionalNamedSelector<String> plateSelector;
	// @Nullable
	// private OptionalNamedSelector<String> rowSelector;
	// @Nullable
	// private OptionalNamedSelector<String> columnSelector;
	@Nullable
	private OptionalNamedSelector<String> fieldSelector;
	@Nullable
	private OptionalNamedSelector<String> channelSelector;
	private Map<String, Map<String, Map<Integer, Map<Integer, Map<Integer, FormatReader>>>>> joinTable;
	// private ImagePanelHits imagePanel = new ImagePanelHits();
	private DisplayJAI imagePanel = new DisplayJAI();
	// private ImageCanvas imagePanel = new ImageCanvas(new ImagePlus());
	private List<ActionListener> listenersToNotGCd = new ArrayList<ActionListener>();
	private JScrollPane imageScrollPane;
	private ImagePlus imagePlus;

	private WellSelectionWidget<String, NamedSelector<String>> wellSelection;

	private BoundedRangeModel zoomModel = new DefaultBoundedRangeModel(100, 0,
			10, 400);
	private JSlider zoomSlider = new JSlider(zoomModel);
	private SpinnerModel secondZoomModel = new SpinnerNumberModel(100, 10, 400,
			1);
	private JSpinner zoomSpinner = new JSpinner(secondZoomModel);
	private JPanel otherPanel;
	private SimpleWellSelection wellSel;
	private HiLiteListenerWells hiLiteListener;
	{
		zoomModel.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(final ChangeEvent e) {
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
			public void stateChanged(final ChangeEvent e) {
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
					final boolean isRetryable) throws RuntimeException {
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
		setComponent(new JScrollPane(panel));
		panel.setLayout(new javax.swing.BoxLayout(panel,
				javax.swing.BoxLayout.Y_AXIS));
		panel.setAlignmentY(Component.TOP_ALIGNMENT);
		panel.setAlignmentX(Component.LEFT_ALIGNMENT);
		imageScrollPane = new JScrollPane(imagePanel);
		imageScrollPane.setPreferredSize(new Dimension(800, 600));
		final JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		// panel.add(imageScrollPane);
		imagePanel.setPreferredSize(new Dimension(800, 600));
		// panel.add(new JScrollPane(controls));
		final JSplitPane horizontalSplitPane = new JSplitPane(
				JSplitPane.HORIZONTAL_SPLIT);
		otherPanel = new JPanel();
		// horizontalSplitPane.setRightComponent(otherPanel);
		// horizontalSplitPane.setLeftComponent(imageScrollPane);
		horizontalSplitPane.setOneTouchExpandable(true);
		final JPanel jPanel = new JPanel();
		// splitPane.setLeftComponent(horizontalSplitPane);
		splitPane.setLeftComponent(imageScrollPane);
		jPanel.add(controls);
		jPanel.add(otherPanel);
		// splitPane.setRightComponent(new JScrollPane(controls));
		splitPane.setRightComponent(new JScrollPane(jPanel));
		splitPane.setOneTouchExpandable(true);
		panel.add(splitPane);
		joinTable = Collections.emptyMap();
		otherPanel.add(zoomSlider);
		otherPanel.add(zoomSpinner);
		zoomModel.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(final ChangeEvent e) {
				repaintImage();
			}
		});
		final Format format = Format._96;
		final List<String> vals = createPlateVals(format);
		// wellSelection = new WellSelectionWidget<String,
		// NamedSelector<String>>(
		// format, new SettingsModelListSelection("", vals, "A1"),
		// SelectionType.Single, controlsHandlerFactory, null,
		// OptionalNamedSelector.createSingle("", NamedSelector
		// .createValues(vals)));
		// otherPanel.add(wellSelection.getComponentPanel());

		wellSel = new SimpleWellSelection(Format._96);
		hiLiteListener = new HiLiteListenerWells();
		otherPanel.add(wellSel);
		final JMenu hiliteMenu = new JMenu(HiLiteHandler.HILITE);
		getJMenuBar().add(hiliteMenu);
		hiliteMenu.add(new AbstractAction(HiLiteHandler.HILITE_SELECTED) {
			private static final long serialVersionUID = -6054254350021725863L;

			@Override
			public void actionPerformed(final ActionEvent e) {
				final Set<RowKey> selections = findSelectedRowKeys();
				getNodeModel().getInHiLiteHandler(0)
						.fireHiLiteEvent(selections);
			}
		});
		hiliteMenu.add(new AbstractAction(HiLiteHandler.UNHILITE_SELECTED) {
			private static final long serialVersionUID = 8360935725278415300L;

			@Override
			public void actionPerformed(final ActionEvent e) {
				final Set<RowKey> selections = findSelectedRowKeys();
				getNodeModel().getInHiLiteHandler(0).fireUnHiLiteEvent(
						selections);
			}
		});
		hiliteMenu.add(new AbstractAction(HiLiteHandler.CLEAR_HILITE) {
			private static final long serialVersionUID = 7449582397283093888L;

			@Override
			public void actionPerformed(final ActionEvent e) {
				getNodeModel().getInHiLiteHandler(0).fireClearHiLiteEvent();
			}
		});
	}

	/**
	 * @param format
	 * @return
	 */
	private List<String> createPlateVals(final Format format) {
		final List<String> ret = new ArrayList<String>(format.getWellCount());
		for (int i = 0; i < format.getRow(); ++i) {
			for (int j = 0; j < format.getCol(); ++j) {
				ret.add(Character.toString((char) ('A' + i)) + i);
			}
		}
		return ret;
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
		panel.revalidate();
		panel.repaint();
	}

	/**
	 * @param hilites
	 * @param plate
	 * @return
	 */
	protected Set<Pair<String, Integer>> select(
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
		// recreateRowSelector();
		updateWellSelection();
		final ActionListener actionListener = new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				// rowSelector.setActiveValues(findValues(joinTable.get(
				// plateSelector.getSelected()).keySet(), plateSelector
				// .getValueMapping()));
				// rowSelector.notifyListeners();
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
			public void actionPerformed(final ActionEvent e) {
				fieldSelector.notifyListeners();
			}
		});
	}

	protected static <T> Collection<Integer> findValues(final Set<T> set,
			final Map<Integer, T> valueMapping) {
		final Collection<Integer> ret = new LinkedHashSet<Integer>();
		for (final Entry<Integer, T> entry : valueMapping.entrySet()) {
			if (set.contains(entry.getValue())) {
				ret.add(entry.getKey());
			}
		}
		return ret;
	}

	protected void recreateRowSelector() {
		// deregister(rowSelector);
		// rowSelector = OptionalNamedSelector.createSingle(ROW, plateSelector
		// .getSelections().isEmpty() ? Collections.<String> emptySet()
		// : getPlateMap().keySet());
		// recreateColumnSelector();
		final ActionListener actionListener = new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				// columnSelector.setActiveValues(findValues(
				// asStringSet(getPlateRowMap().keySet()), columnSelector
				// .getValueMapping()));
				channelSelector.notifyListeners();
			}
		};
		// rowSelector.addActionListener(actionListener);
		listenersToNotGCd.add(actionListener);
		// controlsHandlerFactory.register(rowSelector, SplitType.SingleSelect,
		// GENERAL, ControlTypes.Buttons);
	}

	private void recreateColumnSelector() {
		// deregister(columnSelector);
		// columnSelector = OptionalNamedSelector.createSingle(COLUMN,
		// rowSelector
		// .getSelections().isEmpty() ? Collections.<String> emptySet()
		// : asStringSet(getPlateRowMap().keySet()));
		recreateFieldSelector();
		final ActionListener actionListener = new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				// fieldSelector.setActiveValues(findValues(
				// asStringSet(getPlateRowColMap().keySet()),
				// fieldSelector.getValueMapping()));
				channelSelector.notifyListeners();
			}
		};
		// columnSelector.addActionListener(actionListener);
		listenersToNotGCd.add(actionListener);
		// controlsHandlerFactory.register(columnSelector,
		// SplitType.SingleSelect,
		// GENERAL, ControlTypes.Buttons);
	}

	private void recreateFieldSelector() {
		deregister(fieldSelector);
		fieldSelector = OptionalNamedSelector.createSingle(FIELD,
		/*
		 * columnSelector.getSelections().isEmpty() ? Collections .<String>
		 * emptySet() :
		 */asStringSet(increase(getPlateRowColMap().keySet(), 0)));
		recreateChannelSelector();
		final ActionListener actionListener = new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				channelSelector.notifyListeners();
				// channelSelector.setActiveValues(channelSelector
				// .getActiveValues());
				// channelSelector.setActiveValues(findValues(
				// asStringSet(getPlateRowColFieldMap().keySet()),
				// channelSelector.getValueMapping()));
			}
		};
		fieldSelector.addActionListener(actionListener);
		listenersToNotGCd.add(actionListener);
		controlsHandlerFactory.register(fieldSelector, SplitType.SingleSelect,
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
		channelSelector = new OptionalNamedSelector<String>(CHANNEL,
				NamedSelector.createValues(Arrays.asList("Channel 1",
						"Channel 2")), Collections
						.singleton(Integer.valueOf(1)));
		final ActionListener actionListener = new ActionListener() {

			@Override
			public void actionPerformed(final ActionEvent e) {
				try {
					final Entry<Integer, FormatReader> entry = getPlateRowColFieldMap()
							.entrySet().iterator().next();
					final FormatReader formatReader = entry.getValue();
					final ImagePlusReader imagePlusReader = ImagePlusReader
							.makeImagePlusReader(formatReader);
					imagePlusReader.setSeries(entry.getKey().intValue());
					if (imagePanel.getWidth() == 0
							|| imagePanel.getHeight() == 0) {
						imagePanel.setSize(imagePlusReader.getSizeX(),
								imagePlusReader.getSizeY());
					}
					imageScrollPane.getViewport().setSize(
							imagePlusReader.getSizeX(),
							imagePlusReader.getSizeY());
					final Set<Integer> channels = channelSelector
							.getSelections();
					if (channels.size() == 1) {
						final int channel = channels.iterator().next()
								.intValue() - 1;
						final ImageProcessor[] openProcessors = imagePlusReader
								.openProcessors(channel);
						final ImageProcessor ip = /* reader */openProcessors[0];
						imagePlus = new ImagePlus("", ip);
						// imagePanel.prepareImage(imagePlus.getBufferedImage(),
						// null);
						repaintImage();
						// imagePanel.set(imagePlus.getBufferedImage());
						// imagePanel.setImage(imagePlus.getBufferedImage());
						return;
					}
					// final byte[] green = new byte[256];
					// Arrays.fill(green, (byte) -128);
					// final byte[] red = new byte[256];
					// final byte[] blue = new byte[256];
					// for (int i = red.length; i-- > 0;) {
					// blue[i] = (byte) (255 - i & 255);
					// red[i] = (byte) (255 - i & 255);
					// green[i] = (byte) (255 - i & 255);
					// }
					// Arrays.fill(red, (byte) 0);
					// Arrays.fill(green, (byte) 0);
					// Arrays.fill(blue, (byte) 0);
					// final LUT lut = new LUT(red, green, blue);
					// lut.min = 50;
					// lut.max = 200;
					// final ImagePlus ch1 = new ImagePlus("ch1",
					// imagePlusReader
					// .openProcessors(0)[0]);
					// // final LUT lut1 = new LUT(red, new byte[256], new
					// // byte[256]);
					// // final LUT lut2 = new LUT(new byte[256], new byte[256],
					// // green);
					// // ch1.getProcessor().setColorModel(lut1);
					// new ImageConverter(ch1).convertToGray8();
					//					
					// final ImagePlus ch2 = new ImagePlus("ch2",
					// imagePlusReader
					// .openProcessors(1)[0]);
					// // ch2.getProcessor().setColorModel(lut2);
					// new ImageConverter(ch2).convertToGray8();
					// // imageStack.addSlice("channel 1", imagePlusReader
					// // .openProcessors(0)[0]);
					// // imageStack.addSlice("channel 2", imagePlusReader
					// // .openProcessors(1)[0]);
					// imageStack.addSlice("channel 1", ch1.getProcessor());
					// imageStack.addSlice("channel 2", ch2.getProcessor());
					// final LutApplier lutApplier = new LutApplier();
					// // lutApplier.setup("", imagePlus0);
					// // lutApplier.run(imagePlus0.getProcessor());
					//					
					// // imagePlus0.getProcessor().setColorModel(lut);
					// final LutViewer lutViewer = new LutViewer();
					// // lutViewer.setup("", imagePlus0);
					// // lutViewer.run(imagePlus.getProcessor());
					final ImageStack imageStack = new ImageStack(
							imagePlusReader.getSizeX(), imagePlusReader
									.getSizeY());
					for (int i = 0; i < Math.min(3, imagePlusReader.getSizeC()); ++i) {
						final ImagePlus image = new ImagePlus(null,
								imagePlusReader.openProcessors(i)[0]);
						new ImageConverter(image).convertToGray8();
						imageStack.addSlice(null, image.getProcessor());
					}
					imagePlus = new ImagePlus("", imageStack);
					new ImageConverter(imagePlus).convertRGBStackToRGB();
					repaintImage();
					// imagePanel.prepareImage(imagePlus.getBufferedImage(),
					// null);
					// imagePanel.set(imagePlus.getBufferedImage());
					// imagePanel.setImage(imagePlus.getBufferedImage());
				} catch (final FormatException ex) {
					// TODO Auto-generated catch block
					ex.printStackTrace();
				} catch (final IOException ex) {
					// TODO Auto-generated catch block
					ex.printStackTrace();
				}
			}
		};
		channelSelector.addActionListener(actionListener);
		listenersToNotGCd.add(actionListener);
		controlsHandlerFactory.register(channelSelector,
				SplitType.PrimarySplit, GENERAL, ControlTypes.Buttons);
		if (getPlateRowColFieldMap() != null) {
			actionListener.actionPerformed(null);
		}

	}

	/**
	 * 
	 */
	protected void repaintImage() {
		final BufferedImage origImage = imagePlus.getBufferedImage();
		final float scale = zoomModel.getValue() / 100.0f;
		final ParameterBlock pb = new ParameterBlock();
		pb.addSource(origImage);
		pb.add(scale);
		pb.add(scale);
		pb.add(0.0f);
		pb.add(0.0f);
		pb.add(new InterpolationNearest());
		final RenderedOp planarImage = JAI.create("scale", pb);
		imagePanel.set(planarImage);
	}

	private static <T> Set<String> asStringSet(final Iterable<T> vals) {
		final Set<String> ret = new LinkedHashSet<String>();
		for (final T t : vals) {
			ret.add(t.toString());
		}
		return ret;
	}

	private void deregisterPreviousSelectors() {
		deregister(plateSelector);
		plateSelector = null;
		// deregister(rowSelector);
		// rowSelector = null;
		// deregister(columnSelector);
		// columnSelector = null;
		deregister(fieldSelector);
		fieldSelector = null;
		deregister(channelSelector);
		channelSelector = null;
	}

	/**
	 * @param selector
	 *            The selector to deregister from
	 *            {@link #controlsHandlerFactory} .
	 */
	private void deregister(final NamedSelector<String> selector) {
		if (selector != null) {
			controlsHandlerFactory.deregister(selector);
		}
	}

	private static <V> Map<Integer, V> setValues(final JSlider slider,
			final Set<V> set, final Map<Integer, V> values) {
		values.clear();
		final LinkedHashMap<Integer, V> ret = new LinkedHashMap<Integer, V>();
		final Hashtable<Integer, JLabel> labels = new Hashtable<Integer, JLabel>();
		int i = 0;
		for (final V o : set) {
			final Integer key = Integer.valueOf(i);
			ret.put(key, o);
			labels.put(key, new JLabel(o.toString()));
			++i;
		}
		values.putAll(ret);
		slider.setLabelTable(labels);
		slider.setMinimum(0);
		slider.setMaximum(labels.size() - 1);
		slider.setPaintLabels(true);
		return ret;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onClose() {
		// TODO: generated method stub
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onOpen() {
		// TODO: generated method stub
	}

	/**
	 * @return
	 */
	private Map<String, Map<Integer, Map<Integer, Map<Integer, FormatReader>>>> getPlateMap() {
		return joinTable.get(plateSelector.getSelected());
	}

	/**
	 * @return
	 */
	private Map<Integer, Map<Integer, Map<Integer, FormatReader>>> getPlateRowMap() {
		// return getPlateMap().get(rowSelector.getSelected());
		return getPlateMap().get(wellSel.getSelection().substring(0, 1));
	}

	/**
	 * @return
	 */
	private Map<Integer, Map<Integer, FormatReader>> getPlateRowColMap() {
		// return getPlateRowMap().get(
		// Integer.valueOf(columnSelector.getSelected()));
		return getPlateRowMap().get(
				Integer.valueOf(Integer.parseInt(wellSel.getSelection()
						.substring(1))));
	}

	/**
	 * @return
	 */
	private Map<Integer, FormatReader> getPlateRowColFieldMap() {
		return getPlateRowColMap().get(
				Integer.valueOf(fieldSelector.getSelected()));
	}

	/**
	 * @return
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
}

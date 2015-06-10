package ie.tcd.imm.hits.image.loci.view;

import ie.tcd.imm.hits.common.Format;
import ie.tcd.imm.hits.knime.view.ControlsHandler;
import ie.tcd.imm.hits.knime.view.SplitType;
import ie.tcd.imm.hits.util.ITriple;
import ie.tcd.imm.hits.util.Pair;
import ie.tcd.imm.hits.util.select.NamedSelector;
import ie.tcd.imm.hits.util.select.OptionalNamedSelector;
import ie.tcd.imm.hits.util.swing.VariableControl.ControlTypes;
import ie.tcd.imm.hits.view.impl.ControlsHandlerFactory;
import ie.tcd.imm.hits.view.util.SimpleWellSelection;
import ie.tcd.imm.hits.view.util.ZoomScrollPane;
import ie.tcd.imm.hits.view.util.Zoomable.ZoomListener;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ImageConverter;
import ij.process.ImageProcessor;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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
import javax.swing.BoundedRangeModel;
import javax.swing.DefaultBoundedRangeModel;
import javax.swing.JComponent;
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
 * <code>NodeView</code> for the "Loci Viewer" Node. Shows images based on
 * Bio-Formats.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
public class LociViewerNodeFastView extends NodeView<LociViewerNodeModel> {
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

	private ControlsHandler<SettingsModel, String, NamedSelector<String>> controlsHandlerFactory;
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
					final boolean isRetryable) throws RuntimeException/* => */{
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
		hiliteMenu
				.add(new AbstractAction(HiLiteHandler.HILITE_SELECTED)/* => */{
					private static final long serialVersionUID = -6054254350021725863L;

					@Override
					public void actionPerformed(final ActionEvent e) {
						final Set<RowKey> selections = findSelectedRowKeys();
						getNodeModel().getInHiLiteHandler(0).fireHiLiteEvent(
								selections);
					}
				});
		hiliteMenu
				.add(new AbstractAction(HiLiteHandler.UNHILITE_SELECTED)/* => */{
					private static final long serialVersionUID = 8360935725278415300L;

					@Override
					public void actionPerformed(final ActionEvent e) {
						final Set<RowKey> selections = findSelectedRowKeys();
						getNodeModel().getInHiLiteHandler(0).fireUnHiLiteEvent(
								selections);
					}
				});
		hiliteMenu.add(new AbstractAction(HiLiteHandler.CLEAR_HILITE)/* => */{
			private static final long serialVersionUID = 7449582397283093888L;

			@Override
			public void actionPerformed(final ActionEvent e) {
				getNodeModel().getInHiLiteHandler(0).fireClearHiLiteEvent();
			}
		});
		setComponent(splitPane);
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
		timeSelector = OptionalNamedSelector.createSingle(TIME,
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
		zSelector = OptionalNamedSelector.createSingle(Z,
				asStringSet(getPlateRowColFieldTimeMap().keySet()));
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
		channelSelector = new OptionalNamedSelector<String>(CHANNEL,
				NamedSelector.createValues(channelNames), Collections
						.singleton(Integer.valueOf(1)));
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
						repaintImage();
						return;
					}
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
				} catch (final FormatException ex) {
					imagePlus = new ImagePlus();
					logger.error("", ex);
				} catch (final IOException ex) {
					imagePlus = new ImagePlus();
					logger.error("", ex);
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
	 * Repaint the computed image with the current scaling.
	 */
	protected void repaintImage() {
		final RenderedImage origImage = (RenderedImage) imagePlus.getImage();
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
		// imagePanel.revalidate();
		// imageScrollPane.revalidate();
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
	private void deregister(final NamedSelector<String> selector) {
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
		return getPlateRowColFieldTimeMap().get(
				Double.valueOf(zSelector.getSelected()));
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
}

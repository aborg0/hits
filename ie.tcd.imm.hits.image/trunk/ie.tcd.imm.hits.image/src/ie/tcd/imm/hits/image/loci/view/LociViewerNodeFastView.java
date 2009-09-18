package ie.tcd.imm.hits.image.loci.view;

import ie.tcd.imm.hits.knime.view.ControlsHandler;
import ie.tcd.imm.hits.knime.view.SplitType;
import ie.tcd.imm.hits.util.NamedSelector;
import ie.tcd.imm.hits.util.OptionalNamedSelector;
import ie.tcd.imm.hits.util.swing.VariableControl.ControlTypes;
import ie.tcd.imm.hits.view.impl.ControlsHandlerFactory;
import ij.ImagePlus;
import ij.process.ImageProcessor;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import javax.annotation.Nullable;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;

import loci.formats.FormatException;
import loci.formats.FormatReader;
import loci.plugins.util.ImagePlusReader;

import org.hcdc.imgview.ImagePanel;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeView;
import org.knime.core.node.defaultnodesettings.SettingsModel;

/**
 * <code>NodeView</code> for the "OMEViewer" Node. Shows images based on OME.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
public class LociViewerNodeFastView extends NodeView<LociViewerNodeModel> {
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
	@Nullable
	private OptionalNamedSelector<String> rowSelector;
	@Nullable
	private OptionalNamedSelector<String> columnSelector;
	@Nullable
	private OptionalNamedSelector<String> fieldSelector;
	@Nullable
	private OptionalNamedSelector<String> channelSelector;
	private Map<String, Map<String, Map<Integer, Map<Integer, Map<Integer, FormatReader>>>>> joinTable;
	private ImagePanel imagePanel = new ImagePanel(800, 600);
	private List<ActionListener> listenersToNotGCd = new ArrayList<ActionListener>();

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
		setComponent(new JScrollPane(panel));
		panel.setLayout(new javax.swing.BoxLayout(panel,
				javax.swing.BoxLayout.Y_AXIS));
		panel.setAlignmentY(Component.TOP_ALIGNMENT);
		panel.setAlignmentX(Component.LEFT_ALIGNMENT);
		panel.add(new JScrollPane(imagePanel));
		imagePanel.setPreferredSize(new Dimension(800, 600));
		panel.add(new JScrollPane(controls));
		joinTable = Collections.emptyMap();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void modelChanged() {
		deregisterPreviousSelectors();
		joinTable = getNodeModel().getJoinTable();
		recreatePlateSelector();
		panel.revalidate();
		panel.repaint();
	}

	/**
	 * 
	 */
	private void recreatePlateSelector() {
		listenersToNotGCd.clear();
		deregister(plateSelector);
		plateSelector = OptionalNamedSelector.createSingle(PLATE, joinTable
				.keySet());
		recreateRowSelector();
		final ActionListener actionListener = new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				// rowSelector.setActiveValues(findValues(joinTable.get(
				// plateSelector.getSelected()).keySet(), plateSelector
				// .getValueMapping()));
				rowSelector.notifyListeners();
			}
		};
		plateSelector.addActionListener(actionListener);
		listenersToNotGCd.add(actionListener);
		controlsHandlerFactory.register(plateSelector, SplitType.SingleSelect,
				GENERAL, ControlTypes.List);
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
		deregister(rowSelector);
		rowSelector = OptionalNamedSelector.createSingle(ROW, plateSelector
				.getSelections().isEmpty() ? Collections.<String> emptySet()
				: getPlateMap().keySet());
		recreateColumnSelector();
		final ActionListener actionListener = new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				// columnSelector.setActiveValues(findValues(
				// asStringSet(getPlateRowMap().keySet()), columnSelector
				// .getValueMapping()));
				channelSelector.notifyListeners();
			}
		};
		rowSelector.addActionListener(actionListener);
		listenersToNotGCd.add(actionListener);
		controlsHandlerFactory.register(rowSelector, SplitType.SingleSelect,
				GENERAL, ControlTypes.Buttons);
	}

	private void recreateColumnSelector() {
		deregister(columnSelector);
		columnSelector = OptionalNamedSelector.createSingle(COLUMN, rowSelector
				.getSelections().isEmpty() ? Collections.<String> emptySet()
				: asStringSet(getPlateRowMap().keySet()));
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
		columnSelector.addActionListener(actionListener);
		listenersToNotGCd.add(actionListener);
		controlsHandlerFactory.register(columnSelector, SplitType.SingleSelect,
				GENERAL, ControlTypes.Buttons);
	}

	private void recreateFieldSelector() {
		deregister(fieldSelector);
		fieldSelector = OptionalNamedSelector.createSingle(FIELD,
				columnSelector.getSelections().isEmpty() ? Collections
						.<String> emptySet() : asStringSet(increase(
						getPlateRowColMap().keySet(), -1)));
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
		channelSelector = OptionalNamedSelector.createSingle(CHANNEL, Arrays
				.asList("Channel 1", "Channel 2"));
		final ActionListener actionListener = new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				ImageProcessor ip;
				try {
					final int channel = channelSelector.getSelections()
							.iterator().next().intValue() - 1;
					final Entry<Integer, FormatReader> entry = getPlateRowColFieldMap()
							.entrySet().iterator().next();
					final FormatReader formatReader = entry.getValue();
					final ImagePlusReader imagePlusReader = ImagePlusReader
							.makeImagePlusReader(formatReader);
					imagePlusReader.setSeries(entry.getKey().intValue());
					final ImageProcessor[] openProcessors = imagePlusReader
							.openProcessors(channel);
					ip = /* reader */openProcessors[0];
					imagePanel.setImage(new ImagePlus("", ip)
							.getBufferedImage());
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
				SplitType.SingleSelect, GENERAL, ControlTypes.Buttons);
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
		deregister(rowSelector);
		rowSelector = null;
		deregister(columnSelector);
		columnSelector = null;
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
		return getPlateMap().get(rowSelector.getSelected());
	}

	/**
	 * @return
	 */
	private Map<Integer, Map<Integer, FormatReader>> getPlateRowColMap() {
		return getPlateRowMap().get(
				Integer.parseInt(columnSelector.getSelected()));
	}

	/**
	 * @return
	 */
	private Map<Integer, FormatReader> getPlateRowColFieldMap() {
		return getPlateRowColMap().get(
				Integer.parseInt(fieldSelector.getSelected()));
	}

}

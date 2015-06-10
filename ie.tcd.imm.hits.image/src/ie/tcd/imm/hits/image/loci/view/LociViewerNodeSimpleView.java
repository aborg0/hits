package ie.tcd.imm.hits.image.loci.view;

import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ImageProcessor;

import java.awt.Component;
import java.awt.Dimension;
import java.io.IOException;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.Map.Entry;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import loci.formats.ChannelSeparator;
import loci.formats.FormatException;
import loci.formats.FormatReader;
import loci.formats.in.InCellReader;
import loci.plugins.util.ImagePlusReader;

import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeView;

import visad.DataReferenceImpl;
import visad.ImageFlatField;
import visad.LocalDisplay;
import visad.ScalarMap;
import visad.VisADException;
import visad.java2d.DisplayImplJ2D;

/**
 * <code>NodeView</code> for the "OMEViewer" Node. Shows images based on OME.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
public class LociViewerNodeSimpleView extends NodeView<LociViewerNodeModel> {
	private static final NodeLogger logger = NodeLogger
			.getLogger(LociViewerNodeSimpleView.class);

	private JPanel panel = new JPanel();

	/**
	 * Creates a new view.
	 * 
	 * @param nodeModel
	 *            The model (class: {@link LociViewerNodeModel})
	 */
	protected LociViewerNodeSimpleView(final LociViewerNodeModel nodeModel) {
		super(nodeModel);
		setComponent(new JScrollPane(panel));
		panel.setLayout(new javax.swing.BoxLayout(panel,
				javax.swing.BoxLayout.Y_AXIS));
		panel.setAlignmentY(Component.TOP_ALIGNMENT);
		panel.setAlignmentX(Component.LEFT_ALIGNMENT);

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void modelChanged() {
		final Map<String, Map<String, Map<Integer, Map<Integer, Map<Integer, FormatReader>>>>> joinTable = getNodeModel()
				.getJoinTable();
		final Map<String, Map<Integer, Map<Integer, Map<Integer, FormatReader>>>> rows = joinTable
				.values().iterator().next();
		final Map<Integer, Map<Integer, Map<Integer, FormatReader>>> columns = rows
				.values().iterator().next();
		final Map<Integer, Map<Integer, FormatReader>> fields = columns
				.values().iterator().next();
		final Map<Integer, FormatReader> ids = fields.values().iterator()
				.next();
		// final IMetadata metadata = ids.values().iterator().next();
		try {
			// final InCellReader r = new InCellReader()
			/*
			 * { private String myId; public InCellReader setCurrentId(final
			 * String id) throws FormatException, IOException { myId = id;
			 * return this; } public void setMetadataStore( final loci
			 * .formats.meta.MetadataStore store) { super.setMetadataStore
			 * (store); currentId = myId; }; }.setCurrentId(
			 * "C:/Users/bakosg.COLLEGE/tmp/AL-July-Screen Plate 4/19-07-2008 01.41.45 Plate 4.xdce"
			 * )
			 */;
			// final ImagePlusReader reader = ImagePlusReader
			// .makeImagePlusReader(ChannelSeparator
			// .makeChannelSeparator(r));
			// reader.setMetadataStore(metadata);
			// r.setMetadataStore(metadata);
			// MetadataTools.verifyMinimumPopulated(metadata);
			// r
			// .setId("C:/Users/bakosg.COLLEGE/tmp/AL-July-Screen Plate 4/19-07-2008 01.41.45 Plate 4.xdce");
			// MetadataTools.convertMetadata(metadata, r.getMetadataStore());
			final FormatReader r = ids.values().iterator().next();
			final ImagePlusReader reader = ImagePlusReader
					.makeImagePlusReader(ChannelSeparator
							.makeChannelSeparator(r));
			// MetadataTools.populatePixels(metadata, r);
			final int sizeX = r.getSizeX();
			final int sizeY = r.getSizeY();
			final ImageStack stack = new ImageStack(sizeX, sizeY);
			final int imageCount = r.getImageCount();
			logger.debug(imageCount);
			// for (int j = 0; j < Math.min(1, reader.getSeriesCount()); j++) {
			final int j = 0;
			r.setSeries(j);
			for (int i = 0; i < Math.min(3, imageCount); i++) {
				ImageProcessor ip;
				ip = /* reader */ImagePlusReader.makeImagePlusReader(r)
						.openProcessors(i)[0];
				// final ImagePlus bit8 = new ImagePlus("" + i, ip);
				// new ij.process.ImageConverter(bit8).convertToGray8();
				// stack.addSlice(1 + j + "_" + (i + 1), bit8.getProcessor());
				stack.addSlice(1 + j + "_" + (i + 1), ip);
				// logger.debug("i: " + i);
			}
			// }
			panel.removeAll();

			final ImagePlus imagePlus = new ImagePlus("aaa", stack);
			// new ImageConverter(imagePlus).convertRGBStackToRGB();
			// imagePlus.setFileInfo(imagePlus.getFileInfo());
			LocalDisplay displayImplJ2D;
			try {
				displayImplJ2D = new DisplayImplJ2D("");
				final ImageFlatField d = new ImageFlatField(
				// new BufferedImage(
						// sizeX, sizeY, BufferedImage.TYPE_INT_RGB)
						imagePlus.getBufferedImage());
				for (final ScalarMap map : d.getType().guessMaps(false)) {
					displayImplJ2D.addMap(map);
				}
				final DataReferenceImpl ref = new DataReferenceImpl("image");
				ref.setData(d);
				displayImplJ2D.addReference(ref);
				final Component component = displayImplJ2D.getComponent();
				component.setPreferredSize(new Dimension(800, 600));
				// final DataReferenceImpl channelRef = new DataReferenceImpl(
				// "channel");
				final JSlider plateSlider = new JSlider();
				final Map<Integer, String> plateValues = new LinkedHashMap<Integer, String>();
				setValues(plateSlider, joinTable.keySet(), plateValues);
				final JSlider rowSlider = new JSlider();
				final Map<Integer, String> rowValues = new LinkedHashMap<Integer, String>();
				final JSlider colSlider = new JSlider();
				final Map<Integer, Integer> colValues = new LinkedHashMap<Integer, Integer>();
				// rowListener.stateChanged(null);
				final JSlider fieldSlider = new JSlider();
				final Map<Integer, Integer> fieldValues = new LinkedHashMap<Integer, Integer>();
				final JSlider channelSlider = new JSlider();
				final LinkedHashMap<Integer, String> channelValues = new LinkedHashMap<Integer, String>();
				{
					for (int i = 0; i < r.getChannelDimLengths()[0]; ++i) {
						String channelName = // MetadataTools.asRetrieve(
						// r.getMetadataStore()).getLogicalChannelEmWave(
						// 0, i)
						// + " "
						// + MetadataTools
						// .asRetrieve(r.getMetadataStore())
						// .getLogicalChannelExWave(0, i);
						"channel " + (i + 1);

						if (reader.getReader() instanceof InCellReader) {
							final InCellReader rr = (InCellReader) reader
									.getReader();
							channelName = rr.getChannelNames().get(i);
						}
						channelValues.put(Integer.valueOf(i), channelName);
					}
				}
				setValues(channelSlider, new LinkedHashSet<String>(
						channelValues.values()), channelValues);
				final ChangeListener channelListener = new ChangeListener() {
					@Override
					public void stateChanged(final ChangeEvent e) {
						ImageProcessor ip;
						try {
							final int newValue = channelSlider.getValue();
							final Entry<Integer, FormatReader> pair = joinTable
									.get(
											plateValues.get(plateSlider
													.getValue()))
									.get(rowValues.get(rowSlider.getValue()))
									.get(colValues.get(colSlider.getValue()))
									.get(
											fieldValues.get(fieldSlider
													.getValue())).entrySet()
									.iterator().next();
							final Integer key = pair.getKey();
							final ImagePlusReader imagePlusReader = ImagePlusReader
									.makeImagePlusReader(pair.getValue());
							imagePlusReader.setSeries(key);
							final ImageProcessor[] openProcessors = imagePlusReader
									.openProcessors(newValue);
							ip = /* reader */openProcessors[0];
							ref.setData(new ImageFlatField(
									new ImagePlus("", ip).getBufferedImage()));
						} catch (final FormatException ex) {
							// TODO Auto-generated catch block
							ex.printStackTrace();
						} catch (final IOException ex) {
							// TODO Auto-generated catch block
							ex.printStackTrace();
						} catch (final VisADException ex) {
							// TODO Auto-generated catch block
							ex.printStackTrace();
						}
					}
				};
				channelSlider.addChangeListener(channelListener);
				// colListener.stateChanged(null);
				final ChangeListener fieldListener = new ChangeListener() {
					@Override
					public void stateChanged(final ChangeEvent e) {
						channelListener.stateChanged(e);
					}
				};
				fieldSlider.addChangeListener(fieldListener);
				final ChangeListener colListener = new ChangeListener() {

					@Override
					public void stateChanged(final ChangeEvent e) {
						fieldValues.clear();
						setValues(fieldSlider, joinTable.get(
								plateValues.get(plateSlider.getValue())).get(
								rowValues.get(rowSlider.getValue())).get(
								colValues.get(colSlider.getValue())).keySet(),
								fieldValues);
						fieldListener.stateChanged(e);
					}
				};
				colSlider.addChangeListener(colListener);
				final ChangeListener rowListener = new ChangeListener() {
					@Override
					public void stateChanged(final ChangeEvent e) {
						colValues.clear();
						setValues(colSlider, joinTable.get(
								plateValues.get(plateSlider.getValue())).get(
								rowValues.get(rowSlider.getValue())).keySet(),
								colValues);
						colListener.stateChanged(e);
					}
				};
				rowSlider.addChangeListener(rowListener);
				final ChangeListener plateListener = new ChangeListener() {
					@Override
					public void stateChanged(final ChangeEvent e) {
						rowValues.clear();
						setValues(rowSlider, new TreeSet<String>(joinTable.get(
								plateValues.get(plateSlider.getValue()))
								.keySet()), rowValues);
						rowListener.stateChanged(e);
					}
				};

				// final VisADSlider channelSlider = new VisADSlider(channelRef,
				// 0, imageCount - 1, 0, RealType.Generic, "channel");
				// final StepWidget stepWidget = new StepWidget(true);
				// stepWidget.setBounds(1, imageCount, 1);
				// final Cell channelCell = new CellImpl() {
				//
				// @Override
				// public void doAction() throws VisADException,
				// RemoteException {
				// ImageProcessor ip;
				// try {
				// // final int newValue = (int) (((Real) channelRef
				// // .getData()).getValue() + .5);
				// // if (Math.abs(newValue
				// // - ((Real) channelRef.getData()).getValue()) >
				// // .005) {
				// // channelRef.setData(new Real(newValue));
				// // return;
				// // }
				// final ImagePlusReader imagePlusReader = ImagePlusReader
				// .makeImagePlusReader(r);
				// final int key = joinTable.get(
				// plateValues.get(plateSlider.getValue()))
				// .get(rowValues.get(rowSlider.getValue()))
				// .get(colValues.get(colSlider.getValue()))
				// .get(
				// fieldValues.get(fieldSlider
				// .getValue())).entrySet()
				// .iterator().next().getKey().intValue();
				// imagePlusReader.setSeries(key);
				// ip = /* reader */imagePlusReader
				// .openProcessors(channelSlider.getValue())[0];
				// ref.setData(new ImageFlatField(
				// new ImagePlus("", ip).getBufferedImage()));
				// } catch (final FormatException e) {
				// // TODO Auto-generated catch block
				// e.printStackTrace();
				// } catch (final IOException e) {
				// // TODO Auto-generated catch block
				// e.printStackTrace();
				// }
				//
				// }
				// };
				panel.add(component);
				panel.add(channelSlider);
				panel.add(plateSlider);
				panel.add(rowSlider);
				panel.add(colSlider);
				panel.add(fieldSlider);
				panel.add(displayImplJ2D.getWidgetPanel());
				plateSlider.addChangeListener(plateListener);
				plateSlider.setValue(0);
				plateListener.stateChanged(null);
				// channelCell.addReference(channelRef);
				// panel.add(stepWidget);
			} catch (final VisADException e) {
				logger.error("Unable to create display", e);
				e.printStackTrace();
			}

			// panel.add(new ImageCanvas(imagePlus));

		} catch (final FormatException e) {
			panel.removeAll();
		} catch (final IOException e) {
			panel.removeAll();
		}
		panel.revalidate();
		panel.repaint();
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

}

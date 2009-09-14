package ie.tcd.imm.hits.image.loci.view;

import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ImageConverter;
import ij.process.ImageProcessor;

import java.awt.BorderLayout;
import java.io.IOException;
import java.util.Map;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import loci.formats.FormatException;
import loci.formats.FormatReader;
import loci.plugins.util.ImagePlusReader;
import loci.visbio.PanelManager;
import loci.visbio.SystemManager;
import loci.visbio.TaskManager;
import loci.visbio.VisBioEvent;
import loci.visbio.VisBioFrame;
import loci.visbio.WindowManager;
import loci.visbio.data.DataControls;
import loci.visbio.data.DataManager;
import loci.visbio.overlays.OverlayManager;
import loci.visbio.state.OptionManager;
import loci.visbio.state.StateManager;
import loci.visbio.util.SwingUtil;
import loci.visbio.view.DisplayManager;
import loci.visbio.view.DisplayWindow;

import org.knime.core.node.NodeView;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

/**
 * <code>NodeView</code> for the "OMEViewer" Node. Shows images based on OME.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
public class LociViewerNodeView extends NodeView<LociViewerNodeModel> {

	private JPanel panel = new JPanel();
	private static final VisBioFrame visBioFrame = new VisBioFrame();
	private DisplayWindow displayWindow;
	private DataManager dataManager;
	static {
		visBioFrame.setVisible(false);
		final OptionManager om = new OptionManager(visBioFrame);
		final StateManager sm = new StateManager(visBioFrame);
		final WindowManager wm = new WindowManager(visBioFrame);
		// visBioFrame.addManager(sm);
		visBioFrame.addManager(om);
		visBioFrame.addManager(wm);
		visBioFrame.addManager(new PanelManager(visBioFrame));
		visBioFrame.addManager(new DisplayManager(visBioFrame));
		visBioFrame.addManager(new OverlayManager(visBioFrame));

		visBioFrame.addManager(new TaskManager(visBioFrame));
		visBioFrame.addManager(new SystemManager(visBioFrame));

		try {
			UIManager.setLookAndFeel(UIManager
					.getCrossPlatformLookAndFeelClassName());
		} catch (final ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (final InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (final IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (final UnsupportedLookAndFeelException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// LAFUtil.initLookAndFeel();
	}

	/**
	 * Creates a new view.
	 * 
	 * @param nodeModel
	 *            The model (class: {@link LociViewerNodeModel})
	 */
	protected LociViewerNodeView(final LociViewerNodeModel nodeModel) {
		super(nodeModel);
		dataManager = new DataManager(visBioFrame);
		visBioFrame.addManager(dataManager);
		final DisplayManager displayManager = new DisplayManager(visBioFrame);
		visBioFrame.addManager(displayManager);
		displayManager.doEvent(new VisBioEvent(displayManager,
				VisBioEvent.LOGIC_ADDED, null, false));
		displayWindow = new DisplayWindow(displayManager, nodeModel.toString(),
				false);
		displayManager.addDisplay(displayWindow);
		displayWindow.setVisible(true);
		// displayManager.getControls().
		visBioFrame.addManager(displayManager);
		setComponent(new JScrollPane(panel));
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
			// MetadataTools.populatePixels(metadata, r);
			final int sizeX = r.getSizeX();
			final int sizeY = r.getSizeY();
			final ImageStack stack = new ImageStack(sizeX, sizeY);
			final int imageCount = r.getImageCount();
			// logger.debug(imageCount);
			// for (int j = 0; j < Math.min(1, reader.getSeriesCount()); j++) {
			final int j = 0;
			r.setSeries(j);
			for (int i = 0; i < Math.min(3, imageCount); i++) {
				ImageProcessor ip;
				ip = /* reader */ImagePlusReader.makeImagePlusReader(r)
						.openProcessors(i)[0];
				final ImagePlus bit8 = new ImagePlus("" + i, ip);
				new ij.process.ImageConverter(bit8).convertToGray8();
				stack.addSlice(1 + j + "_" + (i + 1), bit8.getProcessor());
				// logger.debug("i: " + i);
			}
			// }
			panel.removeAll();

			final ImagePlus imagePlus = new ImagePlus("aaa", stack);
			new ImageConverter(imagePlus).convertRGBStackToRGB();
			imagePlus.setFileInfo(imagePlus.getFileInfo());
			// panel.add(new ImageCanvas(imagePlus));

			// final DefaultMutableTreeNode root = dataManager.getDataRoot();
			// root.add(new DefaultMutableTreeNode(r));
			final DataControls dataControls = new DataControls(dataManager);
			dataManager.addData(new ProxyDataSet(r));
			// dataManager.addData(arg0)
			dataControls.addData(// Dataset.makeTransform(dataManager)
					new ProxyDataSet(r));
			// lay out frame components
			final PanelBuilder builder = new PanelBuilder(new FormLayout(
					"pref:grow", "fill:pref:grow"));
			builder.setDefaultDialogBorder();
			final CellConstraints cc = new CellConstraints();
			builder.add(dataControls, cc.xy(1, 1));
			final JScrollPane scroll = new JScrollPane(builder.getPanel());
			SwingUtil.configureScrollPane(scroll);
			panel.add(scroll, BorderLayout.CENTER);

			// new DataBrowser(imagePlus).setVisible(true);
		} catch (final FormatException e) {
			panel.removeAll();
		} catch (final IOException e) {
			panel.removeAll();
		}
		panel.revalidate();
		panel.repaint();
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

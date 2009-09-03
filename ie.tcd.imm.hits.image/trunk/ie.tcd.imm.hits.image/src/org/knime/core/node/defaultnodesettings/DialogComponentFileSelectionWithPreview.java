/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package org.knime.core.node.defaultnodesettings;

import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ImageConverter;
import ij.process.ImageProcessor;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Map.Entry;

import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import loci.formats.ChannelSeparator;
import loci.formats.CoreMetadata;
import loci.formats.tools.ImageInfo;
import loci.plugins.util.ImagePlusReader;

import org.hcdc.plate.ImagePanel;
import org.knime.core.node.NodeLogger;

/**
 * This class allows to select a file and show preview of metadata, and images.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
public class DialogComponentFileSelectionWithPreview extends
		DialogComponentFileChooser {
	private static final NodeLogger logger = NodeLogger
			.getLogger(DialogComponentFileSelectionWithPreview.class);
	private JPanel metaInfo;
	private ImagePanel imagePanel;

	/**
	 * @param stringModel
	 * @param historyID
	 * @param validExtensions
	 */
	public DialogComponentFileSelectionWithPreview(
			final SettingsModelString stringModel, final String historyID,
			final String... validExtensions) {
		this(stringModel, historyID, JFileChooser.OPEN_DIALOG, validExtensions);
	}

	/**
	 * @param stringModel
	 * @param historyID
	 * @param dialogType
	 * @param directoryOnly
	 */
	public DialogComponentFileSelectionWithPreview(
			final SettingsModelString stringModel, final String historyID,
			final int dialogType, final boolean directoryOnly) {
		this(stringModel, historyID, dialogType, directoryOnly, new String[0]);
	}

	/**
	 * @param stringModel
	 * @param historyID
	 * @param dialogType
	 * @param validExtensions
	 */
	public DialogComponentFileSelectionWithPreview(
			final SettingsModelString stringModel, final String historyID,
			final int dialogType, final String... validExtensions) {
		this(stringModel, historyID, dialogType, false, validExtensions);
	}

	/**
	 * @param stringModel
	 * @param historyID
	 * @param dialogType
	 * @param directoryOnly
	 * @param validExtensions
	 */
	public DialogComponentFileSelectionWithPreview(
			final SettingsModelString stringModel, final String historyID,
			final int dialogType, final boolean directoryOnly,
			final String... validExtensions) {
		super(stringModel, historyID, dialogType, directoryOnly,
				validExtensions);
		metaInfo = new JPanel();
		imagePanel = new ImagePanel(400, 400);
		// imagePanel.setPreferredSize(new Dimension(400, 400));
		getComponentPanel().add(new JScrollPane(imagePanel));
		getComponentPanel().add(new JScrollPane(metaInfo));
	}

	@Override
	protected void updateComponent() {
		super.updateComponent();
		try {
			final String imageUrl = ((SettingsModelString) getModel())
					.getStringValue();
			if (!new File(imageUrl).exists()) {
				throw new FileNotFoundException();
			}
			final ImageInfo imageInfo = new ImageInfo();
			imageInfo.testRead(new String[] { imageUrl });
			final ImagePlusReader imageReader = // new
			// ImagePlusReader();
			ImagePlusReader.makeImagePlusReader(ChannelSeparator
					.makeChannelSeparator(ImagePlusReader.makeImageReader()));

			final ImageStack stack;
			try {
				imageReader.setId(imageUrl);
				final int sizeX = imageReader.getSizeX();
				final int sizeY = imageReader.getSizeY();
				stack = new ImageStack(sizeX, sizeY);
				final int imageCount = imageReader.getImageCount();
				logger.debug(imageCount);
				for (int j = 0; j < Math.min(1, imageReader.getSeriesCount()); j++) {
					imageReader.setSeries(j);
					for (int i = 0; i < Math.min(3, imageCount); i++) {
						final ImageProcessor ip = imageReader.openProcessors(i)[0];
						final ImagePlus bit8 = new ImagePlus("" + i, ip);
						new ImageConverter(bit8).convertToGray8();
						stack.addSlice(1 + j + "_" + (i + 1), bit8
								.getProcessor());
						logger.debug("i: " + i);
					}
				}
				// final JDialog dialog = new JDialog();
				// final Panel[] panels = new Panel[imageReader
				// .getSeriesCount()];
				// for (int i = panels.length; i-- > 0;) {
				// panels[i] = new Panel();
				// }
				// final ThumbLoader thumbLoader = new ThumbLoader(
				// imageReader, panels, dialog, false);
				// final ExecutorService executor = new
				// ThreadPoolExecutor(
				// 1, 1, 900, TimeUnit.SECONDS,
				// new ArrayBlockingQueue<Runnable>(1, true));
				// final Future<?> future =
				// executor.submit(thumbLoader);
				// final JTabbedPane view = new JTabbedPane(
				// SwingConstants.LEFT);
				// dialog.add(new JScrollPane(view));
				// for (final Panel panel : panels) {
				// view.addTab("", panel);
				// }
				// dialog.setVisible(true);
				final ImagePlus imagePlus = new ImagePlus("xx", stack
				// .getProcessor(1)
				);// IJ.openImage(imageUrl);
				metaInfo.removeAll();
				final StringBuilder fileInfo = new StringBuilder(
						imagePlus == null ? ""
								: imagePlus.getFileInfo() == null ? ""
										: imagePlus.getFileInfo().toString());
				final Hashtable<?, ?> seriesMetadata = imageReader
						.getSeriesMetadata();
				fileInfo.append("\nSeries\n");
				for (final Entry<?, ?> entry : seriesMetadata.entrySet()) {
					fileInfo.append(entry.getKey()).append(" -> ").append(
							entry.getValue()).append("\n");
				}
				fileInfo.append("\nCoreMeta\n");
				final CoreMetadata[] coreMetadata = imageReader
						.getCoreMetadata();
				fileInfo.append(coreMetadata[0].seriesMetadata).append("\n");
				// for (final CoreMetadata cm : coreMetadata) {
				// fileInfo.append(cm.imageCount).append("\n");
				// }

				final Hashtable<?, ?> globalMetadata = imageReader
						.getGlobalMetadata();
				for (final Entry<?, ?> entry : globalMetadata.entrySet()) {
					fileInfo.append(entry.getKey()).append(" -> ").append(
							entry.getValue()).append("\n");
				}
				metaInfo.add(new JScrollPane(new JTextArea(fileInfo.toString(),
						5, 80)));
				logger.info(fileInfo);
				final ImageConverter imageConverter = new ImageConverter(
						imagePlus);
				imageConverter.convertRGBStackToRGB();
				final Image image = imagePlus.getImage();
				assert image != null;
				imagePanel.setImage(image);
				// future.get(3, TimeUnit.SECONDS);
				// thumbLoader.stop();
			} finally {
				imageReader.close();
			}
			// imageReader.setMetadataStore(null);

		} catch (final FileNotFoundException ex) {
			if (imagePanel != null) {
				imagePanel.setImage(new BufferedImage(400, 400,
						BufferedImage.TYPE_INT_RGB));
			}
			if (metaInfo != null) {
				metaInfo.removeAll();
			}
		} catch (final IOException ex) {
			handleThrowable(ex);
		} catch (final RuntimeException ex) {
			handleThrowable(ex);
		} catch (final ClassCircularityError ex) {
			handleThrowable(ex);
		} catch (final Throwable t) {
			handleThrowable(t);
		}
		getComponentPanel().revalidate();
		getComponentPanel().repaint();
	}

	private void handleThrowable(final Throwable ex) {
		logger.debug(ex.getMessage(), ex);
		if (imagePanel != null) {
			imagePanel.setImage(new BufferedImage(400, 400,
					BufferedImage.TYPE_INT_RGB));
		}
		if (metaInfo != null) {
			metaInfo.removeAll();
		}
	}
}

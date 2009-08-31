/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package org.knime.core.node.defaultnodesettings;

import ij.ImagePlus;
import ij.ImageStack;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import loci.formats.ChannelSeparator;
import loci.formats.FormatException;
import loci.formats.in.InCellReader;
import loci.plugins.util.ImagePlusReader;

import org.hcdc.imgview.ImagePanel;
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
		final JPanel metaInfo = new JPanel();
		try {
			final InCellReader inCellReader = new InCellReader();
			try {
				inCellReader
						.setId("/media/disk/Users/bakosg.COLLEGE/tmp/AL-July-Screen Plate 4/19-07-2008 01.41.45 Plate 4.xdce");
			} catch (final FormatException e1) {
				logger.error("", e1);
				e1.printStackTrace();
			} catch (final IOException e1) {
				logger.error("", e1);
				e1.printStackTrace();
			}
		} catch (final Throwable t) {
			logger.error(t.getMessage(), t);
			t.printStackTrace();
		}
		final ImagePanel imagePanel = new ImagePanel(400, 400);
		getModel().addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(final ChangeEvent e) {
				try {
					final String imageUrl = ((SettingsModelString) getModel())
							.getStringValue();
					if (!new File(imageUrl).exists()) {
						throw new FileNotFoundException();
					}
					final ImagePlusReader imageReader = // new
					// ImagePlusReader();
					ImagePlusReader.makeImagePlusReader(ChannelSeparator
							.makeChannelSeparator(ImagePlusReader
									.makeImageReader()));
					final ImageStack stack;
					try {
						imageReader.setId(imageUrl);
						final int sizeX = imageReader.getSizeX();
						final int sizeY = imageReader.getSizeY();
						stack = new ImageStack(sizeX, sizeY);
						final int imageCount = imageReader.getImageCount();
						logger.debug(imageCount);
						for (int i = 0; i < imageCount; i++) {
							stack.addSlice("" + (i + 1), imageReader
									.openProcessors(i)[0]);
							logger.debug("i: " + i);
						}
					} finally {
						imageReader.close();
					}
					// imageReader.setMetadataStore(null);
					final ImagePlus imagePlus = new ImagePlus("xx", stack
							.getProcessor(1));// IJ.openImage(imageUrl);
					metaInfo.removeAll();
					final String fileInfo = imagePlus == null ? "" : imagePlus
							.getFileInfo() == null ? "" : imagePlus
							.getFileInfo().toString();
					metaInfo.add(new JLabel(fileInfo));
					final Image image = imagePlus.getImage();
					imagePanel.setImage(image);
				} catch (final IOException ex) {
					imagePanel.setImage(new BufferedImage(400, 400,
							BufferedImage.TYPE_INT_RGB));
					logger.debug(ex.getMessage(), ex);
					// ex.printStackTrace();
				} catch (final RuntimeException ex) {
					imagePanel.setImage(new BufferedImage(400, 400,
							BufferedImage.TYPE_INT_RGB));
					logger.error(ex.getMessage(), ex);
					// ex.printStackTrace();
					// } catch (final FormatException ex) {
					// imagePanel.setImage(new BufferedImage(400, 400,
					// BufferedImage.TYPE_INT_RGB));
					// logger
					// .debug("Problem reading file: " + ex.getMessage(),
					// ex);
					// ex.printStackTrace();
				} catch (final ClassCircularityError ex) {
					imagePanel.setImage(new BufferedImage(400, 400,
							BufferedImage.TYPE_INT_RGB));
					logger.debug("Coding problem: " + ex.getMessage(), ex);
					// ex.printStackTrace();
				} catch (final Throwable t) {
					imagePanel.setImage(new BufferedImage(400, 400,
							BufferedImage.TYPE_INT_RGB));
					logger.debug("Coding problem: " + t.getMessage(), t);
					// t.printStackTrace();
				}
			}
		});
		getComponentPanel().add(new JScrollPane(imagePanel));
		getComponentPanel().add(new JScrollPane(metaInfo));
	}
}

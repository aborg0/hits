/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package org.knime.core.node.defaultnodesettings;

import ie.tcd.imm.hits.image.loci.read.LociReaderNodeModel;
import ie.tcd.imm.hits.image.util.ConvertImage;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ImageConverter;
import ij.process.ImageProcessor;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;
import javax.swing.DefaultListModel;
import javax.swing.JFileChooser;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import loci.formats.ChannelSeparator;
import loci.formats.CoreMetadata;
import loci.formats.gui.ExtensionFileFilter;
import loci.plugins.util.ImagePlusReader;

import org.knime.core.node.NodeLogger;

import com.sun.media.jai.widget.DisplayJAI;

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
	private DisplayJAI imagePanel;
	private String extension = "";
	private JList fileNames = new JList(new DefaultListModel());
	private ExecutorService threadPoolExecutor = new ThreadPoolExecutor(1, 1,
			60, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(1));
	@Nullable
	private volatile Future<?> future;

	/**
	 * @param stringModel
	 *            The used {@link SettingsModelString}.
	 * @param historyID
	 *            The history id used for checking history of previous values.
	 * @param validExtensions
	 *            The accepted extension of values.
	 */
	public DialogComponentFileSelectionWithPreview(
			final SettingsModelString stringModel, final String historyID,
			final String... validExtensions) {
		this(stringModel, historyID, JFileChooser.OPEN_DIALOG, validExtensions);
	}

	/**
	 * @param stringModel
	 *            The used {@link SettingsModelString}.
	 * @param historyID
	 *            The history id used for checking history of previous values.
	 * @param dialogType
	 *            The type of the file open dialog. ({@link JFileChooser})
	 * @param directoryOnly
	 *            Accepts only folders, or allow files too.
	 */
	public DialogComponentFileSelectionWithPreview(
			final SettingsModelString stringModel, final String historyID,
			final int dialogType, final boolean directoryOnly) {
		this(stringModel, historyID, dialogType, directoryOnly, new String[0]);
	}

	/**
	 * @param stringModel
	 *            The used {@link SettingsModelString}.
	 * @param historyID
	 *            The history id used for checking history of previous values.
	 * @param dialogType
	 *            The type of the file open dialog. ({@link JFileChooser})
	 * @param validExtensions
	 *            The accepted extension of values.
	 */
	public DialogComponentFileSelectionWithPreview(
			final SettingsModelString stringModel, final String historyID,
			final int dialogType, final String... validExtensions) {
		this(stringModel, historyID, dialogType, false, validExtensions);
	}

	/**
	 * @param stringModel
	 *            The used {@link SettingsModelString}.
	 * @param historyID
	 *            The history id used for checking history of previous values.
	 * @param dialogType
	 *            The type of the file open dialog. ({@link JFileChooser})
	 * @param directoryOnly
	 *            Accepts only folders, or allow files too.
	 * @param validExtensions
	 *            The accepted extension of values.
	 */
	public DialogComponentFileSelectionWithPreview(
			final SettingsModelString stringModel, final String historyID,
			final int dialogType, final boolean directoryOnly,
			final String... validExtensions) {
		super(stringModel, historyID, dialogType, directoryOnly,
				validExtensions);
		metaInfo = new JPanel();
		imagePanel = new DisplayJAI();
		getComponentPanel().add(new JScrollPane(imagePanel));
		getComponentPanel().add(new JScrollPane(metaInfo));
		getComponentPanel().add(new JScrollPane(fileNames));
		fileNames.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		fileNames.getSelectionModel().addListSelectionListener(
				new ListSelectionListener() {
					@Override
					public void valueChanged(final ListSelectionEvent e) {
						stopPreview();
						final Object selectedValue = fileNames
								.getSelectedValue();
						if (selectedValue != null) {
							final File modelFile = new File(
									((SettingsModelString) getModel())
											.getStringValue());
							final File file = modelFile.isFile() ? modelFile
									: new File(modelFile.getAbsolutePath()
											.concat((String) selectedValue));
							updatePreview(file.getAbsolutePath());
						}
					}
				});
	}

	@Override
	protected void updateComponent() {
		super.updateComponent();
		final SettingsModelString model = (SettingsModelString) getModel();
		final String imageUrl = model.getStringValue();
		if (extension == null) {
			extension = "";
		}
		updateList(imageUrl, extension);
		updatePreview(imageUrl);
	}

	/**
	 * @param imageUrl
	 *            The new url to use.
	 */
	private void updatePreview(final String imageUrl) {
		final File file = new File(imageUrl);
		if (!file.exists() || !file.isFile()) {
			if (imagePanel != null) {
				stopPreview();
				try {
					final BufferedImage bi = new BufferedImage(400, 400,
							BufferedImage.TYPE_INT_RGB);
					bi.getGraphics().drawString(
							"Error loading image, file does not exist", 100,
							200);
					imagePanel.set(bi);
				} catch (final RuntimeException e) {
					logger.debug("Problem with handling exception: "
							+ e.getMessage());
				}
			}
			if (metaInfo != null) {
				metaInfo.removeAll();
			}
			// updateList(imageUrl, extension);
			return;
		}
		// updateList(imageUrl, extension);
		final StringBuilder fileInfo = new StringBuilder();
		final ImagePlus[] pointer = new ImagePlus[1];

		final Runnable runnable = new Runnable() {
			public void run() {
				final ImagePlusReader imageReader = ImagePlusReader
						.makeImagePlusReader(new ChannelSeparator(
								ImagePlusReader.makeImageReader()));

				try {

					try {
						imageReader.setId(imageUrl);
						final int sizeX = imageReader.getSizeX();
						final int sizeY = imageReader.getSizeY();
						final ImageStack stack = new ImageStack(sizeX, sizeY);
						final int imageCount = imageReader.getImageCount();
						logger.debug(imageCount);
						for (int j = 0; j < Math.min(1, imageReader
								.getSeriesCount()); j++) {
							imageReader.setSeries(j);
							for (int i = 0; i < Math.min(3, imageCount); i++) {
								final ImageProcessor ip = imageReader
										.openProcessors(i)[0];
								final ImagePlus bit8 = new ImagePlus("" + i, ip);
								new ImageConverter(bit8).convertToGray8();
								stack.addSlice(1 + j + "_" + (i + 1), bit8
										.getProcessor()
								// ip
										);
								logger.debug("i: " + i);
							}
						}
						final ImagePlus imagePlus = new ImagePlus("xx", stack);
						metaInfo.removeAll();
						fileInfo.append(imagePlus == null ? "" : imagePlus
								.getFileInfo() == null ? "" : imagePlus
								.getFileInfo().toString());
						final Hashtable<?, ?> seriesMetadata = imageReader
								.getSeriesMetadata();
						fileInfo.append("\nSeries\n");
						for (final Entry<?, ?> entry : seriesMetadata
								.entrySet()) {
							fileInfo.append(entry.getKey()).append(" -> ")
									.append(entry.getValue()).append("\n");
						}
						fileInfo.append("\nCoreMeta\n");
						final CoreMetadata[] coreMetadata = imageReader
								.getCoreMetadata();
						fileInfo.append(coreMetadata[0].seriesMetadata).append(
								"\n");

						// final Hashtable<?, ?> globalMetadata = imageReader
						// .getGlobalMetadata();
						// for (final Entry<?, ?> entry : globalMetadata
						// .entrySet()) {
						// fileInfo.append(entry.getKey()).append(" -> ")
						// .append(entry.getValue()).append("\n");
						// }
						pointer[0] = imagePlus;
					} finally {
						imageReader.close();
					}
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							metaInfo.removeAll();
							metaInfo.add(new JScrollPane(new JTextArea(fileInfo
									.toString(), 5, 80)));
							logger.info(fileInfo);
							final ImageConverter imageConverter = new ImageConverter(
									pointer[0]);
							imageConverter.convertRGBStackToRGB();
							// TODO update when newer ImageJ is available
							final BufferedImage image = ConvertImage
									.toBufferedImage(pointer[0].getImage());
							// .getBufferedImage();
							assert image != null;
							imagePanel.set(image);
							getComponentPanel().revalidate();
							getComponentPanel().repaint();
						}
					});
				} catch (final FileNotFoundException ex) {
					handleThrowable(ex, false);
				} catch (final IOException ex) {
					handleThrowable(ex);
				} catch (final RuntimeException ex) {
					handleThrowable(ex);
				} catch (final ClassCircularityError ex) {
					handleThrowable(ex);
				} catch (final Throwable t) {
					handleThrowable(t);
				}
			}
		};
		stopPreview();
		if (threadPoolExecutor == null) {
			return;
		}
		future = threadPoolExecutor.submit(runnable);
	}

	private void handleThrowable(final Throwable ex) {
		handleThrowable(ex, true);
	}

	private void handleThrowable(final Throwable ex, final boolean log) {
		if (log) {
			logger.debug(ex.getMessage(), ex);
		}
		if (imagePanel != null) {
			try {
				imagePanel.set(new BufferedImage(400, 400,
						BufferedImage.TYPE_INT_RGB));
			} catch (final RuntimeException e) {
				logger.debug("Problem with handling exception: "
						+ e.getMessage());
			}
		}
		if (metaInfo != null) {
			metaInfo.removeAll();
		}
	}

	/** Stops the preview thread. */
	public synchronized void stopPreview() {
		if (threadPoolExecutor != null && future != null) {
			future.cancel(true);
			future = null;
			// final List<Runnable> notExecuted =
			// threadPoolExecutor.shutdownNow();

			// if (notExecuted.size() > 0) {
			// logger.debug("Terminated " + notExecuted.size() + " task(s).");
			// }
		}
	}

	/**
	 * Sets the new extension for filter, and update the list of matching files.
	 * 
	 * @param extension
	 *            A {@code |} separated list of extensions.
	 */
	public void setExtension(final String extension) {
		if (!this.extension.equals(extension)) {
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					updateList(((SettingsModelString) getModel())
							.getStringValue(), extension);
				}
			});
		}
		this.extension = extension;
	}

	private void updateList(final String folderName, final String extension) {
		stopPreview();
		if (fileNames == null) {
			return;
		}
		((DefaultListModel) fileNames.getModel()).clear();
		final File parent = new File(folderName);
		if (parent.isFile()) {
			((DefaultListModel) fileNames.getModel()).addElement(parent
					.getName());
		}
		final List<String> contents = new ArrayList<String>();
		final String[] extensions = extension.split("\\|");
		for (int i = extensions.length; i-- > 0;) {
			while (extensions[i].length() > 0 && extensions[i].charAt(0) == '.') {
				extensions[i] = extensions[i].substring(1);
			}
		}
		final java.io.FileFilter fileFilter = new ExtensionFileFilter(
				extensions, "");
		LociReaderNodeModel.visit(parent, parent, contents, fileFilter);
		for (final String string : contents) {
			((DefaultListModel) fileNames.getModel()).addElement(string);
		}
	}
}

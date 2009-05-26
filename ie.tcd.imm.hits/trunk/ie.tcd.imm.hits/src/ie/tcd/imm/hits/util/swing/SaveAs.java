/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package ie.tcd.imm.hits.util.swing;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.color.ColorSpace;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.annotation.Nullable;
import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;

/**
 * An action to save a component to {@link ImageType} formats.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
public abstract class SaveAs extends AbstractAction {
	private static final long serialVersionUID = -6981404140757972969L;
	@Nullable
	private final Component parent;
	/** The drawing component */
	private final Component component;
	private final ImageType type;
	private final JFileChooser fileChooser;

	/**
	 * Creates the proper action to save to the selected {@code type} format.
	 * 
	 * @param parent
	 *            The parent component (for the file chooser dialog).
	 * @param name
	 *            The name of the action.
	 * @param drawingPane
	 *            The {@link Component} to draw.
	 * @param type
	 *            Type of the result image.
	 * @return The new {@link SaveAs} {@link Action}.
	 */
	public static SaveAs createAction(@Nullable final Component parent,
			final String name, final Component drawingPane, final ImageType type) {
		switch (type) {
		case png:
			return new PngSaveAs(parent, name, drawingPane);
		case svg:
			return new SvgSaveAs(parent, name, drawingPane);
		default:
			throw new UnsupportedOperationException(
					"Saving not supported to format: " + type);
		}
	}

	/**
	 * @param parent
	 *            Parent component (for file chooser).
	 * @param name
	 *            Name of the {@link Action}.
	 * @param drawingPane
	 *            The component to draw.
	 * @param type
	 *            Type of image to save.
	 */
	protected SaveAs(@Nullable final Component parent, final String name,
			final Component drawingPane, final ImageType type) {
		super(name);
		this.parent = parent;
		this.component = drawingPane;
		this.type = type;
		fileChooser = new JFileChooser();
		getFileChooser().setFileFilter(
				new FileNameExtensionFilter(type.getDescription(), type
						.getExtensions()));
	}

	@Override
	public void actionPerformed(final ActionEvent e) {
		saveToFile(true);
		getFileChooser().setSelectedFile(null);
	}

	/**
	 * Saves the file based on the {@link #fileChooser}'s actual state.
	 * 
	 * @param infoMessage
	 *            On success it should popup an information dialog, or not.
	 * @return On success {@code true} else {@code false}.
	 */
	public boolean saveToFile(final boolean infoMessage) {
		final boolean db = component.isDoubleBuffered();
		((JComponent) component).setDoubleBuffered(false);
		checkForOverwrite: while (true) {
			try {
				if (getFileChooser().getSelectedFile() == null) {
					switch (getFileChooser().showSaveDialog(parent)) {
					case JFileChooser.APPROVE_OPTION:
						break;
					case JFileChooser.CANCEL_OPTION:
						return false;
					}
				}
				final File selectedFile = addMissingExtension(getFileChooser()
						.getSelectedFile(), type.getExtensions()[0]);
				if (selectedFile.exists()) {
					switch (JOptionPane.showConfirmDialog(parent, "Overwrite "
							+ selectedFile.getAbsolutePath() + "?",
							"Overwrite existing file?",
							JOptionPane.YES_NO_CANCEL_OPTION)) {
					case JOptionPane.YES_OPTION:
						break;
					case JOptionPane.NO_OPTION:
						fileChooser.setSelectedFile(null);
						continue checkForOverwrite;
					case JOptionPane.CANCEL_OPTION:
						if (infoMessage) {
							JOptionPane.showMessageDialog(parent,
									"Saving of file ("
											+ selectedFile.getAbsolutePath()
											+ ") cancelled.", "Save cancelled",
									JOptionPane.INFORMATION_MESSAGE);
						}
						return false;
					}
				}
				saveToFile(component, selectedFile);
				if (infoMessage) {
					JOptionPane.showMessageDialog(parent,
							"Successfully saved to: " + selectedFile);
				}
				return true;
			} catch (final Throwable t) {
				JOptionPane.showMessageDialog(parent,
						"Error occured during save: " + t.getMessage(),
						"Error saving", JOptionPane.ERROR_MESSAGE);
			} finally {
				((JComponent) component).setDoubleBuffered(db);
			}
		}
	}

	/**
	 * Adds {@code extension} to {@code selectedFile}, if not yet present.
	 * 
	 * @param selectedFile
	 *            A {@link File}.
	 * @param extension
	 *            A (preferably non-empty) {@link String}.
	 * @return A {@link File} with the given {@code extension} and same
	 *         path/name as {@code selectedFile}.
	 */
	private File addMissingExtension(final File selectedFile,
			final String extension) {
		return selectedFile.getName().toLowerCase().endsWith(
				extension.toLowerCase()) ? selectedFile : new File(selectedFile
				.getParentFile(), selectedFile.getName()
				+ (extension.length() < 0 && extension.charAt(0) == '.' ? ""
						: ".") + extension);
	}

	/**
	 * @param component
	 *            The component to save.
	 * @param selectedFile
	 *            The selected file.
	 */
	protected abstract void saveToFile(Component component, File selectedFile);

	/**
	 * @return the fileChooser
	 */
	public JFileChooser getFileChooser() {
		return fileChooser;
	}

	private static final class PngSaveAs extends SaveAs {
		private static final long serialVersionUID = 4335469648086107939L;

		PngSaveAs(@Nullable final Component parent, final String name,
				final Component drawingPane) {
			super(parent, name, drawingPane, ImageType.png);
		}

		@Override
		protected void saveToFile(final Component component,
				final File selectedFile) {
			final BufferedImage bi = new BufferedImage(component.getWidth(),
					component.getHeight(), ColorSpace.TYPE_RGB);
			final Graphics2D g = bi.createGraphics();
			g.setColor(Color.BLACK);
			g.setBackground(Color.WHITE);
			g.setClip(0, 0, component.getWidth(), component.getHeight());
			component.paintAll(g);
			try {
				ImageIO.write(bi, "png", selectedFile);
			} catch (final IOException e1) {
				throw new RuntimeException(e1);
			}
		}

	}

	private static final class SvgSaveAs extends SaveAs {
		private static final long serialVersionUID = 59357548575248851L;

		SvgSaveAs(@Nullable final Component parent, final String name,
				final Component drawingPane) {
			super(parent, name, drawingPane, ImageType.svg);
		}

		@Override
		protected void saveToFile(final Component component,
				final File selectedFile) {
			try {
				final Class<?> SVGGraphics2DClass = Class
						.forName("org.apache.batik.svggen.SVGGraphics2D");
				final Class<?> domImplClass = Class
						.forName("org.apache.batik.dom.GenericDOMImplementation");
				final Method getDomImplMethod = domImplClass
						.getMethod("getDOMImplementation");
				final DOMImplementation domImpl = (DOMImplementation) getDomImplMethod
						.invoke(null);
				// Create an instance of org.w3c.dom.Document.
				final String svgNS = "http://www.w3.org/2000/svg";
				final Document document = domImpl.createDocument(svgNS, "svg",
						null);

				// Create an instance of the SVG Generator.
				final Graphics2D svgGenerator = (Graphics2D) SVGGraphics2DClass
						.getConstructor(Document.class).newInstance(document);
				// Ask the test to render into the SVG Graphics2D
				// implementation.
				component.paintAll(svgGenerator);

				// Finally, stream out SVG to the standard output using
				// UTF-8 encoding.
				final boolean useCSS = true; // we want to use CSS style
				// attributes
				final Method stream = SVGGraphics2DClass.getMethod("stream",
						Writer.class, boolean.class);
				final FileOutputStream fos = new FileOutputStream(selectedFile);
				try {
					final Writer out = new OutputStreamWriter(fos, "UTF-8");
					try {
						stream.invoke(svgGenerator, out, useCSS);
						// svgGenerator.stream(out, useCSS);
					} finally {
						out.close();
					}
				} finally {
					fos.close();
				}
			} catch (final IllegalArgumentException e) {
				handleMissingBatik();
			} catch (final IllegalAccessException e) {
				handleMissingBatik();

			} catch (final InvocationTargetException e) {
				handleMissingBatik();
			} catch (final SecurityException e) {
				handleMissingBatik();
			} catch (final NoSuchMethodException e) {
				handleMissingBatik();
			} catch (final InstantiationException e) {
				handleMissingBatik();
			} catch (final ClassNotFoundException e) {
				handleMissingBatik();
			} catch (final IOException e) {
				JOptionPane.showMessageDialog(super.component,
						"Failed to save file: " + e.getMessage(),
						"Failed to save file", JOptionPane.ERROR_MESSAGE);
			}
		}

		private void handleMissingBatik() {
			JOptionPane
					.showMessageDialog(
							super.component,
							"The Apache Batik SVG Generation or the Apache Batik DOM extension is not installed, but these are necessary for this functionality.\n"
									+ "You can install them from the Orbit download page:\n"
									+ "http://download.eclipse.org/tools/orbit/downloads/",
							"Download Apache Batik SVG Generation/Apache Batik DOM",
							JOptionPane.INFORMATION_MESSAGE);
			setEnabled(false);
		}
	}
}
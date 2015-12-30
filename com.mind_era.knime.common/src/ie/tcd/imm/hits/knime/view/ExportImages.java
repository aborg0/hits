/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package ie.tcd.imm.hits.knime.view;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.Callable;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;

import org.knime.core.node.defaultnodesettings.DialogComponentFileChooser;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

import ie.tcd.imm.hits.util.Traversable;
import ie.tcd.imm.hits.util.swing.ImageType;
import ie.tcd.imm.hits.util.swing.SaveAs;

/**
 * An {@link Action} to save images to a folder.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
@Nonnull
@CheckReturnValue
public abstract class ExportImages extends AbstractAction {
	private static final long serialVersionUID = 5761525362417025576L;
	private final ImageType type;
	/** The {@link JScrollPane} where the data image will be painted. */
	protected JScrollPane scrollPane;

	private final JCheckBox askBeforeOverwrite = new JCheckBox(
			"Ask before overwrite", true);

	private final JComboBox<ImageType> fileFormat = new JComboBox<>();

	/**
	 * @param type
	 *            Format of images to save.
	 */
	public ExportImages(final ImageType type) {
		this("", type);
	}

	/**
	 * @param name
	 *            Name of {@link Action}.
	 * @param type
	 *            Format of images to save.
	 */
	public ExportImages(final String name, final ImageType type) {
		this(name, null, type);
	}

	/**
	 * @param name
	 *            Name of {@link Action}.
	 * @param icon
	 *            Icon of {@link Action}.
	 * @param type
	 *            Format of images to save.
	 */
	public ExportImages(final String name, @Nullable final Icon icon,
			final ImageType type) {
		super(name, icon);
		this.type = type;
		for (final ImageType value : ImageType.values()) {
			fileFormat.addItem(value);
		}
		fileFormat.setSelectedItem(type);
	}

	@Override
	public void actionPerformed(final ActionEvent e) {
		final JDialog dialog = new JDialog((Frame) null,
				"Image export parameters", true);
		final Container contentPane = dialog.getContentPane();
		final SettingsModelString fileNameModel = new SettingsModelString(
				"fileName", System.getProperty("user.home"));
		final DialogComponentFileChooser fileChooser = new DialogComponentFileChooser(
				fileNameModel, "", JFileChooser.SAVE_DIALOG, true, type
						.getExtensions());
		final JPanel controls = new JPanel();
		controls.setLayout(new FlowLayout());
		controls.add(fileChooser.getComponentPanel());
		final JLabel x = new JLabel("width: ");
		controls.add(x);
		final JSpinner width = new JSpinner(new SpinnerNumberModel(800, 50,
				20000, 100));
		controls.add(width);
		final JLabel y = new JLabel("height: ");
		controls.add(y);
		final JSpinner height = new JSpinner(new SpinnerNumberModel(600, 50,
				20000, 100));
		controls.add(height);
		// contentPane.add(fileFormat);
		controls.add(askBeforeOverwrite);
		final JComponent component = createAdditionalControls();
		if (component != null) {
			controls.add(component);
		}
		final JButton okButton = new JButton("OK");
		final boolean[] ok = new boolean[1];
		okButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				ok[0] = true;
				dialog.setVisible(false);
				dialog.dispose();
			}
		});
		contentPane.setLayout(new BorderLayout());
		contentPane.add(controls, BorderLayout.CENTER);
		contentPane.add(okButton, BorderLayout.SOUTH);
		dialog.setPreferredSize(new Dimension(400, 400));
		dialog.pack();
		while (true) {
			dialog.setVisible(true);
			if (ok[0]) {
				if (fileNameModel.getStringValue().isEmpty()) {
					JOptionPane
							.showMessageDialog(
									dialog,
									"No folder specified for the result images. Please select one.",
									"No folder specified",
									JOptionPane.WARNING_MESSAGE);
					dialog.setVisible(true);
					continue;
				}
				final int w = ((Integer) width.getModel().getValue())
						.intValue(), h = ((Integer) height.getModel()
						.getValue()).intValue();
				final JFrame frame = new JFrame();
				scrollPane = new JScrollPane();
				frame.getContentPane().add(scrollPane);
				scrollPane.getViewport().setPreferredSize(new Dimension(w, h));
				frame.pack();
				final boolean[] stopped = new boolean[1];
				frame.addWindowListener(new WindowAdapter() {
					@Override
					public void windowClosing(final WindowEvent e) {
						stopped[0] = true;
					}
				});
				frame.setVisible(true);
				final boolean madeDirs = new File(fileNameModel
						.getStringValue()).mkdirs();
				assert madeDirs || !madeDirs;
				final Traversable<JComponent, String> traversable = createTraversable(
						fileNameModel.getStringValue(), w, h);
				final Runnable frameDispose = new Runnable() {
					@Override
					public void run() {
						frame.dispose();
					}
				};
				try {
					traversable.traverse(new Callable<Boolean>() {
						@Override
						public Boolean call() throws Exception {
							if (stopped[0]) {
								return Boolean.FALSE;
							}
							final StringBuilder fileName = new StringBuilder(
									fileNameModel.getStringValue());
							fileName.append(File.separatorChar);
							fileName.append(traversable.getState().trim());
							final Runnable doRun = new Runnable() {
								@Override
								public void run() {
									setupComponent(traversable);
									final JComponent component = traversable
											.getElement();
									component.setDoubleBuffered(false);
									final SaveAs action = SaveAs.createAction(
											null, fileName.toString(),
											component, (ImageType) fileFormat
													.getSelectedItem());
									final File file = new File(fileName
											.toString());
									action.getFileChooser().setSelectedFile(
											file);
									stopped[0] = !action.saveToFile(false,
											askBeforeOverwrite.getModel()
													.isSelected());
									if (stopped[0]) {
										JOptionPane
												.showMessageDialog(
														null,
														"Save cancelled.",
														"Save cancelled",
														JOptionPane.INFORMATION_MESSAGE);
									}
								}
							};
							if (SwingUtilities.isEventDispatchThread()) {
								doRun.run();
							} else {
								try {
									SwingUtilities.invokeAndWait(doRun);
								} catch (final InterruptedException e1) {
									JOptionPane.showMessageDialog(null,
											"Interrupted", "Interrupted",
											JOptionPane.ERROR_MESSAGE);
								} catch (final InvocationTargetException e1) {
									JOptionPane.showMessageDialog(null,
											"Interrupted", "Interrupted",
											JOptionPane.ERROR_MESSAGE);
								}
							}
							return Boolean.TRUE;
						}
					});
				} finally {
					if (SwingUtilities.isEventDispatchThread()) {
						frameDispose.run();
					} else {
						try {
							SwingUtilities.invokeAndWait(frameDispose);
						} catch (final InterruptedException e1) {
							throw new RuntimeException(e1);
						} catch (final InvocationTargetException e1) {
							throw new RuntimeException(e1);
						}
					}
				}
				if (!stopped[0]) {
					JOptionPane.showMessageDialog(null,
							"Images successfully exported to "
									+ fileNameModel.getStringValue(),
							"Images successfully exported",
							JOptionPane.INFORMATION_MESSAGE);
				}
				break;
			} else {
				break;
			}
		}
	}

	/**
	 * Creates a control on the save dialog.
	 * 
	 * @return A new {@link JComponent}, or {@code null} if none is needed.
	 */
	protected abstract @Nullable
	JComponent createAdditionalControls();

	/**
	 * Creates the {@link Traversable} component used to visit the things to
	 * export.
	 * 
	 * @param folderName
	 *            The folder of the result images.
	 * @param width
	 *            Width of images.
	 * @param height
	 *            Height of images.
	 * @return The {@link Traversable} to visit things.
	 */
	protected abstract Traversable<JComponent, String> createTraversable(
			final String folderName, final int width, final int height);

	/**
	 * Sets the parameters of the {@code traversable}'s current
	 * {@link Traversable#getElement() component}. (Called before saving to
	 * file, should set the {@link #scrollPane}'s
	 * {@link JScrollPane#setViewportView(java.awt.Component) viewport}.)
	 * 
	 * @param traversable
	 *            A {@link Traversable}.
	 */
	protected abstract void setupComponent(
			Traversable<JComponent, String> traversable);
}

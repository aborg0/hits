/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package ie.tcd.imm.hits.util.swing.colour;

import ie.tcd.imm.hits.util.Traversable;
import ie.tcd.imm.hits.util.swing.ImageType;
import ie.tcd.imm.hits.util.swing.SaveAs;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.lang.reflect.InvocationTargetException;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;

import org.knime.core.node.defaultnodesettings.DialogComponentFileChooser;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

import edu.umd.cs.findbugs.annotations.DefaultAnnotation;

/**
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
@DefaultAnnotation( { Nonnull.class, CheckReturnValue.class })
abstract class ExportImages extends AbstractAction {
	private static final long serialVersionUID = 5761525362417025576L;
	private static final ExecutorService executor = new ThreadPoolExecutor(1,
			1, 10, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(10));
	private final Component parent;
	private ImageType type;

	/**
	 * 
	 */
	public ExportImages() {
		this("");
	}

	/**
	 * @param name
	 */
	public ExportImages(final String name) {
		this(name, null);
	}

	/**
	 * @param name
	 * @param icon
	 */
	public ExportImages(final String name, final Icon icon) {
		super(name, icon);
		parent = null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	@Override
	public void actionPerformed(final ActionEvent e) {
		final JDialog dialog = new JDialog((Frame) null,
				"Image export parameters", true);
		final Container contentPane = dialog.getContentPane();
		final SettingsModelString fileNameModel = new SettingsModelString("",
				"");
		final DialogComponentFileChooser fileChooser = new DialogComponentFileChooser(
				fileNameModel, "", JFileChooser.SAVE_DIALOG, true, type
						.getExtensions());
		contentPane.add(fileChooser.getComponentPanel());
		// springLayout.addLayoutComponent(fileChooser, new Constraints());
		final JLabel x = new JLabel("width: ");
		contentPane.add(x);
		final JSpinner width = new JSpinner(new SpinnerNumberModel(800, 200,
				20000, 100));
		contentPane.add(width);
		final JLabel y = new JLabel("height: ");
		contentPane.add(y);
		final JSpinner height = new JSpinner(new SpinnerNumberModel(600, 200,
				20000, 100));
		contentPane.add(height);
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
		contentPane.add(okButton);
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
				final JScrollPane scrollPane = new JScrollPane();
				frame.getContentPane().add(scrollPane);
				scrollPane.getViewport().setPreferredSize(
						new Dimension(((Number) width.getModel().getValue())
								.intValue(), ((Number) height.getModel()
								.getValue()).intValue()));
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
				final Callable<Boolean> worker = new Callable<Boolean>() {
					@Override
					public Boolean call() throws Exception {
						traversable.traverse(new Callable<Boolean>() {
							@Override
							public Boolean call() throws Exception {
								if (stopped[0]) {
									return Boolean.FALSE;
								}
								final StringBuilder fileName = new StringBuilder(
										fileNameModel.getStringValue());
								fileName.append(File.separatorChar);
								fileName.append(traversable.getState());
								fileName.append(".png");
								try {
									SwingUtilities
											.invokeAndWait(new Runnable() {
												@Override
												public void run() {
													setupComponent(traversable);
													SaveAs
															.createAction(
																	parent,
																	fileName
																			.toString(),
																	traversable
																			.getType(),
																	type)
															.actionPerformed(
																	new ActionEvent(
																			null,
																			0,
																			""));
												}
											});
								} catch (final InterruptedException e1) {
									JOptionPane.showMessageDialog(null,
											"Interrupted", "Interrupted",
											JOptionPane.ERROR_MESSAGE);
								} catch (final InvocationTargetException e1) {
									JOptionPane.showMessageDialog(null,
											"Interrupted", "Interrupted",
											JOptionPane.ERROR_MESSAGE);
								}
								return Boolean.TRUE;
							}
						});
						SwingUtilities.invokeAndWait(new Runnable() {
							@Override
							public void run() {
								frame.dispose();
							}
						});
						return Boolean.valueOf(!stopped[0]);
					}
				};
				executor.submit(worker);
				break;
			} else {
				break;
			}
		}
	}

	/**
	 * @param folderName
	 * @param width
	 * @param height
	 * @return
	 */
	protected abstract Traversable<JComponent, String> createTraversable(
			final String folderName, final int width, final int height);

	/**
	 * @param traversable
	 */
	protected abstract void setupComponent(
			Traversable<JComponent, String> traversable);
}

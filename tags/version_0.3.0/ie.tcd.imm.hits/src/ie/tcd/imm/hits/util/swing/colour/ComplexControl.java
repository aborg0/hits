/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package ie.tcd.imm.hits.util.swing.colour;

import ie.tcd.imm.hits.knime.view.heatmap.HeatmapNodeModel.StatTypes;
import ie.tcd.imm.hits.util.Pair;
import ie.tcd.imm.hits.util.interval.Interval;
import ie.tcd.imm.hits.util.interval.Interval.DefaultInterval;
import ie.tcd.imm.hits.util.swing.colour.ComplexLegend.ComplexSample;

import java.util.ArrayList;
import java.util.List;
import java.util.NavigableMap;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Map.Entry;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.JSpinner.NumberEditor;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import edu.umd.cs.findbugs.annotations.DefaultAnnotation;

/**
 * A slightly complex control to handle {@link ComplexModel}
 * {@link ColourComputer}s.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
@DefaultAnnotation( { Nonnull.class, CheckReturnValue.class })
public class ComplexControl extends JButton implements
		ColourControl<ComplexModel> {
	private static final long serialVersionUID = 8142060029147780548L;

	/** The no (visible) text label. */
	private static final String NO_TEXT = "\u00a0";

	private ComplexModel model;
	private final ComplexLegend sample = new ComplexLegend();
	private final JCheckBox connectButton;

	private static final class ComplexColourPanel extends JPanel implements
			ActionListener {
		private static final long serialVersionUID = 7049084436247735940L;

		/**
		 * The column number for the spinners.
		 */
		private static final int SPINNER_COLUMNS = 7;

		private ComplexModel mod;

		private final List<Spinners<?>> spinners = new ArrayList<Spinners<?>>();

		private final ComplexControl parent;

		private static interface Spinners<Colours> {
			/** @return The low values {@link JSpinner} */
			public JSpinner getLowSpinner();

			/** @return The high values {@link JSpinner} */
			public JSpinner getHighSpinner();

			/** @return The actual state of the implementation. */
			public Pair<Interval<Double>, Colours> getState();
		}

		private static class ColourButtonAction extends AbstractAction {
			private static final long serialVersionUID = -1459471211823151265L;
			private final SpinnersCommon parent;
			private final JButton button;
			private final Positions pos;

			/**
			 * @param parent
			 *            The parent component.
			 * @param button
			 *            The button to listen. (It's background colour works as
			 *            a model.)
			 * @param pos
			 *            The position of the {@code button}.
			 */
			public ColourButtonAction(final SpinnersCommon parent,
					final JButton button, final Positions pos) {
				super("\u2588\u2588\u2588\u2588");
				this.parent = parent;
				this.button = button;
				this.pos = pos;
			}

			@Override
			public void actionPerformed(final ActionEvent e) {
				final Color newColour = JColorChooser.showDialog(parent,
						"Select a color", button.getBackground());
				if (newColour != null) {
					parent.setColour(pos, newColour);
					parent.fireModelChange();
				}
			}
		}

		private static class DiscreteControl extends SpinnersCommon implements
				Spinners<Color> {
			private static final long serialVersionUID = 4240875805367342933L;
			private final JButton colourButton;

			/**
			 * @param key
			 *            initial values
			 * @param colour
			 *            initial colour
			 */
			public DiscreteControl(final Interval<Double> key,
					final Color colour) {
				super(key);
				final GridBagLayout gbl = new GridBagLayout();
				setLayout(gbl);
				final GridBagConstraints highConstraints = new GridBagConstraints();
				highConstraints.gridx = 0;
				highConstraints.gridy = 0;
				highConstraints.fill = GridBagConstraints.HORIZONTAL;
				gbl.setConstraints(getHighSpinner(), highConstraints);
				add(getHighSpinner(), highConstraints);
				final GridBagConstraints colourConstraint = new GridBagConstraints();
				colourConstraint.gridheight = 2;
				colourConstraint.gridx = 1;
				colourConstraint.gridy = 0;
				colourConstraint.fill = GridBagConstraints.HORIZONTAL;
				colourButton = new JButton();
				colourButton.setBackground(colour);
				colourButton.setForeground(colour);
				gbl.setConstraints(colourButton, colourConstraint);
				colourButton.setAction(new ColourButtonAction(this,
						colourButton, Positions.Middle));
				add(colourButton, colourConstraint);
				final GridBagConstraints lowSpinnerConstraint = new GridBagConstraints();
				lowSpinnerConstraint.gridx = 0;
				lowSpinnerConstraint.gridy = 1;
				lowSpinnerConstraint.fill = GridBagConstraints.HORIZONTAL;
				add(getLowSpinner(), lowSpinnerConstraint);
			}

			@Override
			public Pair<Interval<Double>, Color> getState() {
				return new Pair<Interval<Double>, Color>(
						new DefaultInterval<Double>((Double) getLowSpinner()
								.getValue(), (Double) getHighSpinner()
								.getValue(), true, false), colourButton
								.getBackground());
			}

			@Override
			protected void setColour(final Positions pos, final Color newColour) {
				colourButton.setBackground(newColour);
				colourButton.setForeground(newColour);
			}
		}

		private class ContinuousControl extends SpinnersCommon implements
				Spinners<Pair<Color, Color>> {
			private static final long serialVersionUID = -5070099376158524580L;
			private final JButton highColourButton;
			private final JButton lowColourButton;

			/**
			 * @param key
			 *            initial values
			 * @param lowColour
			 *            initial low colour
			 * @param highColour
			 *            initial high colour
			 */
			public ContinuousControl(final Interval<Double> key,
					final Color lowColour, final Color highColour) {
				super(key);
				setLayout(new GridBagLayout());
				add(getHighSpinner(), new GridBagConstraints());
				highColourButton = new JButton();
				highColourButton.setBackground(highColour);
				highColourButton.setForeground(highColour);
				final GridBagConstraints hbcc = new GridBagConstraints();
				hbcc.gridx = 1;
				hbcc.gridy = 0;
				add(highColourButton, hbcc);
				highColourButton.setAction(new ColourButtonAction(this,
						highColourButton, Positions.Up));
				final GridBagConstraints lsc = new GridBagConstraints();
				lsc.gridx = 0;
				lsc.gridy = 1;
				add(getLowSpinner(), lsc);
				lowColourButton = new JButton();
				lowColourButton.setBackground(lowColour);
				lowColourButton.setForeground(lowColour);
				lowColourButton.setAction(new ColourButtonAction(this,
						lowColourButton, Positions.Down));
				final GridBagConstraints lbcc = new GridBagConstraints();
				lbcc.gridx = 1;
				lbcc.gridy = 1;
				add(lowColourButton, lbcc);
			}

			@Override
			public Pair<Interval<Double>, Pair<Color, Color>> getState() {
				return new Pair<Interval<Double>, Pair<Color, Color>>(
						new DefaultInterval<Double>((Double) getLowSpinner()
								.getValue(), (Double) getHighSpinner()
								.getValue(), true, false),
						new Pair<Color, Color>(lowColourButton.getBackground(),
								highColourButton.getBackground()));
			}

			@Override
			protected void setColour(final Positions pos, final Color newColour) {
				switch (pos) {
				case Down:
					lowColourButton.setBackground(newColour);
					lowColourButton.setForeground(newColour);
					break;
				case Up:
					highColourButton.setBackground(newColour);
					highColourButton.setForeground(newColour);
					break;
				default:
					break;
				}
			}

		}

		private static abstract class SpinnersCommon extends ListenablePanel
				implements ActionListener {
			private static final long serialVersionUID = 6896425032410426881L;
			private final JSpinner lowSpinner;
			private final JSpinner highSpinner;

			/**
			 * Commons constructor to the {@link Spinners} implementations.
			 * 
			 * @param key
			 *            The initial {@link Interval}.
			 */
			protected SpinnersCommon(final Interval<Double> key) {
				super();
				lowSpinner = new JSpinner(new SpinnerNumberModel(key.getLow()
						.doubleValue(), Double.NEGATIVE_INFINITY,
						Double.POSITIVE_INFINITY, .1));
				highSpinner = new JSpinner(new SpinnerNumberModel(key.getHigh()
						.doubleValue(), Double.NEGATIVE_INFINITY,
						Double.POSITIVE_INFINITY, .1));
				((NumberEditor) lowSpinner.getEditor()).getTextField()
						.setColumns(ComplexColourPanel.SPINNER_COLUMNS);
				((NumberEditor) highSpinner.getEditor()).getTextField()
						.setColumns(ComplexColourPanel.SPINNER_COLUMNS);
				lowSpinner.addChangeListener(new ChangeListener() {
					@Override
					public void stateChanged(final ChangeEvent e) {
						if (((Double) lowSpinner.getValue())
								.compareTo((Double) highSpinner.getValue()) > 0) {
							lowSpinner.setValue(highSpinner.getValue());
						}
						fireModelChange();
					}
				});
				highSpinner.addChangeListener(new ChangeListener() {
					@Override
					public void stateChanged(final ChangeEvent e) {
						if (((Double) lowSpinner.getValue())
								.compareTo((Double) highSpinner.getValue()) > 0) {
							highSpinner.setValue(lowSpinner.getValue());
						}
						fireModelChange();
					}
				});
			}

			/**
			 * Changes the colour value at {@code pos}.
			 * 
			 * @param pos
			 *            The position where the {@code newColour} belongs to.
			 * @param newColour
			 *            The new {@link Color} at that positon.
			 */
			protected abstract void setColour(Positions pos, Color newColour);

			/**
			 * @return the low spinner
			 * @see Spinners#getLowSpinner()
			 */
			public final JSpinner getLowSpinner() {
				return lowSpinner;
			}

			/**
			 * @return the high spinner
			 * @see Spinners#getHighSpinner()
			 */
			public final JSpinner getHighSpinner() {
				return highSpinner;
			}

			@Override
			public void actionPerformed(final ActionEvent e) {
				fireModelChange();
			}
		}

		/**
		 * @param parent
		 *            The parent component.
		 * @param model
		 *            initial model
		 */
		public ComplexColourPanel(final ComplexControl parent,
				final ComplexModel model) {
			super();
			this.parent = parent;
			setModel(model);
		}

		/**
		 * @param spinner
		 *            The actual {@link Spinners}.
		 * @param pos
		 *            The position to add new control.
		 * @param isDiscrete
		 *            The type of control should be {@link DiscreteControl} (
		 *            {@code true}), or {@link ContinuousControl} ({@code false}
		 *            ).
		 * @return The add action for that position and type.
		 */
		public Action createAddAction(final Spinners<?> spinner,
				final Positions pos, final boolean isDiscrete) {
			return new AbstractAction(isDiscrete ? "single colour"
					: "linear gradient") {
				private static final long serialVersionUID = -5090340712099813802L;

				@Override
				public void actionPerformed(final ActionEvent e) {
					final SortedMap<Interval<Double>, Color> discretes = new TreeMap<Interval<Double>, Color>(
							mod.getDiscretes());
					final SortedMap<Interval<Double>, Pair<Color, Color>> continuouses = new TreeMap<Interval<Double>, Pair<Color, Color>>(
							mod.getContinuouses());
					final Pair<Interval<Double>, ?> state = spinner.getState();
					if (isDiscrete) {
						switch (pos) {
						case Up:
							discretes.put(new DefaultInterval<Double>(state
									.getLeft().getHigh(), state.getLeft()
									.getHigh(), true, false), Color.BLACK);
							break;
						case Down:
							discretes.put(new DefaultInterval<Double>(state
									.getLeft().getLow(), state.getLeft()
									.getLow(), true, false), Color.BLACK);
							break;
						default:
							break;
						}
					} else {
						switch (pos) {
						case Up:
							continuouses.put(new DefaultInterval<Double>(state
									.getLeft().getHigh(), state.getLeft()
									.getHigh(), true, false),
									new Pair<Color, Color>(Color.WHITE,
											Color.BLACK));
							break;
						case Down:
							continuouses.put(new DefaultInterval<Double>(state
									.getLeft().getLow(), state.getLeft()
									.getLow(), true, false),
									new Pair<Color, Color>(Color.WHITE,
											Color.BLACK));
							break;
						default:
							break;
						}
					}
					setModel(new ComplexModel(continuouses, discretes));
				}
			};
		}

		/**
		 * @param model
		 */
		private void setModel(final ComplexModel model) {
			mod = model;
			removeAll();
			spinners.clear();
			setLayout(new GridLayout(1, 2));
			final ComplexSample samp = ComplexSample.create(true);
			samp.setModel(mod);
			add(samp);
			final JPanel panel = new JPanel(new GridLayout(0, 1));
			final NavigableMap<Interval<Double>, Object> union = new TreeMap<Interval<Double>, Object>(
					mod.getDiscretes());
			union.putAll(mod.getContinuouses());
			for (final Entry<Interval<Double>, ?> entry : union.descendingMap()
					.entrySet()) {
				final Spinners<?> spinner;
				if (entry.getValue() instanceof Pair) {
					final Pair<?, ?> pair = (Pair<?, ?>) entry.getValue();
					if (pair.getLeft() instanceof Color
							&& pair.getRight() instanceof Color) {
						@SuppressWarnings("unchecked")
						final Pair<Color, Color> colours = (Pair<Color, Color>) pair;
						spinner = new ContinuousControl(entry.getKey(), colours
								.getLeft(), colours.getRight());
					} else {
						throw new IllegalStateException(
								"Only colours are supported: "
										+ pair.getLeft().getClass() + " "
										+ pair.getRight().getClass());
					}
				} else if (entry.getValue() instanceof Color) {
					final Color colour = (Color) entry.getValue();
					spinner = new DiscreteControl(entry.getKey(), colour);
				} else {
					throw new IllegalStateException(
							"Only gradient and single colours are supported."
									+ entry.getValue().getClass());
				}
				spinners.add(spinner);
				final JPopupMenu popup = createPopupMenu(spinner);
				if (spinner instanceof ListenablePanel) {
					final ListenablePanel p = (ListenablePanel) spinner;
					panel.add(p);
					p.addActionListener(this);
					p.setComponentPopupMenu(popup);
				} else {
					assert false;
				}
			}
			add(panel);
			setPreferredSize(new Dimension(300, union.size() * 70));
			connectControls(parent.connectButton.isSelected());
			revalidate();
			repaint();
		}

		/**
		 * @param spinner
		 *            A {@link DiscreteControl} of a {@link ContinuousControl}.
		 * @return The popup menu belonging to the {@code spinner}.
		 */
		private JPopupMenu createPopupMenu(final Spinners<?> spinner) {
			final JPopupMenu popup = new JPopupMenu("Add/Remove");
			popup.add(new JMenuItem(new AbstractAction("Remove") {
				private static final long serialVersionUID = -6291744466377161856L;

				@Override
				public void actionPerformed(final ActionEvent e) {
					final SortedMap<Interval<Double>, Pair<Color, Color>> continuouses = new TreeMap<Interval<Double>, Pair<Color, Color>>(
							mod.getContinuouses());
					final SortedMap<Interval<Double>, Color> discretes = new TreeMap<Interval<Double>, Color>(
							mod.getDiscretes());
					final Pair<Interval<Double>, ?> state = spinner.getState();
					final Color discreteColour = discretes.get(state.getLeft());
					if (discreteColour != null
							&& discreteColour.equals(state.getRight())) {
						discretes.remove(state.getLeft());
					} else {
						final Pair<Color, Color> continuousColours = continuouses
								.get(state.getLeft());
						if (continuousColours != null
								&& continuousColours.equals(state.getRight())) {
							continuouses.remove(state.getLeft());
						} else {
							assert false;
						}
					}
					setModel(new ComplexModel(continuouses, discretes));
				}
			}));
			final JMenu above = new JMenu("Add above");
			popup.add(above);
			above.add(new JMenuItem(
					createAddAction(spinner, Positions.Up, true)));
			above.add(new JMenuItem(createAddAction(spinner, Positions.Up,
					false)));
			final JMenu below = new JMenu("Add below");
			popup.add(below);
			below.add(new JMenuItem(createAddAction(spinner, Positions.Down,
					true)));
			below.add(new JMenuItem(createAddAction(spinner, Positions.Down,
					false)));
			return popup;
		}

		private static interface NeighbourChangeListener extends ChangeListener {
		}

		/**
		 * @param connect
		 *            Connects the neighbour controls if possible.
		 */
		public void connectControls(final boolean connect) {
			if (connect) {
				Spinners<?> last = null;
				for (final Spinners<?> spinner : spinners) {
					if (last != null) {
						final Spinners<?> prev = last;
						prev.getLowSpinner().addChangeListener(
								new NeighbourChangeListener() {
									public void stateChanged(final ChangeEvent e) {
										if (!prev
												.getLowSpinner()
												.getValue()
												.equals(
														spinner
																.getHighSpinner()
																.getValue())) {
											spinner.getHighSpinner().setValue(
													prev.getLowSpinner()
															.getValue());
										}
									}
								});
						spinner.getHighSpinner().addChangeListener(
								new NeighbourChangeListener() {
									public void stateChanged(final ChangeEvent e) {
										if (!prev
												.getLowSpinner()
												.getValue()
												.equals(
														spinner
																.getHighSpinner()
																.getValue())) {
											prev.getLowSpinner().setValue(
													spinner.getHighSpinner()
															.getValue());
										}
									}
								});
					}
					last = spinner;
				}
			} else {
				for (final Spinners<?> spinner : spinners) {
					for (final ChangeListener listener : spinner
							.getLowSpinner().getChangeListeners().clone()) {
						if (listener instanceof NeighbourChangeListener) {
							spinner.getLowSpinner().removeChangeListener(
									listener);
						}
					}
					for (final ChangeListener listener : spinner
							.getHighSpinner().getChangeListeners().clone()) {
						if (listener instanceof NeighbourChangeListener) {
							spinner.getHighSpinner().removeChangeListener(
									listener);
						}
					}
				}
			}
		}

		@Override
		public void actionPerformed(final ActionEvent e) {
			final NavigableMap<Interval<Double>, Color> discretes = new TreeMap<Interval<Double>, Color>();
			final NavigableMap<Interval<Double>, Pair<Color, Color>> continuouses = new TreeMap<Interval<Double>, Pair<Color, Color>>();
			for (final Spinners<?> spinner : spinners) {
				final Pair<Interval<Double>, ?> state = spinner.getState();
				if (state.getRight() instanceof Color) {
					final Color col = (Color) state.getRight();
					discretes.put(state.getLeft(), col);
				}
				if (state.getRight() instanceof Pair) {
					@SuppressWarnings("unchecked")
					final Pair<Color, Color> pair = (Pair<Color, Color>) state
							.getRight();
					continuouses.put(state.getLeft(), pair);
				}
			}
			setModel(new ComplexModel(continuouses, discretes));
		}
	}

	/**
	 * Constructs a control with the default settings.
	 * 
	 * @param parameter
	 *            The associated parameter.
	 * @param stat
	 *            The associated statistics type.
	 */
	public ComplexControl(final String parameter, @Nullable final StatTypes stat) {
		this(new ComplexModelFactory().getDefaultModel(), parameter, stat);
	}

	/**
	 * Constructs a control with {@code model} initial positions.
	 * 
	 * @param model
	 *            The initial model.
	 * @param parameter
	 *            The associated parameter.
	 * @param stat
	 *            The associated statistics type.
	 */
	public ComplexControl(final ComplexModel model, final String parameter,
			@Nullable final StatTypes stat) {
		super();
		connectButton = new JCheckBox();

		final Action complexAction = new AbstractAction(ComplexControl.NO_TEXT) {
			private static final long serialVersionUID = -5519318869307880427L;

			@Override
			public void actionPerformed(final ActionEvent e) {
				if (ComplexControl.NO_TEXT.equals(e.getActionCommand())) {
					final ComplexColourPanel ccpanel = new ComplexColourPanel(
							ComplexControl.this, getColourModel());
					final JDialog dialog = new JDialog(JOptionPane
							.getRootFrame(), "Adjust colour ranges ("
							+ parameter + (stat == null ? "" : ", " + stat)
							+ ")", true);
					final JComponent optionPane = new JPanel(new BorderLayout());
					optionPane.add(ccpanel, BorderLayout.CENTER);
					final JPanel buttonPanel = new JPanel();
					buttonPanel.add(new JButton(new AbstractAction("OK") {
						private static final long serialVersionUID = 4629170869596602196L;

						@Override
						public void actionPerformed(final ActionEvent e) {
							ComplexControl.this.setModel(ccpanel.mod);
							ComplexControl.this.fireActionPerformed(e);
							dialog.dispose();
						}
					}));
					buttonPanel.add(new JButton(new AbstractAction("Cancel") {
						private static final long serialVersionUID = -3808194145044700722L;

						@Override
						public void actionPerformed(final ActionEvent e) {
							dialog.dispose();
						}
					}));
					final AbstractAction connectAction = new AbstractAction(
							"Connect neighbours") {
						private static final long serialVersionUID = 361372379394476430L;

						@Override
						public void actionPerformed(final ActionEvent e) {
							final boolean connect = connectButton.isSelected();
							ccpanel.connectControls(connect);
						}
					};
					connectButton.setAction(connectAction);
					if (!connectButton.isSelected()) {
						connectButton.doClick();
					}
					buttonPanel.add(connectButton);
					optionPane.add(buttonPanel, BorderLayout.SOUTH);
					dialog.setContentPane(optionPane);
					dialog.setPreferredSize(new Dimension(ccpanel
							.getPreferredSize().width + 10, ccpanel
							.getPreferredSize().height + 30));
					dialog.setSize(new Dimension(
							ccpanel.getPreferredSize().width + 10, ccpanel
									.getPreferredSize().height + 70));
					dialog.setVisible(true);
				}
			}
		};
		setAction(complexAction);
		setText(ComplexControl.NO_TEXT);
		setPreferredSize(new Dimension(30, 70));
		final GridBagLayout gbl = new GridBagLayout();
		setLayout(gbl);

		setModel(model);
		final GridBagConstraints sampleConstraint = new GridBagConstraints();
		sampleConstraint.fill = GridBagConstraints.VERTICAL;
		sample.setPreferredSize(new Dimension(70, 70));
		sample.setMinimumSize(new Dimension(60, 50));
		add(sample, sampleConstraint);
	}

	@Override
	public void setModel(final ComplexModel model) {
		this.model = model;
		update();
		fireActionPerformed(new ActionEvent(this, (int) (System
				.currentTimeMillis() & 0xffffffff), "modelChanged"));
	}

	private void update() {
		sample.setModel(model, Orientation.East);
		sample.repaint();
	}

	@Override
	public ComplexModel getColourModel() {
		return model;
	}
}

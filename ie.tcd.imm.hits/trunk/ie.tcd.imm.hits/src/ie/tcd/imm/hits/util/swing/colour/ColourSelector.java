package ie.tcd.imm.hits.util.swing.colour;

import ie.tcd.imm.hits.knime.view.heatmap.HeatmapNodeModel.StatTypes;
import ie.tcd.imm.hits.knime.view.prefs.ColourPreferenceConstants;
import ie.tcd.imm.hits.knime.xls.ImporterNodePlugin;
import ie.tcd.imm.hits.util.Displayable;
import ie.tcd.imm.hits.util.JEPHelper;
import ie.tcd.imm.hits.util.Pair;
import ie.tcd.imm.hits.util.interval.Interval;
import ie.tcd.imm.hits.util.interval.Interval.DefaultInterval;
import ie.tcd.imm.hits.util.swing.colour.ColourSelector.Line.ComplexMetaModel;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

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
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AbstractDocument;
import javax.swing.text.BadLocationException;

import org.nfunk.jep.JEP;
import org.nfunk.jep.Node;

import edu.umd.cs.findbugs.annotations.DefaultAnnotation;

/**
 * This class is for selecting the colours for different parameters and its
 * statistical, or normal values.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
@DefaultAnnotation( { Nonnull.class, CheckReturnValue.class })
public class ColourSelector extends JPanel implements HasMetaModel {
	private static final long serialVersionUID = 1927466055122883656L;

	/** The default value for low values */
	public static Double DEFAULT_LOW = Double.valueOf(-2.0);
	/** The default value for middle values */
	public static Double DEFAULT_MID = Double.valueOf(0.0);
	/** The default value for high values */
	public static Double DEFAULT_HIGH = Double.valueOf(2.0);

	/**
	 * The possible ranges of an interval.
	 */
	public static enum RangeType implements Displayable {
		/** The minimum value. */
		min("Minimum"),
		/** The maximum value */
		max("Maximum"),
		/** The median value */
		median("Median"),
		/** The mean/average value */
		average("Average or Mean"),
		/** The standard deviation value */
		stdev("Standard Deviation"),
		/** The median absolute deviation value */
		mad("Median Absolute Deviation"),
		/** Q3 - Q1 */
		iqr("Interquartile Range"),
		/** The lower, first quartile */
		q1("First Quartile"),
		/** The upper, third quartile */
		q3("Third Quartile");

		private final String displayText;

		private RangeType(final String displayText) {
			this.displayText = displayText;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see ie.tcd.imm.hits.util.Displayable#getDisplayText()
		 */
		@Override
		public String getDisplayText() {
			return displayText;
		}
	}

	/**
	 * This is a {@link ColourModel} for each parameter and {@link StatTypes}
	 * with real valued parameters.
	 */
	public static class ColourModel implements Serializable {
		private static final long serialVersionUID = -6758031469463423849L;
		private final Map<String, Map<StatTypes, ColourComputer>> models = new TreeMap<String, Map<StatTypes, ColourComputer>>();
		private final List<ActionListener> listeners = new ArrayList<ActionListener>();

		/**
		 * Sets the {@link ColourComputer} for the selected {@code parameter}
		 * and {@code stat}.
		 * 
		 * @param <ComputerType>
		 *            The type of the {@code model}.
		 * 
		 * @param parameter
		 *            A parameter.
		 * @param stat
		 *            A {@link StatTypes}.
		 * @param model
		 *            The new {@link ColourComputer}.
		 */
		protected <ComputerType extends ColourComputer> void setModel(
				final String parameter, final StatTypes stat,
				final ComputerType model) {
			if (!models.containsKey(parameter)) {
				models.put(parameter, new EnumMap<StatTypes, ColourComputer>(
						StatTypes.class));
			}
			final Map<StatTypes, ColourComputer> map = models.get(parameter);
			map.put(stat, model);
			fireModelChanged();
		}

		/**
		 * Removes {@code listener}.
		 * 
		 * @param listener
		 *            An {@link ActionListener}.
		 */
		public void removeActionListener(final ActionListener listener) {
			listeners.remove(listener);
		}

		/**
		 * Adds the {@link ActionListener} to the listeners if not previously
		 * contained.
		 * 
		 * @param listener
		 *            An {@link ActionListener}.
		 */
		public void addActionListener(final ActionListener listener) {
			if (!listeners.contains(listener)) {
				listeners.add(listener);
			}
		}

		private void fireModelChanged(final ActionEvent e) {
			for (final ActionListener listener : listeners) {
				listener.actionPerformed(e);
			}
		}

		private void fireModelChanged() {
			fireModelChanged(new ActionEvent(this, (int) (System
					.currentTimeMillis() & 0xffffffff), "modelChanged"));
		}

		/**
		 * Gets the associated {@link ColourComputer} for {@code parameter} and
		 * {@code stat}. It may return {@code null}.
		 * 
		 * @param parameter
		 *            A parameter.
		 * @param stat
		 *            A {@link StatTypes} (with non-discrete values).
		 * @return The associated {@link ColourComputer} or {@code null}.
		 */
		public @Nullable
		ColourComputer getModel(final String parameter, final StatTypes stat) {
			final Map<StatTypes, ColourComputer> map = models.get(parameter);
			if (map != null) {
				return map.get(stat);
			}
			return null;
		}

		/**
		 * Sends an {@link ActionEvent} to the listeners of this model.
		 */
		public void notifyListeners() {
			fireModelChanged();
		}
	}

	/**
	 * A line of parameters for a statistics.
	 */
	static final class Line extends JPanel implements HasMetaModel {
		private static final long serialVersionUID = -50401089349519532L;

		/**
		 * Opens a control to set the colours for more than one type.
		 */
		private static final class GeneralSetter extends AbstractAction {
			private static final long serialVersionUID = 9173042440603816992L;
			private final Action action;
			private ComplexColourPanel ccpanel;
			private final JCheckBox connectButton;
			private final HasMetaModel modellable;

			/**
			 * @param connectButton
			 *            The {@link JCheckBox} for connecting the neighbours.
			 * @param modellable
			 *            The component having {@link ComplexMetaModel}.
			 * @param action
			 *            The action to do on OK.
			 */
			public GeneralSetter(final JCheckBox connectButton,
					final HasMetaModel modellable, final Action action) {
				super();
				this.connectButton = connectButton;
				this.modellable = modellable;
				this.action = action;
				setEnabled(action.isEnabled());
				if (action instanceof AbstractAction) {
					final AbstractAction a = (AbstractAction) action;
					for (final Object key : a.getKeys()) {
						if (key instanceof String) {
							final String k = (String) key;
							putValue(k, action.getValue(k));
						}
					}
				}
			}

			@Override
			public void actionPerformed(final ActionEvent e) {
				ccpanel = new ComplexColourPanel(connectButton, modellable
						.getMetaModel());
				final JDialog dialog = new JDialog(JOptionPane.getRootFrame(),
						"Adjust colour ranges", true);
				final JComponent optionPane = new JPanel(new BorderLayout());
				optionPane.add(ccpanel, BorderLayout.CENTER);
				final JPanel buttonPanel = new JPanel();
				buttonPanel.add(new JButton(new AbstractAction("OK") {
					private static final long serialVersionUID = 4629170869596602196L;

					@Override
					public void actionPerformed(final ActionEvent e) {
						action.actionPerformed(new ActionEvent(
								GeneralSetter.this, (int) (System
										.currentTimeMillis() & 0xffffffff),
								"update"));
						ccpanel.connectControls(false);
						connectButton.setSelected(false);
						dialog.dispose();
					}
				}));
				buttonPanel.add(new JButton(new AbstractAction("Cancel") {
					private static final long serialVersionUID = -3808194145044700722L;

					@Override
					public void actionPerformed(final ActionEvent e) {
						ccpanel.connectControls(false);
						connectButton.setSelected(false);
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

		/**
		 * The metamodel ({@link String} bounds) for {@link ComplexModel}.
		 */
		static class ComplexMetaModel implements Serializable {
			private static final long serialVersionUID = 8712811371387296304L;
			/** The default {@link ComplexMetaModel} metamodel. */
			public static final ComplexMetaModel DEFAULT_META_MODEL;
			static {
				final List<Pair<Pair<String, String>, Object>> conts = new LinkedList<Pair<Pair<String, String>, Object>>();
				conts
						.add(new Pair<Pair<String, String>, Object>(
								new Pair<String, String>("median+1.5mad",
										"median+2mad"), new Pair<Color, Color>(
										Color.BLACK, Color.RED)));
				conts
						.add(new Pair<Pair<String, String>, Object>(
								new Pair<String, String>("median-2mad",
										"median-1.5mad"),
								new Pair<Color, Color>(Color.GREEN, Color.BLACK)));
				DEFAULT_META_MODEL = new ComplexMetaModel(conts);
			}
			private final List<Pair<Pair<String, String>, Object>> entries = new LinkedList<Pair<Pair<String, String>, Object>>();

			/**
			 * Constructs the {@link ComplexMetaModel}.
			 * 
			 * @param entries
			 *            The {@link #getEntries() entries} the
			 *            {@link ComplexMetaModel}.
			 */
			public ComplexMetaModel(
					final Collection<? extends Pair<Pair<String, String>, Object>> entries) {
				this.entries.addAll(entries);
			}

			/**
			 * @return The entries of the metamodel.
			 */
			protected List<Pair<Pair<String, String>, Object>> getEntries() {
				return entries;
			}
		}

		/**
		 * Control handle both gradient and single colour controls.
		 */
		private static final class ComplexColourPanel extends JPanel implements
				ActionListener {
			private static final long serialVersionUID = 7049084436247735940L;

			/**
			 * The column number for the spinners.
			 */
			private static final int SPINNER_COLUMNS = 13;

			private ComplexMetaModel mod;

			private final List<Spinners<?>> spinners = new ArrayList<Spinners<?>>();

			// private final Line parent;
			private final JCheckBox connectButton;

			private static interface Spinners<Colours> {
				/** @return The low values {@link JTextField} */
				public JTextField getLowField();

				/** @return The high values {@link JTextField} */
				public JTextField getHighField();

				/** @return The actual state of the implementation. */
				public Pair<Pair<String, String>, Colours> getState();
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
				 *            The button to listen. (It's background colour
				 *            works as a model.)
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

			/**
			 * Control to setup single colour interval.
			 */
			private static class DiscreteControl extends SpinnersCommon
					implements Spinners<Color> {
				private static final long serialVersionUID = 4240875805367342933L;
				private final JButton colourButton;

				/**
				 * @param key
				 *            initial values
				 * @param colour
				 *            initial colour
				 */
				public DiscreteControl(final Pair<String, String> key,
						final Color colour) {
					super(key);
					final GridBagLayout gbl = new GridBagLayout();
					setLayout(gbl);
					final GridBagConstraints highConstraints = new GridBagConstraints();
					highConstraints.gridx = 0;
					highConstraints.gridy = 0;
					highConstraints.fill = GridBagConstraints.HORIZONTAL;
					gbl.setConstraints(getHighField(), highConstraints);
					add(getHighField(), highConstraints);
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
					add(getLowField(), lowSpinnerConstraint);
				}

				@Override
				public Pair<Pair<String, String>, Color> getState() {
					return new Pair<Pair<String, String>, Color>(
							new Pair<String, String>(getLowField().getText(),
									getHighField().getText()), colourButton
									.getBackground());
				}

				@Override
				protected void setColour(final Positions pos,
						final Color newColour) {
					colourButton.setBackground(newColour);
					colourButton.setForeground(newColour);
				}
			}

			/**
			 * Control to setup gradient colours interval.
			 */
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
				public ContinuousControl(final Pair<String, String> key,
						final Color lowColour, final Color highColour) {
					super(key);
					setLayout(new GridBagLayout());
					add(getHighField(), new GridBagConstraints());
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
					add(getLowField(), lsc);
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
				public Pair<Pair<String, String>, Pair<Color, Color>> getState() {
					return new Pair<Pair<String, String>, Pair<Color, Color>>(
							new Pair<String, String>(getLowField().getText(),
									getHighField().getText()),
							new Pair<Color, Color>(lowColourButton
									.getBackground(), highColourButton
									.getBackground()));
				}

				@Override
				protected void setColour(final Positions pos,
						final Color newColour) {
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

			/**
			 * Common supertype for the {@link Spinners} implementations
			 * {@link ContinuousControl} and {@link DiscreteControl}.
			 */
			private static abstract class SpinnersCommon extends
					ListenablePanel implements ActionListener {
				private static final long serialVersionUID = -4842029784301291797L;
				private final JTextField lowTextField;
				private final JTextField highTextField;

				/**
				 * Checks a field to have proper content.
				 */
				private static class Checker implements DocumentListener {
					private final JTextField field;
					private final Color orig;
					private final Color error;

					/**
					 * Constructs a {@link Checker}.
					 * 
					 * @param field
					 *            The field to check.
					 */
					Checker(final JTextField field) {
						super();
						this.field = field;
						orig = field.getBackground();
						error = orig.equals(Color.RED) ? Color.YELLOW
								: Color.RED;
					}

					@Override
					public void removeUpdate(final DocumentEvent e) {
						check(e);
					}

					@Override
					public void insertUpdate(final DocumentEvent e) {
						check(e);
					}

					@Override
					public void changedUpdate(final DocumentEvent e) {
						check(e);
					}

					private void check(final DocumentEvent e) {
						final JEP jep = new JEP();
						jep.setImplicitMul(true);
						JEPHelper.addConstant(jep, RangeType.average, 0.0);
						JEPHelper.addConstant(jep, RangeType.median, 0.1);
						JEPHelper.addConstant(jep, RangeType.min, -4.0);
						JEPHelper.addConstant(jep, RangeType.max, 4.5);
						JEPHelper.addConstant(jep, RangeType.stdev, 1.0);
						JEPHelper.addConstant(jep, RangeType.mad, .97);
						try {
							final Node res = jep.parse(e.getDocument().getText(
									0, e.getDocument().getLength()));
							field.setBackground(orig);
							field.setToolTipText(jep.evaluate(res).toString());
							final int position = field.getCaretPosition();
							field.postActionEvent();
							// field.requestFocusInWindow();
							field.setCaretPosition(position < field.getText()
									.length()
									|| position == 0 ? position : field
									.getText().length() - 1);
						} catch (final org.nfunk.jep.ParseException e1) {
							field.setBackground(error);
							field.setToolTipText(e1.getMessage());
						} catch (final BadLocationException e1) {
							assert false;
						}
					}
				}

				/**
				 * Commons constructor to the {@link Spinners} implementations.
				 * 
				 * @param key
				 *            The initial {@link Interval}.
				 */
				protected SpinnersCommon(final Pair<String, String> key) {
					super();
					lowTextField = new JTextField(key.getLeft(),
							ComplexColourPanel.SPINNER_COLUMNS);
					lowTextField.getDocument().addDocumentListener(
							new Checker(lowTextField));
					final ActionListener changeAction = new ActionListener() {
						@Override
						public void actionPerformed(final ActionEvent e) {
							SpinnersCommon.this.fireModelChange();
						}
					};
					lowTextField.addActionListener(changeAction);
					highTextField = new JTextField(key.getRight(),
							ComplexColourPanel.SPINNER_COLUMNS);
					highTextField.getDocument().addDocumentListener(
							new Checker(highTextField));
					highTextField.addActionListener(changeAction);
				}

				/**
				 * Changes the colour value at {@code pos}.
				 * 
				 * @param pos
				 *            The position where the {@code newColour} belongs
				 *            to.
				 * @param newColour
				 *            The new {@link Color} at that positon.
				 */
				protected abstract void setColour(Positions pos, Color newColour);

				/**
				 * @return the low text field
				 * @see Spinners#getLowField()
				 */
				public final JTextField getLowField() {
					return lowTextField;
				}

				/**
				 * @return the high spinner
				 * @see Spinners#getHighField()
				 */
				public final JTextField getHighField() {
					return highTextField;
				}

				@Override
				public void actionPerformed(final ActionEvent e) {
					fireModelChange();
				}
			}

			/**
			 * @param connectButton
			 *            The connect button.
			 * @param model
			 *            initial model
			 */
			public ComplexColourPanel(final JCheckBox connectButton,
					final ComplexMetaModel model) {
				super();
				this.connectButton = connectButton;
				setModel(model);
			}

			/**
			 * @param spinner
			 *            The actual {@link Spinners}.
			 * @param pos
			 *            The position to add new control.
			 * @param isDiscrete
			 *            The type of control should be {@link DiscreteControl}
			 *            ({@code true}), or {@link ContinuousControl} ({@code
			 *            false}).
			 * @return The add action for that position and type.
			 */
			public Action createAddAction(final Spinners<?> spinner,
					final Positions pos, final boolean isDiscrete) {
				return new AbstractAction(isDiscrete ? "single colour"
						: "linear gradient") {
					private static final long serialVersionUID = -5090340712099813802L;

					@Override
					public void actionPerformed(final ActionEvent e) {
						// final NavigableMap<Pair<String, String>, Color>
						// discrete = new TreeMap<Pair<String, String>, Color>(
						// mod.getDiscrete());
						// final NavigableMap<Pair<String, String>, Pair<Color,
						// Color>> continuous = new TreeMap<Pair<String,
						// String>, Pair<Color, Color>>(
						// mod.getContinuous());
						final LinkedList<Pair<Pair<String, String>, Object>> entries = new LinkedList<Pair<Pair<String, String>, Object>>(
								mod.getEntries());
						final Pair<Pair<String, String>, ?> state = spinner
								.getState();
						final ListIterator<Pair<Pair<String, String>, Object>> listIterator = entries
								.listIterator();
						while (listIterator.hasNext()) {
							if (listIterator.next().equals(state)) {
								break;
							}
						}
						@SuppressWarnings("unchecked")
						final Pair<Pair<String, String>, Object> casted = (Pair<Pair<String, String>, Object>) state;
						if (isDiscrete) {
							switch (pos) {
							case Up:
								listIterator
										.set(new Pair<Pair<String, String>, Object>(
												new Pair<String, String>(state
														.getLeft().getRight(),
														state.getLeft()
																.getRight()),
												Color.BLACK));
								listIterator.add(casted);
								break;
							case Down:
								listIterator
										.add(new Pair<Pair<String, String>, Object>(
												new Pair<String, String>(state
														.getLeft().getLeft(),
														state.getLeft()
																.getLeft()),
												Color.BLACK));
								break;
							default:
								break;
							}
						} else {
							switch (pos) {
							case Up:
								listIterator
										.set(new Pair<Pair<String, String>, Object>(
												new Pair<String, String>(state
														.getLeft().getRight(),
														state.getLeft()
																.getRight()),
												new Pair<Color, Color>(
														Color.WHITE,
														Color.BLACK)));
								listIterator.add(casted);
								break;
							case Down:
								listIterator
										.add(new Pair<Pair<String, String>, Object>(
												new Pair<String, String>(state
														.getLeft().getLeft(),
														state.getLeft()
																.getLeft()),
												new Pair<Color, Color>(
														Color.WHITE,
														Color.BLACK)));
								break;
							default:
								break;
							}
						}
						setModel(new ComplexMetaModel(entries));
					}
				};
			}

			/**
			 * @param model
			 *            The new {@link ComplexMetaModel}.
			 */
			private void setModel(final ComplexMetaModel model) {
				final boolean bigChange = mod == null
						|| mod.entries.size() != model.entries.size();
				mod = model;
				if (bigChange) {
					removeAll();
					spinners.clear();
					setLayout(new GridLayout(1, 1));
					final JPanel panel = new JPanel(new GridLayout(0, 1));
					final Collection<Pair<Pair<String, String>, Object>> entries = new LinkedList<Pair<Pair<String, String>, Object>>(
							mod.getEntries());
					for (final Pair<Pair<String, String>, ?> entry : entries) {
						final Spinners<?> spinner;
						if (entry.getRight() instanceof Pair) {
							final Pair<?, ?> pair = (Pair<?, ?>) entry
									.getRight();
							if (pair.getLeft() instanceof Color
									&& pair.getRight() instanceof Color) {
								@SuppressWarnings("unchecked")
								final Pair<Color, Color> colours = (Pair<Color, Color>) pair;
								spinner = new ContinuousControl(
										entry.getLeft(), colours.getLeft(),
										colours.getRight());
							} else {
								throw new IllegalStateException(
										"Only colours are supported: "
												+ pair.getLeft().getClass()
												+ " "
												+ pair.getRight().getClass());
							}
						} else if (entry.getRight() instanceof Color) {
							final Color colour = (Color) entry.getRight();
							spinner = new DiscreteControl(entry.getLeft(),
									colour);
						} else {
							throw new IllegalStateException(
									"Only gradient and single colours are supported."
											+ entry.getRight().getClass());
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
					setPreferredSize(new Dimension(300, entries.size() * 70));
				}
				connectControls(connectButton.isSelected());
				revalidate();
				repaint();
			}

			/**
			 * @param spinner
			 *            A {@link DiscreteControl} of a
			 *            {@link ContinuousControl}.
			 * @return The popup menu belonging to the {@code spinner}.
			 */
			private JPopupMenu createPopupMenu(final Spinners<?> spinner) {
				final JPopupMenu popup = new JPopupMenu("Add/Remove");
				popup.add(new JMenuItem(new AbstractAction("Remove") {
					private static final long serialVersionUID = -6291744466377161856L;

					@Override
					public void actionPerformed(final ActionEvent e) {
						final LinkedList<Pair<Pair<String, String>, Object>> entries = new LinkedList<Pair<Pair<String, String>, Object>>(
								mod.getEntries());
						final ListIterator<Pair<Pair<String, String>, Object>> listIterator = entries
								.listIterator();
						final Pair<Pair<String, String>, ?> state = spinner
								.getState();
						while (listIterator.hasNext()) {
							if (listIterator.next().equals(state)) {
								listIterator.remove();
							}
						}
						setModel(new ComplexMetaModel(entries));
					}
				}));
				final JMenu above = new JMenu("Add above");
				popup.add(above);
				above.add(new JMenuItem(createAddAction(spinner, Positions.Up,
						true)));
				above.add(new JMenuItem(createAddAction(spinner, Positions.Up,
						false)));
				final JMenu below = new JMenu("Add below");
				popup.add(below);
				below.add(new JMenuItem(createAddAction(spinner,
						Positions.Down, true)));
				below.add(new JMenuItem(createAddAction(spinner,
						Positions.Down, false)));
				return popup;
			}

			private static abstract class NeighbourListener implements
					DocumentListener {
				@Override
				public void changedUpdate(final DocumentEvent e) {
					stateChanged();
				}

				@Override
				public void insertUpdate(final DocumentEvent e) {
					stateChanged();
				}

				@Override
				public void removeUpdate(final DocumentEvent e) {
					stateChanged();
				}

				/**
				 * The common action on any change.
				 */
				protected abstract void stateChanged();
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
							prev.getLowField().getDocument()
									.addDocumentListener(
											new NeighbourListener() {

												public void stateChanged() {
													if (!prev
															.getLowField()
															.getText()
															.equals(
																	spinner
																			.getHighField()
																			.getText())) {
														SwingUtilities
																.invokeLater(new Runnable() {
																	@Override
																	public void run() {
																		if (!prev
																				.getLowField()
																				.getText()
																				.equals(
																						spinner
																								.getHighField()
																								.getText())) {
																			spinner
																					.getHighField()
																					.setText(
																							prev
																									.getLowField()
																									.getText());
																		}
																	}
																});
													}
												}
											});
							spinner.getHighField().getDocument()
									.addDocumentListener(
											new NeighbourListener() {
												public void stateChanged() {
													if (!prev
															.getLowField()
															.getText()
															.equals(
																	spinner
																			.getHighField()
																			.getText())) {
														SwingUtilities
																.invokeLater(new Runnable() {
																	@Override
																	public void run() {
																		if (!prev
																				.getLowField()
																				.getText()
																				.equals(
																						spinner
																								.getHighField()
																								.getText())) {
																			prev
																					.getLowField()
																					.setText(
																							spinner
																									.getHighField()
																									.getText());
																		}
																	}
																});
													}
												}
											});
						}
						last = spinner;
					}
				} else {
					for (final Spinners<?> spinner : spinners) {
						removeListener(spinner.getHighField());
						removeListener(spinner.getLowField());
					}
				}
			}

			/**
			 * Removes the {@link NeighbourListener}s from {@code field}.
			 * 
			 * @param field
			 *            A {@link JTextField}.
			 */
			private void removeListener(final JTextField field) {
				for (final DocumentListener listener : ((AbstractDocument) field
						.getDocument()).getDocumentListeners()) {
					if (listener instanceof NeighbourListener) {
						final NeighbourListener l = (NeighbourListener) listener;
						field.getDocument().removeDocumentListener(l);
					}
				}
			}

			@Override
			public void actionPerformed(final ActionEvent e) {
				final LinkedList<Pair<Pair<String, String>, Object>> entries = new LinkedList<Pair<Pair<String, String>, Object>>();
				for (final Spinners<?> spinner : spinners) {
					final Pair<Pair<String, String>, ?> state = spinner
							.getState();
					if (state.getRight() instanceof Color) {
						final Color col = (Color) state.getRight();
						entries.add(new Pair<Pair<String, String>, Object>(
								state.getLeft(), col));
					}
					if (state.getRight() instanceof Pair) {
						@SuppressWarnings("unchecked")
						final Pair<Color, Color> pair = (Pair<Color, Color>) state
								.getRight();
						entries.add(new Pair<Pair<String, String>, Object>(
								state.getLeft(), pair));
					}
				}
				setModel(new ComplexMetaModel(entries));
			}
		}

		/** The button to connect or not the neighbour controls. */
		protected final JCheckBox connectButton;
		private ComplexMetaModel metamodel;
		private final StatTypes stat;
		private final String[] parameters;
		private final Map<String, Map<StatTypes, Map<RangeType, Double>>> ranges;

		private Line(final ColourSelector parent, final StatTypes stat,
				final Iterable<String> parameters,
				final ComplexMetaModel metaModel,
				final Map<String, Map<StatTypes, Map<RangeType, Double>>> ranges) {
			super();
			this.stat = stat;
			final LinkedList<String> linkedList = new LinkedList<String>();
			for (final String param : parameters) {
				linkedList.add(param);
			}
			this.parameters = linkedList.toArray(new String[0]);
			this.ranges = ranges;
			setModel(metaModel);
			connectButton = new JCheckBox();
			setLayout(new GridLayout(1, 0));
			// add(new JLabel(stat.name()));
			final JButton general = new JButton(new GeneralSetter(
					connectButton, Line.this, new AbstractAction(stat.name()) {
						private static final long serialVersionUID = 77194926481962404L;

						@Override
						public void actionPerformed(final ActionEvent e) {
							Line.this
									.setModel(((GeneralSetter) e.getSource()).ccpanel.mod);
						}
					}));
			add(general);
			for (final String parameter : parameters) {
				final ColourComputer computer = parent.model.getModel(
						parameter, stat);
				final ColourFactory<ColourComputer> factory = // parent.model
				FactoryRegistry.getInstance().getFactory(computer);
				final ColourControl<?> control = factory.createControl(
						parent.model, parameter, stat, computer);
				// final DoubleValueSelector doubleValueSelector = new
				// DoubleValueSelector();
				// doubleValueSelector.setModel(parent.model.getModel(parameter,
				// stat));
				// doubleValueSelector.addActionListener(new ActionListener() {
				// @Override
				// public void actionPerformed(final ActionEvent e) {
				// parent.model.setModel(parameter, stat,
				// ((DoubleValueSelector) e.getSource()).model);
				// }
				// });
				// add(doubleValueSelector);
				if (control instanceof JComponent) {
					final JComponent component = (JComponent) control;
					add(component);
				}
			}
		}

		/**
		 * Sets the {@code mod} {@link ComplexMetaModel} as a
		 * {@link ColourModel} in {@link ColourSelector}.
		 * 
		 * @param mod
		 *            The {@link ComplexMetaModel} to transform to
		 *            {@link ColourModel}.
		 */
		protected void setModel(final ComplexMetaModel mod) {
			metamodel = mod;
			int i = 0;
			for (final Component comp : getComponents()) {
				if (comp instanceof ColourControl) {
					@SuppressWarnings("unchecked")
					final ColourControl<ComplexModel> control = (ColourControl<ComplexModel>) comp;
					final Map<Interval<Double>, Pair<Color, Color>> conts = new TreeMap<Interval<Double>, Pair<Color, Color>>();
					final Map<Interval<Double>, Color> discs = new TreeMap<Interval<Double>, Color>();
					for (final Pair<Pair<String, String>, Object> entry : metamodel.entries) {
						final JEP jep = new JEP();
						jep.setImplicitMul(true);
						final String lowStr = entry.getLeft().getLeft();
						final String highStr = entry.getLeft().getRight();
						final Map<RangeType, Double> map = ranges.get(
								parameters[i]).get(stat);
						for (final Entry<RangeType, Double> e : map.entrySet()) {
							JEPHelper
									.addConstant(jep, e.getKey(), e.getValue());
						}
						try {
							final Node lowNode = jep.parse(lowStr);
							final Node highNode = jep.parse(highStr);
							final Double lowVal = (Double) jep
									.evaluate(lowNode);
							final Double highVal = (Double) jep
									.evaluate(highNode);
							if (entry.getRight() instanceof Color) {
								final Color color = (Color) entry.getRight();
								discs.put(new DefaultInterval<Double>(lowVal,
										highVal, true, false), color);
							}
							if (entry.getRight() instanceof Pair) {
								@SuppressWarnings("unchecked")
								final Pair<Color, Color> pair = (Pair<Color, Color>) entry
										.getRight();
								assert pair.getLeft() instanceof Color : pair;
								assert pair.getRight() instanceof Color : pair;
								conts.put(new DefaultInterval<Double>(lowVal,
										highVal, true, false), pair);
							}
						} catch (final org.nfunk.jep.ParseException e) {
							throw new RuntimeException(e);
						}
					}
					control.setModel(new ComplexModel(conts, discs));
					++i;
				}
			}
		}

		@Override
		public ComplexMetaModel getMetaModel() {
			return metamodel;
		}
	}

	private final JPanel doublePanel;

	/**
	 * Creates a {@link ColourSelector} based on {@code parameters} and {@code
	 * stats}.
	 * 
	 * @param parameters
	 *            Some parameter names.
	 * @param stats
	 *            Some {@link StatTypes}.
	 */
	public ColourSelector(final Iterable<String> parameters,
			final Iterable<StatTypes> stats) {
		super();
		doublePanel = new JPanel();
		doublePanel.setLayout(new GridLayout(0, 1));
		doublePanel.setBorder(new TitledBorder("Colours for heatmap values"));
		update(parameters, stats, Collections
				.<String, Map<StatTypes, Map<RangeType, Double>>> emptyMap());
		add(doublePanel);
	}

	private final ColourModel model = new ColourModel();
	private final Map<StatTypes, Line.ComplexMetaModel> metaModels = new EnumMap<StatTypes, Line.ComplexMetaModel>(
			StatTypes.class);
	private ComplexMetaModel metaModel = ComplexMetaModel.DEFAULT_META_MODEL;

	private final JCheckBox connectNeighbours = new JCheckBox();
	{
		connectNeighbours.doClick();
	}

	/**
	 * Updates the panel based on {@code parameters} and {@code stats}.
	 * 
	 * @param parameters
	 *            Some parameter names.
	 * @param stats
	 *            Some {@link StatTypes}.
	 * @param ranges
	 *            The ranges of the parameter/statistics.
	 */
	public void update(final Iterable<String> parameters,
			final Iterable<StatTypes> stats,
			final Map<String, Map<StatTypes, Map<RangeType, Double>>> ranges) {
		doublePanel.removeAll();
		final JPanel titles = new JPanel();
		titles.setLayout(new GridLayout(1, 0));
		final Color defaultLowColor = ComplexModelFactory
				.getColour(ColourPreferenceConstants.DOWN_COLOUR);
		final Color defaultMiddleColor = ComplexModelFactory
				.getColour(ColourPreferenceConstants.MIDDLE_COLOUR);
		final Color defaultHighColor = ComplexModelFactory
				.getColour(ColourPreferenceConstants.UP_COLOUR);
		// titles.add(new JLabel("Statistics"));
		// titles.add(new JLabel(""));
		titles.add(new JButton(new Line.GeneralSetter(connectNeighbours, this,
				new AbstractAction("Statistics") {
					private static final long serialVersionUID = 3829531426621904766L;

					@Override
					public void actionPerformed(final ActionEvent e) {
						metaModel = ColourSelector.this.metaModel = ((Line.GeneralSetter) e
								.getSource()).ccpanel.mod;
						for (final Entry<StatTypes, ComplexMetaModel> entr : metaModels
								.entrySet()) {
							entr.setValue(metaModel);
						}
						for (final Component component : doublePanel
								.getComponents()) {
							if (component instanceof Line) {
								final Line line = (Line) component;
								line.setModel(metaModel);
							}
						}
					}
				})));
		for (final String parameter : parameters) {
			titles.add(new JLabel(parameter));
		}
		doublePanel.add(titles);
		for (final String parameter : parameters) {
			if (!model.models.containsKey(parameter)) {
				model.models
						.put(parameter, new EnumMap<StatTypes, ColourComputer>(
								StatTypes.class));
			}
			final Map<StatTypes, ColourComputer> map = model.models
					.get(parameter);
			for (final StatTypes stat : stats) {
				if (!map.containsKey(stat)) {
					final Map<StatTypes, Map<RangeType, Double>> possMap = ranges
							.get(parameter);
					if (possMap == null || !possMap.containsKey(stat)) {
						map.put(stat,
						// DEFAULT_MODEL
								new ComplexModelFactory().getDefaultModel());
					} else {
						final Map<RangeType, Double> rangeMap = possMap
								.get(stat);
						final Double defaultLowValue = rangeMap
								.get(Displayable.Util
										.findByDisplayText(
												ImporterNodePlugin
														.getDefault()
														.getPreferenceStore()
														.getString(
																ColourPreferenceConstants.DOWN_VALUE),
												RangeType.values()));
						final Double defaultMiddleValue = rangeMap
								.get(Displayable.Util
										.findByDisplayText(
												ImporterNodePlugin
														.getDefault()
														.getPreferenceStore()
														.getString(
																ColourPreferenceConstants.MIDDLE_VALUE),
												RangeType.values()));
						final Double defaultHighValue = rangeMap
								.get(Displayable.Util
										.findByDisplayText(
												ImporterNodePlugin
														.getDefault()
														.getPreferenceStore()
														.getString(
																ColourPreferenceConstants.UP_VALUE),
												RangeType.values()));
						final Map<Interval<Double>, Pair<Color, Color>> conts = new TreeMap<Interval<Double>, Pair<Color, Color>>();
						final Map<Interval<Double>, Color> discs = new TreeMap<Interval<Double>, Color>();
						conts
								.put(
										defaultLowValue == null
												|| defaultMiddleValue == null ? new DefaultInterval<Double>(
												ColourSelector.DEFAULT_LOW,
												ColourSelector.DEFAULT_MID,
												true, false)
												: new DefaultInterval<Double>(
														defaultLowValue,
														defaultMiddleValue,
														true, false),
										new Pair<Color, Color>(defaultLowColor,
												defaultMiddleColor));
						conts
								.put(
										defaultHighValue == null
												|| defaultMiddleValue == null ? new DefaultInterval<Double>(
												ColourSelector.DEFAULT_MID,
												ColourSelector.DEFAULT_HIGH,
												true, false)
												: new DefaultInterval<Double>(
														defaultMiddleValue,
														defaultHighValue, true,
														false),
										new Pair<Color, Color>(
												defaultMiddleColor,
												defaultHighColor));
						map.put(stat, new ComplexModel(conts, discs));
					}
				}
			}
		}
		for (final StatTypes stat : stats) {
			if (!stat.isDiscrete()) {
				doublePanel.add(new Line(this, stat, parameters,
						getMetaModel(stat), ranges));
			}
		}
	}

	/**
	 * @param stat
	 *            A {@link StatTypes}.
	 * @return The {@link ComplexMetaModel} belonging to {@code stat}.
	 */
	private Line.ComplexMetaModel getMetaModel(final StatTypes stat) {
		final Line.ComplexMetaModel metaModel = metaModels.get(stat);
		return metaModel == null ? Line.ComplexMetaModel.DEFAULT_META_MODEL
				: metaModel;
	}

	/**
	 * @return The current {@link ColourModel}.
	 */
	public ColourModel getModel() {
		return model;
	}

	@Override
	public ComplexMetaModel getMetaModel() {
		return metaModel;
	}
}

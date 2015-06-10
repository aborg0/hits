/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package ie.tcd.imm.hits.knime.view.heatmap;

import ie.tcd.imm.hits.knime.view.ExportImages;
import ie.tcd.imm.hits.knime.view.heatmap.HeatmapNodeModel.StatTypes;
import ie.tcd.imm.hits.util.Misc;
import ie.tcd.imm.hits.util.Pair;
import ie.tcd.imm.hits.util.Traversable;
import ie.tcd.imm.hits.util.swing.ImageType;
import ie.tcd.imm.hits.util.swing.colour.ColourComputer;
import ie.tcd.imm.hits.util.swing.colour.ColourFactory;
import ie.tcd.imm.hits.util.swing.colour.ColourLegend;
import ie.tcd.imm.hits.util.swing.colour.ColourSelector;
import ie.tcd.imm.hits.util.swing.colour.FactoryRegistry;
import ie.tcd.imm.hits.util.swing.colour.Orientation;
import ie.tcd.imm.hits.util.swing.colour.ColourSelector.ColourModel;
import ie.tcd.imm.hits.util.template.CompoundToken;
import ie.tcd.imm.hits.util.template.SimpleToken;
import ie.tcd.imm.hits.util.template.Token;
import ie.tcd.imm.hits.util.template.TokenizeException;
import ie.tcd.imm.hits.util.template.Tokenizer;
import ie.tcd.imm.hits.util.template.TokenizerFactory;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.regex.Pattern;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.border.TitledBorder;

import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;

import edu.umd.cs.findbugs.annotations.DefaultAnnotation;

/**
 * An {@link Action} to save the {@link ColourModel}s as images.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 * @param <ModelType>
 *            Type of the associated model.
 */
@DefaultAnnotation( { Nonnull.class, CheckReturnValue.class })
public class ExportLegendAction<ModelType extends ColourComputer> extends
		ExportImages {
	private static final long serialVersionUID = -6774710490788015500L;

	/** Default pattern for the generated file names. */
	private static final String DEFAULT_PATTERN = "{s}_{p}";

	private final ColourSelector colourSelector;

	private JComboBox orientationCombobox;

	private JList parameters;

	private JList statTypes;

	private JTextField pattern;

	/**
	 * @param colourSelector
	 *            The {@link ColourSelector} component.
	 * @param type
	 *            The image format to save.
	 * 
	 * @see #ExportLegendAction(String, ColourSelector, ImageType)
	 */
	public ExportLegendAction(final ColourSelector colourSelector,
			final ImageType type) {
		this("Export colour legend", colourSelector, type);
	}

	/**
	 * @param name
	 *            The name of the {@link Action}.
	 * @param colourSelector
	 *            The {@link ColourSelector} instance of colours.
	 * @param type
	 *            The result image format.
	 * 
	 * @see #ExportLegendAction(String, Icon, ColourSelector, ImageType)
	 */
	public ExportLegendAction(final String name,
			final ColourSelector colourSelector, final ImageType type) {
		this(name, null, colourSelector, type);
	}

	/**
	 * @param name
	 *            The name of the action.
	 * @param icon
	 *            The associated {@link Icon}. Might be {@code null}.
	 * @param colourSelector
	 *            The control handling the {@link ColourModel}.
	 * @param type
	 *            The type of the result image.
	 * 
	 * @see AbstractAction#AbstractAction(String, Icon)
	 */
	public ExportLegendAction(final String name, final @Nullable Icon icon,
			final ColourSelector colourSelector, final ImageType type) {
		super(name, icon, type);
		this.colourSelector = colourSelector;
	}

	@Override
	protected Traversable<JComponent, String> createTraversable(
			final String folderName, final int width, final int height) {
		return new Traversable<JComponent, String>() {
			private @Nullable
			JComponent component;
			private @Nullable
			String name;

			@SuppressWarnings("unchecked")
			private final Tokenizer groupingTokenizer = new TokenizerFactory()
					.createGroupingTokenizer(org.knime.core.util.Pair.
							<Token, List<Token>> create(null, Collections
									.<Token> emptyList()), Arrays
							.<Class<? extends Token>> asList(SimpleToken.class,
									CompoundToken.class), false, Pattern
							.compile("\\{"), Pattern.compile("\\}"), 0);

			@Override
			public void traverse(final Callable<?> callable) {
				final Map<String, Collection<StatTypes>> modelKeys = colourSelector
						.getModel().getModelKeys();
				final Set<?> selectedParameters = new HashSet<Object>(Arrays
						.asList(parameters.getSelectedValues()));
				final Set<?> selectedStatTypes = new HashSet<Object>(Arrays
						.asList(statTypes.getSelectedValues()));

				List<Token> tokens;
				try {
					tokens = groupingTokenizer.parse(pattern.getText());
				} catch (final TokenizeException e1) {
					final int confirm = JOptionPane.showConfirmDialog(null,
							"Wrong pattern:\n" + e1.getMessage()
									+ "\nUsing default: ", "Wrong pattern",
							JOptionPane.OK_CANCEL_OPTION);
					switch (confirm) {
					case JOptionPane.CANCEL_OPTION:
						return;
					case JOptionPane.OK_OPTION:
						try {
							tokens = groupingTokenizer.parse(DEFAULT_PATTERN);
						} catch (final TokenizeException e) {
							throw new IllegalStateException(
									"Programming inconsistency: "
											+ DEFAULT_PATTERN, e);
						}
						break;
					default:
						throw new IllegalStateException("Unknown selection: "
								+ confirm);
					}
				}
				assert tokens != null;
				if (!modelKeys.isEmpty()) {
					final ColourFactory<ColourComputer> factory = FactoryRegistry
							.getInstance().getFactory(
									colourSelector.getModel().getModel(
											modelKeys.entrySet().iterator()
													.next().getKey(),
											modelKeys.entrySet().iterator()
													.next().getValue()
													.iterator().next()));
					for (final Entry<String, Collection<StatTypes>> entry : modelKeys
							.entrySet()) {
						if (!selectedParameters.contains(entry.getKey())) {
							continue;
						}
						for (final StatTypes stat : entry.getValue()) {
							if (!selectedStatTypes.contains(stat)) {
								continue;
							}
							final ColourComputer model = colourSelector
									.getModel().getModel(entry.getKey(), stat);
							if (model == null) {
								continue;
							}
							final ColourLegend<ColourComputer> legend = factory
									.createLegend(model);
							legend.setModel(model, Orientation
									.valueOf(orientationCombobox
											.getSelectedItem().toString()));
							if (legend instanceof JComponent) {
								component = (JComponent) legend;
								component.setBounds(0, 0, width, height);
								final StringBuilder sb = new StringBuilder();
								for (final Token token : tokens) {
									if (token instanceof SimpleToken) {
										sb.append(token.getText());
									}
									if (token instanceof CompoundToken<?>) {
										final CompoundToken<?> compound = (CompoundToken<?>) token;
										final String text = compound.getText();
										if ("s".equals(text)) {
											sb.append(Misc
													.convertToFileName(stat
															.toString()));
										} else if ("p".equals(text)) {
											sb.append(Misc
													.convertToFileName(entry
															.getKey()));
										} else {
											throw new IllegalStateException(
													"Wrong type of pattern: "
															+ text);
										}
									}
								}
								name = sb.toString();
								try {
									callable.call();
								} catch (final Exception e) {
									throw new RuntimeException(e);
								}
							}
						}
					}
					component = null;
					name = null;
				}
			}

			@Override
			public String getState() {
				return name;
			}

			@Override
			public JComponent getElement() {
				return component;
			}
		};
	}

	@Override
	protected void setupComponent(
			final Traversable<JComponent, String> traversable) {
		final JComponent component = traversable.getElement();
		scrollPane.setViewportView(component);
	}

	@Override
	protected JComponent createAdditionalControls() {
		final JPanel ret = new JPanel();
		ret.setLayout(new BorderLayout(2, 2));
		ret.add(new JLabel("Position of numbers: "));
		orientationCombobox = new JComboBox();
		orientationCombobox.addItem("East");
		orientationCombobox.addItem("West");
		orientationCombobox.addItem("South");
		orientationCombobox.addItem("North");
		ret.add(orientationCombobox, BorderLayout.NORTH);
		final JPanel filterPanel = new JPanel();
		filterPanel.setBorder(new TitledBorder("Filters"));
		final Map<String, Collection<StatTypes>> modelKeys = colourSelector
				.getModel().getModelKeys();
		parameters = new JList(modelKeys.keySet().toArray());
		parameters
				.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		parameters.setSelectionInterval(0, modelKeys.size() - 1);
		filterPanel.add(parameters);
		final EnumSet<StatTypes> presentStatTypes = EnumSet
				.noneOf(StatTypes.class);
		for (final Collection<StatTypes> coll : modelKeys.values()) {
			for (final StatTypes st : coll) {
				presentStatTypes.add(st);
			}
		}
		statTypes = new JList(presentStatTypes.toArray());
		statTypes
				.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		statTypes.setSelectionInterval(0, presentStatTypes.size() - 1);
		filterPanel.add(statTypes);
		ret.add(filterPanel, BorderLayout.CENTER);
		pattern = new JTextField(DEFAULT_PATTERN, 15);
		pattern.setBorder(new TitledBorder("Pattern"));
		ret.add(new JButton(new AbstractAction("Help") {
			private static final long serialVersionUID = 4773608547092729436L;

			@Override
			public void actionPerformed(final ActionEvent e) {
				final Display display = Display.getDefault();
				display.asyncExec(new Runnable() {
					@Override
					public void run() {
						PlatformUI
								.getWorkbench()
								.getHelpSystem()
								.displayHelpResource(
										"/ie.tcd.imm.hits/help/visualise.xhtml#exportColourLegend");
					}
				});
			}
		}), BorderLayout.EAST);
		ret.add(pattern, BorderLayout.SOUTH);
		return ret;
	}
}

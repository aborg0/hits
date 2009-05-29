/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package ie.tcd.imm.hits.knime.view.heatmap;

import ie.tcd.imm.hits.knime.view.ExportImages;
import ie.tcd.imm.hits.knime.view.heatmap.HeatmapNodeModel.StatTypes;
import ie.tcd.imm.hits.util.Misc;
import ie.tcd.imm.hits.util.Traversable;
import ie.tcd.imm.hits.util.swing.ImageType;
import ie.tcd.imm.hits.util.swing.colour.ColourComputer;
import ie.tcd.imm.hits.util.swing.colour.ColourFactory;
import ie.tcd.imm.hits.util.swing.colour.ColourLegend;
import ie.tcd.imm.hits.util.swing.colour.ColourSelector;
import ie.tcd.imm.hits.util.swing.colour.FactoryRegistry;
import ie.tcd.imm.hits.util.swing.colour.Orientation;
import ie.tcd.imm.hits.util.swing.colour.ColourSelector.ColourModel;

import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

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

	private final ColourSelector colourSelector;

	private JComboBox orientationCombobox;

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
		this("", colourSelector, type);
	}

	/**
	 * @param name
	 * @param colourSelector
	 * @param type
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

			@Override
			public void traverse(final Callable<?> callable) {
				final Map<String, Collection<StatTypes>> modelKeys = colourSelector
						.getModel().getModelKeys();
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
						for (final StatTypes stat : entry.getValue()) {
							final ColourComputer model = colourSelector
									.getModel().getModel(entry.getKey(), stat);
							final ColourLegend<ColourComputer> legend = factory
									.createLegend(model);
							legend.setModel(model, Orientation
									.valueOf(orientationCombobox
											.getSelectedItem().toString()));
							if (legend instanceof JComponent) {
								component = (JComponent) legend;
								component.setBounds(0, 0, width, height);
								name = Misc.convertToFileName(stat + "_"
										+ entry.getKey());
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
		ret.add(new JLabel("Position of numbers: "));
		orientationCombobox = new JComboBox();
		orientationCombobox.addItem("East");
		orientationCombobox.addItem("West");
		orientationCombobox.addItem("South");
		orientationCombobox.addItem("North");
		ret.add(orientationCombobox);
		return ret;
	}
}

/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
 * and KNIME GmbH, Konstanz, Germany
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * -------------------------------------------------------------------
 */
package com.mind_era.knime.util.view.dendrogram.viewonly;

import java.awt.Color;
import java.util.Collections;
import java.util.Map;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.swing.AbstractButton;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JToggleButton;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.LineBorder;

import org.knime.base.node.viz.plotter.dendrogram.DendrogramPlotterProperties;
import org.knime.base.node.viz.plotter.props.DefaultTab;

import com.mind_era.knime.common.util.swing.colour.ColourSelector;
import com.mind_era.knime.common.util.swing.colour.ColourSelector.ColourModel;
import com.mind_era.knime.common.util.swing.colour.ColourSelector.RangeType;
import com.mind_era.knime.common.view.StatTypes;

/**
 * The properties for the dendrogram with heatmap node.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
@Nonnull
@CheckReturnValue
public class HeatmapDendrogramPlotterProperties extends
		DendrogramPlotterProperties {
	private static final long serialVersionUID = -6241965284919230058L;
	private final ColourSelector colourSelector;
	private final JButton zoomOut = new JButton("Zoom out");
	private final JSpinner cellWidth = new JSpinner(new SpinnerNumberModel(20,
			2, 100, 2));
	private final JToggleButton flipHorizontal = new JToggleButton(
			"Flip horizontal", false);
	private final JToggleButton flipVertical = new JToggleButton(
			"Flip vertical");
	private final JCheckBox showValues = new JCheckBox("Show values");
	private final JSpinner clusterCount = new JSpinner(new SpinnerNumberModel(
			1, 1, 1, 1));

	/**
	 * Constructs the properties with a new tab, and default colours.
	 */
	public HeatmapDendrogramPlotterProperties() {
		super();
		// Removing the Appearance tab, because we do not use those settings
		removeTabAt(1);

		colourSelector = new ColourSelector(Collections.<String> emptyList(),
				Collections.singleton(StatTypes.raw));
		addTab("Colours", new JScrollPane(colourSelector));
		final DefaultTab defaultTab = (DefaultTab) getComponentAt(0);
		final Box box = new Box(BoxLayout.X_AXIS);
		box.setBorder(new LineBorder(Color.GRAY));
		defaultTab.add(box);
		box.add(zoomOut);
		box.add(new JLabel("Heatmap width: "));
		box.add(cellWidth);
		box.add(showValues);
		box.add(flipHorizontal);
		box.add(flipVertical);
		flipVertical.setEnabled(false);
		box.add(new JLabel("Clusters: "));
		box.add(clusterCount);
	}

	/**
	 * @return The {@link ColourModel} for the heatmap.
	 */
	public ColourModel getColourModel() {
		return colourSelector.getModel();
	}

	/**
	 * @return The {@link ColourSelector} of the heatmap.
	 */
	public ColourSelector getColourSelector() {
		return colourSelector;
	}

	/**
	 * Updates the ranges for the heatmap.
	 * 
	 * @param parameters
	 *            The new parameters.
	 * @param ranges
	 *            The ranges belonging to the parameters, {@link StatTypes}.
	 */
	public void update(final Iterable<String> parameters,
			final Map<String, Map<StatTypes, Map<RangeType, Double>>> ranges) {
		colourSelector.update(parameters, Collections.singleton(StatTypes.raw),
				ranges);
	}

	/**
	 * @return The zoom out {@link JButton}.
	 */
	public JButton getZoomOut() {
		return zoomOut;
	}

	/**
	 * @return The cell width {@link JSpinner}
	 */
	public JSpinner getCellWidth() {
		return cellWidth;
	}

	/**
	 * @return The {@link AbstractButton} for horizontal direction.
	 */
	public AbstractButton getFlipHorizontal() {
		return flipHorizontal;
	}

	/**
	 * @return The {@link AbstractButton} for vertical order of nodes.
	 */
	public AbstractButton getFlipVertical() {
		return flipVertical;
	}

	/**
	 * @return The {@link AbstractButton} for showing the values.
	 */
	public AbstractButton getShowValues() {
		return showValues;
	}

	/**
	 * @return The {@link JSpinner} for the cluster count.
	 */
	public JSpinner getClusterCount() {
		return clusterCount;
	}
}

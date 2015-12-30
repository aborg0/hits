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

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "Dendrogram" Node. Allows to create
 * dendrogram with a heatmap of parameters.
 * 
 * @author <a href="bakosg@tcd.ie">Gabor Bakos</a>
 */
public class DendrogramNodeFactory extends NodeFactory<DendrogramNodeModel> {

	/**
	 * {@inheritDoc}
	 */
	@Override
	public DendrogramNodeModel createNodeModel() {
		return new DendrogramNodeModel();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getNrNodeViews() {
		return 1;
	}

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	@Override
	public NodeView<DendrogramNodeModel> createNodeView(final int viewIndex,
			final DendrogramNodeModel nodeModel) {
		// return new HierarchicalClusterNodeView(nodeModel,
		// new HeatmapDendrogramPlotter(
		// new HeatmapDendrogramDrawingPane(),
		// new HeatmapDendrogramPlotterProperties()));
		return new DendrogramNodeView(nodeModel, new HeatmapDendrogramPlotter(
				new HeatmapDendrogramDrawingPane(),
				new HeatmapDendrogramPlotterProperties()));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean hasDialog() {
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public NodeDialogPane createNodeDialogPane() {
		return new DendrogramNodeDialog();
	}
}

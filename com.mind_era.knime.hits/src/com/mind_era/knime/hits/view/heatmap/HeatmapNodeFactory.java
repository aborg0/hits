/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package com.mind_era.knime.hits.view.heatmap;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "Heatmap" Node. Shows the heatmap of the
 * plates.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
public class HeatmapNodeFactory extends NodeFactory<HeatmapNodeModel> {

	/**
	 * {@inheritDoc}
	 */
	@Override
	public HeatmapNodeModel createNodeModel() {
		return new HeatmapNodeModel();
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
	@Override
	public NodeView<HeatmapNodeModel> createNodeView(final int viewIndex,
			final HeatmapNodeModel nodeModel) {
		return new HeatmapNodeView(nodeModel);
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
		return new HeatmapNodeDialog();
	}

}

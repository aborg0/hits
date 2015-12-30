/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package ie.tcd.imm.hits.knime.view.heatmap.simple;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "SimpleHeatmap" Node. Shows a simple heatmap
 * of the data.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
@Nonnull
@CheckReturnValue
public class SimpleHeatmapNodeFactory extends
		NodeFactory<SimpleHeatmapNodeModel> {

	/**
	 * {@inheritDoc}
	 */
	@Override
	public SimpleHeatmapNodeModel createNodeModel() {
		return new SimpleHeatmapNodeModel();
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
	public NodeView<SimpleHeatmapNodeModel> createNodeView(final int viewIndex,
			final SimpleHeatmapNodeModel nodeModel) {
		return new SimpleHeatmapNodeView(nodeModel);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean hasDialog() {
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public NodeDialogPane createNodeDialogPane() {
		throw new UnsupportedOperationException("No dialog present.");
	}

}

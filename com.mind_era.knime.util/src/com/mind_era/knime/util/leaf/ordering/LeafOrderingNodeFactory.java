/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package com.mind_era.knime.util.leaf.ordering;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "LeafOrdering" Node. Reorders a tree to an
 * optimal ordering. See <tt>New Hierarchical Clustering</tt> node.
 * 
 * @author <a href="bakosg@tcd.ie">Gabor Bakos</a>
 */
public class LeafOrderingNodeFactory extends NodeFactory<LeafOrderingNodeModel> {

	/**
	 * {@inheritDoc}
	 */
	@Override
	public LeafOrderingNodeModel createNodeModel() {
		return new LeafOrderingNodeModel();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getNrNodeViews() {
		return 0;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public NodeView<LeafOrderingNodeModel> createNodeView(final int viewIndex,
			final LeafOrderingNodeModel nodeModel) {
		throw new ArrayIndexOutOfBoundsException("No views. (" + viewIndex
				+ ")");
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
		return new LeafOrderingNodeDialog();
	}

}

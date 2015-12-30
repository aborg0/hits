/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package com.mind_era.knime.util.merge;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "Merge" Node. Resorts the rows. It is mostly
 * like an "anti-sort".
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
public class MergeNodeFactory extends NodeFactory<MergeNodeModel> {

	/**
	 * {@inheritDoc}
	 */
	@Override
	public MergeNodeModel createNodeModel() {
		return new MergeNodeModel();
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
	public NodeView<MergeNodeModel> createNodeView(final int viewIndex,
			final MergeNodeModel nodeModel) {
		throw new IndexOutOfBoundsException("No views available.");
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
		return new MergeNodeDialog();
	}
}

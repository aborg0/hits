/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package com.mind_era.knime.util.product;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "DirectProduct" Node. This node takes input
 * tables and creates a direct product of the rows.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
public class DirectProductNodeFactory extends
		NodeFactory<DirectProductNodeModel> {

	/**
	 * {@inheritDoc}
	 */
	@Override
	public DirectProductNodeModel createNodeModel() {
		return new DirectProductNodeModel();
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
	public NodeView<DirectProductNodeModel> createNodeView(final int viewIndex,
			final DirectProductNodeModel nodeModel) {
		throw new IndexOutOfBoundsException("No views: " + viewIndex);
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
		return new DirectProductNodeDialog();
	}

}

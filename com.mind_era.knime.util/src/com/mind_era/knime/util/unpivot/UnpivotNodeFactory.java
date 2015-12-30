/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package com.mind_era.knime.util.unpivot;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "Unpivot" Node. Introduces new rows (and
 * column(s)) based on the column name structure.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
public class UnpivotNodeFactory extends NodeFactory<UnpivotNodeModel> {

	/**
	 * {@inheritDoc}
	 */
	@Override
	public UnpivotNodeModel createNodeModel() {
		return new UnpivotNodeModel();
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
	public NodeView<UnpivotNodeModel> createNodeView(final int viewIndex,
			final UnpivotNodeModel nodeModel) {
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
		return new UnpivotNodeDialog();
	}

}

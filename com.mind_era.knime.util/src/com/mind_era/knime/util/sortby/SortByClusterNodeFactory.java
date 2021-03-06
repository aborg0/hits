/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package com.mind_era.knime.util.sortby;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "SortByCluster" Node. Sorts the data by the
 * order defined by the clustering.
 * 
 * @author <a href="bakosg@tcd.ie">Gabor Bakos</a>
 */
@Nonnull
@CheckReturnValue
public class SortByClusterNodeFactory extends
		NodeFactory<SortByClusterNodeModel> {

	/**
	 * {@inheritDoc}
	 */
	@Override
	public SortByClusterNodeModel createNodeModel() {
		return new SortByClusterNodeModel();
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
	public NodeView<SortByClusterNodeModel> createNodeView(final int viewIndex,
			final SortByClusterNodeModel nodeModel) {
		throw new IndexOutOfBoundsException("No views (" + viewIndex + ")");
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
		return new SortByClusterNodeDialog();
	}

}

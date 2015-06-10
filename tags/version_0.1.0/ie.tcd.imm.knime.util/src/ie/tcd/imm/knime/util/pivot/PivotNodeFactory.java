/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package ie.tcd.imm.knime.util.pivot;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "Pivot" Node. Converts some information
 * present in rows to columns.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
public class PivotNodeFactory extends NodeFactory<PivotNodeModel> {

	/**
	 * {@inheritDoc}
	 */
	@Override
	public PivotNodeModel createNodeModel() {
		return new PivotNodeModel();
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
	public NodeView<PivotNodeModel> createNodeView(final int viewIndex,
			final PivotNodeModel nodeModel) {
		throw new IndexOutOfBoundsException("No views. " + viewIndex);
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
		return new PivotNodeDialog();
	}

}

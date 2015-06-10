/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package ie.tcd.imm.hits.knime.biomart;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "BiomartAnnotator" Node. Adds some
 * annotations from the BioMart databases using the biomaRt R package.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
public class BiomartAnnotatorNodeFactory extends
		NodeFactory<BiomartAnnotatorNodeModel> {

	/**
	 * {@inheritDoc}
	 */
	@Override
	public BiomartAnnotatorNodeModel createNodeModel() {
		return new BiomartAnnotatorNodeModel();
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
	public NodeView<BiomartAnnotatorNodeModel> createNodeView(
			final int viewIndex, final BiomartAnnotatorNodeModel nodeModel) {
		throw new IndexOutOfBoundsException("No views defined. (" + viewIndex
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
		return new BiomartAnnotatorNodeDialog();
	}

}

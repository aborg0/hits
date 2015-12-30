package com.mind_era.knime.hits.plate.format;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "PlateFormat" Node. Converts between 96,
 * 384, 1536 format plates. It is also capable of mixing replicates in.
 * 
 * @author <a href="bakosg@tcd.ie">Gabor Bakos</a>
 */
public class PlateFormatNodeFactory extends NodeFactory<PlateFormatNodeModel> {

	/**
	 * {@inheritDoc}
	 */
	@Override
	public PlateFormatNodeModel createNodeModel() {
		return new PlateFormatNodeModel();
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
	public NodeView<PlateFormatNodeModel> createNodeView(final int viewIndex,
			final PlateFormatNodeModel nodeModel) {
		throw new ArrayIndexOutOfBoundsException("No views (" + viewIndex + ")");
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
		return new PlateFormatNodeDialog();
	}

}

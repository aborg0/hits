package com.mind_era.knime.util.stat.p;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "ScoreToPValue" Node. Converts scores to p
 * values (also computes the predicted frequency of that value if a sample size
 * selected).
 * 
 * @author <a href="bakosg@tcd.ie">Gabor Bakos</a>
 */
public class ScoreToPValueNodeFactory extends
		NodeFactory<ScoreToPValueNodeModel> {

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ScoreToPValueNodeModel createNodeModel() {
		return new ScoreToPValueNodeModel();
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
	public NodeView<ScoreToPValueNodeModel> createNodeView(final int viewIndex,
			final ScoreToPValueNodeModel nodeModel) {
		throw new ArrayIndexOutOfBoundsException("No views!");
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
		return new ScoreToPValueNodeDialog();
	}
}

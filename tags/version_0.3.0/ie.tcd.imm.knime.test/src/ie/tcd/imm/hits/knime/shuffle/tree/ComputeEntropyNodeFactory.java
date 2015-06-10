package ie.tcd.imm.hits.knime.shuffle.tree;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "ComputeEntropy" Node. This node computes
 * the sum distance through the leaves.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
public class ComputeEntropyNodeFactory extends
		NodeFactory<ComputeEntropyNodeModel> {

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ComputeEntropyNodeModel createNodeModel() {
		return new ComputeEntropyNodeModel();
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
	public NodeView<ComputeEntropyNodeModel> createNodeView(
			final int viewIndex, final ComputeEntropyNodeModel nodeModel) {
		throw new ArrayIndexOutOfBoundsException();
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
		throw new UnsupportedOperationException();
	}

}

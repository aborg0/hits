package ie.tcd.imm.hits.knime.shuffle.tree;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "ShuffleTree" Node. Randomly change the
 * order of branches in a tree.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
public class ShuffleTreeNodeFactory extends NodeFactory<ShuffleTreeNodeModel> {

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ShuffleTreeNodeModel createNodeModel() {
		return new ShuffleTreeNodeModel();
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
	public NodeView<ShuffleTreeNodeModel> createNodeView(final int viewIndex,
			final ShuffleTreeNodeModel nodeModel) {
		throw new ArrayIndexOutOfBoundsException();
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
		return new ShuffleTreeNodeDialog();
	}

}

package ie.tcd.imm.hits.knime.util.leaf.ordering;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "ReverseOrder" Node. Flips every branch in
 * the tree to the opposite, so it will result a tree with the reverse order of
 * leaves.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
public class ReverseOrderNodeFactory extends NodeFactory<ReverseOrderNodeModel> {

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ReverseOrderNodeModel createNodeModel() {
		return new ReverseOrderNodeModel();
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
	public NodeView<ReverseOrderNodeModel> createNodeView(final int viewIndex,
			final ReverseOrderNodeModel nodeModel) {
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
		throw new IllegalStateException("No dialogs present.");
	}

}

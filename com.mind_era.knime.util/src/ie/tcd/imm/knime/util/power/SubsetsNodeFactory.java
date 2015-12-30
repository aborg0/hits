package ie.tcd.imm.knime.util.power;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "Subsets" Node. Generates all possible
 * subsets of the input.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
public class SubsetsNodeFactory extends NodeFactory<SubsetsNodeModel> {

	/**
	 * {@inheritDoc}
	 */
	@Override
	public SubsetsNodeModel createNodeModel() {
		return new SubsetsNodeModel();
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
	public NodeView<SubsetsNodeModel> createNodeView(final int viewIndex,
			final SubsetsNodeModel nodeModel) {
		throw new ArrayIndexOutOfBoundsException("No views");
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
		return new SubsetsNodeDialog();
	}
}

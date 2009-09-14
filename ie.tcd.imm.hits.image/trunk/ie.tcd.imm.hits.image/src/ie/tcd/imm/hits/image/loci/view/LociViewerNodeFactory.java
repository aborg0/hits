package ie.tcd.imm.hits.image.loci.view;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "OMEViewer" Node. Shows images based on OME.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
public class LociViewerNodeFactory extends NodeFactory<LociViewerNodeModel> {

	/**
	 * {@inheritDoc}
	 */
	@Override
	public LociViewerNodeModel createNodeModel() {
		return new LociViewerNodeModel();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getNrNodeViews() {
		return 2;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public NodeView<LociViewerNodeModel> createNodeView(final int viewIndex,
			final LociViewerNodeModel nodeModel) {
		switch (viewIndex) {
		case 0:
			return new LociViewerNodeView(nodeModel);
		case 1:
			return new LociViewerNodeSimpleView(nodeModel);
		default:
			throw new IndexOutOfBoundsException("Wrong index: " + viewIndex);
		}
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
		return new LociViewerNodeDialog();
	}

}

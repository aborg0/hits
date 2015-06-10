package ie.tcd.imm.hits.image.loci.read;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "OMEReader" Node. This node reads image
 * information in OME format.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
public class LociReaderNodeFactory extends NodeFactory<LociReaderNodeModel> {

	/**
	 * {@inheritDoc}
	 */
	@Override
	public LociReaderNodeModel createNodeModel() {
		return new LociReaderNodeModel();
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
	public NodeView<LociReaderNodeModel> createNodeView(final int viewIndex,
			final LociReaderNodeModel nodeModel) {
		throw new IndexOutOfBoundsException("No views.\nviewIndex: "
				+ viewIndex);
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
		return new LociReaderNodeDialog();
	}

}

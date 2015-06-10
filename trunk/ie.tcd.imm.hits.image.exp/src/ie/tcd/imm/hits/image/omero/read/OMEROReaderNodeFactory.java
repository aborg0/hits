package ie.tcd.imm.hits.image.omero.read;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "OMEROReader" Node. Allows to import data
 * from OMERO servers.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
public class OMEROReaderNodeFactory extends NodeFactory<OMEROReaderNodeModel> {

	/**
	 * {@inheritDoc}
	 */
	@Override
	public OMEROReaderNodeModel createNodeModel() {
		return new OMEROReaderNodeModel();
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
	public NodeView<OMEROReaderNodeModel> createNodeView(final int viewIndex,
			final OMEROReaderNodeModel nodeModel) {
		throw new IndexOutOfBoundsException("Invalid view index: " + viewIndex);
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
		return new OMEROReaderNodeDialog();
	}
}

package ie.tcd.imm.hits.image.loci.convert;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "ConvertToImage" Node. Converts the images
 * from LOCI Reader to KNIME imaging format.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
public class ConvertToImageNodeFactory extends
		NodeFactory<ConvertToImageNodeModel> {

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ConvertToImageNodeModel createNodeModel() {
		return new ConvertToImageNodeModel();
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
	public NodeView<ConvertToImageNodeModel> createNodeView(
			final int viewIndex, final ConvertToImageNodeModel nodeModel) {
		throw new IndexOutOfBoundsException("index: " + viewIndex);
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
		return new ConvertToImageNodeDialog();
	}

}

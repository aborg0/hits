package ie.tcd.imm.hits.knime.interop;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "BioConverter" Node. Converts between
 * different kind of plate formats.
 * 
 * @author <a href="bakosg@tcd.ie">Gabor Bakos</a>
 */
public class BioConverterNodeFactory extends NodeFactory<BioConverterNodeModel> {

	/**
	 * {@inheritDoc}
	 */
	@Override
	public BioConverterNodeModel createNodeModel() {
		return new BioConverterNodeModel();
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
	public NodeView<BioConverterNodeModel> createNodeView(final int viewIndex,
			final BioConverterNodeModel nodeModel) {
		throw new ArrayIndexOutOfBoundsException("No views: " + viewIndex);
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
		return new BioConverterNodeDialog();
	}

}

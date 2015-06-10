package ie.tcd.imm.hits.knime.biomart;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "BiomartAnnotator" Node. Adds some
 * annotations from the BioMart databases using the biomaRt R package.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
public class BiomartAnnotatorNodeFactory extends NodeFactory {

	/**
	 * {@inheritDoc}
	 */
	@Override
	public NodeModel createNodeModel() {
		return new BiomartAnnotatorNodeModel();
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
	public NodeView createNodeView(final int viewIndex,
			final NodeModel nodeModel) {
		throw new IndexOutOfBoundsException("No views defined. (" + viewIndex
				+ ")");
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
		return new BiomartAnnotatorNodeDialog();
	}

}

package ie.tcd.imm.hits.knime.xls;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "Importer" Node. Reads the data from xls
 * files
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
public class ImporterNodeFactory extends NodeFactory {

	/**
	 * {@inheritDoc}
	 */
	@Override
	public NodeModel createNodeModel() {
		return new ImporterNodeModel();
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
		throw new ArrayIndexOutOfBoundsException("No view with index: "
				+ viewIndex);
		// return new ImporterNodeView(nodeModel);
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
		return new ImporterNodeDialog();
	}

}

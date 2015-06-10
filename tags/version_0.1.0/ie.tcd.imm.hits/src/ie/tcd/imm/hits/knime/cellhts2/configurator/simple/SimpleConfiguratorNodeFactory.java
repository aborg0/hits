package ie.tcd.imm.hits.knime.cellhts2.configurator.simple;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "SimpleConfigurator" Node. This node reads
 * the specified CellHTS 2 configuration files for using them as input for
 * CellHTS nodes.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
public class SimpleConfiguratorNodeFactory extends NodeFactory<SimpleConfiguratorNodeModel> {

	/**
	 * {@inheritDoc}
	 */
	@Override
	public SimpleConfiguratorNodeModel createNodeModel() {
		return new SimpleConfiguratorNodeModel();
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
	public NodeView<SimpleConfiguratorNodeModel> createNodeView(final int viewIndex,
			final SimpleConfiguratorNodeModel nodeModel) {
		throw new ArrayIndexOutOfBoundsException("Index: " + viewIndex);
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
		return new SimpleConfiguratorNodeDialog();
	}

}

package ie.tcd.imm.hits.knime.cellhts2.worker;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "CellHTS2" Node.
 * This node performs the calculations using CellHTS2
 *
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
public class CellHTS2NodeFactory extends NodeFactory {

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeModel createNodeModel() {
        return new CellHTS2NodeModel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getNrNodeViews() {
        return 1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeView createNodeView(final int viewIndex,
            final NodeModel nodeModel) {
        return new CellHTS2NodeView(nodeModel);
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
        return new CellHTS2NodeDialog();
    }

}


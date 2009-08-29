package ie.tcd.imm.hits.image;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "INCellImageReader" Node.
 * This node reads/handles INCell images.
 *
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
public class INCellImageReaderNodeFactory 
        extends NodeFactory<INCellImageReaderNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public INCellImageReaderNodeModel createNodeModel() {
        return new INCellImageReaderNodeModel();
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
    public NodeView<INCellImageReaderNodeModel> createNodeView(final int viewIndex,
            final INCellImageReaderNodeModel nodeModel) {
        return new INCellImageReaderNodeView(nodeModel);
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
        return new INCellImageReaderNodeDialog();
    }

}


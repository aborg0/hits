package ie.tcd.imm.hits.knime.cellhts2.worker;

import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeView;

/**
 * <code>NodeView</code> for the "CellHTS2" Node.
 * This node performs the calculations using CellHTS2
 *
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
public class CellHTS2NodeView extends NodeView {

    /**
     * Creates a new view.
     * 
     * @param nodeModel The model (class: {@link CellHTS2NodeModel})
     */
    protected CellHTS2NodeView(final NodeModel nodeModel) {
        super(nodeModel);

        // TODO instantiate the components of the view here.

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void modelChanged() {

        // TODO retrieve the new model from your nodemodel and 
        // update the view.
        CellHTS2NodeModel nodeModel = 
            (CellHTS2NodeModel)getNodeModel();
        assert nodeModel != null;
        
        // be aware of a possibly not executed nodeModel! The data you retrieve
        // from your nodemodel could be null, emtpy, or invalid in any kind.
        
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onClose() {
    
        // TODO things to do when closing the view
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onOpen() {

        // TODO things to do when opening the view
    }

}


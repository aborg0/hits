package ie.tcd.imm.hits.knime.view.dendrogram;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "Dendrogram" Node. Allows to create
 * dendrogram with a heatmap of parameters.
 * 
 * @author TCD
 */
public class DendrogramNodeFactory extends NodeFactory<DendrogramNodeModel> {

	/**
	 * {@inheritDoc}
	 */
	@Override
	public DendrogramNodeModel createNodeModel() {
		return new DendrogramNodeModel();
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
	@SuppressWarnings("unchecked")
	@Override
	public NodeView<DendrogramNodeModel> createNodeView(final int viewIndex,
			final DendrogramNodeModel nodeModel) {
		// return new HierarchicalClusterNodeView(nodeModel,
		// new HeatmapDendrogramPlotter(
		// new HeatmapDendrogramDrawingPane(),
		// new HeatmapDendrogramPlotterProperties()));
		return new DendrogramNodeView(nodeModel, new HeatmapDendrogramPlotter(
				new HeatmapDendrogramDrawingPane(),
				new HeatmapDendrogramPlotterProperties()));
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
		return new DendrogramNodeDialog();
	}
}

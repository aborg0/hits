/**
 * 
 */
package ie.tcd.imm.hits.knime.view.dendrogram;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.knime.base.node.viz.plotter.DataProvider;
import org.knime.base.node.viz.plotter.dendrogram.BinaryTree;
import org.knime.base.node.viz.plotter.dendrogram.BinaryTreeNode;
import org.knime.base.node.viz.plotter.dendrogram.DendrogramDrawingPane;
import org.knime.base.node.viz.plotter.dendrogram.DendrogramNode;
import org.knime.base.node.viz.plotter.dendrogram.DendrogramPlotter;
import org.knime.base.node.viz.plotter.dendrogram.DendrogramPoint;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.StringCell;

import edu.umd.cs.findbugs.annotations.DefaultAnnotation;

/**
 * The plotter for the heatmap with dendrogram node.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
@DefaultAnnotation( { Nonnull.class, CheckReturnValue.class })
public class HeatmapDendrogramPlotter extends DendrogramPlotter {
	private static final long serialVersionUID = -7290110714225915563L;
	private DendrogramNode rootNode;

	/**
	 * @param heatmapDendrogramDrawingPane
	 *            The drawing pane.
	 * @param heatmapDendrogramPlotterProperties
	 *            The properties component.
	 */
	public HeatmapDendrogramPlotter(
			final HeatmapDendrogramDrawingPane heatmapDendrogramDrawingPane,
			final HeatmapDendrogramPlotterProperties heatmapDendrogramPlotterProperties) {
		super(heatmapDendrogramDrawingPane, heatmapDendrogramPlotterProperties);
		heatmapDendrogramPlotterProperties.getColourModel().addActionListener(
				new ActionListener() {
					@Override
					public void actionPerformed(final ActionEvent e) {
						getDrawingPane().repaint();
					}
				});
		heatmapDendrogramDrawingPane
				.setColourModel(heatmapDendrogramPlotterProperties
						.getColourModel());
	}

	@Override
	public void setDataProvider(final DataProvider provider) {
		super.setDataProvider(provider);
		if (provider instanceof DendrogramNodeModel) {
			final DendrogramNodeModel model = (DendrogramNodeModel) provider;
			final Iterable<String> selectedColumns = model.getSelectedColumns();
			((HeatmapDendrogramPlotterProperties) getProperties()).update(
					selectedColumns, model.getRange()
			// Collections// TODO
					// .<String, Map<StatTypes, Map<RangeType, Double>>>
					// emptyMap()
					);
			((HeatmapDendrogramDrawingPane) getDrawingPane())
					.setNodeModel(model);
		}
	}

	@Override
	public void updatePaintModel() {
		if (rootNode == null) {
			return;
		}
		if (getDataProvider() == null
				|| getDataProvider().getDataArray(1) == null
				|| getDataProvider().getDataArray(1).size() == 0) {
			return;
		}
		final double min = 0;
		final double max = rootNode.getMaxDistance() * 10;
		setPreserve(false);
		createXCoordinate(min, max);
		final int offset = getDataProvider().getDataArray(1).getDataTableSpec()
				.getNumColumns() * 20;
		getXAxis().setStartTickOffset(offset);

		// getYAxis().setStartTickOffset();
		final Set<RowKey> rowKeys = new LinkedHashSet<RowKey>();
		getRowKeys(rootNode, rowKeys);
		final Set<DataCell> keys = new LinkedHashSet<DataCell>();
		for (final RowKey rk : rowKeys) {
			keys.add(new StringCell(rk.getString()));
		}
		createNominalYCoordinate(keys);
		// m_tree = null;
		final BinaryTree<DendrogramPoint> tree = viewModel();
		((DendrogramDrawingPane) getDrawingPane()).setRootNode(tree);
		((HeatmapDendrogramDrawingPane) getDrawingPane())
				.setHeatmapCellHeight((int) getYAxis().getCoordinate()
						.getUnusedDistBetweenTicks(
								getDrawingPaneDimension().height));
		getDrawingPane().repaint();
	}

	@Override
	public void createXCoordinate(final double min, final double max) {
		super.createXCoordinate(min, max);
	}

	/**
	 * @return The model for the view.
	 */
	private BinaryTree<DendrogramPoint> viewModel() {
		final BinaryTree<DendrogramPoint> tree = rootNode == null ? null
				: new BinaryTree<DendrogramPoint>(createViewModelFor(rootNode));
		return tree;
	}

	@Override
	public void updateSize() {
		if (rootNode == null) {
			return;
		}
		if (getDataProvider() == null
				|| getDataProvider().getDataArray(1) == null) {
			return;
		}
		if (getXAxis() == null || getYAxis() == null) {
			updatePaintModel();
		}
		// getYAxis().setPreferredSize(
		// new Dimension(150, getYAxis().getPreferredSize().height));
		final BinaryTree<DendrogramPoint> tree = viewModel();
		((DendrogramDrawingPane) getDrawingPane()).setRootNode(tree);
		((HeatmapDendrogramDrawingPane) getDrawingPane())
				.setHeatmapCellHeight((int) getYAxis().getCoordinate()
						.getUnusedDistBetweenTicks(
								getDrawingPaneDimension().height));
		getDrawingPane().repaint();
	}

	private void getRowKeys(final DendrogramNode node, final Set<RowKey> ids) {
		if (node == null) {
			return;
		}
		if (node.isLeaf()) {
			ids.add(node.getLeafDataPoint().getKey());
			return;
		}
		getRowKeys(node.getFirstSubnode(), ids);
		getRowKeys(node.getSecondSubnode(), ids);

	}

	@Override
	public void setRootNode(final DendrogramNode root) {
		super.setRootNode(root);
		rootNode = root;
	}

	/**
	 * Recursive method to convert the result of the hierachical clustering
	 * result represented by a
	 * {@link org.knime.base.node.viz.plotter.dendrogram.DendrogramNode} into a
	 * {@link org.knime.base.node.viz.plotter.dendrogram.BinaryTree} of
	 * {@link org.knime.base.node.viz.plotter.dendrogram.DendrogramPoint}s.
	 * 
	 * @param node
	 *            the node to convert
	 * @return the visual model of the passed
	 *         {@link org.knime.base.node.viz.plotter.dendrogram.DendrogramNode}
	 */
	private BinaryTreeNode<DendrogramPoint> createViewModelFor(
			final DendrogramNode node) {
		if (getXAxis() == null || getXAxis().getCoordinate() == null
				|| getYAxis() == null || getYAxis().getCoordinate() == null) {
			updatePaintModel();
		}
		BinaryTreeNode<DendrogramPoint> viewNode;
		// distinction between cluster node and leaf:
		final Dimension dim = getDrawingPaneDimension();
		// final int height = dim.height - (2 * OFFSET) - (m_dotSize / 2);
		final int width = dim.width;
		int x = (int) getXAxis().getCoordinate().calculateMappedValue(
				new DoubleCell(node.getDist()), width);

		x += getDataProvider().getDataArray(1).getDataTableSpec()
				.getNumColumns() * 20;
		int y;
		DendrogramPoint p;
		if (!node.isLeaf()) {
			y = dim.height - getYPosition(node);
			p = new DendrogramPoint(new Point(x, y), node.getDist());
		} else {
			final DataRow row = node.getLeafDataPoint();
			y = dim.height
					- (int) getYAxis().getCoordinate().calculateMappedValue(
							new StringCell(row.getKey().getString()),
							dim.height);
			p = new DendrogramPoint(new Point(x, y), node.getDist());
			final DataTableSpec spec = getDataProvider().getDataArray(1)
					.getDataTableSpec();
			p.setColor(spec.getRowColor(row));
			p.setShape(spec.getRowShape(row));
			p.setRelativeSize(spec.getRowSizeFactor(row));
			p.setHilite(delegateIsHiLit(row.getKey()));
		}
		viewNode = new BinaryTreeNode<DendrogramPoint>(p);
		final Set<RowKey> keys = new LinkedHashSet<RowKey>();
		getRowKeys(node, keys);
		viewNode.getContent().addRows(keys);
		// viewNode.getContent().setSelected(
		// m_selected.contains(viewNode.getContent()));
		viewNode.getContent().setHilite(delegateIsHiLit(keys));
		if (node.getFirstSubnode() != null) {
			final BinaryTreeNode<DendrogramPoint> leftNode = createViewModelFor(node
					.getFirstSubnode());
			leftNode.setParent(viewNode);
			viewNode.setLeftChild(leftNode);
		}
		if (node.getSecondSubnode() != null) {
			final BinaryTreeNode<DendrogramPoint> rightNode = createViewModelFor(node
					.getSecondSubnode());
			rightNode.setParent(viewNode);
			viewNode.setRightChild(rightNode);
		}
		return viewNode;
	}

	/**
	 * The y position is the center of the distance between the two subnodes or
	 * the position of the leaf node on the y axis.
	 * 
	 * @param node
	 *            the node to determine the mapped y position for
	 * @return the y position of the visual model for the passed node
	 */
	private int getYPosition(final DendrogramNode node) {
		if (node.isLeaf()) {
			final DataCell value = new StringCell(node.getLeafDataPoint()
					.getKey().getString());
			return (int) getYAxis().getCoordinate().calculateMappedValue(value,
					getDrawingPaneDimension().height);
		}
		return (getYPosition(node.getFirstSubnode()) + getYPosition(node
				.getSecondSubnode())) / 2;
	}

}

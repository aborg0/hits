/**
 * 
 */
package ie.tcd.imm.hits.knime.view.dendrogram;

import ie.tcd.imm.hits.knime.view.heatmap.HeatmapNodeModel.StatTypes;
import ie.tcd.imm.hits.util.swing.colour.ColourComputer;
import ie.tcd.imm.hits.util.swing.colour.ColourSelector.ColourModel;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Stroke;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.knime.base.node.util.DataArray;
import org.knime.base.node.viz.plotter.dendrogram.BinaryTree;
import org.knime.base.node.viz.plotter.dendrogram.BinaryTreeNode;
import org.knime.base.node.viz.plotter.dendrogram.DendrogramDrawingPane;
import org.knime.base.node.viz.plotter.dendrogram.DendrogramPoint;
import org.knime.base.node.viz.plotter.dendrogram.BinaryTree.Traversal;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.RowKey;
import org.knime.core.data.property.ColorAttr;

import edu.umd.cs.findbugs.annotations.DefaultAnnotation;

/**
 * Drawing pane for the heatmap with dendrogram node.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
@DefaultAnnotation( { Nonnull.class, CheckReturnValue.class })
public class HeatmapDendrogramDrawingPane extends DendrogramDrawingPane {
	private static final long serialVersionUID = 5198699225298295730L;
	private static final float BOLD = 2.0f;
	private BinaryTree<DendrogramPoint> rootNode;
	private float lineThickness = 1.0f;
	private DendrogramNodeModel nodeModel;
	private int[] indices;
	private ColourModel colourModel;
	private int cellHeight;
	private int cellWidth = 20;
	private final List<String> selectedColumns = new ArrayList<String>();
	private int maxStringLength;
	private String[] keys;

	/** Constructs the drawing pane. */
	public HeatmapDendrogramDrawingPane() {
		super();
	}

	/**
	 * @param nodeModel
	 *            the new {@link DendrogramNodeModel}.
	 */
	public void setNodeModel(final DendrogramNodeModel nodeModel) {
		this.nodeModel = nodeModel;
		maxStringLength = 0;
		if (this.nodeModel != null) {
			computeIndices();
			for (final DataRow row : this.nodeModel.getDataArray(1)) {
				maxStringLength = Math.max(maxStringLength, getFontMetrics(
						getFont()).stringWidth(row.getKey().getString()));
			}
		}
		selectedColumns.clear();
		selectedColumns.addAll(this.nodeModel.getSelectedColumns());
	}

	/**
	 * @return The maximal length of {@link RowKey}s in pixels.
	 */
	public int getMaxStringLength() {
		return maxStringLength;
	}

	private void computeIndices() {
		final DataArray dataArray = nodeModel.getDataArray(1);
		final List<String> selectedColumns = new LinkedList<String>(nodeModel
				.getSelectedColumns());
		indices = new int[selectedColumns.size()];
		int i = 0;
		for (final String column : selectedColumns) {
			indices[i++] = dataArray.getDataTableSpec().findColumnIndex(column);
		}
	}

	@Override
	public void setRootNode(final BinaryTree<DendrogramPoint> root) {
		rootNode = root;
		super.setRootNode(root);
		if (rootNode != null) {
			keys = new String[nodeModel.getDataArray(1).size()];
			int i = 0;
			for (final BinaryTreeNode<DendrogramPoint> node : root
					.getNodes(Traversal.IN)) {
				if (node.isLeaf()) {
					keys[i++] = node.getContent().getRows().iterator().next()
							.getString();
				}
			}
		}
	}

	@Override
	public void paintContent(final Graphics g) {
		if (rootNode == null) {
			return;
		}
		final Stroke backupStroke = ((Graphics2D) g).getStroke();
		final Color backupColor = g.getColor();
		final List<BinaryTreeNode<DendrogramPoint>> nodes = rootNode
				.getNodes(BinaryTree.Traversal.IN);
		final int fontHeight = g.getFontMetrics().getHeight();
		for (final BinaryTreeNode<DendrogramPoint> node : nodes) {
			final DendrogramPoint dendroPoint = node.getContent();
			if (dendroPoint.getRows().size() == 1 && nodeModel != null) {
				final Point point = dendroPoint.getPoint();
				final String key = dendroPoint.getRows().iterator().next()
						.getString();
				final int index = nodeModel.getMap().get(key).intValue();
				final DataRow row = nodeModel.getDataArray(1).getRow(index);
				final Color color = g.getColor();
				for (int i = 0; i < indices.length; ++i) {
					final DataCell cell = row.getCell(indices[i]);
					if (cell instanceof DoubleValue) {
						final double val = ((DoubleValue) cell)
								.getDoubleValue();
						final ColourComputer model = colourModel.getModel(
								nodeModel.getSelectedColumns().get(i),
								StatTypes.raw);
						final Color col = model.compute(val);
						g.setColor(col);
						g.fillRect(point.x + (i - selectedColumns.size())
								* cellWidth, point.y - cellHeight / 2,
								cellWidth, cellHeight + 1);
					}
				}
				g.setColor(color);
				g.drawString(row.getKey().getString(), point.x
						- selectedColumns.size()// indices.length
						* cellWidth
						- g.getFontMetrics().stringWidth(
								row.getKey().getString()), point.y
						+ /*
						 * cellHeight / 2 -
						 */fontHeight / 3);
			}
			// set the correct stroke and color
			g.setColor(ColorAttr.DEFAULT.getColor(node.getContent()
					.isSelected(), node.getContent().isHilite()));
			if (node.getContent().isSelected() || node.getContent().isHilite()) {
				((Graphics2D) g).setStroke(new BasicStroke(
						(lineThickness * HeatmapDendrogramDrawingPane.BOLD)));
				if (node.getContent().getRows().size() == 1) {
					final Point point = node.getContent().getPoint();
					g.drawRect(point.x - selectedColumns.size() * cellWidth,
							point.y - cellHeight / 2 + 1, cellWidth
									* selectedColumns.size(), cellHeight);
				}
			} else {
				((Graphics2D) g).setStroke(new BasicStroke(lineThickness));
			}
			if (node.getLeftChild() != null || node.getRightChild() != null) {
				// draw vertical line
				final Point leftPoint = node.getLeftChild().getContent()
						.getPoint();
				final Point rightPoint = node.getRightChild().getContent()
						.getPoint();
				g.drawLine(node.getContent().getPoint().x/* leftPoint.x */,
						leftPoint.y /* node.getContent().getPoint().y */, node
								.getContent().getPoint().x/* rightPoint.x */,
						rightPoint.y /* node.getContent().getPoint().y */);
			}
			// draw horizontal line
			if (node.getParent() != null) {
				g.setColor(ColorAttr.DEFAULT.getColor(node.getParent()
						.getContent().isSelected(), node.getParent()
						.getContent().isHilite()));
				// check if parent is selected
				// if yes bold line, else normal line
				if (node.getParent().getContent().isSelected()
						|| node.getParent().getContent().isHilite()) {
					((Graphics2D) g)
							.setStroke(new BasicStroke(
									(lineThickness * HeatmapDendrogramDrawingPane.BOLD)));
				} else {
					((Graphics2D) g).setStroke(new BasicStroke(lineThickness));
				}
				g.drawLine(node.getContent().getPoint().x, node.getContent()
						.getPoint().y,
						node.getParent().getContent().getPoint().x, node
								.getContent().getPoint().y);
			}
			((Graphics2D) g).setStroke(backupStroke);
			g.setColor(backupColor);
		}
	}

	/**
	 * Sets the actual cell height of the heatmap.
	 * 
	 * @param cellHeight
	 *            The new cell height.
	 */
	public void setHeatmapCellHeight(final int cellHeight) {
		this.cellHeight = cellHeight;
	}

	/**
	 * @param colourModel
	 *            The new ColourModel.
	 */
	public void setColourModel(final ColourModel colourModel) {
		this.colourModel = colourModel;
	}

	@Override
	public String getToolTipText(final MouseEvent event) {
		if (rootNode == null) {
			return "";
		}
		final Point p = event.getPoint();
		final DataArray dataArray = nodeModel.getDataArray(1);
		final int startPos = maxStringLength + selectedColumns.size()
		// dataArray.getDataTableSpec().getNumColumns()
				* cellWidth;
		final int allCount = selectedColumns.size();
		if (p.x < startPos && p.y < getHeight()) {
			final int idx = allCount - (startPos - p.x + cellWidth - 1)
					/ cellWidth;
			final int rowIdx = dataArray.size() - 1 - p.y * dataArray.size()
					/ getHeight();
			if (idx < 0) {
				return keys[rowIdx];
			}
			return "<html>"
					+ selectedColumns.get(idx)
					+ ": <b>"
					+ Math.round(((DoubleValue) nodeModel.getDataArray(1)
							.getRow(nodeModel.getMap().get(keys[rowIdx]))
							.getCell(indices[idx])).getDoubleValue() * 1000)
					/ 1000.0 + "</b> (" + keys[rowIdx] + ")</html>";
		} else {
			return super.getToolTipText(event);
		}
	}

	/**
	 * @return The width of a cell in the heatmap part.
	 */
	public int getCellWidth() {
		return cellWidth;
	}

	/**
	 * @param cellWidth
	 *            The new cell width in the heatmap part.
	 */
	public void setCellWidth(final int cellWidth) {
		this.cellWidth = cellWidth;
	}

	/**
	 * @return The (unmodifiable) {@link List} of selected columns.
	 */
	public List<String> getSelectedColumns() {
		return Collections.unmodifiableList(selectedColumns);
	}

	@Override
	public void setLineThickness(final int thickness) {
		super.setLineThickness(thickness);
		lineThickness = thickness;
	}
}

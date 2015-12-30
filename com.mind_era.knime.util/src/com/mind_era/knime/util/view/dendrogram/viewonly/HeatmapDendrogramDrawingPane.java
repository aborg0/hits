/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
 * and KNIME GmbH, Konstanz, Germany
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * -------------------------------------------------------------------
 */
package com.mind_era.knime.util.view.dendrogram.viewonly;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Stroke;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.knime.base.node.util.DataArray;
import org.knime.base.node.viz.plotter.dendrogram.BinaryTree;
import org.knime.base.node.viz.plotter.dendrogram.BinaryTree.Traversal;
import org.knime.base.node.viz.plotter.dendrogram.BinaryTreeNode;
import org.knime.base.node.viz.plotter.dendrogram.DendrogramDrawingPane;
import org.knime.base.node.viz.plotter.dendrogram.DendrogramPoint;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.RowKey;
import org.knime.core.data.property.ColorAttr;

import com.mind_era.knime.common.util.HiliteType;
import com.mind_era.knime.common.util.Misc;
import com.mind_era.knime.common.util.swing.colour.ColourComputer;
import com.mind_era.knime.common.util.swing.colour.ColourSelector.ColourModel;
import com.mind_era.knime.common.view.StatTypes;

/**
 * Drawing pane for the heatmap with dendrogram node.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
@Nonnull
@CheckReturnValue
public class HeatmapDendrogramDrawingPane extends DendrogramDrawingPane {
	private static final long serialVersionUID = 5198699225298295730L;
	private static final float BOLD = 2.0f;
	private @Nullable BinaryTree<DendrogramPoint> rootNode;
	private float lineThickness = 1.0f;
	private DendrogramNodeModel nodeModel;
	private @Nullable int[] indices;
	private @Nullable int[] selectedIndices;
	private @Nullable ColourModel colourModel;
	private int cellHeight;
	private int cellWidth = 20;
	private final List<String> selectedColumns = new ArrayList<String>();
	private final List<String> visibleColumns = new ArrayList<String>();
	private int maxStringLength;
	private @Nullable String[] keys;
	private boolean directionLeftToRight;

	private int leafX;
	private boolean showValues;
	private int clusterCount;
	private final Set<String> lastClusterKeys = new HashSet<String>();

	private HiliteType hilite = HiliteType.Normal;

	// private boolean directionUpToDown;

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
		if (this.nodeModel != null && this.nodeModel.getDataArray(1) != null) {
			computeIndices();
			for (final DataRow row : this.nodeModel.getOrigData()) {
				maxStringLength = Math.max(
						maxStringLength,
						getFontMetrics(getFont()).stringWidth(
								row.getKey().getString()));
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
		final DataTable dataArray = nodeModel.getOrigData();
		indices = new int[visibleColumns.size()];
		selectedIndices = new int[selectedColumns.size()];
		int i = 0;
		for (final String column : visibleColumns) {
			indices[i++] = dataArray.getDataTableSpec().findColumnIndex(column);
		}
		i = 0;
		for (final String selected : selectedColumns) {
			final int idx = dataArray.getDataTableSpec().findColumnIndex(
					selected);
			selectedIndices[i] = -1;
			for (int j = indices.length; j-- > 0;) {
				if (indices[j] == idx) {
					selectedIndices[i++] = j;
					break;
				}
			}
		}
	}

	@Override
	public void setRootNode(final BinaryTree<DendrogramPoint> root) {
		rootNode = root;
		super.setRootNode(root);
		if (rootNode != null) {
			keys = new String[nodeModel.getOrigData().size()];
			int i = 0;
			for (final BinaryTreeNode<DendrogramPoint> node : root
					.getNodes(Traversal.IN)) {
				if (node.isLeaf()) {
					keys[i++] = node.getContent().getRows().iterator().next()
							.getString();
				}
			}
		}
		repaint();
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

		final FontMetrics fm = g.getFontMetrics();
		final int fontHeight = fm.getHeight();
		for (final BinaryTreeNode<DendrogramPoint> node : nodes) {
			final DendrogramPoint dendroPoint = node.getContent();
			if (dendroPoint.getRows().size() == 1 && nodeModel != null) {
				final Point point = dendroPoint.getPoint();
				leafX = point.x;
				final String key = dendroPoint.getRows().iterator().next()
						.getString();
				final int index = nodeModel.getMap().get(key).intValue();
				final DataRow row = nodeModel.getOrigData().getRow(index);
				final Color color = g.getColor();
				for (int i = 0; i < indices.length; ++i) {
					final DataCell cell = row.getCell(indices[i]);
					if (cell instanceof DoubleValue) {
						final double val = ((DoubleValue) cell)
								.getDoubleValue();
						final ColourComputer model = colourModel.getModel(
								visibleColumns.get(i), StatTypes.raw);
						final Color col = model.compute(val);
						g.setColor(col);
						g.fillRect(
								point.x
										+ (directionLeftToRight ? (i - visibleColumns
												.size()) * cellWidth
												: i /*- 1*/
														* cellWidth), point.y
										- cellHeight / 2, cellWidth,
								cellHeight + 1);
						if (showValues) {
							g.setColor(Color.RGBtoHSB(col.getRed(),
									col.getGreen(), col.getBlue(), null)[2] > .6f ? Color.BLACK
									: Color.WHITE);
							final String str = Misc.round(val);
							g.drawString(
									str,
									point.x
											+ (directionLeftToRight ? (i - visibleColumns
													.size()) * cellWidth
													: i * cellWidth)

											+ (cellWidth - fm.stringWidth(str))
											/ 2, point.y + fontHeight / 3);
						}
					}
				}

				final ColorAttr colorAttr = nodeModel.getOrigData()
						.getDataTableSpec().getRowColor(row);
				if (colorAttr != ColorAttr.DEFAULT) {
					final Color rowColor = colorAttr.getColor();
					g.setColor(rowColor);
					g.fillRect(
							directionLeftToRight ? point.x
									- visibleColumns.size() * cellWidth
									- maxStringLength : leafX
									+ visibleColumns.size() * cellWidth,
							point.y - cellHeight / 2, maxStringLength,
							cellHeight + 1);
					g.setColor(Color.RGBtoHSB(rowColor.getGreen(),
							rowColor.getGreen(), rowColor.getBlue(), null)[2] < .4f ? Color.WHITE
							: Color.BLACK);
				} else {
					g.setColor(Color.RGBtoHSB(getBackground().getGreen(),
							getBackground().getGreen(), getBackground()
									.getBlue(), null)[2] < .4f ? Color.WHITE
							: Color.BLACK);
				}
				g.drawString(
						row.getKey().getString(),
						directionLeftToRight ? point.x - visibleColumns.size()
								* cellWidth
								- fm.stringWidth(row.getKey().getString())
								: point.x + visibleColumns.size() * cellWidth,
						point.y + /*
								 * cellHeight / 2 -
								 */fontHeight / 3);
				if (lastClusterKeys.contains(row.getKey().getString())) {
					g.setColor(Color.RGBtoHSB(getBackground().getGreen(),
							getBackground().getGreen(), getBackground()
									.getBlue(), null)[2] < .4f ? Color.WHITE
							: Color.BLACK);
					final int y = point.y + cellHeight / 2;
					((Graphics2D) g).setStroke(new BasicStroke(1.0f,
							BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER,
							10.0f, new float[] { 2.0f, 3.0f }, 0.0f));
					g.drawLine(0, y, getWidth(), y);
				}
				g.setColor(color);
			}
			// set the correct stroke and color
			switch (hilite) {
			case Normal:
				g.setColor(ColorAttr.DEFAULT.getColor(node.getContent()
						.isSelected(), node.getContent().isHilite()));
				break;
			case HideUnHilit:
				g.setColor(node.getContent().isSelected()
						|| node.getContent().isHilite() ? ColorAttr.DEFAULT
						.getColor(node.getContent().isSelected(), false)
						: getBackground());
				break;
			case FadeUnHilit:
				g.setColor(node.getContent().isSelected()
						|| node.getContent().isHilite() ? ColorAttr.DEFAULT
						.getColor(node.getContent().isSelected(), false)
						: node.getContent().isSelected() ? ColorAttr.INACTIVE_SELECTED
								: ColorAttr.INACTIVE);
				break;
			default:
				break;
			}
			if (node.getContent().isSelected() || node.getContent().isHilite()) {
				((Graphics2D) g).setStroke(new BasicStroke(
						(lineThickness * HeatmapDendrogramDrawingPane.BOLD)));
				if (node.isLeaf()) {
					assert node.getContent().getRows().size() == 1 : node
							.getContent().getRows().size();
					final Point point = node.getContent().getPoint();
					g.drawRect(
							point.x
									- (directionLeftToRight ? visibleColumns
											.size() * cellWidth : 0), point.y
									- cellHeight / 2 + 1, cellWidth
									* visibleColumns.size(), cellHeight);
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
				switch (hilite) {
				case Normal:
					g.setColor(ColorAttr.DEFAULT.getColor(node.getContent()
							.isSelected(), node.getContent().isHilite()));
					break;
				case HideUnHilit:
					g.setColor(node.getContent().isSelected()
							|| node.getContent().isHilite() ? ColorAttr.DEFAULT
							.getColor(node.getContent().isSelected(), false)
							: getBackground());
					break;
				case FadeUnHilit:
					g.setColor(node.getContent().isSelected()
							|| node.getContent().isHilite() ? ColorAttr.DEFAULT
							.getColor(node.getContent().isSelected(), false)
							: node.getContent().isSelected() ? ColorAttr.INACTIVE_SELECTED
									: ColorAttr.INACTIVE);
					break;
				default:
					break;
				}
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
			} else {
				g.fillOval(dendroPoint.getPoint().x, dendroPoint.getPoint().y,
						4, 4);
			}
		}
		((Graphics2D) g).setStroke(backupStroke);
		g.setColor(ColorAttr.SELECTED);
		for (final int selectedIndex : selectedIndices) {
			final int pos = leafX
					+ (directionLeftToRight ? (selectedIndex - visibleColumns
							.size()) * cellWidth : selectedIndex * cellWidth);
			g.drawLine(pos, 0, pos, getHeight());
			g.drawLine(pos + cellWidth, 0, pos + cellWidth, getHeight());
		}
		g.setColor(backupColor);
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
		final DataArray dataArray = nodeModel.getOrigData();
		final int allCount = visibleColumns.size();
		final int startPos = directionLeftToRight ? maxStringLength + allCount
				* cellWidth : leafX;
		if ((directionLeftToRight && p.x < startPos || !directionLeftToRight
				&& p.x > startPos)
				&& p.y < getHeight()) {
			final int idx = directionLeftToRight ? allCount
					- (startPos - p.x + cellWidth - 1) / cellWidth
					: (p.x - startPos) / cellWidth;
			final int rowIdx = dataArray.size() - 1 - p.y * dataArray.size()
					/ getHeight();
			if (idx < 0 && directionLeftToRight || !directionLeftToRight
					&& idx >= allCount) {
				return keys[rowIdx];
			}
			final DataCell cell = nodeModel.getOrigData()
					.getRow(nodeModel.getMap().get(keys[rowIdx]).intValue())
					.getCell(indices[idx]);
			return "<html>"
					+ visibleColumns.get(idx)
					+ ": <b>"
					+ (cell instanceof DoubleValue ? Math
							.round(((DoubleValue) cell).getDoubleValue() * 1000) / 1000.0
							: Double.NaN) + "</b> (" + keys[rowIdx]
					+ ")</html>";
		}
		return super.getToolTipText(event);
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
	 * @return The (unmodifiable) {@link List} of visible columns.
	 */
	List<String> getVisibleColumns() {
		return Collections.unmodifiableList(visibleColumns);
	}

	@Override
	public void setLineThickness(final int thickness) {
		super.setLineThickness(thickness);
		lineThickness = thickness;
	}

	/**
	 * @param directionLeftToRight
	 *            The new value of indicator of left to right increase field.
	 */
	public void setHorizontalDirection(final boolean directionLeftToRight) {
		this.directionLeftToRight = directionLeftToRight;
	}

	//
	// /**
	// * @param directionUpToDown
	// * The new value of the up to down order of values field.
	// */
	// public void setVerticalDirection(final boolean directionUpToDown) {
	// this.directionUpToDown = directionUpToDown;
	// }

	/**
	 * Selects the visible columns (and clears selection of columns).
	 * 
	 * @param visibleColumns
	 *            The visible columns.
	 */
	protected void setVisibleColumns(final List<String> visibleColumns) {
		this.visibleColumns.clear();
		this.visibleColumns.addAll(visibleColumns);
		selectedColumns.clear();
		computeIndices();
	}

	/**
	 * @param selectedColumns
	 *            The selected columns.
	 */
	protected void setSelectedColumns(final List<String> selectedColumns) {
		this.selectedColumns.clear();
		this.selectedColumns.addAll(selectedColumns);
		computeIndices();
	}

	/**
	 * Updates the showValues property.
	 * 
	 * @param showValues
	 *            The new value for the property.
	 */
	public void setShowValues(final boolean showValues) {
		this.showValues = showValues;
	}

	/**
	 * Updates the cluster count to show on pane.
	 * 
	 * @param clusterCount
	 *            The new cluster count.
	 */
	public void setClusterCount(final int clusterCount) {
		final List<BinaryTreeNode<DendrogramPoint>> nodes = rootNode
				.getNodes(Traversal.PRE);
		if (clusterCount > (nodes.size() + 1) / 2 || clusterCount < 1) {
			throw new IllegalArgumentException(
					"The number of cluster cannot exceed the number of leaves in the hierarchy, and must be at least 0.");
		}
		this.clusterCount = clusterCount;
		lastClusterKeys.clear();
		final TreeSet<BinaryTreeNode<DendrogramPoint>> treeSet = new TreeSet<BinaryTreeNode<DendrogramPoint>>(
				new Comparator<BinaryTreeNode<DendrogramPoint>>() {
					@Override
					public int compare(
							final BinaryTreeNode<DendrogramPoint> o1,
							final BinaryTreeNode<DendrogramPoint> o2) {
						return Double.compare(o1.getContent().getDistance(), o2
								.getContent().getDistance());
					}
				});
		for (final BinaryTreeNode<DendrogramPoint> node : nodes) {
			treeSet.add(node);
		}
		while (lastClusterKeys.size() < this.clusterCount - 1) {
			final BinaryTreeNode<DendrogramPoint> last = treeSet.last();
			treeSet.remove(last);
			BinaryTreeNode<DendrogramPoint> p = last.getRightChild();
			while (!p.isLeaf()) {
				p = p.getLeftChild();
				// p = p.getLeftChild().getContent().getDistance() > p
				// .getRightChild().getContent().getDistance()
				// /* || p.getLeftChild().isLeaf() */? p.getLeftChild() : p
				// .getRightChild();
			}
			lastClusterKeys.add(p.getContent().getRows().iterator().next()
					.getString());
		}
	}

	/**
	 * Sets the strategy of HiLite to {@code hilite}.
	 * 
	 * @param hilite
	 *            The new strategy to HiLite the content.
	 */
	public void setHilite(final HiliteType hilite) {
		this.hilite = hilite;
	}
}

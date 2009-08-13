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
package ie.tcd.imm.hits.knime.view.dendrogram.viewonly;

import ie.tcd.imm.hits.knime.util.HiliteType;
import ie.tcd.imm.hits.knime.util.ShiftedLogarithmicMappingMethod;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.base.node.util.DataArray;
import org.knime.base.node.viz.plotter.AbstractPlotter;
import org.knime.base.node.viz.plotter.DataProvider;
import org.knime.base.node.viz.plotter.dendrogram.BinaryTree;
import org.knime.base.node.viz.plotter.dendrogram.BinaryTreeNode;
import org.knime.base.node.viz.plotter.dendrogram.DendrogramDrawingPane;
import org.knime.base.node.viz.plotter.dendrogram.DendrogramNode;
import org.knime.base.node.viz.plotter.dendrogram.DendrogramPlotter;
import org.knime.base.node.viz.plotter.dendrogram.DendrogramPoint;
import org.knime.base.node.viz.plotter.dendrogram.BinaryTree.Traversal;
import org.knime.base.util.coordinate.AscendingNumericTickPolicyStrategy;
import org.knime.base.util.coordinate.Coordinate;
import org.knime.base.util.coordinate.DescendingNumericTickPolicyStrategy;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.property.ColorAttr;
import org.knime.core.data.property.ShapeFactory;
import org.knime.core.data.property.ShapeFactory.Shape;

import edu.umd.cs.findbugs.annotations.DefaultAnnotation;

/**
 * The plotter for the heatmap with dendrogram node.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
@DefaultAnnotation( { Nonnull.class, CheckReturnValue.class })
public class HeatmapDendrogramPlotter extends DendrogramPlotter {
	private static final long serialVersionUID = -7290110714225915563L;

	static {
		Coordinate.addMappingMethod(DoubleValue.class,
				ShiftedLogarithmicMappingMethod.ID_BASE_E,
				new ShiftedLogarithmicMappingMethod(1.0));
		Coordinate.addMappingMethod(DoubleValue.class,
				ShiftedLogarithmicMappingMethod.ID_BASE_10,
				new ShiftedLogarithmicMappingMethod(10.0, 1.0));
		Coordinate.addMappingMethod(DoubleValue.class,
				ShiftedLogarithmicMappingMethod.ID_BASE_2,
				new ShiftedLogarithmicMappingMethod(2.0, 1.0));
	}

	private DendrogramNode rootNode;

	/** The set of selected dendrogram points. */
	private final Set<DendrogramPoint> selected = new HashSet<DendrogramPoint>();
	private BinaryTree<DendrogramPoint> tree;

	private boolean directionLeftToRight;

	private final Map<RowKey, Integer> mapFromKeysToIndices = new HashMap<RowKey, Integer>();

	// private boolean directionUpToDown;

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
		heatmapDendrogramPlotterProperties.getZoomOut().setAction(
				new AbstractAction("Zoom out") {
					private static final long serialVersionUID = 5536007737196887615L;
					{
						setEnabled(false);
					}

					@Override
					public void actionPerformed(final ActionEvent e) {
						// HeatmapDendrogramPlotter.this.setWidth((int) Math
						// .round(HeatmapDendrogramPlotter.this.getWidth()
						// / AbstractPlotter.DEFAULT_ZOOM_FACTOR));
						// HeatmapDendrogramPlotter.this.setHeight((int) Math
						// .round(HeatmapDendrogramPlotter.this
						// .getHeight()
						// / AbstractPlotter.DEFAULT_ZOOM_FACTOR));
						// getDrawingPane().setPreferredSize(
						// new Dimension(HeatmapDendrogramPlotter.this
						// .getWidth(),
						// HeatmapDendrogramPlotter.this
						// .getHeight()));
						// getDrawingPane().invalidate();
						// getDrawingPane().revalidate();
						//
						// updateAxisLength();
						// updateSize();
						//
						// final Rectangle visibleRect = getDrawingPane()
						// .getVisibleRect();
						// final int vWidth = visibleRect.width;
						// final int vHeight = visibleRect.height;
						// final int prefX = (int) (clicked.x /
						// AbstractPlotter.DEFAULT_ZOOM_FACTOR)
						// - vWidth / 2;
						// final int prefY = (int) (clicked.y /
						// AbstractPlotter.DEFAULT_ZOOM_FACTOR)
						// - vHeight / 2;
						// final Rectangle recToVisible = new Rectangle(prefX,
						// prefY, visibleRect.width, visibleRect.height);
						//
						// getDrawingPane().scrollRectToVisible(recToVisible);
						// m_scrolling.revalidate();
						// getDrawingPane().repaint();
						//
						// zoomByWindow(new Rectangle(
						// -heatmapDendrogramDrawingPane.getWidth() / 10,
						// -heatmapDendrogramDrawingPane.getHeight() / 10,
						// heatmapDendrogramDrawingPane.getWidth() * 11,
						// -heatmapDendrogramDrawingPane.getHeight() * 11));
					}
				});
		heatmapDendrogramPlotterProperties.getCellWidth().addChangeListener(
				new ChangeListener() {
					@Override
					public void stateChanged(final ChangeEvent e) {
						heatmapDendrogramDrawingPane
								.setCellWidth(((Integer) heatmapDendrogramPlotterProperties
										.getCellWidth().getValue()).intValue());
						updateSize();
						updatePaintModel();
						getDrawingPane().revalidate();
						getDrawingPane().repaint();
					}
				});
		heatmapDendrogramPlotterProperties.getFlipHorizontal().setAction(
				new AbstractAction("Flip Horizontal") {
					private static final long serialVersionUID = 8403925256828225880L;

					@Override
					public void actionPerformed(final ActionEvent e) {
						updateDirection(heatmapDendrogramPlotterProperties);
						getXAxis()
								.getCoordinate()
								.setPolicy(
										directionLeftToRight ? AscendingNumericTickPolicyStrategy.ID
												: DescendingNumericTickPolicyStrategy.ID);
						updateSize();
						heatmapDendrogramDrawingPane.repaint();
					}
				});
		heatmapDendrogramPlotterProperties.getFlipVertical().setAction(
				new AbstractAction("Flip Vertical") {
					private static final long serialVersionUID = -5557966689877616495L;
					{
						setEnabled(false);
					}

					@Override
					public void actionPerformed(final ActionEvent e) {
						updateDirection(heatmapDendrogramPlotterProperties);
						updateSize();
						heatmapDendrogramDrawingPane.repaint();
					}
				});
		heatmapDendrogramPlotterProperties.getShowValues().setAction(
				new AbstractAction("Show Values") {
					private static final long serialVersionUID = 8315638463449648686L;

					@Override
					public void actionPerformed(final ActionEvent e) {
						heatmapDendrogramDrawingPane
								.setShowValues(heatmapDendrogramPlotterProperties
										.getShowValues().isSelected());
						heatmapDendrogramDrawingPane.repaint();
					}
				});
		getXAxis().addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(final ChangeEvent e) {
				heatmapDendrogramPlotterProperties
						.getFlipHorizontal()
						.setSelected(
								getXAxis()
										.getCoordinate()
										.getCurrentPolicy()
										.getDisplayName()
										.equals(
												DescendingNumericTickPolicyStrategy.ID));
				heatmapDendrogramPlotterProperties.getFlipHorizontal()
						.getAction().actionPerformed(
								new ActionEvent(e.getSource(), 0,
										heatmapDendrogramPlotterProperties
												.getFlipHorizontal()
												.getActionCommand()));
			}
		});
		heatmapDendrogramPlotterProperties.getClusterCount().addChangeListener(
				new ChangeListener() {
					@Override
					public void stateChanged(final ChangeEvent e) {
						heatmapDendrogramDrawingPane
								.setClusterCount(((Number) heatmapDendrogramPlotterProperties
										.getClusterCount().getValue())
										.intValue());
						heatmapDendrogramDrawingPane.repaint();
					}
				});
		updateDirection(heatmapDendrogramPlotterProperties);
	}

	private void updateDirection(
			final HeatmapDendrogramPlotterProperties heatmapDendrogramPlotterProperties) {
		directionLeftToRight = !heatmapDendrogramPlotterProperties
				.getFlipHorizontal().isSelected();
		// getXAxis().getCoordinate().setPolicy(
		// directionLeftToRight ? AscendingNumericTickPolicyStrategy.ID
		// : DescendingNumericTickPolicyStrategy.ID);
		((HeatmapDendrogramDrawingPane) getDrawingPane())
				.setHorizontalDirection(directionLeftToRight);
		// directionUpToDown = !heatmapDendrogramPlotterProperties
		// .getFlipVertical().isSelected();
		// ((HeatmapDendrogramDrawingPane) getDrawingPane())
		// .setVerticalDirection(directionUpToDown);
	}

	@Override
	public void setDataProvider(final DataProvider provider) {
		super.setDataProvider(provider);
		if (provider instanceof DendrogramNodeModel) {
			final DendrogramNodeModel model = (DendrogramNodeModel) provider;
			((HeatmapDendrogramPlotterProperties) getProperties()).update(model
					.getColumns(), model.getRange());
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
		final HeatmapDendrogramDrawingPane dp = (HeatmapDendrogramDrawingPane) getDrawingPane();
		final int offset = dp.getMaxStringLength()
				+ dp.getVisibleColumns().size() * dp.getCellWidth();

		final double min = 0;
		final double max = rootNode.getMaxDistance();
		setPreserve(false);
		createXCoordinate(min, /* Math.max(max + 2, */max * getWidth()
				/ (getWidth() - offset)/* ) */);
		getXAxis().getCoordinate().setPolicy(
				directionLeftToRight ? AscendingNumericTickPolicyStrategy.ID
						: DescendingNumericTickPolicyStrategy.ID);
		getXAxis().setStartTickOffset(offset);

		final Set<RowKey> rowKeys = new LinkedHashSet<RowKey>();
		getRowKeys(rootNode, rowKeys);
		final Set<DataCell> keys = new LinkedHashSet<DataCell>();
		for (final RowKey rk : rowKeys) {
			keys.add(new StringCell(rk.getString()));
		}
		createNominalYCoordinate(keys);
		tree = viewModel();
		final int size = tree.getNodes(Traversal.PRE).size();
		final JSpinner spinner = ((HeatmapDendrogramPlotterProperties) getProperties())
				.getClusterCount();
		final int leafCount = (1 + size) / 2;
		@SuppressWarnings("unchecked")
		final int compareResult = ((SpinnerNumberModel) spinner.getModel())
				.getMaximum().compareTo(Integer.valueOf(leafCount));
		if (compareResult != 0) {
			spinner.setModel(new SpinnerNumberModel(1, 1, leafCount, 1));
		}
		((DendrogramDrawingPane) getDrawingPane()).setRootNode(tree);
		dp.setHeatmapCellHeight((int) getYAxis().getCoordinate()
				.getUnusedDistBetweenTicks(getDrawingPaneDimension().height));
		getDrawingPane().repaint();
	}

	/**
	 * @return The model for the view.
	 */
	private BinaryTree<DendrogramPoint> viewModel() {
		mapFromKeysToIndices.clear();
		int i = 0;
		for (final DataRow row : getDataProvider().getDataArray(1)) {
			mapFromKeysToIndices.put(row.getKey(), Integer.valueOf(i++));
		}
		final BinaryTree<DendrogramPoint> ret = rootNode == null ? null
				: new BinaryTree<DendrogramPoint>(createViewModelFor(rootNode));
		return ret;
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
		final BinaryTree<DendrogramPoint> binaryTree = viewModel();
		((DendrogramDrawingPane) getDrawingPane()).setRootNode(binaryTree);
		((HeatmapDendrogramDrawingPane) getDrawingPane())
				.setHeatmapCellHeight((int) getYAxis().getCoordinate()
						.getUnusedDistBetweenTicks(
								getDrawingPaneDimension().height));
		getDrawingPane().repaint();
		getXAxis().repaint();
		getYAxis().repaint();
	}

	private void getRowKeys(final DendrogramNode node, final Set<RowKey> ids) {
		if (node == null) {
			return;
		}
		if (node.isLeaf()) {
			final RowKey key = getKey(node);
			ids.add(key);
			return;
		}
		getRowKeys(node.getFirstSubnode(), ids);
		getRowKeys(node.getSecondSubnode(), ids);

	}

	@Deprecated
	private RowKey getKey(final DendrogramNode node) {
		if (node.getLeafDataPoint() != null) {
			return node.getLeafDataPoint().getKey();
		}
		RowKey key;
		try {
			final Method getKey = node.getClass().getMethod("getLeafRowKey");
			key = (RowKey) getKey.invoke(node);
		} catch (final RuntimeException e) {
			throw e;
		} catch (final Exception e) {
			throw new RuntimeException(e);
		}
		return key;
	}

	/** {@inheritDoc} */
	@Override
	public void setRootNode(final DendrogramNode root) {
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
		final HeatmapDendrogramDrawingPane dp = (HeatmapDendrogramDrawingPane) getDrawingPane();
		final int offset = dp.getMaxStringLength()
				+ dp.getVisibleColumns().size() * dp.getCellWidth();
		final int width = dim.width - offset;
		int x = (int) getXAxis().getCoordinate().calculateMappedValue(
				new DoubleCell(node.getDist()), width);

		x += directionLeftToRight ? offset : -offset;
		int y;
		DendrogramPoint p;
		if (!node.isLeaf()) {
			y = dim.height - getYPosition(node);
			p = new DendrogramPoint(new Point(x, y), node.getDist());
			p.setRelativeSize(1.0);
		} else {
			// final DataRow row = node.getLeafDataPoint();
			final RowKey key = getKey(node);
			final DataArray table = getDataProvider().getDataArray(1);
			final Integer rowIndex = mapFromKeysToIndices.get(key);
			if (rowIndex == null) {
				throw new IllegalStateException("Not found the " + key
						+ " key in the second table's rows.");
			}
			final DataRow row = table.getRow(rowIndex.intValue());
			y = dim.height
					- (int) getYAxis().getCoordinate().calculateMappedValue(
							new StringCell(getKey(node)
							/* row.getKey() */.getString()), dim.height);
			p = new DendrogramPoint(new Point(x, y), node.getDist());
			final DataTableSpec spec = table.getDataTableSpec();
			final ColorAttr rowColor = spec.getRowColor(row);
			p.setColor(rowColor == null ? ColorAttr.DEFAULT : rowColor);
			final Shape rowShape = spec.getRowShape(row);
			p.setShape(rowShape == null ? ShapeFactory
					.getShape(ShapeFactory.DEFAULT) : rowShape);
			p.setRelativeSize(spec.getRowSizeFactor(row));
			p.setHilite(delegateIsHiLit(getKey(node)
			/* row.getKey() */));
		}
		viewNode = new BinaryTreeNode<DendrogramPoint>(p);
		final Set<RowKey> keys = new LinkedHashSet<RowKey>();
		getRowKeys(node, keys);
		viewNode.getContent().addRows(keys);
		viewNode.getContent().setSelected(
				selected.contains(viewNode.getContent()));
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
			final DataCell value = new StringCell(getKey(node).getString());
			final int calculateMappedValue = (int) getYAxis().getCoordinate()
					.calculateMappedValue(value,
							getDrawingPaneDimension().height);
			return calculateMappedValue;
			// return directionUpToDown ? calculateMappedValue
			// : getDrawingPaneDimension().height - calculateMappedValue;
		}
		return (getYPosition(node.getFirstSubnode()) + getYPosition(node
				.getSecondSubnode())) / 2;
	}

	@Override
	public void selectElementsIn(final Rectangle selectionRectangle) {
		for (final BinaryTreeNode<DendrogramPoint> node : tree
				.getNodes(BinaryTree.Traversal.IN)) {
			if (selectionRectangle.contains(node.getContent().getPoint())) {
				selected.add(node.getContent());
				selectElementsRecursively(node);
			}
		}
		updatePaintModel();
	}

	private void selectElementsRecursively(
			final BinaryTreeNode<DendrogramPoint> node) {
		if (node.isLeaf()) {
			return;
		}
		selected.add(node.getLeftChild().getContent());
		selectElementsRecursively(node.getLeftChild());
		selected.add(node.getRightChild().getContent());
		selectElementsRecursively(node.getRightChild());
	}

	@Override
	public void clearSelection() {
		selected.clear();
	}

	@Override
	public void hiLiteSelected() {
		for (final DendrogramPoint p : selected) {
			delegateHiLite(p.getRows());
		}
		updatePaintModel();
	}

	@Override
	public void unHiLiteSelected() {
		for (final DendrogramPoint p : selected) {
			delegateUnHiLite(p.getRows());
		}
		updatePaintModel();
	}

	@Override
	public void dispose() {
		try {
			super.dispose();
		} catch (final ClassCastException e) {
			// No (serious) problem
		}
	}

	@Override
	public Action getFadeAction() {
		return new AbstractAction(AbstractPlotter.FADE_UNHILITED) {
			private static final long serialVersionUID = 52543500465537535L;

			@Override
			public void actionPerformed(final ActionEvent e) {
				((HeatmapDendrogramDrawingPane) getDrawingPane())
						.setHilite(HiliteType.FadeUnHilit);
				getDrawingPane().repaint();
			}
		};
	}

	@Override
	public Action getShowAllAction() {
		return new AbstractAction(AbstractPlotter.SHOW_ALL) {
			private static final long serialVersionUID = -3628234319863271438L;

			@Override
			public void actionPerformed(final ActionEvent e) {
				((HeatmapDendrogramDrawingPane) getDrawingPane())
						.setHilite(HiliteType.Normal);
				getDrawingPane().repaint();
			}
		};
	}

	@Override
	public Action getHideAction() {
		return new AbstractAction(AbstractPlotter.HIDE_UNHILITED) {
			private static final long serialVersionUID = -9125403146492421287L;

			@Override
			public void actionPerformed(final ActionEvent e) {
				((HeatmapDendrogramDrawingPane) getDrawingPane())
						.setHilite(HiliteType.HideUnHilit);
				getDrawingPane().repaint();
			}
		};
	}
}

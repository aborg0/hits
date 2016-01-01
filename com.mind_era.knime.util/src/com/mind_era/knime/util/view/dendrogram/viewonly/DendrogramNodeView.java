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

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JRadioButtonMenuItem;

import org.knime.base.node.mine.cluster.hierarchical.HierarchicalClusterNodeView;
import org.knime.base.node.viz.plotter.dendrogram.DendrogramPlotter;
import org.knime.base.node.viz.plotter.node.DefaultVisualizationNodeView;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.node.NodeModel;

import com.mind_era.knime.common.util.swing.ImageType;
import com.mind_era.knime.common.util.swing.SaveAs;
import com.mind_era.knime.common.util.swing.colour.ComplexModel;
import com.mind_era.knime.common.view.heatmap.ExportLegendAction;

/**
 * A {@link HierarchicalClusterNodeView} with a ability to save the main content
 * as a PNG file.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
@Nonnull
@CheckReturnValue
public class DendrogramNodeView extends DefaultVisualizationNodeView {

	private static final String DATA_MENU = "Data";

	private static enum DataOrder {
		/** no selection, only from second */
		OnlySecond("Columns from second port (no indication of first)"),
		/** no selection, only from first */
		OnlyFirst("Columns only from first port (present in second too)", true),
		/** first in front, commons are selected */
		BothButFirstBefore(
				"Columns in first in front (all of the columns from second port)"),
		/** second in front, commons are selected */
		BothButSecondBefore(
				"Columns in second at back (all of the columns from second port)"),
		/**
		 * all of them in the order of second, only from second (commons
		 * selected)
		 */
		BothInOrderOfSecond(
				"Columns from second port (selecting the columns present in first)");
		private final String name;
		private final boolean selected;

		private DataOrder(final String name, final boolean initiallySelected) {
			this.name = name;
			this.selected = initiallySelected;
		}

		private DataOrder(final String name) {
			this(name, false);
		}

		/**
		 * @return the name
		 */
		public String getName() {
			return name;
		}

		/**
		 * @return the selected
		 */
		public boolean isSelected() {
			return selected;
		}
	}

	private class SelectData extends AbstractAction {
		private static final long serialVersionUID = -3563827330967746913L;
		private final HeatmapDendrogramPlotter plotter;
		private final DataOrder order;

		public SelectData(final String string,
				final HeatmapDendrogramPlotter plotter, final DataOrder order) {
			super(string);
			this.plotter = plotter;
			this.order = order;
		}

		@Override
		public void actionPerformed(final ActionEvent e) {
			if (((AbstractButton) e.getSource()).isSelected()
					&& getNodeModel().getOrigData() != null) {
				final List<String> visibleColumns = new ArrayList<String>();
				final List<String> selectedColumns = new ArrayList<String>();
				final DendrogramNodeModel nodeModel = getNodeModel();
				final DataTableSpec origSpec = nodeModel.getOrigData()
						.getDataTableSpec();
				final List<String> origCols = new ArrayList<String>();
				for (final DataColumnSpec spec : origSpec) {
					if (spec.getType().isCompatible(DoubleValue.class)) {
						origCols.add(spec.getName());
					}
				}
				final HeatmapDendrogramDrawingPane dp = (HeatmapDendrogramDrawingPane) plotter
						.getDrawingPane();
				switch (order) {
				case BothInOrderOfSecond:
					visibleColumns.addAll(origCols);
					selectedColumns.addAll(nodeModel.getSelectedColumns());
					break;
				case BothButFirstBefore:
				{
					visibleColumns.addAll(origCols);
					visibleColumns.removeAll(nodeModel.getSelectedColumns());
					final List<String> visible = nodeModel.getSelectedColumns().stream().filter(c -> origCols.contains(c))
							.collect(Collectors.toList());
					visibleColumns.addAll(0, visible);
					selectedColumns.addAll(visible);
					break;
				}
				case BothButSecondBefore:
				{
					visibleColumns.addAll(origCols);
					visibleColumns.removeAll(nodeModel.getSelectedColumns());
					final List<String> visible = nodeModel.getSelectedColumns().stream().filter(c -> origCols.contains(c))
							.collect(Collectors.toList());
					visibleColumns.addAll(visible);
					selectedColumns.addAll(visible);
					break;
				}
				case OnlyFirst:
					visibleColumns.addAll(nodeModel.getSelectedColumns());
					if (!origCols.containsAll(visibleColumns)) {
						visibleColumns.retainAll(origCols);
					}
					break;
				case OnlySecond:
					visibleColumns.addAll(origCols);
					break;
				default:
					throw new UnsupportedOperationException(order.toString());
				}
				dp.setVisibleColumns(visibleColumns);
				dp.setSelectedColumns(selectedColumns);
				plotter.updatePaintModel();
			}
		}
	}

	/**
	 * Adds a menu to the original view.
	 * 
	 * @param nodeModel
	 *            A {@link NodeModel}.
	 * @param heatmapDendrogramPlotter
	 *            A {@link DendrogramPlotter}.
	 */
	public DendrogramNodeView(final NodeModel nodeModel,
			final HeatmapDendrogramPlotter heatmapDendrogramPlotter) {
		super(nodeModel, heatmapDendrogramPlotter);
		final JMenu file = getJMenuBar().getMenu(0);
		final JMenuItem exportPNG = new JMenuItem(SaveAs.createAction(this
				.getComponent(), "Export view as PNG", heatmapDendrogramPlotter
				.getDrawingPane(), ImageType.png));
		file.add(exportPNG);
		final JMenuItem exportSVG = new JMenuItem(SaveAs.createAction(this
				.getComponent(), "Export view as SVG", heatmapDendrogramPlotter
				.getDrawingPane(), ImageType.svg));
		file.add(exportSVG);
		file.add(new ExportLegendAction<ComplexModel>(
				((HeatmapDendrogramPlotterProperties) heatmapDendrogramPlotter
						.getProperties()).getColourSelector(), ImageType.png));
		final JMenu dataMenu = new JMenu(DATA_MENU);
		final ButtonGroup group = new ButtonGroup();
		for (final DataOrder order : DataOrder.values()) {
			final JRadioButtonMenuItem menuItem = new JRadioButtonMenuItem(
					order.getName(), order.isSelected());
			menuItem.setAction(new SelectData(order.getName(),
					heatmapDendrogramPlotter, order));
			group.add(menuItem);
			dataMenu.add(menuItem);
		}
		getJMenuBar().add(dataMenu);
	}

	@Override
	protected void modelChanged() {
		super.modelChanged();
		((HeatmapDendrogramPlotter) getComponent()).setRootNode(getNodeModel()
				.getRoot());
		final JMenuBar menuBar = getJMenuBar();
		for (int i = menuBar.getMenuCount(); i-- > 0;) {
			final JMenu menu = menuBar.getMenu(i);
			if (DATA_MENU.equals(menu.getText())) {
				for (final Component c : menu.getMenuComponents()) {
					if (c instanceof AbstractButton) {
						final AbstractButton button = (AbstractButton) c;
						if (button.isSelected()) {
							button.getAction().actionPerformed(
									new ActionEvent(button, 0, null));
						}
					}
				}
			}
		}
	}

	@Override
	protected DendrogramNodeModel getNodeModel() {
		return (DendrogramNodeModel) super.getNodeModel();
	}
}

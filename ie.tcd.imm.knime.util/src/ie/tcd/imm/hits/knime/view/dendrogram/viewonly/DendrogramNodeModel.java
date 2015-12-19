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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import org.knime.base.node.mine.cluster.hierarchical.ClusterTreeModel;
import org.knime.base.node.mine.cluster.hierarchical.view.ClusterViewNode;
import org.knime.base.node.util.DataArray;
import org.knime.base.node.util.DefaultDataArray;
import org.knime.base.node.viz.plotter.DataProvider;
import org.knime.base.node.viz.plotter.dendrogram.DendrogramNode;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.container.ContainerTable;
import org.knime.core.data.container.DataContainer;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.PortUtil;

import ie.tcd.imm.hits.knime.util.SimpleModelBuilder;
import ie.tcd.imm.hits.knime.view.StatTypes;
import ie.tcd.imm.hits.util.swing.colour.ColourSelector.RangeType;

/**
 * This is the model implementation of Dendrogram. Allows to create dendrogram
 * with a heatmap of parameters.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
public class DendrogramNodeModel extends NodeModel implements DataProvider {
	/** The logger. */
	static final NodeLogger logger = NodeLogger
			.getLogger(DendrogramNodeModel.class);

	/** Reset should clear it. */
	private final Map<String, Integer> mapOfKeys = new HashMap<String, Integer>();

	private final Map<String, Map<StatTypes, Map<RangeType, Double>>> ranges = new HashMap<String, Map<StatTypes, Map<RangeType, Double>>>();

	/** The selected columns. */
	private final List<String> selectedColumns = new ArrayList<String>();

//	static final String CFGKEY_EXPORT_IMAGE = "export.image";
//	static final ImageExportOption DEFAULT_EXPORT_IMAGE = ImageExportOption.None;
//
//	private SettingsModelEnum<ImageExportOption> exportImage = new SettingsModelEnum<ImageExportOption>(
//			CFGKEY_EXPORT_IMAGE, DEFAULT_EXPORT_IMAGE,
//			ImageExportOption.values());

	private @Nullable
	DataArray origData;

	private @Nullable
	ClusterViewNode root;

	/**
	 * Constructor for the node model.
	 */
	protected DendrogramNodeModel() {
		super(new PortType[] { ClusterTreeModel.TYPE, BufferedDataTable.TYPE },
				new PortType[] { /*ImagePortObject.TYPE*/ });
	}

	@Override
	protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs)
			throws InvalidSettingsException {
//		switch (exportImage.getEnumValue()) {
//		case None:
//		case Svg:
			return new PortObjectSpec[] { /*new ImagePortObjectSpec(SvgCell.TYPE)*/ };
//		case Png:
//			return new PortObjectSpec[] { /*new ImagePortObjectSpec(
//					PNGImageContent.TYPE)*/ };
//		default:
//			throw new UnsupportedOperationException(
//					"Not supported image export format: "
//							+ exportImage.getEnumValue());
//		}
	}

	@Override
	protected PortObject[] execute(final PortObject[] data,
			final ExecutionContext exec) throws Exception {
		selectedColumns.clear();
		setTreeModel((ClusterTreeModel) data[0]);
//		long rowCount;
		origData = new DefaultDataArray((BufferedDataTable) data[1], 1,
				(int)(/*rowCount =*/ ((BufferedDataTable) data[1]).size()));
		mapOfKeys.clear();
		int i = 0;
		for (final DataRow row : origData/* data[0] */) {
			mapOfKeys.put(row.getKey().getString(), Integer.valueOf(i++));
		}
		fillStats((BufferedDataTable) data[1]);
//		final HeatmapDendrogramDrawingPane view;
//		if (exportImage.getEnumValue() == ImageExportOption.None) {
//			view = null;
//		} else {
//			view = new HeatmapDendrogramDrawingPane();
//			final HeatmapDendrogramPlotterProperties props = new HeatmapDendrogramPlotterProperties();
//			props.getCellWidth().setValue(20);
//			props.getShowValues().getModel().setSelected(false);
//			final HeatmapDendrogramPlotter plotter = new HeatmapDendrogramPlotter(
//					view, props);
//			plotter.setDataProvider(this);
//			plotter.setHiLiteHandler(getInHiLiteHandler(1));
//			plotter.setRootNode(root);
//
//			view.setColourModel(new ColourModel());
//			view.setHeatmapCellHeight(17);
//			view.setShowValues(false);
//			// view.setRootNode(plotter.viewModel());
//			view.setNodeModel(this);
//			// final DendrogramNodeView nodeView = new DendrogramNodeView(this,
//			// plotter);
//			// nodeView.modelChanged();
//			// YODO move to DendrogramNodeView, as SelectData is available only
//			// there.
//			final Dimension newSize = new Dimension(1000, (int) (2 * rowCount));
//			plotter.setSize(view.getPreferredSize());
//			view.setPreferredSize(newSize);
//			plotter.updateSize();
//			view.setCellWidth(20);
//		}
//		switch (exportImage.getEnumValue()) {
//		case None:
//			return new PortObject[] { new ImagePortObject(
//					ImageExportOption.Empty, new ImagePortObjectSpec(
//							SvgCell.TYPE)) };
//		case Png:
//			return new PortObject[] { new ImagePortObject(
//					ImageExportOption.Png.paint(view), new ImagePortObjectSpec(
//							PNGImageContent.TYPE)) };
//		case Svg:
//			return new PortObject[] { new ImagePortObject(
//					ImageExportOption.Svg.paint(view), new ImagePortObjectSpec(
//							SvgCell.TYPE)) };
//		}
		return new PortObject[] { /*new ImagePortObject(null,
				new ImagePortObjectSpec(SvgCell.TYPE))*/ };
	}

	private void setTreeModel(final ClusterTreeModel data) {
		clusterTreeModel = data;
		final DataTableSpec spec = (DataTableSpec) clusterTreeModel.getSpec();
		selectedColumns.clear();
		for (final DataColumnSpec colSpec : spec) {
			if (colSpec.getType().isASuperTypeOf(colSpec.getType())) {
				selectedColumns.add(colSpec.getName());
			}
		}
		root = clusterTreeModel.getRoot();
	}

	/**
	 * @param table
	 *            A table with the original values.
	 */
	private void fillStats(final DataTable table) {
		final Map<String, Map<StatTypes, List<Double>>> vals = new HashMap<String, Map<StatTypes, List<Double>>>();
		ranges.clear();
		final List<String> columns = getColumns();
		final int[] colIndices = new int[columns.size()];
		final String[] colNames = new String[columns.size()];
		{
			int i = 0;
			for (final String colName : columns) {
				colNames[i] = colName;
				colIndices[i++] = table.getDataTableSpec().findColumnIndex(
						colName);
				final EnumMap<StatTypes, List<Double>> valMap = new EnumMap<StatTypes, List<Double>>(
						StatTypes.class);
				valMap.put(StatTypes.raw, new ArrayList<Double>());
				vals.put(colName, valMap);
				final EnumMap<StatTypes, Map<RangeType, Double>> retMaps = new EnumMap<StatTypes, Map<RangeType, Double>>(
						StatTypes.class);
				ranges.put(colName, retMaps);
				retMaps.put(StatTypes.raw, new EnumMap<RangeType, Double>(
						RangeType.class));
			}
		}
		for (final DataRow row : table) {
			for (int i = 0; i < colIndices.length; ++i) {
				final DataCell cell = row.getCell(colIndices[i]);
				if (cell instanceof DoubleValue) {
					final DoubleValue val = (DoubleValue) cell;
					vals.get(colNames[i]).get(StatTypes.raw)
							.add(Double.valueOf(val.getDoubleValue()));
				}
			}
		}
		SimpleModelBuilder.computeStatistics(ranges, vals);
	}

	/**
	 * @return The selected columns.
	 */
	public List<String> getSelectedColumns() {
		return selectedColumns == null ? Collections.<String> emptyList()
				: Collections.unmodifiableList(selectedColumns);
	}

	@Override
	protected void validateSettings(final NodeSettingsRO settings)
			throws InvalidSettingsException {
		// For compatibility reasons exportImage is not checked.
	}

	@Override
	protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
			throws InvalidSettingsException {
//		try {
//			exportImage.loadSettingsFrom(settings);
//		} catch (final InvalidSettingsException e) {
//			exportImage.setStringValue(DEFAULT_EXPORT_IMAGE.getDisplayText());
//		}
	}

	@Override
	protected void saveSettingsTo(final NodeSettingsWO settings) {
//		exportImage.saveSettingsTo(settings);
	}

	/**
	 * @return The map of the keys to positions ({@code 0}-based); unmodifiable.
	 */
	public Map<String, Integer> getMap() {
		return Collections.unmodifiableMap(mapOfKeys);
	}

	@Override
	protected void loadInternals(final File nodeInternDir,
			final ExecutionMonitor exec) throws IOException,
			CanceledExecutionException {
		final ContainerTable data = DataContainer.readFromZip(new File(
				nodeInternDir, DendrogramNodeModel.CFG_H_CLUST_DATA));
		origData = new DefaultDataArray(data, 1, (int) data.size());
		int i = 0;
		for (final DataRow dataRow : origData) {
			mapOfKeys.put(dataRow.getKey().getString(), Integer.valueOf(i++));
		}
		fillStats(origData);
		final PortObject portObject = PortUtil.readObjectFromFile(new File(
				nodeInternDir, CFG_H_TREE_DATA), exec);
		if (portObject instanceof ClusterTreeModel) {
			setTreeModel(clusterTreeModel = (ClusterTreeModel) portObject);
		}
	}

	private static final String CFG_H_CLUST_DATA = "hClustData";

	private static final String CFG_H_TREE_DATA = "hTreeData";

	private @Nullable ClusterTreeModel clusterTreeModel;

	@Override
	protected void saveInternals(final File nodeInternDir,
			final ExecutionMonitor exec) throws IOException,
			CanceledExecutionException {
		if (origData != null) {
			final File dataFile = new File(nodeInternDir,
					DendrogramNodeModel.CFG_H_CLUST_DATA);
			DataContainer.writeToZip(origData, dataFile, exec);
			PortUtil.writeObjectToFile(clusterTreeModel, new File(
					nodeInternDir, CFG_H_TREE_DATA), exec);
		}
	}

	/**
	 * @return The actual ranges.
	 */
	Map<String, Map<StatTypes, Map<RangeType, Double>>> getRange() {
		return ranges;
	}

	@Override
	protected void reset() {
		mapOfKeys.clear();
		ranges.clear();
		root = null;
		origData = null;
		selectedColumns.clear();
	}

	/**
	 * @return The original data table.
	 */
	public @Nullable
	DataArray getOrigData() {
		return origData;
	}

	/**
	 * @return The root {@link DendrogramNode}.
	 */
	public @Nullable
	DendrogramNode getRoot() {
		return root;
	}

	@Override
	public @Nullable
	DataArray getDataArray(final int index) {
		return origData;
	}

	/**
	 * @return The compatible columns from the second port.
	 */
	public List<String> getColumns() {
		if (origData == null) {
			return Collections.<String> emptyList();
		}
		final List<String> ret = new ArrayList<String>();
		for (final DataColumnSpec spec : getOrigData().getDataTableSpec()) {
			if (DoubleCell.TYPE.isASuperTypeOf(spec.getType())) {
				ret.add(spec.getName());
			}
		}
		return ret;
	}
}

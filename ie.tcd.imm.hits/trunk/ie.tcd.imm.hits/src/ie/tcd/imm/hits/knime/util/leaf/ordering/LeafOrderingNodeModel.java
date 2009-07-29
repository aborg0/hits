/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package ie.tcd.imm.hits.knime.util.leaf.ordering;

import ie.tcd.imm.hits.util.Triple;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.knime.base.node.mine.cluster.hierarchical.ClusterTreeModel;
import org.knime.base.node.mine.cluster.hierarchical.view.ClusterViewNode;
import org.knime.base.node.viz.plotter.dendrogram.DendrogramNode;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowKey;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelColumnName;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.distmatrix.type.DistanceVectorDataValue;

/**
 * This is the model implementation of LeafOrdering. Reorders a tree to an
 * optimal ordering. See <tt>New Hierarchical Clustering</tt> node.
 * <p>
 * See also: <a
 * href="http://bioinformatics.oxfordjournals.org/cgi/reprint/19/9/1070.pdf"
 * >K-ary clustering with optimal leaf ordering for gene expression data</a>
 * from <em>Ziv Bar-Joseph</em>, <em>Erik D. Demaine</em>,
 * <em>David K. Gifford</em>, <em>Nathan Srebro</em>, <em>Angele M. Hamel</em>
 * and <em>Tommi S. Jaakkola</em>.
 * 
 * @author <a href="bakosg@tcd.ie">Gabor Bakos</a>
 */
@SuppressWarnings("restriction")
public class LeafOrderingNodeModel extends NodeModel {

	/** Configuration key for distance column. */
	static final String CFGKEY_DISTANCE_COLUMN = "distance.column";
	/** Default value for distance column. */
	static final String DEFAULT_DISTANCE_COLUMN = "Distance";

	private SettingsModelColumnName distanceColumnModel = new SettingsModelColumnName(
			CFGKEY_DISTANCE_COLUMN, DEFAULT_DISTANCE_COLUMN);

	/**
	 * Constructor for the node model.
	 */
	protected LeafOrderingNodeModel() {
		super(new PortType[] { ClusterTreeModel.TYPE, BufferedDataTable.TYPE },
				new PortType[] { ClusterTreeModel.TYPE });
	}

	@Override
	protected PortObject[] execute(final PortObject[] inObjects,
			final ExecutionContext exec) throws Exception {
		final ClusterTreeModel model = (ClusterTreeModel) inObjects[0];
		final BufferedDataTable data = (BufferedDataTable) inObjects[1];
		final int distanceColumnIdx = data.getDataTableSpec().findColumnIndex(
				distanceColumnModel.getColumnName());
		final Map<RowKey, DistanceVectorDataValue> distanceMatrix = new HashMap<RowKey, DistanceVectorDataValue>();
		for (final DataRow dataRow : data) {
			final DistanceVectorDataValue distanceVector = (DistanceVectorDataValue) dataRow
					.getCell(distanceColumnIdx);
			distanceMatrix.put(dataRow.getKey(), distanceVector);
		}
		final ClusterViewNode origRoot = model.getRoot();
		final Map<Triple<DendrogramNode, RowKey, RowKey>, Number> m = visit(
				origRoot,
				new HashMap<Triple<DendrogramNode, RowKey, RowKey>, Number>(),
				distanceMatrix);
		// order using m
		return new PortObject[] { new ClusterTreeModel((DataTableSpec) model
				.getSpec(), origRoot, model.getClusterDistances(), model
				.getClusterDistances().length + 1) };
	}

	private Map<Triple<DendrogramNode, RowKey, RowKey>, Number> visit(
			final DendrogramNode root,
			final Map<Triple<DendrogramNode, RowKey, RowKey>, Number> m,
			final Map<RowKey, DistanceVectorDataValue> d) {
		if (root.isLeaf()) {
			final RowKey key = root.getLeafDataPoint().getKey();
			return Collections.singletonMap(Triple.apply(root, key, key),
					(Number) Double.valueOf(0));
		}
		final Map<Triple<DendrogramNode, RowKey, RowKey>, Number> leftM = visit(
				root.getFirstSubnode(), m, d);
		final Map<Triple<DendrogramNode, RowKey, RowKey>, Number> rightM = visit(
				root.getSecondSubnode(), m, d);
		final Map<Triple<DendrogramNode, RowKey, RowKey>, Number> ret = new HashMap<Triple<DendrogramNode, RowKey, RowKey>, Number>(
				leftM);
		ret.putAll(rightM);
		final Set<RowKey> leftKeys = computeLeaves(root.getFirstSubnode());
		final Set<RowKey> rightKeys = computeLeaves(root.getSecondSubnode());
		final Map<RowKey, Map<RowKey, Number>> t = computeT(root, m, d,
				leftKeys, rightKeys);
		for (final Entry<RowKey, Map<RowKey, Number>> entry : t.entrySet()) {
			for (final Entry<RowKey, Number> innerEntry : entry.getValue()
					.entrySet()) {
				double max = Double.NEGATIVE_INFINITY;
				for (final RowKey l : rightKeys) {
					final double alternative = entry.getValue().get(l)
							.doubleValue()
							+ rightM.get(
									Triple.apply(root.getSecondSubnode(), l,
											innerEntry.getKey())).doubleValue();
					if (alternative > max) {
						max = alternative;
					}
				}
				ret
						.put(Triple.apply(root, entry.getKey(), innerEntry
								.getKey()), Double.valueOf(max));
			}
		}
		return ret;
	}

	private Map<RowKey, Map<RowKey, Number>> computeT(
			final DendrogramNode root,
			final Map<Triple<DendrogramNode, RowKey, RowKey>, Number> m,
			final Map<RowKey, DistanceVectorDataValue> d,
			final Set<RowKey> leftKeys, final Set<RowKey> rightKeys) {
		final Map<RowKey, Map<RowKey, Number>> ret = new HashMap<RowKey, Map<RowKey, Number>>();
		for (final RowKey i : leftKeys) {
			for (final RowKey l : rightKeys) {
				Double maxValue = Double.NEGATIVE_INFINITY;
				for (final RowKey h : leftKeys) {
					final double similarity = -d.get(i).getDistance(d.get(h));
					final double alternative = similarity
							+ m.get(Triple.apply(root.getFirstSubnode(), i, h))
									.doubleValue();
					if (alternative > maxValue) {
						maxValue = alternative;
					}
				}
				if (!ret.containsKey(i)) {
					ret.put(i, new HashMap<RowKey, Number>());
				}
				ret.get(i).put(l, maxValue);
			}
		}
		return ret;
	}

	private Set<RowKey> computeLeaves(final DendrogramNode root) {
		final Set<RowKey> ret = new HashSet<RowKey>();
		if (!root.isLeaf()) {
			computeLeaves(root.getFirstSubnode(), ret);
			computeLeaves(root.getSecondSubnode(), ret);
		}
		return ret;
	}

	private void computeLeaves(final DendrogramNode root, final Set<RowKey> ret) {
		if (root.isLeaf()) {
			ret.add(root.getLeafDataPoint().getKey());
		} else {
			computeLeaves(root.getFirstSubnode(), ret);
			computeLeaves(root.getSecondSubnode(), ret);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void reset() {
		// No state to reset
	}

	@Override
	protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs)
			throws InvalidSettingsException {
		return new PortObjectSpec[] { inSpecs[0] };
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveSettingsTo(final NodeSettingsWO settings) {
		distanceColumnModel.saveSettingsTo(settings);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
			throws InvalidSettingsException {
		distanceColumnModel.loadSettingsFrom(settings);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void validateSettings(final NodeSettingsRO settings)
			throws InvalidSettingsException {
		distanceColumnModel.validateSettings(settings);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadInternals(final File internDir,
			final ExecutionMonitor exec) throws IOException,
			CanceledExecutionException {
		// No internal state
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveInternals(final File internDir,
			final ExecutionMonitor exec) throws IOException,
			CanceledExecutionException {
		// No internal state
	}

}

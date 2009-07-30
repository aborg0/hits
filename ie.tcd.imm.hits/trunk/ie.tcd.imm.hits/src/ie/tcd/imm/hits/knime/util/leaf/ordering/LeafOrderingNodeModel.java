/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package ie.tcd.imm.hits.knime.util.leaf.ordering;

import ie.tcd.imm.hits.util.Pair;
import ie.tcd.imm.hits.util.Triple;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
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
import org.knime.core.node.NodeLogger;
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
// @SuppressWarnings("restriction")
public class LeafOrderingNodeModel extends NodeModel {
	private static final NodeLogger logger = NodeLogger
			.getLogger(LeafOrderingNodeModel.class);

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
		final DendrogramNode origRoot = model.getRoot();
		final Map<Triple<DendrogramNode, RowKey, RowKey>, Number> m = visit(
				origRoot,
				new HashMap<Triple<DendrogramNode, RowKey, RowKey>, Number>(),
				distanceMatrix);
		final Map<RowKey, Pair<DataRow, Integer>> rows = new HashMap<RowKey, Pair<DataRow, Integer>>();
		int idx = 0;
		for (final DataRow dataRow : data) {
			rows.put(dataRow.getKey(), Pair.apply(dataRow, Integer
					.valueOf(idx++)));
		}
		final ClusterViewNode tree = buildNewTree(convertM(m), origRoot, rows)
				.getO1();
		final ArrayList<DistanceVectorDataValue> origList = new ArrayList<DistanceVectorDataValue>(
				model.getClusterDistances().length + 1), newList = new ArrayList<DistanceVectorDataValue>(
				model.getClusterDistances().length + 1);
		flatten(origRoot, origList, distanceMatrix);
		flatten(origRoot, newList, distanceMatrix);
		logger.info("Before: " + sumDistance(origList));
		logger.info("After: " + sumDistance(newList));
		return new PortObject[] { new ClusterTreeModel((DataTableSpec) model
				.getSpec(), tree, model.getClusterDistances(), model
				.getClusterDistances().length + 1) };
	}

	private Map<DendrogramNode, Map<Pair<RowKey, RowKey>, Number>> convertM(
			final Map<Triple<DendrogramNode, RowKey, RowKey>, Number> m) {
		final Map<DendrogramNode, Map<Pair<RowKey, RowKey>, Number>> ret = new HashMap<DendrogramNode, Map<Pair<RowKey, RowKey>, Number>>();
		for (final Entry<Triple<DendrogramNode, RowKey, RowKey>, Number> entry : m
				.entrySet()) {
			final Triple<DendrogramNode, RowKey, RowKey> entryKey = entry
					.getKey();
			final DendrogramNode key = entryKey.getO1();
			if (!ret.containsKey(key)) {
				ret.put(key, new HashMap<Pair<RowKey, RowKey>, Number>());
			}
			// ENH keep only the winner
			ret.get(key).put(Pair.apply(entryKey.getO2(), entryKey.getO3()),
					entry.getValue());
		}
		return ret;
	}

	private static void flatten(final DendrogramNode root,
			final List<DistanceVectorDataValue> ret,
			final Map<RowKey, DistanceVectorDataValue> d) {
		if (root.isLeaf()) {
			ret.add(d.get(getLeafKey(root)));
			return;
		}
		flatten(root.getFirstSubnode(), ret, d);
		flatten(root.getSecondSubnode(), ret, d);
	}

	private static double sumDistance(
			final List<DistanceVectorDataValue> distances) {

		final Iterator<DistanceVectorDataValue> it = distances.iterator();
		if (!it.hasNext()) {
			return Double.NaN;
		}
		double ret = 0.0;
		DistanceVectorDataValue last = it.next();
		while (it.hasNext()) {
			final DistanceVectorDataValue next = it.next();
			ret += last.getDistance(next);
			last = next;
		}
		return ret;
	}

	private static Triple<ClusterViewNode, RowKey, RowKey> buildNewTree(
			final Map<DendrogramNode, Map<Pair<RowKey, RowKey>, Number>> m,
			final DendrogramNode root,
			final Map<RowKey, Pair<DataRow, Integer>> rows) {
		if (root.isLeaf()) {
			final Pair<DataRow, Integer> leafRow = rows.get(getLeafKey(root));
			return Triple.apply(
					new ClusterViewNode(leafRow.getLeft().getKey()), leafRow
							.getLeft().getKey(), leafRow.getLeft().getKey());
		}
		final Triple<ClusterViewNode, RowKey, RowKey> firstTree = buildNewTree(
				m, root.getFirstSubnode(), rows);
		final Triple<ClusterViewNode, RowKey, RowKey> secondTree = buildNewTree(
				m, root.getSecondSubnode(), rows);
		final Map<Pair<RowKey, RowKey>, Number> map = m.get(root);
		Pair<RowKey, RowKey> pairNoChange = Pair.apply(firstTree.getO3(),
				secondTree.getO2());
		if (!map.containsKey(pairNoChange)) {
			pairNoChange = pairNoChange.flip();
		}
		Pair<RowKey, RowKey> pairChange = Pair.apply(secondTree.getO3(),
				firstTree.getO2());
		if (!map.containsKey(pairChange)) {
			pairChange = pairChange.flip();
		}
		assert map.containsKey(pairNoChange);
		assert map.containsKey(pairChange);
		if (map.get(pairNoChange).doubleValue() > map.get(pairChange)
				.doubleValue()) {
			return Triple.apply(new ClusterViewNode(firstTree.getO1(),
					secondTree.getO1(), root.getDist()), firstTree.getO2(),
					secondTree.getO3());
		}
		// assert map.containsKey(pairChange);
		return Triple.apply(new ClusterViewNode(secondTree.getO1(), firstTree
				.getO1(), root.getDist()), secondTree.getO2(), firstTree
				.getO3());
		// double max = Double.NEGATIVE_INFINITY;
		// Pair<RowKey, RowKey> pair = null;
		// for (final Entry<Pair<RowKey, RowKey>, Number> entry :
		// map.entrySet()) {
		// if (entry.getValue().doubleValue() > max) {
		// max = entry.getValue().doubleValue();
		// pair = entry.getKey();
		// }
		// }

		// if (secondTree.getO2().equals(pair.getRight())) {
		// assert firstTree.getO3().equals(pair.getLeft()) : "first: "
		// + firstTree.getO3() + "\npair: " + pair;
		// return Triple.apply((IClusterNode) new InnerNode(firstTree.getO1(),
		// secondTree.getO1(), root.getDist()), firstTree.getO2(),
		// secondTree.getO3());
		// }
		// if (secondTree.getO3().equals(pair.getLeft())) {
		// assert firstTree.getO2().equals(pair.getLeft()) : "first: "
		// + firstTree.getO2() + "\npair: " + pair;
		// return Triple.apply((IClusterNode) new InnerNode(
		// secondTree.getO1(), firstTree.getO1(), root.getDist()),
		// secondTree.getO2(), firstTree.getO3());
		// }
		// if (firstTree.getO3().equals(secondTree.getO2())) {
		// return Triple.apply((IClusterNode) new InnerNode(firstTree.getO1(),
		// secondTree.getO1(), root.getDist()), firstTree.getO2(),
		// secondTree.getO3());
		// }
		// if (firstTree.getO2().equals(secondTree.getO3())) {
		// return Triple.apply((IClusterNode) new InnerNode(
		// secondTree.getO1(), firstTree.getO1(), root.getDist()),
		// secondTree.getO2(), firstTree.getO3());
		// }
		// if (firstTree.getO2().equals(firstTree.getO3())) {
		// assert secondTree.getO2().equals(secondTree.getO3()) : secondTree;
		// return Triple.apply((IClusterNode) new InnerNode(
		// secondTree.getO1(), firstTree.getO1(), root.getDist()),
		// secondTree.getO2(), firstTree.getO3());
		// }
		// throw new IllegalStateException("First: " + firstTree.getO2() + ", "
		// + firstTree.getO3() + "\nSecond: " + secondTree.getO2() + ", "
		// + secondTree.getO3());
	}

	private Map<Triple<DendrogramNode, RowKey, RowKey>, Number> visit(
			final DendrogramNode root,
			final Map<Triple<DendrogramNode, RowKey, RowKey>, Number> m,
			final Map<RowKey, DistanceVectorDataValue> d) {
		if (root.isLeaf()) {
			final RowKey key = getLeafKey(root);
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
		final Map<RowKey, Map<RowKey, Number>> t = computeT(root
				.getFirstSubnode(), ret, d, leftKeys, rightKeys);
		for (final Entry<RowKey, Map<RowKey, Number>> entry : t.entrySet()) {
			for (final Entry<RowKey, Number> innerEntry : entry.getValue()
					.entrySet()) {
				double max = Double.NEGATIVE_INFINITY;
				for (final RowKey l : rightKeys) {
					final Triple<DendrogramNode, RowKey, RowKey> key = Triple
							.apply(root.getSecondSubnode(), l, innerEntry
									.getKey());
					if (!rightM.containsKey(key)) {
						continue;
					}
					final double alternative = entry.getValue().get(l)
							.doubleValue()
							+ rightM.get(key).doubleValue();
					if (alternative > max) {
						max = alternative;
					}
				}
				ret
						.put(Triple.apply(root, entry.getKey(), innerEntry
								.getKey()), Double.valueOf(max));
				// ret
				// .put(Triple.apply(root, innerEntry.getKey(), entry
				// .getKey()), Double.valueOf(max));
			}
		}
		if (leftKeys.size() == 1 && rightKeys.size() == 1) {
			final RowKey right = rightKeys.iterator().next();
			final RowKey left = leftKeys.iterator().next();
			final double similarity = d.get(right).getDistance(d.get(left));
			ret
					.put(Triple.apply(root, right, left), Double
							.valueOf(similarity));
		}
		return ret;
	}

	@Deprecated
	private static RowKey getLeafKey(final DendrogramNode node) {
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
					final Triple<DendrogramNode, RowKey, RowKey> key = Triple
							.apply(root, i, h);
					if (!m.containsKey(key)) {
						continue;
					}
					final double similarity = d.get(i).getDistance(d.get(h));
					final double alternative = root.isLeaf() || i.equals(h) ? 0.0
							: similarity + m.get(key).doubleValue();
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
		computeLeaves(root, ret);
		return ret;
	}

	private void computeLeaves(final DendrogramNode root, final Set<RowKey> ret) {
		if (root.isLeaf()) {
			ret.add(getLeafKey(root));
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
		if (!((DataTableSpec) inSpecs[1])
				.containsCompatibleType(DistanceVectorDataValue.class)) {
			throw new InvalidSettingsException(
					"No distance column present. Check Distance Matrix Calculate node.");
		}
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

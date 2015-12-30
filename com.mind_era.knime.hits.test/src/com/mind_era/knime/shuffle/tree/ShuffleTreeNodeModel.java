package com.mind_era.knime.shuffle.tree;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

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
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.distmatrix.type.DistanceVectorDataValue;

import com.mind_era.knime.util.leaf.ordering.LeafOrderingNodeModel;

/**
 * This is the model implementation of ShuffleTree. Randomly change the order of
 * branches in a tree.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
public class ShuffleTreeNodeModel extends NodeModel {

	private static final NodeLogger logger = NodeLogger
			.getLogger(ShuffleTreeNodeModel.class);
	private Random random;

	/**
	 * Constructor for the node model.
	 */
	protected ShuffleTreeNodeModel() {
		super(new PortType[] { ClusterTreeModel.TYPE, BufferedDataTable.TYPE },
				new PortType[] { ClusterTreeModel.TYPE });
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected PortObject[] execute(final PortObject[] inData,
			final ExecutionContext exec) throws Exception {
		random = new Random();
		final ClusterTreeModel orig = (ClusterTreeModel) inData[0];
		final BufferedDataTable data = (BufferedDataTable) inData[1];
		final int distanceColumnIdx = data.getDataTableSpec().findColumnIndex(
				"Distance");
		final Map<RowKey, DistanceVectorDataValue> distanceMatrix = new HashMap<RowKey, DistanceVectorDataValue>();
		for (final DataRow dataRow : data) {
			final DistanceVectorDataValue distanceVector = (DistanceVectorDataValue) dataRow
					.getCell(distanceColumnIdx);
			distanceMatrix.put(dataRow.getKey(), distanceVector);
		}
		final ClusterViewNode newRoot = shuffle(orig.getRoot());
		final ArrayList<DistanceVectorDataValue> origList = new ArrayList<DistanceVectorDataValue>(
				orig.getClusterDistances().length + 1), newList = new ArrayList<DistanceVectorDataValue>(
				orig.getClusterDistances().length + 1);
		LeafOrderingNodeModel.flatten(orig.getRoot(), origList, distanceMatrix);
		LeafOrderingNodeModel.flatten(newRoot, newList, distanceMatrix);
		logger.info("Before: " + LeafOrderingNodeModel.sumDistance(origList));
		logger.info("After: " + LeafOrderingNodeModel.sumDistance(newList));
		final ClusterTreeModel ret = new ClusterTreeModel((DataTableSpec) orig
				.getSpec(), newRoot, orig.getClusterDistances(), orig
				.getClusterDistances().length + 1) {
			@Override
			public String getSummary() {
				return "Before: " + LeafOrderingNodeModel.sumDistance(origList)
						+ "\nAfter:  "
						+ LeafOrderingNodeModel.sumDistance(newList);
			}
		};
		return new PortObject[] { ret };
	}

	private ClusterViewNode shuffle(final DendrogramNode root) {
		return root.isLeaf() ? (ClusterViewNode) root
				: random.nextBoolean() ? new ClusterViewNode(shuffle(root
						.getSecondSubnode()), shuffle(root.getFirstSubnode()),
						root.getDist()) : new ClusterViewNode(shuffle(root
						.getFirstSubnode()), shuffle(root.getSecondSubnode()),
						root.getDist());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void reset() {
		// TODO: generated method stub
	}

	/**
	 * {@inheritDoc}
	 */
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
		// TODO: generated method stub
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
			throws InvalidSettingsException {
		// TODO: generated method stub
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void validateSettings(final NodeSettingsRO settings)
			throws InvalidSettingsException {
		// TODO: generated method stub
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadInternals(final File internDir,
			final ExecutionMonitor exec) throws IOException,
			CanceledExecutionException {
		// TODO: generated method stub
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveInternals(final File internDir,
			final ExecutionMonitor exec) throws IOException,
			CanceledExecutionException {
		// TODO: generated method stub
	}

}

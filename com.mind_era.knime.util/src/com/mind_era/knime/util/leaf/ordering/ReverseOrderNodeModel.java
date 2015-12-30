package com.mind_era.knime.util.leaf.ordering;

import java.io.File;
import java.io.IOException;

import org.knime.base.node.mine.cluster.hierarchical.ClusterTreeModel;
import org.knime.base.node.mine.cluster.hierarchical.view.ClusterViewNode;
import org.knime.base.node.viz.plotter.dendrogram.DendrogramNode;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;

/**
 * This is the model implementation of ReverseOrder. Flips every branch in the
 * tree to the opposite, so it will result a tree with the reverse order of
 * leaves.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
public class ReverseOrderNodeModel extends NodeModel {

	/**
	 * Constructor for the node model.
	 */
	protected ReverseOrderNodeModel() {
		super(new PortType[] { ClusterTreeModel.TYPE },
				new PortType[] { ClusterTreeModel.TYPE });
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected PortObject[] execute(final PortObject[] inData,
			final ExecutionContext exec) throws Exception {
		final ClusterTreeModel orig = (ClusterTreeModel) inData[0];
		final ClusterViewNode newRoot = reverse(orig.getRoot());
		final ClusterTreeModel ret = new ClusterTreeModel((DataTableSpec) orig
				.getSpec(), newRoot, orig.getClusterDistances(), orig
				.getClusterDistances().length + 1);
		return new PortObject[] { ret };
	}

	private ClusterViewNode reverse(final DendrogramNode root) {
		return root.isLeaf() ? (ClusterViewNode) root : new ClusterViewNode(
				reverse(root.getSecondSubnode()), reverse(root
						.getFirstSubnode()), root.getDist());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void reset() {
		// No state
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
		// No settings
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
			throws InvalidSettingsException {
		// No settings
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void validateSettings(final NodeSettingsRO settings)
			throws InvalidSettingsException {
		// No settings
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadInternals(final File internDir,
			final ExecutionMonitor exec) throws IOException,
			CanceledExecutionException {
		// No internal state.
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveInternals(final File internDir,
			final ExecutionMonitor exec) throws IOException,
			CanceledExecutionException {
		// No internal state.
	}
}

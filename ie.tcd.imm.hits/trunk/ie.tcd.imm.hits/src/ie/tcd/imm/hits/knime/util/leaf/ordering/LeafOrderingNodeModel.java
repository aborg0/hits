package ie.tcd.imm.hits.knime.util.leaf.ordering;

import java.util.Arrays;

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
public class LeafOrderingNodeModel extends NodeModel {

	/**
	 * Constructor for the node model.
	 */
	protected LeafOrderingNodeModel() {
		super(new PortType[] { ClusterTreeModel.TYPE },
				new PortType[] { ClusterTreeModel.TYPE });
	}

	@Override
	protected PortObject[] execute(final PortObject[] inObjects,
			final ExecutionContext exec) throws Exception {
		final ClusterTreeModel model = (ClusterTreeModel) inObjects[0];
		final ClusterViewNode origRoot = model.getRoot();
		// final ClusterViewNode root = new ClusterViewNode(
		// (ClusterViewNode) origRoot.getFirstSubnode(),
		// (ClusterViewNode) origRoot.getSecondSubnode(), origRoot
		// .getDist());
		System.out.println(Arrays.toString(model.getClusterDistances()));
		final int leafCount = countLeaves(origRoot);
		return new PortObject[] { new ClusterTreeModel((DataTableSpec) model
				.getSpec(), origRoot, model.getClusterDistances(), model
				.getClusterDistances().length + 1) };
	}

	/**
	 * @param root
	 *            A {@link DendrogramNode}.
	 * @return The leaves under {@code root}.
	 */
	private static int countLeaves(final DendrogramNode root) {
		return root.isLeaf() ? 1 : countLeaves(root.getFirstSubnode())
				+ countLeaves(root.getSecondSubnode());
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

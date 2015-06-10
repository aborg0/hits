package ie.tcd.imm.hits.knime.shuffle.tree;

import ie.tcd.imm.hits.knime.util.leaf.ordering.LeafOrderingNodeModel;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.knime.base.node.mine.cluster.hierarchical.ClusterTreeModel;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
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
import org.knime.distmatrix.type.DistanceVectorDataCell;
import org.knime.distmatrix.type.DistanceVectorDataValue;

/**
 * This is the model implementation of ComputeEntropy. This node computes the
 * sum distance through the leaves.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
public class ComputeEntropyNodeModel extends NodeModel {

	private static final DataTableSpec RESULT_SPEC = new DataTableSpec(
			"Distance", new DataColumnSpecCreator("distance", DoubleCell.TYPE)
					.createSpec());

	/**
	 * Constructor for the node model.
	 */
	protected ComputeEntropyNodeModel() {
		super(new PortType[] { ClusterTreeModel.TYPE, BufferedDataTable.TYPE },
				new PortType[] { BufferedDataTable.TYPE });
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected BufferedDataTable[] execute(final PortObject[] inData,
			final ExecutionContext exec) throws Exception {
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
		final ArrayList<DistanceVectorDataValue> origList = new ArrayList<DistanceVectorDataValue>(
				orig.getClusterDistances().length + 1);
		LeafOrderingNodeModel.flatten(orig.getRoot(), origList, distanceMatrix);
		final BufferedDataContainer container = exec
				.createDataContainer(RESULT_SPEC);
		container.addRowToTable(new DefaultRow("0", new DoubleCell(
				LeafOrderingNodeModel.sumDistance(origList))));
		container.close();
		final BufferedDataTable ret = container.getTable();
		return new BufferedDataTable[] { ret };
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
	protected DataTableSpec[] configure(final PortObjectSpec[] inSpecs)
			throws InvalidSettingsException {
		final DataColumnSpec columnSpec = ((DataTableSpec) inSpecs[1])
				.getColumnSpec("Distance");
		if (columnSpec == null
				|| !columnSpec.getType().equals(DistanceVectorDataCell.TYPE)) {
			throw new InvalidSettingsException("No proper \"Distance\" column!");
		}
		return new DataTableSpec[] { RESULT_SPEC };
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveSettingsTo(final NodeSettingsWO settings) {
		// No state
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
			throws InvalidSettingsException {
		// No state
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void validateSettings(final NodeSettingsRO settings)
			throws InvalidSettingsException {
		// No state
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadInternals(final File internDir,
			final ExecutionMonitor exec) throws IOException,
			CanceledExecutionException {
		// No state
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveInternals(final File internDir,
			final ExecutionMonitor exec) throws IOException,
			CanceledExecutionException {
		// No state
	}

}

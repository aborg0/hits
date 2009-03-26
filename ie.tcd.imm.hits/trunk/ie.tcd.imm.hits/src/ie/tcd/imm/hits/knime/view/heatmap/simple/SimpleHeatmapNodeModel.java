package ie.tcd.imm.hits.knime.view.heatmap.simple;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.knime.base.node.util.DataArray;
import org.knime.base.node.util.DefaultDataArray;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
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

/**
 * This is the model implementation of SimpleHeatmap. Shows a simple heatmap of
 * the data.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
public class SimpleHeatmapNodeModel extends NodeModel {

	// the logger instance
	private static final NodeLogger logger = NodeLogger
			.getLogger(SimpleHeatmapNodeModel.class);
	private DataArray table;

	/**
	 * Constructor for the node model.
	 */
	protected SimpleHeatmapNodeModel() {
		super(1, 0);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
			final ExecutionContext exec) throws Exception {
		table = new DefaultDataArray(inData[0], 1, inData[0].getRowCount());
		return new BufferedDataTable[] {};
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void reset() {
		table = null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
			throws InvalidSettingsException {
		return new DataTableSpec[] {};
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveSettingsTo(final NodeSettingsWO settings) {
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
			throws InvalidSettingsException {
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void validateSettings(final NodeSettingsRO settings)
			throws InvalidSettingsException {
	}

	private static final String TABLE = "table";

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadInternals(final File internDir,
			final ExecutionMonitor exec) throws IOException,
			CanceledExecutionException {
		final ContainerTable containerTable = DataContainer
				.readFromZip(new File(internDir, TABLE));
		table = new DefaultDataArray(containerTable, 1, containerTable
				.getRowCount());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveInternals(final File internDir,
			final ExecutionMonitor exec) throws IOException,
			CanceledExecutionException {
		DataContainer.writeToZip(table, new File(internDir, TABLE), exec);
	}

	protected DataArray getTable() {
		return table;
	}

	protected List<String> getColumns() {
		if (table == null) {
			return Collections.emptyList();
		}
		final List<String> ret = new ArrayList<String>();
		for (final DataColumnSpec spec : table.getDataTableSpec()) {
			if (DoubleCell.TYPE.isASuperTypeOf(spec.getType())) {
				ret.add(spec.getName());
			}
		}
		return ret;
	}
}

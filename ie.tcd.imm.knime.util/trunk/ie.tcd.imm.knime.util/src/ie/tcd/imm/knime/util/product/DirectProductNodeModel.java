package ie.tcd.imm.knime.util.product;

import java.util.ArrayList;
import java.util.List;

import java.io.File;
import java.io.IOException;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * This is the model implementation of DirectProduct. This node takes input
 * tables and creates a direct product of the rows.
 * <p>
 * For example, given:<br/>
 * A:<br/>
 * <table>
 * <th>
 * <td>Col1</td>
 * <td>Col2</td></th>
 * <tr>
 * <td>a</td>
 * <td>1</td>
 * </tr>
 * <tr>
 * <td>b</td>
 * <td>2</td>
 * </tr>
 * </table>
 * and B:<br/>
 * <table>
 * <th>
 * <td>Col3</td></th>
 * <tr>
 * <td>x</td>
 * </tr>
 * <tr>
 * <td>y</td>
 * </tr>
 * <tr>
 * <td>z</td>
 * </tr>
 * </table>
 * the result is:
 * <table>
 * <th>
 * <td>Col1</td></th>
 * <td>Col2</td></th>
 * <td>Col3</td></th>
 * <tr>
 * <td>a</td>
 * <td>1</td>
 * <td>x</td>
 * </tr>
 * <tr>
 * <td>b</td>
 * <td>2</td>
 * <td>x</td>
 * </tr>
 * <tr>
 * <td>a</td>
 * <td>1</td>
 * <td>y</td>
 * </tr>
 * <tr>
 * <td>b</td>
 * <td>2</td>
 * <td>y</td>
 * </tr>
 * <tr>
 * <td>a</td>
 * <td>1</td>
 * <td>z</td>
 * </tr>
 * <tr>
 * <td>b</td>
 * <td>2</td>
 * <td>z</td>
 * </tr>
 * </table>
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
public class DirectProductNodeModel extends NodeModel {

	/**
	 * Constructor for the node model.
	 */
	protected DirectProductNodeModel() {
		super(2, 1);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
			final ExecutionContext exec) throws Exception {
		final BufferedDataContainer out = exec.createDataContainer(
				new DataTableSpec(inData[0].getDataTableSpec(), inData[1]
						.getDataTableSpec()), true);
		for (final DataRow row0 : inData[0]) {
			for (final DataRow row1 : inData[1]) {
				final List<DataCell> cells = new ArrayList<DataCell>();
				for (final DataCell cell : row0) {
					cells.add(cell);
				}
				for (final DataCell cell : row1) {
					cells.add(cell);
				}
				out.addRowToTable(new DefaultRow(row0.getKey() + "_"
						+ row1.getKey(), cells));
			}

		}
		out.close();
		final BufferedDataTable ret = out.getTable();
		return new BufferedDataTable[] { ret };
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
	protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
			throws InvalidSettingsException {

		return new DataTableSpec[] { new DataTableSpec(inSpecs[0], inSpecs[1]) };
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

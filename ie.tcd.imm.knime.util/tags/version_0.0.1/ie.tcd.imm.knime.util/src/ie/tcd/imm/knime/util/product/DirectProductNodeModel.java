/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
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
		// Do nothing
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
		// No settings yet
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
			throws InvalidSettingsException {
		// No settings yet
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void validateSettings(final NodeSettingsRO settings)
			throws InvalidSettingsException {
		// No settings yet
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadInternals(final File internDir,
			final ExecutionMonitor exec) throws IOException,
			CanceledExecutionException {
		// No settings yet
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveInternals(final File internDir,
			final ExecutionMonitor exec) throws IOException,
			CanceledExecutionException {
		// No settings yet
	}
}

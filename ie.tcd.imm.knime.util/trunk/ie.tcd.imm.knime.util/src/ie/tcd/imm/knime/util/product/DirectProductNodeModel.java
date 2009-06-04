/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package ie.tcd.imm.knime.util.product;

import ie.tcd.imm.knime.util.TransformingNodeModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;

/**
 * This is the model implementation of DirectProduct. This node takes input
 * tables and creates a direct product of the rows.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
public class DirectProductNodeModel extends TransformingNodeModel {

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
	protected BufferedDataTable[] executeDerived(
			final BufferedDataTable[] inData, final ExecutionContext exec)
			throws Exception {
		final BufferedDataContainer out = exec.createDataContainer(
				new DataTableSpec(inData[0].getDataTableSpec(), inData[1]
						.getDataTableSpec()), true);
		final HashMap<RowKey, Set<RowKey>> first = new HashMap<RowKey, Set<RowKey>>();
		final HashMap<RowKey, Set<RowKey>> second = new HashMap<RowKey, Set<RowKey>>();
		final HashMap<RowKey, Set<RowKey>> firstBack = new HashMap<RowKey, Set<RowKey>>();
		final HashMap<RowKey, Set<RowKey>> secondBack = new HashMap<RowKey, Set<RowKey>>();
		for (final DataRow row0 : inData[0]) {
			final HashSet<RowKey> firstSet = new HashSet<RowKey>();
			first.put(row0.getKey(), firstSet);
			for (final DataRow row1 : inData[1]) {
				if (!second.containsKey(row1.getKey())) {
					second.put(row1.getKey(), new HashSet<RowKey>());
				}
				final Set<RowKey> secondSet = second.get(row1.getKey());
				final List<DataCell> cells = new ArrayList<DataCell>();
				for (final DataCell cell : row0) {
					cells.add(cell);
				}
				for (final DataCell cell : row1) {
					cells.add(cell);
				}
				final String rowId = row0.getKey() + "_" + row1.getKey();
				final RowKey rowKey = new RowKey(rowId);
				firstSet.add(rowKey);
				secondSet.add(rowKey);
				firstBack.put(rowKey, Collections.singleton(row0.getKey()));
				secondBack.put(rowKey, Collections.singleton(row1.getKey()));
				out.addRowToTable(new DefaultRow(rowId, cells));
			}
		}
		setMapping(true, 0, 0, firstBack);
		setMapping(true, 1, 0, secondBack);
		out.close();
		final BufferedDataTable ret = out.getTable();
		return new BufferedDataTable[] { ret };
	}

	/**
	 * {@inheritDoc}
	 */
	// @Override
	// protected PortObject[] executeDerived(final PortObject[] inObjects,
	// final ExecutionContext exec) throws Exception {
	// return super.executeDerived(inObjects, exec);
	// }
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
}

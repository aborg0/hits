/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package ie.tcd.imm.knime.util.unpivot;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.regex.PatternSyntaxException;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnDomain;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponent;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.defaultnodesettings.SettingsModelStringArray;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.tableview.TableView;

/**
 * A component to preview the result of the unpivoting.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
class DialogComponentTable extends DialogComponent implements ChangeListener {

	private final int portIndex;
	private final SettingsModelString pattern;
	private final TableView table;

	private final List<String> newColumns = new ArrayList<String>();

	/**
	 * @param model
	 * @param pattern
	 * @param portIndex
	 */
	public DialogComponentTable(final SettingsModelStringArray model,
			final SettingsModelString pattern, final int portIndex) {
		super(model);
		this.pattern = pattern;
		this.portIndex = portIndex;
		table = new TableView();
		getComponentPanel().add(table);
	}

	@Override
	protected void checkConfigurabilityBeforeLoad(final PortObjectSpec[] specs)
			throws NotConfigurableException {
		// Do nothing.
	}

	@Override
	protected void setEnabledComponents(final boolean enabled) {
		table.setEnabled(enabled);
	}

	@Override
	public void setToolTipText(final String text) {
		// No tooltip yet
	}

	@Override
	protected void updateComponent() {
		final PortObjectSpec lastTableSpec = getLastTableSpec(portIndex);
		final List<DataRow> dataRows = new ArrayList<DataRow>();
		final List<DataColumnSpec> colSpecs = new ArrayList<DataColumnSpec>();
		if (lastTableSpec instanceof DataTableSpec) {
			final DataTableSpec spec = (DataTableSpec) lastTableSpec;
			try {
				final Map<List<String>, Map<String, Integer>> parts = UnpivotNodeModel
						.createParts(pattern.getStringValue(), spec);
				newColumns.clear();
				final Set<Integer> participating = new HashSet<Integer>();
				for (final Map<String, Integer> map : parts.values()) {
					for (final Integer i : map.values()) {
						participating.add(i);
					}
				}
				for (int i = 0; i < spec.getNumColumns(); ++i) {
					if (!participating.contains(Integer.valueOf(i))) {
						colSpecs.add(spec.getColumnSpec(i));
					}
				}
				final int singleCount = colSpecs.size();
				int rowCount = 0;
				if (!parts.isEmpty()) {
					for (int i = parts.keySet().iterator().next().size(); i-- > 0;) {
						final String colName = "Col_" + i;
						newColumns.add(colName);
						colSpecs.add(new DataColumnSpecCreator(colName,
								StringCell.TYPE).createSpec());
					}
				}
				final Map<String, DataType> types = new LinkedHashMap<String, DataType>();
				for (final Entry<List<String>, Map<String, Integer>> outer : parts
						.entrySet()) {
					final Map<String, Integer> map = outer.getValue();
					for (final Entry<String, Integer> entry : map.entrySet()) {
						final String colName = entry.getKey();
						final DataType origType = types.get(colName);
						if (origType == null) {
							types.put(colName, spec.getColumnSpec(
									entry.getValue().intValue()).getType());
						} else {
							types.put(colName, DataType.getCommonSuperType(
									origType, spec.getColumnSpec(
											entry.getValue().intValue())
											.getType()));
						}
					}
				}
				for (final Entry<String, DataType> entry : types.entrySet()) {
					colSpecs.add(new DataColumnSpecCreator(entry.getKey(),
							entry.getValue()).createSpec());
				}
				for (final Entry<List<String>, Map<String, Integer>> entry : parts
						.entrySet()) {
					final List<DataCell> cells = new ArrayList<DataCell>(
							colSpecs.size());
					for (int i = singleCount; i-- > 0;) {
						cells.add(DataType.getMissingCell());
					}
					for (final String val : entry.getKey()) {
						cells.add(new StringCell(val));
					}
					for (final Entry<String, Integer> inner : entry.getValue()
							.entrySet()) {
						final DataColumnDomain domain = spec.getColumnSpec(
								inner.getValue().intValue()).getDomain();
						cells.add(domain.getLowerBound() == null ? domain
								.getValues() == null
								|| domain.getValues().isEmpty() ? DataType
								.getMissingCell() : domain.getValues()
								.iterator().next() : domain.getLowerBound());
					}
					dataRows.add(new DefaultRow("Row" + rowCount, cells));
					++rowCount;
				}
			} catch (final PatternSyntaxException e) {
				for (final DataColumnSpec dataColumnSpec : spec) {
					colSpecs.add(dataColumnSpec);
				}
			}
		}
		@SuppressWarnings("deprecation")
		final DataTable data = new org.knime.core.data.def.DefaultTable(
				dataRows.toArray(new DataRow[dataRows.size()]),
				new DataTableSpec(colSpecs.toArray(new DataColumnSpec[colSpecs
						.size()])));
		table.setDataTable(data);
		table.repaint();
	}

	@Override
	protected void validateSettingsBeforeSave() throws InvalidSettingsException {
		((SettingsModelStringArray) getModel()).setStringArrayValue(newColumns
				.toArray(new String[newColumns.size()]));
	}

	@Override
	public void stateChanged(final ChangeEvent e) {
		updateComponent();
	}

}

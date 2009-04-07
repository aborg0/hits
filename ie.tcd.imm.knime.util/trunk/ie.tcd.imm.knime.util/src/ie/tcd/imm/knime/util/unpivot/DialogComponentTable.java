/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package ie.tcd.imm.knime.util.unpivot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.swing.JTable;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponent;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.defaultnodesettings.SettingsModelStringArray;
import org.knime.core.node.port.PortObjectSpec;

import edu.umd.cs.findbugs.annotations.DefaultAnnotation;

/**
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
@DefaultAnnotation( { Nonnull.class, CheckReturnValue.class })
class DialogComponentTable extends DialogComponent implements ChangeListener {

	private final int portIndex;
	private final SettingsModelString pattern;
	private final JTable table;

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
		table = new JTable(new Object[][] { { 1, 1, "Sth", "a", "B", "C" },
				{ 1, 2, "Sth", "?", "?", "?" } }, new Object[] { "Plate",
				"Replicate", "Parameter", "Col_1", "Col_2", "Col_3" });
		table.setTableHeader(new JTableHeader(table.getColumnModel()));
		getComponentPanel().add(table);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seeorg.knime.core.node.defaultnodesettings.DialogComponent#
	 * checkConfigurabilityBeforeLoad(org.knime.core.node.port.PortObjectSpec[])
	 */
	@Override
	protected void checkConfigurabilityBeforeLoad(final PortObjectSpec[] specs)
			throws NotConfigurableException {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.knime.core.node.defaultnodesettings.DialogComponent#setEnabledComponents
	 * (boolean)
	 */
	@Override
	protected void setEnabledComponents(final boolean enabled) {
		table.setEnabled(enabled);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.knime.core.node.defaultnodesettings.DialogComponent#setToolTipText
	 * (java.lang.String)
	 */
	@Override
	public void setToolTipText(final String text) {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.knime.core.node.defaultnodesettings.DialogComponent#updateComponent()
	 */
	@Override
	protected void updateComponent() {
		// TODO Auto-generated method stub
		final PortObjectSpec lastTableSpec = getLastTableSpec(portIndex);
		final List<DataColumnSpec> singleSpecs = new ArrayList<DataColumnSpec>();
		final List<Map<String, String>> names = new ArrayList<Map<String, String>>();
		if (lastTableSpec instanceof DataTableSpec) {
			final DataTableSpec spec = (DataTableSpec) lastTableSpec;
			try {
				final Pattern p = Pattern.compile(pattern.getStringValue());
				for (final DataColumnSpec colSpec : spec) {
					final Matcher matcher = p.matcher(colSpec.getName());
					if (matcher.matches()) {
						final HashMap<String, String> newRow = new HashMap<String, String>();
						for (int i = 1; i <= matcher.groupCount(); ++i) {
							newRow.put(matcher.groupCount() == newColumns
									.size() ? newColumns.get(i - 1) : "Col_"
									+ i, matcher.group(i));
						}
						names.add(newRow);
					} else {
						singleSpecs.add(colSpec);
					}
				}
			} catch (final PatternSyntaxException e) {
				for (final DataColumnSpec dataColumnSpec : spec) {
					singleSpecs.add(dataColumnSpec);
				}
			}
		}
		// final DefaultTableColumnModel columnModel = (DefaultTableColumnModel)
		// table
		// .getColumnModel();

		// for (int i = columnModel.getColumnCount(); i-- > 0;) {
		// columnModel.removeColumn(columnModel.getColumn(i));
		// }
		final Map<String, Integer> colIndices = new HashMap<String, Integer>();
		int colIndex = 0;
		for (final DataColumnSpec col : singleSpecs) {
			final TableColumn column = new TableColumn();
			column.setHeaderValue(col);
			column.setIdentifier(col);
			// columnModel.addColumn(column);
			colIndices.put(col.toString(), Integer.valueOf(colIndex++));
		}
		newColumns.clear();
		if (!names.isEmpty()) {
			for (final String name : names.iterator().next().keySet()) {
				final TableColumn column = new TableColumn();
				column.setHeaderValue(name);
				column.setIdentifier(name);
				// columnModel.addColumn(column);
				colIndices.put(name, Integer.valueOf(colIndex++));
				newColumns.add(name);
			}
		}
		// final DefaultTableModel model = (DefaultTableModel) table.getModel();
		// for (int i = model.getRowCount(); i-- > 0;) {
		// model.removeRow(i);
		// }
		for (final Map<String, String> map : names) {
			// final Vector<String> v = new Vector<String>(columnModel
			// .getColumnCount());
			// v.setSize(columnModel.getColumnCount());
			// for (int i = singleSpecs.size(); i-- > 0;) {
			// v.add("?");
			// }
			// for (final Entry<String, String> entry : map.entrySet()) {
			// v.set(colIndices.get(entry.getKey()).intValue(), entry
			// .getValue());
			// }
			// model.addRow(v);
		}
		// model.newRowsAdded(new TableModelEvent(model));
		table.repaint();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seeorg.knime.core.node.defaultnodesettings.DialogComponent#
	 * validateSettingsBeforeSave()
	 */
	@Override
	protected void validateSettingsBeforeSave() throws InvalidSettingsException {
		// TODO Auto-generated method stub
		((SettingsModelStringArray) getModel()).setStringArrayValue(newColumns
				.toArray(new String[newColumns.size()]));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * javax.swing.event.ChangeListener#stateChanged(javax.swing.event.ChangeEvent
	 * )
	 */
	@Override
	public void stateChanged(final ChangeEvent e) {
		updateComponent();
	}

}

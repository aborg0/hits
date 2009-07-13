package ie.tcd.imm.hits.knime.plate.format;

import ie.tcd.imm.hits.common.Format;

import java.io.File;
import java.io.IOException;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.IntValue;
import org.knime.core.data.container.AbstractCellFactory;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.def.IntCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelEnum;

/**
 * This is the model implementation of Plate Format. Converts between 96, 384,
 * 1536 format plates. It is also capable of mixing replicates in.
 * 
 * @author <a href="bakosg@tcd.ie">Gabor Bakos</a>
 */
public class PlateFormatNodeModel extends NodeModel {

	// the logger instance
	private static final NodeLogger logger = NodeLogger
			.getLogger(PlateFormatNodeModel.class);

	/** Configuration key for the well count of input plates. */
	static final String CFGKEY_FROM_WELL_COUNT = "from.well.count";
	/** Default value for the well count of input plates. */
	static final Format DEFAULT_FROM_WELL_COUNT = Format._96;

	/** Configuration key for the well count of output plates. */
	static final String CFGKEY_TO_WELL_COUNT = "to.well.count";
	/** Default value for the well count of output plates. */
	static final Format DEFAULT_TO_WELL_COUNT = Format._384;

	private final SettingsModelEnum<Format> fromWellCount = new SettingsModelEnum<Format>(
			CFGKEY_FROM_WELL_COUNT, DEFAULT_FROM_WELL_COUNT, Format.values());

	private final SettingsModelEnum<Format> toWellCount = new SettingsModelEnum<Format>(
			CFGKEY_TO_WELL_COUNT, DEFAULT_TO_WELL_COUNT, Format.values());

	/**
	 * Constructor for the node model.
	 */
	protected PlateFormatNodeModel() {
		super(1, 1);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
			final ExecutionContext exec) throws Exception {
		final DataTableSpec spec = inData[0].getDataTableSpec();
		final ColumnRearranger rearranger = new ColumnRearranger(spec);
		final int plateIndex = spec.findColumnIndex("Plate") == -1 ? spec
				.findColumnIndex("plate") : spec.findColumnIndex("Plate");
		final DataColumnSpec plateSpec = spec.getColumnSpec(plateIndex);
		final int rowIndex = spec.findColumnIndex("row");
		final DataColumnSpec rowSpec = spec.getColumnSpec(rowIndex);
		final int colIndex = spec.findColumnIndex("col");
		final DataColumnSpec colSpec = spec.getColumnSpec(colIndex);
		rearranger.replace(
				new AbstractCellFactory(plateSpec, rowSpec, colSpec) {
					private final DataCell[] EMPTY = new DataCell[] {
							DataType.getMissingCell(),
							DataType.getMissingCell(),
							DataType.getMissingCell() };
					@SuppressWarnings("synthetic-access")
					private final Format from = fromWellCount.getEnumValue();
					@SuppressWarnings("synthetic-access")
					private final Format to = toWellCount.getEnumValue();
					private final int fromToTo = from.getWellCount()
							/ to.getWellCount();
					private final int toToFrom = to.getWellCount()
							/ from.getWellCount();
					private final int fromToToCol = from.getCol() / to.getCol();
					private final int toToFromCol = to.getCol() / from.getCol();
					// private final int fromToToRow = from.getRow() /
					// to.getRow();
					private final int toToFromRow = to.getRow() / from.getRow();

					@Override
					public DataCell[] getCells(final DataRow dataRow) {

						final Integer oldPlate = getCell(dataRow, plateIndex);
						final Integer oldRow = getCell(dataRow, rowIndex);
						final Integer oldCol = getCell(dataRow, colIndex);
						if (oldPlate == null || oldRow == null
								|| oldCol == null) {
							return EMPTY;
						}
						final int plate = oldPlate.intValue() - 1;
						final int row = oldRow.intValue() - 1;
						final int col = oldCol.intValue() - 1;
						final boolean toLarger = fromToTo == 0;
						final int newPlate = (toLarger ? plate / toToFrom
								: plate * fromToTo + col / to.getCol() + row
										/ to.getRow() * fromToToCol) + 1;
						final int newRow = (toLarger ? plate / toToFromRow
								* from.getRow() + row : row % to.getRow()) + 1;
						final int newCol = (toLarger ? plate % toToFromCol
								* from.getCol() + col : col % to.getCol()) + 1;
						return new DataCell[] { new IntCell(newPlate),
								new IntCell(newRow), new IntCell(newCol) };
					}

					private Integer getCell(final DataRow row, final int index) {
						final DataCell cell = row.getCell(index);
						return cell.isMissing() ? null : Integer
								.valueOf(((IntValue) cell).getIntValue());
					}
				}, plateIndex, rowIndex, colIndex);
		final BufferedDataTable ret = exec.createColumnRearrangeTable(
				inData[0], rearranger, exec);
		logger.debug("Table converted to " + toWellCount.getStringValue()
				+ " well format.");
		return new BufferedDataTable[] { ret };
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void reset() {
		// TODO Code executed on reset.
		// Models build during execute are cleared here.
		// Also data handled in load/saveInternals will be erased here.
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
			throws InvalidSettingsException {

		// TODO: check if user settings are available, fit to the incoming
		// table structure, and the incoming types are feasible for the node
		// to execute. If the node can execute in its current state return
		// the spec of its output data table(s) (if you can, otherwise an array
		// with null elements), or throw an exception with a useful user message

		return new DataTableSpec[] { inSpecs[0] };
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveSettingsTo(final NodeSettingsWO settings) {
		fromWellCount.saveSettingsTo(settings);
		toWellCount.saveSettingsTo(settings);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
			throws InvalidSettingsException {
		fromWellCount.loadSettingsFrom(settings);
		toWellCount.loadSettingsFrom(settings);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void validateSettings(final NodeSettingsRO settings)
			throws InvalidSettingsException {
		fromWellCount.validateSettings(settings);
		toWellCount.validateSettings(settings);
	}

	@Override
	protected void loadInternals(final File nodeInternDir,
			final ExecutionMonitor exec) throws IOException,
			CanceledExecutionException {
		// No state
	}

	@Override
	protected void saveInternals(final File nodeInternDir,
			final ExecutionMonitor exec) throws IOException,
			CanceledExecutionException {
		// No state
	}
}

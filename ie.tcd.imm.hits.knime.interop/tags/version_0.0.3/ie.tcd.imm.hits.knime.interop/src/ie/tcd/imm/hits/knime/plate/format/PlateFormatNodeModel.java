package ie.tcd.imm.hits.knime.plate.format;

import ie.tcd.imm.hits.common.Format;

import java.io.File;
import java.io.IOException;

import javax.annotation.Nullable;

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
import org.knime.core.node.defaultnodesettings.SettingsModelEnumWithIcon;

/**
 * This is the model implementation of Plate Format. Converts between 96, 384,
 * 1536 format plates. It is also capable of mixing replicates in.
 * 
 * @author <a href="bakosg@tcd.ie">Gabor Bakos</a>
 */
public class PlateFormatNodeModel extends NodeModel {

	/**
	 * This class computes the new indices for plates/replicates/rows/columns.
	 */
	static final class PlateFormatCellFactory extends AbstractCellFactory {
		/**  */
		private final int plateIndex;
		/**  */
		private final int rowIndex;
		/**  */
		private final int colIndex;
		private final DataCell[] EMPTY = new DataCell[] {
				DataType.getMissingCell(), DataType.getMissingCell(),
				DataType.getMissingCell() };
		private final Format from;
		private final Format to;
		private final CombinationPattern pattern;

		/**
		 * Constructs {@link PlateFormatCellFactory}.
		 * 
		 * @param colSpecs
		 *            The input table's {@link DataColumnSpec}s (order: {@code
		 *            plate}, {@code row}, {@code col}).
		 * @param plateIndex
		 *            The index of the {@code plate} column in the input.
		 * @param rowIndex
		 *            The index of the {@code row} column in the input.
		 * @param colIndex
		 *            The index of the {@code col} column in the input.
		 * @param from
		 *            The input table {@link Format}.
		 * @param to
		 *            The output table {@link Format}.
		 * @param pattern
		 *            The {@link CombinationPattern} to use.
		 */
		PlateFormatCellFactory(final DataColumnSpec[] colSpecs,
				final int plateIndex, final int rowIndex, final int colIndex,
				final Format from, final Format to,
				final CombinationPattern pattern) {
			super(colSpecs);
			this.plateIndex = plateIndex;
			this.rowIndex = rowIndex;
			this.colIndex = colIndex;
			this.from = from;
			this.to = to;
			this.pattern = pattern;
			if (pattern.isCombineReplicates()) {
				throw new UnsupportedOperationException(
						"Support for replicates is not implemented");
			}
			if (pattern.getPipettes() != 0) {
				assert to.getRow() % pattern.getPipettes() == 0 : "to.getRow: "
						+ to.getRow() + "\npipettes: " + pattern.getPipettes();
			}
		}

		@Override
		public DataCell[] getCells(final DataRow dataRow) {

			final Integer oldPlate = getCell(dataRow, plateIndex);
			final Integer oldRow = getCell(dataRow, rowIndex);
			final Integer oldCol = getCell(dataRow, colIndex);
			if (oldPlate == null || oldRow == null || oldCol == null) {
				return EMPTY;
			}
			final int plate = oldPlate.intValue() - 1;
			final int row = oldRow.intValue() - 1;
			final int col = oldCol.intValue() - 1;
			final int newPlate = pattern
					.plateCompute(plate, row, col, from, to);
			final int newRow = pattern.rowCompute(plate, row, from, to);
			final int newCol = pattern.colCompute(plate, col, from, to);
			return new DataCell[] { new IntCell(newPlate), new IntCell(newRow),
					new IntCell(newCol) };
		}

		@Nullable
		private Integer getCell(final DataRow row, final int index) {
			final DataCell cell = row.getCell(index);
			return cell.isMissing() ? null : Integer.valueOf(((IntValue) cell)
					.getIntValue());
		}
	}

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

	/** Configuration key for the combination pattern. */
	static final String CFGKEY_COMBINATION_PATTERN = "combination.pattern";
	/** Default value for the combination pattern. */
	static final CombinationPattern DEFAULT_COMBINATION_PATTERN = CombinationPattern.LeftToRightThenDown8PipettesClose;
	/** The possible {@link CombinationPattern} values. */
	static final CombinationPattern[] POSSIBLE_COMBINATION_PATTERN_VALUES = new CombinationPattern[] {
			CombinationPattern.LeftToRightThenDown8PipettesClose,
			CombinationPattern.LeftToRightThenDown,
			CombinationPattern.UpToDownThenRight,
			CombinationPattern.UpToDownThenRight8Pipettes };

	private final SettingsModelEnum<Format> fromWellCount = new SettingsModelEnum<Format>(
			CFGKEY_FROM_WELL_COUNT, DEFAULT_FROM_WELL_COUNT, Format.values());

	private final SettingsModelEnum<Format> toWellCount = new SettingsModelEnum<Format>(
			CFGKEY_TO_WELL_COUNT, DEFAULT_TO_WELL_COUNT, Format.values());

	private final SettingsModelEnumWithIcon<CombinationPattern> combinationPattern = new SettingsModelEnumWithIcon<CombinationPattern>(
			CFGKEY_COMBINATION_PATTERN, DEFAULT_COMBINATION_PATTERN,
			POSSIBLE_COMBINATION_PATTERN_VALUES);

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
		rearranger.replace(new PlateFormatCellFactory(new DataColumnSpec[] {
				plateSpec, rowSpec, colSpec }, plateIndex, rowIndex, colIndex,
				fromWellCount.getEnumValue(), toWellCount.getEnumValue(),
				combinationPattern.getEnumValue()), plateIndex, rowIndex,
				colIndex);
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
		// Nothing to do
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
			throws InvalidSettingsException {
		return new DataTableSpec[] { inSpecs[0] };
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveSettingsTo(final NodeSettingsWO settings) {
		fromWellCount.saveSettingsTo(settings);
		toWellCount.saveSettingsTo(settings);
		combinationPattern.saveSettingsTo(settings);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
			throws InvalidSettingsException {
		fromWellCount.loadSettingsFrom(settings);
		toWellCount.loadSettingsFrom(settings);
		combinationPattern.loadSettingsFrom(settings);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void validateSettings(final NodeSettingsRO settings)
			throws InvalidSettingsException {
		fromWellCount.validateSettings(settings);
		toWellCount.validateSettings(settings);
		try {
			combinationPattern.validateSettings(settings);
		} catch (final InvalidSettingsException e) {
			if (settings instanceof NodeSettingsWO) {
				((NodeSettingsWO) settings)
						.addString(CFGKEY_COMBINATION_PATTERN,
								CombinationPattern.LeftToRightThenDown
										.getDisplayText());
			}
			logger.debug("Unable to load the combination pattern, using "
					+ CombinationPattern.LeftToRightThenDown.getDisplayText(),
					e);
		}
	}

	/** {@inheritDoc} */
	@Override
	protected void loadInternals(final File nodeInternDir,
			final ExecutionMonitor exec) throws IOException,
			CanceledExecutionException {
		// No state
	}

	/** {@inheritDoc} */
	@Override
	protected void saveInternals(final File nodeInternDir,
			final ExecutionMonitor exec) throws IOException,
			CanceledExecutionException {
		// No state
	}
}

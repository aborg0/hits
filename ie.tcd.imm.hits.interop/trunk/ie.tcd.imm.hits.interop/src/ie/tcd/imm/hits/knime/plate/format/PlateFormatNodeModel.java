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
		private final int fromToTo;
		private final int toToFrom;
		private final int fromToToCol;
		private final int toToFromCol;
		private final int fromToToRow;
		private final int toToFromRow;
		private final boolean toLarger;

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
			fromToTo = from.getWellCount() / to.getWellCount();
			toToFrom = to.getWellCount() / from.getWellCount();
			fromToToCol = from.getCol() / to.getCol();
			toToFromCol = to.getCol() / from.getCol();
			fromToToRow = from.getRow() / to.getRow();
			toToFromRow = to.getRow() / from.getRow();
			toLarger = fromToTo == 0;
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
			final int newPlate = plateCompute(plate, row, col);
			// newPlate = (toLarger ? plate / toToFrom : plate
			// * fromToTo + col / to.getCol() + row
			// / to.getRow() * fromToToCol) + 1;
			final int newRow = rowCompute(plate, row);
			// newRow = (toLarger ? plate % toToFrom / toToFromRow
			// * from.getRow() + row : row % to.getRow()) + 1;
			final int newCol = colCompute(plate, col);
			// newCol = (toLarger ? plate % toToFromCol
			// * from.getCol() + col : col % to.getCol()) + 1;
			return new DataCell[] { new IntCell(newPlate), new IntCell(newRow),
					new IntCell(newCol) };
		}

		/**
		 * @param plate
		 *            The original plate number ({@code 0} based).
		 * @param col
		 *            The original column number ({@code 0} based).
		 * @return The computed column number ({@code 1} based).
		 */
		int colCompute(final int plate, final int col) {
			final int newCol;
			if (toLarger) {
				if (pattern.isFirstHorizontal()) {
					if (pattern.isHorizontalToRight()) {
						if (pattern.isVerticalToDown()) {
							if (pattern.getPipettes() == 0) {
								if (pattern.isCloseWells()) {
									throw new UnsupportedOperationException();
								} else {
									newCol = plate % toToFromCol
											* from.getCol() + col + 1;
								}
							} else {
								if (pattern.isCloseWells()) {
									newCol = plate % toToFromCol + col
											* toToFromCol + 1;
								} else {
									throw new UnsupportedOperationException();
								}
							}
						} else {
							if (pattern.getPipettes() == 0) {
								if (pattern.isCloseWells()) {
									throw new UnsupportedOperationException();
								} else {
									throw new UnsupportedOperationException();
								}
							} else {
								if (pattern.isCloseWells()) {
									throw new UnsupportedOperationException();
								} else {
									throw new UnsupportedOperationException();
								}
							}
						}
					} else {
						if (pattern.isVerticalToDown()) {
							if (pattern.getPipettes() == 0) {
								throw new UnsupportedOperationException();
							} else {
								throw new UnsupportedOperationException();
							}
						} else {
							if (pattern.getPipettes() == 0) {
								throw new UnsupportedOperationException();
							} else {
								throw new UnsupportedOperationException();
							}
						}
					}
				} else {// first vertical
					if (pattern.isHorizontalToRight()) {
						if (pattern.isVerticalToDown()) {
							if (pattern.getPipettes() == 0) {
								if (pattern.isCloseWells()) {
									throw new UnsupportedOperationException();
								} else {
									newCol = plate % toToFrom / toToFromCol
											* from.getCol() + col + 1;
								}
							} else {
								if (pattern.isCloseWells()) {
									throw new UnsupportedOperationException();
								} else {
									newCol = plate % toToFrom / toToFromCol
											* from.getCol() + col + 1;
								}
							}
						} else {
							if (pattern.getPipettes() == 0) {
								if (pattern.isCloseWells()) {
									throw new UnsupportedOperationException();
								} else {
									throw new UnsupportedOperationException();
								}
							} else {
								if (pattern.isCloseWells()) {
									throw new UnsupportedOperationException();
								} else {
									throw new UnsupportedOperationException();
								}
							}
						}
					} else {
						if (pattern.isVerticalToDown()) {
							if (pattern.getPipettes() == 0) {
								if (pattern.isCloseWells()) {
									throw new UnsupportedOperationException();
								} else {
									throw new UnsupportedOperationException();
								}
							} else {
								if (pattern.isCloseWells()) {
									throw new UnsupportedOperationException();
								} else {
									throw new UnsupportedOperationException();
								}
							}
						} else {
							if (pattern.getPipettes() == 0) {
								if (pattern.isCloseWells()) {
									throw new UnsupportedOperationException();
								} else {
									throw new UnsupportedOperationException();
								}
							} else {
								if (pattern.isCloseWells()) {
									throw new UnsupportedOperationException();
								} else {
									throw new UnsupportedOperationException();
								}
							}
						}
					}
				}
			} else {// toSmaller
				newCol = (pattern.isCloseWells() ? col / fromToToCol : col
						% to.getCol()) + 1;
			}
			return newCol;
		}

		/**
		 * @param plate
		 *            The original plate number ({@code 0} based).
		 * @param row
		 *            The original row number ({@code 0} based).
		 * @return The computed row number ({@code 1} based).
		 */
		int rowCompute(final int plate, final int row) {
			final int newRow;
			if (toLarger) {
				if (pattern.isFirstHorizontal()) {
					if (pattern.isHorizontalToRight()) {
						if (pattern.isVerticalToDown()) {
							if (pattern.getPipettes() == 0) {
								if (pattern.isCloseWells()) {
									throw new UnsupportedOperationException();
								} else {
									newRow = plate % toToFrom / toToFromRow
											* from.getRow() + row + 1;
								}
							} else {
								if (pattern.isCloseWells()) {
									newRow = plate % toToFrom / toToFromCol
											+ row * toToFromCol + 1;
								} else {
									throw new UnsupportedOperationException();
								}
							}
						} else {
							if (pattern.getPipettes() == 0) {
								if (pattern.isCloseWells()) {
									throw new UnsupportedOperationException();
								} else {
									throw new UnsupportedOperationException();
								}
							} else {
								if (pattern.isCloseWells()) {
									throw new UnsupportedOperationException();
								} else {
									throw new UnsupportedOperationException();
								}
							}
						}
					} else {
						if (pattern.isVerticalToDown()) {
							if (pattern.getPipettes() == 0) {
								if (pattern.isCloseWells()) {
									throw new UnsupportedOperationException();
								} else {
									throw new UnsupportedOperationException();
								}
							} else {
								if (pattern.isCloseWells()) {
									throw new UnsupportedOperationException();
								} else {
									throw new UnsupportedOperationException();
								}
							}
						} else {
							if (pattern.getPipettes() == 0) {
								if (pattern.isCloseWells()) {
									throw new UnsupportedOperationException();
								} else {
									throw new UnsupportedOperationException();
								}
							} else {
								if (pattern.isCloseWells()) {
									throw new UnsupportedOperationException();
								} else {
									throw new UnsupportedOperationException();
								}
							}
						}
					}
				} else {// first vertical
					if (pattern.isHorizontalToRight()) {
						if (pattern.isVerticalToDown()) {
							if (pattern.getPipettes() == 0) {
								if (pattern.isCloseWells()) {
									throw new UnsupportedOperationException();
								} else {
									newRow = plate % toToFrom % toToFromCol
											* from.getRow() + row + 1;
								}
							} else {
								if (pattern.isCloseWells()) {
									throw new UnsupportedOperationException();
								} else {
									newRow = plate
											% toToFrom
											% toToFromCol
											+ row
											* (to.getRow() / pattern
													.getPipettes()) + 1;
								}
							}
						} else {
							if (pattern.getPipettes() == 0) {
								if (pattern.isCloseWells()) {
									throw new UnsupportedOperationException();
								} else {
									throw new UnsupportedOperationException();
								}
							} else {
								if (pattern.isCloseWells()) {
									throw new UnsupportedOperationException();
								} else {
									throw new UnsupportedOperationException();
								}
							}
						}
					} else {
						if (pattern.isVerticalToDown()) {
							if (pattern.getPipettes() == 0) {
								if (pattern.isCloseWells()) {
									throw new UnsupportedOperationException();
								} else {
									throw new UnsupportedOperationException();
								}
							} else {
								if (pattern.isCloseWells()) {
									throw new UnsupportedOperationException();
								} else {
									throw new UnsupportedOperationException();
								}
							}
						} else {
							if (pattern.getPipettes() == 0) {
								if (pattern.isCloseWells()) {
									throw new UnsupportedOperationException();
								} else {
									throw new UnsupportedOperationException();
								}
							} else {
								if (pattern.isCloseWells()) {
									throw new UnsupportedOperationException();
								} else {
									throw new UnsupportedOperationException();
								}
							}
						}
					}
				}
			} else {// toSmaller
				if (pattern.isFirstHorizontal()) {
					if (pattern.isHorizontalToRight()) {
						if (pattern.isVerticalToDown()) {
							if (pattern.getPipettes() == 0) {
								if (pattern.isCloseWells()) {
									throw new UnsupportedOperationException();
								} else {
									newRow = row % to.getRow() + 1;
								}
							} else {
								if (pattern.isCloseWells()) {
									newRow = row
											/ (from.getRow() / pattern
													.getPipettes())
											% to.getRow() + 1;
								} else {
									throw new UnsupportedOperationException();
								}
							}
						} else {
							if (pattern.getPipettes() == 0) {
								if (pattern.isCloseWells()) {
									throw new UnsupportedOperationException();
								} else {
									throw new UnsupportedOperationException();
								}
							} else {
								if (pattern.isCloseWells()) {
									throw new UnsupportedOperationException();
								} else {
									throw new UnsupportedOperationException();
								}
							}
						}
					} else {
						if (pattern.isVerticalToDown()) {
							if (pattern.getPipettes() == 0) {
								if (pattern.isCloseWells()) {
									throw new UnsupportedOperationException();
								} else {
									throw new UnsupportedOperationException();
								}
							} else {
								if (pattern.isCloseWells()) {
									throw new UnsupportedOperationException();
								} else {
									throw new UnsupportedOperationException();
								}
							}
						} else {
							if (pattern.getPipettes() == 0) {
								if (pattern.isCloseWells()) {
									throw new UnsupportedOperationException();
								} else {
									throw new UnsupportedOperationException();
								}
							} else {
								if (pattern.isCloseWells()) {
									throw new UnsupportedOperationException();
								} else {
									throw new UnsupportedOperationException();
								}
							}
						}
					}
				} else {// first vertical
					if (pattern.isHorizontalToRight()) {
						if (pattern.isVerticalToDown()) {
							if (pattern.getPipettes() == 0) {
								if (pattern.isCloseWells()) {
									throw new UnsupportedOperationException();
								} else {
									newRow = row % to.getRow() + 1;
								}
							} else {
								if (pattern.isCloseWells()) {
									if (pattern.isCloseWells()) {
										throw new UnsupportedOperationException();
									} else {
										throw new UnsupportedOperationException();
									}
								} else {
									newRow = row
											/ (from.getRow() / pattern
													.getPipettes())
											% to.getRow() + 1;
								}
							}
						} else {
							if (pattern.getPipettes() == 0) {
								if (pattern.isCloseWells()) {
									throw new UnsupportedOperationException();
								} else {
									throw new UnsupportedOperationException();
								}
							} else {
								if (pattern.isCloseWells()) {
									throw new UnsupportedOperationException();
								} else {
									throw new UnsupportedOperationException();
								}
							}
						}
					} else {
						if (pattern.isVerticalToDown()) {
							if (pattern.getPipettes() == 0) {
								if (pattern.isCloseWells()) {
									throw new UnsupportedOperationException();
								} else {
									throw new UnsupportedOperationException();
								}
							} else {
								if (pattern.isCloseWells()) {
									throw new UnsupportedOperationException();
								} else {
									throw new UnsupportedOperationException();
								}
							}
						} else {
							if (pattern.getPipettes() == 0) {
								if (pattern.isCloseWells()) {
									throw new UnsupportedOperationException();
								} else {
									throw new UnsupportedOperationException();
								}
							} else {
								if (pattern.isCloseWells()) {
									throw new UnsupportedOperationException();
								} else {
									throw new UnsupportedOperationException();
								}
							}
						}
					}
				}
			}
			return newRow;
		}

		/**
		 * @param plate
		 *            The original plate number ({@code 0} based).
		 * @param col
		 *            The original column number ({@code 0} based).
		 * @param row
		 *            The original row number ({@code 0} based).
		 * @return The computed column number ({@code 1} based).
		 */
		int plateCompute(final int plate, final int row, final int col) {
			final int newPlate;
			if (toLarger) {
				newPlate = plate / toToFrom + 1;
			} else {
				if (pattern.isFirstHorizontal()) {
					if (pattern.isHorizontalToRight()) {
						if (pattern.isVerticalToDown()) {
							if (pattern.getPipettes() == 0) {
								if (pattern.isCloseWells()) {
									throw new UnsupportedOperationException();
								} else {
									newPlate = plate * fromToTo + col
											/ to.getCol() + row / to.getRow()
											* fromToToCol + 1;
								}
							} else {
								if (pattern.isCloseWells()) {
									newPlate = plate * fromToTo + col
											% fromToToCol + row % fromToToRow
											* fromToToCol + 1;
								} else {
									throw new UnsupportedOperationException();
								}
							}
						} else {
							if (pattern.isCloseWells()) {
								throw new UnsupportedOperationException();
							} else {
								throw new UnsupportedOperationException();
							}
						}
					} else {
						if (pattern.isVerticalToDown()) {
							if (pattern.isCloseWells()) {
								throw new UnsupportedOperationException();
							} else {
								throw new UnsupportedOperationException();
							}
						} else {
							if (pattern.isCloseWells()) {
								throw new UnsupportedOperationException();
							} else {
								throw new UnsupportedOperationException();
							}
						}
					}
				} else {
					if (pattern.isHorizontalToRight()) {
						if (pattern.isVerticalToDown()) {
							if (pattern.getPipettes() == 0) {
								if (pattern.isCloseWells()) {
									throw new UnsupportedOperationException();
								} else {
									newPlate = plate * fromToTo + col
											/ to.getCol() * fromToToRow + row
											/ to.getRow() + 1;
								}
							} else {
								if (pattern.isCloseWells()) {
									throw new UnsupportedOperationException();
								} else {
									newPlate = plate
											* fromToTo
											+ col
											/ to.getCol()
											* fromToToRow
											+ row
											% (from.getRow() / pattern
													.getPipettes()) + 1;
								}
							}
						} else {
							if (pattern.isCloseWells()) {
								throw new UnsupportedOperationException();
							} else {
								throw new UnsupportedOperationException();
							}
						}
					} else {
						if (pattern.isVerticalToDown()) {
							if (pattern.isCloseWells()) {
								throw new UnsupportedOperationException();
							} else {
								throw new UnsupportedOperationException();
							}
						} else {
							if (pattern.isCloseWells()) {
								throw new UnsupportedOperationException();
							} else {
								throw new UnsupportedOperationException();
							}
						}
					}
				}
			}
			return newPlate;
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
	static final CombinationPattern DEFAULT_COMBINATION_PATTERN = CombinationPattern.UpToDownThenRight8Pipettes;

	private final SettingsModelEnum<Format> fromWellCount = new SettingsModelEnum<Format>(
			CFGKEY_FROM_WELL_COUNT, DEFAULT_FROM_WELL_COUNT, Format.values());

	private final SettingsModelEnum<Format> toWellCount = new SettingsModelEnum<Format>(
			CFGKEY_TO_WELL_COUNT, DEFAULT_TO_WELL_COUNT, Format.values());

	private final SettingsModelEnum<CombinationPattern> combinationPattern = new SettingsModelEnum<CombinationPattern>(
			CFGKEY_COMBINATION_PATTERN, DEFAULT_COMBINATION_PATTERN,
			CombinationPattern.values());

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

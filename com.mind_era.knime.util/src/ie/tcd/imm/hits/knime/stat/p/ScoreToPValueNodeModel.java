package ie.tcd.imm.hits.knime.stat.p;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.math3.exception.MaxCountExceededException;
import org.apache.commons.math3.special.Erf;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.container.AbstractCellFactory;
import org.knime.core.data.container.ColumnRearranger;
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
import org.knime.core.node.defaultnodesettings.SettingsModelFilterString;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;

/**
 * This is the model implementation of ScoreToPValue. Converts scores to p
 * values (also computes the predicted frequency of that value if a sample size
 * selected).
 * 
 * @author <a href="bakosg@tcd.ie">Gabor Bakos</a>
 */
public class ScoreToPValueNodeModel extends NodeModel {
	private static final NodeLogger logger = NodeLogger
			.getLogger(ScoreToPValueNodeModel.class);

	/** Column name prefix for p values. */
	public static final String P_PREFIX = "p ";
	/** Column name prefix for frequency values. */
	public static final String N_PREFIX = "N ";

	/** Configuration key for the selected columns. */
	static final String CFGKEY_COLUMNS = "columns";
	/** Configuration key for the sample count. */
	static final String CFGKEY_SAMPLE_COUNT = "sample.count";
	/** Default value for the sample count. */
	static final int DEFAULT_SAMPLE_COUNT = 0;

	private SettingsModelFilterString columnsModel = new SettingsModelFilterString(
			CFGKEY_COLUMNS);
	private SettingsModelIntegerBounded sampleCountModel = new SettingsModelIntegerBounded(
			CFGKEY_SAMPLE_COUNT, DEFAULT_SAMPLE_COUNT, 0, Integer.MAX_VALUE);
	private ColumnRearranger columnRearranger;

	/**
	 * Constructor for the node model.
	 */
	protected ScoreToPValueNodeModel() {
		super(1, 1);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
			final ExecutionContext exec) throws Exception {
		final BufferedDataTable rearrangeTable = exec
				.createColumnRearrangeTable(inData[0], columnRearranger, exec);
		return new BufferedDataTable[] { rearrangeTable };
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
		columnRearranger = new ColumnRearranger(inSpecs[0]);
		final List<String> colNames = columnsModel.getIncludeList();
		final DataColumnSpec[] pSpecs = new DataColumnSpec[colNames.size()];
		final int[] colIndices = new int[colNames.size()];
		for (int i = pSpecs.length; i-- > 0;) {
			final DataColumnSpecCreator specCreator = new DataColumnSpecCreator(
					inSpecs[0].getColumnSpec(colNames.get(i)));
			specCreator.setName(P_PREFIX + colNames.get(i));
			pSpecs[i] = specCreator.createSpec();
			colIndices[i] = inSpecs[0].findColumnIndex(colNames.get(i));
		}
		columnRearranger.append(new AbstractCellFactory(pSpecs) {
			@Override
			public DataCell[] getCells(final DataRow row) {
				final DataCell[] ret = new DataCell[colIndices.length];
				for (int i = colIndices.length; i-- > 0;) {
					try {
						ret[i] = new DoubleCell(.5 + .5 * Erf
								.erf(((DoubleValue) row.getCell(colIndices[i]))
										.getDoubleValue()
										/ Math.sqrt(2)));
					} catch (final MaxCountExceededException e) {
						ret[i] = DataType.getMissingCell();
						logger.debug(e.getMessage(), e);
					}
				}
				return ret;
			}
		});
		final int sampleCount = sampleCountModel.getIntValue();
		if (sampleCount > 0) {
			final DataColumnSpec[] nSpecs = new DataColumnSpec[colNames.size()];
			for (int i = nSpecs.length; i-- > 0;) {
				final DataColumnSpecCreator specCreator = new DataColumnSpecCreator(
						inSpecs[0].getColumnSpec(colNames.get(i)));
				specCreator.setName(N_PREFIX + colNames.get(i));
				nSpecs[i] = specCreator.createSpec();
			}
			columnRearranger.append(new AbstractCellFactory(nSpecs) {
				@Override
				public DataCell[] getCells(final DataRow row) {
					final DataCell[] ret = new DataCell[colIndices.length];
					for (int i = colIndices.length; i-- > 0;) {
						try {
							ret[i] = new DoubleCell(sampleCount
									* (.5 + .5 * Erf.erf(((DoubleValue) row
											.getCell(colIndices[i]))
											.getDoubleValue()
											/ Math.sqrt(2))));
						} catch (final MaxCountExceededException e) {
							ret[i] = DataType.getMissingCell();
						}
					}
					return ret;
				}
			});
		}
		return new DataTableSpec[] { columnRearranger.createSpec() };
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveSettingsTo(final NodeSettingsWO settings) {
		columnsModel.saveSettingsTo(settings);
		sampleCountModel.saveSettingsTo(settings);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
			throws InvalidSettingsException {
		columnsModel.loadSettingsFrom(settings);
		sampleCountModel.loadSettingsFrom(settings);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void validateSettings(final NodeSettingsRO settings)
			throws InvalidSettingsException {
		columnsModel.validateSettings(settings);
		sampleCountModel.validateSettings(settings);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadInternals(final File internDir,
			final ExecutionMonitor exec) throws IOException,
			CanceledExecutionException {
		// Nothing to do
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveInternals(final File internDir,
			final ExecutionMonitor exec) throws IOException,
			CanceledExecutionException {
		// Nothing to do
	}

}

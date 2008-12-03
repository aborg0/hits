package ie.tcd.imm.hits.knime.ranking;

import ie.tcd.imm.hits.knime.util.ModelBuilder;
import ie.tcd.imm.hits.knime.view.heatmap.HeatmapNodeModel.StatTypes;

import java.io.File;
import java.io.IOException;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * This is the model implementation of Rank. This node ranks the results for
 * each parameter and for each normalisation methods with selectable neutral
 * values and the direction of the upregulation.<br>
 * The downregulated values has negative rankings, the upregulated has positive
 * ones. If present it uses the (final) well annotation information of the
 * wells, and only the sample wells are ranked. If the well annotations are not
 * present the ranking is based on all wells with non-missing values.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
public class RankNodeModel extends NodeModel {

	// the logger instance
	private static final NodeLogger logger = NodeLogger
			.getLogger(RankNodeModel.class);

	/**
	 * These are the possible groups for creating the groups for the ranking.
	 */
	public static enum RankingGroups {
		/** The group is a whole experiment */
		experiment,
		/**
		 * The group is a plate within an experiment, but with the replicates.
		 * <p>
		 * If there were replicates, then the mean of the replicates will be
		 * used for the ranking.
		 */
		plate,
		/**
		 * The group is independent from the plates, but computed for each
		 * replicate in the same experiment.
		 */
		replicate,
		/** The group depends on both the plate and the replicate values. */
		plateAndReplicate;
	}

	/**
	 * The strategy to handle ties. This decides on what to do when there are
	 * ties in the order.
	 */
	public static enum TieHandling {
		/**
		 * The next in the rank will have increased ranking, like the in this
		 * case: {@code 1.2, 2.1, 2.1, 2.4} will generate: {@code 4, 2, 2, 1}
		 * rankings.
		 */
		increase,
		/**
		 * The next rank will has exactly {@code 1} difference to the tied
		 * values' rank. Like the in this case: {@code 1.2, 2.1, 2.1, 2.4} will
		 * generate: {@code 3, 2, 2, 1} rankings.
		 */
		continuous;
	}

	/** Configuration key for the well annotation column. */
	static final String CFGKEY_WELL_ANNOTATION = "ie.tcd.imm.hits.knime.ranking.well.annotation";

	/** initial default well annotation value. */
	static final String DEFAULT_WELL_ANNOTATION = "well annotation";

	/** Configuration key for the rank prefix. */
	static final String CFGKEY_RANK_PREFIX = "ie.tcd.imm.hits.knime.ranking.prefix";

	/** initial default rank prefix. */
	static final String DEFAULT_RANK_PREFIX = "rank";

	/** Configuration key for the ranking groups. */
	static final String CFGKEY_RANKING_GROUPS = "ie.tcd.imm.hits.knime.ranking.groups";

	/** initial default ranking groups. */
	static final String DEFAULT_RANKING_GROUPS = "experiment";

	/** Configuration key for the statistics. */
	static final String CFGKEY_STATISTICS = "ie.tcd.imm.hits.knime.ranking.statistics";

	/** initial default statistics. */
	static final String[] DEFAULT_STATISTICS = new String[] { StatTypes.score
			.name() };

	/** Configuration key for the regulation. */
	static final String CFGKEY_REGULATION = "ie.tcd.imm.hits.knime.ranking.regulation";

	/** initial default regulation. */
	static final String DEFAULT_REGULATION = "0+";

	/** Configuration key for the tie handling. */
	static final String CFGKEY_TIE = "ie.tcd.imm.hits.knime.ranking.tie.handling";

	/** initial default tie handling. */
	static final String DEFAULT_TIE = TieHandling.continuous.name();

	private final SettingsModelString wellAnnotationColumn = new SettingsModelString(
			CFGKEY_WELL_ANNOTATION, DEFAULT_WELL_ANNOTATION);

	private final SettingsModelString rankPrefix = new SettingsModelString(
			CFGKEY_RANK_PREFIX, DEFAULT_RANK_PREFIX);

	private final SettingsModelString groupingModel = new SettingsModelString(
			CFGKEY_RANKING_GROUPS, DEFAULT_RANKING_GROUPS);

	private final SettingsModelString regulationModel = new SettingsModelString(
			CFGKEY_REGULATION, DEFAULT_REGULATION);

	private final SettingsModelString tieHandlingModel = new SettingsModelString(
			CFGKEY_TIE, DEFAULT_TIE);

	/**
	 * Constructor for the node model.
	 */
	protected RankNodeModel() {
		super(1, 1);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
			final ExecutionContext exec) throws Exception {
		final ModelBuilder modelBuilder = new ModelBuilder(inData[0]);
		logger.debug("Ranking the following parameters, and statistics: "
				+ modelBuilder.getParameters() + "; "
				+ modelBuilder.getStatistics());
		// the data table spec of the single output table,
		// the table will have three columns:
		// final DataColumnSpec[] allColSpecs = new DataColumnSpec[3];
		// allColSpecs[0] = new DataColumnSpecCreator("Column 0",
		// StringCell.TYPE)
		// .createSpec();
		// allColSpecs[1] = new DataColumnSpecCreator("Column 1",
		// DoubleCell.TYPE)
		// .createSpec();
		// allColSpecs[2] = new DataColumnSpecCreator("Column 2", IntCell.TYPE)
		// .createSpec();
		// final DataTableSpec outputSpec = new DataTableSpec(allColSpecs);
		// the execution context will provide us with storage capacity, in this
		// case a data container to which we will add rows sequentially
		// Note, this container can also handle arbitrary big data tables, it
		// will buffer to disc if necessary.
		// final BufferedDataContainer container = exec
		// .createDataContainer(outputSpec);
		// let's add m_count rows to it
		// for (int i = 0; i < m_count.getIntValue(); i++) {
		// final RowKey key = new RowKey("Row " + i);
		// the cells of the current row, the types of the cells must match
		// the column spec (see above)
		// final DataCell[] cells = new DataCell[3];
		// cells[0] = new StringCell("String_" + i);
		// cells[1] = new DoubleCell(0.5 * i);
		// cells[2] = new IntCell(i);
		// final DataRow row = new DefaultRow(key, cells);
		// container.addRowToTable(row);

		// check if the execution monitor was canceled
		// exec.checkCanceled();
		// exec.setProgress(i / (double) m_count.getIntValue(), "Adding row "
		// + i);
		// }
		// once we are done, we close the container and return its table
		// container.close();
		// final BufferedDataTable out = container.getTable();
		return new BufferedDataTable[] { /* out */null };
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

		return new DataTableSpec[] { null };
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveSettingsTo(final NodeSettingsWO settings) {
		wellAnnotationColumn.saveSettingsTo(settings);
		rankPrefix.saveSettingsTo(settings);
		groupingModel.saveSettingsTo(settings);
		regulationModel.saveSettingsTo(settings);
		tieHandlingModel.saveSettingsTo(settings);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
			throws InvalidSettingsException {
		wellAnnotationColumn.loadSettingsFrom(settings);
		rankPrefix.loadSettingsFrom(settings);
		groupingModel.loadSettingsFrom(settings);
		regulationModel.loadSettingsFrom(settings);
		tieHandlingModel.loadSettingsFrom(settings);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void validateSettings(final NodeSettingsRO settings)
			throws InvalidSettingsException {
		wellAnnotationColumn.validateSettings(settings);
		rankPrefix.validateSettings(settings);
		groupingModel.validateSettings(settings);
		regulationModel.validateSettings(settings);
		tieHandlingModel.validateSettings(settings);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadInternals(final File internDir,
			final ExecutionMonitor exec) throws IOException,
			CanceledExecutionException {

		// TODO load internal data.
		// Everything handed to output ports is loaded automatically (data
		// returned by the execute method, models loaded in loadModelContent,
		// and user settings set through loadSettingsFrom - is all taken care
		// of). Load here only the other internals that need to be restored
		// (e.g. data used by the views).

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveInternals(final File internDir,
			final ExecutionMonitor exec) throws IOException,
			CanceledExecutionException {

		// TODO save internal models.
		// Everything written to output ports is saved automatically (data
		// returned by the execute method, models saved in the saveModelContent,
		// and user settings saved through saveSettingsTo - is all taken care
		// of). Save here only the other internals that need to be preserved
		// (e.g. data used by the views).

	}

}

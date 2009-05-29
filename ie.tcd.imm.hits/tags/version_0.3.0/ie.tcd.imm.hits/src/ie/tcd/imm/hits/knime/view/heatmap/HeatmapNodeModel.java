/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package ie.tcd.imm.hits.knime.view.heatmap;

import ie.tcd.imm.hits.knime.cellhts2.prefs.PreferenceConstants.PossibleStatistics;
import ie.tcd.imm.hits.knime.util.ModelBuilder;
import ie.tcd.imm.hits.knime.util.ModelBuilder.SpecAnalyser;
import ie.tcd.imm.hits.knime.view.heatmap.ViewModel.ParameterModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.Map.Entry;

import java.awt.Color;
import java.io.File;
import java.io.IOException;

import javax.annotation.Nullable;

import org.eclipse.core.runtime.Assert;
import org.knime.base.node.mine.sota.view.interaction.HiliteManager;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.ContainerTable;
import org.knime.core.data.container.DataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.property.hilite.HiLiteHandler;
import org.knime.core.node.property.hilite.HiLiteManager;

/**
 * This is the model implementation of Heatmap. Shows the heatmap of the plates.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
public class HeatmapNodeModel extends NodeModel {
	private static final String INPUT_TABLE_ZIP = "inputtable.zip";

	/** Configuration key for the save settings option. */
	static final String CFGKEY_SAVE_SETTINGS = "ie.tcd.imm.hits.knime.view.heatmap.settings";
	/** Default value for the save settings option. */
	static final boolean DEFAULT_SAVE_SETTINGS = false;

	private final SettingsModelBoolean saveSettingsModel = new SettingsModelBoolean(
			CFGKEY_SAVE_SETTINGS, DEFAULT_SAVE_SETTINGS);

	/** The file name of the saved settings. */
	static final String SAVE_SETTINGS_FILE_NAME = "settings.xml";

	// the logger instance
	private static final NodeLogger logger = NodeLogger
			.getLogger(HeatmapNodeModel.class);

	/** These are the parameters which are present in the model. */
	private final Collection<ParameterModel> possibleParameters = new HashSet<ParameterModel>();

	/**
	 * This enum lists all supported statistic types. Any other has no special
	 * handling.
	 */
	public static enum StatTypes {
		/** The score statistic */
		score(false, true, false),
		/** The raw values (each replicates) */
		raw(true, true, false),
		/** The median of replicates */
		median(false, true, false),
		/** The mean or the diff of replicates */
		meanOrDiff(false, true, false),
		/** The normalised values (each replicates) */
		normalised(true, true, false),
		/** The raw value divided by the (plate, replicate) median */
		rawPerMedian(true, true, false),
		/** Ranking using the replicate value */
		rankReplicates(true, true, true),
		/** Ranking <em>not</em> using the replicate value */
		rankNonReplicates(false, true, true),
		/** The experiment name. */
		experimentName(false, false, true),
		/** The normalisation, scoring parameters. */
		normalisation(false, false, true),
		/** Any other numeric value from the table (non replicate specific) */
		otherNumeric(false, false, false),
		/** Any other enumerated value from the table (non replicate specific) */
		otherEnumeration(false, false, true),
		/** The plate index */
		plate(false, false, true),
		/** The replicate index */
		replicate(true, false, true),
		/** The parameters, like 'Cell 1/(Form Factor)', ... */
		parameter(false, true, true),
		/** A 'meta' StatTypes, this is for selection from the first 8 values. */
		metaStatType(false, true, true);

		/** Different values for each replicate? */
		private final boolean isUseReplicates;
		/** Different values for each parameter? */
		private final boolean isDependOnParameters;
		/** The values are enumerable, or real values? */
		private final boolean isDiscrete;

		private StatTypes(final boolean isUseReplicates,
				final boolean isDependOnParameters, final boolean isDiscrete) {
			this.isUseReplicates = isUseReplicates;
			this.isDependOnParameters = isDependOnParameters;
			this.isDiscrete = isDiscrete;
		}

		/**
		 * @return Tells whether if it has different values for the parameters
		 *         or not.
		 */
		public boolean isDependOnParameters() {
			return isDependOnParameters;
		}

		/**
		 * @return The values are discrete, or those are from a continuous real
		 *         interval.
		 */
		public boolean isDiscrete() {
			return isDiscrete;
		}

		/**
		 * @return Does it depend on the replicate parameter?
		 */
		public boolean isUseReplicates() {
			return isUseReplicates;
		}

		/**
		 * A mapping from some {@link StatTypes} &Rarr;
		 * {@link PossibleStatistics}.
		 */
		public static final Map<StatTypes, PossibleStatistics> mapToPossStats;
		/**
		 * The {@link StatTypes} with {@link StatTypes#isUseReplicates()}
		 * {@code false}.
		 */
		public static final List<StatTypes> scoreTypes = Collections
				.unmodifiableList(Arrays.asList(new StatTypes[] {
						StatTypes.score, StatTypes.median,
						StatTypes.meanOrDiff, StatTypes.rankNonReplicates }));
		/**
		 * The {@link StatTypes} with {@link StatTypes#isUseReplicates()}
		 * {@code true}.
		 */
		public static final List<StatTypes> replicateTypes = Collections
				.unmodifiableList(Arrays.asList(new StatTypes[] {
						StatTypes.raw, StatTypes.rawPerMedian,
						StatTypes.normalised, StatTypes.rankReplicates }));
		static {
			final EnumMap<StatTypes, PossibleStatistics> map = new EnumMap<StatTypes, PossibleStatistics>(
					StatTypes.class);
			map.put(StatTypes.score, PossibleStatistics.SCORE);
			map.put(StatTypes.normalised, PossibleStatistics.NORMALISED);
			map.put(StatTypes.median, PossibleStatistics.MEDIAN);
			map.put(StatTypes.meanOrDiff, PossibleStatistics.MEAN_OR_DIFF);
			map.put(StatTypes.raw, PossibleStatistics.RAW);
			map.put(StatTypes.rawPerMedian,
					PossibleStatistics.RAW_PER_PLATE_REPLICATE_MEAN);

			mapToPossStats = Collections.unmodifiableMap(map);
		}
	}

	/** This is the {@link HiliteManager} which updates the HiLites. */
	private final HiLiteManager hiliteManager = new HiLiteManager();

	private @Nullable
	ModelBuilder modelBuilder;

	private File internDir;

	/**
	 * Constructor for the node model.
	 */
	protected HeatmapNodeModel() {
		super(1, 0);
		// setAutoExecutable(true);
		setInHiLiteHandler(0, hiliteManager.getFromHiLiteHandler());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
			final ExecutionContext exec) throws Exception {
		executeInner(inData[0], exec);
		return new BufferedDataTable[] { /*
										 * (BufferedDataTable) modelBuilder
										 * .getTable()
										 */};
	}

	private void executeInner(final DataTable table, final ExecutionMonitor exec)
			throws CanceledExecutionException {
		modelBuilder = new ModelBuilder(table);
		exec.checkCanceled();
		final SpecAnalyser sa = modelBuilder.getSpecAnalyser();
		if (sa.isHasReplicate()) {
			final ParameterModel replicates = new ParameterModel("replicate",
					StatTypes.replicate, null, Collections
							.singletonList("replicate"), Collections
							.<String> emptyList());
			for (int i = modelBuilder.getMinReplicate(); i <= modelBuilder
					.getMaxReplicate(); ++i) {
				replicates.getColorLegend()
						.put(Integer.valueOf(i), Color.BLACK);
			}
			possibleParameters.add(replicates);
		}
		if (sa.isHasPlate()) {
			final ParameterModel plates = new ParameterModel("plate",
					StatTypes.plate, null, Collections.singletonList("plate"),
					Collections.<String> emptyList());
			for (int i = modelBuilder.getMinPlate(); i <= modelBuilder
					.getMaxPlate(); ++i) {
				plates.getColorLegend().put(Integer.valueOf(i), Color.BLACK);
			}
			possibleParameters.add(plates);
		}
		final ParameterModel params = new ParameterModel("parameters",
				StatTypes.parameter, null, Collections.<String> emptyList(), sa
						.getParameters());
		for (final String parameter : sa.getParameters()) {
			params.getColorLegend().put(parameter, Color.BLACK);
		}
		possibleParameters.add(params);
		possibleParameters.add(new ParameterModel("score", StatTypes.score,
				null, Collections.singletonList("score"), Collections
						.<String> emptyList()));
		possibleParameters.add(new ParameterModel("median", StatTypes.median,
				null, Collections.singletonList("median"), Collections
						.<String> emptyList()));
		possibleParameters.add(new ParameterModel("medianOrDiff",
				StatTypes.meanOrDiff, null, Collections
						.singletonList("medianOrDiff"), Collections
						.<String> emptyList()));
		possibleParameters.add(new ParameterModel("raw", StatTypes.raw, null,
				Collections.singletonList("raw"), Collections
						.<String> emptyList()));
		possibleParameters.add(new ParameterModel("normalized",
				StatTypes.normalised, null, Collections
						.singletonList("normalized"), Collections
						.<String> emptyList()));
		possibleParameters.add(new ParameterModel(
				"raw/(median plate, replicate)", StatTypes.rawPerMedian, null,
				Collections.singletonList("raw/(median plate, replicate)"),
				Collections.<String> emptyList()));
		final List<String> statsAsStrings = new ArrayList<String>(sa
				.getStatistics().size());
		for (final StatTypes type : sa.getStatistics()) {
			statsAsStrings.add(type.name());
		}
		final ParameterModel statsParamModel = new ParameterModel("statistics",
				StatTypes.metaStatType, null, Collections.<String> emptyList(),
				statsAsStrings);
		{
			for (int i = StatTypes.values().length; i-- > 0;) {
				statsParamModel.getColorLegend().put(Integer.valueOf(i + 1),
						Color.BLACK);
			}
		}
		possibleParameters.add(statsParamModel);
		final Set<String> experiments = new TreeSet<String>(modelBuilder
				.getReplicates().keySet());
		experiments.addAll(modelBuilder.getScores().keySet());
		logger.debug(experiments);
		possibleParameters.add(new ParameterModel("experiment",
				StatTypes.experimentName, null, Collections
						.singletonList(ModelBuilder.EXPERIMENT_COLUMN),
				new ArrayList<String>(experiments)));
		final Set<String> normalisations = new TreeSet<String>();
		for (final Entry<String, Map<String, Map<Integer, Map<String, Map<StatTypes, double[]>>>>> entry : modelBuilder
				.getScores().entrySet()) {
			normalisations.addAll(entry.getValue().keySet());
		}
		for (final Entry<String, Map<String, Map<Integer, Map<Integer, Map<String, Map<StatTypes, double[]>>>>>> entry : modelBuilder
				.getReplicates().entrySet()) {
			normalisations.addAll(entry.getValue().keySet());
		}
		logger.debug(normalisations);
		possibleParameters.add(new ParameterModel("normalisation",
				StatTypes.normalisation, null, Arrays.asList(new String[] {
						ModelBuilder.NORMALISATION_METHOD_COLUMN,
						ModelBuilder.LOG_TRANSFORM_COLUMN,
						ModelBuilder.NORMALISATION_KIND_COLUMN,
						ModelBuilder.VARIANCE_ADJUSTMENT_COLUMN,
						ModelBuilder.SCORING_METHOD_COLUMN,
						ModelBuilder.SUMMARISE_METHOD_COLUMN }),
				new ArrayList<String>(normalisations)));
		setInHiLiteHandler(0, hiliteManager.getFromHiLiteHandler());
	}

	/**
	 * @return The actual table used.
	 */
	@Nullable
	public DataTable getTable() {
		return modelBuilder == null ? null : modelBuilder.getTable();
	}

	/**
	 * @return The possible {@link ParameterModel}s associated to this
	 *         {@link HeatmapNodeModel}.
	 */
	public Collection<ParameterModel> getPossibleParameters() {
		return Collections.unmodifiableCollection(possibleParameters);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void reset() {
		// TODO Code executed on reset.
		possibleParameters.clear();
		modelBuilder = null;
		// Models build during execute are cleared here.
		// Also data handled in load/saveInternals will be erased here.
	}

	/**
	 * @return The {@link ModelBuilder} belonging to the current
	 *         {@link #getTable() table}.
	 */
	public @Nullable
	ModelBuilder getModelBuilder() {
		return modelBuilder;
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
		return new DataTableSpec[] { /* inSpecs[0] */};
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveSettingsTo(final NodeSettingsWO settings) {
		saveSettingsModel.saveSettingsTo(settings);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
			throws InvalidSettingsException {
		saveSettingsModel.loadSettingsFrom(settings);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void validateSettings(final NodeSettingsRO settings)
			throws InvalidSettingsException {
		saveSettingsModel.validateSettings(settings);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadInternals(final File internDir,
			final ExecutionMonitor exec) throws IOException,
			CanceledExecutionException {
		this.internDir = internDir;
		// TODO load internal data.
		final File file = new File(internDir, INPUT_TABLE_ZIP);
		if (file.isFile() && file.exists()) {
			final ContainerTable table = DataContainer.readFromZip(file);
			executeInner(table, exec);
		}
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
		final File file = new File(internDir, INPUT_TABLE_ZIP);
		if (getTable() != null) {
			DataContainer.writeToZip(getTable(), file, exec);
		} else {
			final boolean deleted = file.delete();
			assert deleted || !deleted;
		}
		// Everything written to output ports is saved automatically (data
		// returned by the execute method, models saved in the saveModelContent,
		// and user settings saved through saveSettingsTo - is all taken care
		// of). Save here only the other internals that need to be preserved
		// (e.g. data used by the views).
	}

	@Override
	protected void setInHiLiteHandler(final int inIndex,
			final HiLiteHandler hiLiteHdl) {
		super.setInHiLiteHandler(inIndex, hiLiteHdl);
		Assert.isTrue(inIndex == 0, "Only the first inport supports HiLite.");
		hiliteManager.addToHiLiteHandler(hiLiteHdl);
	}

	/**
	 * @return the save settings property value.
	 */
	boolean isSaveSettings() {
		return saveSettingsModel.getBooleanValue();
	}

	/**
	 * @return the internal directory to store the view parameters.
	 */
	File getInternDir() {
		return internDir;
	}
}

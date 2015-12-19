/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package ie.tcd.imm.hits.knime.view.heatmap;

import ie.tcd.imm.hits.common.PublicConstants;
import ie.tcd.imm.hits.knime.util.ModelBuilder;
import ie.tcd.imm.hits.knime.util.ModelBuilder.SpecAnalyser;
import ie.tcd.imm.hits.knime.view.StatTypes;
import ie.tcd.imm.hits.knime.view.heatmap.ViewModel.ParameterModel;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.Map.Entry;

import javax.annotation.Nullable;

import org.eclipse.core.runtime.Assert;
import org.knime.base.node.mine.sota.view.interaction.HiliteManager;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.IntValue;
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
						.singletonList(PublicConstants.EXPERIMENT_COLUMN),
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
						PublicConstants.NORMALISATION_METHOD_COLUMN,
						PublicConstants.LOG_TRANSFORM_COLUMN,
						PublicConstants.NORMALISATION_KIND_COLUMN,
						PublicConstants.VARIANCE_ADJUSTMENT_COLUMN,
						PublicConstants.SCORING_METHOD_COLUMN,
						PublicConstants.SUMMARISE_METHOD_COLUMN }),
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
		final SpecAnalyser specAnalyser = new SpecAnalyser(inSpecs[0]);
		if (specAnalyser.getWellIndex() == -1
				|| inSpecs[0].getColumnSpec(specAnalyser.getWellIndex())
						.getType().isCompatible(IntValue.class)) {
			throw new InvalidSettingsException("No "
					+ PublicConstants.WELL_COL_NAME
					+ " column in the input (with proper (Integer) type).");
		}
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

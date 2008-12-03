package ie.tcd.imm.hits.knime.view.heatmap;

import ie.tcd.imm.hits.knime.view.heatmap.ViewModel.ParameterModel;
import ie.tcd.imm.hits.util.Pair;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.eclipse.core.runtime.Assert;
import org.knime.base.node.mine.sota.view.interaction.HiliteManager;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.StringValue;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.property.hilite.DefaultHiLiteManager;
import org.knime.core.node.property.hilite.HiLiteHandler;

/**
 * This is the model implementation of Heatmap. Shows the heatmap of the plates.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
public class HeatmapNodeModel extends NodeModel {

	private static final String RAW_PLATE_REPLICATE_MEDIAN_START = "raw/(plate, replicate mean)_";

	private static final String MEAN_OR_DIFF_START = "mean or diff_";

	private static final String NORMALISED_START = "normalized_";

	private static final String MEDIAN_START = "median_";

	private static final String RAW_START = "raw_";

	// the logger instance
	private static final NodeLogger logger = NodeLogger
			.getLogger(HeatmapNodeModel.class);

	private static final String SCORE_START = "score_";

	public static final StatTypes[] scoreTypes = new StatTypes[] {
			StatTypes.score, StatTypes.median, StatTypes.meanOrDiff };

	public static final StatTypes[] replicateTypes = new StatTypes[] {
			StatTypes.raw, StatTypes.rawPerMedian, StatTypes.normalized };

	/** These are the parameters which are present in the model. */
	private final Collection<ParameterModel> possibleParameters = new HashSet<ParameterModel>();

	private final List<String> parameters = new ArrayList<String>();

	private final EnumSet<StatTypes> statistics = EnumSet
			.noneOf(StatTypes.class);

	/** key, plate, position [0-95] */
	Map<DataCell, Pair<Integer, Integer>> keyToPlateAndPosition = new HashMap<DataCell, Pair<Integer, Integer>>();

	private boolean hasPlate;

	private boolean hasReplicate;

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
		normalized(true, true, false),
		/** The raw value divided by the (plate, replicate) median */
		rawPerMedian(true, true, false),
		/** Ranking using the replicate value */
		rankReplicates(true, true, true),
		/** Ranking <em>not</em> using the replicate value */
		rankNonReplicates(false, true, true),
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
	}

	/** Plate, column, values */
	final Map<Integer, Map<String, String[]>> texts = new TreeMap<Integer, Map<String, String[]>>();

	/** Plate, parameter, statistics type, values */
	final Map<Integer, Map<String, EnumMap<StatTypes, double[]>>> scoreValues = new TreeMap<Integer, Map<String, EnumMap<StatTypes, double[]>>>();

	/** plate, replicate, parameter, values */
	final Map<Integer, Map<Integer, Map<String, EnumMap<StatTypes, double[]>>>> replicateValues = new TreeMap<Integer, Map<Integer, Map<String, EnumMap<StatTypes, double[]>>>>();

	/** This is the {@link HiliteManager} which updates the HiLites. */
	private final DefaultHiLiteManager hiliteManager = new DefaultHiLiteManager();

	private BufferedDataTable table;

	/**
	 * Constructor for the node model.
	 */
	protected HeatmapNodeModel() {
		super(1, 1);
		setAutoExecutable(true);
		setInHiLiteHandler(0, hiliteManager);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
			final ExecutionContext exec) throws Exception {
		keyToPlateAndPosition.clear();
		table = inData[0];
		hasPlate = false;
		hasReplicate = false;
		int plateIndex = -1;
		int replicateIndex = -1;
		int wellIndex = -1;
		final EnumMap<StatTypes, Map<String, Integer>> indices = new EnumMap<StatTypes, Map<String, Integer>>(
				StatTypes.class);
		for (final StatTypes stat : StatTypes.values()) {
			indices.put(stat, new TreeMap<String, Integer>());
		}
		final Map<String, Integer> stringIndices = new TreeMap<String, Integer>();
		final Map<String, Integer> valueIndices = new TreeMap<String, Integer>();
		int idx = -1;
		for (final DataColumnSpec spec : table.getDataTableSpec()) {
			++idx;
			if (spec.getName().startsWith(SCORE_START)) {
				parameters.add(spec.getName().substring(SCORE_START.length()));
				statistics.add(StatTypes.score);
				indices.get(StatTypes.score).put(
						spec.getName().substring(SCORE_START.length()),
						Integer.valueOf(idx));
				continue;
			}
			if (spec.getName().startsWith(RAW_START)) {
				statistics.add(StatTypes.raw);
				indices.get(StatTypes.raw).put(
						spec.getName().substring(RAW_START.length()),
						Integer.valueOf(idx));
				continue;
			}
			if (spec.getName().startsWith(MEDIAN_START)) {
				statistics.add(StatTypes.median);
				indices.get(StatTypes.median).put(
						spec.getName().substring(MEDIAN_START.length()),
						Integer.valueOf(idx));
				continue;
			}
			if (spec.getName().startsWith(NORMALISED_START)) {
				statistics.add(StatTypes.normalized);
				indices.get(StatTypes.normalized).put(
						spec.getName().substring(NORMALISED_START.length()),
						Integer.valueOf(idx));
				continue;
			}
			if (spec.getName().startsWith(MEAN_OR_DIFF_START)) {
				statistics.add(StatTypes.meanOrDiff);
				indices.get(StatTypes.meanOrDiff).put(
						spec.getName().substring(MEAN_OR_DIFF_START.length()),
						Integer.valueOf(idx));
				continue;
			}
			if (spec.getName().startsWith(RAW_PLATE_REPLICATE_MEDIAN_START)) {
				statistics.add(StatTypes.rawPerMedian);
				indices.get(StatTypes.rawPerMedian).put(
						spec.getName().substring(
								RAW_PLATE_REPLICATE_MEDIAN_START.length()),
						Integer.valueOf(idx));
				continue;
			}
			if (spec.getName().equalsIgnoreCase("plate")) {
				hasPlate = true;
				plateIndex = Integer.valueOf(idx);
				continue;
			}
			if (spec.getName().equalsIgnoreCase("replicate")) {
				hasReplicate = true;
				replicateIndex = idx;
				continue;
			}
			if (spec.getName().equalsIgnoreCase("well")) {
				wellIndex = idx;
				continue;
			}
			if (spec.getType().isCompatible(StringValue.class)) {
				stringIndices.put(spec.getName(), Integer.valueOf(idx));
				continue;
			}
			if (spec.getType().isCompatible(DoubleValue.class)) {
				valueIndices.put(spec.getName(), Integer.valueOf(idx));
			}
		}
		logger.debug("Statistics: " + statistics);
		if (!hasPlate) {
			throw new IllegalStateException("No plate information found");
		}
		int minReplicate = Integer.MAX_VALUE;
		int maxReplicate = Integer.MIN_VALUE, minPlate = Integer.MAX_VALUE, maxPlate = Integer.MIN_VALUE;
		for (final DataRow dataRow : table) {
			final Integer plate = getInt(dataRow, plateIndex);
			if (!replicateValues.containsKey(plate)) {
				replicateValues
						.put(
								plate,
								new HashMap<Integer, Map<String, EnumMap<StatTypes, double[]>>>());
			}
			if (!scoreValues.containsKey(plate)) {
				scoreValues.put(plate,
						new HashMap<String, EnumMap<StatTypes, double[]>>());
			}
			if (!texts.containsKey(plate)) {
				final HashMap<String, String[]> map = new HashMap<String, String[]>();
				texts.put(plate, map);
				for (final String colName : stringIndices.keySet()) {
					map.put(colName, new String[96]);
				}
			}
			final int well = convertWellToPosition(((StringCell) dataRow
					.getCell(wellIndex)).getStringValue());
			keyToPlateAndPosition.put(dataRow.getKey().getId(),
					new Pair<Integer, Integer>(plate, Integer.valueOf(well)));
			if (hasReplicate) {
				final Integer replicate = getInt(dataRow, replicateIndex);
				minReplicate = Math.min(replicate.intValue(), minReplicate);
				maxReplicate = Math.max(replicate.intValue(), maxReplicate);
				final Map<Integer, Map<String, EnumMap<StatTypes, double[]>>> outerMap = replicateValues
						.get(plate);
				if (!outerMap.containsKey(replicate)) {
					outerMap
							.put(
									replicate,
									new HashMap<String, EnumMap<StatTypes, double[]>>());
				}
				final Map<String, EnumMap<StatTypes, double[]>> paramMap = outerMap
						.get(replicate);
				for (final String param : parameters) {
					if (!paramMap.containsKey(param)) {
						paramMap.put(param, new EnumMap<StatTypes, double[]>(
								StatTypes.class));
					}
					final EnumMap<StatTypes, double[]> enumMap = paramMap
							.get(param);
					for (final StatTypes stat : replicateTypes) {
						if (!enumMap.containsKey(stat)) {
							enumMap.put(stat, new double[96]);
						}
						enumMap.get(stat)[well] = ((DoubleValue) dataRow
								.getCell(indices.get(stat).get(param)
										.intValue())).getDoubleValue();
					}
				}
			}
			if (statistics.contains(StatTypes.score)
					|| statistics.contains(StatTypes.meanOrDiff)
					|| statistics.contains(StatTypes.median)) {
				final Map<String, EnumMap<StatTypes, double[]>> map = scoreValues
						.get(plate);
				for (final String param : parameters) {
					if (!map.containsKey(param)) {
						map.put(param, new EnumMap<StatTypes, double[]>(
								StatTypes.class));
						for (final StatTypes type : scoreTypes) {
							map.get(param).put(type, new double[96]);
						}
					}
					final EnumMap<StatTypes, double[]> values = map.get(param);
					for (final StatTypes type : new StatTypes[] {
							StatTypes.score, StatTypes.median,
							StatTypes.meanOrDiff }) {

						final double[] vals = values.get(type);
						if (false) {
							vals[well] = param.equals("Cell Count") ? 0.0
									: (param.equals("Nuc Displacement")) ? 1
											: -1;
						} else {
							vals[well] = ((DoubleValue) dataRow.getCell(indices
									.get(type).get(param).intValue()))
									.getDoubleValue();
						}
					}
				}
			}
			if (hasPlate) {
				final int val = ((IntCell) dataRow.getCell(plateIndex))
						.getIntValue();
				minPlate = Math.min(val, minPlate);
				maxPlate = Math.max(val, maxPlate);
				for (final Entry<String, Integer> entry : stringIndices
						.entrySet()) {
					final DataCell cell = dataRow.getCell(entry.getValue()
							.intValue());
					texts.get(plate).get(entry.getKey())[well] = cell
							.isMissing() ? "" : ((StringValue) cell)
							.getStringValue().intern();
				}
			}
		}
		if (hasReplicate) {
			final ParameterModel replicates = new ParameterModel("replicate",
					StatTypes.replicate, null, Collections
							.singletonList("replicate"), Collections
							.<String> emptyList());
			for (int i = minReplicate; i <= maxReplicate; ++i) {
				replicates.getColorLegend()
						.put(Integer.valueOf(i), Color.BLACK);
			}
			possibleParameters.add(replicates);
		}
		if (hasPlate) {
			final ParameterModel plates = new ParameterModel("plate",
					StatTypes.plate, null, Collections.singletonList("plate"),
					Collections.<String> emptyList());
			for (int i = minPlate; i <= maxPlate; ++i) {
				plates.getColorLegend().put(Integer.valueOf(i), Color.BLACK);
			}
			possibleParameters.add(plates);
		}
		final ParameterModel params = new ParameterModel("parameters",
				StatTypes.parameter, null, Collections.<String> emptyList(),
				parameters);
		for (final String parameter : parameters) {
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
				StatTypes.normalized, null, Collections
						.singletonList("normalized"), Collections
						.<String> emptyList()));
		possibleParameters.add(new ParameterModel(
				"raw/(median plate, replicate)", StatTypes.rawPerMedian, null,
				Collections.singletonList("raw/(median plate, replicate)"),
				Collections.<String> emptyList()));
		final List<String> statsAsStrings = new ArrayList<String>(statistics
				.size());
		for (final StatTypes type : statistics) {
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
		setInHiLiteHandler(0, hiliteManager);
		return new BufferedDataTable[] { table };
	}

	/**
	 * @return The actual table used.
	 */
	public BufferedDataTable getTable() {
		return table;
	}

	private Integer getInt(final DataRow dataRow, final int plateIndex) {
		return Integer.valueOf(((IntCell) dataRow.getCell(plateIndex))
				.getIntValue());
	}

	@Deprecated
	private int convertWellToPosition(final String well) {
		return ((Character.toLowerCase(well.charAt(0)) - 'a')
				* 12
				+ Integer.parseInt(well.substring(well.length() - 2, well
						.length())) - 1);
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
		parameters.clear();
		statistics.clear();
		hasPlate = hasReplicate = false;
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

		// TODO save user settings to the config object.

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
			throws InvalidSettingsException {

		// TODO load (valid) settings from the config object.
		// It can be safely assumed that the settings are valided by the
		// method below.

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void validateSettings(final NodeSettingsRO settings)
			throws InvalidSettingsException {

		// TODO check if the settings could be applied to our model
		// e.g. if the count is in a certain range (which is ensured by the
		// SettingsModel).
		// Do not actually set any values of any member variables.

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

	@Override
	protected void setInHiLiteHandler(final int inIndex,
			final HiLiteHandler hiLiteHdl) {
		super.setInHiLiteHandler(inIndex, hiLiteHdl);
		Assert.isTrue(inIndex == 0, "Only the first inport supports HiLite.");
		hiliteManager.addHiLiteHandler(hiLiteHdl);
	}
}

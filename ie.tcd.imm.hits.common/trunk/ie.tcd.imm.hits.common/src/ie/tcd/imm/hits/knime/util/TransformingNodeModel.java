/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package ie.tcd.imm.hits.knime.util;

import ie.tcd.imm.hits.util.SuffixFilenameFilter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.io.File;
import java.io.IOException;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.RowIterator;
import org.knime.core.data.RowKey;
import org.knime.core.data.StringValue;
import org.knime.core.data.container.ContainerTable;
import org.knime.core.data.container.DataContainer;
import org.knime.core.data.def.DefaultRow;
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
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortType;
import org.knime.core.node.property.hilite.DefaultHiLiteMapper;
import org.knime.core.node.property.hilite.HiLiteHandler;
import org.knime.core.node.property.hilite.HiLiteMapper;
import org.knime.core.node.property.hilite.HiLiteTranslator;
import org.knime.core.util.Pair;

import edu.umd.cs.findbugs.annotations.DefaultAnnotation;

/**
 * Common base class for the transforming {@link NodeModel}s. Supports HiLite.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
@DefaultAnnotation( { Nonnull.class, CheckReturnValue.class })
public abstract class TransformingNodeModel extends NodeModel {

	private static interface MapperConstructor {
		HiLiteMapper create(Map<RowKey, Set<RowKey>> map);
	}

	private static final MapperConstructor singleMapping = new MapperConstructor() {
		@Override
		public HiLiteMapper create(final Map<RowKey, Set<RowKey>> map) {
			return new DefaultHiLiteMapper(map);
		}
	};

	/**
	 * The currently supported HiLiting strategies.<br/>
	 * Note: Others strategies require to implement a class similar to
	 * {@link HiLiteTranslator} (something like was in this file as
	 * HiLiteListenerMapper in r340).<br/>
	 * The HiLiteHandler custom class might be also useful (last present at
	 * r348), and also the ie.tcd.imm.knime.util.TwoWayHiLiteMapper, last
	 * present at r348.
	 */
	public static HiLite[] supportedHiLites = new HiLite[] { HiLite.NoHiLite,
			HiLite.OnlyIfAllSelected };

	private static final NodeLogger logger = NodeLogger
			.getLogger(TransformingNodeModel.class);

	/** Configuration key for the HiLite support. */
	public static final String CFGKEY_HILITE = "hilite";
	/** Default value for the HiLite behaviour. */
	public static final HiLite DEFAULT_HILITE = HiLite.NoHiLite;
	private final SettingsModelString hiLiteBehaviourModel = new SettingsModelString(
			TransformingNodeModel.CFGKEY_HILITE, DEFAULT_HILITE
					.getDisplayText());

	private static final Pattern fileNamePattern = Pattern
			.compile("(\\d+)_(\\d+)_.+");

	/** File name suffix for the forward mapping tables. */
	protected static final String forwardMappingSuffix = "ForwardMapping.zip";

	// The pair's first elem is the "from", the second is the "to" port index
	// (0-based), the value maps are mapping the row keys from "from" to "to".
	private final Map<Pair<Integer, Integer>, Map<RowKey, Set<RowKey>>> rowKeyForwardMapping = new HashMap<Pair<Integer, Integer>, Map<RowKey, Set<RowKey>>>();

	/** inports &rArr; outports */
	private final Map<Integer, List<Integer>> connections = new HashMap<Integer, List<Integer>>();

	private final Map<Integer, HiLiteTranslator> translators = new HashMap<Integer, HiLiteTranslator>();

	/**
	 * @param nrInDataPorts
	 *            Number of in data ports (all {@link BufferedDataTable}s).
	 * @param nrOutDataPorts
	 *            Number of out data ports (all {@link BufferedDataTable}s).
	 * @see NodeModel#NodeModel(int, int)
	 */
	protected TransformingNodeModel(final int nrInDataPorts,
			final int nrOutDataPorts) {
		this(nrInDataPorts, nrOutDataPorts, createTotalConnections(
				nrInDataPorts, nrOutDataPorts));
	}

	/**
	 * Generates a total mapping between the in and out ports.
	 * 
	 * @param nrInDataPorts
	 *            A non-negative integer.
	 * @param nrOutDataPorts
	 *            A non-negative integer.
	 * @return A total mapping between the {@code [0, nrInDataPorts)} and
	 *         {@code [0, nrOutDataPorts)} integer intervals.
	 */
	protected static Map<Integer, List<Integer>> createTotalConnections(
			final int nrInDataPorts, final int nrOutDataPorts) {
		final Map<Integer, List<Integer>> ret = new HashMap<Integer, List<Integer>>();
		for (int i = nrInDataPorts; i-- > 0;) {
			final List<Integer> list = new ArrayList<Integer>(nrOutDataPorts);
			ret.put(Integer.valueOf(i), list);
			for (int j = nrOutDataPorts; j-- > 0;) {
				list.add(Integer.valueOf(j));
			}
		}
		return ret;
	}

	/**
	 * @param nrInDataPorts
	 *            Number of in data ports (all {@link BufferedDataTable}s).
	 * @param nrOutDataPorts
	 *            Number of out data ports (all {@link BufferedDataTable}s).
	 * @param connections
	 *            The interesting connections between the in and out ports.
	 * @see NodeModel#NodeModel(int, int)
	 */
	protected TransformingNodeModel(final int nrInDataPorts,
			final int nrOutDataPorts,
			final Map<Integer, List<Integer>> connections) {
		super(nrInDataPorts, nrOutDataPorts);
		this.connections.putAll(copyConnections(connections));
		addHiLiteHandlers(nrInDataPorts);
	}

	/**
	 * @param mapping
	 *            A {@link Map} from {@link Integer}s to a {@link List} of
	 *            {@link Integer}s.
	 * @return A copy of the {@code mapping}.
	 */
	protected Map<? extends Integer, ? extends List<Integer>> copyConnections(
			final Map<Integer, List<Integer>> mapping) {
		final Map<Integer, List<Integer>> ret = new HashMap<Integer, List<Integer>>();
		for (final Entry<Integer, List<Integer>> entry : mapping.entrySet()) {
			ret.put(entry.getKey(), new ArrayList<Integer>(entry.getValue()));
		}
		return ret;
	}

	/**
	 * @param inPortTypes
	 *            Types of in data ports.
	 * @param outPortTypes
	 *            Types of out data ports.
	 * @see NodeModel#NodeModel(PortType[], PortType[])
	 */
	public TransformingNodeModel(final PortType[] inPortTypes,
			final PortType[] outPortTypes) {
		this(inPortTypes, outPortTypes, createTotalConnections(
				inPortTypes.length, outPortTypes.length));
	}

	/**
	 * @param inPortTypes
	 *            Types of in data ports.
	 * @param outPortTypes
	 *            Types of out data ports.
	 * @param connections
	 *            The interesting connections between the in and out ports.
	 * @see NodeModel#NodeModel(PortType[], PortType[])
	 */
	public TransformingNodeModel(final PortType[] inPortTypes,
			final PortType[] outPortTypes,
			final Map<Integer, List<Integer>> connections) {
		super(inPortTypes, outPortTypes);
		this.connections.putAll(copyConnections(connections));
		addHiLiteHandlers(inPortTypes.length);
	}

	private void addHiLiteHandlers(final int nrInDataPorts) {
		for (int i = nrInDataPorts; i-- > 0;) {
			final HiLiteTranslator hiLiteTranslator = new HiLiteTranslator();
			translators.put(Integer.valueOf(i), hiLiteTranslator);
		}
	}

	@Override
	protected void setInHiLiteHandler(final int inIndex,
			final HiLiteHandler hiLiteHdl) {
		final HiLiteTranslator hiLiteTranslator = translators.get(Integer
				.valueOf(inIndex));
		hiLiteTranslator.removeAllToHiliteHandlers();
		hiLiteTranslator.addToHiLiteHandler(hiLiteHdl);
	}

	@Override
	protected HiLiteHandler getOutHiLiteHandler(final int outIndex) {
		return translators.get(Integer.valueOf(0))//
				.getFromHiLiteHandler();
	}

	@Override
	protected final BufferedDataTable[] execute(
			final BufferedDataTable[] inData, final ExecutionContext exec)
			throws Exception {
		final BufferedDataTable[] ret = executeDerived(inData, exec);
		adjustHiLite();
		return ret;
	}

	@Override
	protected PortObject[] execute(final PortObject[] inObjects,
			final ExecutionContext exec) throws Exception {
		final PortObject[] executeDerived = executeDerived(inObjects, exec);
		adjustHiLite();
		return executeDerived;
	}

	/**
	 * Please override if you need special PortObjects. If only
	 * {@link BufferedDataTable}s used, it is not necessary, override instead
	 * {@link #executeDerived(BufferedDataTable[], ExecutionContext)}.
	 * 
	 * @param inObjects
	 *            The input {@link PortObject}s.
	 * @param exec
	 *            An {@link ExecutionContext}.
	 * @return The result {@link PortObject}s.
	 * @throws Exception
	 *             Any problem from the derived method:
	 *             {@link #executeDerived(BufferedDataTable[], ExecutionContext)}
	 *             .
	 * @see NodeModel#execute(PortObject[], ExecutionContext)
	 */
	protected PortObject[] executeDerived(final PortObject[] inObjects,
			final ExecutionContext exec) throws Exception {
		// default implementation: the standard version needs to hold: all
		// ports are data ports!

		// (1) case PortObjects to BufferedDataTable
		final BufferedDataTable[] inTables = new BufferedDataTable[inObjects.length];
		for (int i = 0; i < inObjects.length; i++) {
			try {
				inTables[i] = (BufferedDataTable) inObjects[i];
			} catch (final ClassCastException cce) {
				throw new IOException("Input Port " + i
						+ " does not hold data table specs. "
						+ "Likely reason: wrong version"
						+ " of NodeModel.execute() overwritten!");
			}
		}
		// (2) call old-fashioned, data-only execute
		final BufferedDataTable[] outData = executeDerived(inTables, exec);
		// (3) return new POs (upcast from BDT automatic)
		return outData;

	}

	/**
	 * Sets a mapping from one direction to another. When {@code forward} is
	 * {@code true}, then {@code mapping} goes from the new row keys to the
	 * original row keys, else the opposite.
	 * 
	 * @param forward
	 *            Declares the direction of {@code mapping}.
	 * @param from
	 *            The input port index (from {@code 0}).
	 * @param to
	 *            The output port index (from {@code 0}).
	 * @param mapping
	 *            The mapping from output to input, or input to output depending
	 *            on the value of {@code forward} ({@code true}/{@code false}).
	 */
	protected void setMapping(final boolean forward, final int from,
			final int to, final Map<RowKey, Set<RowKey>> mapping) {
		assert forward;
		rowKeyForwardMapping.put(new Pair<Integer, Integer>(Integer
				.valueOf(from), Integer.valueOf(to)), mapping);
		final HiLiteTranslator hiLiteTranslator = translators.get(Integer
				.valueOf(from));
		if (getHiLite() != HiLite.NoHiLite) {
			hiLiteTranslator.setMapper(singleMapping.create(mapping));
		}
	}

	/**
	 * Adjust the HiLites according to the new state.
	 */
	private void adjustHiLite() {
		if (getHiLite() != HiLite.NoHiLite) {
			for (final Entry<Integer, List<Integer>> entry : connections
					.entrySet()) {
				final HiLiteTranslator hiLiteTranslator = translators.get(entry
						.getKey());
				for (final Integer other : connections.get(entry.getKey())) {
					hiLiteTranslator.setMapper(singleMapping
							.create(rowKeyForwardMapping
									.get(new Pair<Integer, Integer>(entry
											.getKey(), other))));
				}
			}
		}
	}

	/**
	 * Replaces the call of
	 * {@link NodeModel#execute(BufferedDataTable[], ExecutionContext)}. <br/>
	 * Override with real code if using only {@link BufferedDataTable}s, else
	 * with method throwing an {@link UnsupportedOperationException}.
	 * 
	 * @param inData
	 *            The input {@link BufferedDataTable data tables}.
	 * @param exec
	 *            The {@link ExecutionContext}.
	 * @return The result {@link BufferedDataTable data tables}.
	 * @throws Exception
	 *             Any problem.
	 * @see #executeDerived(PortObject[], ExecutionContext)
	 * @see NodeModel#execute(BufferedDataTable[], ExecutionContext)
	 */
	protected abstract BufferedDataTable[] executeDerived(
			BufferedDataTable[] inData, ExecutionContext exec) throws Exception;

	/**
	 * @return the HiLite behaviour
	 * @throws IllegalArgumentException
	 *             Wrong value set to the model.
	 */
	public HiLite getHiLite() throws IllegalArgumentException {
		return HiLite.valueOfDisplayText(hiLiteBehaviourModel.getStringValue());
	}

	@Override
	protected void saveInternals(final File nodeInternDir,
			final ExecutionMonitor exec) throws IOException,
			CanceledExecutionException {
		if (getHiLite() != HiLite.NoHiLite) {
			saveMapping(nodeInternDir, exec, forwardMappingSuffix,
					rowKeyForwardMapping);
		}
	}

	@Override
	protected void saveSettingsTo(final NodeSettingsWO settings) {
		hiLiteBehaviourModel.saveSettingsTo(settings);
	}

	@Override
	protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
			throws InvalidSettingsException {
		hiLiteBehaviourModel.loadSettingsFrom(settings);
	}

	@Override
	protected void validateSettings(final NodeSettingsRO settings)
			throws InvalidSettingsException {
		try {
			hiLiteBehaviourModel.validateSettings(settings);
		} catch (final InvalidSettingsException e) {
			if (settings instanceof NodeSettingsWO) {
				final NodeSettingsWO set = (NodeSettingsWO) settings;
				set.addString(TransformingNodeModel.CFGKEY_HILITE,
						TransformingNodeModel.DEFAULT_HILITE.getDisplayText());
			} else {
				throw e;
			}
		}
	}

	/**
	 * @param nodeInternDir
	 *            The node's internal folder.
	 * @param exec
	 *            The {@link ExecutionMonitor}.
	 * @param suffix
	 *            The suffix of file names.
	 * @param mapping
	 *            The mapping to save.
	 * @throws IOException
	 *             File write problem
	 * @throws CanceledExecutionException
	 *             Save cancelled
	 */
	private void saveMapping(final File nodeInternDir,
			final ExecutionMonitor exec, final String suffix,
			final Map<Pair<Integer, Integer>, Map<RowKey, Set<RowKey>>> mapping)
			throws IOException, CanceledExecutionException {
		for (final Entry<Pair<Integer, Integer>, Map<RowKey, Set<RowKey>>> entry : mapping
				.entrySet()) {
			final int fromIndex = entry.getKey().getFirst().intValue();
			final int toIndex = entry.getKey().getSecond().intValue();
			final File file = new File(nodeInternDir, generateFileName(
					fromIndex, toIndex, suffix));
			saveMapping(file, entry.getValue(), exec);
		}
	}

	/**
	 * @param file
	 *            The file where the table will be saved.
	 * @param map
	 *            The mapping to save.
	 * @param exec
	 *            The {@link ExecutionMonitor}.
	 * @throws CanceledExecutionException
	 *             Save cancelled
	 * @throws IOException
	 *             File write error
	 */
	private static void saveMapping(final File file,
			final Map<RowKey, Set<RowKey>> map, final ExecutionMonitor exec)
			throws IOException, CanceledExecutionException {
		int tableSize = 0;
		for (final Set<RowKey> vals : map.values()) {
			tableSize += vals.size();
		}
		final String[][] table = new String[tableSize][2];
		{
			int i = 0;
			for (final Entry<RowKey, Set<RowKey>> entry : map.entrySet()) {
				for (final RowKey val : entry.getValue()) {
					table[i][0] = entry.getKey().getString();
					table[i++][1] = val.getString();
				}

			}
		}
		final int tSize = tableSize;
		final DataTable defaultTable = new DataTable() {
			@Override
			public DataTableSpec getDataTableSpec() {
				return new DataTableSpec(new String[] { "key1", "keyN" },
						new DataType[] { StringCell.TYPE, StringCell.TYPE });
			}

			@Override
			public RowIterator iterator() {
				return new RowIterator() {
					private int i = 0;

					@Override
					public DataRow next() {
						return new DefaultRow(new RowKey(String.valueOf(i)),
								table[i++]);
					}

					@Override
					public boolean hasNext() {
						return i < tSize;
					}
				};
			}
		};
		DataContainer.writeToZip(defaultTable, file, exec);
	}

	/**
	 * @param fromIndex
	 *            The {@code 0}-based from index.
	 * @param toIndex
	 *            The {@code 0}-based to index.
	 * @param suffix
	 *            The suffix of the file name.
	 * @return The file name generated.
	 */
	protected static String generateFileName(final int fromIndex,
			final int toIndex, final String suffix) {
		return fromIndex + "_" + toIndex + "_" + suffix;
	}

	@Override
	protected void loadInternals(final File nodeInternDir,
			final ExecutionMonitor exec) throws IOException,
			CanceledExecutionException {
		if (getHiLite() != HiLite.NoHiLite) {
			logger.debug("loading mapping");
			loadMapping(nodeInternDir, forwardMappingSuffix,
					rowKeyForwardMapping, exec);
			adjustHiLite();
		}
	}

	/**
	 * @param nodeInternDir
	 *            The node's internal folder.
	 * @param suffix
	 *            The suffix of filename.
	 * @param mapping
	 *            The result mapping (should be empty, mutable).
	 * @param exec
	 *            The {@link ExecutionMonitor}.
	 * @throws IOException
	 *             Problem loading files.
	 */
	private void loadMapping(
			final File nodeInternDir,
			final String suffix,
			final Map<Pair<Integer, Integer>, Map<RowKey, Set<RowKey>>> mapping,
			final ExecutionMonitor exec) throws IOException {
		final File[] files = nodeInternDir.listFiles(new SuffixFilenameFilter(
				suffix));
		for (final File file : files) {
			final Matcher matcher = fileNamePattern.matcher(file.getName());
			if (matcher.matches()) {
				final Integer from = Integer.valueOf(matcher.group(1));
				final Integer to = Integer.valueOf(matcher.group(2));
				final Pair<Integer, Integer> pair = new Pair<Integer, Integer>(
						from, to);
				final HashMap<RowKey, Set<RowKey>> ret = new HashMap<RowKey, Set<RowKey>>();
				final ContainerTable table = DataContainer.readFromZip(file);
				for (final DataRow dataRow : table) {
					final RowKey key1 = new RowKey(((StringValue) dataRow
							.getCell(0)).getStringValue());
					if (!ret.containsKey(key1)) {
						ret.put(key1, new LinkedHashSet<RowKey>());
					}
					final Set<RowKey> set = ret.get(key1);
					set.add(new RowKey(((StringValue) dataRow.getCell(1))
							.getStringValue()));
				}
				mapping.put(pair, ret);
			}
		}
	}

	/**
	 * @return If HiLite is supported a new {@link HashMap}, else {@code null}.
	 */
	@Nullable
	protected Map<RowKey, Set<RowKey>> createMapping() {
		switch (getHiLite()) {
		case NoHiLite:
			return null;
		default:
			return new HashMap<RowKey, Set<RowKey>>();
		}
	}
}

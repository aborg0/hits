/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package ie.tcd.imm.knime.util;

//import javax.annotation.CheckReturnValue;
//import javax.annotation.Nonnull;

import ie.tcd.imm.util.SuffixFilenameFilter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.io.File;
import java.io.IOException;

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
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortType;
import org.knime.core.node.property.hilite.DefaultHiLiteMapper;
import org.knime.core.node.property.hilite.HiLiteHandler;
import org.knime.core.node.property.hilite.HiLiteListener;
import org.knime.core.node.property.hilite.HiLiteManager;
import org.knime.core.node.property.hilite.HiLiteTranslator;
import org.knime.core.node.property.hilite.KeyEvent;
import org.knime.core.util.Pair;

//import edu.umd.cs.findbugs.annotations.DefaultAnnotation;

/**
 * Common base class for the transforming {@link NodeModel}s. Supports HiLite.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
// @DefaultAnnotation( { Nonnull.class, CheckReturnValue.class })
public abstract class TransformingNodeModel extends NodeModel {

	/**
	 * 
	 */
	private final class HiLiteListenerMapper implements HiLiteListener {
		/**  */
		private final Integer inIndex;
		private final boolean forward;

		/**
		 * @param inIndex
		 * @param forward
		 */
		HiLiteListenerMapper(final Integer inIndex, final boolean forward) {
			super();
			this.inIndex = inIndex;
			this.forward = forward;
		}

		@SuppressWarnings("synthetic-access")
		private boolean checkHiLiteEnabled() {
			return getHiLite() != HiLite.NoHiLite
					&& !empty(connections.get(inIndex));
		}

		private boolean empty(final List<Integer> list) {
			return list != null && !list.isEmpty();
		}

		@SuppressWarnings("synthetic-access")
		@Override
		public void unHiLiteAll(final KeyEvent event) {
			if (checkHiLiteEnabled()) {
				for (final Integer out : connections.get(inIndex)) {
					getOutHiLiteHandler(out.intValue()).fireClearHiLiteEvent();
				}
			}
		}

		@Override
		public void unHiLite(final KeyEvent event) {
			hilite(event, false);
		}

		/**
		 * @param event
		 * @param hilite
		 */
		@SuppressWarnings("synthetic-access")
		private void hilite(final KeyEvent event, final boolean hilite) {
			if (checkHiLiteEnabled()) {
				for (final Entry<Pair<Integer, Integer>, Map<String, List<String>>> entry : (forward ? rowKeyBackwardMapping
						: rowKeyBackwardMapping).entrySet()) {
					final Map<String, List<String>> map = entry.getValue();
					final HashSet<RowKey> otherKeys = new HashSet<RowKey>();
					for (final RowKey key : event.keys()) {
						final List<String> list = map.get(key.getString());
						if (list != null) {
							for (final String otherKey : list) {
								otherKeys.add(new RowKey(otherKey));
							}
						}
					}
					final int otherIndex = entry.getKey().getSecond()
							.intValue();
					final HiLiteHandler outHiLiteHandler = forward ? getOutHiLiteHandler(otherIndex)
							: getInHiLiteHandler(otherIndex);
					if (hilite) {
						outHiLiteHandler.fireHiLiteEvent(otherKeys);
					} else {
						outHiLiteHandler.fireUnHiLiteEvent(otherKeys);
					}
				}
			}
		}

		@Override
		public void hiLite(final KeyEvent event) {
			hilite(event, true);
		}
	}

	/** Configuration key for the HiLite support. */
	public static final String CFGKEY_HILITE = "hilite";
	/** Default value for the HiLite behaviour. */
	public static final HiLite DEFAULT_HILITE = HiLite.NoHiLite;
	private final SettingsModelString hiLiteBehaviourModel = new SettingsModelString(
			TransformingNodeModel.CFGKEY_HILITE, DEFAULT_HILITE
					.getDisplayText());

	private static final Pattern fileNamePattern = Pattern
			.compile("(\\d+)_(\\d+)_[.]+");

	/** File name suffix for the forward mapping tables. */
	protected static final String forwardMappingSuffix = "ForwardMapping.zip";
	/** File name suffix for the backward mapping tables. */
	protected static final String backwardMappingSuffix = "BackwardMapping.zip";

	// The pair's first elem is the "from", the second is the "to" port index
	// (0-based), the value maps are mapping the row keys from "from" to "to".
	private final Map<Pair<Integer, Integer>, Map<RowKey, Set<RowKey>>> rowKeyForwardMapping = new HashMap<Pair<Integer, Integer>, Map<RowKey, Set<RowKey>>>();
	// The pair's first elem is the "from", the second is the "to" port index
	// (0-based), the value maps are mapping the row keys from "to" to "from".
	private final Map<Pair<Integer, Integer>, Map<String, List<String>>> rowKeyBackwardMapping = new HashMap<Pair<Integer, Integer>, Map<String, List<String>>>();

	/** inports &rArr; outports */
	private final Map<Integer, List<Integer>> connections = new HashMap<Integer, List<Integer>>();

	private final Map<Integer, HiLiteTranslator> translators = new HashMap<Integer, HiLiteTranslator>();
	private final Map<Integer, HiLiteManager> outHiliteHandlers = new HashMap<Integer, HiLiteManager>();

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
		addHiLiteHandlers(nrInDataPorts, nrOutDataPorts);
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
		addHiLiteHandlers(inPortTypes.length, outPortTypes.length);
	}

	private void addHiLiteHandlers(final int nrInDataPorts,
			final int nrOutDataPorts) {
		for (int i = nrInDataPorts; i-- > 0;) {
			translators.put(Integer.valueOf(i), new HiLiteTranslator());
		}
		for (int i = nrOutDataPorts; i-- > 0;) {
			final HiLiteManager manager = new HiLiteManager();
			for (final Entry<Integer, List<Integer>> entry : connections
					.entrySet()) {
				for (final Integer out : entry.getValue()) {
					if (out.intValue() == i) {
						manager.addToHiLiteHandler(translators.get(
								entry.getKey()).getFromHiLiteHandler());
					}
				}
			}
			outHiliteHandlers.put(Integer.valueOf(i), manager);
		}
		// for (int i = nrInDataPorts; i-- > 0;) {
		// final HiLiteHandler inHiLiteHandler = getInHiLiteHandler(i);
		// final Integer inIndex = Integer.valueOf(i);
		// inHiLiteHandler.addHiLiteListener(new HiLiteListenerMapper(inIndex,
		// true));
		// }
		// for (int i = nrOutDataPorts; i-- > 0;) {
		// final HiLiteHandler outHiLiteHandler = getOutHiLiteHandler(i);
		// outHiLiteHandler.addHiLiteListener(new HiLiteListenerMapper(Integer
		// .valueOf(i), false));
		// }
	}

	@Override
	protected void setInHiLiteHandler(final int inIndex,
			final HiLiteHandler hiLiteHdl) {
		final HiLiteTranslator hiLiteTranslator = translators.get(Integer
				.valueOf(inIndex));
		hiLiteTranslator.removeAllToHiliteHandlers();
		hiLiteTranslator.addToHiLiteHandler(hiLiteHdl);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.knime.core.node.NodeModel#getOutHiLiteHandler(int)
	 */
	@Override
	protected HiLiteHandler getOutHiLiteHandler(final int outIndex) {
		return // outHiliteHandlers.get(Integer.valueOf(outIndex))
		translators.get(Integer.valueOf(0))//
				.getFromHiLiteHandler();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.knime.core.node.NodeModel#execute(org.knime.core.node.BufferedDataTable
	 * [], org.knime.core.node.ExecutionContext)
	 */
	@Override
	protected final BufferedDataTable[] execute(
			final BufferedDataTable[] inData, final ExecutionContext exec)
			throws Exception {
		final BufferedDataTable[] ret = executeDerived(inData, exec);
		adjustHiLite();
		return ret;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.knime.core.node.NodeModel#execute(org.knime.core.node.port.PortObject
	 * [], org.knime.core.node.ExecutionContext)
	 */
	@Override
	protected PortObject[] execute(final PortObject[] inObjects,
			final ExecutionContext exec) throws Exception {
		final PortObject[] executeDerived = executeDerived(inObjects, exec);
		adjustHiLite();
		return executeDerived;
	}

	/**
	 * @param inObjects
	 * @param exec
	 * @return
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

	protected void setMapping(final boolean forward, final int from,
			final int to, final Map<RowKey, Set<RowKey>> mapping) {
		assert forward;
		rowKeyForwardMapping.put(new Pair<Integer, Integer>(Integer
				.valueOf(from), Integer.valueOf(to)), mapping);
	}

	/**
	 * 
	 */
	private void adjustHiLite() {
		if (getHiLite() != HiLite.NoHiLite) {
			for (final Entry<Integer, List<Integer>> entry : connections
					.entrySet()) {
				final HiLiteTranslator hiLiteTranslator = translators.get(entry
						.getKey());
				for (final Integer other : connections.get(entry.getKey())) {
					hiLiteTranslator.setMapper(new DefaultHiLiteMapper(
							rowKeyForwardMapping
									.get(new Pair<Integer, Integer>(entry
											.getKey(), other))));
				}
			}
		}
	}

	/**
	 * @param inData
	 * @param exec
	 * @return
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
			// saveMapping(nodeInternDir, exec, forwardMappingSuffix,
			// rowKeyForwardMapping);
			saveMapping(nodeInternDir, exec, backwardMappingSuffix,
					rowKeyBackwardMapping);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seeorg.knime.core.node.NodeModel#saveSettingsTo(org.knime.core.node.
	 * NodeSettingsWO)
	 */
	@Override
	protected void saveSettingsTo(final NodeSettingsWO settings) {
		hiLiteBehaviourModel.saveSettingsTo(settings);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.knime.core.node.NodeModel#loadValidatedSettingsFrom(org.knime.core
	 * .node.NodeSettingsRO)
	 */
	@Override
	protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
			throws InvalidSettingsException {
		hiLiteBehaviourModel.loadSettingsFrom(settings);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seeorg.knime.core.node.NodeModel#validateSettings(org.knime.core.node.
	 * NodeSettingsRO)
	 */
	@Override
	protected void validateSettings(final NodeSettingsRO settings)
			throws InvalidSettingsException {
		hiLiteBehaviourModel.validateSettings(settings);
	}

	/**
	 * @param nodeInternDir
	 * @param exec
	 * @param suffix
	 * @param mapping
	 * @throws IOException
	 * @throws CanceledExecutionException
	 */
	private void saveMapping(final File nodeInternDir,
			final ExecutionMonitor exec, final String suffix,
			final Map<Pair<Integer, Integer>, Map<String, List<String>>> mapping)
			throws IOException, CanceledExecutionException {
		for (final Entry<Pair<Integer, Integer>, Map<String, List<String>>> entry : mapping
				.entrySet()) {
			final int fromIndex = entry.getKey().getFirst().intValue();
			final int toIndex = entry.getKey().getSecond().intValue();
			final File file = new File(nodeInternDir, generateFileName(
					fromIndex, toIndex, suffix));
			saveMaping(file, entry.getValue(), exec);
		}
	}

	/**
	 * @param file
	 * @param mapping
	 * @param exec
	 * @throws CanceledExecutionException
	 * @throws IOException
	 */
	private static void saveMaping(final File file,
			final Map<String, List<String>> mapping, final ExecutionMonitor exec)
			throws IOException, CanceledExecutionException {
		int tableSize = 0;
		for (final List<String> vals : mapping.values()) {
			tableSize += vals.size();
		}
		final String[][] table = new String[tableSize][2];
		{
			int i = 0;
			for (final Entry<String, List<String>> entry : mapping.entrySet()) {
				for (final String val : entry.getValue()) {
					table[i][0] = entry.getKey();
					table[i++][1] = val;
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
			// loadMapping(nodeInternDir, forwardMappingSuffix,
			// rowKeyForwardMapping, exec);
			loadMapping(nodeInternDir, backwardMappingSuffix,
					rowKeyBackwardMapping, exec);
		}
	}

	/**
	 * @param nodeInternDir
	 * @param suffix
	 * @param mapping
	 * @param exec
	 * @throws IOException
	 */
	private void loadMapping(
			final File nodeInternDir,
			final String suffix,
			final Map<Pair<Integer, Integer>, Map<String, List<String>>> mapping,
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
				final HashMap<String, List<String>> ret = new HashMap<String, List<String>>();
				final ContainerTable table = DataContainer.readFromZip(file);
				for (final DataRow dataRow : table) {
					final String key1 = ((StringValue) dataRow.getCell(0))
							.getStringValue();
					if (!ret.containsKey(key1)) {
						ret.put(key1, new ArrayList<String>());
					}
					final List<String> list = ret.get(dataRow.getKey()
							.getString());
					list.add(((StringValue) dataRow.getCell(1))
							.getStringValue());
				}
				mapping.put(pair, ret);
			}
		}
	}
}

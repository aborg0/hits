package ie.tcd.imm.hits.image.loci.read;

import ie.tcd.imm.hits.common.PublicConstants;
import ie.tcd.imm.hits.image.loci.LociReaderCell;
import ie.tcd.imm.hits.util.Misc;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import loci.formats.ChannelSeparator;
import loci.formats.FormatReader;
import loci.formats.IFormatReader;
import loci.formats.ImageReader;
import loci.formats.MetadataTools;
import loci.formats.gui.ExtensionFileFilter;
import loci.formats.meta.IMetadata;
import loci.formats.meta.MetadataRetrieve;
import loci.formats.ome.OMEXML200809Metadata;
import loci.formats.ome.OMEXMLMetadata;
import loci.plugins.util.ImagePlusReader;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnDomainCreator;
import org.knime.core.data.DataColumnProperties;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.RowKey;
import org.knime.core.data.collection.CollectionCellFactory;
import org.knime.core.data.collection.ListCell;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataContainer;
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
 * This is the model implementation of OMEReader. This node reads image
 * information in OME format.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
public class LociReaderNodeModel extends NodeModel {
	private static NodeLogger logger = NodeLogger
			.getLogger(LociReaderNodeModel.class);
	/** The key for units in the result table properties. */
	public static final String UNIT = "Unit";
	/** The configuration key for folder model. */
	static final String CFGKEY_FOLDER = "folder";
	/** The default value for folder model. */
	static final String DEFAULT_FOLDER = new File(System
			.getProperty("user.home")).getAbsoluteFile().toURI().toString();

	/** The configuration key for the extensions to use. */
	static final String CFGKEY_EXTENSION = "extension";
	/** The default value for the extensions to use. */
	static final String DEFAULT_EXTENSION = "xdce";
	/** The allowed extensions. */
	static final ArrayList<String> ALLOWED_EXTENSIONS = LociReaderNodeDialog
			.computeExtensions();

	private final SettingsModelString folder = new SettingsModelString(
			CFGKEY_FOLDER, DEFAULT_FOLDER);
	private final SettingsModelString extensions = new SettingsModelString(
			CFGKEY_EXTENSION, DEFAULT_EXTENSION);

	/**
	 * Constructor for the node model.
	 */
	protected LociReaderNodeModel() {
		super(0, 3);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
			final ExecutionContext exec) throws Exception {
		String timeUnit = null;
		String zUnit = null;
		final BufferedDataContainer xmlPlateContainer = exec
				.createDataContainer(createXmlPlateSpec());
		final BufferedDataContainer plateContainer = exec
				.createDataContainer(createPlateSpec());
		try {
			final ImagePlusReader reader = ImagePlusReader
					.makeImagePlusReader(new ChannelSeparator(ImagePlusReader
							.makeImageReader()));
			exec.checkCanceled();
			final IMetadata omeXml = new OMEXML200809Metadata();
			// OMEXMLFactory.newOMENode();
			// MetadataTools.createOMEXMLMetadata("");
			omeXml.createRoot();
			reader.setMetadataStore(omeXml);
			reader.setMetadataFiltered(true);
			reader.setOriginalMetadataPopulated(true);

			final String rawFolder = folder.getStringValue();
			final File f = new File(rawFolder);
			String extension;
			File folderFile;
			if (f.isFile()) {
				folderFile = f;
				extension = "";
			} else {
				folderFile = new File(rawFolder);
				extension = extensions.getStringValue();
			}
			int progress = 0;
			final URI[] files = visit(folderFile, extension);
			for (final URI file : files) {
				exec.checkCanceled();
				exec.setProgress(progress * 1.0 / files.length, "Loading: "
						+ file.getPath());
				reader.setId(file.getPath());
				logger.debug("loaded: " + file);
				final IFormatReader formatReader = ((ImageReader) ((ChannelSeparator) reader
						.getReader()).getReader()).getReader();
				final int colCount = getPrivateField(formatReader, "wellCols",
						1);
				final int rowCount = getPrivateField(formatReader, "wellRows",
						1);
				final int fieldCount = getPrivateField(formatReader,
						"fieldCount", formatReader.getSeriesCount() / rowCount
								/ colCount);
				final String relPos = new File(file).getAbsolutePath()
						.substring(folderFile.getAbsolutePath().length());
				final String xml = ((OMEXMLMetadata) MetadataTools
						.asRetrieve(formatReader.getMetadataStore())).dumpXML();
				xmlPlateContainer.addRowToTable(new DefaultRow(new RowKey(
						relPos), new StringCell(relPos), DataType
						.getMissingCell()/* Plate */,
						DataType.getMissingCell()/* Row */, DataType
								.getMissingCell()/* column */, DataType
								.getMissingCell()/* Z */, DataType
								.getMissingCell()/* C */, DataType
								.getMissingCell()/* T */, new LociReaderCell(
								(FormatReader) formatReader),// 
						new StringCell(xml), new StringCell(relPos)));
				final Collection<StringCell> channelNames = asStringDataCells(getChannelNames(formatReader));
				final String currentTimeUnit = getTimeUnit(reader);
				if (timeUnit == null) {
					timeUnit = currentTimeUnit;
				}
				final String currentZUnit = getZUnit(reader);
				if (zUnit == null) {
					zUnit = currentZUnit;
				}
				for (int i = 0; i < reader.getSeriesCount(); ++i) {
					exec.checkCanceled();
					exec.setProgress((progress + i
							/ (double) reader.getSeriesCount())
							/ files.length);
					reader.setSeries(i);
					if (Runtime.getRuntime().freeMemory() < 1000000) {
						exec.setMessage("stopped because of not enough memory");
						logger.warn("stopped because of not enough memory");
						break;
					}
					final Collection<DoubleCell> timeCells = asDoubleDataCells(getTimes(
							MetadataTools.asRetrieve(formatReader
									.getMetadataStore()), i, formatReader
									.getSizeT(), multiplierLinear(timeUnit,
									currentTimeUnit)));
					final Collection<DoubleCell> zCells = asDoubleDataCells(getZs(
							MetadataTools.asRetrieve(formatReader
									.getMetadataStore()), i, formatReader
									.getSizeZ(), multiplierLinear(zUnit,
									currentZUnit)));
					// final Integer timepoint = omeXml.getWellSampleTimepoint(
					// 0/* plate */, 0 /* well */, 0/* field */);
					// final Integer z = omeXml.getTiffDataFirstZ(0/* imageIdx
					// */,
					// 0/* pixelsIndex */, 0/* tiffDataIndex */);
					// final int imageCount = reader.getSizeZ()
					// * reader.getSizeT();// * reader.getEffectiveSizeC();
					// for (int j = 0; j < imageCount; ++j) {
					// final int[] zctCoords = FormatTools.getZCTCoords(
					// reader, j);
					plateContainer.addRowToTable(new DefaultRow(
							new RowKey("Row_" + relPos + "_" + i
							// + "_" + j
							),
							new StringCell(relPos),
							new StringCell(Misc.toUpperLetter(Integer
									.toString(i / fieldCount / colCount + 1))),
							new IntCell(i / fieldCount % colCount + 1),
							new IntCell(i % fieldCount + 1),
							// Z
							CollectionCellFactory.createListCell(zCells),
							// T
							CollectionCellFactory.createListCell(timeCells),// new
							// DoubleCell(zctCoords[2]),
							// C
							CollectionCellFactory.createListCell(channelNames),
							new StringCell(relPos), new IntCell(i)));
					// }
					if (i % 100 == 0) {
						logger.debug("i: " + i);
					}
				}
				++progress;
			}
		} finally {
			plateContainer.close();
			xmlPlateContainer.close();
		}
		final BufferedDataContainer experimentContainer = exec
				.createDataContainer(createExperimentDescriptionSpec());
		try {

		} finally {
			experimentContainer.close();
		}
		final BufferedDataTable table = plateContainer.getTable();
		final BufferedDataTable tableWithDomain = exec.createSpecReplacerTable(
				table, addValues(table, timeUnit, zUnit));
		return new BufferedDataTable[] { tableWithDomain,
				xmlPlateContainer.getTable(), experimentContainer.getTable() };
	}

	private static final Map<String, Double> siPrefices = new HashMap<String, Double>();
	static {
		final String[] prefices = new String[] { "y", "z", "a", "f", "p", "n",
				"\u00B5", "m", "", "k", "M", "G", "T", "P", "E", "Z", "Y" };
		for (int i = prefices.length; i-- > 0;) {
			siPrefices.put(prefices[i], Math.pow(10, i * 3 - 24));
		}
		siPrefices.put("c", .01);// centi
		siPrefices.put("d", .1);// deci
	}

	/**
	 * Computes the proper multiplier based on the prefixes.
	 * 
	 * @param unit
	 *            The unit to convert to.
	 * @param currentUnit
	 *            The unit to convert from.
	 * @return The current prefix divided by the unit prefix.
	 */
	private final double multiplierLinear(final String unit,
			final String currentUnit) {
		if (unit.equals(currentUnit)) {
			return 1.0;
		}
		String unitName;
		if (unit.length() == 1) {
			unitName = unit;
		} else {
			unitName = unit.substring(1);
		}
		if (!currentUnit.endsWith(unitName)) {
			throw new IllegalArgumentException("Incompatible units: " + unit
					+ ", " + currentUnit);
		}
		final String unitPrefix = getPrefix(unit, unitName);
		final String currentUnitPrefix = getPrefix(currentUnit, unitName);
		try {
			final double unitPref = siPrefices.get(unitPrefix);
			final double currentPref = siPrefices.get(currentUnitPrefix);
			return currentPref / unitPref;
		} catch (final NullPointerException e) {
			throw new IllegalStateException("Unkown prefix: " + unitPrefix
					+ " or " + currentUnitPrefix);
		}
	}

	/**
	 * Selects the SI prefix of a unit.
	 * 
	 * @param unit
	 *            A unit of measure (with SI prefix).
	 * @param unitName
	 *            The name of the unit (with no SI prefix).
	 * @return The SI prefix {@link String}.
	 */
	private static String getPrefix(final String unit, final String unitName) {
		final String unitPrefix = unit.substring(0, unit.length()
				- unitName.length());
		return unitPrefix;
	}

	/**
	 * Selects the Z coordinate's unit.
	 * 
	 * @param reader
	 *            An {@link IFormatReader}.
	 * @return The {@link String} representation of the Z coordinate unit.
	 */
	private final String getZUnit(final IFormatReader reader) {
		return "\u00B5m";
	}

	/**
	 * Selects the time coordinate's unit.
	 * 
	 * @param reader
	 *            An {@link IFormatReader}.
	 * @return The {@link String} representation of the time coordinate unit.
	 */
	private final String getTimeUnit(final IFormatReader reader) {
		return "s";
	}

	/**
	 * Adds domain values to selected columns.
	 * 
	 * @param table
	 *            A table with plate, channel, time columns.
	 * @param zUnit
	 *            The unit for the Z values.
	 * @param timeUnit
	 *            The unit for the time values.
	 * @return The same {@link DataTableSpec} with added domain of the values in
	 *         plate, channel, time columns.
	 */
	private static final DataTableSpec addValues(final BufferedDataTable table,
			final String timeUnit, final String zUnit) {
		final DataTableSpec tableSpec = table.getDataTableSpec();
		final DataColumnSpec[] colSpecs = new DataColumnSpec[tableSpec
				.getNumColumns()];
		final Set<String> selectedNames = new HashSet<String>(Arrays.asList(
				PublicConstants.LOCI_PLATE, PublicConstants.LOCI_Z,
				PublicConstants.LOCI_TIME, PublicConstants.LOCI_CHANNELS,
				PublicConstants.LOCI_ID));
		final Map<Integer, Set<DataCell>> map = new HashMap<Integer, Set<DataCell>>();
		for (int i = colSpecs.length; i-- > 0;) {
			colSpecs[i] = tableSpec.getColumnSpec(i);
			if (selectedNames.contains(colSpecs[i].getName())) {
				map.put(Integer.valueOf(i), new LinkedHashSet<DataCell>());
			}
		}
		for (final DataRow dataRow : table) {
			for (final Entry<Integer, Set<DataCell>> entry : map.entrySet()) {
				final DataCell cell = dataRow
						.getCell(entry.getKey().intValue());
				entry.getValue().add(cell);
			}
		}
		for (final Entry<Integer, Set<DataCell>> entry : map.entrySet()) {
			final int index = entry.getKey().intValue();
			final DataColumnSpecCreator creator = new DataColumnSpecCreator(
					colSpecs[index]);
			creator.setDomain(new DataColumnDomainCreator(entry.getValue())
					.createDomain());
			if (colSpecs[index].getName().equals(PublicConstants.LOCI_TIME)) {
				creator.setProperties(new DataColumnProperties(Collections
						.singletonMap(UNIT, timeUnit)));
			} else if (colSpecs[index].getName().equals(PublicConstants.LOCI_Z)) {
				creator.setProperties(new DataColumnProperties(Collections
						.singletonMap(UNIT, zUnit)));
			}
			colSpecs[index] = creator.createSpec();
		}
		return new DataTableSpec(colSpecs);
	}

	/**
	 * Converts {@code strings} to {@link StringCell}s.
	 * 
	 * @param strings
	 *            Some {@link String}s.
	 * @return A list of {@link StringCell}s.
	 */
	private static final Collection<StringCell> asStringDataCells(
			final Collection<String> strings) {
		final Collection<StringCell> ret = new ArrayList<StringCell>(strings
				.size());
		for (final String channelName : strings) {
			ret.add(new StringCell(channelName));
		}
		return ret;
	}

	/**
	 * Converts {@code numbers} to {@link DoubleCell}s.
	 * 
	 * @param numbers
	 *            Some {@link Number}s.
	 * @return A list of {@link DoubleCell}s.
	 */
	private static final Collection<DoubleCell> asDoubleDataCells(
			final Collection<? extends Number> numbers) {
		final Collection<DoubleCell> ret = new ArrayList<DoubleCell>(numbers
				.size());
		for (final Number number : numbers) {
			ret.add(new DoubleCell(number.doubleValue()));
		}
		return ret;
	}

	/**
	 * @param reader
	 *            An {@link IFormatReader}.
	 * @return The logical channel names.
	 */
	public static final List<String> getChannelNames(final IFormatReader reader) {
		return getChannelNames(MetadataTools.asRetrieve(reader
				.getMetadataStore()), reader.getSizeC());
	}

	/**
	 * @param metadata
	 *            A {@link MetadataRetrieve}.
	 * @param sizeC
	 *            The effective channel count.
	 * @return The logical channel names.
	 */
	public static final List<String> getChannelNames(
			final MetadataRetrieve metadata, final int sizeC) {
		final List<String> ret = new ArrayList<String>(sizeC);
		for (int i = 0; i < sizeC; ++i) {
			ret.add(metadata.getLogicalChannelName(0, i));
		}
		return ret;
	}

	/**
	 * @param metadata
	 *            A {@link MetadataRetrieve}.
	 * @param serie
	 *            The well number.
	 * @param sizeT
	 *            The number of points in the T dimension.
	 * @param multiplier
	 *            The value to multiply the results to have the proper unit.
	 * @return The time points.
	 */
	private final Collection<? extends Number> getTimes(
			final MetadataRetrieve metadata, final int serie, final int sizeT,
			final double multiplier) {
		final List<Number> ret = new ArrayList<Number>(sizeT);
		for (int i = 0; i < sizeT; ++i) {
			final double diff = metadata.getPlaneTimingDeltaT(serie, 0, i)
					- metadata.getPlaneTimingDeltaT(serie, 0, 0);
			ret.add(Double.valueOf(diff * multiplier));
		}
		return ret;
	}

	/**
	 * @param metadata
	 *            A {@link MetadataRetrieve}.
	 * @param serie
	 *            The well number.
	 * @param sizeZ
	 *            The number of points in the Z dimension.
	 * @param multiplier
	 *            The value to multiply the results to have the proper unit.
	 * @return The Z coordinates points.
	 */
	private final Collection<? extends Number> getZs(
			final MetadataRetrieve metadata, final int serie, final int sizeZ,
			final double multiplier) {
		final List<Number> ret = new ArrayList<Number>(sizeZ);
		for (int i = 0; i < sizeZ; ++i) {
			ret.add(Double.valueOf(metadata.getStagePositionPositionZ(serie, 0,
					i)
					* multiplier));
		}
		return ret;
	}

	/**
	 * @param formatReader
	 *            A {@link FormatReader}
	 * @param fieldName
	 *            the name of the field.
	 * @param defaultValue
	 *            The default value if not found.
	 * @return The value of the field.
	 */
	private static final int getPrivateField(final IFormatReader formatReader,
			final String fieldName, final int defaultValue) {
		int ret;

		try {
			if (formatReader.getClass().getDeclaredField(fieldName) != null) {
				final Field field = formatReader.getClass().getDeclaredField(
						fieldName);
				field.setAccessible(true);
				ret = field.getInt(formatReader);
			} else {
				ret = defaultValue;
			}
		} catch (final SecurityException e) {
			return defaultValue;
		} catch (final IllegalArgumentException e) {
			return defaultValue;
		} catch (final NoSuchFieldException e) {
			return defaultValue;
		} catch (final IllegalAccessException e) {
			return defaultValue;
		}
		return ret;
	}

	private final URI[] visit(final File file, final String extension) {
		if (file.isFile()) {
			return new URI[] { file.toURI() };
		}
		final String[] exts = extension.split("\\|");
		for (int i = exts.length; i-- > 0;) {
			while (exts[i].length() > 0 && exts[i].charAt(0) == '.') {
				exts[i] = exts[i].substring(1);
			}
		}
		final ExtensionFileFilter fileFilter = new ExtensionFileFilter(exts, "");
		final ArrayList<String> contents = new ArrayList<String>();
		visit(file, file, contents, fileFilter);
		final URI[] uris = new URI[contents.size()];
		for (int j = 0; j < uris.length; j++) {
			uris[j] = new File(file, contents.get(j)).toURI();
		}
		return uris;
	}

	/**
	 * Adds the matching relative paths to {@code contents}.
	 * 
	 * @param origParent
	 *            The original parent of files.
	 * @param parent
	 *            The current parent file.
	 * @param contents
	 *            The results collected here.
	 * @param fileFilter
	 *            The {@link FileFilter} used to filter files.
	 */
	public static void visit(final File origParent, final File parent,
			final List<String> contents, final java.io.FileFilter fileFilter) {
		final String origPath = origParent.getAbsolutePath();
		if (parent.isFile() && fileFilter.accept(parent)) {
			addFile(parent, origPath, contents);
		}
		final File[] listFiles = parent.listFiles(fileFilter);
		if (listFiles == null) {
			return;
		}
		for (final File file : listFiles) {
			addFile(file, origPath, contents);
		}
		for (final File possFolder : parent.listFiles()) {
			if (possFolder.isDirectory()) {
				visit(origParent, possFolder, contents, fileFilter);
			}
		}
	}

	/**
	 * Adds the relative path of {@code file} to {@code contents}.
	 * 
	 * @param file
	 *            The file to add to {@code contents}.
	 * @param origPath
	 *            The original path (from which it is relative to).
	 * @param contents
	 *            The list of relative paths.
	 */
	public static void addFile(final File file, final String origPath,
			final List<String> contents) {
		final String absolutePath = file.getAbsolutePath();
		if (file.isFile() && absolutePath.startsWith(origPath)) {
			contents.add(absolutePath.substring(origPath.length()));
		}
	}

	private static DataTableSpec createExperimentDescriptionSpec() {
		return new DataTableSpec();
	}

	private static DataTableSpec createXmlPlateSpec() {
		return new DataTableSpec(new DataColumnSpecCreator(
				PublicConstants.LOCI_PLATE, StringCell.TYPE).createSpec(),
				new DataColumnSpecCreator(PublicConstants.LOCI_ROW,
						StringCell.TYPE).createSpec(),
				new DataColumnSpecCreator(PublicConstants.LOCI_COLUMN,
						IntCell.TYPE).createSpec(), new DataColumnSpecCreator(
						PublicConstants.LOCI_FIELD, IntCell.TYPE).createSpec(),
				new DataColumnSpecCreator(PublicConstants.LOCI_Z, ListCell
						.getCollectionType(DoubleCell.TYPE)).createSpec(),
				new DataColumnSpecCreator(PublicConstants.LOCI_TIME, ListCell
						.getCollectionType(DoubleCell.TYPE)).createSpec(),
				new DataColumnSpecCreator(PublicConstants.LOCI_CHANNELS,
						ListCell.getCollectionType(StringCell.TYPE))
						.createSpec(), new DataColumnSpecCreator(
						PublicConstants.LOCI_IMAGE_CONTENT, DataType
								.getType(LociReaderCell.class)).createSpec(),
				new DataColumnSpecCreator(PublicConstants.LOCI_XML,
						StringCell.TYPE).createSpec(),
				new DataColumnSpecCreator(PublicConstants.LOCI_ID,
						StringCell.TYPE).createSpec());
	}

	private static DataTableSpec createPlateSpec() {
		return new DataTableSpec(new DataColumnSpecCreator(
				PublicConstants.LOCI_PLATE, StringCell.TYPE).createSpec(),
				new DataColumnSpecCreator(PublicConstants.LOCI_ROW,
						StringCell.TYPE).createSpec(),
				new DataColumnSpecCreator(PublicConstants.LOCI_COLUMN,
						IntCell.TYPE).createSpec(), new DataColumnSpecCreator(
						PublicConstants.LOCI_FIELD, IntCell.TYPE).createSpec(),
				new DataColumnSpecCreator(PublicConstants.LOCI_Z, ListCell
						.getCollectionType(DoubleCell.TYPE)).createSpec(),
				new DataColumnSpecCreator(PublicConstants.LOCI_TIME, ListCell
						.getCollectionType(DoubleCell.TYPE)).createSpec(),
				new DataColumnSpecCreator(PublicConstants.LOCI_CHANNELS,
						ListCell.getCollectionType(StringCell.TYPE))
						.createSpec(), new DataColumnSpecCreator(
						PublicConstants.LOCI_ID, StringCell.TYPE).createSpec(),
				new DataColumnSpecCreator(PublicConstants.IMAGE_ID,
						IntCell.TYPE).createSpec());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void reset() {
		// Do nothing.
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
			throws InvalidSettingsException {
		return new DataTableSpec[] { createPlateSpec(), createXmlPlateSpec(),
				createExperimentDescriptionSpec() };
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveSettingsTo(final NodeSettingsWO settings) {
		folder.saveSettingsTo(settings);
		extensions.saveSettingsTo(settings);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
			throws InvalidSettingsException {
		folder.loadSettingsFrom(settings);
		extensions.loadSettingsFrom(settings);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void validateSettings(final NodeSettingsRO settings)
			throws InvalidSettingsException {
		folder.validateSettings(settings);
		extensions.validateSettings(settings);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadInternals(final File internDir,
			final ExecutionMonitor exec) throws IOException,
			CanceledExecutionException {
		// No internal state.
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveInternals(final File internDir,
			final ExecutionMonitor exec) throws IOException,
			CanceledExecutionException {
		// No internal state.
	}
}

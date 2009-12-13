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
import java.util.List;

import loci.formats.ChannelSeparator;
import loci.formats.FormatReader;
import loci.formats.IFormatReader;
import loci.formats.ImageReader;
import loci.plugins.util.ImagePlusReader;

import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DefaultRow;
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

import visad.util.ExtensionFileFilter;

/**
 * This is the model implementation of OMEReader. This node reads image
 * information in OME format.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
public class LociReaderNodeModel extends NodeModel {
	private static NodeLogger logger = NodeLogger
			.getLogger(LociReaderNodeModel.class);
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

	private static final SettingsModelString folder = new SettingsModelString(
			CFGKEY_FOLDER, DEFAULT_FOLDER);
	private static final SettingsModelString extensions = new SettingsModelString(
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
		final BufferedDataContainer xmlPlateContainer = exec
				.createDataContainer(createXmlPlateSpec());
		final BufferedDataContainer plateContainer = exec
				.createDataContainer(createPlateSpec());
		try {
			// new ImageInfo().testRead(new String[] {
			// folder.getStringValue(),
			// "-omexml", "-nopix", "-nometa", "-nocore" });
			final ImagePlusReader reader = ImagePlusReader
					.makeImagePlusReader(new ChannelSeparator(ImagePlusReader
							.makeImageReader()));
			reader.setMetadataFiltered(true);
			reader.setOriginalMetadataPopulated(false);
			// final IMetadata omeXml = MetadataTools.createOMEXMLMetadata();
			// reader.setMetadataStore(new DummyMetadata());
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
			for (final URI file : visit(folderFile, extension)) {
				reader.setId(file.getPath());
				logger.debug("loaded: " + file);
				final IFormatReader formatReader = ((ImageReader) ((ChannelSeparator) reader
						.getReader()).getReader()).getReader();
				// if (formatReader.getClass().getField("channelNames") !=null)
				// {
				//					
				// }
				final int colCount = getPrivateField(formatReader, "wellCols",
						1);
				final int rowCount = getPrivateField(formatReader, "wellRows",
						1);
				final int fieldCount = getPrivateField(formatReader,
						"fieldCount", formatReader.getSeriesCount() / rowCount
								/ colCount);
				final String relPos = new File(file).getAbsolutePath()
						.substring(folderFile.getAbsolutePath().length());
				xmlPlateContainer
						.addRowToTable(new DefaultRow(
								new RowKey(relPos),
								new StringCell(relPos),
								DataType.getMissingCell()/* Plate */,
								DataType.getMissingCell()/* Row */,
								DataType.getMissingCell()/* column */,
								new LociReaderCell(
										(FormatReader) ((ImageReader) ((ChannelSeparator) reader
												.getReader()).getReader())
												.getReader()),
								// new
								// StringCell(MetadataTools.getOMEXML(MetadataTools
								// .asRetrieve(reader.getMetadataStore()))),
								new StringCell(relPos)));
				for (int i = 0; i < reader.getSeriesCount(); ++i) {
					exec.checkCanceled();
					reader.setSeries(i);
					if (Runtime.getRuntime().freeMemory() < 1000000) {
						exec.setMessage("stopped because of not enough memory");
						logger.warn("stopped because of not enough memory");
						break;
					}
					plateContainer.addRowToTable(new DefaultRow(
							new RowKey("Row_" + i),
							new StringCell(relPos),
							// new StringCell(Misc.toUpperLetter(Integer
							// .toString(i / 8 / 12 + 1))), new IntCell(
							// i / 8 % 12 + 1), new IntCell(i % 8 + 1),
							new StringCell(Misc.toUpperLetter(Integer
									.toString(i / fieldCount / colCount + 1))),
							new IntCell(i / fieldCount % colCount + 1),
							new IntCell(i % fieldCount + 1), new StringCell(
									relPos), new IntCell(i)));
					if (i % 100 == 0) {
						logger.debug("i: " + i);
					}
				}
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
		return new BufferedDataTable[] { plateContainer.getTable(),
				xmlPlateContainer.getTable(), experimentContainer.getTable() };
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
	private static int getPrivateField(final IFormatReader formatReader,
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

	private URI[] visit(final File file, final String extension) {
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
				new DataColumnSpecCreator(PublicConstants.LOCI_IMAGE_CONTENT,
						DataType.getType(LociReaderCell.class)).createSpec(),
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
				new DataColumnSpecCreator(PublicConstants.LOCI_ID,
						StringCell.TYPE).createSpec(),
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
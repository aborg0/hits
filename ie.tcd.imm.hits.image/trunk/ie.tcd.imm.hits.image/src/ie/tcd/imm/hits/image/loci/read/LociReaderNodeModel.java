package ie.tcd.imm.hits.image.loci.read;

import ie.tcd.imm.hits.image.loci.OMEReaderCell;

import java.io.File;
import java.io.IOException;
import java.net.URI;

import loci.formats.ChannelSeparator;
import loci.formats.FormatReader;
import loci.formats.ImageReader;
import loci.formats.meta.DummyMetadata;
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

/**
 * This is the model implementation of OMEReader. This node reads image
 * information in OME format.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
public class LociReaderNodeModel extends NodeModel {
	private static NodeLogger logger = NodeLogger
			.getLogger(LociReaderNodeModel.class);

	static final String CFGKEY_FOLDER = "folder";
	static final String DEFAULT_FOLDER = new File(System
			.getProperty("user.home")).getAbsoluteFile().toURI().toString();

	private static final SettingsModelString folder = new SettingsModelString(
			CFGKEY_FOLDER, DEFAULT_FOLDER);

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
					.makeImagePlusReader(ChannelSeparator
							.makeChannelSeparator(ImagePlusReader
									.makeImageReader()));
			reader.setMetadataFiltered(true);
			reader.setOriginalMetadataPopulated(false);
			// final IMetadata omeXml = MetadataTools.createOMEXMLMetadata();
			reader.setMetadataStore(new DummyMetadata());
			for (final URI file : visit(new File(folder.getStringValue()))) {
				reader.setId(file.getPath());
				logger.debug("loaded: " + file);
				xmlPlateContainer
						.addRowToTable(new DefaultRow(
								new RowKey(new File(file).getName()),
								new StringCell(file.getPath()),
								DataType.getMissingCell()/* Plate */,
								DataType.getMissingCell()/* Row */,
								DataType.getMissingCell()/* column */,
								new OMEReaderCell(
										(FormatReader) ((ImageReader) ((ChannelSeparator) reader
												.getReader()).getReader())
												.getReader()),
								// new
								// StringCell(MetadataTools.getOMEXML(MetadataTools
								// .asRetrieve(reader.getMetadataStore()))),
								new StringCell(file.getPath())));
				for (int i = 0; i < reader.getSeriesCount(); ++i) {
					exec.checkCanceled();
					reader.setSeries(i);
					if (Runtime.getRuntime().freeMemory() < 1000000) {
						logger.warn("stopped because of not enough memory");
						break;
					}
					plateContainer.addRowToTable(new DefaultRow(new RowKey(
							"Row_" + i), new StringCell(file.getPath()),
							new StringCell(String.valueOf('A' + i / 8 / 12)),
							new IntCell(i / 8 % 12), new IntCell(i % 8),
							new StringCell(file.getPath()), new IntCell(i)));
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

	private URI[] visit(final File file) {
		return new URI[] { file.toURI() };
	}

	private static DataTableSpec createExperimentDescriptionSpec() {
		return new DataTableSpec();
	}

	private static DataTableSpec createXmlPlateSpec() {
		return new DataTableSpec(new DataColumnSpecCreator("Plate",
				StringCell.TYPE).createSpec(), new DataColumnSpecCreator("Row",
				StringCell.TYPE).createSpec(), new DataColumnSpecCreator(
				"Column", IntCell.TYPE).createSpec(),
				new DataColumnSpecCreator("Field", IntCell.TYPE).createSpec(),
				new DataColumnSpecCreator("OME", DataType
						.getType(OMEReaderCell.class)).createSpec(),
				new DataColumnSpecCreator("OME-id", StringCell.TYPE)
						.createSpec());
	}

	private static DataTableSpec createPlateSpec() {
		return new DataTableSpec(new DataColumnSpecCreator("Plate",
				StringCell.TYPE).createSpec(), new DataColumnSpecCreator("Row",
				StringCell.TYPE).createSpec(), new DataColumnSpecCreator(
				"Column", IntCell.TYPE).createSpec(),
				new DataColumnSpecCreator("Field", IntCell.TYPE).createSpec(),
				new DataColumnSpecCreator("OME-id", StringCell.TYPE)
						.createSpec(), new DataColumnSpecCreator("Image-id",
						IntCell.TYPE).createSpec());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void reset() {
		// TODO: generated method stub
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
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
			throws InvalidSettingsException {
		folder.loadSettingsFrom(settings);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void validateSettings(final NodeSettingsRO settings)
			throws InvalidSettingsException {
		folder.validateSettings(settings);
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

package ie.tcd.imm.hits.knime.cellhts2.configurator.simple;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
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
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.SuppressWarnings;

/**
 * This is the model implementation of SimpleConfigurator. This node reads the
 * specified CellHTS 2 configuration files for using them as input for CellHTS
 * nodes.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
@NotThreadSafe
@DefaultAnnotation(Nonnull.class)
public class SimpleConfiguratorNodeModel extends NodeModel {

	// the logger instance
	// private static final NodeLogger logger = NodeLogger
	// .getLogger(SimpleConfiguratorNodeModel.class);

	/** Column name of values */
	public static final String DESC_VALUE_COL_NAME = "Value";
	/** Column name of keys */
	public static final String DESC_KEY_COL_NAME = "Key";
	/** Column name of groups */
	public static final String DESC_GROUP_COL_NAME = "Group";
	/** Column name of content */
	public static final String CONTENT_COL_NAME = "Content";
	/** Column name of wells */
	public static final String WELL_COL_NAME = "Well";
	/** Column name of plates */
	public static final String PLATE_COL_NAME = "Plate";

	/** Configuration key for the plate configuration file parameter */
	static final String CFGKEY_PLATE_CONFIG = "ie.tcd.imm.hits.knime.cellhts2.configurator.simple.plateconfig";
	/** Default value for the plate configuration file parameter */
	static final String DEFAULT_PLATE_CONFIG = "";// "Plateconf.txt";

	/** Configuration key for the description file parameter */
	static final String CFGKEY_DESCRIPTION_FILE = "ie.tcd.imm.hits.knime.cellhts2.configurator.simple.description";
	/** Default value for the description file parameter */
	static final String DEFAULT_DESCRIPTION = "";// "Description.txt";

	private final DataColumnSpec[] plateColSpecs = new DataColumnSpec[3];
	{
		plateColSpecs[0] = new DataColumnSpecCreator(PLATE_COL_NAME,
				StringCell.TYPE).createSpec();
		plateColSpecs[1] = new DataColumnSpecCreator(WELL_COL_NAME,
				StringCell.TYPE).createSpec();
		plateColSpecs[2] = new DataColumnSpecCreator(CONTENT_COL_NAME,
				StringCell.TYPE).createSpec();
	}
	private final DataColumnSpec[] descColSpecs = new DataColumnSpec[3];
	{
		descColSpecs[0] = new DataColumnSpecCreator(DESC_GROUP_COL_NAME,
				StringCell.TYPE).createSpec();
		descColSpecs[1] = new DataColumnSpecCreator(DESC_KEY_COL_NAME,
				StringCell.TYPE).createSpec();
		descColSpecs[2] = new DataColumnSpecCreator(DESC_VALUE_COL_NAME,
				StringCell.TYPE).createSpec();
	}
	// /** initial default count value. */
	// static final int DEFAULT_COUNT = 100;

	// example value: the models count variable filled from the dialog
	// and used in the models execution method. The default components of the
	// dialog work with "SettingsModels".
	private final SettingsModelString plateConfModel = new SettingsModelString(
			SimpleConfiguratorNodeModel.CFGKEY_PLATE_CONFIG,
			SimpleConfiguratorNodeModel.DEFAULT_PLATE_CONFIG);
	private final SettingsModelString descriptionFileModel = new SettingsModelString(
			SimpleConfiguratorNodeModel.CFGKEY_DESCRIPTION_FILE,
			SimpleConfiguratorNodeModel.DEFAULT_DESCRIPTION);

	/**
	 * Constructor for the node model.
	 */
	protected SimpleConfiguratorNodeModel() {
		super(0, 2);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
			final ExecutionContext exec) throws Exception {
		final BufferedDataContainer plateConfContainer = exec
				.createDataContainer(new DataTableSpec(plateColSpecs));
		final BufferedDataContainer descConfContainer = exec
				.createDataContainer(new DataTableSpec(descColSpecs));
		readPlateConf(plateConfContainer);
		readDescription(descConfContainer);
		// once we are done, we close the container and return its table
		plateConfContainer.close();
		descConfContainer.close();
		return new BufferedDataTable[] { plateConfContainer.getTable(),
				descConfContainer.getTable() };
	}

	@SuppressWarnings("NP")
	private void readPlateConf(final BufferedDataContainer plateConfContainer)
			throws FileNotFoundException, IOException {
		final File plateConfFile = new File(plateConfModel.getStringValue());
		final FileReader reader = new FileReader(plateConfFile);
		try {
			final BufferedReader br = new BufferedReader(reader);
			try {
				int i = 0;
				String line;
				final int plateCount;
				try {
					final int wellCount = Integer.parseInt(br.readLine()
							.replace("Wells:", "").trim());
					assert wellCount == 96 || wellCount == 384;
					plateCount = Integer.parseInt(br.readLine().replace(
							"Plates:", "").trim());
				} catch (final RuntimeException e) {
					throw new IllegalStateException(
							"Missing, or wrong prolog (like:\nWells: 96\nPlates: 1\n)");
				}
				try {
					if (!br.readLine().trim().equalsIgnoreCase(
							"Plate\tWell\tContent")) {
						throw new IllegalStateException(
								"Missing header!\nPlate\tWell\tContent");
					}
				} catch (final NullPointerException e) {
					throw new IllegalStateException(
							"Missing header, unexpected end of file:\nPlate\tWell\tContent");
				}
				while ((line = br.readLine()) != null) {
					final String[] parts = line.split("\t");
					if (!parts[0].trim().equals("*")) {
						final int plate = Integer.parseInt(parts[0]);
						if (plate > plateCount) {
							throw new IllegalArgumentException(
									"Wrong plate count in line:\n" + line
											+ "\n" + ++i);
						}
					}
					final String well = parts[1];
					plateConfContainer.addRowToTable(new DefaultRow(new RowKey(
							new IntCell(++i)), new StringCell(parts[0]),
							new StringCell(well), new StringCell(parts[2])));
				}
			} finally {
				br.close();
			}
		} finally {
			reader.close();
		}
	}

	private void readDescription(final BufferedDataContainer descConfContainer)
			throws FileNotFoundException, IOException {
		final File descConfFile = new File(descriptionFileModel
				.getStringValue());
		final FileReader reader = new FileReader(descConfFile);
		try {
			final BufferedReader br = new BufferedReader(reader);
			try {
				int i = 0;
				String currentGroup = null;
				String line;
				while ((line = br.readLine()) != null) {
					++i;
					if (line.trim().length() == 0) {
						continue;
					}
					if (line.charAt(0) == '['
							&& line.charAt(line.length() - 1) == ']') {
						currentGroup = line.substring(1, line.length() - 1);
					} else {
						final int splitPoint = line.indexOf(':');
						if (splitPoint == -1) {
							throw new IllegalStateException("Wrong line:\n"
									+ line + "\n   in line: " + i + " of "
									+ descConfFile.getAbsolutePath());
						}
						descConfContainer
								.addRowToTable(new DefaultRow(new RowKey(
										new IntCell(i)), new StringCell(
										currentGroup == null ? ""
												: currentGroup),
										new StringCell(line.substring(0,
												splitPoint)), new StringCell(
												line.substring(splitPoint + 1))));
					}
				}
			} finally {
				br.close();
			}
		} finally {
			reader.close();
		}
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
		return new DataTableSpec[] { new DataTableSpec(plateColSpecs),
				new DataTableSpec(descColSpecs) };
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveSettingsTo(final NodeSettingsWO settings) {

		// TODO save user settings to the config object.

		plateConfModel.saveSettingsTo(settings);
		descriptionFileModel.saveSettingsTo(settings);

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

		plateConfModel.loadSettingsFrom(settings);
		descriptionFileModel.loadSettingsFrom(settings);

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

		plateConfModel.validateSettings(settings);
		descriptionFileModel.validateSettings(settings);

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

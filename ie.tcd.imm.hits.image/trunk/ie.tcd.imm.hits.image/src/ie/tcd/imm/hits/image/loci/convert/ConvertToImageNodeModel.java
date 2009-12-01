package ie.tcd.imm.hits.image.loci.convert;

import ie.tcd.imm.hits.common.PublicConstants;
import ie.tcd.imm.hits.image.loci.LociReaderCell;
import ie.tcd.imm.hits.util.Pair;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ImageConverter;
import ij.process.ImageProcessor;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.annotation.Nullable;

import loci.formats.FormatException;
import loci.formats.FormatReader;
import loci.plugins.util.ImagePlusReader;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.IntValue;
import org.knime.core.data.StringValue;
import org.knime.core.data.collection.CollectionDataValue;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.exp.imaging.data.def.DefaultImageCell;

/**
 * This is the model implementation of ConvertToImage. Converts the images from
 * LOCI Plate Reader to KNIME imaging format.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
public class ConvertToImageNodeModel extends NodeModel {
	private static final String IMAGE = "image";
	/** Configuration key for the combine channels property. */
	static final String CFGKEY_COMBINE_CHANNELS = "combine channels";
	/** Default value of the combine channels property. */
	static final boolean DEFAULT_COMBINE_CHANNELS = true;

	private SettingsModelBoolean combineChannels = new SettingsModelBoolean(
			CFGKEY_COMBINE_CHANNELS, DEFAULT_COMBINE_CHANNELS);

	/**
	 * Constructor for the node model.
	 */
	protected ConvertToImageNodeModel() {
		super(2, 1);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
			final ExecutionContext exec) throws Exception {
		final ColumnRearranger rearranger = createRearranger(inData[0]
				.getDataTableSpec(), inData[1].getDataTableSpec(), inData[1]);
		final BufferedDataTable ret = exec.createColumnRearrangeTable(
				inData[0], rearranger, exec);
		return new BufferedDataTable[] { ret };
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void reset() {
		// No state saved, nothing to do
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
			throws InvalidSettingsException {
		return new DataTableSpec[] { createSpec(inSpecs[0], inSpecs[1]) };
	}

	/**
	 * @param dataTableSpec
	 * @param readerTableSpec
	 * @return
	 */
	private static DataTableSpec createSpec(final DataTableSpec dataTableSpec,
			final DataTableSpec readerTableSpec) {
		return createRearranger(dataTableSpec, readerTableSpec, null)
				.createSpec();
	}

	/**
	 * FIXME Cannot done with ColumnRearranger
	 * 
	 * @param dataTableSpec
	 * @param readerTableSpec
	 * @param readerTable
	 * @return
	 */
	private static ColumnRearranger createRearranger(
			final DataTableSpec dataTableSpec,
			final DataTableSpec readerTableSpec,
			@Nullable final BufferedDataTable readerTable) {
		final ColumnRearranger ret = new ColumnRearranger(dataTableSpec);
		final int plate0Index = dataTableSpec
				.findColumnIndex(PublicConstants.LOCI_PLATE);
		final int row0Index = dataTableSpec
				.findColumnIndex(PublicConstants.LOCI_ROW);
		final int col0Index = dataTableSpec
				.findColumnIndex(PublicConstants.LOCI_COLUMN);
		final int field0Index = dataTableSpec
				.findColumnIndex(PublicConstants.LOCI_FIELD);
		final int time0Index = dataTableSpec
				.findColumnIndex(PublicConstants.LOCI_TIME);
		final int z0Index = dataTableSpec
				.findColumnIndex(PublicConstants.LOCI_Z);
		final int id0Index = dataTableSpec
				.findColumnIndex(PublicConstants.LOCI_ID);
		final int imageId0Index = dataTableSpec
				.findColumnIndex(PublicConstants.IMAGE_ID);
		final int plate1Index = readerTableSpec
				.findColumnIndex(PublicConstants.LOCI_PLATE);
		final int row1Index = readerTableSpec
				.findColumnIndex(PublicConstants.LOCI_ROW);
		final int col1Index = readerTableSpec
				.findColumnIndex(PublicConstants.LOCI_COLUMN);
		final int field1Index = readerTableSpec
				.findColumnIndex(PublicConstants.LOCI_FIELD);
		final int content1Index = readerTableSpec
				.findColumnIndex(PublicConstants.LOCI_IMAGE_CONTENT);
		final int id1Index = readerTableSpec
				.findColumnIndex(PublicConstants.LOCI_ID);
		final Map<String, Map<String, Map<Integer, Map<Integer, Pair<FormatReader, String>>>>> xmls = new LinkedHashMap<String, Map<String, Map<Integer, Map<Integer, Pair<FormatReader, String>>>>>();
		if (readerTable != null) {
			for (final DataRow row : readerTable) {
				final DataCell plateCell = row.getCell(plate1Index);
				final String plate;
				if (plateCell instanceof StringValue) {
					plate = ((StringValue) plateCell).getStringValue();
				} else {
					plate = null;
				}
				if (!xmls.containsKey(plate)) {
					xmls
							.put(
									plate,
									new LinkedHashMap<String, Map<Integer, Map<Integer, Pair<FormatReader, String>>>>());
				}
				final Map<String, Map<Integer, Map<Integer, Pair<FormatReader, String>>>> inner0 = xmls
						.get(plate);
				final String rowValue;
				final DataCell rowCell = row.getCell(row1Index);
				if (rowCell instanceof StringValue) {
					rowValue = ((StringValue) rowCell).getStringValue();
				} else {
					rowValue = null;
				}
				if (!inner0.containsKey(rowValue)) {
					inner0
							.put(
									rowValue,
									new LinkedHashMap<Integer, Map<Integer, Pair<FormatReader, String>>>());
				}
				final Map<Integer, Map<Integer, Pair<FormatReader, String>>> inner1 = inner0
						.get(rowValue);
				final Integer column;
				final DataCell columnCell = row.getCell(col1Index);
				if (columnCell instanceof IntValue) {
					column = Integer.valueOf(((IntValue) columnCell)
							.getIntValue());
				} else {
					column = null;
				}
				if (!inner1.containsKey(column)) {
					inner1.put(column,
							new HashMap<Integer, Pair<FormatReader, String>>());
				}
				final Map<Integer, Pair<FormatReader, String>> inner2 = inner1
						.get(column);
				final Integer field;
				final DataCell fieldCell = row.getCell(field1Index);
				if (fieldCell instanceof IntValue) {
					field = Integer.valueOf(((IntValue) fieldCell)
							.getIntValue());
				} else {
					field = null;
				}
				inner2
						.put(field, Pair.apply(((LociReaderCell) row
								.getCell(content1Index)).getReader(),
								((StringValue) row.getCell(id1Index))
										.getStringValue()));
			}
		}

		ret.replace(new SingleCellFactory(new DataColumnSpecCreator(IMAGE,
				DefaultImageCell.TYPE).createSpec()) {
			@Override
			public DataCell getCell(final DataRow row) {
				if (readerTable == null) {
					throw new IllegalStateException(
							"Not supposed to generate the table without enough information.");
				}
				final String plate;
				final DataCell plateCell = row.getCell(plate0Index);
				if (plateCell instanceof StringValue) {
					plate = ((StringValue) plateCell).getStringValue();
				} else {
					throw new IllegalStateException("wrong value: " + plateCell);
				}
				final Map<String, Map<Integer, Map<Integer, Pair<FormatReader, String>>>> inner0 = xmls
						.get(xmls.containsKey(plate) ? plate : null);
				final String rowValue;
				final DataCell rowCell = row.getCell(row0Index);
				if (rowCell instanceof StringValue) {
					rowValue = ((StringValue) rowCell).getStringValue();
				} else {
					throw new IllegalStateException("wrong value: " + rowCell);
				}
				final Map<Integer, Map<Integer, Pair<FormatReader, String>>> inner1 = inner0
						.get(inner0.containsKey(rowValue) ? rowValue : null);
				final Integer column;
				final DataCell columnCell = row.getCell(col0Index);
				if (columnCell instanceof IntValue) {
					column = Integer.valueOf(((IntValue) columnCell)
							.getIntValue());
				} else {
					throw new IllegalStateException("wrong value: "
							+ columnCell);
				}
				final Map<Integer, Pair<FormatReader, String>> inner2 = inner1
						.get(inner1.containsKey(column) ? column : null);
				final Integer field;
				final DataCell fieldCell = row.getCell(field0Index);
				if (fieldCell instanceof IntValue) {
					field = Integer.valueOf(((IntValue) fieldCell)
							.getIntValue());
				} else {
					throw new IllegalStateException("wrong value: " + fieldCell);
				}
				final Pair<FormatReader, String> pair = inner2.get(inner2
						.containsKey(field) ? field : null);
				final CollectionDataValue timeCell = (CollectionDataValue) row
						.getCell(time0Index);
				int timeIndex = 0;
				for (final DataCell timeDataCell : timeCell.iterator()
						.hasNext() ? timeCell : Collections
						.singletonList(new DoubleCell(0.0))) {
					final Double time = Double
							.valueOf(((DoubleValue) timeDataCell)
									.getDoubleValue());
					final CollectionDataValue zCell = (CollectionDataValue) row
							.getCell(z0Index);
					final int zIndex = 0;
					for (final DataCell zDataCell : zCell.iterator().hasNext() ? zCell
							: Collections.singletonList(new DoubleCell(0.0))) {
						final Double z = Double
								.valueOf(((DoubleValue) zDataCell)
										.getDoubleValue());
						final DataCell omeIdCell = row.getCell(id0Index);
						final String omeId = ((StringValue) omeIdCell)
								.getStringValue();
						if (!omeId.equals(pair.getRight())) {
							throw new IllegalStateException(
									"Not matching ids: " + omeId + " <-> "
											+ pair.getRight());
						}
						final ImagePlusReader imagePlusReader = ImagePlusReader
								.makeImagePlusReader(pair.getLeft());
						imagePlusReader.setSeries(((IntValue) row
								.getCell(imageId0Index)).getIntValue());
						if (pair.getLeft().getSizeC() == 1) {
							final int channel = // channels.iterator().next()
							// .intValue() - 1;
							0;
							ImageProcessor[] openProcessors;
							try {
								openProcessors = imagePlusReader
										.openProcessors(channel);
								final ImageProcessor ip = /* reader */openProcessors[0];
								return new DefaultImageCell(new ImagePlus("",
										ip));
							} catch (final FormatException e) {
								return DataType.getMissingCell();
							} catch (final IOException e) {
								return DataType.getMissingCell();
							}
						}
						final ImageStack imageStack = new ImageStack(
								imagePlusReader.getSizeX(), imagePlusReader
										.getSizeY());
						try {
							for (int i = 0; i < Math.min(3, imagePlusReader
									.getSizeC()); ++i) {

								ImagePlus image;
								image = new ImagePlus(null,
										imagePlusReader
												.openProcessors(imagePlusReader
														.getIndex(zIndex, i,
																timeIndex))[0]);
								new ImageConverter(image).convertToGray8();
								imageStack.addSlice(null, image.getProcessor());
							}
							final ImagePlus imagePlus = new ImagePlus("",
									imageStack);
							new ImageConverter(imagePlus)
									.convertRGBStackToRGB();

							return new DefaultImageCell(imagePlus);
						} catch (final FormatException e) {
							return DataType.getMissingCell();
						} catch (final IOException e) {
							return DataType.getMissingCell();
						}
						// ++zIndex;
					}
					++timeIndex;
				}
				throw new IllegalStateException(
						"Should not happen: no time or z information: "
								+ row.getKey());
			}
		}, PublicConstants.LOCI_ID);
		return ret;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveSettingsTo(final NodeSettingsWO settings) {
		combineChannels.saveSettingsTo(settings);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
			throws InvalidSettingsException {
		combineChannels.loadSettingsFrom(settings);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void validateSettings(final NodeSettingsRO settings)
			throws InvalidSettingsException {
		combineChannels.validateSettings(settings);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadInternals(final File internDir,
			final ExecutionMonitor exec) throws IOException,
			CanceledExecutionException {
		// No internal state to load
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveInternals(final File internDir,
			final ExecutionMonitor exec) throws IOException,
			CanceledExecutionException {
		// No internal state to save
	}

}

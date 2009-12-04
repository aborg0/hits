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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.annotation.Nullable;
import javax.xml.parsers.ParserConfigurationException;

import loci.formats.FormatException;
import loci.formats.FormatReader;
import loci.formats.MetadataTools;
import loci.formats.meta.IMetadata;
import loci.formats.ome.OMEXML200809Metadata;
import loci.plugins.util.ImagePlusReader;
import ome.xml.OMEXMLFactory;
import ome.xml.OMEXMLNode;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnDomainCreator;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.IntValue;
import org.knime.core.data.RowKey;
import org.knime.core.data.StringValue;
import org.knime.core.data.collection.CollectionDataValue;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.node.BufferedDataContainer;
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
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

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
	/** A small value for double comparisons. */
	private static double EPSILON = 6E-3;

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

		final BufferedDataTable readerTable = inData[1];
		final Pair<DataTableSpec, Map<String, Integer>> spec = createSpec(
				inData[0], readerTable);
		final BufferedDataContainer container = exec.createDataContainer(spec
				.getLeft());
		final DataTableSpec dataTableSpec = inData[0].getDataTableSpec();
		final DataTableSpec readerTableSpec = readerTable.getDataTableSpec();
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
		final int channel0Index = dataTableSpec
				.findColumnIndex(PublicConstants.LOCI_CHANNELS);
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
		final int xml1Index = readerTableSpec
				.findColumnIndex(PublicConstants.LOCI_XML);
		final int id1Index = readerTableSpec
				.findColumnIndex(PublicConstants.LOCI_ID);
		// Plate, row, column, field, ((reader, metadata), join id)
		final Map<String, Map<String, Map<Integer, Map<Integer, Pair<Pair<FormatReader, IMetadata>, String>>>>> xmls = createHelperStructure(
				readerTable, plate1Index, row1Index, col1Index, field1Index,
				content1Index, xml1Index, id1Index);
		final int numColumns = container.getTableSpec().getNumColumns();
		final SortedSet<Integer> specIndices = new TreeSet<Integer>(spec
				.getRight().values());
		final Set<Integer> columnsToRemove = new HashSet<Integer>(Arrays
				.asList(Integer.valueOf(id0Index)));
		if (!combineChannels.getBooleanValue()) {
			columnsToRemove.add(Integer.valueOf(dataTableSpec
					.findColumnIndex(PublicConstants.LOCI_CHANNELS)));
		}
		final int resultTimeIndex = spec.getLeft().findColumnIndex(
				PublicConstants.LOCI_TIME), resultZIndex = spec.getLeft()
				.findColumnIndex(PublicConstants.LOCI_Z);

		int progress = 0;
		for (final DataRow row : inData[0]) {
			exec.checkCanceled();
			exec.setProgress(progress * 1.0 / inData[0].getRowCount());
			final ArrayList<DataCell> values = new ArrayList<DataCell>(
					numColumns);
			for (int i = 0; i < specIndices.first().intValue(); ++i) {
				if (!columnsToRemove.contains(Integer.valueOf(i))) {
					values.add(row.getCell(i));
				}
			}
			assert specIndices.size() + specIndices.first().intValue() - 1 == specIndices
					.last().intValue();
			for (int i = specIndices.size(); i-- > 0;) {
				values.add(DataType.getMissingCell());
			}
			for (int i = specIndices.first().intValue() + 1; i < row
					.getNumCells(); ++i) {
				if (!columnsToRemove.contains(Integer.valueOf(i - 1))) {
					values.add(row.getCell(i));
				}
			}
			final String plate;
			final DataCell plateCell = row.getCell(plate0Index);
			if (plateCell instanceof StringValue) {
				plate = ((StringValue) plateCell).getStringValue();
			} else {
				throw new IllegalStateException("wrong value: " + plateCell);
			}
			final Map<String, Map<Integer, Map<Integer, Pair<Pair<FormatReader, IMetadata>, String>>>> inner0 = xmls
					.get(xmls.containsKey(plate) ? plate : null);
			final String rowValue;
			final DataCell rowCell = row.getCell(row0Index);
			if (rowCell instanceof StringValue) {
				rowValue = ((StringValue) rowCell).getStringValue();
			} else {
				throw new IllegalStateException("wrong value: " + rowCell);
			}
			final Map<Integer, Map<Integer, Pair<Pair<FormatReader, IMetadata>, String>>> inner1 = inner0
					.get(inner0.containsKey(rowValue) ? rowValue : null);
			final Integer column;
			final DataCell columnCell = row.getCell(col0Index);
			if (columnCell instanceof IntValue) {
				column = Integer.valueOf(((IntValue) columnCell).getIntValue());
			} else {
				throw new IllegalStateException("wrong value: " + columnCell);
			}
			final Map<Integer, Pair<Pair<FormatReader, IMetadata>, String>> inner2 = inner1
					.get(inner1.containsKey(column) ? column : null);
			final Integer field;
			final DataCell fieldCell = row.getCell(field0Index);
			if (fieldCell instanceof IntValue) {
				field = Integer.valueOf(((IntValue) fieldCell).getIntValue());
			} else {
				throw new IllegalStateException("wrong value: " + fieldCell);
			}
			final Pair<Pair<FormatReader, IMetadata>, String> pair = inner2
					.get(inner2.containsKey(field) ? field : null);
			final CollectionDataValue timeCell = (CollectionDataValue) row
					.getCell(time0Index);
			int timeIndex = 0;
			for (final DataCell timeDataCell : timeCell.iterator().hasNext() ? timeCell
					: Collections.singletonList(new DoubleCell(0.0))) {
				// final Double time = Double.valueOf(((DoubleValue)
				// timeDataCell)
				// .getDoubleValue());
				values.set(resultTimeIndex, timeDataCell);

				final CollectionDataValue zCell = (CollectionDataValue) row
						.getCell(z0Index);
				int zIndex = 0;
				for (final DataCell zDataCell : zCell.iterator().hasNext() ? zCell
						: Collections.singletonList(new DoubleCell(0.0))) {
					// final Double z = Double.valueOf(((DoubleValue) zDataCell)
					// .getDoubleValue());
					values.set(resultZIndex, zDataCell);
					final DataCell omeIdCell = row.getCell(id0Index);
					final String omeId = ((StringValue) omeIdCell)
							.getStringValue();
					if (!omeId.equals(pair.getRight())) {
						throw new IllegalStateException("Not matching ids: "
								+ omeId + " <-> " + pair.getRight());
					}
					final int serie = ((IntValue) row.getCell(imageId0Index))
							.getIntValue();
					final IMetadata metadata = pair.getLeft().getRight();
					final double zDoubleValue = ((DoubleValue) zDataCell)
							.getDoubleValue();
					final double timeDiff = metadata.getPlaneTimingDeltaT(
							serie, 0, timeIndex)
							- metadata.getPlaneTimingDeltaT(serie, 0, 0);
					values.set(resultTimeIndex, new DoubleCell(timeDiff));
					final FormatReader formatReader = pair.getLeft().getLeft();
					final Number correctZValue = metadata
							.getStagePositionPositionZ(serie, 0, zIndex);
					if (Math.abs(correctZValue.doubleValue() - zDoubleValue)
							/ Math.max(EPSILON, Math.max(Math.abs(correctZValue
									.doubleValue()), Math.abs(zDoubleValue))) < EPSILON) {
						values.set(resultZIndex, new DoubleCell(correctZValue
								.doubleValue()));
					}
					final ImagePlusReader imagePlusReader = ImagePlusReader
							.makeImagePlusReader(formatReader);
					imagePlusReader.setSeries(serie);
					final List<String> channelNames = getChannels(row,
							channel0Index);

					// LociReaderNodeModel
					// .getChannelNames(formatReader);
					final RowKey rowKey = new RowKey(row.getKey().getString()
							+ "_" + timeIndex + "_" + zIndex);
					if (channelNames.size() == 1) {
						for (int channel = 0; channel < formatReader.getSizeC(); ++channel) {
							// final int channel = 0;
							// final String channelName =
							// getChannelName(channelNames,
							// channel);
							final String name = getChannelName(metadata,
									channel);
							if (!spec.getRight().containsKey(name)) {
								continue;
							}
							final int pos = spec.getRight().get(name)
									.intValue();
							try {
								final ImageProcessor[] openProcessors = imagePlusReader
										.openProcessors(channel);
								final ImageProcessor ip = /* reader */openProcessors[0];
								final ImagePlus imagePlus = new ImagePlus(null,
										ip);
								new ImageConverter(imagePlus).convertToGray8();
								values
										.set(pos, new DefaultImageCell(
												imagePlus));
							} catch (final FormatException e) {
								// Keep missing cell
							} catch (final IOException e) {
								// Keep missing cell
							}
							break;
						}
					} else {
						if (combineChannels.getBooleanValue()) {
							final int pos = spec.getRight().get(IMAGE)
									.intValue();
							final ImageStack imageStack = new ImageStack(
									imagePlusReader.getSizeX(), imagePlusReader
											.getSizeY());
							try {
								for (int i = 0; i < Math.min(3, imagePlusReader
										.getSizeC()); ++i) {
									final ImagePlus image = new ImagePlus(
											null,
											imagePlusReader
													.openProcessors(imagePlusReader
															.getIndex(zIndex,
																	i,
																	timeIndex))[0]);
									new ImageConverter(image).convertToGray8();
									imageStack.addSlice(null, image
											.getProcessor());
								}
								final ImagePlus imagePlus = new ImagePlus("",
										imageStack);
								new ImageConverter(imagePlus)
										.convertRGBStackToRGB();
								values
										.set(pos, new DefaultImageCell(
												imagePlus));
							} catch (final FormatException e) {
								// Keep missing cell
							} catch (final IOException e) {
								// Keep missing cell
							}
						} else {// separate channels
							try {
								for (int i = 0; i < formatReader.getSizeC(); ++i) {
									final String channelName = getChannelName(
											metadata, i);
									if (!spec.getRight().containsKey(
											channelName)) {
										continue;
									}
									final ImagePlus image = new ImagePlus(
											null,
											imagePlusReader
													.openProcessors(imagePlusReader
															.getIndex(zIndex,
																	i,
																	timeIndex))[0]);
									new ImageConverter(image).convertToGray8();
									values.set(spec.getRight().get(channelName)
											.intValue(), new DefaultImageCell(
											image));
								}
							} catch (final FormatException e) {
								// Keep missing cell
							} catch (final IOException e) {
								// Keep missing cell
							}
						}
					}
					container.addRowToTable(new DefaultRow(rowKey, values));
					++zIndex;
				}
				++timeIndex;
			}
			++progress;
		}
		container.close();
		return new BufferedDataTable[] { container.getTable() };
	}

	private Map<String, Map<String, Map<Integer, Map<Integer, Pair<Pair<FormatReader, IMetadata>, String>>>>> createHelperStructure(
			final BufferedDataTable readerTable, final int plate1Index,
			final int row1Index, final int col1Index, final int field1Index,
			final int content1Index, final int xml1Index, final int id1Index)
			throws ParserConfigurationException, SAXException, IOException {
		final Map<String, Map<String, Map<Integer, Map<Integer, Pair<Pair<FormatReader, IMetadata>, String>>>>> xmls = new LinkedHashMap<String, Map<String, Map<Integer, Map<Integer, Pair<Pair<FormatReader, IMetadata>, String>>>>>();
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
								new LinkedHashMap<String, Map<Integer, Map<Integer, Pair<Pair<FormatReader, IMetadata>, String>>>>());
			}
			final Map<String, Map<Integer, Map<Integer, Pair<Pair<FormatReader, IMetadata>, String>>>> inner0 = xmls
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
								new LinkedHashMap<Integer, Map<Integer, Pair<Pair<FormatReader, IMetadata>, String>>>());
			}
			final Map<Integer, Map<Integer, Pair<Pair<FormatReader, IMetadata>, String>>> inner1 = inner0
					.get(rowValue);
			final Integer column;
			final DataCell columnCell = row.getCell(col1Index);
			if (columnCell instanceof IntValue) {
				column = Integer.valueOf(((IntValue) columnCell).getIntValue());
			} else {
				column = null;
			}
			if (!inner1.containsKey(column)) {
				inner1
						.put(
								column,
								new HashMap<Integer, Pair<Pair<FormatReader, IMetadata>, String>>());
			}
			final Map<Integer, Pair<Pair<FormatReader, IMetadata>, String>> inner2 = inner1
					.get(column);
			final Integer field;
			final DataCell fieldCell = row.getCell(field1Index);
			if (fieldCell instanceof IntValue) {
				field = Integer.valueOf(((IntValue) fieldCell).getIntValue());
			} else {
				field = null;
			}
			final FormatReader reader = ((LociReaderCell) row
					.getCell(content1Index)).getReader();
			final String xml = ((StringValue) row.getCell(xml1Index))
					.getStringValue();
			final IMetadata omeXml = new OMEXML200809Metadata();
			// OMEXMLFactory.newOMENode();
			// MetadataTools.createOMEXMLMetadata("");
			// omeXml.createRoot();
			final Document document = OMEXMLFactory.parseOME(xml);
			final OMEXMLNode newOMENodeFromSource = OMEXMLFactory
					.newOMENodeFromSource(document);
			// final Object root = MetadataTools.createOMEXMLRoot(xml);
			// omeXml.setRoot(root);
			omeXml.setRoot(newOMENodeFromSource);
			// MetadataTools.convertMetadata(xml, omeXml);
			// reader.setMetadataStore(omeXml);
			// reader.setMetadataFiltered(true);
			// reader.setOriginalMetadataPopulated(true);
			// final MetadataStore store =
			// MetadataTools.createOMEXMLMetadata(xml);
			// MetadataStore store = reader.getMetadataStore();
			// MetadataTools.convertMetadata(xml, store);
			// reader.setMetadataStore(store);
			inner2.put(field, Pair.apply(Pair.apply(reader, omeXml),
					((StringValue) row.getCell(id1Index)).getStringValue()));
		}
		return xmls;
	}

	/**
	 * Finds the channels in from the selected {@code row}.
	 * 
	 * @param row
	 *            A {@link DataRow}.
	 * @param channel0Index
	 *            The index of channels in the {@code row}.
	 * @return The name of the channels.
	 */
	private List<String> getChannels(final DataRow row, final int channel0Index) {
		final DataCell cell = row.getCell(channel0Index);
		if (cell instanceof StringValue) {
			final StringValue v = (StringValue) cell;
			return Collections.singletonList(v.getStringValue());
		}
		if (cell instanceof CollectionDataValue) {
			final CollectionDataValue collCell = (CollectionDataValue) cell;
			final ArrayList<String> ret = new ArrayList<String>(collCell.size());
			for (final DataCell dataCell : collCell) {
				if (dataCell instanceof StringValue) {
					final StringValue chCell = (StringValue) dataCell;
					ret.add(chCell.getStringValue());
				}
			}
			return ret;
		}
		return Collections.emptyList();
	}

	// /**
	// * Selects the channel name from the list of channel names.
	// *
	// * @param channelNames
	// * {@link List} of channel names.
	// * @param channel
	// * The position in {@code channelNames}, {@code 0}-based.
	// * @return The selected channel name (or {@link #IMAGE} if we combine the
	// * channels).
	// */
	// private String getChannelName(final List<String> channelNames,
	// final int channel) {
	// return combineChannels.getBooleanValue() ? IMAGE : channelNames
	// .get(channel);
	// }

	/**
	 * @param metadata
	 *            An {@link IMetadata}.
	 * @param channelIndex
	 *            The channel index starting from {@code 0}.
	 * @return The name of the selected channel.
	 */
	private String getChannelName(final IMetadata metadata,
			final int channelIndex) {
		final String logicalChannelName = MetadataTools.asRetrieve(metadata)
				.getLogicalChannelName(0, channelIndex);
		return combineChannels.getBooleanValue() ? IMAGE : logicalChannelName;
	}

	/**
	 * Creates the final spec for the result {@link BufferedDataTable}.
	 * 
	 * @param bufferedDataTable
	 *            The first input table.
	 * @param readerTable
	 *            The second input table.
	 * @return The {@link DataTableSpec} and the possible channel names with the
	 *         positions ({@code 0}-based) in the output table.
	 */
	private Pair<DataTableSpec, Map<String, Integer>> createSpec(
			final BufferedDataTable bufferedDataTable,
			final BufferedDataTable readerTable) {
		final DataTableSpec origSpec = bufferedDataTable.getDataTableSpec();
		final Pair<DataTableSpec, Map<String, Integer>> possSpec = createSpec(
				origSpec, readerTable.getDataTableSpec());
		if (possSpec != null) {
			return possSpec;
		}
		final int idx = origSpec.findColumnIndex(PublicConstants.LOCI_CHANNELS);
		final Set<DataCell> channelNames = new LinkedHashSet<DataCell>();
		for (final DataRow dataRow : bufferedDataTable) {
			final DataCell cell = dataRow.getCell(idx);
			channelNames.add(cell);
		}
		final DataColumnSpec[] newSpecColumns = new DataColumnSpec[origSpec
				.getNumColumns()];
		for (int i = origSpec.getNumColumns(); i-- > 0;) {
			final DataColumnSpec columnSpec = origSpec.getColumnSpec(i);
			if (columnSpec.getName().equals(PublicConstants.LOCI_CHANNELS)) {
				final DataColumnSpecCreator dataColumnSpecCreator = new DataColumnSpecCreator(
						columnSpec);
				dataColumnSpecCreator.setDomain(new DataColumnDomainCreator(
						channelNames).createDomain());
				newSpecColumns[i] = dataColumnSpecCreator.createSpec();
			} else {
				newSpecColumns[i] = columnSpec;
			}
		}
		return createSpec(new DataTableSpec(newSpecColumns), readerTable
				.getDataTableSpec());
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
		final Pair<DataTableSpec, Map<String, Integer>> pair = createSpec(
				inSpecs[0], inSpecs[1]);
		return new DataTableSpec[] { pair == null ? null : pair.getLeft() };
	}

	/**
	 * Creates the {@link DataTableSpec} if it has enough information, else
	 * returns {@code null}. Also gives the positions of the selected channels.
	 * 
	 * @param dataTableSpec
	 *            The {@link DataTableSpec} of the first input table.
	 * @param readerTableSpec
	 *            The {@link DataTableSpec} of the second input table.
	 * @return The new table spec, may be {@code null} if the channels are not
	 *         specified.
	 */
	@Nullable
	private Pair<DataTableSpec, Map<String, Integer>> createSpec(
			final DataTableSpec dataTableSpec,
			final DataTableSpec readerTableSpec) {
		final List<DataColumnSpec> ret = new ArrayList<DataColumnSpec>(
				dataTableSpec.getNumColumns() - 1);
		final Map<String, Integer> connection = new HashMap<String, Integer>();
		if (combineChannels.getBooleanValue()) {
			for (final DataColumnSpec columnSpec : dataTableSpec) {
				if (columnSpec.getName().equals(PublicConstants.LOCI_ID)) {
					// Replace LOCI_ID with IMAGE
					ret.add(new DataColumnSpecCreator(IMAGE,
							DefaultImageCell.TYPE).createSpec());
					connection.put(IMAGE, Integer.valueOf(ret.size() - 1));
				} else
				// Remove IMAGE_ID
				if (!columnSpec.getName().equals(PublicConstants.IMAGE_ID)) {
					if (columnSpec.getName().equals(PublicConstants.LOCI_Z)
							|| columnSpec.getName().equals(
									PublicConstants.LOCI_TIME)) {
						final DataColumnSpecCreator specCreator = new DataColumnSpecCreator(
								columnSpec.getName(), DoubleCell.TYPE);
						specCreator.setProperties(columnSpec.getProperties());
						ret.add(specCreator.createSpec());
					} else {
						ret.add(columnSpec);
					}
				}
			}
		} else {
			final DataColumnSpec channels = dataTableSpec
					.getColumnSpec(PublicConstants.LOCI_CHANNELS);
			if (channels.getDomain() == null
					|| channels.getDomain().getValues() == null
					|| channels.getDomain().getValues().isEmpty()) {
				return null;
			}
			for (final DataColumnSpec columnSpec : dataTableSpec) {
				if (columnSpec.getName().equals(PublicConstants.LOCI_ID)) {
					final Set<String> channelNames = new LinkedHashSet<String>();
					for (final DataCell collectionCell : channels.getDomain()
							.getValues()) {
						collectChannelNames(channelNames, collectionCell);
					}
					for (final String channelName : channelNames) {
						ret.add(new DataColumnSpecCreator(channelName,
								DefaultImageCell.TYPE).createSpec());
						connection.put(channelName, Integer
								.valueOf(ret.size() - 1));
					}
				} else
				// Remove IMAGE_ID and LOCI_CHANNELS
				if (!columnSpec.getName().equals(PublicConstants.IMAGE_ID)
						&& !columnSpec.getName().equals(
								PublicConstants.LOCI_CHANNELS)) {
					if (columnSpec.getName().equals(PublicConstants.LOCI_Z)
							|| columnSpec.getName().equals(
									PublicConstants.LOCI_TIME)) {
						final DataColumnSpecCreator specCreator = new DataColumnSpecCreator(
								columnSpec.getName(), DoubleCell.TYPE);
						specCreator.setProperties(columnSpec.getProperties());
						ret.add(specCreator.createSpec());
					} else {
						ret.add(columnSpec);
					}
				}
			}
		}
		return Pair.apply(new DataTableSpec(ret.toArray(new DataColumnSpec[ret
				.size()])), Collections.unmodifiableMap(connection));
	}

	/**
	 * Collect the channel names from the {@code collectionCell} to the {@code
	 * channelNames} collection.
	 * 
	 * @param channelNames
	 *            The result {@link Set} of channel names.
	 * @param collectionCell
	 *            A DataCell containing channel name(s).
	 */
	private static void collectChannelNames(final Set<String> channelNames,
			final DataCell collectionCell) {
		if (collectionCell instanceof CollectionDataValue) {
			final CollectionDataValue collCell = (CollectionDataValue) collectionCell;
			for (final DataCell dataCell : collCell) {
				if (dataCell instanceof StringValue) {
					final StringValue channelName = (StringValue) dataCell;
					channelNames.add(channelName.getStringValue());
				}
			}
		}
		if (collectionCell instanceof StringValue) {
			final StringValue stringCell = (StringValue) collectionCell;
			channelNames.add(stringCell.getStringValue());
		}
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

/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package ie.tcd.imm.hits.image.loci;

import ie.tcd.imm.hits.image.loci.read.LociReaderNodeModel;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import loci.formats.FormatReader;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataCellDataInput;
import org.knime.core.data.DataCellDataOutput;
import org.knime.core.data.DataCellSerializer;
import org.knime.core.data.DataType;
import org.knime.core.node.NodeLogger;

/**
 * {@link DataCell} implementation for LOCI Bio-Formats reader values.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
public class LociReaderCell extends DataCell implements LociReaderValue {
	private static final NodeLogger logger = NodeLogger
			.getLogger(LociReaderNodeModel.class);
	/** The mandatory {@code TYPE} constant. */
	public static final DataType TYPE = DataType.getType(LociReaderCell.class);

	/**
	 * Cell serializer for this kind of cells.
	 */
	private static class LociReaderCellSerializer implements
			DataCellSerializer<LociReaderCell> {

		/** {@inheritDoc} */
		@Override
		public LociReaderCell deserialize(final DataCellDataInput input)
				throws IOException {
			final int serializedLength = input.readInt();
			assert serializedLength > 0 : serializedLength;
			final byte[] serData = new byte[serializedLength];
			input.readFully(serData);
			final ObjectInputStream ois = new ObjectInputStream(
					new ByteArrayInputStream(serData));
			try {
				final Object readObject = ois.readObject();
				if (readObject instanceof FormatReader) {
					final FormatReader reader = (FormatReader) readObject;
					return new LociReaderCell(reader);
				}
				final String errorMessage = "Wrong data read: "
						+ readObject.getClass() + "; expected a subclass of "
						+ FormatReader.class;
				logger.error(errorMessage);
				throw new IllegalStateException(errorMessage);
			} catch (final ClassNotFoundException e) {
				logger.error("Not found: " + e.getMessage(), e);
				throw new RuntimeException(e);
			} finally {
				ois.close();
			}
		}

		/** {@inheritDoc} */
		@Override
		public void serialize(final LociReaderCell cell,
				final DataCellDataOutput output) throws IOException {
			final ByteArrayOutputStream bos = new ByteArrayOutputStream();
			final ObjectOutputStream oos = new ObjectOutputStream(bos);
			try {
				oos.writeObject(cell.reader);
			} finally {
				oos.close();
			}
			final byte[] byteArray = bos.toByteArray();
			output.writeInt(byteArray.length);
			output.write(byteArray);
		}

	}

	private static final long serialVersionUID = 6570316044931632481L;

	private final FormatReader reader;

	private static final DataCellSerializer<LociReaderCell> SERIALIZER = new LociReaderCellSerializer();

	/**
	 * Get serializer as required by {@link DataCell}.
	 * 
	 * @return Such a serializer.
	 */
	public static final DataCellSerializer<LociReaderCell> getCellSerializer() {
		return SERIALIZER;
	}

	/**
	 * @param reader
	 *            The {@link FormatReader} to wrap.
	 */
	public LociReaderCell(final FormatReader reader) {
		super();
		this.reader = reader;
	}

	/** {@inheritDoc} */
	@Override
	protected boolean equalsDataCell(final DataCell dc) {
		if (dc == this) {
			return true;
		}
		return reader.equals(((LociReaderCell) dc).reader);
	}

	/** {@inheritDoc} */
	@Override
	public int hashCode() {
		return reader.hashCode();
	}

	/** {@inheritDoc} */
	@Override
	public String toString() {
		return reader.toString();
	}

	/** {@inheritDoc} */
	@Override
	public FormatReader getReader() {
		return reader;
	}
}

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
 * TODO Javadoc!
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
public class LociReaderCell extends DataCell implements LociReaderValue {
	private static final NodeLogger logger = NodeLogger
			.getLogger(LociReaderNodeModel.class);
	public static final DataType TYPE = DataType.getType(LociReaderCell.class);

	/**
	 * TODO Javadoc!
	 * 
	 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
	 */
	private static class LociReaderCellSerializer implements
			DataCellSerializer<LociReaderCell> {

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.knime.core.data.DataCellSerializer#deserialize(org.knime.core
		 * .data.DataCellDataInput)
		 */
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

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.knime.core.data.DataCellSerializer#serialize(org.knime.core.data
		 * .DataCell, org.knime.core.data.DataCellDataOutput)
		 */
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
	 * 
	 */
	public LociReaderCell(final FormatReader reader) {
		super();
		this.reader = reader;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.knime.core.data.DataCell#equalsDataCell(org.knime.core.data.DataCell)
	 */
	@Override
	protected boolean equalsDataCell(final DataCell dc) {
		if (dc == this) {
			return true;
		}
		return reader.equals(((LociReaderCell) dc).reader);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.knime.core.data.DataCell#hashCode()
	 */
	@Override
	public int hashCode() {
		return reader.hashCode();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.knime.core.data.DataCell#toString()
	 */
	@Override
	public String toString() {
		return reader.toString();
	}

	@Override
	public FormatReader getReader() {
		return reader;
	}
}

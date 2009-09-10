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
import org.knime.core.node.NodeLogger;

/**
 * TODO Javadoc!
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
public class OMEReaderCell extends DataCell {
	private static final NodeLogger logger = NodeLogger.getLogger(LociReaderNodeModel.class);
	
	/**
	 * TODO Javadoc!
	 * 
	 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
	 */
	private static class OMEReaderCellSerializer implements
			DataCellSerializer<OMEReaderCell> {

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.knime.core.data.DataCellSerializer#deserialize(org.knime.core
		 * .data.DataCellDataInput)
		 */
		@Override
		public OMEReaderCell deserialize(final DataCellDataInput input)
				throws IOException {
			int serializedLength = input.readInt();
			assert serializedLength > 0 : serializedLength;
			byte[] serData = new byte[serializedLength];
			ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(serData));
			try {
				Object readObject = ois.readObject();
				if (readObject instanceof FormatReader) {
					FormatReader reader = (FormatReader) readObject;
					return new OMEReaderCell(reader);
				}
				String errorMessage = "Wrong data read: " + readObject.getClass() + "; expected a subclass of " + FormatReader.class;
				logger.error(errorMessage);
				throw new IllegalStateException(errorMessage);
			} catch (ClassNotFoundException e) {
				logger.error("Not found: " + e.getMessage(), e);
				throw new RuntimeException(e);
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
		public void serialize(final OMEReaderCell cell,
				final DataCellDataOutput output) throws IOException {
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream(bos);
			try {
				oos.writeObject(cell.reader);
			}finally {
				oos.close();
			}
			byte[] byteArray = bos.toByteArray();
			output.write(byteArray.length);
			output.write(byteArray);
		}

	}

	private static final long serialVersionUID = 6570316044931632481L;

	private final FormatReader reader;

	private static final DataCellSerializer<OMEReaderCell> SERIALIZER = new OMEReaderCellSerializer();

	/**
	 * Get serializer as required by {@link DataCell}.
	 * 
	 * @return Such a serializer.
	 */
	public static final DataCellSerializer<OMEReaderCell> getCellSerializer() {
		return SERIALIZER;
	}

	/**
	 * 
	 */
	public OMEReaderCell(final FormatReader reader) {
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
		return reader.equals(((OMEReaderCell) dc).reader);
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

	public FormatReader getReader() {
		return reader;
	}
}

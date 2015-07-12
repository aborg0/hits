/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package ie.tcd.imm.hits.image.omero;

import org.knime.core.data.DataCell;

/**
 * An implementation of {@link OmeroValue} to represent OMERO server images.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
public class OmeroCell extends DataCell implements OmeroValue {
	/** Generated serial version */
	private static final long serialVersionUID = -8817191262689660239L;

	/**
	 * 
	 */
	public OmeroCell() {

		// TODO Auto-generated constructor stub
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.knime.core.data.DataCell#equalsDataCell(org.knime.core.data.DataCell)
	 */
	@Override
	protected boolean equalsDataCell(final DataCell dc) {
		// TODO Auto-generated method stub
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.knime.core.data.DataCell#hashCode()
	 */
	@Override
	public int hashCode() {
		// TODO Auto-generated method stub
		return 0;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.knime.core.data.DataCell#toString()
	 */
	@Override
	public String toString() {
		return "";
	}
}

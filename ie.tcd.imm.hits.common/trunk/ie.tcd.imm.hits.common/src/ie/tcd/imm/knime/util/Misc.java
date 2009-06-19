/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package ie.tcd.imm.knime.util;

import java.util.List;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;

import edu.umd.cs.findbugs.annotations.DefaultAnnotation;

/**
 * Some utility methods to related to KNIME.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
@DefaultAnnotation( { Nonnull.class, CheckReturnValue.class })
public class Misc {

	/**
	 * Checks whether the column names in {@code list} are available or not in
	 * {@code dataTableSpec}.
	 * 
	 * @param list
	 *            A {@link List} of column names.
	 * @param dataTableSpec
	 *            A {@link DataTableSpec}.
	 * @throws InvalidSettingsException
	 *             If not found any of the column names.
	 */
	public static void checkList(final List<String> list,
			final DataTableSpec dataTableSpec) throws InvalidSettingsException {
		for (final String colName : list) {
			if (dataTableSpec.getColumnSpec(colName) == null) {
				throw new InvalidSettingsException("Not a valid column name: "
						+ colName + ", please reconnect the input node.");
			}
		}
	}
}

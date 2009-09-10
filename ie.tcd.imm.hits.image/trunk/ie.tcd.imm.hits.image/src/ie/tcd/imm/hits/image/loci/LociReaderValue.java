/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package ie.tcd.imm.hits.image.loci;

import javax.swing.Icon;

import loci.formats.FormatReader;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataValue;
import org.knime.core.data.DataValueComparator;
import org.knime.core.data.renderer.DataValueRendererFamily;
import org.knime.core.data.renderer.DefaultDataValueRenderer;
import org.knime.core.data.renderer.DefaultDataValueRendererFamily;

/**
 * TODO Javadoc!
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
public interface LociReaderValue extends DataValue {

	public static final UtilityFactory UTILITY = new UtilityFactory() {
		private final DataValueComparator comparator = new DataValueComparator() {
			@Override
			protected int compareDataValues(final DataValue v1,
					final DataValue v2) {
				return 0;
			}
		};
		private final Icon icon = loadIcon(LociReaderValue.class,
				"/read/default.png");

		@Override
		protected DataValueComparator getComparator() {
			return comparator;
		}

		@Override
		protected DataValueRendererFamily getRendererFamily(
				final DataColumnSpec spec) {
			return new DefaultDataValueRendererFamily(
					new DefaultDataValueRenderer() {
						/**  */
						private static final long serialVersionUID = 6476838310144221648L;

						@Override
						public boolean accepts(final DataColumnSpec spec) {
							return spec.getType().equals(LociReaderCell.TYPE);
						}

						protected void setValue(final Object value) {
							super.setValue(value);
						};

						public String getDescription() {
							return "LOCI Bio-Formats reader";
						}
					});
		}

		@Override
		public Icon getIcon() {
			return icon;
		}
	};

	public FormatReader getReader();
}

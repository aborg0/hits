/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package ie.tcd.imm.hits.knime.util;

import ie.tcd.imm.hits.util.Misc;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.knime.base.util.coordinate.LogarithmicMappingMethod;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnDomain;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.def.DoubleCell;

import edu.umd.cs.findbugs.annotations.DefaultAnnotation;

/**
 * Same as the {@link LogarithmicMappingMethod} except this {@code
 * log(offset+x)}.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
@DefaultAnnotation( { Nonnull.class, CheckReturnValue.class })
public class ShiftedLogarithmicMappingMethod extends LogarithmicMappingMethod {

	/**
	 * Identifier for a logarithmic mapping method with base e ( ln ).
	 */
	public static final String ID_BASE_E = "ln(1+x)MappingMethod";

	/**
	 * Identifier for a logarithmic mapping method with base 2 ( ld ).
	 */
	public static final String ID_BASE_2 = "ld(1+x)MappingMethod";

	/**
	 * Identifier for a logarithmic mapping method with base 10 ( log ).
	 */
	public static final String ID_BASE_10 = "log(1+x)MappingMethod";

	/** This is the offset of the argument ({@code log(offset+x)}) */
	private final double offset;

	/**
	 * Creates an {@code ln(x+offset)} mapping.
	 * 
	 * @param offset
	 *            The offset of the logarithmic transformation.
	 */
	public ShiftedLogarithmicMappingMethod(final double offset) {
		super();
		this.offset = offset;
	}

	/**
	 * Creates a {@code log_base(x+offset)} mapping.
	 * 
	 * @param base
	 *            The base of the logarithm.
	 * @param offset
	 *            The offset of the logarithmic transformation.
	 */
	public ShiftedLogarithmicMappingMethod(final double base,
			final double offset) {
		super(base);
		this.offset = offset;
	}

	@Override
	public boolean isCompatibleWithDomain(final DataColumnDomain domain) {
		// TODO Auto-generated method stub
		final DataCell lowerBound = domain.getLowerBound();
		final DataCell upperBound = domain.getUpperBound();
		if (lowerBound instanceof DoubleValue
				&& upperBound instanceof DoubleValue) {
			final DoubleValue lb = (DoubleValue) lowerBound;
			final DoubleValue ub = (DoubleValue) upperBound;
			return lb.getDoubleValue() >= -offset
					&& ub.getDoubleValue() > 1 - offset;
		}
		return super.isCompatibleWithDomain(domain);
	}

	@Override
	public DataCell doMapping(final DataCell in) {
		if (in instanceof DoubleValue) {
			final DoubleValue orig = (DoubleValue) in;
			return super.doMapping(new DoubleCell(offset
					+ orig.getDoubleValue()));
		}
		return super.doMapping(in);
	}

	@Override
	public double getLabel(final DataCell cell) {
		if (cell instanceof DoubleValue) {
			final DoubleValue orig = (DoubleValue) cell;
			return super.getLabel(new DoubleCell(orig.getDoubleValue()))
					- offset;
		}
		return super.getLabel(cell);
	}

	@Override
	public String getDisplayName() {
		return offset >= 0 ? super.getDisplayName().replace("x",
				"x+" + Misc.round(offset)) : super.getDisplayName().replace(
				"x", "x-" + Misc.round(Math.abs(offset)));
	}
}

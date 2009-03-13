/**
 * 
 */
package ie.tcd.imm.hits.util.swing.colour;

import ie.tcd.imm.hits.knime.util.VisualUtils;
import ie.tcd.imm.hits.util.Pair;
import ie.tcd.imm.hits.util.interval.Interval;
import ie.tcd.imm.hits.util.interval.Interval.DefaultInterval;

import java.awt.Color;
import java.io.Serializable;
import java.util.Collections;
import java.util.Map;
import java.util.NavigableMap;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Map.Entry;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import edu.umd.cs.findbugs.annotations.DefaultAnnotation;

/**
 * A colour model allowing to have multiple intermediate values as constant
 * colours.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
@DefaultAnnotation( { Nonnull.class, CheckReturnValue.class })
public class ComplexModel implements ColourComputer, Serializable {
	private static final long serialVersionUID = -8287978216868912139L;

	private final NavigableMap<Interval<Double>, Color> discretes;
	private final NavigableMap<Interval<Double>, Pair<Color, Color>> continuouses;

	/**
	 * @param continuouses
	 * @param discretes
	 */
	public ComplexModel(
			final Map<Interval<Double>, Pair<Color, Color>> continuouses,
			final Map<Interval<Double>, Color> discretes) {
		this.continuouses = new TreeMap<Interval<Double>, Pair<Color, Color>>(
				continuouses);
		this.discretes = new TreeMap<Interval<Double>, Color>(discretes);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ie.tcd.imm.hits.util.swing.colour.ColourComputer#compute(double)
	 */
	@Override
	public Color compute(final double val) {
		final Double dval = Double.valueOf(val);
		final DefaultInterval<Double> valInterval = new DefaultInterval<Double>(
				dval, dval, true, true);
		final Entry<Interval<Double>, Color> discreteEntry = selectEntry(dval,
				valInterval, discretes);
		if (discreteEntry != null) {
			return discreteEntry.getValue();
		}
		final Entry<Interval<Double>, Pair<Color, Color>> continuousEntry = selectEntry(
				dval, valInterval, continuouses);
		if (continuousEntry != null) {
			return VisualUtils.colourOf(val, continuousEntry.getValue()
					.getLeft(), null, continuousEntry.getValue().getRight(),
					continuousEntry.getKey().getLow().doubleValue(),
					Double.NaN, continuousEntry.getKey().getHigh()
							.doubleValue());
		} else {
			final Entry<Interval<Double>, Pair<Color, Color>> higherEntry = continuouses
					.higherEntry(valInterval);
			// if (higherEntry != null
			// && dval.compareTo(higherEntry.getKey().getHigh()) > 0) {
			// return higherEntry.getValue().getRight();// Unreachable
			// }
			final Entry<Interval<Double>, Pair<Color, Color>> lowerEntry = continuouses
					.lowerEntry(valInterval);
			// if (lowerEntry != null
			// && dval.compareTo(lowerEntry.getKey().getLow()) < 0) {
			// return lowerEntry.getValue().getLeft();// Unreachable
			// }
			if (lowerEntry != null && higherEntry != null) {
				return (val - lowerEntry.getKey().getHigh().doubleValue() > higherEntry
						.getKey().getLow()
						- val) ? higherEntry.getValue().getLeft() : lowerEntry
						.getValue().getRight();
			}
			if (lowerEntry == null && higherEntry != null) {
				return higherEntry.getValue().getLeft();
			}
			if (higherEntry == null && lowerEntry != null) {
				return lowerEntry.getValue().getRight();
			}
		}
		return Color.BLACK;
	}

	/**
	 * Selects the proper entry from {@code map} belonging to
	 * {@code val, valInterval}, or {@code null} if none found.
	 * 
	 * @param <ValType>
	 *            The value type of {@code map}.
	 * @param val
	 *            The value to find.
	 * @param valInterval
	 *            The same as {@code val}, but as an {@link Interval}.
	 * @param map
	 *            A {@link Map}.
	 * @return The found {@link Interval} and value, or {@code null}.
	 */
	@Nullable
	private <ValType> Entry<Interval<Double>, ValType> selectEntry(
			final Double val, final DefaultInterval<Double> valInterval,
			final NavigableMap<Interval<Double>, ValType> map) {
		final Entry<Interval<Double>, ValType> higher = map
				.higherEntry(valInterval);
		final Entry<Interval<Double>, ValType> lower = map
				.lowerEntry(valInterval);
		final boolean hasResult = (higher != null && higher.getKey().contains(
				val))
				|| (lower != null && lower.getKey().contains(val));

		if (hasResult) {
			if (lower != null && lower.getKey().contains(val)) {
				return lower;
			}
			assert higher != null;
			return higher.getKey().contains(val) ? higher : null;
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ie.tcd.imm.hits.util.swing.colour.ColourComputer#getTooltip()
	 */
	@Override
	public String getTooltip() {
		// FIXME better tooltip!
		return discretes.keySet() + " | " + continuouses.keySet();
	}

	/**
	 * @return The intervals with linear gradient change.
	 */
	protected SortedMap<Interval<Double>, Pair<Color, Color>> getContinuouses() {
		return Collections.unmodifiableSortedMap(continuouses);
	}

	/**
	 * @return The intervals with single colour.
	 */
	protected SortedMap<Interval<Double>, Color> getDiscretes() {
		return Collections.unmodifiableSortedMap(discretes);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return discretes + " | " + continuouses;
	}
}

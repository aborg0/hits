/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package com.mind_era.knime.common.util.swing.colour;

import java.awt.Color;
import java.io.Serializable;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.knime.core.util.Pair;

import com.mind_era.knime.common.util.VisualUtils;
import com.mind_era.knime.common.util.interval.Interval;
import com.mind_era.knime.common.util.interval.Interval.DefaultInterval;

/**
 * A colour model allowing to have multiple intermediate values as constant
 * colours.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
@Nonnull
@CheckReturnValue
public class ComplexModel implements ColourComputer, Serializable {
	private static final long serialVersionUID = -8287978216868912139L;

	private final NavigableMap<Interval<Double>, Color> discretes;
	private final NavigableMap<Interval<Double>, Pair<Color, Color>> continuouses;

	/**
	 * Constructs a {@link ComplexModel}.
	 * 
	 * @param continuouses
	 *            The (linear) gradient changes.
	 * @param discretes
	 *            The single colour intervals.
	 */
	public ComplexModel(
			final Map<Interval<Double>, Pair<Color, Color>> continuouses,
			final Map<Interval<Double>, Color> discretes) {
		this.continuouses = new TreeMap<Interval<Double>, Pair<Color, Color>>(
				continuouses);
		this.discretes = new TreeMap<Interval<Double>, Color>(discretes);
	}

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
					.getFirst(), null, continuousEntry.getValue().getSecond(),
					continuousEntry.getKey().getLow().doubleValue(),
					Double.NaN, continuousEntry.getKey().getHigh()
							.doubleValue());
		} else {
			final Entry<Interval<Double>, Pair<Color, Color>> higherEntry = continuouses
					.higherEntry(valInterval);
			final Entry<Interval<Double>, Pair<Color, Color>> lowerEntry = continuouses
					.lowerEntry(valInterval);
			if (lowerEntry != null && higherEntry != null) {
				return val - lowerEntry.getKey().getHigh().doubleValue() > higherEntry
						.getKey().getLow()
						- val ? higherEntry.getValue().getFirst() : lowerEntry
						.getValue().getSecond();
			}
			if (lowerEntry == null && higherEntry != null) {
				return higherEntry.getValue().getFirst();
			}
			if (higherEntry == null && lowerEntry != null) {
				return lowerEntry.getValue().getSecond();
			}
			final Entry<Interval<Double>, Color> higherEntryDisc = discretes
					.higherEntry(valInterval);
			final Entry<Interval<Double>, Color> lowerEntryDisc = discretes
					.lowerEntry(valInterval);
			if (higherEntryDisc != null && lowerEntryDisc != null) {
				// Do nothing, in the middle parts we do not want colour things.
			} else if (higherEntryDisc != null) {
				return higherEntryDisc.getValue();
			} else if (lowerEntryDisc != null) {
				return lowerEntryDisc.getValue();
			}
		}
		return Color.BLACK;
	}

	/**
	 * Selects the proper entry from {@code map} belonging to {@code val,
	 * valInterval}, or {@code null} if none found.
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
		final boolean hasResult = higher != null
				&& higher.getKey().contains(val) || lower != null
				&& lower.getKey().contains(val);

		if (hasResult) {
			if (lower != null && lower.getKey().contains(val)) {
				return lower;
			}
			assert higher != null;
			return higher.getKey().contains(val) ? higher : null;
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	@Override
	public String getTooltip() {
		final NavigableMap<Interval<Double>, Object> union = new TreeMap<Interval<Double>, Object>();
		union.putAll(discretes);
		union.putAll(continuouses);
		final StringBuilder sb = new StringBuilder();
		sb.append("<html>");
		for (final Entry<Interval<Double>, Object> entry : union.entrySet()) {
			sb
					.append("<font color=\"#")
					.append(
							Integer
									.toHexString(entry.getValue() instanceof Color ? ((Color) entry
											.getValue()).getRGB() & 0x00ffffff
											: ((Pair<Color, Color>) entry
													.getValue()).getFirst()
													.getRGB() & 0x00ffffff))
					.append("\">").append(
							Math.round(entry.getKey().getLow() * 100) / 100.0)
					.append("</font>").append(
							entry.getValue() instanceof Color ? "-" : "-&gt;");
			sb
					.append("<font color=\"#")
					.append(
							Integer
									.toHexString(entry.getValue() instanceof Color ? ((Color) entry
											.getValue()).getRGB() & 0x00ffffff
											: ((Pair<Color, Color>) entry
													.getValue()).getSecond()
													.getRGB() & 0x00ffffff))
					.append("\">").append(
							Math.round(entry.getKey().getHigh() * 100) / 100.0)
					.append("</font>").append(", ");
		}
		if (sb.length() > 0) {
			sb.setLength(sb.length() - ", ".length());
		}
		sb.append("</html>");
		return sb.toString();
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

	/**
	 * @return The values appearing in one of the intervals, in ascending order.
	 *         (Modifiable.)
	 */
	protected SortedSet<Double> getValues() {
		final SortedSet<Double> ret = new TreeSet<Double>();
		for (final Interval<Double> interval : continuouses.keySet()) {
			ret.add(interval.getLow());
			ret.add(interval.getHigh());
		}
		for (final Interval<Double> interval : discretes.keySet()) {
			ret.add(interval.getLow());
			ret.add(interval.getHigh());
		}
		return ret;
	}

	@Override
	public String toString() {
		return discretes + " | " + continuouses;
	}
}

/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package ie.tcd.imm.hits.knime.util;

import ie.tcd.imm.hits.knime.util.ModelBuilder.SpecAnalyser;
import ie.tcd.imm.hits.knime.view.heatmap.HeatmapNodeModel.StatTypes;
import ie.tcd.imm.hits.util.swing.colour.ColourSelector.RangeType;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.def.IntCell;

/**
 * A class to create simple models.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
public class SimpleModelBuilder implements Serializable {
	private static final long serialVersionUID = -2037522274855411494L;
	/** The analysed table */
	private final DataTable table;
	/** The {@link SpecAnalyser} for the table. */
	private final SpecAnalyser specAnalyser;

	/**
	 * Selects the {@code index + 1}<sup>th</sup> cell value from {@code
	 * dataRow}.
	 * 
	 * @param dataRow
	 *            A {@link DataRow}.
	 * @param index
	 *            A {@code 0}-based index to select a cell in {@code dataRow}.
	 * @return The value at {@code index} position in {@code dataRow}.
	 * @throws ClassCastException
	 *             if the cell is not an {@link IntCell}.
	 * @throws ArrayIndexOutOfBoundsException
	 *             if the {@code index} is not valid.
	 */
	public static Integer getInt(final DataRow dataRow, final int index) {
		return Integer
				.valueOf(((IntCell) dataRow.getCell(index)).getIntValue());
	}

	/**
	 * Computes some statistics of {@code vals} to {@code ret}.
	 * 
	 * @param ret
	 *            The result ranges map.
	 * @param vals
	 *            The values to analyse.
	 */
	public static void computeStatistics(
			final Map<String, Map<StatTypes, Map<RangeType, Double>>> ret,
			final Map<String, Map<StatTypes, List<Double>>> vals) {
		for (final Entry<String, Map<StatTypes, List<Double>>> entry : vals
				.entrySet()) {
			for (final Entry<StatTypes, List<Double>> subEntry : entry
					.getValue().entrySet()) {
				final List<Double> values = subEntry.getValue();
				Collections.sort(values);
				if (!ret.containsKey(entry.getKey())) {
					continue;
				}
				ret.get(entry.getKey()).get(subEntry.getKey()).put(
						RangeType.min,
						values.size() > 0 ? values.get(0) : Double.NaN);
				int n = 0;
				double sum = 0.0;
				for (final Double d : values) {
					if (!d.isNaN()) {
						++n;
						sum += d.doubleValue();
					}
				}
				final double median = n > 0 ? (n % 2 != 0 ? values.get(n / 2)
						: (values.get(n / 2) + values.get(n / 2 - 1)) / 2)
						: Double.NaN;
				final double q1 = n > 0 ? (n % 4 == 1 ? values.get(n / 4) : ((4
						* n + 4 - n + 1)
						% 4 * values.get(n / 4) + (n - 1) % 4
						* values.get(n / 4 - 1)) / 4) : Double.NaN;
				final double q3 = n > 0 ? (n % 4 == 1 ? values.get(3 * n / 4)
						: ((4 * n + 4 - n + 1) % 4 * values.get(3 * n / 4) + (n - 1)
								% 4 * values.get(3 * n / 4 - 1)) / 4)
						: Double.NaN;
				final double maxVal = n > 0 ? values.get(n - 1) : Double.NaN;
				ret.get(entry.getKey()).get(subEntry.getKey()).put(
						RangeType.max, maxVal);
				ret.get(entry.getKey()).get(subEntry.getKey()).put(
						RangeType.median, median);
				ret.get(entry.getKey()).get(subEntry.getKey()).put(
						RangeType.q1, q1);
				ret.get(entry.getKey()).get(subEntry.getKey()).put(
						RangeType.q3, q3);
				ret.get(entry.getKey()).get(subEntry.getKey()).put(
						RangeType.iqr, q3 - q1);
				final double average = sum / n;
				ret.get(entry.getKey()).get(subEntry.getKey()).put(
						RangeType.average,
						n == 0 ? Double.NaN : Double.valueOf(average));
				final MathContext context = new MathContext(10,
						RoundingMode.HALF_EVEN);
				BigDecimal sumDiffSquare = new BigDecimal(0.0);
				final List<Double> diffAbs = new ArrayList<Double>(n);
				for (final Double d : values) {
					if (!d.isNaN()) {
						final double diff = d - average;
						final BigDecimal diffBig = BigDecimal.valueOf(diff);
						sumDiffSquare = sumDiffSquare.add(diffBig.multiply(
								diffBig, context), context);
						diffAbs.add(Double.valueOf(Math.abs(diff)));
					}
				}
				Collections.sort(diffAbs);
				ret.get(entry.getKey()).get(subEntry.getKey()).put(
						RangeType.stdev,
						n == 0 ? Double.NaN : Double.valueOf(Math
								.sqrt(sumDiffSquare.doubleValue() / n)));
				ret.get(entry.getKey()).get(subEntry.getKey()).put(
						RangeType.mad,
						n == 0 ? Double.NaN
								: Double.valueOf((n % 2 != 0 ? diffAbs
										.get(n / 2) : (diffAbs.get(n / 2)
										.doubleValue() + diffAbs.get(n / 2 - 1)
										.doubleValue()) / 2) * 1.4826));

			}
		}
	}

	/**
	 * @param table
	 *            A {@link DataTable}.
	 * @param specAnalyser
	 *            A {@link SpecAnalyser} belonging to {@code table}.
	 * 
	 */
	public SimpleModelBuilder(final DataTable table,
			final SpecAnalyser specAnalyser) {
		super();
		this.table = table;
		this.specAnalyser = specAnalyser;
	}

	/**
	 * @return the table
	 */
	public DataTable getTable() {
		return table;
	}

	/**
	 * Collects the values belonging to the parameters and statistics. (
	 * {@link Double#NaN}s are not filtered.
	 * 
	 * @return The map from parameters to a map from stats to values.
	 */
	public Map<String, Map<StatTypes, List<Double>>> computeAllVals() {
		final Map<String, Map<StatTypes, List<Double>>> ret = new TreeMap<String, Map<StatTypes, List<Double>>>();
		final EnumMap<StatTypes, Map<String, Integer>> indices = specAnalyser
				.getIndices();
		final Map<String, Integer> valueIndices = specAnalyser
				.getValueIndices();
		// indices.get(StatTypes.raw).putAll(valueIndices);
		if (!indices.containsKey(StatTypes.otherNumeric)) {
			indices.put(StatTypes.otherNumeric,
					new LinkedHashMap<String, Integer>());
		}
		indices.get(StatTypes.otherNumeric).putAll(valueIndices);
		for (final Entry<StatTypes, Map<String, Integer>> outerEntry : indices
				.entrySet()) {
			for (final String param : outerEntry.getValue().keySet()) {
				final EnumMap<StatTypes, List<Double>> valMap = new EnumMap<StatTypes, List<Double>>(
						StatTypes.class);
				if (!ret.containsKey(param)) {
					ret.put(param, valMap);
				}
				ret.get(param)
						.put(outerEntry.getKey(), new ArrayList<Double>());
			}
		}
		for (final DataRow row : table) {
			for (final Entry<StatTypes, Map<String, Integer>> outerEntry : indices
					.entrySet()) {
				for (final Entry<String, Integer> innerEntry : outerEntry
						.getValue().entrySet()) {
					final int index = innerEntry.getValue().intValue();
					final DataCell cell = row.getCell(index);
					if (cell instanceof DoubleValue) {
						final DoubleValue val = (DoubleValue) cell;
						ret.get(innerEntry.getKey()).get(outerEntry.getKey())
								.add(Double.valueOf(val.getDoubleValue()));
					}
				}
			}
		}
		return ret;
	}

	/**
	 * @return the {@link SpecAnalyser} associated to {@link #getTable() table}.
	 */
	public SpecAnalyser getSpecAnalyser() {
		return specAnalyser;
	}
}

/**
 * 
 */
package ie.tcd.imm.hits.knime.util;

import ie.tcd.imm.hits.knime.util.ModelBuilder.SpecAnalyser;
import ie.tcd.imm.hits.knime.view.heatmap.HeatmapNodeModel.StatTypes;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.EnumMap;
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
	protected final DataTable table;
	protected final SpecAnalyser specAnalyser;

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
	 * @param specAnalyser
	 * @param table
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

	public Map<String, Map<StatTypes, List<Double>>> computeAllVals() {
		final Map<String, Map<StatTypes, List<Double>>> ret = new TreeMap<String, Map<StatTypes, List<Double>>>();
		final EnumMap<StatTypes, Map<String, Integer>> indices = specAnalyser
				.getIndices();
		final Map<String, Integer> valueIndices = specAnalyser
				.getValueIndices();
		indices.get(StatTypes.raw).putAll(valueIndices);
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

}

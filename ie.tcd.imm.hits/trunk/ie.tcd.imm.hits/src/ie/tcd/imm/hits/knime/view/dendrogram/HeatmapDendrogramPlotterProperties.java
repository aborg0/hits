/**
 * 
 */
package ie.tcd.imm.hits.knime.view.dendrogram;

import ie.tcd.imm.hits.knime.view.heatmap.HeatmapNodeModel.StatTypes;
import ie.tcd.imm.hits.util.swing.colour.ColourSelector;
import ie.tcd.imm.hits.util.swing.colour.ColourSelector.ColourModel;
import ie.tcd.imm.hits.util.swing.colour.ColourSelector.RangeType;

import java.util.Collections;
import java.util.Map;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.swing.JScrollPane;

import org.knime.base.node.viz.plotter.dendrogram.DendrogramPlotterProperties;

import edu.umd.cs.findbugs.annotations.DefaultAnnotation;

/**
 * The properties for the dendrogram with heatmap node.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
@DefaultAnnotation( { Nonnull.class, CheckReturnValue.class })
public class HeatmapDendrogramPlotterProperties extends
		DendrogramPlotterProperties {
	private static final long serialVersionUID = -6241965284919230058L;
	private final ColourSelector colourSelector;

	/**
	 * Constructs the properties with a new tab, and default colours.
	 */
	public HeatmapDendrogramPlotterProperties() {
		super();
		colourSelector = new ColourSelector(Collections.<String> emptyList(),
				Collections.singleton(StatTypes.raw));
		addTab("Colours", new JScrollPane(colourSelector));
	}

	/**
	 * @return The {@link ColourModel} for the heatmap.
	 */
	public ColourModel getColourModel() {
		return colourSelector.getModel();
	}

	/**
	 * Updates the ranges for the heatmap.
	 * 
	 * @param parameters
	 *            The new parameters.
	 * @param ranges
	 *            The ranges belonging to the parameters, {@link StatTypes}.
	 */
	public void update(final Iterable<String> parameters,
			final Map<String, Map<StatTypes, Map<RangeType, Double>>> ranges) {
		colourSelector.update(parameters, Collections.singleton(StatTypes.raw),
				ranges);
	}
}

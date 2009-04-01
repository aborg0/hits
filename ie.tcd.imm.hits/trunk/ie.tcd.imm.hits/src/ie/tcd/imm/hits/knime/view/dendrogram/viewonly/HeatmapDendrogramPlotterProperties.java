/**
 * 
 */
package ie.tcd.imm.hits.knime.view.dendrogram.viewonly;

import ie.tcd.imm.hits.knime.view.heatmap.HeatmapNodeModel.StatTypes;
import ie.tcd.imm.hits.util.swing.colour.ColourSelector;
import ie.tcd.imm.hits.util.swing.colour.ColourSelector.ColourModel;
import ie.tcd.imm.hits.util.swing.colour.ColourSelector.RangeType;

import java.util.Collections;
import java.util.Map;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.swing.AbstractButton;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JToggleButton;
import javax.swing.SpinnerNumberModel;

import org.knime.base.node.viz.plotter.dendrogram.DendrogramPlotterProperties;
import org.knime.base.node.viz.plotter.props.DefaultTab;

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
	private final JButton zoomOut = new JButton("Zoom out");
	private final JSpinner cellWidth = new JSpinner(new SpinnerNumberModel(20,
			2, 100, 2));
	private final JToggleButton flipHorizontal = new JToggleButton(
			"Flip horizontal", false);
	private final JToggleButton flipVertical = new JToggleButton(
			"Flip vertical");
	private final JCheckBox showValues = new JCheckBox("Show values");
	private final JSpinner clusterCount = new JSpinner(new SpinnerNumberModel(
			1, 1, 1, 1));

	/**
	 * Constructs the properties with a new tab, and default colours.
	 */
	public HeatmapDendrogramPlotterProperties() {
		super();
		colourSelector = new ColourSelector(Collections.<String> emptyList(),
				Collections.singleton(StatTypes.raw));
		addTab("Colours", new JScrollPane(colourSelector));
		final DefaultTab defaultTab = (DefaultTab) getComponentAt(0);
		final Box box = (Box) defaultTab.getComponent(0);
		box.add(zoomOut);
		box.add(new JLabel("Heatmap width: "));
		box.add(cellWidth);
		box.add(flipHorizontal);
		box.add(flipVertical);
		flipVertical.setEnabled(false);
		box.add(showValues);
		box.add(new JLabel("Clusters: "));
		box.add(clusterCount);
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

	/**
	 * @return The zoom out {@link JButton}.
	 */
	public JButton getZoomOut() {
		return zoomOut;
	}

	/**
	 * @return The cell width {@link JSpinner}
	 */
	public JSpinner getCellWidth() {
		return cellWidth;
	}

	/**
	 * @return The {@link AbstractButton} for horizontal direction.
	 */
	public AbstractButton getFlipHorizontal() {
		return flipHorizontal;
	}

	/**
	 * @return The {@link AbstractButton} for vertical order of nodes.
	 */
	public AbstractButton getFlipVertical() {
		return flipVertical;
	}

	/**
	 * @return The {@link AbstractButton} for showing the values.
	 */
	public AbstractButton getShowValues() {
		return showValues;
	}

	/**
	 * @return The {@link JSpinner} for the cluster count.
	 */
	public JSpinner getClusterCount() {
		return clusterCount;
	}
}

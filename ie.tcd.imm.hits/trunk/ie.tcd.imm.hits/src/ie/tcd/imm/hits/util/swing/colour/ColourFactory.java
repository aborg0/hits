/**
 * 
 */
package ie.tcd.imm.hits.util.swing.colour;

import ie.tcd.imm.hits.knime.view.heatmap.HeatmapNodeModel.StatTypes;
import ie.tcd.imm.hits.util.swing.colour.ColourSelector.ColourModel;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import edu.umd.cs.findbugs.annotations.DefaultAnnotation;

/**
 * Factory for the controls and the legends.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
@DefaultAnnotation( { Nonnull.class, CheckReturnValue.class })
public interface ColourFactory<Computer extends ColourComputer> {

	/**
	 * Should add a listener to the control.
	 * 
	 * @param stat
	 * @param parameter
	 * @param colourModel
	 * 
	 * @return
	 */
	public ColourControl<Computer> createControl(ColourModel colourModel,
			String parameter, StatTypes stat);

	// public void updateControl(
	// ColourControl<? extends ColourComputer<This>> control);

	public ColourLegend<Computer> createLegend();

	//
	// public void updateLegend(ColourLegend<? extends ColourComputer<This>>
	// legend);

	public Computer getDefaultModel();
}

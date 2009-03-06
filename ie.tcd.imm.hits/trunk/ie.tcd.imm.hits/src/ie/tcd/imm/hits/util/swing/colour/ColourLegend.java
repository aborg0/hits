/**
 * 
 */
package ie.tcd.imm.hits.util.swing.colour;

import ie.tcd.imm.hits.util.swing.colour.ColourSelector.SampleWithText.Orientation;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import edu.umd.cs.findbugs.annotations.DefaultAnnotation;

/**
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 * 
 */
@DefaultAnnotation( { Nonnull.class, CheckReturnValue.class })
public interface ColourLegend<T extends ColourComputer> {
	public void setModel(T model, Orientation orientation);
}

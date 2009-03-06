/**
 * 
 */
package ie.tcd.imm.hits.util.swing.colour;

import java.awt.Color;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import edu.umd.cs.findbugs.annotations.DefaultAnnotation;

/**
 * Implementations compute the {@link Color} value for a {@code double} value.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
@DefaultAnnotation( { Nonnull.class, CheckReturnValue.class })
public interface ColourComputer {
	/**
	 * Computes a {@link Color} for the {@code val}.
	 * 
	 * @param val
	 *            A {@code double} value.
	 * @return The {@link Color} belonging to {@code val}.
	 */
	public Color compute(double val);

	public String getTooltip();
}

package ie.tcd.imm.hits.knime.util;

import java.awt.Color;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import edu.umd.cs.findbugs.annotations.DefaultAnnotation;

/**
 * This class gives some visual representation related methods.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
@DefaultAnnotation( { Nonnull.class, CheckReturnValue.class })
public class VisualUtils {

	/**
	 * Selects the colour for a value {@code d} based on the other parameters.
	 * <p>
	 * There must be the following order:<br>
	 * if {@code black} is {@code null}:<br>
	 * {@code low < high}<br>
	 * else:<br>
	 * {@code low < mid < high}.
	 * 
	 * @param d
	 *            A value for colour.
	 * @param blue
	 *            The "down" {@link Color}.
	 * @param black
	 *            The "middle" or natural {@link Color}.
	 * @param red
	 *            The "up" {@link Color}.
	 * @param low
	 *            The "down" value. Must be finite.
	 * @param mid
	 *            The "middle" or natural value. May be anything if
	 *            {@code black} is {@code null}.
	 * @param high
	 *            The "up" value. Must be finite.
	 * @return The {@link Color} belonging for {@code d}.
	 */
	public static Color colourOf(final double d, final Color blue, @Nullable
	final Color black, final Color red, final double low, final double mid,
			final double high) {
		if (d < low) {
			return blue;
		}
		if (d > high) {
			return red;
		}
		if (black == null) {
			return new Color(((float) (blue.getRed() + (d - low) / (high - low)
					* (red.getRed() - blue.getRed())) / 256.0f), ((float) (blue
					.getGreen() + (d - low) / (high - low)
					* (red.getGreen() - blue.getGreen())) / 256.0f),
					(float) (blue.getBlue() / 256.0f + (d - low) / (high - low)
							* (red.getBlue() - blue.getBlue()) / 256.0f));
		}
		if (d < mid) {
			return new Color((float) (black.getRed() / 256.0f + (mid - d)
					/ (mid - low) * (blue.getRed() - black.getRed()) / 256.0f),
					(float) (black.getGreen() / 256.0f + (mid - d)
							/ (mid - low)
							* (blue.getGreen() - black.getGreen()) / 256.0f),
					(float) (black.getBlue() / 256.0f + (mid - d) / (mid - low)
							* (blue.getBlue() - black.getBlue()) / 256.0f));
		}
		return new Color((float) (black.getRed() / 256.0f + (d - mid)
				/ (high - mid) * (red.getRed() - black.getRed()) / 256.0f),
				(float) (black.getGreen() / 256.0f + (d - mid) / (high - mid)
						* (red.getGreen() - black.getGreen()) / 256.0f),
				(float) (black.getBlue() / 256.0f + (d - mid) / (high - mid)
						* (red.getBlue() - black.getBlue()) / 256.0f));
	}

}

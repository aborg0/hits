/**
 * 
 */
package ie.tcd.imm.hits.knime.cellhts2.prefs;

import javax.annotation.Nonnull;

/**
 * Something that has a text to display. It is useful if for different objects
 * it has different texts.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
public interface Displayable {
	/**
	 * @return The display text of the object.
	 */
	public @Nonnull
	String getDisplayText();
}

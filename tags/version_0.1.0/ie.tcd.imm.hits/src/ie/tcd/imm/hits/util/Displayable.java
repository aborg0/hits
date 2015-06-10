/**
 * 
 */
package ie.tcd.imm.hits.util;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

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

	/**
	 * Utility method(s) for {@link Displayable} handling.
	 */
	public static final class Util {
		// Hiding constructor.
		private Util() {
			super();
		}

		/**
		 * Finds the first element from {@code values}, whose
		 * {@link Displayable#getDisplayText()} is {@code text}.
		 * 
		 * @param <EnumType>
		 *            An {@link Enum} extending {@link Displayable}.
		 * @param text
		 *            A non-{@code null} {@link String}.
		 * @param values
		 *            The values of {@code EnumType}.
		 * @return The first element from {@code values}, whose
		 *         {@link Displayable#getDisplayText()} is {@code text}.
		 */
		public static @Nullable
		<EnumType extends Enum<EnumType> & Displayable> EnumType findByDisplayText(
				final String text, final EnumType... values) {
			for (final EnumType value : values) {
				if (text.equals(value.getDisplayText())) {
					return value;
				}
			}
			return null;
		}
	}
}

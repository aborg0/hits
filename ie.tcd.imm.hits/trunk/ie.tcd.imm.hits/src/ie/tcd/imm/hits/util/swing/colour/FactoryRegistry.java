/**
 * 
 */
package ie.tcd.imm.hits.util.swing.colour;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import edu.umd.cs.findbugs.annotations.DefaultAnnotation;

/**
 * A registry for the {@link ColourFactory}s.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
@DefaultAnnotation( { Nonnull.class, CheckReturnValue.class })
public class FactoryRegistry {
	private static final FactoryRegistry instance = new FactoryRegistry();

	static {
		instance.registerFactory(ComplexModel.class, new ComplexModelFactory());
		instance.registerFactory(ContinuousModel.class,
				ColourSelector.DEFAULT_MODEL);
	}

	/**
	 * @return The singleton instance.
	 */
	public static FactoryRegistry getInstance() {
		return instance;
	}

	private final Map<Class<? extends ColourComputer>, ColourFactory<?>> factories = new HashMap<Class<? extends ColourComputer>, ColourFactory<?>>();

	/**
	 * Gets the associated {@link ColourFactory} for {@code parameter} and
	 * {@code stat}. It may return {@code null}.
	 * 
	 * @param computer
	 *            A {@link ColourComputer}.
	 * @param <Computer>
	 *            The type of {@code computer}.
	 * 
	 * @return The associated {@link ColourFactory} or {@code null}.
	 */
	public @Nullable
	<Computer extends ColourComputer> ColourFactory<Computer> getFactory(
			final Computer computer) {
		@SuppressWarnings("unchecked")
		// It is OK, we know it is correctly registered
		final ColourFactory<Computer> ret = (ColourFactory<Computer>) factories
				.get(computer.getClass());
		return ret;
		// @SuppressWarnings("unchecked")
		// final ColourFactory<Computer> ret = (ColourFactory<Computer>)
		// computer;
		// return ret;
		// return (ColourFactory<Computer>) new ComplexModelFactory();
	}

	/**
	 * Registers a {@link ColourFactory} to a {@link ColourComputer}
	 * implementation.
	 * 
	 * @param <Computer>
	 *            The type of the {@link ColourComputer}.
	 * @param cls
	 *            The class of {@link ColourComputer}.
	 * @param factory
	 *            The {@link ColourFactory} to register.
	 */
	public <Computer extends ColourComputer> void registerFactory(
			final Class<? extends Computer> cls,
			final ColourFactory<Computer> factory) {
		factories.put(cls, factory);
	}
}

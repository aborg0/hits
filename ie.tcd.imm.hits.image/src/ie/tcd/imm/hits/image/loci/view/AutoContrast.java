/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package ie.tcd.imm.hits.image.loci.view;

import ij.process.LUT;

import java.awt.event.ActionEvent;
import java.util.Arrays;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;

/**
 * Performs auto contrast based on the specified parameters and the information
 * available in the histogram.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
public class AutoContrast extends AbstractAction {
	private static final long serialVersionUID = 8068953847116850572L;
	private final AutoContrastStrategy strategy;
	private final double[] additionalParameters;

	/** The default strategy to keep the last settings. */
	public static final AutoContrast keepLast = new AutoContrast("Keep last",
			AutoContrastStrategy.KeepLast);

	/**
	 * An ActionEvent with the image's histogram for one channel.
	 */
	public static class ContrastActionEvent extends ActionEvent {
		private static final long serialVersionUID = 2072789870816299042L;
		private final int[] histogram;
		private final LUT lut;

		/**
		 * @param source
		 *            Same as {@link ActionEvent#getSource() source}.
		 * @param id
		 *            Same as {@link ActionEvent#getID() id}.
		 * @param command
		 *            Same as {@link ActionEvent#getActionCommand() command}.
		 * @param when
		 *            Same as {@link ActionEvent#getWhen() when}.
		 * @param modifiers
		 *            Same as {@link ActionEvent#getModifiers() modifiers}.
		 * @param histogram
		 *            The histogram to use for contrast computations.
		 * @param lut
		 *            The {@link LUT} to modify.
		 */
		public ContrastActionEvent(final Object source, final int id,
				final String command, final long when, final int modifiers,
				final int[] histogram, final LUT lut) {
			super(source, id, command, when, modifiers);
			this.histogram = histogram.clone();
			this.lut = lut;
		}

	}

	/**
	 * @param strategy
	 *            The {@link AutoContrastStrategy}.
	 * @param additionalParameters
	 *            The additional parameters for {@code strategy}.
	 * 
	 */
	public AutoContrast(final AutoContrastStrategy strategy,
			final double... additionalParameters) {
		this("", strategy, additionalParameters);
	}

	/**
	 * @param name
	 *            The name of the {@link Action}.
	 * @param strategy
	 *            The {@link AutoContrastStrategy}.
	 * @param additionalParameters
	 *            The additional parameters for {@code strategy}.
	 */
	public AutoContrast(final String name, final AutoContrastStrategy strategy,
			final double... additionalParameters) {
		this(name, null, strategy, additionalParameters);
	}

	/**
	 * @param name
	 *            The name of the {@link Action}.
	 * @param icon
	 *            The icon of the {@link Action}.
	 * @param strategy
	 *            The {@link AutoContrastStrategy}.
	 * @param additionalParameters
	 *            The additional parameters for {@code strategy}.
	 */
	public AutoContrast(final String name, final Icon icon,
			final AutoContrastStrategy strategy,
			final double... additionalParameters) {
		super(name, icon);
		this.strategy = strategy;
		this.additionalParameters = additionalParameters.clone();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	@Override
	public void actionPerformed(final ActionEvent e) {
		if (e instanceof ContrastActionEvent) {
			final ContrastActionEvent event = (ContrastActionEvent) e;
			strategy.changeLut(event.lut, event.histogram, additionalParameters
					.clone());
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(additionalParameters);
		result = prime * result + (strategy == null ? 0 : strategy.hashCode());
		return result;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final AutoContrast other = (AutoContrast) obj;
		if (!Arrays.equals(additionalParameters, other.additionalParameters)) {
			return false;
		}
		if (strategy == null) {
			if (other.strategy != null) {
				return false;
			}
		} else if (!strategy.equals(other.strategy)) {
			return false;
		}
		if (this.enabled != other.enabled) {
			return false;
		}
		if (getKeys() == null) {
			return other.getKeys() == null;
		}
		for (final Object key : getKeys()) {
			if (key instanceof String) {
				final String str = (String) key;
				if (!getValue(str).equals(other.getValue(str))) {
					return false;
				}
			}
		}
		return true;
	}

	@Override
	public String toString() {
		final String string = strategy + " "
				+ Arrays.toString(additionalParameters);
		return enabled ? string : "-~" + string + "~-";
	}
}

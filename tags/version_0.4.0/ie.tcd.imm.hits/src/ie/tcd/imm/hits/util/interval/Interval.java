/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package ie.tcd.imm.hits.util.interval;

import java.io.Serializable;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import edu.umd.cs.findbugs.annotations.DefaultAnnotation;

/**
 * Represents the mathematical interval on double values.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 * @param <Content>
 *            The type of the contained elements.
 */
@DefaultAnnotation( { Nonnull.class, CheckReturnValue.class })
public interface Interval<Content extends Comparable<? super Content>> extends
		Comparable<Interval<? extends Content>> {
	/**
	 * @return The lower endpoint.
	 */
	public Content getLow();

	/**
	 * @return The higher endpoint.
	 */
	public Content getHigh();

	/**
	 * @param val
	 *            A value.
	 * @return If {@code val} in the interval {@code true} else {@code false}.
	 */
	public boolean contains(final Content val);

	/**
	 * @return The low endpoint is closed?
	 */
	public boolean isClosedLow();

	/**
	 * @return The high endpoint is closed?
	 */
	public boolean isClosedHigh();

	/**
	 * An immutable implementation of {@link Interval}.
	 * 
	 * @param <Content>
	 *            Type of the contained elements.
	 */
	@DefaultAnnotation( { Nonnull.class, CheckReturnValue.class })
	public static final class DefaultInterval<Content extends Comparable<Content> & Serializable>
			implements Interval<Content>, Serializable {
		private static final long serialVersionUID = -3298761805751950358L;
		private final Content low;
		private final Content high;
		private final boolean closedLow;
		private final boolean closedHigh;

		/**
		 * Contstucts the interval.
		 * 
		 * @param low
		 *            The low endpoint.
		 * @param high
		 *            The high endpoint.
		 * @param closedLow
		 *            The low endpoint is closed or not.
		 * @param closedHigh
		 *            The high endpoint is closed or not.
		 */
		public DefaultInterval(final Content low, final Content high,
				final boolean closedLow, final boolean closedHigh) {
			this.low = low;
			this.high = high;
			this.closedLow = closedLow;
			this.closedHigh = closedHigh;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * ie.tcd.imm.hits.util.interval.Interval#contains(java.lang.Comparable)
		 */
		@Override
		public boolean contains(final Content val) {
			final int compLow = getLow().compareTo(val);
			final int compHigh = getHigh().compareTo(val);
			if (compLow <= 0 && compHigh >= 0) {
				return (compLow != 0 || closedLow)
						&& (compHigh != 0 || closedHigh);
			}
			return false;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see ie.tcd.imm.hits.util.interval.Interval#getHigh()
		 */
		@Override
		public Content getHigh() {
			return high;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see ie.tcd.imm.hits.util.interval.Interval#getLow()
		 */
		@Override
		public Content getLow() {
			return low;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.lang.Comparable#compareTo(java.lang.Object)
		 */
		@Override
		public int compareTo(final Interval<? extends Content> o) {
			final int lowComp = low.compareTo(o.getLow());
			if (lowComp != 0) {
				// assert high.compareTo(o.getHigh()) == lowComp :
				// this.toString()
				// + " o: " + o;
				return lowComp;
			}
			if (closedLow && !o.isClosedLow()) {
				return -1;
			}
			if (!closedLow && o.isClosedLow()) {
				return 1;
			}
			final int highComp = high.compareTo(o.getHigh());
			if (highComp == 0) {
				if (closedHigh && !o.isClosedHigh()) {
					return 1;
				}
				if (!closedHigh && o.isClosedHigh()) {
					return -1;
				}
			}
			return highComp;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see ie.tcd.imm.hits.util.interval.Interval#isClosedLow()
		 */
		@Override
		public boolean isClosedLow() {
			return closedLow;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see ie.tcd.imm.hits.util.interval.Interval#isClosedHigh()
		 */
		@Override
		public boolean isClosedHigh() {
			return closedHigh;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			return (closedLow ? '[' : '(') + low.toString() + ", " + high
					+ (closedHigh ? ']' : ')');
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + (closedHigh ? 1231 : 1237);
			result = prime * result + (closedLow ? 1231 : 1237);
			result = prime * result + (high == null ? 0 : high.hashCode());
			result = prime * result + (low == null ? 0 : low.hashCode());
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
			final DefaultInterval<?> other = (DefaultInterval<?>) obj;
			if (closedHigh != other.closedHigh) {
				return false;
			}
			if (closedLow != other.closedLow) {
				return false;
			}
			if (high == null) {
				if (other.high != null) {
					return false;
				}
			} else if (!high.equals(other.high)) {
				return false;
			}
			if (low == null) {
				if (other.low != null) {
					return false;
				}
			} else if (!low.equals(other.low)) {
				return false;
			}
			return true;
		}
	}
}

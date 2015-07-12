/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package ie.tcd.imm.hits.image.loci.view;

import ie.tcd.imm.hits.util.Displayable;
import ij.process.LUT;

import java.util.Arrays;

/**
 * An enum for the simple strategies of auto contrast.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
public enum AutoContrastStrategy implements Displayable {

	/**
	 * Simply keeps the last set values, does not change. (If no previous set,
	 * uses {@link #MinModeMax}.)
	 */
	KeepLast {
		@Override
		public void changeLut(final LUT lut, final int[] histogram,
				final double... additionalParams)/* => */{
			if (Double.isInfinite(lut.min) || Double.isNaN(lut.max)) {
				MinModeMax.changeLut(lut, histogram);
			}
		}

		@Override
		public String getDisplayText() /* => */{
			return "Keep last";
		}
	},
	/** Sets according to the current min/max values for each channel. */
	MinModeMax {
		@Override
		public void changeLut(final LUT lut, final int[] histogram,
				final double... additionalParams)/* => */{
			double left, right;
			switch (additionalParams.length) {
			case 0:
				left = right = 1.0;
				break;
			case 1:
				left = right = additionalParams[0];
				break;
			case 2:
				left = additionalParams[0];
				right = additionalParams[1];
				break;
			default:
				throw new IllegalArgumentException("Too many parameters: "
						+ Arrays.toString(additionalParams));
			}
			final double min = findMin(histogram), max = findMax(histogram), mode = mode(histogram);
			lut.min = Math.abs(mode - min) < EPSILON ? min : mode - left
					* (mode - min);
			lut.max = Math.abs(max - mode) < EPSILON ? max : mode + right
					* (max - mode);
		}

		@Override
		public String getDisplayText()/* => */{
			return "Min/Max";
		}
	},
	/**
	 * Finds the most common value, and selects the best fitting Gauss curve's
	 * selected neighbourhood.
	 */
	FirstHighest {
		@Override
		public void changeLut(final LUT lut, final int[] histogram,
				final double... additionalParams)/* => */{
			double left, right;
			switch (additionalParams.length) {
			case 0:
				left = right = 1.0;
				break;
			case 1:
				left = right = additionalParams[0];
				break;
			case 2:
				left = additionalParams[0];
				right = additionalParams[1];
				break;
			default:
				throw new IllegalArgumentException("Too many parameters: "
						+ Arrays.toString(additionalParams));
			}
			final double mode = avg(histogram);
			final double stdDev = findBestFittingGauss(histogram, mode);
			lut.min = Math.abs(stdDev) < 1 ? mode - left : mode - left * stdDev;
			lut.max = Math.abs(stdDev) < 1 ? mode + right : mode + right
					* stdDev;
		}

		@Override
		public String getDisplayText()/* => */{
			return "Around most common";
		}
	},
	/**
	 * Finds the most common value, and selects the best fitting Gauss curve,
	 * removes those values and uses the same algorithm as
	 * {@link AutoContrastStrategy#FirstHighest} in the remaining parts.
	 */
	SecondHighest {
		@Override
		public void changeLut(final LUT lut, final int[] histogram,
				final double... additionalParams)/* => */{
			final double mode = mode(histogram);
			// final long n = sum(histogram);

			final int[] removedGauss = removeBump(histogram, mode);
			// removeGauss((int) mode,
			// findBestFittingGauss(histogram, mode), histogram, n);

			// final int[] diff = new int[histogram.length];
			// for (int i = diff.length; i-- > 0;) {
			// diff[i] = histogram[i] - removedGauss[i];
			// }
			// debugView(histogram, removedGauss, diff);
			FirstHighest.changeLut(lut, removedGauss, additionalParams);
		}

		@Override
		public String getDisplayText() {
			return "Around second most common";
		}
	},
	/** Similar to {@link #SecondHighest}, but with one more iteration. */
	ThirdHighest {
		@Override
		public void changeLut(final LUT lut, final int[] histogram,
				final double... additionalParams) /* => */{
			final double mode = mode(histogram);
			// final long n = sum(histogram);
			final int[] removedGauss = removeBump(histogram, mode);
			// removeGauss((int) mode,
			// findBestFittingGauss(histogram, mode), histogram, n);
			SecondHighest.changeLut(lut, removedGauss, additionalParams);
		}

		@Override
		public String getDisplayText() {
			return "Around third most common";
		}
	};

	/**
	 * Changes the {@code lut} based on {@code histogram, additionalParams}.
	 * 
	 * @param lut
	 *            The {@link LUT} to change.
	 * @param histogram
	 *            A histogram.
	 * @param additionalParams
	 *            Additional parameters.
	 */
	public abstract void changeLut(final LUT lut, final int[] histogram,
			final double... additionalParams);

	/**
	 * @param histogram
	 *            A histogram.
	 * @param highValue
	 *            The index where there is a high value in the {@code histogram}
	 *            array.
	 * @return A histogram without the largest "bump".
	 */
	protected int[] removeBump(final int[] histogram, final double highValue) {
		final int[] ret = new int[histogram.length];
		final double[] posAndSlope = posAndSlope(histogram, highValue);
		for (int i = ret.length; i-- > 0;) {
			if (i > posAndSlope[2]) {
				ret[i] = histogram[i];
				// Math.max(0, (int) (histogram[i] - (i - posAndSlope[2])
				// * histogram[(int) posAndSlope[2]] * posAndSlope[3]));
			} else if (i < posAndSlope[0]) {
				ret[i] = histogram[i];
				// Math.max(0, (int) (histogram[i] - (i - posAndSlope[0]
				// * histogram[(int) posAndSlope[0]] * posAndSlope[1])));
			}
		}
		return ret;
	}

	/**
	 * Tries to identify the proper positions and slopes on each side of {@code
	 * mode}.
	 * 
	 * @param histogram
	 *            A histogram array.
	 * @param mode
	 *            The index of a large value in {@code histogram}.
	 * @return The values:
	 *         <ol start="0">
	 *         <li>Left position</li>
	 *         <li>Left slope</li>
	 *         <li>Right position</li>
	 *         <li>Right slope</li>
	 *         </ol>
	 */
	private double[] posAndSlope(final int[] histogram, final double mode) {
		final double[] ret = new double[4];
		for (int i = (int) mode + 5; i < histogram.length; ++i) {
			double maxSlope = 0;
			for (int j = 2; j < 5; ++j) {
				final int slope = Math.abs(histogram[i] - histogram[i - j]) / j;
				if (slope > maxSlope) {
					maxSlope = slope;
				}
			}
			if (maxSlope < 20) {// FIXME find a better constant
				ret[2] = i;
				ret[3] = maxSlope;
				break;
			}
		}
		for (int i = (int) mode - 5; i-- > 0;) {
			double maxSlope = 0;
			for (int j = 2; j < 5; ++j) {
				final int slope = Math.abs(histogram[i] - histogram[i + j]) / j;
				if (slope > maxSlope) {
					maxSlope = slope;
				}
			}
			if (maxSlope < 20) {// FIXME find a better constant
				ret[0] = i;
				ret[1] = maxSlope;
				break;
			}
		}
		return ret;
	}

	// /** */
	// BrightField;

	/**
	 * Computes the standard deviation like measure based on {@code mode}.
	 * 
	 * @param histogram
	 *            A histogram array.
	 * @param mode
	 *            A large value in {@code histogram}.
	 * @return The standard deviation of histogram
	 */
	protected double findBestFittingGauss(final int[] histogram,
			final double mode) {
		long sum = 0;
		// long sumX = 0;
		double sumSquare = 0.0;
		for (int i = 0; i < histogram.length; i++) {
			sum += histogram[i];
			// sumX += i * histogram[i];
		}
		// final double avg = 1.0 * sumX / sum;
		for (int i = 0; i < histogram.length; i++) {
			final double diff = mode - i;
			sumSquare += histogram[i] * diff * diff;
		}
		return Math.sqrt(Math.abs(sumSquare / sum - mode * mode));
	}

	// private static int[] removeGauss(final int modeIndex, final double
	// stdDev,
	// final int[] histogram, final long n) {
	// final int[] ret = new int[histogram.length];
	// for (int i = histogram.length; i-- > 0;) {
	// ret[i] = Math.max(0, (int) (histogram[i] - n
	// * value(i, modeIndex, stdDev * stdDev)));
	// }
	// return ret;
	// }

	// /**
	// * @param i
	// * @param modeIndex
	// * @param var
	// * @return
	// */
	// private static double value(final double x, final double mean,
	// final double var) {
	// return oneDivSqrt2Pi / Math.sqrt(var)
	// * Math.exp(-(x - mean) * (x - mean) / 2 / var);
	// }

	/**
	 * Finds the maximal index of non-zero values in histogram.
	 * 
	 * @param histogram
	 *            An array of positive int values.
	 * @return The maximal index where the value is not {@code 0}, or the last
	 *         index if there were no such index ({@code -1} if the array is
	 *         empty).
	 */
	private static double findMax(final int[] histogram) {
		for (int i = histogram.length; i-- > 0;) {
			if (histogram[i] > 0) {
				return i;
			}
		}
		return histogram.length - 1;
	}

	/**
	 * Finds the minimal index of non-zero values in histogram.
	 * 
	 * @param histogram
	 *            An array of positive int values.
	 * @return The minimal index where the value is not {@code 0}, or the
	 *         {@code 0} if there were no such index.
	 */
	private static double findMin(final int[] histogram) {
		for (int i = 0; i < histogram.length; ++i) {
			if (histogram[i] > 0) {
				return i;
			}
		}
		return 0;
	}

	/**
	 * Finds the most common value from the histogram (the mode).
	 * 
	 * @param histogram
	 *            An array of positive int values.
	 * @return The mode value.
	 */
	private static double mode(final int[] histogram) {
		int firstCommon = histogram.length - 1, lastCommon = 0;
		int mode = 0;
		for (int i = histogram.length; i-- > 0;) {
			if (histogram[i] > mode) {
				firstCommon = i;
				lastCommon = i;
				mode = histogram[i];
			} else if (histogram[i] == mode) {
				lastCommon = i;
			}
		}
		return (firstCommon + lastCommon) / 2.0;
	}

	// private static long sum(final int[] histogram) {
	// long ret = 0;
	// for (final int i : histogram) {
	// ret += i;
	// }
	// return ret;
	// }

	private static double avg(final int[] histogram) {
		long ret = 0;
		long sum = 0;
		for (int i = histogram.length; i-- > 0;) {
			ret += histogram[i];
			sum += i * histogram[i];
		}
		return sum * 1.0 / ret;
	}

	// private static void debugView(final int[]... histograms) {
	// final JPanel panel = new JPanel();
	//
	// for (final int[] histogramInt : histograms) {
	//
	// final XYDataset histogram = createSerie(histogramInt);
	// final JFreeChart lineChart = ChartFactory.createXYLineChart(
	// "Histogram", null, null, histogram,
	// PlotOrientation.VERTICAL, true, true, false);
	//
	// lineChart.getXYPlot().getDomainAxis().setRange(
	// histogram.getXValue(0, 0),
	// histogram.getXValue(0, histogram.getItemCount(0) - 1));
	// final LogarithmicAxis rangeAxis = new LogarithmicAxis("Frequency");
	// // rangeAxis.setAutoRangeIncludesZero(true);
	// rangeAxis.setAllowNegativesFlag(true);
	// lineChart.getXYPlot().setRangeAxis(rangeAxis);
	// final ChartPanel chartPanel = new ChartPanel(lineChart);
	// panel.add(chartPanel);
	// }
	// JOptionPane.showOptionDialog(null, panel, "",
	// JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null,
	// null, null);
	//
	// }

	// /**
	// * @param histogramInt
	// * @return
	// */
	// private static XYDataset createSerie(final int[] histogram) {
	// final XYSeries ret = new XYSeries("");
	// final double[] dh = new double[histogram.length];
	// for (int i = dh.length; i-- > 0;) {
	// dh[i] = histogram[i];
	// }
	// int min = 0;
	// for (int i = 0; i < histogram.length; ++i) {
	// if (histogram[i] == 0) {
	// min = i;
	// } else {
	// break;
	// }
	// }
	// int max = histogram.length - 1;
	// for (int i = histogram.length; i-- > 0;) {
	// if (histogram[i] == 0) {
	// max = i;
	// } else {
	// break;
	// }
	// }
	// // final double[][] data = new double[2][max - min + 1];
	// for (int i = max - min; i-- > 0;) {
	// // data[0][i] = min + i;
	// // data[1][i] = histogram[i + min];
	// ret.add(min + i, histogram[i + min]);
	// }
	// return new XYSeriesCollection(ret);
	// }

	private static double EPSILON = 1e-6;
	// private static final double oneDivSqrt2Pi = 1.0 / Math.sqrt(2.0 *
	// Math.PI);
}

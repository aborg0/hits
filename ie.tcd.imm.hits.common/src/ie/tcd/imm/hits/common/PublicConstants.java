/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package ie.tcd.imm.hits.common;

import java.io.Serializable;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import edu.umd.cs.findbugs.annotations.DefaultAnnotation;

/**
 * This interface contains the public constants of HiTS project. Some of the
 * constants are in enums in the same package.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
@DefaultAnnotation( { Nonnull.class, CheckReturnValue.class })
public interface PublicConstants {

	/** The name of the replicate column. */
	public static final String REPLICATE_COLUMN = "Replicate";
	/** The name of the plate column. */
	public static final String PLATE_COLUMN = "Plate";
	/** The name of the experiment column. */
	public static final String EXPERIMENT_COLUMN = "Experiment";
	/** The name of the normalisation method column. */
	public static final String NORMALISATION_METHOD_COLUMN = "Normalisation method";
	/** The name of the log transform column. */
	public static final String LOG_TRANSFORM_COLUMN = "log transform";
	/** The name of the normalisation kind (additive/multiplicative) column. */
	public static final String NORMALISATION_KIND_COLUMN = "Normalisation kind";
	/** The name of the variance adjustment column. */
	public static final String VARIANCE_ADJUSTMENT_COLUMN = "Variance adjustment";
	/** The name of the scoring method column. */
	public static final String SCORING_METHOD_COLUMN = "Scoring method";
	/** The name of the summarise method column. */
	public static final String SUMMARISE_METHOD_COLUMN = "Summarise method";
	/** The plate column name in the result table. */
	public static final String PLATE_COL_NAME = PLATE_COLUMN;
	/** The replicate column name in the result table. */
	public static final String REPLICATE_COL_NAME = REPLICATE_COLUMN;
	/** The well column name in the result table. */
	public static final String WELL_COL_NAME = "Well";
	/** The gene id column name in the result table. */
	public static final String GENE_ID_COL_NAME = "GeneID";
	/** The gene annotation column name in the result table. */
	public static final String GENE_ANNOTATION_COL_NAME = "GeneSymbol";
	/** The plate identifier column name in LOCI nodes. */
	public static final String LOCI_PLATE = "LOCI-Plate";
	/** The row column name in LOCI nodes. */
	public static final String LOCI_ROW = "LOCI-Row";
	/** The 'column' column name in LOCI nodes. */
	public static final String LOCI_COLUMN = "LOCI-Column";
	/** The field column name in LOCI nodes. */
	public static final String LOCI_FIELD = "LOCI-Field";
	/** The LOCI join identifier column name in LOCI nodes. */
	public static final String LOCI_ID = "LOCI-Id";
	/** The LOCI serialised image content column in LOCI nodes. */
	public static final String LOCI_IMAGE_CONTENT = "LOCI";
	/**
	 * The image (series) identifier in sub-images for serialised image
	 * contents.
	 */
	public static final String IMAGE_ID = "Image-Id";

	/**
	 * Utility methods related to the constants.
	 */
	public static class StaticUtil implements Serializable {
		private static final long serialVersionUID = -6260847102729522225L;

		// Hide constructor
		private StaticUtil() {
			super();
		}

		/**
		 * Constructs the common column prefix for the {@code stat}
		 * {@link PossibleStatistics}.
		 * 
		 * @param stat
		 *            A {@link PossibleStatistics} value.
		 * @return The {@link PossibleStatistics#getDisplayText() display text}
		 *         followed by {@code _}.
		 */
		public static String createPrefix(final PossibleStatistics stat) {
			return stat.getDisplayText() + "_";
		}
	}
}

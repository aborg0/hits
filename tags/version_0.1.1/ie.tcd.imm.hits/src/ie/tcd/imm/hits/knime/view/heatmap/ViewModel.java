/**
 * 
 */
package ie.tcd.imm.hits.knime.view.heatmap;

import ie.tcd.imm.hits.common.Format;
import ie.tcd.imm.hits.knime.view.heatmap.ColourSelector.ColourModel;
import ie.tcd.imm.hits.knime.view.heatmap.ControlPanel.ArrangementModel;
import ie.tcd.imm.hits.knime.view.heatmap.HeatmapNodeModel.StatTypes;
import ie.tcd.imm.hits.knime.view.heatmap.ViewModel.WellViewModel.Places;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.TableModel;
import javax.xml.bind.annotation.XmlRootElement;

import edu.umd.cs.findbugs.annotations.DefaultAnnotation;

/**
 * This is responsible for the visual representation of the {@link Heatmap}s.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
@DefaultAnnotation( { Nonnull.class, CheckReturnValue.class })
@NotThreadSafe
@XmlRootElement(name = "viewmodel")
public class ViewModel implements ActionListener {
	private final WeakHashMap<ChangeListener, Boolean> changeListeners = new WeakHashMap<ChangeListener, Boolean>();
	private final WeakHashMap<ActionListener, Boolean> actionListeners = new WeakHashMap<ActionListener, Boolean>();

	/**
	 * The shape of the well representation.
	 */
	public static enum Shape {
		/** Circle based representation. */
		Circle(4, true, true),
		/** Rectangular representation. */
		Rectangle(0, true, true);
		private final int additionalInformationSlotsCount;
		private final boolean supportThickness;
		private final boolean supportSize;

		private Shape(final int additionalInformationSlots,
				final boolean supportThickness, final boolean supportSize) {
			this.additionalInformationSlotsCount = additionalInformationSlots;
			this.supportThickness = supportThickness;
			this.supportSize = supportSize;
		}

		/**
		 * @return At most this many additional informations can be shown.
		 */
		public int getAdditionalInformationSlotsCount() {
			return additionalInformationSlotsCount;
		}

		/**
		 * @return Does it supports the different thickness of the border?
		 */
		public boolean isSupportThickness() {
			return supportThickness;
		}

		/**
		 * @return Does it support showing a parameter affecting the size of the
		 *         result?
		 */
		public boolean isSupportSize() {
			return supportSize;
		}
	}

	/**
	 * The possible values of a parameter.
	 */
	public static enum ValueType {
		/** The values are distinct */
		Discrete,
		/** The values are from a real interval. */
		Continuous;
	}

	/**
	 * This class represents the layout of wells.
	 */
	public static class WellViewModel implements Serializable {
		private static final long serialVersionUID = 7800658256156783902L;

		/**
		 * This shows the possible places of the well splits.
		 */
		public static enum Places implements CompatibleValues {
			/** Split by the primary splitting position. */
			Primer(EnumSet.of(ValueType.Discrete, ValueType.Continuous)),
			/** Split by the secondary splitting position. */
			Seconder(EnumSet.of(ValueType.Discrete, ValueType.Continuous)),
			/** Additional positions for results. */
			Additional(EnumSet.of(ValueType.Discrete, ValueType.Continuous));
			private final EnumSet<ValueType> compatibleValues;

			private Places(final EnumSet<ValueType> compatibleValues) {
				this.compatibleValues = compatibleValues;
			}

			@Override
			public EnumSet<ValueType> getCompatibleValues() {
				return compatibleValues;
			}
		}
	}

	/**
	 * States what kind of values are accepted.
	 */
	public static interface CompatibleValues {
		/**
		 * @return The compatible values.
		 */
		public EnumSet<ValueType> getCompatibleValues();
	}

	/**
	 * This class is for the {@link SliderModel}s outside of the wells.
	 */
	public static class OverviewModel implements Serializable {
		private static final long serialVersionUID = -4371472124531160973L;

		/**
		 * The possible places of {@link SliderModel}s outside of the wells.
		 */
		public static enum Places implements CompatibleValues {
			/** These are for the rows. */
			Rows(EnumSet.of(ValueType.Discrete)),
			/** These are for the columns */
			Columns(EnumSet.of(ValueType.Discrete)),
			/** These are for selecting the value. */
			Choices(EnumSet.of(ValueType.Discrete)),
			/** These are only on the control panel. */
			Hidden(EnumSet.of(ValueType.Discrete));
			private final EnumSet<ValueType> compatibleValues;

			private Places(final EnumSet<ValueType> compatibleValues) {
				this.compatibleValues = compatibleValues;
			}

			@Override
			public EnumSet<ValueType> getCompatibleValues() {
				return compatibleValues;
			}
		}

		private final List<ParameterModel> rowModel = new ArrayList<ParameterModel>();
		private final List<ParameterModel> colModel = new ArrayList<ParameterModel>();
		private final List<ParameterModel> choiceModel = new ArrayList<ParameterModel>();

		/**
		 * Constructs an {@link OverviewModel} based on the given
		 * {@link ParameterModel}s.
		 * 
		 * @param rowModel
		 *            This describes the distribution of the result by row.
		 * @param colModel
		 *            This describes the distribution of the result by column.
		 * @param choiceModel
		 *            This describes the distribution of the result by selection
		 *            slider.
		 */
		public OverviewModel(final List<ParameterModel> rowModel,
				final List<ParameterModel> colModel,
				final List<ParameterModel> choiceModel) {
			super();
			this.rowModel.addAll(rowModel);
			this.colModel.addAll(colModel);
			this.choiceModel.addAll(choiceModel);
		}

		/**
		 * @return The selection slider's {@link ParameterModel}s.
		 */
		public List<ParameterModel> getChoiceModel() {
			return Collections.unmodifiableList(choiceModel);
		}

		/**
		 * @return The column's {@link ParameterModel}s.
		 */
		public List<ParameterModel> getColModel() {
			return Collections.unmodifiableList(colModel);
		}

		/**
		 * @return The row's {@link ParameterModel}s.
		 */
		public List<ParameterModel> getRowModel() {
			return Collections.unmodifiableList(rowModel);
		}
	}

	/**
	 * Describes the layout of a well.
	 */
	public static class ShapeModel implements Serializable {
		private static final long serialVersionUID = 7291082607986486046L;
		private final ArrangementModel arrangementModel;
		private final List<ParameterModel> primerParameters = new ArrayList<ParameterModel>();
		private final List<ParameterModel> secunderParameters = new ArrayList<ParameterModel>();
		private final List<ParameterModel> additionalParameters = new ArrayList<ParameterModel>();

		private final boolean drawBorder;
		private final boolean drawPrimaryBorders;
		private final boolean drawSecondaryBorders;
		private final boolean drawAdditionalBorders;

		private final int startAngle = 30;

		private ColourModel colourModel;

		/**
		 * Constructs a {@link ShapeModel} using the parameters.
		 * 
		 * @param arrangementModel
		 *            This describes the actual {@link SliderModel}s.
		 * @param primerParameters
		 *            This describes the primary split {@link ParameterModel}s.
		 * @param secunderParameters
		 *            This describes the secondary split {@link ParameterModel}s.
		 * @param additionalParameters
		 *            This describes the additional data {@link ParameterModel}s.
		 * @param drawBorder
		 *            If set draws a (rectangular) border around the well.
		 * @param drawPrimaryBorders
		 *            If set draws borders for the primary selections.
		 * @param drawSecondaryBorders
		 *            If set draws borders for the secondary selections.
		 * @param drawAdditionalBorders
		 *            If set draws borders for the additional data.
		 */
		public ShapeModel(final ArrangementModel arrangementModel,
				final List<ParameterModel> primerParameters,
				final List<ParameterModel> secunderParameters,
				final List<ParameterModel> additionalParameters,
				final boolean drawBorder, final boolean drawPrimaryBorders,
				final boolean drawSecondaryBorders,
				final boolean drawAdditionalBorders) {
			super();
			this.arrangementModel = arrangementModel;
			this.primerParameters.addAll(primerParameters);
			this.secunderParameters.addAll(secunderParameters);
			this.additionalParameters.addAll(additionalParameters);
			this.drawBorder = drawBorder;
			this.drawPrimaryBorders = drawPrimaryBorders;
			this.drawSecondaryBorders = drawSecondaryBorders;
			this.drawAdditionalBorders = drawAdditionalBorders;
			this.colourModel = new ColourModel();
		}

		/**
		 * Constructs a {@link ShapeModel} using the parameters.
		 * 
		 * @param arrangementModel
		 *            This describes the actual {@link SliderModel}s.
		 * @param primerParameters
		 *            This describes the primary split {@link ParameterModel}s.
		 * @param secunderParameters
		 *            This describes the secondary split {@link ParameterModel}s.
		 * @param additionalParameters
		 *            This describes the additional data {@link ParameterModel}s.
		 * @param drawBorders
		 *            If set draws every possible separator line, else it draws
		 *            none.
		 */
		public ShapeModel(final ArrangementModel arrangementModel,
				final List<ParameterModel> primerParameters,
				final List<ParameterModel> secunderParameters,
				final List<ParameterModel> additionalParameters,
				final boolean drawBorders) {
			this(arrangementModel, primerParameters, secunderParameters,
					additionalParameters, drawBorders, drawBorders,
					drawBorders, drawBorders);
		}

		/**
		 * Constructs a {@link ShapeModel} using the parameters.
		 * 
		 * @param arrangementModel
		 *            This describes the actual {@link SliderModel}s.
		 * @param model
		 *            The model to copy.
		 * @param place
		 *            A {@link Places} in the well.
		 * @param drawBorder
		 *            Sets the value of the border to at {@code place} to this
		 *            value.
		 */
		public ShapeModel(final ArrangementModel arrangementModel,
				final ShapeModel model, final WellViewModel.Places place,
				final boolean drawBorder) {
			super();
			this.arrangementModel = arrangementModel;
			this.primerParameters.addAll(model.primerParameters);
			this.secunderParameters.addAll(model.secunderParameters);
			this.additionalParameters.addAll(model.additionalParameters);
			if (place == null) {
				this.drawBorder = drawBorder;
				this.drawPrimaryBorders = model.drawPrimaryBorders;
				this.drawSecondaryBorders = model.drawSecondaryBorders;
				this.drawAdditionalBorders = model.drawAdditionalBorders;
			} else {
				switch (place) {
				case Primer:
					this.drawBorder = model.drawBorder;
					this.drawPrimaryBorders = drawBorder;
					this.drawSecondaryBorders = model.drawSecondaryBorders;
					this.drawAdditionalBorders = model.drawAdditionalBorders;
					break;
				case Seconder:
					this.drawBorder = model.drawBorder;
					this.drawPrimaryBorders = model.drawPrimaryBorders;
					this.drawSecondaryBorders = model.drawBorder;
					this.drawAdditionalBorders = model.drawAdditionalBorders;
					break;
				case Additional:
					this.drawBorder = model.drawBorder;
					this.drawPrimaryBorders = model.drawPrimaryBorders;
					this.drawSecondaryBorders = model.drawSecondaryBorders;
					this.drawAdditionalBorders = model.drawBorder;
					break;

				default:
					throw new IllegalStateException("Wrong type: " + place);
				}
			}
		}

		/**
		 * @return The primary split {@link ParameterModel}s.
		 */
		public List<ParameterModel> getPrimerParameters() {
			return Collections.unmodifiableList(primerParameters);
		}

		/**
		 * @return The secondary split {@link ParameterModel}s.
		 */
		public List<ParameterModel> getSeconderParameters() {
			return Collections.unmodifiableList(secunderParameters);
		}

		/**
		 * @return The additional data {@link ParameterModel}s.
		 */
		public List<ParameterModel> getAdditionalParameters() {
			return Collections.unmodifiableList(additionalParameters);
		}

		/**
		 * @return Draw outside (rectangular) border?
		 */
		public boolean isDrawBorder() {
			return drawBorder;
		}

		/**
		 * @return Draw lines between primary separators?
		 */
		public boolean isDrawPrimaryBorders() {
			return drawPrimaryBorders;
		}

		/**
		 * @return Draw lines between secondary separators?
		 */
		public boolean isDrawSecondaryBorders() {
			return drawSecondaryBorders;
		}

		/**
		 * @return Draw lines around additional data?
		 */
		public boolean isDrawAdditionalBorders() {
			return drawAdditionalBorders;
		}

		/**
		 * @return Starting angle of the primary separator (only used in the
		 *         {@link Shape#Circle} case).
		 */
		public int getStartAngle() {
			return startAngle;
		}

		/**
		 * @return The {@link ArrangementModel} associated to this
		 *         {@link ShapeModel}.
		 */
		public ArrangementModel getArrangementModel() {
			return arrangementModel;
		}

		/**
		 * @return the used {@link ColourModel}.
		 */
		public ColourModel getColourModel() {
			return colourModel;
		}

		/**
		 * Sets the new {@link ColourModel} to {@code colourModel}.
		 * 
		 * @param colourModel
		 *            the new {@link ColourModel}.
		 */
		public void setColourModel(final ColourModel colourModel) {
			this.colourModel = colourModel;
		}

		/**
		 * Updates the parameters.
		 * 
		 * @param possibleParameters
		 *            This contains the possible values.
		 * @see HeatmapNodeModel#getPossibleParameters()
		 */
		public void updateParameters(
				final Collection<ParameterModel> possibleParameters) {
			update(primerParameters, possibleParameters);
			update(secunderParameters, possibleParameters);
			update(additionalParameters, possibleParameters);
		}

		private void update(final List<ParameterModel> orig,
				final Collection<ParameterModel> possibleParameters) {
			final ArrayList<ParameterModel> copy = new ArrayList<ParameterModel>(
					orig);
			orig.clear();
			for (final ParameterModel pm : copy) {
				for (final ParameterModel good : possibleParameters) {
					final StatTypes type = good.getType();
					if (good.getShortName().equals(pm.getShortName())
							&& type == pm.getType()) {
						final ParameterModel merged = new ParameterModel(good
								.getShortName(), type, pm.getAggregateType(),
								good.getColumns(), good.getColumnValues());
						if (type.isDiscrete()) {
							merged.setValueCount(good.getValueCount());
							merged.getColorLegend().putAll(
									good.getColorLegend());
						} else {
							merged.setStartColor(good.getStartColor());
							merged.setMiddleColor(good.getEndColor());
							merged.setEndColor(good.getEndColor());
							merged.setRangeMin(good.getRangeMin());
							merged.setRangeMax(good.getRangeMax());
						}
						orig.add(merged);
						break;
					}
				}
			}
		}
	}

	/**
	 * This enum lists all possible statistics which can be shown/computed for
	 * the values.
	 */
	public static enum AggregateType {
		/** The median of the values based on the free parameters. */
		Median,
		/** The mean of the values based on the free parameters. */
		Mean,
		/** The standard deviation of the values based on the free parameters. */
		StandardDeviation,
		/** The MAD of the values based on the free parameters. */
		MedianAbsoluteDeviation,
		/** The minimum of the values based on the free parameters. */
		Minimum,
		/** The maximum of the values based on the free parameters. */
		Maximum;
	}

	/**
	 * This is an important concept in the view description. This describes the
	 * parameters' properties, like name, possible values, aggregations,
	 * colours, ranges, ...
	 */
	public static class ParameterModel {
		private final String shortName;// Maybe from row names
		private @Nullable
		final AggregateType aggregateType;
		private int valueCount;// The count of different values
		private final List<String> columns;
		private final List<String> columnValues;
		private double rangeMin, rangeMax;
		private Color startColor, middleColor, endColor;
		private final Map<Object, Color> colorLegend = new HashMap<Object, Color>();
		private final StatTypes type;

		/**
		 * Constructs a {@link ParameterModel} using the given parameters.
		 * 
		 * @param shortName
		 *            A short name for the {@link ParameterModel}.
		 * @param type
		 *            The {@link StatTypes} it is belonging to.
		 * @param aggregateType
		 *            The aggregation, may be {@code null}. This shows if there
		 *            will be an aggregation by this parameter.
		 * @param columns
		 *            These columns are represented in the original
		 *            {@link TableModel}s. (Maybe not necessary to have.)
		 * @param columnValues
		 *            These values are used, may be empty, meaning all.
		 */
		public ParameterModel(final String shortName, final StatTypes type,
				@Nullable
				final AggregateType aggregateType, final List<String> columns,
				final List<String> columnValues) {
			super();
			this.shortName = shortName;
			this.type = type;
			this.aggregateType = aggregateType;
			this.columns = columns;
			this.columnValues = columnValues;
		}

		/**
		 * @return How many different values are used.
		 */
		@Deprecated
		public int getValueCount() {
			return valueCount;
		}

		/**
		 * Sets the number of possible different values.
		 * 
		 * @param valueCount
		 *            The number of possible different values.
		 */
		@Deprecated
		public void setValueCount(final int valueCount) {
			assert type.isDiscrete() : type;
			this.valueCount = valueCount;
		}

		/**
		 * @return The lower bound for the values.
		 */
		public double getRangeMin() {
			return rangeMin;
		}

		/**
		 * Sets the lower bound for the values.
		 * 
		 * @param rangeMin
		 *            The new lower bound for the values.
		 */
		public void setRangeMin(final double rangeMin) {
			assert !type.isDiscrete() : type;
			this.rangeMin = rangeMin;
		}

		/**
		 * @return The upper bound for the values.
		 */
		public double getRangeMax() {
			return rangeMax;
		}

		/**
		 * Sets the upper bound for the values.
		 * 
		 * @param rangeMax
		 *            The new upper bound for the values.
		 */
		public void setRangeMax(final double rangeMax) {
			assert !type.isDiscrete();
			this.rangeMax = rangeMax;
		}

		/**
		 * @return The {@link Color} for the lower bound.
		 */
		public Color getStartColor() {
			return startColor;
		}

		/**
		 * Sets the colour for the lower values.
		 * 
		 * @param startColor
		 *            The colour for the lower values.
		 */
		public void setStartColor(final Color startColor) {
			assert !type.isDiscrete() : type;
			this.startColor = startColor;
		}

		/**
		 * @return The {@link Color} for the neutral value.
		 */
		public Color getMiddleColor() {
			return middleColor;
		}

		/**
		 * Sets the colour for the neutral values.
		 * 
		 * @param middleColor
		 *            The colour for the neutral values.
		 */
		public void setMiddleColor(final Color middleColor) {
			assert !type.isDiscrete() : type;
			this.middleColor = middleColor;
		}

		/**
		 * @return The {@link Color} for the upper bound.
		 */
		public Color getEndColor() {
			return endColor;
		}

		/**
		 * Sets the colour for the upper values.
		 * 
		 * @param endColor
		 *            The colour for the upper values.
		 */
		public void setEndColor(final Color endColor) {
			assert !type.isDiscrete() : type;
			this.endColor = endColor;
		}

		/**
		 * @return The name of this {@link ParameterModel}.
		 */
		public String getShortName() {
			return shortName;
		}

		/**
		 * @return The {@link AggregateType} for this {@link ParameterModel}.
		 */
		public AggregateType getAggregateType() {
			return aggregateType;
		}

		/**
		 * @return The columns from the {@link HeatmapNodeModel}.
		 */
		public List<String> getColumns() {
			return columns;
		}

		/**
		 * @return The values used from the columns.
		 */
		public List<String> getColumnValues() {
			return columnValues;
		}

		/**
		 * @return The color legend for different values.
		 */
		public Map<Object, Color> getColorLegend() {
			assert type.isDiscrete() : type;
			return colorLegend;
		}

		/**
		 * @return The {@link StatTypes} represented.
		 */
		public StatTypes getType() {
			return type;
		}

		@Override
		public String toString() {
			return getShortName()
					+ " "
					+ getValueCount()
					+ ": "
					+ (getType().isDiscrete() ? getColorLegend().keySet()
							: rangeMin + " " + rangeMax);
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result
					+ ((aggregateType == null) ? 0 : aggregateType.hashCode());
			result = prime * result
					+ ((colorLegend == null) ? 0 : colorLegend.hashCode());
			result = prime * result
					+ ((columnValues == null) ? 0 : columnValues.hashCode());
			result = prime * result
					+ ((columns == null) ? 0 : columns.hashCode());
			result = prime * result
					+ ((endColor == null) ? 0 : endColor.hashCode());
			result = prime * result
					+ ((middleColor == null) ? 0 : middleColor.hashCode());
			long temp;
			temp = Double.doubleToLongBits(rangeMax);
			result = prime * result + (int) (temp ^ (temp >>> 32));
			temp = Double.doubleToLongBits(rangeMin);
			result = prime * result + (int) (temp ^ (temp >>> 32));
			result = prime * result
					+ ((shortName == null) ? 0 : shortName.hashCode());
			result = prime * result
					+ ((startColor == null) ? 0 : startColor.hashCode());
			result = prime * result + ((type == null) ? 0 : type.hashCode());
			result = prime * result + valueCount;
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
			final ParameterModel other = (ParameterModel) obj;
			if (aggregateType == null) {
				if (other.aggregateType != null) {
					return false;
				}
			} else if (!aggregateType.equals(other.aggregateType)) {
				return false;
			}
			if (colorLegend == null) {
				if (other.colorLegend != null) {
					return false;
				}
			} else if (!colorLegend.equals(other.colorLegend)) {
				return false;
			}
			if (columnValues == null) {
				if (other.columnValues != null) {
					return false;
				}
			} else if (!columnValues.equals(other.columnValues)) {
				return false;
			}
			if (columns == null) {
				if (other.columns != null) {
					return false;
				}
			} else if (!columns.equals(other.columns)) {
				return false;
			}
			if (endColor == null) {
				if (other.endColor != null) {
					return false;
				}
			} else if (!endColor.equals(other.endColor)) {
				return false;
			}
			if (middleColor == null) {
				if (other.middleColor != null) {
					return false;
				}
			} else if (!middleColor.equals(other.middleColor)) {
				return false;
			}
			if (Double.doubleToLongBits(rangeMax) != Double
					.doubleToLongBits(other.rangeMax)) {
				return false;
			}
			if (Double.doubleToLongBits(rangeMin) != Double
					.doubleToLongBits(other.rangeMin)) {
				return false;
			}
			if (shortName == null) {
				if (other.shortName != null) {
					return false;
				}
			} else if (!shortName.equals(other.shortName)) {
				return false;
			}
			if (startColor == null) {
				if (other.startColor != null) {
					return false;
				}
			} else if (!startColor.equals(other.startColor)) {
				return false;
			}
			if (type == null) {
				if (other.type != null) {
					return false;
				}
			} else if (!type.equals(other.type)) {
				return false;
			}
			if (valueCount != other.valueCount) {
				return false;
			}
			return true;
		}

	}

	/**
	 * Constructs a {@link ViewModel} with a different {@link Format}.
	 * 
	 * @param model
	 *            The prototype {@link ViewModel}.
	 * @param format
	 *            The new {@link Format}.
	 */
	public ViewModel(final ViewModel model, final Format format) {
		super();
		this.format = format;
		this.shape = model.shape;
		this.overview = model.overview;
		this.main = model.main;
	}

	/**
	 * Constructs a {@link ViewModel} with a different {@link Shape}.
	 * 
	 * @param model
	 *            The prototype {@link ViewModel}.
	 * @param shape
	 *            The new {@link Shape}.
	 */
	public ViewModel(final ViewModel model, final Shape shape) {
		super();
		this.format = model.format;
		this.shape = shape;
		this.overview = model.overview;
		this.main = model.main;
	}

	/**
	 * Constructs a {@link ViewModel} with a different {@link OverviewModel}.
	 * 
	 * @param model
	 *            The prototype {@link ViewModel}.
	 * @param overview
	 *            The new {@link OverviewModel}.
	 */
	public ViewModel(final ViewModel model, final OverviewModel overview) {
		super();
		this.format = model.format;
		this.shape = model.shape;
		this.overview = overview;
		this.main = model.main;
	}

	/**
	 * Constructs a {@link ViewModel} with a different {@link ShapeModel}.
	 * 
	 * @param model
	 *            The prototype {@link ViewModel}.
	 * @param shapeModel
	 *            The new {@link ShapeModel}.
	 */
	public ViewModel(final ViewModel model, final ShapeModel shapeModel) {
		super();
		this.format = model.format;
		this.shape = model.shape;
		this.overview = model.overview;
		this.main = shapeModel;
	}

	/**
	 * Constructs a {@link ViewModel} with the basic parameters.
	 * 
	 * @param format
	 *            The format of the plate.
	 * @param shape
	 *            The shape of the wells.
	 * @param overview
	 *            The {@link OverviewModel}.
	 * @param shapeModel
	 *            The {@link ShapeModel}.
	 */
	public ViewModel(final Format format, final Shape shape,
			final OverviewModel overview, final ShapeModel shapeModel) {
		super();
		this.format = format;
		this.shape = shape;
		this.overview = overview;
		this.main = shapeModel;
	}

	private final Format format;
	private final Shape shape;
	private final OverviewModel overview;
	private final ShapeModel main;
	private String labelPattern;

	@Override
	public void actionPerformed(final ActionEvent e) {
		for (final ActionListener listener : actionListeners.keySet()) {
			listener.actionPerformed(new ActionEvent(this, e.getID(), e
					.getActionCommand()));
		}
		for (final ChangeListener listener : changeListeners.keySet()) {
			listener.stateChanged(new ChangeEvent(this));
		}
	}

	/**
	 * Adds a {@link ChangeListener} to {@link ViewModel}.
	 * 
	 * @param listener
	 *            A {@link ChangeListener}.
	 * @return It is {@code true} if it was newly added.
	 */
	public boolean addChangeListener(final ChangeListener listener) {
		return changeListeners.put(listener, Boolean.TRUE) == null;
	}

	/**
	 * Removes the {@link ChangeListener} from {@link ViewModel}.
	 * 
	 * @param listener
	 *            A {@link ChangeListener} to remove.
	 * @return It is {@code true} if it was previously contained.
	 */
	public boolean removeChangeListener(final ChangeListener listener) {
		return changeListeners.remove(listener) != null;
	}

	/**
	 * Adds an {@link ActionListener} to {@link ViewModel}.
	 * 
	 * @param listener
	 *            An {@link ActionListener}.
	 * @return It is {@code true} if it was newly added.
	 */
	public boolean addActionListener(final ActionListener listener) {
		return actionListeners.put(listener, Boolean.TRUE) == null;
	}

	/**
	 * Removes the {@link ActionListener} from {@link ViewModel}.
	 * 
	 * @param listener
	 *            A {@link ActionListener} to remove.
	 * @return It is {@code true} if it was previously contained.
	 */
	public boolean removeActionListener(final ActionListener listener) {
		return actionListeners.remove(listener) != null;
	}

	/**
	 * @return The current {@link Format}.
	 */
	public Format getFormat() {
		return format;
	}

	/**
	 * @return The current {@link Shape}.
	 */
	public Shape getShape() {
		return shape;
	}

	/**
	 * @return The current {@link OverviewModel}.
	 */
	public OverviewModel getOverview() {
		return overview;
	}

	/**
	 * @return The current {@link ShapeModel}.
	 */
	public ShapeModel getMain() {
		return main;
	}

	/**
	 * Updates the pattern for the labels.
	 * 
	 * @param labelPattern
	 *            The new pattern.
	 */
	public void setLabelPattern(final String labelPattern) {
		this.labelPattern = labelPattern;
		actionPerformed(new ActionEvent(this,
				(int) (System.currentTimeMillis() & 0xffffffff), "newLabels"));
	}

	/**
	 * @return The pattern for the information panel and for the tooltips.
	 */
	public String getLabelPattern() {
		return labelPattern;
	}
}

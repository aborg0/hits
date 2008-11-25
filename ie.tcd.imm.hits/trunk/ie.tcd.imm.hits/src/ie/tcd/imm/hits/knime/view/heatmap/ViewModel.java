/**
 * 
 */
package ie.tcd.imm.hits.knime.view.heatmap;

import ie.tcd.imm.hits.knime.view.heatmap.ControlPanel.ArrangementModel;
import ie.tcd.imm.hits.knime.view.heatmap.HeatmapNodeModel.StatTypes;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * This is responsible for the visual representation of the {@link Heatmap}s.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
public class ViewModel implements ActionListener {
	private final WeakHashMap<ChangeListener, Boolean> changeListeners = new WeakHashMap<ChangeListener, Boolean>();
	private final WeakHashMap<ActionListener, Boolean> actionListeners = new WeakHashMap<ActionListener, Boolean>();

	public static enum Format {
		_96(8, 12), _384(16, 24);

		private final int row;
		private final int col;

		private Format(final int row, final int col) {
			this.row = row;
			this.col = col;
		}

		public int getRow() {
			return row;
		}

		public int getCol() {
			return col;
		}
	}

	public static enum Shape {
		Circle(4, true, true), Rectangle(0, true, true);
		private final int additionalInformationSlotsCount;
		private final boolean supportThickness;
		private final boolean supportSize;

		private Shape(final int additionalInformationSlots,
				final boolean supportThickness, final boolean supportSize) {
			this.additionalInformationSlotsCount = additionalInformationSlots;
			this.supportThickness = supportThickness;
			this.supportSize = supportSize;
		}

		public int getAdditionalInformationSlotsCount() {
			return additionalInformationSlotsCount;
		}

		public boolean isSupportThickness() {
			return supportThickness;
		}

		public boolean isSupportSize() {
			return supportSize;
		}
	}

	public static enum ValueType {
		Discrete, Continuous;
	}

	public static class WellViewModel {
		public static enum Places implements CompatibleValues {
			Primer(EnumSet.of(ValueType.Discrete, ValueType.Continuous)), Secunder(
					EnumSet.of(ValueType.Discrete, ValueType.Continuous)), Additional(
					EnumSet.of(ValueType.Discrete, ValueType.Continuous));
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

	public static interface CompatibleValues {
		public EnumSet<ValueType> getCompatibleValues();
	}

	public static class OverviewModel {
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

		public OverviewModel(final List<ParameterModel> rowModel,
				final List<ParameterModel> colModel,
				final List<ParameterModel> choiceModel) {
			super();
			this.rowModel.addAll(rowModel);
			this.colModel.addAll(colModel);
			this.choiceModel.addAll(choiceModel);
		}

		public List<ParameterModel> getChoiceModel() {
			return Collections.unmodifiableList(choiceModel);
		}

		public List<ParameterModel> getColModel() {
			return Collections.unmodifiableList(colModel);
		}

		public List<ParameterModel> getRowModel() {
			return Collections.unmodifiableList(rowModel);
		}
	}

	public static class ShapeModel {
		private final ArrangementModel arrangementModel;
		private final List<ParameterModel> primerParameters = new ArrayList<ParameterModel>();
		private final List<ParameterModel> secunderParameters = new ArrayList<ParameterModel>();
		private final List<ParameterModel> additionalParameters = new ArrayList<ParameterModel>();

		private final boolean drawBorder;
		private final boolean drawPrimaryBorders;
		private final boolean drawSecundaryBorders;
		private final boolean drawAdditionalBorders;

		private final int startAngle = 30;

		public ShapeModel(final ArrangementModel arrangementModel,
				final List<ParameterModel> primerParameters,
				final List<ParameterModel> secunderParameters,
				final List<ParameterModel> additionalParameters,
				final boolean drawBorder, final boolean drawPrimaryBorders,
				final boolean drawSecundaryBorders,
				final boolean drawAdditionalBorders) {
			super();
			this.arrangementModel = arrangementModel;
			this.primerParameters.addAll(primerParameters);
			this.secunderParameters.addAll(secunderParameters);
			this.additionalParameters.addAll(additionalParameters);
			this.drawBorder = drawBorder;
			this.drawPrimaryBorders = drawPrimaryBorders;
			this.drawSecundaryBorders = drawSecundaryBorders;
			this.drawAdditionalBorders = drawAdditionalBorders;
		}

		public ShapeModel(final ArrangementModel arrangementModel,
				final List<ParameterModel> primerParameters,
				final List<ParameterModel> secunderParameters,
				final List<ParameterModel> additionalParameters,
				final boolean drawBorders) {
			this(arrangementModel, primerParameters, secunderParameters,
					additionalParameters, drawBorders, drawBorders,
					drawBorders, drawBorders);
		}

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
				this.drawSecundaryBorders = model.drawSecundaryBorders;
				this.drawAdditionalBorders = model.drawAdditionalBorders;
			} else {
				switch (place) {
				case Primer:
					this.drawBorder = model.drawBorder;
					this.drawPrimaryBorders = drawBorder;
					this.drawSecundaryBorders = model.drawSecundaryBorders;
					this.drawAdditionalBorders = model.drawAdditionalBorders;
					break;
				case Secunder:
					this.drawBorder = model.drawBorder;
					this.drawPrimaryBorders = model.drawPrimaryBorders;
					this.drawSecundaryBorders = model.drawBorder;
					this.drawAdditionalBorders = model.drawAdditionalBorders;
					break;
				case Additional:
					this.drawBorder = model.drawBorder;
					this.drawPrimaryBorders = model.drawPrimaryBorders;
					this.drawSecundaryBorders = model.drawSecundaryBorders;
					this.drawAdditionalBorders = model.drawBorder;
					break;

				default:
					throw new IllegalStateException("Wrong type: " + place);
				}
			}
		}

		public List<ParameterModel> getPrimerParameters() {
			return Collections.unmodifiableList(primerParameters);
		}

		public List<ParameterModel> getSecunderParameters() {
			return Collections.unmodifiableList(secunderParameters);
		}

		public List<ParameterModel> getAdditionalParameters() {
			return Collections.unmodifiableList(additionalParameters);
		}

		public boolean isDrawBorder() {
			return drawBorder;
		}

		public boolean isDrawPrimaryBorders() {
			return drawPrimaryBorders;
		}

		public boolean isDrawSecundaryBorders() {
			return drawSecundaryBorders;
		}

		public boolean isDrawAdditionalBorders() {
			return drawAdditionalBorders;
		}

		public int getStartAngle() {
			return startAngle;
		}

		public ArrangementModel getArrangementModel() {
			return arrangementModel;
		}

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

	public static enum AggregateType {
		Median, Mean, StandardDeviation;
	}

	public static class ParameterModel {
		private final String shortName;// Maybe from row names
		private final AggregateType aggregateType;
		private int valueCount;// The count of different values
		private final List<String> columns;
		private final List<String> columnValues;
		private double rangeMin, rangeMax;
		private Color startColor, middleColor, endColor;
		private final Map<Object, Color> colorLegend = new HashMap<Object, Color>();
		private final StatTypes type;

		public ParameterModel(final String shortName, final StatTypes type,
				final AggregateType aggregateType, final List<String> columns,
				final List<String> columnValues) {
			super();
			this.shortName = shortName;
			this.type = type;
			this.aggregateType = aggregateType;
			this.columns = columns;
			this.columnValues = columnValues;
		}

		public int getValueCount() {
			return valueCount;
		}

		public void setValueCount(final int valueCount) {
			assert type.isDiscrete() : type;
			this.valueCount = valueCount;
		}

		public double getRangeMin() {
			return rangeMin;
		}

		public void setRangeMin(final double rangeMin) {
			assert !type.isDiscrete() : type;
			this.rangeMin = rangeMin;
		}

		public double getRangeMax() {
			return rangeMax;
		}

		public void setRangeMax(final double rangeMax) {
			assert !type.isDiscrete();
			this.rangeMax = rangeMax;
		}

		public Color getStartColor() {
			return startColor;
		}

		public void setStartColor(final Color startColor) {
			assert !type.isDiscrete() : type;
			this.startColor = startColor;
		}

		public Color getMiddleColor() {
			return middleColor;
		}

		public void setMiddleColor(final Color middleColor) {
			assert !type.isDiscrete() : type;
			this.middleColor = middleColor;
		}

		public Color getEndColor() {
			return endColor;
		}

		public void setEndColor(final Color endColor) {
			assert !type.isDiscrete() : type;
			this.endColor = endColor;
		}

		public String getShortName() {
			return shortName;
		}

		public AggregateType getAggregateType() {
			return aggregateType;
		}

		public List<String> getColumns() {
			return columns;
		}

		public List<String> getColumnValues() {
			return columnValues;
		}

		public Map<Object, Color> getColorLegend() {
			assert type.isDiscrete() : type;
			return colorLegend;
		}

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

	public ViewModel(final ViewModel model, final Format format) {
		super();
		this.format = format;
		this.shape = model.shape;
		this.overview = model.overview;
		this.main = model.main;
	}

	public ViewModel(final ViewModel model, final Shape shape) {
		super();
		this.format = model.format;
		this.shape = shape;
		this.overview = model.overview;
		this.main = model.main;
	}

	public ViewModel(final ViewModel model, final OverviewModel overview) {
		super();
		this.format = model.format;
		this.shape = model.shape;
		this.overview = overview;
		this.main = model.main;
	}

	public ViewModel(final ViewModel model, final ShapeModel shapeModel) {
		super();
		this.format = model.format;
		this.shape = model.shape;
		this.overview = model.overview;
		this.main = shapeModel;
	}

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

	public boolean addChangeListener(final ChangeListener listener) {
		return changeListeners.put(listener, Boolean.TRUE) == null;
	}

	public boolean removeChangeListener(final ChangeListener listener) {
		return changeListeners.remove(listener) != null;
	}

	public boolean addActionListener(final ActionListener listener) {
		return actionListeners.put(listener, Boolean.TRUE) == null;
	}

	public boolean removeActionListener(final ActionListener listener) {
		return actionListeners.remove(listener) != null;
	}

	public Format getFormat() {
		return format;
	}

	public Shape getShape() {
		return shape;
	}

	public OverviewModel getOverview() {
		return overview;
	}

	public ShapeModel getMain() {
		return main;
	}
}

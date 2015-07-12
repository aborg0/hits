/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package ie.tcd.imm.hits.knime.view.heatmap;

import ie.tcd.imm.hits.knime.view.heatmap.HeatmapNodeModel.StatTypes;
import ie.tcd.imm.hits.knime.view.heatmap.ViewModel.ParameterModel;
import ie.tcd.imm.hits.util.select.Selector;
import ie.tcd.imm.hits.util.swing.VariableControl.ControlTypes;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import org.eclipse.swt.widgets.Slider;
import org.knime.core.util.Pair;

/**
 * This is something that represents a {@link ParameterModel} list and values.
 */
public class SliderModel extends Selector<Pair<ParameterModel, Object>>
		implements Serializable {
	private static final long serialVersionUID = 8868671426882187720L;

	/**
	 * The position of the {@link SliderModel} in the window.
	 */
	public static enum Type {
		/**
		 * The {@link Slider} is not visible, only settable from the control
		 * screen
		 */
		Hidden,
		/** The {@link Slider} splits the wells */
		Splitter,
		/** The {@link Slider} is distributed across the vertical scrollbar */
		ScrollVertical,
		/** The {@link Slider} is distributed across the horizontal scrollbar */
		ScrollHorisontal,
		/** The {@link Slider} values are on the selector panel. */
		Selector;
	}

	/**
	 * The maximum number of independent factors. This is an upper bound for the
	 * different sliders for the same parameters.
	 */
	public static final int MAX_INDEPENDENT_FACTORS = 3;

	private final int subId;

	private final List<ParameterModel> parameters = new ArrayList<ParameterModel>();
	private final ControlTypes preferredControlType;

	/**
	 * Constructs {@link SliderModel}s, with cache.
	 */
	public static class SliderFactory implements Serializable {
		private static final long serialVersionUID = -5156367879519731281L;
		private final Set<SliderModel> sliders = new HashSet<SliderModel>();

		/**
		 * Finds or creates a {@link SliderModel} with the given parameters.
		 * 
		 * @param type
		 *            The {@link Type} (position) of the slider.
		 * @param parameters
		 *            The parameters belonging to the slider.
		 * @param valueMapping
		 *            The values mapped to the integer constants. The integer
		 *            constants usually start from {@code 1}.
		 * @return A {@link Set} of possible {@link SliderModel}s previously
		 *         created, or created a new one with these parameters..
		 */
		public Set<SliderModel> get(final SliderModel.Type type,
				final List<ParameterModel> parameters,
				final Map<Integer, Pair<ParameterModel, Object>> valueMapping) {
			final Set<SliderModel> ret = new HashSet<SliderModel>();
			for (final SliderModel slider : sliders) {
				if (/* slider.type == type && */parameters
						.equals(slider.parameters)
						&& valueMapping.equals(slider.getValueMapping())) {
					ret.add(slider);
				}
			}
			if (ret.isEmpty()) {
				final ControlTypes controlTypes;
				switch (type) {
				case Hidden:
					controlTypes = ControlTypes.ComboBox;
					break;
				case Selector:
					controlTypes = ControlTypes.Slider;
					break;
				case Splitter:
					controlTypes = ControlTypes.Buttons;
					break;
				case ScrollHorisontal:
					controlTypes = ControlTypes.ScrollBarHorisontal;
					break;
				case ScrollVertical:
					controlTypes = ControlTypes.ScrollBarVertical;
					break;
				default:
					throw new IllegalStateException("Unknown slider type: "
							+ type);
				}
				final SliderModel slider = new SliderModel(0, parameters,
						valueMapping, controlTypes);
				sliders.add(slider);
				ret.add(slider);
			}
			return ret;
		}
	}

	private SliderModel(final int subId, final List<ParameterModel> parameters,
			final Map<Integer, Pair<ParameterModel, Object>> valueMapping,
			final ControlTypes controlType) {
		this(subId, parameters, valueMapping, valueMapping.keySet(),
				controlType);
	}

	private SliderModel(final int subId, final List<ParameterModel> parameters,
			final Map<Integer, Pair<ParameterModel, Object>> valueMapping,
			final Set<Integer> selection,
			final ControlTypes preferredControlType) {
		super(valueMapping, selection);
		this.subId = subId;
		this.preferredControlType = preferredControlType;
		this.parameters.addAll(parameters);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ (parameters == null ? 0 : parameters.hashCode());
		result = prime * result + subId;
		// result = prime * result + ((type == null) ? 0 : type.hashCode());
		result = prime * result + super.hashCode();
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
		final SliderModel other = (SliderModel) obj;
		if (parameters == null) {
			if (other.parameters != null) {
				return false;
			}
		} else if (!parameters.equals(other.parameters)) {
			return false;
		}
		if (subId != other.subId) {
			return false;
		}
		// if (type == null) {
		// if (other.type != null) {
		// return false;
		// }
		// } else if (type != other.type) {
		// return false;
		// }
		return super.equals(obj);
	}

	/**
	 * Each slider should belong to a parameter list, which is not necessarily
	 * unique. This value tells which is it.
	 * 
	 * @return The id of the parameter group.
	 */
	public int getSubId() {
		return subId;
	}

	/**
	 * @return The parameters belonging to this {@link SliderModel}.
	 */
	public List<ParameterModel> getParameters() {
		return Collections.unmodifiableList(parameters);
	}

	@Override
	public String toString() {
		return preferredControlType + "_" + subId + " " + parameters;
	}

	/**
	 * @return the preferredControlType
	 */
	public ControlTypes getPreferredControlType() {
		return preferredControlType;
	}

	/**
	 * Finds the proper (the first one) {@link SliderModel}, whose first
	 * {@link ParameterModel}'s type is {@code statType}.
	 * 
	 * @param sliders
	 *            Some {@link SliderModel}s.
	 * @param statType
	 *            A {@link StatTypes}.
	 * @return The first {@link SliderModel} with (first) {@link ParameterModel}
	 *         with type {@code statType} in {@code sliders}, or {@code null} if
	 *         not found.
	 */
	public static @Nullable
	Selector<Pair<ParameterModel, Object>> findSlider(
			final Iterable<SliderModel> sliders, final StatTypes statType) {
		for (final SliderModel sliderModel : sliders) {
			final List<ParameterModel> params = sliderModel.getParameters();
			if (params.size() > 0
					&& params.iterator().next().getType() == statType) {
				return sliderModel;
			}
		}
		return null;
	}
}
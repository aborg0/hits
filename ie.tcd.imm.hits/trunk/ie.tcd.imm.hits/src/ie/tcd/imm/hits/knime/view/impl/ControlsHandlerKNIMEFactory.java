/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package ie.tcd.imm.hits.knime.view.impl;

import ie.tcd.imm.hits.knime.view.ControlsHandler;
import ie.tcd.imm.hits.knime.view.SplitType;
import ie.tcd.imm.hits.knime.view.heatmap.SliderModel;
import ie.tcd.imm.hits.knime.view.heatmap.ViewModel.ParameterModel;
import ie.tcd.imm.hits.knime.view.heatmap.ViewModel.ShapeModel;
import ie.tcd.imm.hits.util.Pair;
import ie.tcd.imm.hits.util.Selectable;
import ie.tcd.imm.hits.util.swing.SelectionType;
import ie.tcd.imm.hits.util.swing.VariableControl;
import ie.tcd.imm.hits.util.swing.VariableControl.ControlTypes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.WeakHashMap;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import javax.swing.event.ChangeEvent;

import org.knime.core.node.defaultnodesettings.SettingsModel;

import edu.umd.cs.findbugs.annotations.DefaultAnnotation;

/**
 * This factory helps to handle the controls for {@link SliderModel}s.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
@DefaultAnnotation( { Nonnull.class, CheckReturnValue.class })
@NotThreadSafe
public class ControlsHandlerKNIMEFactory
		extends
		ControlsHandlerAbstractFactory<Pair<ParameterModel, Object>, SliderModel> {

	/**
	 * A {@link ChangeEvent} for the model changes of the {@link SliderModel}s.
	 */
	public static class ArrangementEvent extends ChangeEvent {
		private static final long serialVersionUID = -7662127679795688559L;
		private final ShapeModel newArrangement;

		/**
		 * @param source
		 *            Mostly a {@link ControlsHandler} instance.
		 * @param newArrangement
		 *            The new arrangement of {@link SliderModel}s.
		 */
		public ArrangementEvent(final Object source,
				final ShapeModel newArrangement) {
			super(source);
			this.newArrangement = newArrangement;
		}

		/**
		 * @return The new arrangement of {@link SliderModel}s.
		 */
		public ShapeModel getNewArrangement() {
			return newArrangement;
		}
	}

	private ShapeModel arrangement;

	private final Map<SplitType, Map<VariableControl<SettingsModel, Pair<ParameterModel, Object>, SliderModel>, Boolean>> splitToControls = new EnumMap<SplitType, Map<VariableControl<SettingsModel, Pair<ParameterModel, Object>, SliderModel>, Boolean>>(
			SplitType.class);
	{
		for (final SplitType type : SplitType.values()) {
			splitToControls
					.put(
							type,
							new WeakHashMap<VariableControl<SettingsModel, Pair<ParameterModel, Object>, SliderModel>, Boolean>());
		}
	}

	/**
	 * Constructs a {@link ControlsHandlerKNIMEFactory}.
	 */
	public ControlsHandlerKNIMEFactory() {
		super();
	}

	/**
	 * Generates a name for the {@link ControlsHandler}.
	 * 
	 * @param slider
	 *            A {@link SliderModel}.
	 * @return The short name of the first {@link ParameterModel} of {@code
	 *         slider}.
	 */
	public static String createName(final SliderModel slider) {
		final String name = slider.getParameters().iterator().next()
				.getShortName();
		return name;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean exchangeControls(
			final VariableControl<SettingsModel, Pair<ParameterModel, Object>, SliderModel> first,
			final VariableControl<SettingsModel, Pair<ParameterModel, Object>, SliderModel> second) {
		assert arrangement != null;
		final Pair<SplitType, String> firstPos = getPosition(first);
		final Pair<SplitType, String> secondPos = getPosition(second);
		if (firstPos == null || secondPos == null) {
			return false;
		}
		if (firstPos.getLeft() == secondPos.getLeft()) {
			return false;
		}
		if (firstPos.getLeft() == SplitType.AdditionalInfo
				|| secondPos.getLeft() == SplitType.AdditionalInfo) {
			return false;
		}
		if (firstPos.getLeft() == SplitType.SingleSelect) {
			return exchangeControls(second, first);
		}
		final Set<SliderModel> sliderModels = arrangement.getArrangementModel()
				.getSliderModels();
		final SliderModel firstSlider = findSlider(first, sliderModels);
		final SliderModel secondSlider = findSlider(second, sliderModels);
		final ArrayList<ParameterModel> primaryParams = new ArrayList<ParameterModel>();
		final ArrayList<ParameterModel> secondaryParams = new ArrayList<ParameterModel>();
		switch (firstPos.getLeft()) {
		case PrimarySplit:
			primaryParams.add(secondSlider.getParameters().iterator().next());
			switch (secondPos.getLeft()) {
			case SeconderSplit:
				secondaryParams.add(firstSlider.getParameters().iterator()
						.next());
				break;
			case SingleSelect:
				secondaryParams.addAll(arrangement.getSeconderParameters());
				firstSlider.selectSingle(firstSlider.getSelections().iterator()
						.next());
				break;
			default:
				break;
			}
			break;
		case SeconderSplit:
			secondaryParams.add(secondSlider.getParameters().iterator().next());
			switch (secondPos.getLeft()) {
			case PrimarySplit:
				primaryParams
						.add(firstSlider.getParameters().iterator().next());
				break;
			case SingleSelect:
				primaryParams.addAll(arrangement.getPrimerParameters());
				firstSlider.selectSingle(firstSlider.getSelections().iterator()
						.next());
				break;
			default:
				break;
			}
			break;
		case SingleSelect:
			throw new IllegalStateException(
					"SingleSelect should not be in this position");
		case AdditionalInfo:
		case HierachicalTabs:
		case ParalelSplitHorisontal:
		case ParalelSplitVertical:
		default:
			throw new UnsupportedOperationException("Not supported type: "
					+ firstPos.getLeft());
		}
		final List<ParameterModel> newAdditionalParams = new ArrayList<ParameterModel>();
		final ShapeModel shapeModel = new ShapeModel(arrangement
				.getArrangementModel(), primaryParams, secondaryParams,
				newAdditionalParams, arrangement.isDrawBorder(), arrangement
						.isDrawPrimaryBorders(), arrangement
						.isDrawSecondaryBorders(), arrangement
						.isDrawAdditionalBorders());
		shapeModel.setColourModel(arrangement.getColourModel());
		arrangement = shapeModel;
		notifyChangeListeners(new ArrangementEvent(this, shapeModel));
		deregister(firstSlider, firstPos.getLeft());
		deregister(secondSlider, secondPos.getLeft());
		register(firstSlider, secondPos.getLeft(), secondPos.getRight(), second
				.getType());
		register(secondSlider, firstPos.getLeft(), firstPos.getRight(), first
				.getType());
		return true;
	}

	/**
	 * Finds a {@link SliderModel} belonging to {@code control} in {@code
	 * sliderModels}.
	 * 
	 * @param control
	 *            A handled {@link VariableControl}.
	 * @param sliderModels
	 *            The possible {@link SliderModel}s.
	 * @return The found {@link SliderModel} or {@code null}.
	 */
	private @Nullable
	SliderModel findSlider(
			final VariableControl<SettingsModel, Pair<ParameterModel, Object>, SliderModel> control,
			final Set<SliderModel> sliderModels) {
		for (final SliderModel sliderModel : sliderModels) {
			if (((SettingsModelListSelection) control.getModel())
					.getConfigName().equals(
							sliderModel.getParameters().iterator().next()
									.getShortName())) {
				return sliderModel;
			}
		}
		return null;
	}

	/**
	 * Sets the new {@link ShapeModel}.
	 * 
	 * @param arrangement
	 *            The new {@link ShapeModel}.
	 */
	public void setArrangement(final ShapeModel arrangement) {
		this.arrangement = arrangement;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * This will create the {@link VariableControl} with the default
	 * {@link ControlTypes} for each {@link SplitType}:
	 * <ul>
	 * <li>{@link SliderModel.Type#Hidden} &rarr; {@link ControlTypes#ComboBox}</li>
	 * <li>{@link SliderModel.Type#ScrollHorisontal} &rarr;
	 * {@link ControlTypes#ScrollBarHorisontal}</li>
	 * <li>{@link SliderModel.Type#ScrollVertical} &rarr;
	 * {@link ControlTypes#ScrollBarVertical}</li>
	 * <li>{@link SliderModel.Type#Selector} &rarr; {@link ControlTypes#Slider}</li>
	 * <li>{@link SliderModel.Type#Splitter} &rarr; {@link ControlTypes#Buttons}
	 * </li>
	 * </ul>
	 */
	@Override
	protected VariableControl<SettingsModel, Pair<ParameterModel, Object>, SliderModel> createNewControl(
			final Selectable<Pair<ParameterModel, Object>> slider,
			final ControlTypes controlType, final SelectionType selectionType,
			final SplitType split) {
		final String name = createName((SliderModel) slider);
		final Map<Integer, Pair<ParameterModel, Object>> valueMapping = slider
				.getValueMapping();
		final List<String> vals = new LinkedList<String>();
		for (final Pair<ParameterModel, Object> pair : valueMapping.values()) {
			vals.add(pair.getRight().toString());
		}
		final Set<Integer> selections = new TreeSet<Integer>(slider
				.getSelections());
		switch (controlType) {
		case Buttons:
		case Invisible:
		case List:
		case ScrollBarHorisontal:
		case ScrollBarVertical:
			// Do nothing, multiple selections are allowed.
			break;
		case ComboBox:
		case RadioButton:
		case Slider:
		case Tab:
			final Integer sel = selections.iterator().next();
			slider.selectSingle(sel);
			break;
		default:
			throw new UnsupportedOperationException(
					"Not supported control type: " + controlType);
		}
		final Collection<String> selected = new HashSet<String>();
		for (final Integer integer : selections) {
			selected.add(vals.get(integer.intValue() - 1));
		}
		final SettingsModelListSelection settingsModelListSelection = new SettingsModelListSelection(
				name, vals, selected);
		final VariableControl<SettingsModel, Pair<ParameterModel, Object>, SliderModel> control = createControl(
				slider, controlType, settingsModelListSelection, selectionType,
				split);
		return control;
	}

	@Override
	protected void adjustModel(final Set<String> selections,
			final List<String> values, final Set<Integer> selectedIndices,
			final Selectable<Pair<ParameterModel, Object>> slider) {
		final Set<Integer> sliderSelection = new HashSet<Integer>(slider
				.getSelections());
		if (selectedIndices.size() == 1) {
			slider.selectSingle(selectedIndices.iterator().next());
		} else {
			for (final Integer index : selectedIndices) {
				if (!sliderSelection.contains(index)) {
					slider.select(index);
				}
			}
			for (final Integer index : sliderSelection) {
				if (!selectedIndices.contains(index)) {
					slider.deselect(index);
				}
			}
		}
	}

}

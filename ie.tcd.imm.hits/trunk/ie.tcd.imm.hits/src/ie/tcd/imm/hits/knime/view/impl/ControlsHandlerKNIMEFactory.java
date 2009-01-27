/**
 * 
 */
package ie.tcd.imm.hits.knime.view.impl;

import ie.tcd.imm.hits.knime.view.ControlsHandler;
import ie.tcd.imm.hits.knime.view.heatmap.SliderModel;
import ie.tcd.imm.hits.knime.view.heatmap.SliderModel.Type;
import ie.tcd.imm.hits.knime.view.heatmap.ViewModel.ParameterModel;
import ie.tcd.imm.hits.util.Pair;
import ie.tcd.imm.hits.util.swing.SelectionType;
import ie.tcd.imm.hits.util.swing.VariableControl;
import ie.tcd.imm.hits.util.swing.VariableControl.ControlTypes;

import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.WeakHashMap;
import java.util.Map.Entry;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import javax.swing.JComponent;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.node.defaultnodesettings.SettingsModel;

import edu.umd.cs.findbugs.annotations.DefaultAnnotation;

/**
 * This factory helps to handle the controls for {@link SliderModel}s.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
@DefaultAnnotation( { Nonnull.class, CheckReturnValue.class })
@NotThreadSafe
public class ControlsHandlerKNIMEFactory implements
		ControlsHandler<SettingsModel> {
	private Map<SliderModel, VariableControl<? extends SettingsModel>> cache = new WeakHashMap<SliderModel, VariableControl<? extends SettingsModel>>();
	private final EnumMap<Type, Map<String, WeakReference<JComponent>>> containers = new EnumMap<Type, Map<String, WeakReference<JComponent>>>(
			Type.class);
	{
		for (final Type type : Type.values()) {
			containers.put(type,
					new HashMap<String, WeakReference<JComponent>>());
		}
	}

	private final Map<JComponent, /* Weak */Map<VariableControl<? extends SettingsModel>, Boolean>> componentToControls = new WeakHashMap<JComponent, Map<VariableControl<? extends SettingsModel>, Boolean>>();
	private final Map<VariableControl<? extends SettingsModel>, JComponent> controlToComponent = new WeakHashMap<VariableControl<? extends SettingsModel>, JComponent>();

	/**
	 * Constructs a {@link ControlsHandlerKNIMEFactory}.
	 */
	public ControlsHandlerKNIMEFactory() {
		super();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ie.tcd.imm.hits.knime.view.ControlsHandler#getComponent(ie.tcd.imm.hits.knime.view.heatmap.SliderModel,
	 *      ie.tcd.imm.hits.util.swing.VariableControl.ControlTypes)
	 */
	@Override
	public VariableControl<? extends SettingsModel> getComponent(
			final SliderModel slider, final ControlTypes controlType) {
		final String name = slider.getParameters()
				+ slider.getType().toString() + slider.getSubId();
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
			selections.clear();
			selections.add(sel);
			for (final Integer select : selections) {
				if (!select.equals(sel)) {
					slider.deselect(select);
				}
			}
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
		settingsModelListSelection.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(final ChangeEvent e) {
				final Set<String> selections = settingsModelListSelection
						.getSelection();
				final List<String> values = settingsModelListSelection
						.getPossibleValues();
				final Set<Integer> selectedIndices = new HashSet<Integer>();
				int i = 1;
				for (final String value : values) {
					if (selections.contains(value)) {
						selectedIndices.add(Integer.valueOf(i));
					}
					++i;
				}
				final Set<Integer> sliderSelection = new HashSet<Integer>(
						slider.getSelections());
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
		});
		final VariableControl<? extends SettingsModel> control = getControl(
				controlType, settingsModelListSelection);
		if (!cache.containsKey(slider)) {
			cache.put(slider, control);
		}
		return cache.get(slider);
	}

	/**
	 * @param controlType
	 * @param settingsModelListSelection
	 * @return
	 */
	private VariableControl<? extends SettingsModel> getControl(
			final ControlTypes controlType,
			final SettingsModelListSelection settingsModelListSelection) {
		final VariableControl<SettingsModel> variableControl;
		switch (controlType) {
		case Buttons:
			variableControl = new ButtonsControl(settingsModelListSelection,
					SelectionType.MultipleAtLeastOne, this);
			break;
		case List:
			variableControl = new ListControl(settingsModelListSelection,
					SelectionType.MultipleAtLeastOne, this);
			break;
		case ComboBox:
			variableControl = new ComboBoxControl(settingsModelListSelection,
					SelectionType.Single, this);
			break;
		case Invisible:
			throw new UnsupportedOperationException("Not supported yet.");

		case Slider:
			variableControl = new SliderControl(settingsModelListSelection,
					SelectionType.Single, this);
			break;
		case RadioButton:
			throw new UnsupportedOperationException("Not supported yet.");
			// if (!cache.containsKey(slider)) {
			// cache.put(slider, new RadioControl(settingsModelListSelection,
			// SelectionType.Single));
			// }
			// return cache.get(slider);
		case Tab:
			throw new UnsupportedOperationException("Not supported yet.");
		case ScrollBarHorisontal:
			throw new UnsupportedOperationException("Not supported yet.");
		case ScrollBarVertical:
			throw new UnsupportedOperationException("Not supported yet.");
		default:
			throw new UnsupportedOperationException("Not supported yet: "
					+ controlType);
		}
		return variableControl;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ie.tcd.imm.hits.knime.view.ControlsHandler#changeControlType(ie.tcd.imm.hits.knime.view.heatmap.SliderModel,
	 *      ie.tcd.imm.hits.util.swing.VariableControl.ControlTypes)
	 */
	@Override
	public boolean changeControlType(
			final VariableControl<SettingsModel> variableControl,
			final ControlTypes type) {
		if (type == variableControl.getType()) {
			return false;
		}
		final JComponent component = controlToComponent.get(variableControl);
		if (component == null) {
			return false;
		}
		final Map<VariableControl<? extends SettingsModel>, Boolean> map = componentToControls
				.get(component);
		final Boolean removed = map.remove(variableControl);
		assert removed != null;
		assert removed.booleanValue() == true;
		component.remove(variableControl.getView());
		SliderModel slider = null;
		for (final Entry<SliderModel, VariableControl<? extends SettingsModel>> entry : cache
				.entrySet()) {
			if (entry.getValue().equals(variableControl)) {
				slider = entry.getKey();
			}
		}
		final VariableControl<? extends SettingsModel> removedVariableControl = cache
				.remove(slider);
		assert removedVariableControl != null;
		final VariableControl<? extends SettingsModel> control = getControl(
				type, (SettingsModelListSelection) variableControl.getModel());
		cache.put(slider, control);
		component.add(control.getView());
		component.revalidate();
		addToMap(map, control);
		controlToComponent.put(control, component);
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ie.tcd.imm.hits.knime.view.ControlsHandler#deregister(ie.tcd.imm.hits.knime.view.heatmap.SliderModel)
	 */
	@Override
	public boolean deregister(final SliderModel model) {
		final VariableControl<? extends SettingsModel> variableControl = cache
				.get(model);
		if (variableControl == null) {
			return false;
		}
		final JComponent component = controlToComponent.get(variableControl);
		if (component == null) {
			return false;
		}
		final Map<VariableControl<? extends SettingsModel>, Boolean> map = componentToControls
				.get(component);
		assert map != null;
		final Boolean removed = map.remove(variableControl);
		assert removed != null;
		assert removed.booleanValue() == true;
		component.remove(variableControl.getView());
		final JComponent removedComponent = controlToComponent
				.remove(variableControl);
		assert removedComponent != null;
		final VariableControl<? extends SettingsModel> removedVariableControl = cache
				.remove(model);
		assert removedVariableControl != null;
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ie.tcd.imm.hits.knime.view.ControlsHandler#move(ie.tcd.imm.hits.util.swing.VariableControl,
	 *      ie.tcd.imm.hits.knime.view.heatmap.SliderModel.Type,
	 *      java.lang.String)
	 */
	@Override
	public boolean move(final VariableControl<SettingsModel> variableControl,
			final Type containerType, final String nameOfContainer) {
		final JComponent newContainer = getContainer(containerType,
				nameOfContainer);
		if (newContainer == null) {
			return false;
		}
		final JComponent oldContainer = controlToComponent.get(variableControl);
		if (oldContainer == null) {
			return false;
		}
		final Map<VariableControl<? extends SettingsModel>, Boolean> oldControls = componentToControls
				.get(oldContainer);
		// Transaction start
		oldControls.remove(variableControl);
		oldContainer.remove(variableControl.getView());
		oldContainer.revalidate();
		newContainer.add(variableControl.getView());
		newContainer.revalidate();
		final Map<VariableControl<? extends SettingsModel>, Boolean> map = componentToControls
				.get(newContainer);
		assert map != null;
		addToMap(map, variableControl);
		// Transaction end
		controlToComponent.put(variableControl, newContainer);
		return true;
	}

	/**
	 * @param map
	 * @param variableControl
	 */
	private void addToMap(
			final Map<VariableControl<? extends SettingsModel>, Boolean> map,
			final VariableControl<? extends SettingsModel> variableControl) {
		map.put(variableControl, Boolean.TRUE);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ie.tcd.imm.hits.knime.view.ControlsHandler#register(ie.tcd.imm.hits.knime.view.heatmap.SliderModel,
	 *      ie.tcd.imm.hits.knime.view.heatmap.SliderModel.Type,
	 *      java.lang.String)
	 */
	@Override
	public boolean register(final SliderModel model, final Type modelType,
			final String nameOfContainer) {
		final JComponent component = getContainer(modelType, nameOfContainer);
		if (component == null) {
			return false;
		}
		final Map<VariableControl<? extends SettingsModel>, Boolean> map = componentToControls
				.get(component);
		assert map != null;
		final ControlTypes controlType;
		switch (modelType) {
		case Hidden:
			controlType = ControlTypes.ComboBox;
			break;
		case Selector:
			controlType = ControlTypes.Slider;
			break;
		case Splitter:
			controlType = ControlTypes.Buttons;
			break;
		case ScrollHorisontal:
		case ScrollVertical:
			throw new UnsupportedOperationException(
					"Scrolls are not supported yet...");
		default:
			throw new UnsupportedOperationException("Not supported position: "
					+ modelType);
		}
		final VariableControl<? extends SettingsModel> variableControl = getComponent(
				model, controlType);
		if (variableControl == null) {
			return false;
		}
		component.add(variableControl.getView());
		addToMap(map, variableControl);
		controlToComponent.put(variableControl, component);
		return true;
	}

	/**
	 * Selects a {@link JComponent} if exists with the proper properties.
	 * 
	 * @param containerType
	 *            A {@link Type}.
	 * @param nameOfContainer
	 *            A name of the container. May be {@code null}.
	 * @return The {@link JComponent} associated to {@code containerType} and
	 *         {@code nameOfContainer}, or {@code null} if it is not
	 *         {@link #setContainer(JComponent, Type, String) set} before.
	 */
	private @Nullable
	JComponent getContainer(final Type containerType, @Nullable
	final String nameOfContainer) {
		final WeakReference<JComponent> component = containers.get(
				containerType).get(nameOfContainer);
		return component == null ? null : component.get();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ie.tcd.imm.hits.knime.view.ControlsHandler#setContainer(javax.swing.JComponent,
	 *      ie.tcd.imm.hits.knime.view.heatmap.SliderModel.Type,
	 *      java.lang.String)
	 */
	@Override
	public void setContainer(final JComponent container, final Type type,
			final String name) {
		final Map<String, WeakReference<JComponent>> map = containers.get(type);
		if (map.isEmpty()) {
			map.put(null, new WeakReference<JComponent>(container));
		}
		map.put(name, new WeakReference<JComponent>(container));
		componentToControls
				.put(
						container,
						new WeakHashMap<VariableControl<? extends SettingsModel>, Boolean>());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ie.tcd.imm.hits.knime.view.ControlsHandler#findContainers()
	 */
	@Override
	public Set<Pair<Type, String>> findContainers() {
		final Set<Pair<Type, String>> ret = new HashSet<Pair<Type, String>>();
		for (final Entry<Type, Map<String, WeakReference<JComponent>>> entry : containers
				.entrySet()) {
			for (final String name : entry.getValue().keySet()) {
				ret.add(new Pair<Type, String>(entry.getKey(), name));
			}
		}
		return ret;
	}
}

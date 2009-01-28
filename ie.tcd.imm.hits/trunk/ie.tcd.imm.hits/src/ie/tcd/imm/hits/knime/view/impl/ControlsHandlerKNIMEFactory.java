/**
 * 
 */
package ie.tcd.imm.hits.knime.view.impl;

import ie.tcd.imm.hits.knime.view.ControlsHandler;
import ie.tcd.imm.hits.knime.view.ControlsHandler.ConsistencyChecker.DefaultConsistencyChecker;
import ie.tcd.imm.hits.knime.view.heatmap.SliderModel;
import ie.tcd.imm.hits.knime.view.heatmap.ControlPanel.ArrangementModel;
import ie.tcd.imm.hits.knime.view.heatmap.SliderModel.Type;
import ie.tcd.imm.hits.knime.view.heatmap.ViewModel.ParameterModel;
import ie.tcd.imm.hits.knime.view.heatmap.ViewModel.ShapeModel;
import ie.tcd.imm.hits.util.Pair;
import ie.tcd.imm.hits.util.swing.SelectionType;
import ie.tcd.imm.hits.util.swing.VariableControl;
import ie.tcd.imm.hits.util.swing.VariableControl.ControlTypes;

import java.awt.Component;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
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
	/**
	 * A {@link ChangeEvent} for the model changes of the {@link SliderModel}s.
	 */
	public class ArrangementEvent extends ChangeEvent {
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
	private ConsistencyChecker checker = new DefaultConsistencyChecker();
	private final List<WeakReference<ChangeListener>> listeners = new ArrayList<WeakReference<ChangeListener>>();
	private ShapeModel arrangement;

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
		final String name = createName(slider);
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
			// selections.clear();
			// selections.add(sel);
			slider.selectSingle(sel);
			// for (final Integer select : selections) {
			// if (!select.equals(sel)) {
			// slider.deselect(select);
			// }
			// }
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
		final VariableControl<? extends SettingsModel> control = createControl(
				slider, controlType, settingsModelListSelection);
		if (!cache.containsKey(slider)) {
			cache.put(slider, control);
		}
		return cache.get(slider);
	}

	/**
	 * Generates a name for the {@link ControlsHandler}.
	 * 
	 * @param slider
	 *            A {@link SliderModel}.
	 * @return The short name of the first {@link ParameterModel} of
	 *         {@code slider}.
	 */
	public static String createName(final SliderModel slider) {
		final String name = slider.getParameters().iterator().next()
				.getShortName();
		return name;
	}

	/**
	 * @param slider
	 *            The {@link SliderModel} containing the possible values, ...
	 * @param controlType
	 *            A {@link ControlTypes}.
	 * @param settingsModelListSelection
	 *            A {@link SettingsModelListSelection}.
	 * @return The control with the proper parameters.
	 */
	private VariableControl<? extends SettingsModel> createControl(
			final SliderModel slider, final ControlTypes controlType,
			final SettingsModelListSelection settingsModelListSelection) {
		final ChangeListener changeListener = createChangeListener(slider,
				settingsModelListSelection);
		final SelectionType selection;
		switch (controlType) {
		case Buttons:
			selection = SelectionType.MultipleAtLeastOne;
			break;
		case List:
			selection = SelectionType.MultipleAtLeastOne;
			break;
		case ComboBox:
			selection = SelectionType.Single;
			break;
		case Invisible:
			selection = SelectionType.Unmodifiable;
			break;
		case RadioButton:
			selection = SelectionType.Single;
			break;
		case ScrollBarHorisontal:
			selection = SelectionType.Unmodifiable;
			break;
		case ScrollBarVertical:
			selection = SelectionType.Unmodifiable;
			break;
		case Slider:
			selection = SelectionType.Single;
			break;
		case Tab:
			selection = SelectionType.Single;
			break;
		default:
			throw new UnsupportedOperationException("Not supported yet: "
					+ controlType);
		}
		return createControl(controlType, settingsModelListSelection,
				changeListener, selection);
	}

	/**
	 * @param slider
	 *            A {@link SliderModel}.
	 * @param settingsModelListSelection
	 *            A {@link SettingsModelListSelection}.
	 * @return The {@link ChangeListener} adjusting the {@code slider}
	 *         selections to {@code settingsModelListSelection} selections to be
	 *         in synchrony.
	 */
	private ChangeListener createChangeListener(final SliderModel slider,
			final SettingsModelListSelection settingsModelListSelection) {
		final ChangeListener changeListener = new ChangeListener() {
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
		};
		return changeListener;
	}

	/**
	 * @param controlType
	 *            The preferred {@link ControlTypes}.
	 * @param settingsModelListSelection
	 *            The used model.
	 * @param changeListener
	 *            The associated {@link ChangeListener}.
	 * @param selection
	 *            The {@link SelectionType selection} mode.
	 * @return The {@link VariableControl} with the desired parameters.
	 */
	private VariableControl<? extends SettingsModel> createControl(
			final ControlTypes controlType,
			final SettingsModelListSelection settingsModelListSelection,
			final ChangeListener changeListener, final SelectionType selection) {
		switch (controlType) {
		case Buttons:
			return new ButtonsControl(settingsModelListSelection, selection,
					this, changeListener);
		case List:
			return new ListControl(settingsModelListSelection, selection, this,
					changeListener);
		case ComboBox:
			return new ComboBoxControl(settingsModelListSelection, selection,
					this, changeListener);
		case Invisible:
			throw new UnsupportedOperationException("Not supported yet.");

		case Slider:
			return new SliderControl(settingsModelListSelection, selection,
					this, changeListener);
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
		int originalPosition = -1;
		{
			final Component[] components = component.getComponents();
			for (int i = 0; i < components.length; i++) {
				if (components[i] == variableControl.getView()) {
					originalPosition = i;
					break;
				}
			}
		}
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
		final SettingsModelListSelection settingsModel = (SettingsModelListSelection) variableControl
				.getModel();
		settingsModel.removeChangeListener(removedVariableControl
				.getModelChangeListener());
		final VariableControl<? extends SettingsModel> control = createControl(
				type, settingsModel,
				createChangeListener(slider, settingsModel),
				removedVariableControl.getSelectionType());
		cache.put(slider, control);
		component.add(control.getView(), originalPosition);
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
		removedVariableControl.getModel().removeChangeListener(
				removedVariableControl.getModelChangeListener());
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
	@Override
	public @Nullable
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see ie.tcd.imm.hits.knime.view.ControlsHandler#setConsistencyChecker(ie.tcd.imm.hits.knime.view.ControlsHandler.ConsistencyChecker)
	 */
	@Override
	public void setConsistencyChecker(final ConsistencyChecker checker) {
		this.checker = checker;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ie.tcd.imm.hits.knime.view.ControlsHandler#addChangeListener(javax.swing.event.ChangeListener)
	 */
	@Override
	public void addChangeListener(final ChangeListener changeListener) {
		if (!containsListener(changeListener)) {
			listeners.add(new WeakReference<ChangeListener>(changeListener));
		}
	}

	/**
	 * @param changeListener
	 *            A {@link ChangeListener}.
	 * @return one of the listeners is {@code changeListener}.
	 */
	private boolean containsListener(final ChangeListener changeListener) {
		for (final WeakReference<ChangeListener> listenerRef : listeners) {
			if (changeListener.equals(listenerRef.get())) {
				return true;
			}
		}
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ie.tcd.imm.hits.knime.view.ControlsHandler#removeChangeListener(javax.swing.event.ChangeListener)
	 */
	@Override
	public void removeChangeListener(final ChangeListener changeListener) {
		for (final Iterator<WeakReference<ChangeListener>> it = listeners
				.iterator(); it.hasNext();) {
			final WeakReference<ChangeListener> listenerRef = it.next();
			final ChangeListener listener = listenerRef.get();
			if (changeListener.equals(listener) || listener == null) {
				it.remove();
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ie.tcd.imm.hits.knime.view.ControlsHandler#exchangeControls(ie.tcd.imm.hits.util.swing.VariableControl,
	 *      ie.tcd.imm.hits.util.swing.VariableControl)
	 */
	@Override
	public boolean exchangeControls(final VariableControl<SettingsModel> first,
			final VariableControl<SettingsModel> second) {
		assert arrangement != null;
		final Pair<Type, String> firstPos = getPosition(first);
		final Pair<Type, String> secondPos = getPosition(second);
		if (firstPos == null || secondPos == null) {
			return false;
		}
		if (firstPos.equals(secondPos)) {
			return false;
		}
		final List<ParameterModel> primerParameters = arrangement
				.getPrimerParameters();
		final List<ParameterModel> seconderParameters = arrangement
				.getSeconderParameters();
		final Map<Type, SliderModel> newArrangement = new HashMap<Type, SliderModel>();
		final ArrangementModel newArrModel = new ArrangementModel();
		final List<ParameterModel> newPrimParams = new ArrayList<ParameterModel>();
		final List<ParameterModel> newSecParams = new ArrayList<ParameterModel>();
		final List<ParameterModel> newAdditionalParams = new ArrayList<ParameterModel>();
		final ShapeModel shapeModel = new ShapeModel(newArrModel,
				newPrimParams, newSecParams, newAdditionalParams, arrangement
						.isDrawBorder(), arrangement.isDrawPrimaryBorders(),
				arrangement.isDrawSecondaryBorders(), arrangement
						.isDrawAdditionalBorders());
		if (checker.checkConsistency(shapeModel)) {
			notifyChangeListeners(new ArrangementEvent(this, shapeModel));
			return true;
		}
		return false;
	}

	private void notifyChangeListeners(final ChangeEvent event) {
		for (final Iterator<WeakReference<ChangeListener>> it = listeners
				.iterator(); it.hasNext();) {
			final WeakReference<ChangeListener> listenerRef = it.next();
			final ChangeListener listener = listenerRef.get();
			if (listener == null) {
				it.remove();
			} else {
				listener.stateChanged(event);
			}
		}
	}

	private @Nullable
	Pair<Type, String> getPosition(
			final VariableControl<SettingsModel> variableControl) {
		Pair<Type, String> ret = null;
		final JComponent view = variableControl.getView();
		for (final Entry<Type, Map<String, WeakReference<JComponent>>> entry : containers
				.entrySet()) {
			for (final Entry<String, WeakReference<JComponent>> contEntry : entry
					.getValue().entrySet()) {
				if (view.equals(contEntry.getValue().get())) {
					if (ret == null || ret.getRight() == null) {
						ret = new Pair<Type, String>(entry.getKey(), contEntry
								.getKey());
					}
				}
			}
		}
		return ret;
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
}

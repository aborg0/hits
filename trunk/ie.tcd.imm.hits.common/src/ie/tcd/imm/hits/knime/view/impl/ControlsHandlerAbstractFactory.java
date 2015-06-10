/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package ie.tcd.imm.hits.knime.view.impl;

import ie.tcd.imm.hits.knime.view.ControlsHandler;
import ie.tcd.imm.hits.knime.view.SplitType;
import ie.tcd.imm.hits.util.select.Selectable;
import ie.tcd.imm.hits.util.swing.SelectionType;
import ie.tcd.imm.hits.util.swing.VariableControl;
import ie.tcd.imm.hits.util.swing.VariableControl.ControlTypes;

import java.awt.Component;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.WeakHashMap;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import javax.swing.JComponent;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.util.Pair;

/**
 * This factory helps to handle {@link VariableControl}s.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 * @param <Model>
 *            The actual model used for the controls.
 * @param <Sel>
 *            The type of the container of {@code Model}s.
 */
@Nonnull
@CheckReturnValue
@NotThreadSafe
public abstract class ControlsHandlerAbstractFactory<Model, Sel extends Selectable<Model>>
		implements ControlsHandler<SettingsModel, Model, Sel> {

	private final Map<Sel, VariableControl<SettingsModel, Model, Sel>> cache = new WeakHashMap<Sel, VariableControl<SettingsModel, Model, Sel>>();

	private final EnumMap<SplitType, Map<String, WeakReference<JComponent>>> containers = new EnumMap<SplitType, Map<String, WeakReference<JComponent>>>(
			SplitType.class);
	{
		for (final SplitType type : SplitType.values()) {
			containers.put(type,
					new HashMap<String, WeakReference<JComponent>>());
		}
	}

	private final Map<JComponent, /* Weak */Map<VariableControl<SettingsModel, Model, Sel>, Boolean>> componentToControls = new WeakHashMap<JComponent, Map<VariableControl<SettingsModel, Model, Sel>, Boolean>>();
	private final Map<VariableControl<SettingsModel, Model, Sel>, JComponent> controlToComponent = new WeakHashMap<VariableControl<SettingsModel, Model, Sel>, JComponent>();
	private final List<WeakReference<ChangeListener>> listeners = new ArrayList<WeakReference<ChangeListener>>();

	private final Map<SplitType, Map<VariableControl<SettingsModel, Model, Sel>, Boolean>> splitToControls = new EnumMap<SplitType, Map<VariableControl<SettingsModel, Model, Sel>, Boolean>>(
			SplitType.class);
	{
		for (final SplitType type : SplitType.values()) {
			splitToControls
					.put(
							type,
							new WeakHashMap<VariableControl<SettingsModel, Model, Sel>, Boolean>());
		}
	}

	private final Map<VariableControl<? extends SettingsModel, ? extends Model, ? extends Selectable<Model>>, SplitType> controlToSplit = new WeakHashMap<VariableControl<? extends SettingsModel, ? extends Model, ? extends Selectable<Model>>, SplitType>();

	/**
	 * Constructs a {@link ControlsHandlerAbstractFactory}.
	 */
	public ControlsHandlerAbstractFactory() {
		super();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public VariableControl<SettingsModel, Model, Sel> getComponent(
			final Sel slider, final ControlTypes controlType,
			final SelectionType selectionType, final SplitType split) {
		if (!cache.containsKey(slider)) {
			final VariableControl<SettingsModel, Model, Sel> control = createNewControl(
					slider, controlType, selectionType, split);
			cache.put(slider, control);
		}
		return cache.get(slider);
	}

	/**
	 * Creates a new {@link VariableControl} based on the input parameters.
	 * Implementation specific!
	 * <p>
	 * Only called from
	 * {@link #getComponent(Selectable, ControlTypes, SelectionType, SplitType)}
	 * , when no suitable found in the cache.
	 * 
	 * @param model
	 *            The model of the new control.
	 * @param controlType
	 *            The type of the control.
	 * @param selectionType
	 *            The {@link SelectionType} of the new control.
	 * @param split
	 *            The {@link SplitType} of the new control.
	 * @return The new control instance.
	 */
	protected abstract VariableControl<SettingsModel, Model, Sel> createNewControl(
			final Sel model, final ControlTypes controlType,
			final SelectionType selectionType, final SplitType split);

	/**
	 * Creates a {@link VariableControl} for {@code slider} (with popup menu).
	 * 
	 * @param slider
	 *            The {@code Model} containing the possible values, ...
	 * @param controlType
	 *            A {@link ControlTypes}.
	 * @param settingsModelListSelection
	 *            A {@link SettingsModelListSelection}.
	 * @param selectionType
	 *            The {@link SelectionType} of the control.
	 * @param split
	 *            The {@link SplitType} of the returned {@link VariableControl}.
	 *            Only needed for the popup menu.
	 * @return The control with the proper parameters.
	 */
	protected VariableControl<SettingsModel, Model, Sel> createControl(
			final Sel slider, final ControlTypes controlType,
			final SettingsModelListSelection settingsModelListSelection,
			final SelectionType selectionType, final SplitType split) {
		final ChangeListener changeListener = createChangeListener(slider,
				settingsModelListSelection);
		return createControl(slider, controlType, settingsModelListSelection,
				changeListener, selectionType, split);
	}

	/**
	 * @param slider
	 *            A {@code Model}.
	 * @param settingsModelListSelection
	 *            A {@link SettingsModelListSelection}.
	 * @return The {@link ChangeListener} adjusting the {@code slider}
	 *         selections to {@code settingsModelListSelection} selections to be
	 *         in synchrony.
	 */
	public ChangeListener createChangeListener(final Selectable<Model> slider,
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
				adjustModel(selections, values, selectedIndices, slider);
				// final Set<Integer> sliderSelection = new HashSet<Integer>(
				// slider.getSelections());
				// if (selectedIndices.size() == 1) {
				// slider.selectSingle(selectedIndices.iterator().next());
				// } else {
				// for (final Integer index : selectedIndices) {
				// if (!sliderSelection.contains(index)) {
				// slider.select(index);
				// }
				// }
				// for (final Integer index : sliderSelection) {
				// if (!selectedIndices.contains(index)) {
				// slider.deselect(index);
				// }
				// }
				// }

			}
		};
		return changeListener;
	}

	/**
	 * @param domainModel
	 *            The model for possible parameters and selections.
	 * @param controlType
	 *            The preferred {@link ControlTypes}.
	 * @param settingsModelListSelection
	 *            The used model.
	 * @param changeListener
	 *            The associated {@link ChangeListener}.
	 * @param selection
	 *            The {@link SelectionType selection} mode.
	 * @param split
	 *            The {@link SplitType} of the new control. Only needed for the
	 *            popup menu.
	 * @return The {@link VariableControl} with the desired parameters.
	 */
	protected VariableControl<SettingsModel, Model, Sel> createControl(
			final Sel domainModel, final ControlTypes controlType,
			final SettingsModelListSelection settingsModelListSelection,
			final ChangeListener changeListener, final SelectionType selection,
			final SplitType split) {
		final VariableControl<SettingsModel, Model, Sel> ret;
		switch (controlType) {
		case Buttons:
			ret = new ButtonsControl<Model, Sel>(settingsModelListSelection,
					selection, this, changeListener, domainModel);
			break;
		case List:
			ret = new ListControl<Model, Sel>(settingsModelListSelection,
					selection, this, changeListener, domainModel);
			break;
		case ComboBox:
			ret = new ComboBoxControl<Model, Sel>(settingsModelListSelection,
					selection, this, changeListener, domainModel);
			break;
		case Invisible:
			throw new UnsupportedOperationException("Not supported yet.");
		case Slider:
			ret = new SliderControl<Model, Sel>(settingsModelListSelection,
					selection, this, changeListener, domainModel);
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
		case TextField:
			ret = new TextFieldControl<Model, Sel>(settingsModelListSelection, selection, this, changeListener, domainModel);
			break;
		default:
			throw new UnsupportedOperationException("Not supported yet: "
					+ controlType);
		}
		final PopupMenu<SettingsModel, Model, Sel> popupMenu = new PopupMenu<SettingsModel, Model, Sel>(
				ret, split, this);
		((AbstractVariableControl<?, ?>) ret).getPanel().addMouseListener(
				popupMenu);
		ret.getView().addMouseListener(popupMenu);
		for (final Component comp : ret.getView().getComponents()) {
			comp.addMouseListener(popupMenu);
		}
		return ret;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean changeControlType(
			final VariableControl<SettingsModel, Model, Sel> variableControl,
			final ControlTypes type) {
		if (type == variableControl.getType()) {
			return false;
		}
		final JComponent component = controlToComponent.get(variableControl);
		if (component == null) {
			return false;
		}
		final Map<VariableControl<SettingsModel, Model, Sel>, Boolean> map = componentToControls
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
		Sel slider = null;
		for (final Entry<Sel, VariableControl<SettingsModel, Model, Sel>> entry : cache
				.entrySet()) {
			if (entry.getValue().equals(variableControl)) {
				slider = entry.getKey();
			}
		}
		final VariableControl<? extends SettingsModel, ? extends Model, ? extends Selectable<Model>> removedVariableControl = cache
				.remove(slider);
		assert removedVariableControl != null;
		final SettingsModelListSelection settingsModel = (SettingsModelListSelection) variableControl
				.getModel();
		settingsModel.removeChangeListener(removedVariableControl
				.getModelChangeListener());
		final SplitType splitType = controlToSplit.get(removedVariableControl);
		assert splitType != null;
		final VariableControl<SettingsModel, Model, Sel> control = createControl(
				slider, type, settingsModel, createChangeListener(slider,
						settingsModel), removedVariableControl
						.getSelectionType(), splitType);
		cache.put(slider, control);
		component.add(control.getView(), originalPosition);
		component.revalidate();
		addToMap(map, control);
		controlToComponent.put(control, component);
		controlToSplit.put(control, splitType);
		final Map<VariableControl<SettingsModel, Model, Sel>, Boolean> map2 = splitToControls
				.get(splitType);
		addToMap(map2, variableControl);
		return true;
	}

	/**
	 * Deregisters a {@code model}.
	 * 
	 * @param model
	 *            A {@code Model}.
	 * @param splitType
	 *            A {@link SplitType} for the model.
	 * @return On success {@code true}, else {@code false}.
	 */
	protected boolean deregister(final Selectable<Model> model,
			final SplitType splitType) {
		final VariableControl<? extends SettingsModel, ? extends Model, ? extends Selectable<Model>> variableControl = cache
				.get(model);
		if (variableControl == null) {
			return false;
		}
		final JComponent component = controlToComponent.get(variableControl);
		if (component == null) {
			return false;
		}
		final Map<VariableControl<SettingsModel, Model, Sel>, Boolean> map = componentToControls
				.get(component);
		assert map != null;
		final Boolean removed = map.remove(variableControl);
		assert removed != null;
		assert removed.booleanValue() == true;
		component.remove(variableControl.getView());
		final JComponent removedComponent = controlToComponent
				.remove(variableControl);
		assert removedComponent != null;
		final VariableControl<? extends SettingsModel, ? extends Model, ? extends Selectable<Model>> removedVariableControl = cache
				.remove(model);
		assert removedVariableControl != null;
		assert removedVariableControl == variableControl;
		removedVariableControl.getModel().removeChangeListener(
				removedVariableControl.getModelChangeListener());
		final Map<VariableControl<SettingsModel, Model, Sel>, Boolean> map2 = splitToControls
				.get(splitType);
		assert map2 != null;
		map2.remove(removedVariableControl);
		controlToSplit.get(removedVariableControl);
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean deregister(final Sel model) {
		final VariableControl<? extends SettingsModel, ? extends Model, ? extends Selectable<Model>> variableControl = cache
				.get(model);
		if (variableControl == null) {
			return false;
		}
		final JComponent component = controlToComponent.get(variableControl);
		if (component == null) {
			return false;
		}
		final SplitType splitType = controlToSplit.get(variableControl);
		assert splitType != null;
		final Boolean r = splitToControls.get(splitType)
				.remove(variableControl);
		assert r != null && r.booleanValue() == true;
		return deregister(model, splitType);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean move(
			final VariableControl<SettingsModel, Model, Sel> variableControl,
			final String nameOfContainer) {
		final JComponent newContainer = getContainer(
				getSplitType(variableControl), nameOfContainer);
		if (newContainer == null) {
			return false;
		}
		final JComponent oldContainer = controlToComponent.get(variableControl);
		if (oldContainer == null) {
			return false;
		}
		final Map<VariableControl<SettingsModel, Model, Sel>, Boolean> oldControls = componentToControls
				.get(oldContainer);
		// Transaction start
		oldControls.remove(variableControl);
		oldContainer.remove(variableControl.getView());
		oldContainer.revalidate();
		newContainer.add(variableControl.getView());
		newContainer.revalidate();
		final Map<VariableControl<SettingsModel, Model, Sel>, Boolean> map = componentToControls
				.get(newContainer);
		assert map != null;
		addToMap(map, variableControl);
		// Transaction end
		controlToComponent.put(variableControl, newContainer);
		return true;
	}

	/**
	 * @param variableControl
	 *            A handled {@link VariableControl}.
	 * @return The {@link SplitType} of {@code variableControl}.
	 */
	private SplitType getSplitType(
			final VariableControl<SettingsModel, Model, Sel> variableControl) {
		return controlToSplit.get(variableControl);
	}

	/**
	 * Adds {@code variableControl} with {@link Boolean#TRUE} to {@code map}.
	 * 
	 * @param <K>
	 *            Type of keys.
	 * @param map
	 *            A map which should contain {@link VariableControl} and
	 *            {@link Boolean}s.
	 * @param variableControl
	 *            A handled {@link VariableControl}.
	 */
	private <K> void addToMap(final Map<K, Boolean> map, final K variableControl) {
		map.put(variableControl, Boolean.TRUE);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean register(final Sel model, final SplitType modelType,
			final String nameOfContainer, final ControlTypes controlType) {
		final JComponent component = getContainer(modelType, nameOfContainer);
		if (component == null) {
			return false;
		}
		final Map<VariableControl<SettingsModel, Model, Sel>, Boolean> map = componentToControls
				.get(component);
		assert map != null;
		final VariableControl<SettingsModel, Model, Sel> variableControl = getComponent(
				model, controlType,
				modelType == SplitType.SingleSelect ? SelectionType.Single
						: SelectionType.MultipleAtLeastOne, modelType);
		if (variableControl == null) {
			return false;
		}
		component.add(variableControl.getView());
		addToMap(map, variableControl);
		final Map<VariableControl<SettingsModel, Model, Sel>, Boolean> map2 = splitToControls
				.get(modelType);
		addToMap(map2, variableControl);
		controlToSplit.put(variableControl, modelType);
		controlToComponent.put(variableControl, component);
		return true;
	}

	/**
	 * Selects a {@link JComponent} if exists with the proper properties.
	 * 
	 * @param splitType
	 *            A {@link SplitType}.
	 * @param nameOfContainer
	 *            A name of the container. May be {@code null}.
	 * @return The {@link JComponent} associated to {@code containerType} and
	 *         {@code nameOfContainer}, or {@code null} if it is not
	 *         {@link #setContainer(JComponent, SplitType, String) set} before.
	 */
	@Override
	public @Nullable
	JComponent getContainer(final SplitType splitType,
			@Nullable final String nameOfContainer) {
		final Map<String, WeakReference<JComponent>> map = containers
				.get(splitType);
		if (map == null) {
			return null;
		}
		final WeakReference<JComponent> component = map.get(nameOfContainer);
		return component == null ? null : component.get();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setContainer(final JComponent container, final SplitType type,
			final String name) {
		final Map<String, WeakReference<JComponent>> map = containers.get(type);
		if (map.isEmpty()) {
			map.put(null, new WeakReference<JComponent>(container));
		}
		map.put(name, new WeakReference<JComponent>(container));
		componentToControls
				.put(
						container,
						new WeakHashMap<VariableControl<SettingsModel, Model, Sel>, Boolean>());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Set<Pair<SplitType, String>> findContainers() {
		final Set<Pair<SplitType, String>> ret = new HashSet<Pair<SplitType, String>>();
		for (final Entry<SplitType, Map<String, WeakReference<JComponent>>> entry : containers
				.entrySet()) {
			for (final String name : entry.getValue().keySet()) {
				ret.add(new Pair<SplitType, String>(entry.getKey(), name));
			}
		}
		return ret;
	}

	/**
	 * {@inheritDoc}
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

	/**
	 * {@inheritDoc}
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

	/**
	 * {@inheritDoc}
	 */
	@Override
	public abstract boolean exchangeControls(
			final VariableControl<SettingsModel, Model, Sel> first,
			final VariableControl<SettingsModel, Model, Sel> second);

	/**
	 * Notifies the added (and still not garbage collected)
	 * {@link ChangeListener}s.
	 * 
	 * @param event
	 *            A {@link ChangeEvent}.
	 */
	protected void notifyChangeListeners(final ChangeEvent event) {
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

	/**
	 * Finds the container's position for {@code variableControl}.
	 * 
	 * @param variableControl
	 *            A handled {@link VariableControl}.
	 * @return The found {@link SplitType} name {@link Pair}, or {@code null}.
	 */
	protected @Nullable
	Pair<SplitType, String> getPosition(
			final VariableControl<SettingsModel, Model, Sel> variableControl) {
		Pair<SplitType, String> ret = null;
		final JComponent view = controlToComponent.get(variableControl);// variableControl.getView();
		for (final Entry<SplitType, Map<String, WeakReference<JComponent>>> entry : containers
				.entrySet()) {
			for (final Iterator<Entry<String, WeakReference<JComponent>>> it = entry
					.getValue().entrySet().iterator(); it.hasNext();) {
				final Entry<String, WeakReference<JComponent>> contEntry = it
						.next();
				if (contEntry.getValue() == null) {
					it.remove();
					continue;
				}
				if (view.equals(contEntry.getValue().get())) {
					if (ret == null || ret.getSecond() == null) {
						ret = new Pair<SplitType, String>(entry.getKey(),
								contEntry.getKey());
					}
				}
			}
		}
		return ret;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Set<VariableControl<SettingsModel, Model, Sel>> getVariableControlsAt(
			final SplitType splitType) {
		final Set<VariableControl<SettingsModel, Model, Sel>> ret = new HashSet<VariableControl<SettingsModel, Model, Sel>>();
		for (final VariableControl<SettingsModel, Model, Sel> variableControl : splitToControls
				.get(splitType).keySet()) {
			ret.add(variableControl);
		}
		return ret;
	}

	/**
	 * Adjusts the {@code model} according to the selection.
	 * 
	 * @param selections
	 *            The selected {@link String}s
	 * @param values
	 *            The possible values.
	 * @param selectedIndices
	 *            The selected indices.
	 * @param model
	 *            The model to modify.
	 */
	protected void adjustModel(final Set<String> selections,
			final List<String> values, final Set<Integer> selectedIndices,
			final Selectable<Model> model) {
		final Set<Integer> sliderSelection = new HashSet<Integer>(model
				.getSelections());
		if (selectedIndices.size() == 1) {
			model.selectSingle(selectedIndices.iterator().next());
		} else {
			for (final Integer index : selectedIndices) {
				if (!sliderSelection.contains(index)) {
					model.select(index);
				}
			}
			for (final Integer index : sliderSelection) {
				if (!selectedIndices.contains(index)) {
					model.deselect(index);
				}
			}
		}
	}
}

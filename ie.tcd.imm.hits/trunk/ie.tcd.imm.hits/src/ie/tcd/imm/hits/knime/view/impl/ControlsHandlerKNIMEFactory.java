/**
 * 
 */
package ie.tcd.imm.hits.knime.view.impl;

import ie.tcd.imm.hits.knime.view.ControlsHandler;
import ie.tcd.imm.hits.knime.view.SplitType;
import ie.tcd.imm.hits.knime.view.heatmap.SliderModel;
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

	private Map<SliderModel, VariableControl<SettingsModel>> cache = new WeakHashMap<SliderModel, VariableControl<SettingsModel>>();
	private final EnumMap<SplitType, Map<String, WeakReference<JComponent>>> containers = new EnumMap<SplitType, Map<String, WeakReference<JComponent>>>(
			SplitType.class);
	{
		for (final SplitType type : SplitType.values()) {
			containers.put(type,
					new HashMap<String, WeakReference<JComponent>>());
		}
	}

	private final Map<JComponent, /* Weak */Map<VariableControl<SettingsModel>, Boolean>> componentToControls = new WeakHashMap<JComponent, Map<VariableControl<SettingsModel>, Boolean>>();
	private final Map<VariableControl<? extends SettingsModel>, JComponent> controlToComponent = new WeakHashMap<VariableControl<? extends SettingsModel>, JComponent>();
	private final List<WeakReference<ChangeListener>> listeners = new ArrayList<WeakReference<ChangeListener>>();
	private ShapeModel arrangement;

	private Map<SplitType, Map<VariableControl<SettingsModel>, Boolean>> splitToControls = new EnumMap<SplitType, Map<VariableControl<SettingsModel>, Boolean>>(
			SplitType.class);
	{
		for (final SplitType type : SplitType.values()) {
			splitToControls.put(type,
					new WeakHashMap<VariableControl<SettingsModel>, Boolean>());
		}
	}

	private Map<VariableControl<? extends SettingsModel>, SplitType> controlToSplit = new WeakHashMap<VariableControl<? extends SettingsModel>, SplitType>();

	/**
	 * Constructs a {@link ControlsHandlerKNIMEFactory}.
	 */
	public ControlsHandlerKNIMEFactory() {
		super();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public VariableControl<SettingsModel> getComponent(
			final SliderModel slider, final ControlTypes controlType,
			final SelectionType selectionType, final SplitType split) {
		if (!cache.containsKey(slider)) {
			final String name = createName(slider);
			final Map<Integer, Pair<ParameterModel, Object>> valueMapping = slider
					.getValueMapping();
			final List<String> vals = new LinkedList<String>();
			for (final Pair<ParameterModel, Object> pair : valueMapping
					.values()) {
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
			final VariableControl<SettingsModel> control = createControl(
					slider, controlType, settingsModelListSelection,
					selectionType, split);
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
	 * Creates a {@link VariableControl} for {@code slider} (with popup menu).
	 * 
	 * @param slider
	 *            The {@link SliderModel} containing the possible values, ...
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
	private VariableControl<SettingsModel> createControl(
			final SliderModel slider, final ControlTypes controlType,
			final SettingsModelListSelection settingsModelListSelection,
			final SelectionType selectionType, final SplitType split) {
		final ChangeListener changeListener = createChangeListener(slider,
				settingsModelListSelection);
		return createControl(controlType, settingsModelListSelection,
				changeListener, selectionType, split);
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
	 * @param split
	 *            The {@link SplitType} of the new control. Only needed for the
	 *            popup menu.
	 * @return The {@link VariableControl} with the desired parameters.
	 */
	private VariableControl<SettingsModel> createControl(
			final ControlTypes controlType,
			final SettingsModelListSelection settingsModelListSelection,
			final ChangeListener changeListener, final SelectionType selection,
			final SplitType split) {
		final VariableControl<SettingsModel> ret;
		switch (controlType) {
		case Buttons:
			ret = new ButtonsControl(settingsModelListSelection, selection,
					this, changeListener);
			break;
		case List:
			ret = new ListControl(settingsModelListSelection, selection, this,
					changeListener);
			break;
		case ComboBox:
			ret = new ComboBoxControl(settingsModelListSelection, selection,
					this, changeListener);
			break;
		case Invisible:
			throw new UnsupportedOperationException("Not supported yet.");
		case Slider:
			ret = new SliderControl(settingsModelListSelection, selection,
					this, changeListener);
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
		final PopupMenu<SettingsModel> popupMenu = new PopupMenu<SettingsModel>(
				ret, split, this);
		((AbstractVariableControl) ret).getPanel().addMouseListener(popupMenu);
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
			final VariableControl<SettingsModel> variableControl,
			final ControlTypes type) {
		if (type == variableControl.getType()) {
			return false;
		}
		final JComponent component = controlToComponent.get(variableControl);
		if (component == null) {
			return false;
		}
		final Map<VariableControl<SettingsModel>, Boolean> map = componentToControls
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
		for (final Entry<SliderModel, VariableControl<SettingsModel>> entry : cache
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
		final SplitType splitType = controlToSplit.get(removedVariableControl);
		assert splitType != null;
		final VariableControl<SettingsModel> control = createControl(type,
				settingsModel, createChangeListener(slider, settingsModel),
				removedVariableControl.getSelectionType(), splitType);
		cache.put(slider, control);
		component.add(control.getView(), originalPosition);
		component.revalidate();
		addToMap(map, control);
		controlToComponent.put(control, component);
		controlToSplit.put(control, splitType);
		final Map<VariableControl<SettingsModel>, Boolean> map2 = splitToControls
				.get(splitType);
		addToMap(map2, variableControl);
		return true;
	}

	/**
	 * Deregisters a {@code model}.
	 * 
	 * @param model
	 *            A {@link SliderModel}.
	 * @param splitType
	 *            A {@link SplitType} for the model.
	 * @return On success {@code true}, else {@code false}.
	 */
	private boolean deregister(final SliderModel model,
			final SplitType splitType) {
		final VariableControl<? extends SettingsModel> variableControl = cache
				.get(model);
		if (variableControl == null) {
			return false;
		}
		final JComponent component = controlToComponent.get(variableControl);
		if (component == null) {
			return false;
		}
		final Map<VariableControl<SettingsModel>, Boolean> map = componentToControls
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
		assert removedVariableControl == variableControl;
		removedVariableControl.getModel().removeChangeListener(
				removedVariableControl.getModelChangeListener());
		final Map<VariableControl<SettingsModel>, Boolean> map2 = splitToControls
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
	public boolean move(final VariableControl<SettingsModel> variableControl,
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
		final Map<VariableControl<SettingsModel>, Boolean> oldControls = componentToControls
				.get(oldContainer);
		// Transaction start
		oldControls.remove(variableControl);
		oldContainer.remove(variableControl.getView());
		oldContainer.revalidate();
		newContainer.add(variableControl.getView());
		newContainer.revalidate();
		final Map<VariableControl<SettingsModel>, Boolean> map = componentToControls
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
			final VariableControl<SettingsModel> variableControl) {
		return controlToSplit.get(variableControl);
	}

	/**
	 * Adds {@code variableControl} with {@link Boolean#TRUE} to {@code map}.
	 * 
	 * @param map
	 *            A map which should contain {@link VariableControl} and
	 *            {@link Boolean}s.
	 * @param variableControl
	 *            A handled {@link VariableControl}.
	 */
	private void addToMap(
			final Map<VariableControl<SettingsModel>, Boolean> map,
			final VariableControl<SettingsModel> variableControl) {
		map.put(variableControl, Boolean.TRUE);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean register(final SliderModel model, final SplitType modelType,
			final String nameOfContainer, final ControlTypes controlType) {
		final JComponent component = getContainer(modelType, nameOfContainer);
		if (component == null) {
			return false;
		}
		final Map<VariableControl<SettingsModel>, Boolean> map = componentToControls
				.get(component);
		assert map != null;
		final VariableControl<SettingsModel> variableControl = getComponent(
				model, controlType,
				modelType == SplitType.SingleSelect ? SelectionType.Single
						: SelectionType.MultipleAtLeastOne, modelType);
		if (variableControl == null) {
			return false;
		}
		component.add(variableControl.getView());
		addToMap(map, variableControl);
		final Map<VariableControl<SettingsModel>, Boolean> map2 = splitToControls
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
	JComponent getContainer(final SplitType splitType, @Nullable
	final String nameOfContainer) {
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
		componentToControls.put(container,
				new WeakHashMap<VariableControl<SettingsModel>, Boolean>());
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
	public boolean exchangeControls(final VariableControl<SettingsModel> first,
			final VariableControl<SettingsModel> second) {
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
	 * Finds a {@link SliderModel} belonging to {@code control} in
	 * {@code sliderModels}.
	 * 
	 * @param control
	 *            A handled {@link VariableControl}.
	 * @param sliderModels
	 *            The possible {@link SliderModel}s.
	 * @return The found {@link SliderModel} or {@code null}.
	 */
	private @Nullable
	SliderModel findSlider(final VariableControl<SettingsModel> control,
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
	 * Notifies the added (and still not garbage collected)
	 * {@link ChangeListener}s.
	 * 
	 * @param event
	 *            A {@link ChangeEvent}.
	 */
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

	/**
	 * Finds the container's position for {@code variableControl}.
	 * 
	 * @param variableControl
	 *            A handled {@link VariableControl}.
	 * @return The found {@link SplitType} name {@link Pair}, or {@code null}.
	 */
	private @Nullable
	Pair<SplitType, String> getPosition(
			final VariableControl<SettingsModel> variableControl) {
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
					if (ret == null || ret.getRight() == null) {
						ret = new Pair<SplitType, String>(entry.getKey(),
								contEntry.getKey());
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

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Set<VariableControl<SettingsModel>> getVariableControlsAt(
			final SplitType splitType) {
		final Set<VariableControl<SettingsModel>> ret = new HashSet<VariableControl<SettingsModel>>();
		for (final VariableControl<SettingsModel> variableControl : splitToControls
				.get(splitType).keySet()) {
			ret.add(variableControl);
		}
		return ret;
	}
}

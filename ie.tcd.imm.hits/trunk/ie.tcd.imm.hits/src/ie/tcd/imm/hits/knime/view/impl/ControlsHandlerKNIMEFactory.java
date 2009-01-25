/**
 * 
 */
package ie.tcd.imm.hits.knime.view.impl;

import ie.tcd.imm.hits.knime.view.ControlsHandler;
import ie.tcd.imm.hits.knime.view.heatmap.SliderModel;
import ie.tcd.imm.hits.knime.view.heatmap.ViewModel.ParameterModel;
import ie.tcd.imm.hits.util.Pair;
import ie.tcd.imm.hits.util.swing.SelectionType;
import ie.tcd.imm.hits.util.swing.VariableControl;
import ie.tcd.imm.hits.util.swing.VariableControl.ControlTypes;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;
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
		final Set<Integer> selections = slider.getSelections();
		final Collection<String> selected = new HashSet<String>();
		for (final Integer integer : selections) {
			selected.add(vals.get(integer.intValue() - 1));
		}
		// switch (controlType) {
		// case Buttons:
		// selected = vals;
		// break;
		// case ComboBox:
		// selected = Collections.singletonList(vals.get(0));
		// break;
		// case Invisible:
		// selected = Collections.<String> emptyList();
		// break;
		// case List:
		// selected = vals;
		// break;
		// case RadioButton:
		// selected = Collections.<String> emptyList();
		// break;
		// case ScrollBarHorisontal:
		// selected = vals;
		// break;
		// case ScrollBarVertical:
		// selected = vals;
		// break;
		// case Slider:
		// selected = Collections.singletonList(vals.get(0));
		// break;
		// case Tab:
		// selected = Collections.singletonList(vals.get(0));
		// break;
		// default:
		// throw new UnsupportedOperationException("Unknown control type: "
		// + controlType);
		// }
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
		switch (controlType) {
		case Buttons:
			if (!cache.containsKey(slider)) {
				cache.put(slider, new ButtonsControl(
						settingsModelListSelection,
						SelectionType.MultipleAtLeastOne));
			}
			return cache.get(slider);
		case List:
			if (!cache.containsKey(slider)) {
				cache.put(slider, new ListControl(settingsModelListSelection,
						SelectionType.MultipleAtLeastOne));
			}
			return cache.get(slider);
		case ComboBox:
			if (!cache.containsKey(slider)) {
				cache.put(slider, new ComboBoxControl(
						settingsModelListSelection, SelectionType.Single));
			}
			return cache.get(slider);
		case Invisible:
			break;

		case Slider:
			if (!cache.containsKey(slider)) {
				cache.put(slider, new SliderControl(settingsModelListSelection,
						SelectionType.Single));
			}
			return cache.get(slider);
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
		return null;
	}
}

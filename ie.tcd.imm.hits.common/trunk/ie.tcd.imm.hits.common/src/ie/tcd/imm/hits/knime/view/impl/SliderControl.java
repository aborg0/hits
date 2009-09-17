/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package ie.tcd.imm.hits.knime.view.impl;

import ie.tcd.imm.hits.knime.view.ControlsHandler;
import ie.tcd.imm.hits.knime.view.ListSelection;
import ie.tcd.imm.hits.util.swing.SelectionType;
import ie.tcd.imm.hits.util.swing.VariableControl;

import java.util.Collections;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.swing.DefaultBoundedRangeModel;
import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.node.defaultnodesettings.SettingsModel;

import edu.umd.cs.findbugs.annotations.DefaultAnnotation;

/**
 * A {@link VariableControl} with {@link VariableControl.ControlTypes#Slider}.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 * @param <Model>
 *            Type of the model for values.
 */
@DefaultAnnotation( { Nonnull.class, CheckReturnValue.class })
public class SliderControl<Model> extends AbstractVariableControl<Model> {
	private final JSlider slider = new JSlider(new DefaultBoundedRangeModel(1,
			0, 1, 1));

	/**
	 * @param model
	 *            The {@link SettingsModelListSelection}.
	 * @param selectionType
	 *            The initial {@link SelectionType}.
	 * @param controlsHandler
	 *            The used {@link ControlsHandler}.
	 * @param changeListener
	 *            The {@link ChangeListener} associated to the {@code model}.
	 */
	public SliderControl(final SettingsModelListSelection model,
			final SelectionType selectionType,
			final ControlsHandler<SettingsModel, Model> controlsHandler,
			final ChangeListener changeListener) {
		super(model, selectionType, controlsHandler, changeListener);
		switch (selectionType) {
		case MultipleAtLeastOne:
		case MultipleOrNone:
			if (model.getSelection().size() > 1) {
				model.setSelection(Collections.singleton(model.getSelection()
						.iterator().next()));
			}
			break;
		case Single:
		case Unmodifiable:
			break;
		default:
			break;
		}
		slider.setName(model.getConfigName());
		slider.setSnapToTicks(true);
		slider.setPaintLabels(true);
		updateComponent();
		getPanel().add(slider);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.knime.core.node.defaultnodesettings.DialogComponent#setEnabledComponents
	 * (boolean)
	 */
	@Override
	protected void setEnabledComponents(final boolean enabled) {
		slider.setEnabled(enabled);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * ie.tcd.imm.hits.knime.view.impl.AbstractVariableControl#updateComponent()
	 */
	@Override
	protected void updateComponent() {
		@SuppressWarnings("unchecked")
		final ListSelection<String> model = (ListSelection<String>) getModel();
		final List<String> possibleValues = model.getPossibleValues();
		final Dictionary<Integer, JLabel> labels = new Hashtable<Integer, JLabel>();
		int i = 1;
		for (final String valueStr : possibleValues) {
			labels.put(Integer.valueOf(i++), new JLabel(valueStr));
		}
		slider.setLabelTable(labels);
		slider.getModel().setMinimum(1);
		slider.getModel().setMaximum(i - 1);
		final String selectionStr = model.getSelection().iterator().next();
		final int selected = select(possibleValues, selectionStr);
		if (slider.getValue() != selected) {
			slider.setValue(selected);
		}
		slider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(final ChangeEvent e) {
				final Set<String> newSelection = Collections
						.singleton(((JLabel) slider.getLabelTable().get(
								Integer.valueOf(slider.getValue()))).getText());
				if (getSelectionType() != SelectionType.Unmodifiable) {
					if (!newSelection.equals(model.getSelection())) {
						model.setSelection(newSelection);
						updateComponent();
					}
				} else {
					if (!newSelection.equals(model.getSelection())) {
						updateComponent();
					}
				}
			}
		});
	}

	/**
	 * Selects the index of {@code selectionStr} in {@code possibleValues}
	 * (starting from {@code 1}).
	 * 
	 * @param possibleValues
	 *            A {@link List} of {@link String}s.
	 * @param selectionStr
	 *            A {@link String} from {@code possibleValues}.
	 * @return The index of {@code selectionStr} in {@code possibleValues}
	 *         starting from {@code 1}.
	 */
	private int select(final List<String> possibleValues,
			final String selectionStr) {
		int i = 1;
		for (final String val : possibleValues) {
			if (val.equals(selectionStr)) {
				return i;
			}
			++i;
		}
		throw new IllegalStateException("Not found selection: " + selectionStr
				+ " in : " + possibleValues);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ie.tcd.imm.hits.knime.view.impl.AbstractVariableControl#getType()
	 */
	@Override
	public ControlTypes getType() {
		return ControlTypes.Slider;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + (slider == null ? 0 : slider.hashCode());
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (!super.equals(obj)) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final SliderControl<?> other = (SliderControl<?>) obj;
		if (slider == null) {
			if (other.slider != null) {
				return false;
			}
		} else if (slider != other.slider) {
			return false;
		}
		return true;
	}

}

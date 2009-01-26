/**
 * 
 */
package ie.tcd.imm.hits.knime.view.impl;

import ie.tcd.imm.hits.knime.view.ListSelection;
import ie.tcd.imm.hits.util.swing.SelectionType;
import ie.tcd.imm.hits.util.swing.VariableControl;

import java.util.List;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.swing.DefaultBoundedRangeModel;
import javax.swing.JSlider;

import edu.umd.cs.findbugs.annotations.DefaultAnnotation;

/**
 * A {@link VariableControl} with {@link VariableControl.ControlTypes#Slider}.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
@DefaultAnnotation( { Nonnull.class, CheckReturnValue.class })
class SliderControl extends AbstractVariableControl {
	private static final long serialVersionUID = -3901494558881660768L;
	private final JSlider slider = new JSlider(new DefaultBoundedRangeModel(1,
			0, 1, 1));

	/**
	 * @param model
	 * @param selectionType
	 */
	public SliderControl(final SettingsModelListSelection model,
			final SelectionType selectionType) {
		super(model, selectionType);
		slider.setName(model.getConfigName());
		updateComponent();
		getPanel().add(slider);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.knime.core.node.defaultnodesettings.DialogComponent#setEnabledComponents(boolean)
	 */
	@Override
	protected void setEnabledComponents(final boolean enabled) {
		slider.setEnabled(enabled);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ie.tcd.imm.hits.knime.view.impl.AbstractVariableControl#updateComponent()
	 */
	@Override
	protected void updateComponent() {
		@SuppressWarnings("unchecked")
		final ListSelection<String> model = (ListSelection<String>) getModel();
		final List<String> possibleValues = model.getPossibleValues();
		int min = Integer.MAX_VALUE;
		int max = Integer.MIN_VALUE;
		for (final String valueStr : possibleValues) {
			final int i = Integer.parseInt(valueStr);
			min = Math.min(min, i);
			max = Math.max(max, i);
		}
		if (slider.getModel().getMinimum() != min) {
			slider.getModel().setMinimum(min);
		}
		if (slider.getModel().getMaximum() != max) {
			slider.getModel().setMaximum(max);
		}
		final String selectionStr = model.getSelection().iterator().next();
		final int selected = Integer.parseInt(selectionStr);
		if (slider.getValue() != selected) {
			slider.setValue(selected);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ie.tcd.imm.hits.knime.view.impl.AbstractVariableControl#getType()
	 */
	@Override
	protected ControlTypes getType() {
		return ControlTypes.Slider;
	}
}

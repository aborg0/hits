package ie.tcd.imm.hits.knime.view;

import ie.tcd.imm.hits.knime.view.heatmap.SliderModel;
import ie.tcd.imm.hits.util.swing.VariableControl;
import ie.tcd.imm.hits.util.swing.VariableControl.ControlTypes;

/**
 * This interface is for handling the components.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
public interface ControlsHandler<ModelType> {
	/**
	 * Creates or gets the component for the {@code slider} with the type of
	 * {@code controlType}.
	 * 
	 * @param slider
	 *            The slider which the control belongs to.
	 * @param controlType
	 *            The type of the control.
	 * @return The associated component in the proper form.
	 */
	public VariableControl<? extends ModelType> getComponent(
			SliderModel slider, ControlTypes controlType);
}

/**
 * 
 */
package ie.tcd.imm.hits.knime.view.impl;

import ie.tcd.imm.hits.knime.view.ControlsHandler;
import ie.tcd.imm.hits.knime.view.heatmap.SliderModel;
import ie.tcd.imm.hits.util.swing.SelectionType;
import ie.tcd.imm.hits.util.swing.VariableControl;
import ie.tcd.imm.hits.util.swing.VariableControl.ControlTypes;

import java.util.Map;
import java.util.WeakHashMap;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;

import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.defaultnodesettings.SettingsModelFilterString;

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
		final String name = slider.getType().toString() + slider.getSubId();
		switch (controlType) {
		case Buttons:
			if (!cache.containsKey(slider)) {
				cache.put(slider, new ButtonsControl(
						new SettingsModelFilterString(name),
						SelectionType.MultipleAtLeastOne));
			}
			return cache.get(slider);
		case ComboBox:
			break;
		case Invisible:
			break;

		case Slider:
			break;
		case RadioButton:
			throw new UnsupportedOperationException("Not supported yet.");
		case Tab:
			throw new UnsupportedOperationException("Not supported yet.");
		case ScrollBarHorisontal:
			throw new UnsupportedOperationException("Not supported yet.");
		case ScrollBarVertical:
			throw new UnsupportedOperationException("Not supported yet.");
		case List:
			throw new UnsupportedOperationException("Not supported yet.");
		default:
			throw new UnsupportedOperationException("Not supported yet: "
					+ controlType);
		}
		return null;
	}
}

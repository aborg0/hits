package ie.tcd.imm.hits.knime.view;

import ie.tcd.imm.hits.knime.view.heatmap.SliderModel;
import ie.tcd.imm.hits.knime.view.heatmap.SliderModel.Type;
import ie.tcd.imm.hits.util.swing.VariableControl;
import ie.tcd.imm.hits.util.swing.VariableControl.ControlTypes;

import java.awt.Container;

import javax.annotation.Nullable;
import javax.swing.JComponent;

/**
 * This interface is for handling the components. (This should also handle the
 * possible drop targets.)
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 * @param <ModelType>
 *            The type of the used model in {@link VariableControl}.
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
	@Deprecated
	public VariableControl<? extends ModelType> getComponent(
			SliderModel slider, ControlTypes controlType);

	/**
	 * Registers the {@link VariableControl} associated to {@code model} to the
	 * container associated with {@code containerType}.
	 * <p>
	 * Registering a {@code model} again will <b>automatically
	 * {@link #deregister(SliderModel)} it</b> and register with the new
	 * parameters.!
	 * <p>
	 * This will create the {@link VariableControl} with the default
	 * {@link ControlTypes} for each {@link Type}:
	 * <ul>
	 * <li>{@link Type#Hidden} &rarr; {@link ControlTypes#ComboBox}</li>
	 * <li>{@link Type#ScrollHorisontal} &rarr;
	 * {@link ControlTypes#ScrollBarHorisontal}</li>
	 * <li>{@link Type#ScrollVertical} &rarr;
	 * {@link ControlTypes#ScrollBarVertical}</li>
	 * <li>{@link Type#Selector} &rarr; {@link ControlTypes#Slider}</li>
	 * <li>{@link Type#Splitter} &rarr; {@link ControlTypes#Buttons}</li>
	 * </ul>
	 * 
	 * @param model
	 *            The model of the control to register.
	 * @param containerType
	 *            The positional type of the container.
	 * @param nameOfContainer
	 *            The name of the container. May be {@code null}, which means
	 *            one of the possible values, not specified.
	 * @return Indicates whether the registration did something ({@code true})
	 *         or not ({@code false}).
	 * @see #deregister(SliderModel)
	 * @see #setContainer(JComponent,
	 *      ie.tcd.imm.hits.knime.view.heatmap.SliderModel.Type, String)
	 */
	public boolean register(SliderModel model, SliderModel.Type containerType,
			@Nullable
			String nameOfContainer);

	/**
	 * Removes all {@link VariableControl}s associated to the {@code model}.
	 * <p>
	 * If {@code model} previously was not
	 * {@link #register(SliderModel, ie.tcd.imm.hits.knime.view.heatmap.SliderModel.Type, String) registered}
	 * it will do nothing.
	 * 
	 * @param model
	 *            A previously registered {@link SliderModel}.
	 * @return Indicates whether the deregistration did something ({@code true})
	 *         or not ({@code false}).
	 * @see #register(SliderModel,
	 *      ie.tcd.imm.hits.knime.view.heatmap.SliderModel.Type, String)
	 */
	public boolean deregister(SliderModel model);

	/**
	 * Registers {@code container} as a {@link Container} for the
	 * {@link SliderModel}s with type {@code type}. It can be referenced as
	 * {@code name} in {@link #register(SliderModel, Type, String)}.
	 * 
	 * @param container
	 *            A {@link JComponent}.
	 * @param type
	 *            A {@link Type} of the {@link SliderModel}.
	 * @param name
	 *            A name associated to the {@code container}
	 * @see #register(SliderModel, Type, String)
	 */
	public void setContainer(JComponent container, SliderModel.Type type,
			String name);

	/**
	 * Moves the {@link VariableControl} associated to {@code model} to the new
	 * position: {@code containerType} and {@code nameOfContainer}.
	 * 
	 * @param model
	 *            A {@link #register(SliderModel, Type, String) registered}
	 *            {@link SliderModel}.
	 * @param containerType
	 *            The <b>new</b> {@link Type}.
	 * @param nameOfContainer
	 *            The <b>new</b> name of the container. May be {@code null},
	 *            which means one of the possible containers.
	 * @return Indicates whether the it has moved ({@code true}) or not ({@code false}).
	 */
	public boolean move(SliderModel model, SliderModel.Type containerType,
			@Nullable
			String nameOfContainer);

	/**
	 * Changes the {@link ControlTypes} of the registered {@link SliderModel} ({@code slider})
	 * to {@code type} if possible. If not possible it will do nothing.
	 * 
	 * @param variableControl
	 *            A {@link #register(SliderModel, Type, String) registered}
	 *            {@link VariableControl}.
	 * @param type
	 *            The <b>new</b> {@link ControlTypes} of the {@code slider}.
	 * @return Indicates whether the change has done ({@code true}) or not ({@code false}).
	 */
	public boolean changeControlType(
			VariableControl<ModelType> variableControl, ControlTypes type);
}

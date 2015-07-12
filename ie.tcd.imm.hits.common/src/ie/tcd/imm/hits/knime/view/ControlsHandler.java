/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package ie.tcd.imm.hits.knime.view;

import ie.tcd.imm.hits.util.select.Selectable;
import ie.tcd.imm.hits.util.swing.SelectionType;
import ie.tcd.imm.hits.util.swing.VariableControl;
import ie.tcd.imm.hits.util.swing.VariableControl.ControlTypes;

import java.awt.Container;
import java.util.Set;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.JComponent;
import javax.swing.event.ChangeListener;

import org.knime.core.util.Pair;

/**
 * This interface is for handling the components. (This should also handle the
 * possible drop targets.)
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 * @param <ModelType>
 *            The type of the used (inner) model in {@link VariableControl}.
 * @param <Model>
 *            The type of the model.
 * @param <Sel>
 *            The type of the container of {@code Model}s.
 */
@Nonnull
@CheckReturnValue
public interface ControlsHandler<ModelType, Model, Sel extends Selectable<Model>> {
	/**
	 * Creates or gets the component for the {@code slider} with the type of
	 * {@code controlType}.
	 * 
	 * @param model
	 *            The slider which the control belongs to.
	 * @param controlType
	 *            The type of the control.
	 * @param selectionType
	 *            The {@link SelectionType} for the returned
	 *            {@link VariableControl}.
	 * @param splitType
	 *            The split behaviour. Necessary for the correct popup menu.
	 * @return The associated component in the proper form.
	 */
	@Deprecated
	public VariableControl<? extends ModelType, ? extends Model, ? extends Sel> getComponent(
			final Sel model, final ControlTypes controlType,
			final SelectionType selectionType, SplitType splitType);

	/**
	 * Registers the {@link VariableControl} associated to {@code model} to the
	 * container associated with {@code containerType}.
	 * <p>
	 * Registering a {@code model} again will <b>automatically
	 * {@link #deregister(Selectable)} it</b> and register with the new
	 * parameters!
	 * 
	 * @param model
	 *            The model of the control to register.
	 * @param splitType
	 *            The positional type of the container.
	 * @param nameOfContainer
	 *            The name of the container. May be {@code null}, which means
	 *            one of the possible values, not specified.
	 * @param preferredControlType
	 *            The preferred control type for the {@code model}.
	 * @return Indicates whether the registration did something ({@code true})
	 *         or not ({@code false}).
	 * @see #deregister(Selectable)
	 * @see #setContainer(JComponent, SplitType, String)
	 */
	public boolean register(final Sel model, final SplitType splitType,
			@Nullable final String nameOfContainer,
			final ControlTypes preferredControlType);

	/**
	 * Removes all {@link VariableControl}s associated to the {@code model}.
	 * <p>
	 * If {@code model} previously was not
	 * {@link #register(Selectable, SplitType, String, ControlTypes) registered}
	 * it will do nothing.
	 * 
	 * @param model
	 *            A previously registered {@code Model}.
	 * @return Indicates whether the deregistration did something ({@code true})
	 *         or not ({@code false}).
	 * @see #register(Selectable, SplitType, String, ControlTypes)
	 */
	public boolean deregister(Sel model);

	/**
	 * Registers {@code container} as a {@link Container} for the {@code Model}s
	 * with type {@code type}. It can be referenced as {@code name} in
	 * {@link #register(Selectable, SplitType, String, ControlTypes)}.
	 * 
	 * @param container
	 *            A {@link JComponent}.
	 * @param type
	 *            A {@link SplitType} of the {@code Model}.
	 * @param name
	 *            A name associated to the {@code container}
	 * @see #register(Selectable, SplitType, String, ControlTypes)
	 */
	public void setContainer(JComponent container, SplitType type, String name);

	/**
	 * Moves the {@link VariableControl} associated to {@code model} to the new
	 * position: {@code containerType} and {@code nameOfContainer}.
	 * 
	 * @param variableControl
	 *            A
	 *            {@link #register(Selectable, SplitType, String, ControlTypes)
	 *            registered} {@link VariableControl}.
	 * @param nameOfContainer
	 *            The <b>new</b> name of the container. May be {@code null},
	 *            which means one of the possible containers.
	 * @return Indicates whether the it has moved ({@code true}) or not ({@code
	 *         false}).
	 */
	public boolean move(VariableControl<ModelType, Model, Sel> variableControl,
			@Nullable String nameOfContainer);

	/**
	 * Changes the {@link ControlTypes} of the registered {@code Model} (
	 * {@code slider}) to {@code type} if possible. If not possible it will do
	 * nothing.
	 * 
	 * @param variableControl
	 *            A
	 *            {@link #register(Selectable, SplitType, String, ControlTypes)
	 *            registered} {@link VariableControl}.
	 * @param type
	 *            The <b>new</b> {@link ControlTypes} of the {@code slider}.
	 * @return Indicates whether the change has done ({@code true}) or not (
	 *         {@code false}).
	 */
	public boolean changeControlType(
			VariableControl<ModelType, Model, Sel> variableControl,
			ControlTypes type);

	/**
	 * @return The possible component positions.
	 * @see #setContainer(JComponent, SplitType, String)
	 */
	public Set<Pair<SplitType, String>> findContainers();

	/**
	 * Selects a {@link JComponent} if exists with the proper properties.
	 * 
	 * @param containerType
	 *            A {@link SplitType}.
	 * @param nameOfContainer
	 *            A name of the container. May be {@code null}.
	 * @return The {@link JComponent} associated to {@code containerType} and
	 *         {@code nameOfContainer}, or {@code null} if it is not
	 *         {@link #setContainer(JComponent, SplitType, String) set} before.
	 */
	public @Nullable
	JComponent getContainer(final SplitType containerType,
			@Nullable final String nameOfContainer);

	/**
	 * Adds a {@link ChangeListener} ({@code changeListener}) to
	 * {@link ControlsHandler}. This will notify the listener about the {@code
	 * Model} type changes.
	 * 
	 * @param changeListener
	 *            A {@link ChangeListener}.
	 * @see #removeChangeListener(ChangeListener)
	 */
	public void addChangeListener(final ChangeListener changeListener);

	/**
	 * Removes {@code changeListener} from the {@link ChangeListener} of the
	 * {@link ControlsHandler}.
	 * 
	 * @param changeListener
	 *            A {@link ChangeListener}
	 * @see #addChangeListener(ChangeListener)
	 */
	public void removeChangeListener(final ChangeListener changeListener);

	/**
	 * Changes the two {@link VariableControl}s. They must be associated with
	 * different {@link SplitType}s.
	 * 
	 * @param first
	 *            A {@link VariableControl}.
	 * @param second
	 *            Another {@link VariableControl}.
	 * @return Indicates whether the change has done ({@code true}) or not (
	 *         {@code false}).
	 */
	public boolean exchangeControls(
			final VariableControl<ModelType, Model, Sel> first,
			final VariableControl<ModelType, Model, Sel> second);

	/**
	 * Finds the handled {@link VariableControl}s with {@link SplitType}:
	 * {@code splitType}.
	 * 
	 * @param splitType
	 *            A {@link SplitType}.
	 * @return The {@link VariableControl}s with {@link SplitType}: {@code
	 *         splitType}.
	 */
	public Set<VariableControl<ModelType, Model, Sel>> getVariableControlsAt(
			SplitType splitType);
}

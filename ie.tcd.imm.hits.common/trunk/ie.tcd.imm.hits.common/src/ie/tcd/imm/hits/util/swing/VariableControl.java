/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package ie.tcd.imm.hits.util.swing;

import ie.tcd.imm.hits.knime.view.ControlsHandler;
import ie.tcd.imm.hits.util.Selectable;

import java.util.EnumSet;
import java.util.Set;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JRadioButton;
import javax.swing.JSlider;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeListener;

/**
 * A Swing control to be able to change the representation of the control look.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 * @param <ModelType>
 *            The type of the model used inside.
 * @param <Model>
 *            The model of the alternatives.
 * @param <Sel>
 *            The type of the container of {@code Model}s.
 */
public interface VariableControl<ModelType, Model, Sel extends Selectable<Model>> {

	/**
	 * The supported control types.
	 */
	public static enum ControlTypes {
		/** Something like {@link JSlider}. */
		Slider(EnumSet.of(SelectionType.Unmodifiable, SelectionType.Single)),
		/** Something like {@link JComboBox}. */
		ComboBox(EnumSet.of(SelectionType.Unmodifiable, SelectionType.Single)),
		/** In a scrollpane the values are distributed horizontally. */
		ScrollBarHorisontal(EnumSet.of(SelectionType.Unmodifiable)),
		/** In a scrollpane the values are distributed vertically. */
		ScrollBarVertical(EnumSet.of(SelectionType.Unmodifiable)),
		/** Something like {@link JRadioButton}. */
		RadioButton(EnumSet
				.of(SelectionType.Unmodifiable, SelectionType.Single)),
		/** Something like {@link JList}. */
		List(EnumSet.of(SelectionType.Unmodifiable, SelectionType.Single,
				SelectionType.MultipleAtLeastOne, SelectionType.MultipleOrNone)),
		/** Some buttons on a panel. */
		Buttons(EnumSet.of(SelectionType.Unmodifiable, SelectionType.Single,
				SelectionType.MultipleAtLeastOne, SelectionType.MultipleOrNone)),
		/** Something like a {@link JTabbedPane}. */
		Tab(EnumSet.of(SelectionType.Unmodifiable, SelectionType.Single)),
		/** The control which is not visible to the user. */
		Invisible(EnumSet.allOf(SelectionType.class));

		private final Set<SelectionType> possibleSelections;

		private ControlTypes(final Set<SelectionType> possibleSelections) {
			this.possibleSelections = possibleSelections;
		}

		/**
		 * @return the possible {@link SelectionType}s.
		 */
		public Set<SelectionType> getPossibleSelections() {
			return possibleSelections;
		}
	}

	/**
	 * @return the selectionType.
	 */
	public SelectionType getSelectionType();

	/**
	 * @return the actual view.
	 */
	public JComponent getView();

	/**
	 * @return the model associated to this control.
	 */
	public ModelType getModel();

	/**
	 * Sets whether the view may be detached or not.
	 * 
	 * @param isFloatable
	 *            the new value of floatable property.
	 * @see #isFloatable()
	 */
	public void setFloatable(boolean isFloatable);

	/**
	 * @return the value of floatable property.
	 * @see #setFloatable(boolean)
	 */
	public boolean isFloatable();

	/**
	 * @return The associated {@link ControlsHandler}.
	 */
	public ControlsHandler<ModelType, Model, Sel> getControlsHandler();

	/**
	 * @return The type of the implementation.
	 */
	public ControlTypes getType();

	/**
	 * @return The {@link ChangeListener} which is associated to the {@code
	 *         ModelType} {@link #getModel() model}.
	 */
	public ChangeListener getModelChangeListener();
}

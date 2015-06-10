/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package ie.tcd.imm.hits.knime.view.impl;

import ie.tcd.imm.hits.knime.view.ControlsHandler;
import ie.tcd.imm.hits.knime.view.ListSelection;
import ie.tcd.imm.hits.util.select.Selectable;
import ie.tcd.imm.hits.util.swing.SelectionType;
import ie.tcd.imm.hits.util.swing.VariableControl;

import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.knime.core.node.defaultnodesettings.SettingsModel;

import edu.umd.cs.findbugs.annotations.DefaultAnnotation;

/**
 * A {@link VariableControl} with control type:
 * {@link VariableControl.ControlTypes#List}.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 * @param <Model>
 *            Type of the model for values.
 * @param <Sel>
 *            The type of the container of {@code Model}s.
 */
@DefaultAnnotation( { Nonnull.class, CheckReturnValue.class })
public class ListControl<Model, Sel extends Selectable<Model>> extends
		AbstractVariableControl<Model, Sel> {
	/**
	 * Constructs a {@link ListControl}.
	 * 
	 * @param model
	 *            The {@link SettingsModelListSelection} for the
	 *            {@link VariableControl}.
	 * @param selectionType
	 *            The {@link SelectionType selection type}.
	 * @param controlsHandler
	 *            The {@link ControlsHandler} for the possible transformations.
	 * @param changeListener
	 *            The {@link ChangeListener} associated to the {@code model}.
	 * @param domainModel
	 *            The model for possible parameters and selections.
	 */
	public ListControl(final SettingsModelListSelection model,
			final SelectionType selectionType,
			final ControlsHandler<SettingsModel, Model, Sel> controlsHandler,
			final ChangeListener changeListener, final Sel domainModel) {
		super(model, selectionType, controlsHandler, changeListener,
				domainModel);
		list.setName(model.getConfigName());
		updateComponent();
		switch (selectionType) {
		case Unmodifiable:
			// Do nothing.
			break;
		case Single:
			list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			break;
		case MultipleAtLeastOne:
			list
					.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
			break;
		case MultipleOrNone:
			list
					.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
			break;
		default:
			throw new UnsupportedOperationException(
					"Not supported selection type: " + selectionType);
		}
		list.addListSelectionListener(new ListSelectionListener() {

			@Override
			public void valueChanged(final ListSelectionEvent e) {
				final Object[] selectedValues = list.getSelectedValues();
				final Set<String> selection = new HashSet<String>();
				for (final Object object : selectedValues) {
					if (object instanceof String) {
						final String str = (String) object;
						selection.add(str);
					}
				}
				switch (selectionType) {
				case Unmodifiable:
					// Do nothing, cannot change.
					break;
				case Single:
					if (selection.size() == 1) {
						model.setSelection(selection);
					}
					break;
				case MultipleAtLeastOne:
					if (selection.size() >= 1) {
						model.setSelection(selection);
					}
					break;
				case MultipleOrNone:
					model.setSelection(selection);
					break;
				default:
					throw new UnsupportedOperationException(
							"Not supported selection type: " + selectionType);
				}
				updateComponent();
			}

		});
		getPanel().add(list);
	}

	private final JList list = new JList(new DefaultListModel());

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.knime.core.node.defaultnodesettings.DialogComponent#setEnabledComponents
	 * (boolean)
	 */
	@Override
	protected void setEnabledComponents(final boolean enabled) {
		list.setEnabled(enabled);
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
		final Set<String> selection = model.getSelection();
		final List<String> elements = getElements(list.getModel());
		final DefaultListModel listModel = (DefaultListModel) list.getModel();
		if (!elements.equals(possibleValues)) {
			listModel.removeAllElements();
			for (final String value : possibleValues) {
				listModel.addElement(value);
			}
		}
		final int[] indices = new int[selection.size()];
		int index = 0;
		int i = 0;
		for (final String value : possibleValues) {
			if (selection.contains(value)) {
				indices[index++] = i;
			}
			++i;
		}
		assert index == selection.size();
		if (!Arrays.equals(indices, list.getSelectedIndices())) {
			list.setSelectedIndices(indices);
		}
	}

	/**
	 * @param model
	 *            A {@link ListModel}.
	 * @return The elements in the {@link ListModel}.
	 */
	private static List<String> getElements(final ListModel model) {
		final List<String> ret = new ArrayList<String>(model.getSize());
		for (int i = 0; i < model.getSize(); ++i) {
			ret.add((String) model.getElementAt(i));
		}
		return ret;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ie.tcd.imm.hits.knime.view.impl.AbstractVariableControl#getType()
	 */
	@Override
	public ControlTypes getType() {
		return ControlTypes.List;
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
		result = prime * result + (list == null ? 0 : list.hashCode());
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
		final ListControl<?, ?> other = (ListControl<?, ?>) obj;
		if (list == null) {
			if (other.list != null) {
				return false;
			}
		} else if (list != other.list) {
			return false;
		}
		return true;
	}

	@Override
	protected void notifyChange(final MouseListener listener,
			final Change change) {
		switch (change) {
		case add:
			list.addMouseListener(listener);
			break;
		case remove:
			list.removeMouseListener(listener);
			break;
		default:
			throw new IllegalArgumentException("Not supported change type: "
					+ change);
		}
		super.notifyChange(listener, change);
	}
}

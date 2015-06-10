/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package ie.tcd.imm.hits.view.impl;

import ie.tcd.imm.hits.knime.view.SplitType;
import ie.tcd.imm.hits.knime.view.impl.ControlsHandlerAbstractFactory;
import ie.tcd.imm.hits.knime.view.impl.SettingsModelListSelection;
import ie.tcd.imm.hits.util.Pair;
import ie.tcd.imm.hits.util.select.OptionalNamedSelector;
import ie.tcd.imm.hits.util.swing.SelectionType;
import ie.tcd.imm.hits.util.swing.VariableControl;
import ie.tcd.imm.hits.util.swing.VariableControl.ControlTypes;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseListener;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.node.defaultnodesettings.SettingsModel;

import edu.umd.cs.findbugs.annotations.DefaultAnnotation;

/**
 * This factory helps to handle {@link VariableControl}s.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 * @param <Model>
 *            The actual model used for the controls.
 */
@DefaultAnnotation( { Nonnull.class, CheckReturnValue.class })
@NotThreadSafe
public class ControlsHandlerFactory<Model> extends
		ControlsHandlerAbstractFactory<Model, OptionalNamedSelector<Model>> {

	/**
	 * Constructs a {@link ControlsHandlerAbstractFactory}.
	 */
	public ControlsHandlerFactory() {
		super();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected VariableControl<SettingsModel, Model, OptionalNamedSelector<Model>> createNewControl(
			final OptionalNamedSelector<Model> model,
			final ControlTypes controlType, final SelectionType selectionType,
			final SplitType split) {
		final Map<Integer, Model> valueMapping = model.getValueMapping();
		final List<String> vals = new LinkedList<String>();
		for (final Model val : valueMapping.values()) {
			vals.add(val.toString());
		}
		final Set<Integer> selections = new TreeSet<Integer>(model
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
			model.selectSingle(sel);
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
				model.getName(), vals, selected);
		final VariableControl<SettingsModel, Model, OptionalNamedSelector<Model>> control = createControl(
				model, controlType, settingsModelListSelection, selectionType,
				split);
		model.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				control.getModelChangeListener().stateChanged(
						new ChangeEvent(e.getSource()));
			}
		});
		return control;
	}

	@Override
	protected VariableControl<SettingsModel, Model, OptionalNamedSelector<Model>> createControl(
			final OptionalNamedSelector<Model> domainModel,
			final ControlTypes controlType,
			final SettingsModelListSelection settingsModelListSelection,
			final ChangeListener changeListener, final SelectionType selection,
			final SplitType split) {
		final VariableControl<SettingsModel, Model, OptionalNamedSelector<Model>> control = super
				.createControl(domainModel, controlType,
						settingsModelListSelection, changeListener, selection,
						split);
		for (final MouseListener l : domainModel.getControlListeners()) {
			control.addControlListener(l);
		}
		return control;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean exchangeControls(
			final VariableControl<SettingsModel, Model, OptionalNamedSelector<Model>> first,
			final VariableControl<SettingsModel, Model, OptionalNamedSelector<Model>> second) {
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
		// final ControlsHandler<SettingsModel, Model, NamedSelector<Model>>
		// controlsHandler = first
		// .getControlsHandler();
		/* controlsHandler. */deregister(first.getDomainModel(), firstPos
				.getLeft());
		deregister(second.getDomainModel(), secondPos.getLeft());
		register(first.getDomainModel(), secondPos.getLeft(), secondPos
				.getRight(), second.getType());
		register(second.getDomainModel(), firstPos.getLeft(), firstPos
				.getRight(), first.getType());
		return true;
	}

}

/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package ie.tcd.imm.hits.view.impl;

import ie.tcd.imm.hits.knime.view.SplitType;
import ie.tcd.imm.hits.knime.view.impl.ControlsHandlerAbstractFactory;
import ie.tcd.imm.hits.knime.view.impl.SettingsModelListSelection;
import ie.tcd.imm.hits.util.NamedSelector;
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
import java.util.TreeSet;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;

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
		ControlsHandlerAbstractFactory<Model, NamedSelector<Model>> {

	/**
	 * Constructs a {@link ControlsHandlerAbstractFactory}.
	 */
	public ControlsHandlerFactory() {
		super();
	}

	// /**
	// * @param controlType
	// * The preferred {@link ControlTypes}.
	// * @param settingsModelListSelection
	// * The used model.
	// * @param changeListener
	// * The associated {@link ChangeListener}.
	// * @param selection
	// * The {@link SelectionType selection} mode.
	// * @param split
	// * The {@link SplitType} of the new control. Only needed for the
	// * popup menu.
	// * @return The {@link VariableControl} with the desired parameters.
	// */
	// @Override
	// protected VariableControl<SettingsModel, Model, Selector<Model>>
	// createControl(
	// final Selector<Model> domainModel, final ControlTypes controlType,
	// final SettingsModelListSelection settingsModelListSelection,
	// final ChangeListener changeListener, final SelectionType selection,
	// final SplitType split) {
	// final VariableControl<SettingsModel, Model, Selector<Model>> ret;
	// switch (controlType) {
	// case Buttons:
	// ret = new WellSelectionWidget<Model, Selector<Model>>(Format._96,
	// settingsModelListSelection, selection, this,
	// changeListener, domainModel);
	// break;
	// case List:
	// ret = new ListControl<Model, Selector<Model>>(
	// settingsModelListSelection, selection, this,
	// changeListener, domainModel);
	// break;
	// case ComboBox:
	// ret = new ComboBoxControl<Model, Selector<Model>>(
	// settingsModelListSelection, selection, this,
	// changeListener, domainModel);
	// break;
	// case Invisible:
	// throw new UnsupportedOperationException("Not supported yet.");
	// case Slider:
	// ret = new SliderControl<Model, Selector<Model>>(
	// settingsModelListSelection, selection, this,
	// changeListener, domainModel);
	// break;
	// case RadioButton:
	// throw new UnsupportedOperationException("Not supported yet.");
	// // if (!cache.containsKey(slider)) {
	// // cache.put(slider, new RadioControl(settingsModelListSelection,
	// // SelectionType.Single));
	// // }
	// // return cache.get(slider);
	// case Tab:
	// throw new UnsupportedOperationException("Not supported yet.");
	// case ScrollBarHorisontal:
	// throw new UnsupportedOperationException("Not supported yet.");
	// case ScrollBarVertical:
	// throw new UnsupportedOperationException("Not supported yet.");
	// default:
	// throw new UnsupportedOperationException("Not supported yet: "
	// + controlType);
	// }
	// final PopupMenu<SettingsModel, Model, Selector<Model>> popupMenu = new
	// PopupMenu<SettingsModel, Model, Selector<Model>>(
	// ret, split, this);
	// ((AbstractVariableControl<?, ?>) ret).getPanel().addMouseListener(
	// popupMenu);
	// ret.getView().addMouseListener(popupMenu);
	// for (final Component comp : ret.getView().getComponents()) {
	// comp.addMouseListener(popupMenu);
	// }
	// return ret;
	// }

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected VariableControl<SettingsModel, Model, NamedSelector<Model>> createNewControl(
			final NamedSelector<Model> model, final ControlTypes controlType,
			final SelectionType selectionType, final SplitType split) {
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
		final VariableControl<SettingsModel, Model, NamedSelector<Model>> control = createControl(
				model, controlType, settingsModelListSelection, selectionType,
				split);
		return control;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean exchangeControls(
			final VariableControl<SettingsModel, Model, NamedSelector<Model>> first,
			final VariableControl<SettingsModel, Model, NamedSelector<Model>> second) {
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

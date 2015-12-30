/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package com.mind_era.knime.hits.view.impl;

import java.util.Collections;

import javax.swing.JPanel;

import org.fest.swing.annotation.GUITest;
import org.fest.swing.fixture.JComboBoxFixture;
import org.fest.swing.fixture.JListFixture;
import org.fest.swing.fixture.JSliderFixture;
import org.fest.swing.fixture.JToggleButtonFixture;
import org.junit.Assert;
import org.junit.Test;
import org.knime.core.node.defaultnodesettings.SettingsModel;

import com.mind_era.knime.common.util.swing.SelectionType;
import com.mind_era.knime.common.util.swing.VariableControl;
import com.mind_era.knime.common.util.swing.VariableControl.ControlTypes;
import com.mind_era.knime.common.view.SplitType;
import com.mind_era.knime.common.view.impl.ButtonsControl;
import com.mind_era.knime.common.view.impl.ComboBoxControl;
import com.mind_era.knime.common.view.impl.ListControl;
import com.mind_era.knime.common.view.impl.SliderControl;
import com.mind_era.knime.hits.view.impl.ControlsHandlerKNIMEFactory;

/**
 * This class tests the {@link VariableControl} implementations.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
@GUITest
@SuppressWarnings("deprecation")
public class ControlTests extends AbstractControlTest {
	/**
	 * 
	 */
	public ControlTests() {
		super();
	}

	/**
	 * Tests {@link ButtonsControl}.
	 */
	@Test
	// (enabled = false)
	// (timeout = 3000)
	@GUITest
	public void buttons() {
		final JPanel view1 = (JPanel) controlsHandler.getComponent(slider1,
				ControlTypes.Buttons, SelectionType.MultipleAtLeastOne,
				SplitType.PrimarySplit).getView();
		final JPanel view2 = (JPanel) controlsHandler.getComponent(slider2,
				ControlTypes.Buttons, SelectionType.MultipleAtLeastOne,
				SplitType.SeconderSplit).getView();
		addViews(view1, view2);

		final JToggleButtonFixture toggleButton0 = window.toggleButton(VALUE_0);
		final JToggleButtonFixture toggleButtonXX = window
				.toggleButton(VALUE_1);
		toggleButtonXX.requireEnabled();
		toggleButtonXX.requireSelected();
		final JToggleButtonFixture toggleButtonXY = window
				.toggleButton(VALUE_2);
		toggleButtonXY.requireEnabled();
		toggleButtonXY.requireSelected();
		toggleButtonXY.click();
		Assert.assertEquals(slider2.getSelections(), Collections
				.singleton(Integer.valueOf(1)));
		window.toggleButton(VALUE_1).click();
		Assert.assertEquals(slider2.getSelections(), Collections
				.singleton(Integer.valueOf(1)));
		window.toggleButton(VALUE_2).click();
		window.toggleButton(VALUE_1).click();
		Assert.assertEquals(slider2.getSelections(), Collections
				.singleton(Integer.valueOf(2)));
		toggleButton0.requireEnabled();
		toggleButton0.requireSelected();
		toggleButton0.click();
		toggleButton0.requireSelected();
	}

	/**
	 * Tests {@link ListControl}.
	 */
	@Test(timeout = 1000)
	@GUITest
	public void list() {
		final VariableControl<? extends SettingsModel, ?, ?> component0 = controlsHandler
				.getComponent(slider1, ControlTypes.List,
						SelectionType.MultipleAtLeastOne,
						SplitType.PrimarySplit);
		final JPanel view1 = (JPanel) component0.getView();
		final VariableControl<?, ?, ?> component1 = controlsHandler
				.getComponent(slider2, ControlTypes.List,
						SelectionType.MultipleAtLeastOne,
						SplitType.SeconderSplit);
		final JPanel view2 = (JPanel) component1.getView();
		addViews(view1, view2);
		final JListFixture list = window.list(ControlsHandlerKNIMEFactory
				.createName(slider2));
		list.requireEnabled();
		Assert.assertEquals(new String[] { VALUE_1, VALUE_2 }, list.contents());
		list.requireSelectedItems(VALUE_1, VALUE_2);
	}

	/**
	 * Tests {@link ComboBoxControl}.
	 */
	@Test(timeout = 2000)
	@GUITest
	public void comboBox() {
		final VariableControl<? extends SettingsModel, ?, ?> component0 = controlsHandler
				.getComponent(slider1, ControlTypes.ComboBox,
						SelectionType.Single, SplitType.PrimarySplit);
		final JPanel view1 = (JPanel) component0.getView();
		final VariableControl<?, ?, ?> component1 = controlsHandler
				.getComponent(slider2, ControlTypes.ComboBox,
						SelectionType.Single, SplitType.SingleSelect);
		final JPanel view2 = (JPanel) component1.getView();
		addViews(view1, view2);
		final JComboBoxFixture comboBox = window
				.comboBox(ControlsHandlerKNIMEFactory.createName(slider1));
		comboBox.requireEnabled();
		comboBox.requireNotEditable();
		comboBox.requireSelection(VALUE_0);
	}

	/**
	 * Tests {@link SliderControl}.
	 */
	@Test(timeout = 3000)
	@GUITest
	public void slider() {
		final VariableControl<? extends SettingsModel, ?, ?> component0 = controlsHandler
				.getComponent(slider1, ControlTypes.Slider,
						SelectionType.Single, SplitType.PrimarySplit);
		final JPanel view1 = (JPanel) component0.getView();
		final VariableControl<?, ?, ?> component1 = controlsHandler
				.getComponent(slider2, ControlTypes.Slider,
						SelectionType.Single, SplitType.SingleSelect);
		final JPanel view2 = (JPanel) component1.getView();
		addViews(view1, view2);
		final JSliderFixture slider = window.slider(ControlsHandlerKNIMEFactory
				.createName(slider1));
		slider.requireEnabled();
		Assert.assertEquals(slider.target.getValue(), 1);
	}
}

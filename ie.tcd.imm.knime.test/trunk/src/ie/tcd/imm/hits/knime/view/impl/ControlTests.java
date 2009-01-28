/**
 * 
 */
package ie.tcd.imm.hits.knime.view.impl;

import ie.tcd.imm.hits.util.swing.VariableControl;
import ie.tcd.imm.hits.util.swing.VariableControl.ControlTypes;

import java.util.Collections;

import javax.swing.JPanel;

import org.fest.swing.annotation.GUITest;
import org.fest.swing.fixture.JComboBoxFixture;
import org.fest.swing.fixture.JListFixture;
import org.fest.swing.fixture.JSliderFixture;
import org.fest.swing.fixture.JToggleButtonFixture;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * This class tests the {@link VariableControl} implementations.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
@GUITest
@Test
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
	// (timeOut = 3000)
	@GUITest
	public void buttons() {
		final JPanel view1 = (JPanel) controlsHandler.getComponent(slider1,
				ControlTypes.Buttons).getView();
		final JPanel view2 = (JPanel) controlsHandler.getComponent(slider2,
				ControlTypes.Buttons).getView();
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
	@Test(timeOut = 1000)
	@GUITest
	public void list() {
		final VariableControl<? extends SettingsModel> component0 = controlsHandler
				.getComponent(slider1, ControlTypes.List);
		final JPanel view1 = (JPanel) component0.getView();
		final VariableControl<?> component1 = controlsHandler.getComponent(
				slider2, ControlTypes.List);
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
	@Test(timeOut = 1000)
	@GUITest
	public void comboBox() {
		final VariableControl<? extends SettingsModel> component0 = controlsHandler
				.getComponent(slider1, ControlTypes.ComboBox);
		final JPanel view1 = (JPanel) component0.getView();
		final VariableControl<?> component1 = controlsHandler.getComponent(
				slider2, ControlTypes.ComboBox);
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
	@Test(timeOut = 1000)
	@GUITest
	public void slider() {
		final VariableControl<? extends SettingsModel> component0 = controlsHandler
				.getComponent(slider1, ControlTypes.Slider);
		final JPanel view1 = (JPanel) component0.getView();
		final VariableControl<?> component1 = controlsHandler.getComponent(
				slider2, ControlTypes.Slider);
		final JPanel view2 = (JPanel) component1.getView();
		addViews(view1, view2);
		final JSliderFixture slider = window.slider(ControlsHandlerKNIMEFactory
				.createName(slider1));
		slider.requireEnabled();
		Assert.assertEquals(slider.target.getValue(), 1);
	}
}

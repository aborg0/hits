/**
 * 
 */
package ie.tcd.imm.hits.knime.view.impl;

import ie.tcd.imm.hits.knime.view.ControlsHandler;
import ie.tcd.imm.hits.knime.view.heatmap.HeatmapNodeModel.StatTypes;
import ie.tcd.imm.hits.knime.view.heatmap.SliderModel.SliderFactory;
import ie.tcd.imm.hits.knime.view.heatmap.SliderModel.Type;
import ie.tcd.imm.hits.knime.view.heatmap.ViewModel.ParameterModel;
import ie.tcd.imm.hits.util.Pair;
import ie.tcd.imm.hits.util.swing.VariableControl.ControlTypes;

import java.util.Collections;

import javax.swing.JPanel;

import org.fest.swing.annotation.GUITest;
import org.fest.swing.core.BasicRobot;
import org.fest.swing.fixture.JPanelFixture;
import org.fest.swing.fixture.JToggleButtonFixture;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

/**
 * This class tests the {@link ButtonsControl}.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
@GUITest
@Test
public class ButtonsControlTests {

	private JPanelFixture panelFixture;

	@BeforeTest
	public void createWindow() {
		final ControlsHandler<? extends SettingsModel> controlsHandler = new ControlsHandlerKNIMEFactory();
		final ParameterModel paramModel = new ParameterModel("",
				StatTypes.metaStatType, null, Collections.<String> emptyList(),
				Collections.<String> emptyList());
		panelFixture = new JPanelFixture(BasicRobot.robotWithNewAwtHierarchy(),
				(JPanel) controlsHandler.getComponent(
						new SliderFactory().get(
								Type.Splitter,
								Collections.singletonList(paramModel),
								Collections.singletonMap(Integer.valueOf(1),
										new Pair<ParameterModel, Object>(
												paramModel, "xx"))).iterator()
								.next(), ControlTypes.Buttons).getView());
	}

	@Test
	public void detach() {
		final JToggleButtonFixture toggleButton = panelFixture.toggleButton();
		Assert.assertNotNull(toggleButton);
	}
}

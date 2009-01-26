/**
 * 
 */
package ie.tcd.imm.hits.knime.view.impl;

import ie.tcd.imm.hits.knime.view.ControlsHandler;
import ie.tcd.imm.hits.knime.view.heatmap.SliderModel;
import ie.tcd.imm.hits.knime.view.heatmap.HeatmapNodeModel.StatTypes;
import ie.tcd.imm.hits.knime.view.heatmap.SliderModel.SliderFactory;
import ie.tcd.imm.hits.knime.view.heatmap.SliderModel.Type;
import ie.tcd.imm.hits.knime.view.heatmap.ViewModel.ParameterModel;
import ie.tcd.imm.hits.util.Pair;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.WindowConstants;
import javax.swing.border.TitledBorder;

import org.fest.swing.annotation.GUITest;
import org.fest.swing.core.BasicRobot;
import org.fest.swing.core.Robot;
import org.fest.swing.fixture.FrameFixture;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import edu.umd.cs.findbugs.annotations.DefaultAnnotation;

/**
 * This test creates a sample and populates with some components from
 * {@link ControlsHandlerKNIMEFactory}.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
@DefaultAnnotation( { Nonnull.class, CheckReturnValue.class })
public class MockTest {
	private JFrame frame;
	private ControlsHandler<SettingsModel> controlsHandler;

	private static final String UPPER_RIGHT = "upper right";
	private static final String UPPER_LEFT = "upper left";
	private static final String LOWER = "lower";
	private static final String LOWER_RIGHT = "lower right";
	private static final String LOWER_CENTER = "lower center";

	private static final ParameterModel plate = new ParameterModel("plate",
			StatTypes.plate, null, Collections.singletonList("plate"), Arrays
					.asList("1", "2", "3", "4"));

	private static final ParameterModel replicate = new ParameterModel(
			"replicate", StatTypes.plate, null, Collections
					.singletonList("replicate"), Arrays.asList("1", "2", "3"));

	private static final ParameterModel experiment = new ParameterModel(
			"experiment", StatTypes.experimentName, null, Collections
					.singletonList("experiment"), Arrays.asList("Test1",
					"Test2"));
	private static final ParameterModel statistics = new ParameterModel(
			"statistics", StatTypes.metaStatType, null, Collections
					.singletonList("stat"), Arrays.asList(StatTypes.score
					.name(), StatTypes.median.name(), StatTypes.raw.name()));
	private static final ParameterModel normalisation = new ParameterModel(
			"normalisation", StatTypes.normalisation, null, Collections
					.singletonList("norm"), Arrays.asList("zscore", "POC"));
	private static final ParameterModel parameter = new ParameterModel(
			"parameters", StatTypes.parameter, null, Collections
					.singletonList("parameters"), Arrays.asList("param1",
					"param2", "param3"));

	private SliderModel experimentSlider;
	private SliderModel statsSlider;
	private SliderModel normSlider;
	private SliderModel paramSlider;
	private SliderModel plateSlider;
	private SliderModel replicateSlider;

	private FrameFixture window;

	@BeforeMethod
	public void createSampleApp() {
		final Robot robot = BasicRobot.robotWithNewAwtHierarchy();
		final JPanel mainPanel = new JPanel();
		mainPanel.setName("mainPanel");
		frame = new JFrame("Test");
		frame.getContentPane().add(mainPanel);
		final JSplitPane upDownSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		mainPanel.add(upDownSplit);
		final JTabbedPane downComponent = new JTabbedPane();
		upDownSplit.setRightComponent(downComponent);
		final JSplitPane leftRightSplit = new JSplitPane(
				JSplitPane.HORIZONTAL_SPLIT);
		upDownSplit.setLeftComponent(leftRightSplit);
		final JPanel rightPanel = new JPanel();
		rightPanel.setName(UPPER_RIGHT);
		leftRightSplit.setRightComponent(rightPanel);
		final JPanel leftPanel = new JPanel();
		leftPanel.setLayout(new BorderLayout());
		final JPanel leftUpperPanel = new JPanel();
		leftUpperPanel.setName(UPPER_LEFT);
		leftPanel.add(leftUpperPanel, BorderLayout.NORTH);
		final JPanel leftCenterPanel = new JPanel();
		leftCenterPanel.setPreferredSize(new Dimension(400, 400));
		final JScrollPane leftCenterArea = new JScrollPane(leftCenterPanel);
		leftCenterArea.setPreferredSize(new Dimension(400, 400));
		leftPanel.add(leftCenterArea, BorderLayout.CENTER);
		leftRightSplit.setLeftComponent(leftPanel);
		final JPanel lowerPanel = new JPanel();
		lowerPanel.setLayout(new BorderLayout());
		final JPanel lowerRightPanel = new JPanel();
		lowerRightPanel.setBorder(new TitledBorder("lower right"));
		lowerRightPanel.setName(LOWER_RIGHT);
		lowerPanel.add(lowerRightPanel, BorderLayout.EAST);
		final JPanel lowerCenterPanel = new JPanel();
		lowerCenterPanel.setName(LOWER_CENTER);
		lowerCenterPanel.setBorder(new TitledBorder("lower center"));
		lowerPanel.add(lowerCenterPanel, BorderLayout.CENTER);
		final JScrollPane lowerArea = new JScrollPane(lowerPanel);
		downComponent.addTab("Sth", lowerArea);
		createSliders(new SliderFactory());
		controlsHandler = new ControlsHandlerKNIMEFactory();
		controlsHandler.setContainer(leftUpperPanel, Type.Selector, UPPER_LEFT);
		controlsHandler.setContainer(lowerRightPanel, Type.Splitter, LOWER);
		controlsHandler.setContainer(lowerCenterPanel, Type.Hidden, LOWER);
		controlsHandler.setContainer(rightPanel, Type.Hidden, UPPER_RIGHT);
		controlsHandler.register(plateSlider, Type.Selector, null);
		controlsHandler.register(replicateSlider, Type.Selector, UPPER_LEFT);
		controlsHandler.register(experimentSlider, Type.Hidden, UPPER_RIGHT);
		controlsHandler.register(statsSlider, Type.Hidden, null);
		controlsHandler.register(normSlider, Type.Hidden, LOWER);
		controlsHandler.register(paramSlider, Type.Splitter, null);

		window = new FrameFixture(robot, frame);
		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		frame.setSize(600, 600);
		frame.setVisible(true);
	}

	/**
	 * @param sliderFactory
	 * 
	 */
	private void createSliders(final SliderFactory sliderFactory) {
		plateSlider = sliderFactory.get(Type.Selector,
				Collections.singletonList(plate), createValueMapping(plate))
				.iterator().next();
		replicateSlider = sliderFactory.get(Type.Selector,
				Collections.singletonList(replicate),
				createValueMapping(replicate)).iterator().next();
		experimentSlider = sliderFactory.get(Type.Hidden,
				Collections.singletonList(experiment),
				createValueMapping(experiment)).iterator().next();
		statsSlider = sliderFactory.get(Type.Hidden,
				Collections.singletonList(statistics),
				createValueMapping(statistics)).iterator().next();
		normSlider = sliderFactory.get(Type.Hidden,
				Collections.singletonList(normalisation),
				createValueMapping(normalisation)).iterator().next();
		paramSlider = sliderFactory.get(Type.Splitter,
				Collections.singletonList(parameter),
				createValueMapping(parameter)).iterator().next();
	}

	@GUITest
	@Test
	public void testInitialArrangement() {
		window.panel(LOWER_RIGHT).toolBar();
		// window.panel(UPPER_LEFT).slider("replicate");
		// window.panel(UPPER_LEFT).slider("plate");
		window.panel(UPPER_RIGHT).comboBox();
		window.panel(LOWER_RIGHT).toggleButton("param1");
		window.panel(LOWER_RIGHT).toggleButton("param2");
		window.panel(LOWER_RIGHT).toggleButton("param3");
		// window.panel(LOWER_CENTER).comboBox("statistics");
		// window.panel(LOWER_CENTER).comboBox("normalisation");
	}

	/**
	 * @param parameterModel
	 * @return
	 */
	private Map<Integer, Pair<ParameterModel, Object>> createValueMapping(
			final ParameterModel parameterModel) {
		final Map<Integer, Pair<ParameterModel, Object>> ret = new TreeMap<Integer, Pair<ParameterModel, Object>>();
		int i = 1;
		for (final String val : parameterModel.getColumnValues()) {
			ret.put(Integer.valueOf(i++), new Pair<ParameterModel, Object>(
					parameterModel, val));
		}
		return ret;
	}
}

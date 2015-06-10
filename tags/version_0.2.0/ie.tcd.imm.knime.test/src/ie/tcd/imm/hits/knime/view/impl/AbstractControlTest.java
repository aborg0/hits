/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package ie.tcd.imm.hits.knime.view.impl;

import ie.tcd.imm.hits.knime.view.ControlsHandler;
import ie.tcd.imm.hits.knime.view.heatmap.SliderModel;
import ie.tcd.imm.hits.knime.view.heatmap.HeatmapNodeModel.StatTypes;
import ie.tcd.imm.hits.knime.view.heatmap.SliderModel.SliderFactory;
import ie.tcd.imm.hits.knime.view.heatmap.SliderModel.Type;
import ie.tcd.imm.hits.knime.view.heatmap.ViewModel.ParameterModel;
import ie.tcd.imm.hits.util.Pair;
import ie.tcd.imm.hits.util.swing.VariableControl;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import java.awt.FlowLayout;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.swing.JFrame;
import javax.swing.JPanel;

import org.fest.swing.core.BasicRobot;
import org.fest.swing.core.Robot;
import org.fest.swing.fixture.FrameFixture;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

import edu.umd.cs.findbugs.annotations.DefaultAnnotation;

/**
 * A common superclass for {@link VariableControl} tests.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
@DefaultAnnotation( { Nonnull.class, CheckReturnValue.class })
public abstract class AbstractControlTest {

	/** A sample value. Associated with {@link #slider1}. */
	protected static final String VALUE_0 = "x";
	/** A sample value. Associated with {@link #slider2}. */
	protected static final String VALUE_1 = "xx";
	/** A sample value. Associated with {@link #slider2}. */
	protected static final String VALUE_2 = "xy";
	/** The {@link FrameFixture} used for tests. */
	protected FrameFixture window;
	/** A sample {@link SliderModel} with one value. */
	protected SliderModel slider1;
	/** A sample {@link SliderModel} with two values. */
	protected SliderModel slider2;
	private JFrame frame;
	/** The {@link ControlsHandler} to create the {@link VariableControl}s. */
	protected ControlsHandler<? extends SettingsModel> controlsHandler;
	private Robot robot;

	/**
	 * 
	 */
	public AbstractControlTest() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * Creates the frame and the sliders, but does not add anything to it. Also
	 * creates the {@link SliderModel}s.
	 */
	@BeforeMethod
	public void createWindow() {
		robot = BasicRobot.robotWithNewAwtHierarchy();
		frame = new JFrame("Test");
		controlsHandler = new ControlsHandlerKNIMEFactory();
		final ParameterModel paramModel1 = new ParameterModel("first",
				StatTypes.metaStatType, null, Collections.<String> emptyList(),
				Collections.<String> emptyList());
		final ParameterModel paramModel2 = new ParameterModel("second",
				StatTypes.metaStatType, null, Collections.<String> emptyList(),
				Collections.<String> emptyList());
		final Map<Integer, Pair<ParameterModel, Object>> valueMapping = new HashMap<Integer, Pair<ParameterModel, Object>>();
		valueMapping.put(Integer.valueOf(1), new Pair<ParameterModel, Object>(
				paramModel2, VALUE_1));
		valueMapping.put(Integer.valueOf(2), new Pair<ParameterModel, Object>(
				paramModel2, VALUE_2));
		slider1 = new SliderFactory()
				.get(
						Type.Splitter,
						Collections.singletonList(paramModel1),
						Collections.singletonMap(Integer.valueOf(1),
								new Pair<ParameterModel, Object>(paramModel1,
										VALUE_0))).iterator().next();
		slider2 = new SliderFactory().get(Type.Splitter,
				Collections.singletonList(paramModel2), valueMapping)
				.iterator().next();
	}

	/**
	 * Adds the panels to the test {@link JFrame}.
	 * 
	 * @param panels
	 *            Some {@link JPanel}s to add.
	 */
	protected void addViews(final JPanel... panels) {
		final JPanel p = new JPanel();
		p.setLayout(new FlowLayout());
		frame.getContentPane().add(p);
		for (final JPanel panel : panels) {
			p.add(panel);
		}
		window = new FrameFixture(robot, frame);
		frame.setSize(200, 200);
		frame.setVisible(true);
		window.focus();
	}

	/**
	 * Clears the {@link JFrame}.
	 */
	@AfterMethod
	public void clearFrame() {
		window.cleanUp();
	}
}

/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package ie.tcd.imm.hits.view.util;

import ie.tcd.imm.hits.common.Format;
import ie.tcd.imm.hits.knime.view.impl.SettingsModelListSelection;
import ie.tcd.imm.hits.util.Selector;
import ie.tcd.imm.hits.util.swing.SelectionType;

import java.util.Collections;

import javax.swing.JFrame;
import javax.swing.WindowConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.testng.annotations.Test;

/**
 * TODO Javadoc!
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
public class WellSelections {
	@Test
	public void open() throws InterruptedException {
		final WellSelectionWidget wellSelectionWidget = new WellSelectionWidget<Object, Selector<Object>>(
				Format._96, new SettingsModelListSelection("", Collections
						.singletonList(""), Collections.singletonList("")),
				SelectionType.Single, null, new ChangeListener() {

					@Override
					public void stateChanged(final ChangeEvent e) {
						System.out.println(e);
					}
				}, new Selector<Object>(Collections.singletonMap(1,
						new Object()), Collections.singleton(1)));
		final JFrame jFrame = new JFrame();
		jFrame.getContentPane().add(wellSelectionWidget.getView());
		jFrame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		jFrame.setSize(800, 600);
		jFrame.setVisible(true);
		Thread.sleep(10000);
	}

}

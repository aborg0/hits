/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package com.mind_era.knime.util.product;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

import com.mind_era.knime.common.util.HiLite;
import com.mind_era.knime.common.util.TransformingNodeModel;

/**
 * <code>NodeDialog</code> for the "DirectProduct" Node. This node takes input
 * tables and creates a direct product of the rows.
 * 
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more
 * complex dialog please derive directly from
 * {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
public class DirectProductNodeDialog extends DefaultNodeSettingsPane {

	/**
	 * New pane for configuring the DirectProduct node.
	 */
	protected DirectProductNodeDialog() {
		super();
		addDialogComponent(new DialogComponentStringSelection(
				new SettingsModelString(TransformingNodeModel.CFGKEY_HILITE,
						TransformingNodeModel.DEFAULT_HILITE.getDisplayText()),
				"HiLite Strategy: ", HiLite
						.asDisplayTexts(TransformingNodeModel.supportedHiLites)));
	}
}

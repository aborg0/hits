/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package ie.tcd.imm.knime.util.product;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;

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
	}
}

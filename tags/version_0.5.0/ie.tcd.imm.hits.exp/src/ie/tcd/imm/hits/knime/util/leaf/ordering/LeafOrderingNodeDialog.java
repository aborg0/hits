/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package ie.tcd.imm.hits.knime.util.leaf.ordering;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelColumnName;
import org.knime.distmatrix.type.DistanceVectorDataValue;

/**
 * <code>NodeDialog</code> for the "LeafOrdering" Node. Reorders a tree to an
 * optimal ordering. See <tt>New Hierarchical Clustering</tt> node.
 * 
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more
 * complex dialog please derive directly from
 * {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author <a href="bakosg@tcd.ie">Gabor Bakos</a>
 */
public class LeafOrderingNodeDialog extends DefaultNodeSettingsPane {

	/**
	 * New pane for configuring the LeafOrdering node.
	 */
	@SuppressWarnings("unchecked")
	protected LeafOrderingNodeDialog() {
		final DialogComponentColumnNameSelection distanceColumn = new DialogComponentColumnNameSelection(
				new SettingsModelColumnName(
						LeafOrderingNodeModel.CFGKEY_DISTANCE_COLUMN,
						LeafOrderingNodeModel.DEFAULT_DISTANCE_COLUMN),
				"Distance column", 1, DistanceVectorDataValue.class);
		addDialogComponent(distanceColumn);
	}
}

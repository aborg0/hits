package ie.tcd.imm.hits.knime.view.heatmap;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;

/**
 * <code>NodeDialog</code> for the "Heatmap" Node. Shows the heatmap of the
 * plates.
 * 
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more
 * complex dialog please derive directly from
 * {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author TCD
 */
public class HeatmapNodeDialog extends DefaultNodeSettingsPane {

	/**
	 * New pane for configuring Heatmap node dialog. This is just a suggestion
	 * to demonstrate possible default dialog components.
	 */
	protected HeatmapNodeDialog() {
		super();

	}
}

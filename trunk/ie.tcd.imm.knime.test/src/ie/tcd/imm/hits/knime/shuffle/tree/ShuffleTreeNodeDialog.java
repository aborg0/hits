package ie.tcd.imm.hits.knime.shuffle.tree;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;

/**
 * <code>NodeDialog</code> for the "ShuffleTree" Node. Randomly change the order
 * of branches in a tree.
 * 
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more
 * complex dialog please derive directly from
 * {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
public class ShuffleTreeNodeDialog extends DefaultNodeSettingsPane {

	/**
	 * New pane for configuring the ShuffleTree node.
	 */
	protected ShuffleTreeNodeDialog() {
		super();
	}
}

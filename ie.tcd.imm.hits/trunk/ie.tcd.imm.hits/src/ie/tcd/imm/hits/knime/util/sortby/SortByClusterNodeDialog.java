package ie.tcd.imm.hits.knime.util.sortby;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;

import edu.umd.cs.findbugs.annotations.DefaultAnnotation;

/**
 * <code>NodeDialog</code> for the "SortByCluster" Node. Sorts the data by the
 * order defined by the clustering.
 * 
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more
 * complex dialog please derive directly from
 * {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author <a href="bakosg@tcd.ie">Gabor Bakos</a>
 */
@DefaultAnnotation( { Nonnull.class, CheckReturnValue.class })
public class SortByClusterNodeDialog extends DefaultNodeSettingsPane {

	/**
	 * New pane for configuring the SortByCluster node.
	 */
	protected SortByClusterNodeDialog() {
		super();
		addDialogComponent(new DialogComponentBoolean(new SettingsModelBoolean(
				SortByClusterNodeModel.CFGKEY_ASCENDING,
				SortByClusterNodeModel.DEFAULT_ASCENDING), "Ascending order?"));
	}
}

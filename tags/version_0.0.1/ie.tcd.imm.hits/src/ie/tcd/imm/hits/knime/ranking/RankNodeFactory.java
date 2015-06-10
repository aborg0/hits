package ie.tcd.imm.hits.knime.ranking;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "Rank" Node. This node ranks the results
 * for each parameter and for each normalisation methods with selectable neutral
 * values and the direction of the upregulation.<br>
 * The downregulated values has negative rankings, the upregulated has positive
 * ones. If present it uses the (final) well annotation information of the
 * wells, and only the sample wells are ranked. If the well annotations are not
 * present the ranking is based on all wells with non-missing values.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
public class RankNodeFactory extends NodeFactory {

	/**
	 * {@inheritDoc}
	 */
	@Override
	public NodeModel createNodeModel() {
		return new RankNodeModel();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getNrNodeViews() {
		return 0;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public NodeView createNodeView(final int viewIndex,
			final NodeModel nodeModel) {
		throw new IndexOutOfBoundsException("No views: " + viewIndex);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean hasDialog() {
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public NodeDialogPane createNodeDialogPane() {
		return new RankNodeDialog();
	}

}

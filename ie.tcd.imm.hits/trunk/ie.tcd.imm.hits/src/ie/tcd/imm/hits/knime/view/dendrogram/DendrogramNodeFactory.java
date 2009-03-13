package ie.tcd.imm.hits.knime.view.dendrogram;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "Dendrogram" Node. Allows to create
 * dendrogram with a heatmap of parameters.
 * 
 * @author TCD
 */
public class DendrogramNodeFactory extends NodeFactory<DendrogramNodeModel> {

	/**
	 * {@inheritDoc}
	 */
	@Override
	public DendrogramNodeModel createNodeModel() {
		return new DendrogramNodeModel();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getNrNodeViews() {
		return 1;
	}

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	@Override
	public NodeView<DendrogramNodeModel> createNodeView(final int viewIndex,
			final DendrogramNodeModel nodeModel) {
		// return new HierarchicalClusterNodeView(nodeModel,
		// new HeatmapDendrogramPlotter(
		// new HeatmapDendrogramDrawingPane(),
		// new HeatmapDendrogramPlotterProperties()));
		return new DendrogramNodeView(nodeModel, new HeatmapDendrogramPlotter(
				new HeatmapDendrogramDrawingPane(),
				new HeatmapDendrogramPlotterProperties()));
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
		final Constructor<?> ctor;
		try {
			final Class<?> cls = Class
					.forName("org.knime.base.node.mine.cluster.hierarchical.HierarchicalClusterNodeDialog");
			ctor = cls.getDeclaredConstructors()[0];
			// ctor = cls
			// .getConstructor(new Class[0]);
			ctor.setAccessible(true);
			// return (NodeDialogPane) cls.newInstance();
			return (NodeDialogPane) ctor.newInstance();
		} catch (final SecurityException e) {
			throw new RuntimeException(e);
			// } catch (final NoSuchMethodException e) {
			// throw new RuntimeException(e);
		} catch (final ClassNotFoundException e) {
			throw new RuntimeException(e);
		} catch (final IllegalArgumentException e) {
			throw new RuntimeException(e);
		} catch (final InstantiationException e) {
			throw new RuntimeException(e);
		} catch (final IllegalAccessException e) {
			throw new RuntimeException(e);
		} catch (final InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}
}

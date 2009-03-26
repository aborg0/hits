/**
 * 
 */
package ie.tcd.imm.hits.knime.view.dendrogram.viewonly;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.color.ColorSpace;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JRadioButtonMenuItem;

import org.knime.base.node.mine.cluster.hierarchical.HierarchicalClusterNodeView;
import org.knime.base.node.viz.plotter.AbstractDrawingPane;
import org.knime.base.node.viz.plotter.dendrogram.DendrogramPlotter;
import org.knime.base.node.viz.plotter.node.DefaultVisualizationNodeView;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.def.IntCell;
import org.knime.core.node.NodeModel;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;

import edu.umd.cs.findbugs.annotations.DefaultAnnotation;

/**
 * A {@link HierarchicalClusterNodeView} with a ability to save the main content
 * as a PNG file.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
@DefaultAnnotation( { Nonnull.class, CheckReturnValue.class })
public class DendrogramNodeView extends DefaultVisualizationNodeView {

	private static final String DATA_MENU = "Data";

	private enum ImageType {
		png, svg;
	}

	private final class SaveAs extends AbstractAction {
		private static final long serialVersionUID = -6981404140757972969L;
		private final HeatmapDendrogramPlotter dendrogramPlotter;
		private final ImageType type;

		private SaveAs(final String name,
				final HeatmapDendrogramPlotter heatmapDendrogramPlotter,
				final ImageType type) {
			super(name);
			this.dendrogramPlotter = heatmapDendrogramPlotter;
			this.type = type;
		}

		@Override
		public void actionPerformed(final ActionEvent e) {
			final JFileChooser fileChooser = new JFileChooser();
			switch (fileChooser.showSaveDialog(getComponent())) {
			case JFileChooser.APPROVE_OPTION:
				final AbstractDrawingPane drawingPane = dendrogramPlotter
						.getDrawingPane();
				final BufferedImage bi = new BufferedImage(drawingPane
						.getWidth(), drawingPane.getHeight(),
						ColorSpace.TYPE_RGB);
				switch (type) {
				case png:
					final Graphics2D g = bi.createGraphics();
					g.setColor(Color.BLACK);
					g.setBackground(Color.WHITE);
					drawingPane.paintAll(g);
					try {
						ImageIO.write(bi, "png", fileChooser.getSelectedFile());
					} catch (final IOException e1) {
						throw new RuntimeException(e1);
					}
					break;
				case svg:
					try {
						final Class<?> SVGGraphics2DClass = Class
								.forName("org.apache.batik.svggen.SVGGraphics2D");
						final Class<?> domImplClass = Class
								.forName("org.apache.batik.dom.GenericDOMImplementation");
						final Method getDomImplMethod = domImplClass
								.getMethod("getDOMImplementation");
						final DOMImplementation domImpl = (DOMImplementation) getDomImplMethod
								.invoke(null);
						// Create an instance of org.w3c.dom.Document.
						final String svgNS = "http://www.w3.org/2000/svg";
						final Document document = domImpl.createDocument(svgNS,
								"svg", null);

						// Create an instance of the SVG Generator.
						final Graphics2D svgGenerator = (Graphics2D) SVGGraphics2DClass
								.getConstructor(Document.class).newInstance(
										document);
						// Ask the test to render into the SVG Graphics2D
						// implementation.
						drawingPane.paintAll(svgGenerator);

						// Finally, stream out SVG to the standard output using
						// UTF-8 encoding.
						final boolean useCSS = true; // we want to use CSS style
						// attributes
						final Method stream = SVGGraphics2DClass.getMethod(
								"stream", Writer.class, boolean.class);
						final FileOutputStream fos = new FileOutputStream(
								fileChooser.getSelectedFile());
						try {
							final Writer out = new OutputStreamWriter(fos,
									"UTF-8");
							try {
								stream.invoke(svgGenerator, out, useCSS);
								// svgGenerator.stream(out, useCSS);
							} finally {
								out.close();
							}
						} finally {
							fos.close();
						}
					} catch (final Exception e1) {
						DendrogramNodeModel.logger
								.debug(
										"No batik svggen found, disabling saving to SVG.",
										e1);
						JOptionPane
								.showMessageDialog(
										drawingPane,
										"The Apache Batik SVG Generation or the Apache Batik DOM extension is not installed, but these are necessary for this functionality.\n"
												+ "You can install them from the Orbit download page:\n"
												+ "http://download.eclipse.org/tools/orbit/downloads/",
										"Download Apache Batik SVG Generation/Apache Batik DOM",
										JOptionPane.INFORMATION_MESSAGE);
						setEnabled(false);
					}
					break;
				default:
					throw new UnsupportedOperationException(
							"Not supported image format: " + type);
				}
				break;
			}
		}
	}

	private static enum DataOrder {
		/** no selection, only from second */
		OnlySecond("Columns from second port (no indication of first)"),
		/** no selection, only from first */
		OnlyFirst("Columns only from first port (present in second too)", true),
		/** first in front, commons are selected */
		BothButFirstBefore(
				"Columns only in first in front (all of the columns from second port)"),
		/** second in front, commons are selected */
		BothButSecondBefore(
				"Columns only in second in front (all of the columns from second port)"),
		/**
		 * all of them in the order of second, only from second (commons
		 * selected)
		 */
		BothInOrderOfSecond(
				"Columns from second port (selecting the columns present in first)");
		private final String name;
		private final boolean selected;

		private DataOrder(final String name, final boolean initiallySelected) {
			this.name = name;
			this.selected = initiallySelected;
		}

		private DataOrder(final String name) {
			this(name, false);
		}

		/**
		 * @return the name
		 */
		public String getName() {
			return name;
		}

		/**
		 * @return the selected
		 */
		public boolean isSelected() {
			return selected;
		}
	}

	private class SelectData extends AbstractAction {
		private static final long serialVersionUID = -3563827330967746913L;
		private final HeatmapDendrogramPlotter plotter;
		private final DataOrder order;

		public SelectData(final String string,
				final HeatmapDendrogramPlotter plotter, final DataOrder order) {
			super(string);
			this.plotter = plotter;
			this.order = order;
		}

		@Override
		public void actionPerformed(final ActionEvent e) {
			if (((AbstractButton) e.getSource()).isSelected()) {
				final List<String> visibleColumns = new ArrayList<String>();
				final List<String> selectedColumns = new ArrayList<String>();
				final DendrogramNodeModel nodeModel = (DendrogramNodeModel) getNodeModel();
				final DataTableSpec origSpec = nodeModel.getOrigData()
						.getDataTableSpec();
				final List<String> origCols = new ArrayList<String>();
				for (final DataColumnSpec spec : origSpec) {
					if (spec.getType().isASuperTypeOf(IntCell.TYPE)) {
						origCols.add(spec.getName());
					}
				}
				final HeatmapDendrogramDrawingPane dp = (HeatmapDendrogramDrawingPane) plotter
						.getDrawingPane();
				switch (order) {
				case BothInOrderOfSecond:
					visibleColumns.addAll(origCols);
					selectedColumns.addAll(nodeModel.getSelectedColumns());
					break;
				case BothButFirstBefore:
					visibleColumns.addAll(origCols);
					visibleColumns.removeAll(nodeModel.getSelectedColumns());
					visibleColumns.addAll(0, nodeModel.getSelectedColumns());
					selectedColumns.addAll(nodeModel.getSelectedColumns());
					break;
				case BothButSecondBefore:
					visibleColumns.addAll(origCols);
					visibleColumns.removeAll(nodeModel.getSelectedColumns());
					visibleColumns.addAll(nodeModel.getSelectedColumns());
					selectedColumns.addAll(nodeModel.getSelectedColumns());
					break;
				case OnlyFirst:
					visibleColumns.addAll(nodeModel.getSelectedColumns());
					if (!origCols.containsAll(visibleColumns)) {
						visibleColumns.retainAll(origCols);
					}
					break;
				case OnlySecond:
					visibleColumns.addAll(origCols);
					break;
				default:
					throw new UnsupportedOperationException(order.toString());
				}
				dp.setVisibleColumns(visibleColumns);
				dp.setSelectedColumns(selectedColumns);
				plotter.updatePaintModel();
			}
		}
	}

	/**
	 * Adds a menu to the original view.
	 * 
	 * @param nodeModel
	 *            A {@link NodeModel}.
	 * @param heatmapDendrogramPlotter
	 *            A {@link DendrogramPlotter}.
	 */
	public DendrogramNodeView(final NodeModel nodeModel,
			final HeatmapDendrogramPlotter heatmapDendrogramPlotter) {
		super(nodeModel, heatmapDendrogramPlotter);
		final JMenu file = getJMenuBar().getMenu(0);
		final JMenuItem exportPNG = new JMenuItem(new SaveAs(
				"Export view as PNG", heatmapDendrogramPlotter, ImageType.png));
		file.add(exportPNG);
		final JMenuItem exportSVG = new JMenuItem(new SaveAs(
				"Export view as SVG", heatmapDendrogramPlotter, ImageType.svg));
		file.add(exportSVG);
		final JMenu dataMenu = new JMenu(DATA_MENU);
		final ButtonGroup group = new ButtonGroup();
		for (final DataOrder order : DataOrder.values()) {
			final JRadioButtonMenuItem menuItem = new JRadioButtonMenuItem(
					order.name, order.selected);
			menuItem.setAction(new SelectData(order.name,
					heatmapDendrogramPlotter, order));
			group.add(menuItem);
			dataMenu.add(menuItem);
		}
		getJMenuBar().add(dataMenu);
	}

	@Override
	protected void modelChanged() {
		super.modelChanged();
		((HeatmapDendrogramPlotter) getComponent())
				.setRootNode(((DendrogramNodeModel) getNodeModel()).getRoot());
		final JMenuBar menuBar = getJMenuBar();
		for (int i = menuBar.getMenuCount(); i-- > 0;) {
			final JMenu menu = menuBar.getMenu(i);
			if (DATA_MENU.equals(menu.getText())) {
				for (final Component c : menu.getMenuComponents()) {
					if (c instanceof AbstractButton) {
						final AbstractButton button = (AbstractButton) c;
						if (button.isSelected()) {
							button.getAction().actionPerformed(
									new ActionEvent(button, 0, null));
						}
					}
				}
			}
		}
	}
}

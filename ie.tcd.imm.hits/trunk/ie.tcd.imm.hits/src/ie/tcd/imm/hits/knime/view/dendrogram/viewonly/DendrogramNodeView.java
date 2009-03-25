/**
 * 
 */
package ie.tcd.imm.hits.knime.view.dendrogram.viewonly;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.color.ColorSpace;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.reflect.Method;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

import org.knime.base.node.mine.cluster.hierarchical.HierarchicalClusterNodeView;
import org.knime.base.node.viz.plotter.AbstractDrawingPane;
import org.knime.base.node.viz.plotter.dendrogram.DendrogramPlotter;
import org.knime.base.node.viz.plotter.node.DefaultVisualizationNodeView;
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
	}

	@Override
	protected void modelChanged() {
		super.modelChanged();
		((HeatmapDendrogramPlotter) getComponent())
				.setRootNode(((DendrogramNodeModel) getNodeModel()).getRoot());
	}
}

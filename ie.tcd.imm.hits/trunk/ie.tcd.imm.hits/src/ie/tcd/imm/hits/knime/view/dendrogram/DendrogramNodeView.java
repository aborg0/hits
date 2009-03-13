/**
 * 
 */
package ie.tcd.imm.hits.knime.view.dendrogram;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.color.ColorSpace;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

import org.knime.base.node.mine.cluster.hierarchical.HierarchicalClusterNodeView;
import org.knime.base.node.viz.plotter.AbstractDrawingPane;
import org.knime.base.node.viz.plotter.dendrogram.DendrogramPlotter;
import org.knime.core.node.NodeModel;

import edu.umd.cs.findbugs.annotations.DefaultAnnotation;

/**
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 * 
 */
@DefaultAnnotation( { Nonnull.class, CheckReturnValue.class })
public class DendrogramNodeView extends HierarchicalClusterNodeView {

	/**
	 * @param nodeModel
	 * @param dendrogramPlotter
	 */
	public DendrogramNodeView(final NodeModel nodeModel,
			final DendrogramPlotter dendrogramPlotter) {
		super(nodeModel, dendrogramPlotter);
		final JMenu file = getJMenuBar().getMenu(0);
		final JMenuItem export = new JMenuItem(new AbstractAction(
				"Export view as PNG") {
			private static final long serialVersionUID = -6981404140757972969L;

			@Override
			public void actionPerformed(ActionEvent e) {
				JFileChooser fileChooser = new JFileChooser();
				switch (fileChooser.showSaveDialog(getComponent())) {
				case JFileChooser.APPROVE_OPTION:
					AbstractDrawingPane drawingPane = dendrogramPlotter
							.getDrawingPane();
					final BufferedImage bi = new BufferedImage(drawingPane
							.getWidth(), drawingPane.getHeight(),
							ColorSpace.TYPE_RGB);
					final Graphics2D g = bi.createGraphics();
					g.setColor(Color.BLACK);
					g.setBackground(Color.WHITE);
					drawingPane.paintAll(g);
					try {
						ImageIO.write(bi, "png", fileChooser.getSelectedFile());
					} catch (IOException e1) {
						throw new RuntimeException(e1);
					}
					break;
				}
			}
		});
		file.add(export);
	}

}

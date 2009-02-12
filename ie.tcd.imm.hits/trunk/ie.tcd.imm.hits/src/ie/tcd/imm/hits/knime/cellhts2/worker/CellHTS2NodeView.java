package ie.tcd.imm.hits.knime.cellhts2.worker;

import java.awt.Component;
import java.awt.Desktop;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;

import org.knime.core.node.NodeView;

import edu.umd.cs.findbugs.annotations.DefaultAnnotation;

/**
 * <code>NodeView</code> for the "CellHTS2" Node.
 * <p>
 * TODO currently this does nothing.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
@NotThreadSafe
@DefaultAnnotation(Nonnull.class)
public class CellHTS2NodeView extends NodeView<CellHTS2NodeModel> {
	private final JTabbedPane mainTabs = new JTabbedPane();

	/**
	 * Creates a new view.
	 * 
	 * @param nodeModel
	 *            The model (class: {@link CellHTS2NodeModel})
	 */
	protected CellHTS2NodeView(final CellHTS2NodeModel nodeModel) {
		super(nodeModel);
		setComponent(mainTabs);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void modelChanged() {
		final CellHTS2NodeModel nodeModel = getNodeModel();
		assert nodeModel != null;
		mainTabs.removeAll();
		final Map<String, String> outDirs = nodeModel.getOutDirs();
		for (final Entry<String, String> entry : outDirs.entrySet()) {
			mainTabs.addTab(entry.getKey(), createMainTab(entry.getValue()));
		}
		// be aware of a possibly not executed nodeModel! The data you retrieve
		// from your nodemodel could be null, emtpy, or invalid in any kind.

	}

	/**
	 * @param value
	 * @return
	 */
	private Component createMainTab(final String outputDir) {
		final JSplitPane splitPane = new JSplitPane();
		final String rootFile = "file:/" + outputDir.replaceAll(" ", "%20")
				+ "/index.html";
		splitPane.setRightComponent(new JButton(new AbstractAction(
				"Show in Browser") {
			private static final long serialVersionUID = 3726816134663712521L;

			@Override
			public void actionPerformed(final ActionEvent e) {
				try {
					Desktop.getDesktop().browse(new URI(rootFile));
				} catch (final IOException e1) {
					JOptionPane.showMessageDialog(null, "Unable to launch: "
							+ e1.getMessage(), "Problem with launch",
							JOptionPane.ERROR_MESSAGE);
					CellHTS2NodeModel.logger.error("Launch problem", e1);
				} catch (final URISyntaxException e1) {
					JOptionPane.showMessageDialog(null, "Unable to launch: "
							+ e1.getMessage(), "Problem with launch",
							JOptionPane.ERROR_MESSAGE);
					CellHTS2NodeModel.logger.error("Launch problem", e1);
				}
			}
		}));
		try {
			splitPane.setLeftComponent(new JScrollPane(
					new JEditorPane(rootFile)));
		} catch (final IOException e1) {
			// JOptionPane.showMessageDialog(null, "Unable to show: "
			// + e1.getMessage(), "Problem with show",
			// JOptionPane.ERROR_MESSAGE);
			splitPane
					.setLeftComponent(new JLabel(
							"Problem with showing this result. See the error log for details."));
			CellHTS2NodeModel.logger.error("Show problem", e1);
		}
		final JScrollPane ret = new JScrollPane(splitPane);
		return ret;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onClose() {

		// TODO things to do when closing the view
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onOpen() {

		// TODO things to do when opening the view
	}

}

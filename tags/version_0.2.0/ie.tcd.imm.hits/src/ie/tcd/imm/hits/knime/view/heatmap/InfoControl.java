/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package ie.tcd.imm.hits.knime.view.heatmap;

import ie.tcd.imm.hits.knime.view.heatmap.HeatmapNodeModel.StatTypes;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.Serializable;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;

/**
 * Sets the information format for the associated information {@link JTable}s.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
public class InfoControl extends JPanel {
	private static final long serialVersionUID = -6252559407229787836L;

	private static final String DEFAULT_PATTERN = "<html>\nPlate: <b>${plate}</b> Well: <b>${well}</b> Gene: ${gene id} <b>${gene symbol}</b>"
			+ " Normalisation: ${Normalisation method} - ${Normalisation kind}<table>\n"
			+ "${"
			+ StatTypes.score.name()
			+ "}</table>\n<table>${"
			+ StatTypes.normalised.name() + "}</table>\n</html>";

	private final JButton updateButton = new JButton("update");
	private final JTextArea text = new JTextArea(DEFAULT_PATTERN, 20, 100);

	private ViewModel viewModel;
	private final LabelPatternUpdater actionListener = new LabelPatternUpdater();

	private final class LabelPatternUpdater implements ActionListener,
			Serializable {
		private static final long serialVersionUID = -2841593296782424832L;

		/**
		 * Updates the {@link InfoControl#viewModel}'s label pattern.
		 * {@inheritDoc}
		 */
		@Override
		public void actionPerformed(final ActionEvent e) {
			viewModel.setLabelPattern(text.getText());
		}
	}

	/**
	 * Constructs an {@link InfoControl} object to be able to influence the
	 * labels of wells.
	 */
	public InfoControl() {
		super();
		add(updateButton);
		add(new JScrollPane(text));
		updateButton.addActionListener(actionListener);
	}

	/**
	 * Updating the {@link ViewModel} to modify.
	 * 
	 * @param viewModel
	 *            The new {@link ViewModel}.
	 */
	public void setViewModel(final ViewModel viewModel) {
		this.viewModel = viewModel;
		viewModel.setLabelPattern(text.getText());
	}
}

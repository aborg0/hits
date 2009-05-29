/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package ie.tcd.imm.hits.knime.view.heatmap.simple;

import ie.tcd.imm.hits.knime.util.SimpleModelBuilder;
import ie.tcd.imm.hits.knime.util.ModelBuilder.SpecAnalyser;
import ie.tcd.imm.hits.knime.view.heatmap.ExportLegendAction;
import ie.tcd.imm.hits.knime.view.heatmap.HeatmapNodeModel.StatTypes;
import ie.tcd.imm.hits.util.Misc;
import ie.tcd.imm.hits.util.swing.ImageType;
import ie.tcd.imm.hits.util.swing.PopupListener;
import ie.tcd.imm.hits.util.swing.SaveAs;
import ie.tcd.imm.hits.util.swing.colour.ColourComputer;
import ie.tcd.imm.hits.util.swing.colour.ColourSelector;
import ie.tcd.imm.hits.util.swing.colour.ComplexModel;
import ie.tcd.imm.hits.util.swing.colour.ColourSelector.ColourModel;
import ie.tcd.imm.hits.util.swing.colour.ColourSelector.RangeType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.swing.AbstractAction;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.base.node.util.DataArray;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.RowKey;
import org.knime.core.data.property.ColorAttr;
import org.knime.core.data.property.ColorHandler;
import org.knime.core.node.NodeView;
import org.knime.core.node.property.hilite.HiLiteHandler;
import org.knime.core.node.property.hilite.HiLiteListener;
import org.knime.core.node.property.hilite.KeyEvent;

import edu.umd.cs.findbugs.annotations.DefaultAnnotation;

/**
 * {@link NodeView} for the "SimpleHeatmap" Node. Shows a simple heatmap of the
 * data.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
@DefaultAnnotation( { Nonnull.class, CheckReturnValue.class })
public class SimpleHeatmapNodeView extends NodeView<SimpleHeatmapNodeModel>
		implements ChangeListener, ActionListener {

	private final JSplitPane main = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
			true);
	private final JSpinner cellWidth = new JSpinner(new SpinnerNumberModel(20,
			2, 50, 2));
	private final JSpinner cellHeight = new JSpinner(new SpinnerNumberModel(20,
			2, 50, 2));
	private final JCheckBox textOnLeft = new JCheckBox("Text on left", true);
	private final JCheckBox showValues = new JCheckBox("Show values", false);
	private final JComboBox statistics = new JComboBox();
	private final View view = new View();
	private final JPanel generalTab = new JPanel();
	private final JPanel colourTab = new JPanel();
	private final ColourSelector selector = new ColourSelector(Collections
			.<String> emptyList(), Collections.<StatTypes> emptyList());
	private final Parameters legend = new Parameters();
	private final JScrollPane viewScrollPane;

	private abstract class HiLiteAction extends AbstractAction {
		private static final long serialVersionUID = 251274937768154434L;

		private HiLiteAction(final String name) {
			super(name);
		}

		@Override
		public void actionPerformed(final ActionEvent e) {
			final Set<RowKey> keys = new HashSet<RowKey>(
					view.selections.size() * 2);
			for (final Integer index : view.selections) {
				keys.add(view.table.getRow(index.intValue()).getKey());
			}
			act(keys);
		}

		protected abstract void act(final Set<RowKey> keys);
	}

	private final class Parameters extends JPanel {
		private static final long serialVersionUID = 559337357788334817L;

		private static final int OFFSET = 3;

		private final List<String> parameters = new ArrayList<String>();
		private int maxLength = -1;

		public Parameters() {
			super();
			setBackground(Color.WHITE);
		}

		void updateList(final List<String> parameters) {
			this.parameters.clear();
			this.parameters.addAll(parameters);
			updateMaxLength();
			setPreferredSize(new Dimension(getPreferredSize().width, maxLength));
			setMaximumSize(new Dimension(getPreferredSize().width, maxLength));
		}

		private void updateMaxLength() {
			maxLength = -1;
			final FontMetrics fm = getFontMetrics(getFont());
			for (final String s : parameters) {
				maxLength = Math.max(maxLength, fm.stringWidth(s));
			}
			maxLength += OFFSET;
		}

		@Override
		protected void paintComponent(final Graphics g) {
			super.paintComponent(g);
			final FontMetrics fm = g.getFontMetrics();
			((Graphics2D) g).rotate(-Math.PI / 2);
			int p = 0;
			final int cellW = ((Number) cellWidth.getValue()).intValue();
			final boolean textLeft = textOnLeft.isSelected();

			final int fontHeight = fm.getHeight();
			for (final String param : parameters) {
				g.drawString(param, -maxLength + OFFSET, p++ * cellW + cellW
						/ 2 + fontHeight / 3
						+ (textLeft ? view.maxStringLength : 0));
			}
		}
	}

	private class View extends JPanel implements HiLiteListener {
		private static final long serialVersionUID = 703031608174399419L;
		private EnumMap<StatTypes, Map<String, Integer>> indices;
		private DataArray table;
		private ColourModel model;
		private int maxStringLength = -1;
		private int maxParamLength = -1;
		private List<String> selectedParams;
		private int fontHeight;
		private int[] colIndices;
		private final Set<Integer> selections = new HashSet<Integer>();
		private final Set<Integer> hilites = new HashSet<Integer>();

		public View() {
			super();
			setName("heatmap");

			final MouseAdapter mouseAdapter = new MouseAdapter() {
				private int dragStart = -1;
				private final Set<Integer> origSelection = new HashSet<Integer>();

				@Override
				public void mouseDragged(final MouseEvent e) {
					if ((e.getModifiers() & MouseEvent.BUTTON1) != 0) {
						if (dragStart < 0) {
							dragStart = e.getY();
							origSelection.addAll(view.selections);
						} else {
							view.selections.clear();
							view.selections.addAll(origSelection);
							select(dragStart, e.getY(), false, e
									.isControlDown());
						}
					}
				}

				@Override
				public void mouseReleased(final MouseEvent e) {
					if (dragStart >= 0) {
						view.selections.clear();
						view.selections.addAll(origSelection);
						select(dragStart, e.getY(), false, e.isControlDown());
					}
					dragStart = -1;
					origSelection.clear();
				}

				@Override
				public void mouseClicked(final MouseEvent e) {
					select(e.getY(), e.getY(), e.isControlDown(), e
							.isControlDown());
				}

				private void select(final int y1, final int y2,
						final boolean xorSelection, final boolean addSelection) {
					if (y1 > y2) {
						select(y2, y1, xorSelection, addSelection);
						return;
					}
					final int cellH = ((Number) cellHeight.getValue())
							.intValue();
					if (y2 > table.size() * cellH) {
						select(y1, table.size() * cellH, xorSelection,
								addSelection);
						return;
					}
					if (!xorSelection && !addSelection) {
						selections.clear();
					}
					if (xorSelection) {
						final int min = y1 / cellH;
						for (int i = (y2 + cellH) / cellH; i-- > min;) {
							final Integer integer = Integer.valueOf(i);
							if (!selections.contains(integer)) {
								selections.add(integer);
							} else {
								selections.remove(integer);
							}
						}
					} else {
						final int min = y1 / cellH;
						for (int i = (y2 + cellH) / cellH; i-- > min;) {
							final Integer integer = Integer.valueOf(i);
							selections.add(integer);
						}
					}
					view.repaint();
				}

				@Override
				public void mouseMoved(final MouseEvent e) {
					final int xx = e.getX();
					final int yy = e.getY();
					final int rowIndex = yy
							/ ((Integer) cellHeight.getValue()).intValue();
					final int xoffset = textOnLeft.isSelected() ? maxStringLength
							: 0;
					final int colIndex = xx < xoffset ? -1 : (xx - xoffset)
							/ ((Number) cellWidth.getValue()).intValue();
					if (rowIndex >= 0 && rowIndex < table.size()) {
						final DataCell cell = colIndex >= 0
								&& colIndex < selectedParams.size() ? table
								.getRow(rowIndex).getCell(colIndices[colIndex])
								: null;
						setToolTipText("<html>"
								+ (cell != null ? selectedParams.get(colIndex)
										+ ": "
										+ (cell instanceof DoubleValue ? ((DoubleValue) cell)
												.getDoubleValue()
												: Double.NaN)
										: "") + " (<b>"
								+ table.getRow(rowIndex).getKey().getString()
								+ "</b>)<html>");
					} else {
						setToolTipText(null);
					}
				}
			};
			addMouseListener(mouseAdapter);
			addMouseMotionListener(mouseAdapter);
		}

		public void updateModel(final ColourModel model, final DataArray table,
				final EnumMap<StatTypes, Map<String, Integer>> indices) {
			this.model = model;
			this.table = table;
			this.indices = indices;
			updateStats();
			updateMaxStringLength();
			updateSize();
		}

		private void updateStats() {
			final StatTypes selectedStat = (StatTypes) statistics
					.getSelectedItem();
			if (selectedStat == null) {
				return;
			}
			selectedParams = new ArrayList<String>(indices.get(selectedStat)
					.keySet());
			legend.updateList(selectedParams);
			colIndices = new int[selectedParams.size() * 1/* selectedStats.size() */];
			int idx = 0;
			for (final String param : selectedParams) {
				colIndices[idx++] = indices.get(selectedStat).get(param)
						.intValue();
			}
		}

		/**
		 * Updates the preferred size.
		 */
		protected void updateSize() {
			setPreferredSize(new Dimension(selectedParams.size()
					* ((Number) cellWidth.getValue()).intValue()
					+ maxStringLength, this.table.size()
					* ((Number) cellHeight.getValue()).intValue()
					+ maxParamLength));
			legend.updateList(selectedParams);
		}

		protected void updateMaxStringLength() {
			final FontMetrics fm = getFontMetrics(getFont());
			maxStringLength = -1;
			for (final DataRow row : table) {
				final String s = row.getKey().getString();
				maxStringLength = Math.max(fm.stringWidth(s), maxStringLength);
			}
			maxParamLength = -1;
			for (final String param : selectedParams) {
				maxParamLength = Math
						.max(maxParamLength, fm.stringWidth(param));
			}
			fontHeight = getFontMetrics(getFont()).getHeight();
		}

		@Override
		protected void paintComponent(final Graphics g) {
			super.paintComponent(g);
			final int cellW = ((Number) cellWidth.getValue()).intValue();
			final int cellH = ((Number) cellHeight.getValue()).intValue();
			final boolean textLeft = textOnLeft.isSelected();
			int y = 0;
			final StatTypes selectedStat = (StatTypes) statistics
					.getSelectedItem();
			final boolean showVals = showValues.isSelected();
			final int allCols = /* selectedStats.size() */1 * selectedParams
					.size();
			for (final DataRow row : table) {
				int i = 0;
				final Integer integer = Integer.valueOf(y);
				final Map<String, Integer> map = indices.get(selectedStat);
				final FontMetrics fm = g.getFontMetrics();
				for (final String param : selectedParams) {
					final int index = map.get(param);
					final ColorHandler colorHandler = table.getDataTableSpec()
							.getColumnSpec(index).getColorHandler();
					final DataCell cell = row.getCell(index);
					if (cell instanceof DoubleValue) {
						final double val = ((DoubleValue) cell)
								.getDoubleValue();
						final ColourComputer m = model.getModel(param,
								selectedStat);
						final Color col = m == null ? colorHandler == null ? Color.BLACK
								: colorHandler.getColorAttr(cell).getColor()
								: m.compute(val);
						g.setColor(col);
						g.fillRect((textLeft ? maxStringLength + i * cellW : i /*- 1*/
								* cellW), y * cellH, cellW, cellH);
						if (showVals) {
							g
									.setColor(Color.RGBtoHSB(col.getRed(), col
											.getGreen(), col.getBlue(), null)[2] > .6f ? Color.BLACK
											: Color.WHITE);
							final String str = Misc.round(val);
							g
									.drawString(str,
											(textLeft ? maxStringLength : 0)
													+ i
													* cellW
													+ (cellW - fm
															.stringWidth(str))
													/ 2, y * cellH + cellH / 2
													+ fontHeight / 3);
						}
					}
					++i;
				}
				if (selections.contains(integer) || hilites.contains(integer)) {
					if (selections.contains(integer)) {
						g
								.setColor(hilites.contains(integer) ? ColorAttr.SELECTED_HILITE
										: ColorAttr.SELECTED);
					} else {
						g.setColor(ColorAttr.HILITE);
					}
					((Graphics2D) g).setStroke(new BasicStroke(2.0f));
					g.drawRect(textLeft ? maxStringLength : 0, y * cellH, cellW
							* selectedParams.size(), cellH);
				}

				final ColorAttr colorAttr = table.getDataTableSpec()
						.getRowColor(row);
				if (colorAttr != ColorAttr.DEFAULT) {
					final Color rowColor = colorAttr.getColor();
					g.setColor(rowColor);
					g.fillRect(textLeft ? 0 : allCols * cellW, y * cellH,
							maxStringLength, cellH);
					g
							.setColor(Color.RGBtoHSB(rowColor.getGreen(),
									rowColor.getGreen(), rowColor.getBlue(),
									null)[2] < .4f ? Color.WHITE : Color.BLACK);
				} else {
					g.setColor(Color.BLACK);
				}
				g.drawString(row.getKey().getString(),
						textLeft ? maxStringLength
								- fm.stringWidth(row.getKey().getString())
								: allCols * cellW, y * cellH + cellH / 2
								+ fontHeight / 3);
				++y;
			}
			((Graphics2D) g).rotate(-Math.PI / 2);
			int p = 0;
			final FontMetrics fm = getFontMetrics(getFont());
			for (final String param : selectedParams) {
				g.drawString(param, -table.size() * cellH
						- fm.stringWidth(param), p++ * cellW + cellW / 2
						+ fontHeight / 3 + (textLeft ? maxStringLength : 0));
			}
		}

		@Override
		public void hiLite(final KeyEvent event) {
			final Set<RowKey> keys = event.keys();
			int i = 0;
			for (final DataRow row : table) {
				if (keys.contains(row.getKey())) {
					hilites.add(Integer.valueOf(i));
				}
				++i;
			}
			repaint();
		}

		@Override
		public void unHiLite(final KeyEvent event) {
			final Set<RowKey> keys = event.keys();
			int i = 0;
			for (final DataRow row : table) {
				if (keys.contains(row.getKey())) {
					hilites.remove(Integer.valueOf(i));
				}
				++i;
			}
			repaint();
		}

		@Override
		public void unHiLiteAll(final KeyEvent event) {
			hilites.clear();
			repaint();
		}
	}

	/**
	 * Creates a new view.
	 * 
	 * @param nodeModel
	 *            The model (class: {@link SimpleHeatmapNodeModel})
	 */
	protected SimpleHeatmapNodeView(final SimpleHeatmapNodeModel nodeModel) {
		super(nodeModel);
		final JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, true);
		split.setOneTouchExpandable(true);
		view.setBackground(Color.WHITE);
		viewScrollPane = new JScrollPane(view);
		viewScrollPane.setViewportView(view);
		viewScrollPane.setAutoscrolls(true);
		viewScrollPane.setColumnHeaderView(legend);
		// split.setLeftComponent(viewScrollPane);
		// split.setRightComponent(legend);
		main.setLeftComponent(viewScrollPane);
		final JTabbedPane tabbedPane = new JTabbedPane();
		main.setRightComponent(tabbedPane);
		main.setOneTouchExpandable(true);
		// main.setPreferredSize(new Dimension(800, 800));
		// view.setPreferredSize(new Dimension(800, 700));
		colourTab.add(selector);
		generalTab.add(new JLabel("Cell width: "));
		generalTab.add(cellWidth);
		generalTab.add(new JLabel("Cell height: "));
		generalTab.add(cellHeight);
		generalTab.add(textOnLeft);
		generalTab.add(statistics);
		generalTab.add(showValues);
		tabbedPane.addTab("Colours", new JScrollPane(colourTab));
		tabbedPane.addTab("General", generalTab);
		updateModel(nodeModel);
		main.setDividerLocation(500);
		setComponent(main);
		textOnLeft.addActionListener(this);
		cellHeight.addChangeListener(this);
		cellWidth.addChangeListener(this);
		showValues.addActionListener(this);
		selector.getModel().addActionListener(this);
		statistics.addActionListener(this);
		view.addMouseListener(new PopupListener(createPopupMenu()));
		split.setDividerLocation(1800);
		getJMenuBar().add(createHiLiteMenu());
		getJMenuBar().getMenu(0).add(
				SaveAs.createAction(getComponent(), "Export as PNG", view,
						ImageType.png));
		getJMenuBar().getMenu(0).add(
				SaveAs.createAction(getComponent(), "Export as SVG", view,
						ImageType.svg));
		getJMenuBar().getMenu(0).add(
				new ExportLegendAction<ComplexModel>(selector, ImageType.png));
	}

	private JMenu createHiLiteMenu() {
		final JMenu ret = new JMenu(HiLiteHandler.HILITE);
		for (final JMenuItem menu : createHiLiteMenuItems()) {
			ret.add(menu);
		}
		createHiLiteMenuItems();
		return ret;
	}

	private JPopupMenu createPopupMenu() {
		final JPopupMenu ret = new JPopupMenu(HiLiteHandler.HILITE);
		for (final JMenuItem menu : createHiLiteMenuItems()) {
			ret.add(menu);
		}
		return ret;
	}

	private List<JMenuItem> createHiLiteMenuItems() {
		final List<JMenuItem> ret = new ArrayList<JMenuItem>(3);
		ret.add(new JMenuItem(new HiLiteAction(HiLiteHandler.HILITE_SELECTED) {
			private static final long serialVersionUID = 4904681992526172024L;

			protected void act(final Set<RowKey> keys) {
				getNodeModel().getInHiLiteHandler(0).fireHiLiteEvent(keys);
			}
		}));
		ret.add(new JMenuItem(
				new HiLiteAction(HiLiteHandler.UNHILITE_SELECTED) {
					private static final long serialVersionUID = 4904681992526172024L;

					protected void act(final Set<RowKey> keys) {
						getNodeModel().getInHiLiteHandler(0).fireHiLiteEvent(
								keys);
					}
				}));
		ret.add(new JMenuItem(new HiLiteAction(HiLiteHandler.CLEAR_HILITE) {
			private static final long serialVersionUID = 4904681992526172024L;

			protected void act(final Set<RowKey> keys) {
				getNodeModel().getInHiLiteHandler(0).fireClearHiLiteEvent();
			}
		}));
		return ret;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void modelChanged() {
		final SimpleHeatmapNodeModel nodeModel = getNodeModel();
		assert nodeModel != null;

		updateModel(nodeModel);
		nodeModel.getInHiLiteHandler(0).addHiLiteListener(view);
		view.hiLite(new KeyEvent(this, nodeModel.getInHiLiteHandler(0)
				.getHiLitKeys()));
	}

	/**
	 * Updates the view based on the new {@code nodeModel}.
	 * 
	 * @param nodeModel
	 *            A {@link SimpleHeatmapNodeModel}.
	 */
	void updateModel(final SimpleHeatmapNodeModel nodeModel) {
		if (nodeModel == null || nodeModel.getTable() == null) {
			main.setVisible(false);
			return;
		}
		main.setVisible(true);
		final SpecAnalyser s = new SpecAnalyser(nodeModel.getTable()
				.getDataTableSpec(), false);
		final SimpleModelBuilder builder = new SimpleModelBuilder(nodeModel
				.getTable(), s);
		final Map<String, Map<StatTypes, Map<RangeType, Double>>> ranges = s
				.initialRanges();
		final Map<String, Map<StatTypes, List<Double>>> vals = builder
				.computeAllVals();
		SimpleModelBuilder.computeStatistics(ranges, vals);
		selector.update(s.getParameters(), s.getStatistics(), ranges);
		selector.repaint();
		final EnumMap<StatTypes, Map<String, Integer>> indices = s.getIndices();
		statistics.removeAllItems();
		for (final Entry<StatTypes, Map<String, Integer>> entry : indices
				.entrySet()) {
			if (!entry.getValue().isEmpty()) {
				statistics.addItem(entry.getKey());
			}
		}
		view.updateModel(selector.getModel(), nodeModel.getTable(), indices);
		legend.updateList(view.selectedParams);
		view.repaint();
		legend.repaint();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onClose() {
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onOpen() {
	}

	@Override
	public void stateChanged(final ChangeEvent e) {
		view.updateSize();
		viewScrollPane.getViewport().revalidate();
		main.repaint();
		view.repaint();
		legend.repaint();
	}

	@Override
	public void actionPerformed(final ActionEvent e) {
		view.updateStats();
		view.repaint();
		legend.repaint();
	}
}
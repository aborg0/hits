package ie.tcd.imm.hits.util.swing.colour;

import ie.tcd.imm.hits.knime.util.VisualUtils;
import ie.tcd.imm.hits.knime.view.heatmap.HeatmapNodeModel.StatTypes;
import ie.tcd.imm.hits.util.swing.colour.ColourSelector.ColourModel;
import ie.tcd.imm.hits.util.swing.colour.ColourSelector.DoubleValueSelector;
import ie.tcd.imm.hits.util.swing.colour.ColourSelector.SampleWithText;
import ie.tcd.imm.hits.util.swing.colour.ColourSelector.DoubleValueSelector.Positions;
import ie.tcd.imm.hits.util.swing.colour.ColourSelector.SampleWithText.Orientation;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.Serializable;

import javax.annotation.Nullable;

/**
 * The colour model for the double values.
 */
public class ContinuousModel implements Serializable, ColourComputer,
		ColourFactory<ContinuousModel> {
	private static final long serialVersionUID = 8613456651113117411L;

	static {
		FactoryRegistry.getInstance().registerFactory(ContinuousModel.class,
				null);
	}

	private final double downVal;
	private final double upVal;
	private @Nullable
	final Double middleVal;
	private final Color down, middle, up;

	/**
	 * Constructs a new {@link ContinuousModel} with the raw parameters.
	 * 
	 * @param downVal
	 *            down/left value
	 * @param middleVal
	 *            middle value, or {@code null}
	 * @param upVal
	 *            up/right value
	 * @param down
	 *            down colour
	 * @param middle
	 *            middle colour
	 * @param up
	 *            up colour
	 */
	public ContinuousModel(final double downVal, @Nullable
	final Double middleVal, final double upVal, final Color down,
			final Color middle, final Color up) {
		super();
		this.downVal = downVal;
		this.middleVal = middleVal;
		this.upVal = upVal;
		this.down = down;
		this.middle = middle;
		this.up = up;
	}

	/**
	 * Creates a new {@link ContinuousModel} based on a previous one with
	 * possibly new {@link Color colour} at {@link Positions position}
	 * {@code pos}.
	 * 
	 * @param model
	 *            A {@link ContinuousModel}.
	 * @param pos
	 *            A {@link Positions}.
	 * @param col
	 *            The new {@link Color}.
	 */
	public ContinuousModel(final ContinuousModel model, final Positions pos,
			final Color col) {
		this(model.getDownVal(), model.getMiddleVal(), model.getUpVal(),
				pos == Positions.Down ? col : model.getDown(),
				pos == Positions.Middle ? col : model.getMiddle(),
				pos == Positions.Up ? col : model.getUp());
	}

	/**
	 * Creates a new {@link ContinuousModel} based on a previous one with
	 * possibly new {@code value} at {@link Positions position} {@code pos}.
	 * 
	 * @param model
	 *            A {@link ContinuousModel}.
	 * @param pos
	 *            A {@link Positions}.
	 * @param val
	 *            The new value.
	 */
	public ContinuousModel(final ContinuousModel model, final Positions pos,
			@Nullable
			final Double val) {
		this(pos == Positions.Down ? val.doubleValue() : model.getDownVal(),
				pos == Positions.Middle ? val : model.getMiddleVal(),
				pos == Positions.Up ? val.doubleValue() : model.getUpVal(),
				model.getDown(), model.getMiddle(), model.getUp());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		return getDownVal() + " (" + getDown() + ") -> " + getMiddleVal()
				+ " (" + getMiddle() + ") -> " + getUpVal() + "(" + getUp()
				+ ")";
	}

	/**
	 * @return The {@link Color} for the lower value.
	 */
	public Color getDown() {
		return down;
	}

	/**
	 * @return The {@link Color} for the middle value. (Maybe {@code null}.)
	 */
	public Color getMiddle() {
		return middle;
	}

	/**
	 * @return The {@link Color} for the higher value.
	 */
	public Color getUp() {
		return up;
	}

	/**
	 * @return The lower value.
	 */
	public double getDownVal() {
		return downVal;
	}

	/**
	 * @return The middle value.
	 */
	public @Nullable
	Double getMiddleVal() {
		return middleVal;
	}

	/**
	 * @return The higher value.
	 */
	public double getUpVal() {
		return upVal;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ie.tcd.imm.hits.util.swing.ColourComputer#compute(double)
	 */
	@Override
	public Color compute(final double val) {
		return VisualUtils.colourOf(val, down, middleVal == null ? null
				: middle, up, downVal, middleVal == null ? Double.NaN
				: middleVal.doubleValue(), upVal);
	}

	@Override
	public ColourControl<ContinuousModel> createControl(
			final ColourModel model, final String parameter,
			final StatTypes stat) {
		final DoubleValueSelector ret = new DoubleValueSelector();
		ret.setModel(this);
		ret.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				final ContinuousModel newModel = ((DoubleValueSelector) e
						.getSource()).getModel();
				model.setModel(parameter, stat, newModel);
				// ret.setModel(newModel);
			}
		});
		return ret;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ie.tcd.imm.hits.util.swing.colour.ColourFactory#createLegend()
	 */
	@Override
	public ColourLegend<ContinuousModel> createLegend() {
		final SampleWithText sample = new SampleWithText();
		sample.setModel(this, Orientation.South);
		return sample;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ie.tcd.imm.hits.util.swing.colour.ColourFactory#getDefaultModel()
	 */
	@Override
	public ContinuousModel getDefaultModel() {
		return ColourSelector.DEFAULT_MODEL;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ie.tcd.imm.hits.util.swing.colour.ColourComputer#getTooltip()
	 */
	@Override
	public String getTooltip() {
		return getDownVal()
				+ " -> "
				+ (getMiddleVal() == null || getMiddle() == null ? ""
						: getMiddleVal() + " -> ") + getUpVal();
	}

}
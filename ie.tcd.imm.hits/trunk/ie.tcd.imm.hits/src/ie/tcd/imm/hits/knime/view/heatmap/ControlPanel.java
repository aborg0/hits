/**
 * 
 */
package ie.tcd.imm.hits.knime.view.heatmap;

import ie.tcd.imm.hits.common.Format;
import ie.tcd.imm.hits.knime.view.SplitType;
import ie.tcd.imm.hits.knime.view.heatmap.HeatmapNodeModel.StatTypes;
import ie.tcd.imm.hits.knime.view.heatmap.SliderModel.SliderFactory;
import ie.tcd.imm.hits.knime.view.heatmap.SliderModel.Type;
import ie.tcd.imm.hits.knime.view.heatmap.ViewModel.ParameterModel;
import ie.tcd.imm.hits.knime.view.heatmap.ViewModel.Shape;
import ie.tcd.imm.hits.knime.view.heatmap.ViewModel.ShapeModel;
import ie.tcd.imm.hits.util.Pair;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.border.TitledBorder;
import javax.swing.plaf.basic.BasicBorders;

import org.knime.core.node.NodeLogger;

/**
 * With this panel you can control the appearance of the heatmap's circles
 * views.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
public class ControlPanel extends JPanel {
	private static final long serialVersionUID = -96828595837428105L;

	private static final NodeLogger logger = NodeLogger
			.getLogger(HeatmapNodeView.class);

	private final ButtonGroup heatMapFormatGroup = new ButtonGroup();
	private final ButtonGroup shapeGroup = new ButtonGroup();

	private final JCheckBox showBorderButton = new JCheckBox("Show border");
	private final JCheckBox showPrimarySeparatorButton = new JCheckBox(
			"Show slice separators");
	private final JCheckBox showSecondarySeparatorButton = new JCheckBox(
			"Show circle separators");
	private final JCheckBox showAdditionalSeparatorButton = new JCheckBox(
			"Show rectangle");

	private final JRadioButton _96;

	private final JRadioButton _384;

	private final JRadioButton circle;

	private final JRadioButton rectangle;

	private final LegendPanel legendPanel;

	/**
	 * This class represents the arrangement of the sliders.
	 */
	public static class ArrangementModel implements Serializable,
			ActionListener {
		private static final long serialVersionUID = -3108970660588264496L;

		private final SliderFactory factory = new SliderFactory();

		private final EnumMap<Type, Collection<SliderModel>> sliders = new EnumMap<Type, Collection<SliderModel>>(
				Type.class);

		private final Map<StatTypes, Collection<String>> typeValues = new EnumMap<StatTypes, Collection<String>>(
				StatTypes.class);

		private final LinkedHashMap<ParameterModel, Collection<SliderModel>> mainArrangement = new LinkedHashMap<ParameterModel, Collection<SliderModel>>();

		private final List<ActionListener> listeners = new ArrayList<ActionListener>();

		/**
		 * Constructs an empty {@link ArrangementModel}.
		 */
		public ArrangementModel() {
			super();
		}

		/**
		 * @return The {@link SliderModel}s at different positions.
		 */
		public Map<Type, Collection<SliderModel>> getSliders() {
			final EnumMap<Type, Collection<SliderModel>> ret = new EnumMap<Type, Collection<SliderModel>>(
					Type.class);
			for (final Map.Entry<Type, Collection<SliderModel>> entry : sliders
					.entrySet()) {
				ret.put(entry.getKey(), Collections
						.unmodifiableCollection(entry.getValue()));
			}
			return Collections.unmodifiableMap(ret);
		}

		/**
		 * @return The actual {@link SliderModel}s.
		 */
		public Set<SliderModel> getSliderModels() {
			final Set<SliderModel> ret = new HashSet<SliderModel>();

			for (final Map.Entry<Type, Collection<SliderModel>> entry : sliders
					.entrySet()) {
				for (final SliderModel sliderModel : entry.getValue()) {
					ret.add(sliderModel);
				}
			}
			return ret;
		}

		/**
		 * Updates the sliders based on the new {@link ParameterModel}s.
		 * 
		 * @param possibleParameters
		 *            The possible parameters which may be present in the
		 *            {@link SliderModel}s.
		 */
		public void mutate(final Collection<ParameterModel> possibleParameters) {
			// TODO mutate the current arrangement, instead of creating new
			// one...
			typeValues.clear();
			for (final StatTypes type : StatTypes.values()) {
				typeValues.put(type, new TreeSet<String>());
			}
			for (final Type type : Type.values()) {
				sliders.put(type, new ArrayList<SliderModel>());
			}
			ParameterModel parameters = null;
			ParameterModel plateModel = null;
			ParameterModel replicateModel = null;
			ParameterModel stats = null;
			ParameterModel experiments = null;
			ParameterModel normalisation = null;
			final Set<ParameterModel> knownStats = new HashSet<ParameterModel>();
			for (final ParameterModel parameterModel : possibleParameters) {
				typeValues.get(parameterModel.getType()).add(
						parameterModel.getShortName());
				switch (parameterModel.getType()) {
				case parameter:
					parameters = parameterModel;
					break;
				case plate:
					plateModel = parameterModel;
					break;
				case replicate:
					replicateModel = parameterModel;
					break;
				case meanOrDiff:
				case median:
				case normalized:
				case raw:
				case rawPerMedian:
				case score:
					knownStats.add(parameterModel);
					break;
				case metaStatType:
					stats = parameterModel;
					break;
				case experimentName:
					experiments = parameterModel;
					break;
				case normalisation:
					normalisation = parameterModel;
				default:
					break;
				}
			}
			if (replicateModel == null) {
				replicateModel = new ParameterModel("replicate",
						StatTypes.replicate, null, Collections
								.<String> emptyList(), Collections
								.singletonList(Integer.toString(1)));
				replicateModel.getColorLegend().put(Integer.valueOf(1),
						Color.BLACK);
			}
			for (final StatTypes type : StatTypes.values()) {
				if (typeValues.get(type).isEmpty()) {
					typeValues.remove(type);
				}
			}
			final SliderModel paramsSlider;
			{
				final Map<Integer, Pair<ParameterModel, Object>> parametersMapping = new TreeMap<Integer, Pair<ParameterModel, Object>>();
				int i = 1;
				for (final Object o : parameters.getColorLegend().keySet()) {
					if (o instanceof String) {
						final String name = (String) o;
						parametersMapping.put(Integer.valueOf(i++),
								new Pair<ParameterModel, Object>(parameters,
										name));
					}
				}
				final Set<SliderModel> set = factory.get(Type.Splitter,
						Collections.singletonList(parameters),
						parametersMapping);
				assert !set.isEmpty();
				sliders.get(Type.Splitter).add(
						paramsSlider = set.iterator().next());
				for (final Integer select : parametersMapping.keySet()) {
					paramsSlider.select(select);
				}
			}
			final SliderModel plateSlider;
			{
				final Map<Integer, Pair<ParameterModel, Object>> plateMapping = new TreeMap<Integer, Pair<ParameterModel, Object>>();
				for (final Object o : plateModel.getColorLegend().keySet()) {
					if (o instanceof Integer) {
						final Integer i = (Integer) o;
						plateMapping.put(i, new Pair<ParameterModel, Object>(
								plateModel, i));
					}
				}
				final Set<SliderModel> plateSet = factory.get(Type.Selector,
						Collections.singletonList(plateModel), plateMapping);
				assert !plateSet.isEmpty();
				sliders.get(Type.Selector).add(
						plateSlider = plateSet.iterator().next());
				plateSlider.select(Integer.valueOf(1));
			}
			final SliderModel replicateSlider;
			{
				final Map<Integer, Pair<ParameterModel, Object>> replicateMapping = new TreeMap<Integer, Pair<ParameterModel, Object>>();
				for (final Object o : replicateModel.getColorLegend().keySet()) {
					if (o instanceof Integer) {
						final Integer i = (Integer) o;
						replicateMapping.put(i,
								new Pair<ParameterModel, Object>(
										replicateModel, i));
					}
				}
				if (replicateMapping.isEmpty()) {
				}
				final Set<SliderModel> replicateSet = factory.get(
						Type.Splitter, Collections
								.singletonList(replicateModel),
						replicateMapping);
				assert !replicateSet.isEmpty();
				sliders.get(Type.Splitter).add(
						replicateSlider = replicateSet.iterator().next());
			}
			final SliderModel statSlider;
			{
				int i = 1;
				final Map<Integer, Pair<ParameterModel, Object>> statMapping = new TreeMap<Integer, Pair<ParameterModel, Object>>();
				for (final String statName : stats.getColumnValues()) {
					statMapping.put(i++, new Pair<ParameterModel, Object>(
							stats, StatTypes.valueOf(statName)));
				}
				final Set<SliderModel> statSet = factory.get(Type.Hidden,
						Collections.singletonList(stats), statMapping);
				assert !statSet.isEmpty();
				sliders.get(Type.Hidden).add(
						statSlider = statSet.iterator().next());
				statSlider.select(Integer.valueOf(1));
			}
			final SliderModel experimentSlider;
			{
				int i = 1;
				final Map<Integer, Pair<ParameterModel, Object>> experimentMapping = new TreeMap<Integer, Pair<ParameterModel, Object>>();
				for (final String experimentName : experiments
						.getColumnValues()) {
					experimentMapping.put(i++,
							new Pair<ParameterModel, Object>(experiments,
									experimentName));
				}
				final Set<SliderModel> experimentSet = factory.get(Type.Hidden,
						Collections.singletonList(experiments),
						experimentMapping);
				assert !experimentSet.isEmpty();
				sliders.get(Type.Hidden).add(
						experimentSlider = experimentSet.iterator().next());
				experimentSlider.select(Integer.valueOf(1));
			}
			final SliderModel normaliseSlider;
			{
				int i = 1;
				final Map<Integer, Pair<ParameterModel, Object>> normaliseMapping = new TreeMap<Integer, Pair<ParameterModel, Object>>();
				for (final String normalisationName : normalisation
						.getColumnValues()) {
					normaliseMapping.put(i++, new Pair<ParameterModel, Object>(
							normalisation, normalisationName));
				}
				final Set<SliderModel> normalisationSet = factory.get(
						Type.Hidden, Collections.singletonList(normalisation),
						normaliseMapping);
				assert !normalisationSet.isEmpty();
				sliders.get(Type.Hidden).add(
						normaliseSlider = normalisationSet.iterator().next());
				normaliseSlider.select(Integer.valueOf(1));
			}
			final ArrayList<SliderModel> mainSliders = new ArrayList<SliderModel>();
			mainSliders.add(experimentSlider);
			mainSliders.add(normaliseSlider);
			mainSliders.add(statSlider);
			mainSliders.add(plateSlider);
			mainSliders.add(replicateSlider);
			mainSliders.add(paramsSlider);
			mainArrangement.put(parameters, mainSliders);
		}

		/**
		 * The mutated {@link StatTypes} &Rarr; labels {@link Map} are returned.
		 * 
		 * @return For each {@link StatTypes} which has meaningful values the
		 *         possible labels are assigned to them. It is modifiable, but
		 *         please do not modify.
		 */
		public Map<StatTypes, Collection<String>> getTypeValuesMap() {
			return typeValues;
		}

		/**
		 * There is a prioritised set of parameters, which are shown. This tells
		 * for which {@link ParameterModel} what {@link SliderModel}s have
		 * affect.
		 * 
		 * @return A {@link Map} from the {@link ParameterModel}s to the
		 *         {@link SliderModel}s.
		 */
		public LinkedHashMap<ParameterModel, Collection<SliderModel>> getMainArrangement() {
			return mainArrangement;
		}

		/**
		 * Adds an {@link ActionListener} ({@code listener}) to the
		 * {@link ArrangementModel}.
		 * 
		 * @param listener
		 *            An {@link ActionListener} to add.
		 */
		public void addListener(final ActionListener listener) {
			listeners.add(listener);
		}

		/**
		 * Removes {@code listener} from the {@link ArrangementModel}.
		 * 
		 * @param listener
		 *            The {@link ActionListener} to remove.
		 */
		public void removeListener(final ActionListener listener) {
			listeners.remove(listener);
		}

		/**
		 * Selects a {@link SliderModel} from {@code mainArrangement} with the
		 * proper {@link SliderModel#getSubId()}, and with a parameter with
		 * proper {@link ParameterModel#getType()}.
		 * 
		 * @param mainArrangement
		 *            A mapping from the {@link ParameterModel}s to the
		 *            affected {@link SliderModel}s.
		 * @param n
		 *            A {@link SliderModel#getSubId()}.
		 * @param stat
		 *            A {@link StatTypes}.
		 * @return The first {@link SliderModel} with
		 *         {@link SliderModel#getSubId()} {@code n} and with a
		 *         {@link ParameterModel#getType()} {@code stat} from
		 *         {@code arrangementModel}. It returns {@code null}, if no
		 *         such {@link SliderModel} found.
		 * @see #getMainArrangement()
		 */
		static SliderModel selectNth(
				final LinkedHashMap<ParameterModel, Collection<SliderModel>> mainArrangement,
				final int n, final StatTypes stat) {
			int u = 0;
			for (final Map.Entry<ParameterModel, Collection<SliderModel>> entry : mainArrangement
					.entrySet()) {
				if (u++ == n) {
					final Collection<SliderModel> sliders = entry.getValue();
					for (final SliderModel slider : sliders) {
						for (final ParameterModel param : slider
								.getParameters()) {
							if (param.getType() == stat) {
								// Not sure whether this is necessary.
								assert slider.getParameters().size() == 1;
								return slider;
							}
						}
					}
				}
			}
			return null;
		}

		@Override
		public String toString() {
			return mainArrangement.toString();
		}

		public void addValue(final SliderModel slider, final Integer origKey,
				final Pair<ParameterModel, Object> map) {
			// TODO Auto-generated method stub

		}

		public void removeValue(final SliderModel slider, final Integer origKey) {
			// TODO Auto-generated method stub

		}

		@Override
		public void actionPerformed(final ActionEvent e) {
			for (final ActionListener listener : listeners) {
				listener.actionPerformed(e);
			}
		}
	}

	private static class ParamaterSelection extends JPanel {
		private static final long serialVersionUID = 5247512869526999773L;

		private final JComboBox typeCombobox = new JComboBox(StatTypes.values());

		private final JComboBox valueCombobox = new JComboBox();

		private final EnumMap<StatTypes, Collection<String>> possibleValues = new EnumMap<StatTypes, Collection<String>>(
				StatTypes.class);
		{
			typeCombobox.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(final ActionEvent e) {
					final StatTypes selectedType = (StatTypes) typeCombobox
							.getSelectedItem();
					valueCombobox.removeAllItems();
					for (final String value : possibleValues.get(selectedType)) {
						valueCombobox.addItem(value);
					}
				}
			});
		}

		/**
		 * Constructs a ParameterSelection.
		 */
		public ParamaterSelection() {
			super();
			add(typeCombobox);
			add(valueCombobox);
			setBorder(new BasicBorders.MarginBorder());
		}

		/**
		 * Sets the possible values of the {@link ParameterModel}/{@link ParamaterSelection}.
		 * 
		 * @param possValues
		 *            The possible values for some {@link StatTypes}. Any
		 *            changes on this will not affect the
		 *            {@link ParamaterSelection} object, you have to reset.
		 */
		public void setPossibleValues(
				final Map<StatTypes, Collection<String>> possValues) {
			possibleValues.clear();
			for (final Map.Entry<StatTypes, Collection<String>> entry : possValues
					.entrySet()) {
				possibleValues.put(entry.getKey(), new ArrayList<String>(entry
						.getValue()));
			}
		}
	}

	private final ParamaterSelection paramaterSelection;

	private final HeatmapNodeView view;

	private final JPanel hiddenSliders = new JPanel();

	private final JPanel primarySliders = new JPanel();
	private final JPanel secondarySliders = new JPanel();

	// private final JPanel additionalSliders = new JPanel();

	/**
	 * Constructs a {@link ControlPanel} for {@code origView} (as parent
	 * {@link Component}).
	 * 
	 * @param origView
	 *            A {@link HeatmapNodeView}.
	 */
	public ControlPanel(final HeatmapNodeView origView) {
		super();
		this.view = origView;
		final GridBagLayout gbLayout = new GridBagLayout();
		setLayout(gbLayout);
		final GridBagConstraints formatConstraints = new GridBagConstraints();
		// final GridBagConstraints paramSelectConstraints = formatConstraints;
		paramaterSelection = new ParamaterSelection();
		// gbLayout.addLayoutComponent(paramaterSelection,
		// paramSelectConstraints);
		// add(paramaterSelection, paramSelectConstraints);
		final JPanel heatmapFormatPanel = new JPanel();
		final BasicBorders.RadioButtonBorder hmBorder = new BasicBorders.RadioButtonBorder(
				Color.LIGHT_GRAY, Color.DARK_GRAY, Color.YELLOW, Color.ORANGE);
		heatmapFormatPanel.setBorder(new TitledBorder(hmBorder, "plate type"));
		_96 = new JRadioButton("96", true);
		heatmapFormatPanel.add(_96);
		_384 = new JRadioButton("384", false);
		heatmapFormatPanel.add(_384);
		heatMapFormatGroup.add(_96);
		heatMapFormatGroup.add(_384);
		final JPanel shapePanel = new JPanel();
		circle = new JRadioButton("circle", true);
		rectangle = new JRadioButton("rectangle", true);
		shapeGroup.add(circle);
		shapeGroup.add(rectangle);
		shapeGroup.setSelected(rectangle.getModel(), true);
		shapePanel.add(circle);
		shapePanel.add(rectangle);
		shapePanel.setBorder(new TitledBorder(
				new BasicBorders.RadioButtonBorder(Color.LIGHT_GRAY,
						Color.DARK_GRAY, Color.YELLOW, Color.ORANGE), "shape"));
		final ActionListener actionListener = new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				origView.changeView(
						_96.isSelected() ? Format._96 : Format._384, circle
								.isSelected() ? Shape.Circle : Shape.Rectangle);
				updateControl(origView.getCurrentViewModel());
			}
		};
		_96.addActionListener(actionListener);
		_384.addActionListener(actionListener);
		circle.addActionListener(actionListener);
		rectangle.addActionListener(actionListener);

		formatConstraints.gridx = 1;
		gbLayout.addLayoutComponent(heatmapFormatPanel, formatConstraints);
		add(heatmapFormatPanel, formatConstraints);
		final GridBagConstraints shapeConstraints = new GridBagConstraints();
		shapeConstraints.gridx = 1;
		shapeConstraints.gridy = 1;
		gbLayout.addLayoutComponent(shapePanel, shapeConstraints);
		add(shapePanel, shapeConstraints);
		addBorderButton(origView, gbLayout, showBorderButton, 1, 2, 0);
		addBorderButton(origView, gbLayout, showPrimarySeparatorButton, 2, 2, 1);
		addBorderButton(origView, gbLayout, showSecondarySeparatorButton, 3, 2,
				2);
		addBorderButton(origView, gbLayout, showAdditionalSeparatorButton, 4,
				2, 3);
		legendPanel = new LegendPanel(true, origView.getCurrentViewModel());
		final GridBagConstraints legendConstraints = new GridBagConstraints();
		legendConstraints.gridx = 4;
		legendConstraints.gridheight = 7;
		gbLayout.addLayoutComponent(legendPanel, legendConstraints);
		legendPanel.setPreferredSize(new Dimension(250, 270));
		add(legendPanel, legendConstraints);
		final GridBagConstraints hiddenSlidersConstraints = new GridBagConstraints();
		hiddenSlidersConstraints.gridx = 1;
		hiddenSlidersConstraints.gridy = 5;
		gbLayout.addLayoutComponent(hiddenSliders, hiddenSlidersConstraints);
		origView.getControlsHandler().setContainer(hiddenSliders,
				SplitType.SingleSelect, PositionConstants.control.name());
		add(hiddenSliders, hiddenSlidersConstraints);
		final GridBagConstraints primaryConstraints = new GridBagConstraints();
		primaryConstraints.gridx = 1;
		primaryConstraints.gridy = 2;
		gbLayout.addLayoutComponent(primarySliders, primaryConstraints);
		{
			final GridLayout gridBagLayout = new GridLayout(1, 0);
			primarySliders.setLayout(gridBagLayout);
		}
		origView.getControlsHandler().setContainer(primarySliders,
				SplitType.PrimarySplit, PositionConstants.primary.name());
		add(primarySliders, primaryConstraints);
		final GridBagConstraints secondaryConstraints = new GridBagConstraints();
		secondaryConstraints.gridx = 1;
		secondaryConstraints.gridy = 3;
		{
			final GridLayout gridLayout = new GridLayout(1, 0);
			secondarySliders.setLayout(gridLayout);
		}
		gbLayout.addLayoutComponent(secondarySliders, secondaryConstraints);
		origView.getControlsHandler().setContainer(secondarySliders,
				SplitType.SeconderSplit, PositionConstants.secondary.name());
		add(secondarySliders, secondaryConstraints);
		// final GridBagConstraints additionalConstraints = new
		// GridBagConstraints();
		// additionalConstraints.gridx = 1;
		// additionalConstraints.gridy = 4;
		// gbLayout.addLayoutComponent(additionalSliders,
		// additionalConstraints);
		// add(additionalSliders, additionalConstraints);
		// origView.getControlsHandler().setContainer(additionalSliders,
		// SplitType.AdditionalInfo, PositionConstants.additional.name());
	}

	/**
	 * Updates the control area using the new {@link ViewModel}.
	 * 
	 * @param currentViewModel
	 *            A {@link ViewModel}.
	 */
	protected void updateControl(final ViewModel currentViewModel) {
		switch (currentViewModel.getShape()) {
		case Circle:
			shapeGroup.setSelected(circle.getModel(), true);
			shapeGroup.setSelected(rectangle.getModel(), false);
			showPrimarySeparatorButton.setText("Show slice separators");
			showSecondarySeparatorButton.setText("Show circle separators");
			showAdditionalSeparatorButton.setVisible(true);
			showAdditionalSeparatorButton.setText("Show rectangle");
			break;
		case Rectangle:
			shapeGroup.setSelected(rectangle.getModel(), true);
			shapeGroup.setSelected(circle.getModel(), false);
			showPrimarySeparatorButton.setText("Show vertical lines");
			showSecondarySeparatorButton.setText("Show horizontal lines");
			showAdditionalSeparatorButton.setVisible(false);
			break;
		default:
			throw new UnsupportedOperationException("Not supported: "
					+ currentViewModel.getShape());
		}
		switch (currentViewModel.getFormat()) {
		case _96:
			heatMapFormatGroup.setSelected(_96.getModel(), true);
			heatMapFormatGroup.setSelected(_384.getModel(), false);
			break;
		case _384:
			heatMapFormatGroup.setSelected(_384.getModel(), true);
			heatMapFormatGroup.setSelected(_96.getModel(), false);
			break;
		default:
			throw new UnsupportedOperationException("Not supported: "
					+ currentViewModel.getFormat());
		}
		setSelection(currentViewModel.getMain(), showBorderButton, 0);
		setSelection(currentViewModel.getMain(), showPrimarySeparatorButton, 1);
		setSelection(currentViewModel.getMain(), showSecondarySeparatorButton,
				2);
		setSelection(currentViewModel.getMain(), showAdditionalSeparatorButton,
				3);
		legendPanel.setViewModel(currentViewModel);
		view.getVolatileModel().removeActionListener(legendPanel);
		view.getVolatileModel().addActionListener(legendPanel);
		currentViewModel.getMain().getColourModel().addActionListener(
				legendPanel);
	}

	private void addBorderButton(final HeatmapNodeView origView,
			final GridBagLayout gbLayout, final JCheckBox checkbox,
			final int row, final int col, final int boolPos) {
		final GridBagConstraints showBorderConstraint = new GridBagConstraints();
		setSelection(origView.getCurrentViewModel().getMain(), checkbox,
				boolPos);
		checkbox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				final ViewModel viewModel = origView.getCurrentViewModel();
				final ShapeModel origShapeModel = viewModel.getMain();
				final ArrangementModel origArrangementModel = origShapeModel
						.getArrangementModel();
				final ShapeModel shapeModel = new ShapeModel(
						origArrangementModel, origShapeModel
								.getPrimerParameters(), origShapeModel
								.getSeconderParameters(), origShapeModel
								.getAdditionalParameters(),
						boolPos == 0 ? checkbox.isSelected() : origShapeModel
								.isDrawBorder(), boolPos == 1 ? checkbox
								.isSelected() : origShapeModel
								.isDrawPrimaryBorders(),
						boolPos == 2 ? checkbox.isSelected() : origShapeModel
								.isDrawSecondaryBorders(),
						boolPos == 3 ? checkbox.isSelected() : origShapeModel
								.isDrawAdditionalBorders());
				shapeModel.setColourModel(origShapeModel.getColourModel());
				origView.setCurrentViewModel(new ViewModel(viewModel,
						shapeModel));
			}
		});
		showBorderConstraint.gridx = col;
		showBorderConstraint.gridy = row;
		gbLayout.addLayoutComponent(checkbox, showBorderConstraint);
		add(checkbox, showBorderConstraint);
	}

	private void setSelection(final ShapeModel model, final JCheckBox checkbox,
			final int boolPos) {
		final boolean[] bools = new boolean[] { model.isDrawBorder(),
				model.isDrawPrimaryBorders(), model.isDrawSecondaryBorders(),
				model.isDrawAdditionalBorders() };
		checkbox.setSelected(bools[boolPos]);
	}

	/**
	 * Updates the legend panel using the new {@code model}.
	 * 
	 * @param model
	 *            A {@link ViewModel}.
	 */
	public void setViewModel(final ViewModel model) {
		legendPanel.setViewModel(model);
	}

	/**
	 * Combines the parameters based on the {@code nodeModel}'s
	 * {@link HeatmapNodeModel#getPossibleParameters()}.
	 * 
	 * @param nodeModel
	 *            An executed {@link HeatmapNodeModel}.
	 */
	public void setModel(final HeatmapNodeModel nodeModel) {
		try {
			combineParameters(nodeModel.getPossibleParameters());
		} catch (final Exception e) {
			logger.error("Problem setting the new results.", e);
		}
		internalUpdateParameters();
	}

	private void internalUpdateParameters() {
		paramaterSelection.setPossibleValues(view.getCurrentViewModel()
				.getMain().getArrangementModel().getTypeValuesMap());
	}

	private void combineParameters(
			final Collection<ParameterModel> possibleParameters) {
		view.getCurrentViewModel().getMain().getArrangementModel().mutate(
				possibleParameters);
		view.getCurrentViewModel().getMain().updateParameters(
				possibleParameters);
		view.getVolatileModel().mutateValues(
				view.getCurrentViewModel().getMain().getArrangementModel(),
				view.getNodeModel());
	}
}

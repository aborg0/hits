/**
 * 
 */
package ie.tcd.imm.hits.knime.view.impl;

import ie.tcd.imm.hits.knime.view.ControlsHandler;
import ie.tcd.imm.hits.knime.view.SplitType;
import ie.tcd.imm.hits.knime.view.heatmap.SliderModel;
import ie.tcd.imm.hits.util.Pair;
import ie.tcd.imm.hits.util.swing.SelectionType;
import ie.tcd.imm.hits.util.swing.VariableControl;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Set;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.node.defaultnodesettings.SettingsModel;

import edu.umd.cs.findbugs.annotations.DefaultAnnotation;

/**
 * This is a {@link VariableControl} with some menu to convert to another type,
 * or make it to an another {@link SliderModel} position.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
@DefaultAnnotation( { Nonnull.class, CheckReturnValue.class })
abstract class VariableControlWithMenu extends AbstractVariableControl {
	/**
	 * An action to change to another {@link SplitType type} of container.
	 */
	private class ChangeAction extends AbstractAction {
		private static final long serialVersionUID = -3091085849327364628L;

		private final SplitType splitType;

		/**
		 * @param splitType
		 */
		public ChangeAction(final SplitType splitType) {
			this.splitType = splitType;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
		 */
		@Override
		public void actionPerformed(final ActionEvent e) {
			getControlsHandler().exchangeControls(
					VariableControlWithMenu.this,
					getControlsHandler().getVariableControlsAt(splitType)
							.iterator().next());
		}

	}

	private static final long serialVersionUID = -3325790267603732617L;

	private class MoveAction extends AbstractAction {
		private static final long serialVersionUID = -701281759726333944L;
		private final Pair<SplitType, String> position;

		/**
		 * @param position
		 */
		public MoveAction(final Pair<SplitType, String> position) {
			this.position = position;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
		 */
		@Override
		public void actionPerformed(final ActionEvent e) {
			getControlsHandler().move(VariableControlWithMenu.this,
					position.getRight());
		}

	}

	private final class VisualChangeAction extends AbstractAction {
		private static final long serialVersionUID = -2164544588301759074L;
		private final ControlTypes controlTypes;

		/**
		 * @param controlTypes
		 */
		public VisualChangeAction(final ControlTypes controlTypes) {
			this.controlTypes = controlTypes;
		}

		@Override
		public void actionPerformed(final ActionEvent e) {
			getControlsHandler().changeControlType(
					VariableControlWithMenu.this, controlTypes);
		}
	}

	/**
	 * @param model
	 * @param selectionType
	 * @param controlsHandler
	 * @param changeListener
	 *            The {@link ChangeListener} associated to the {@code model}.
	 */
	public VariableControlWithMenu(final SettingsModelListSelection model,
			final SelectionType selectionType,
			final ControlsHandler<SettingsModel> controlsHandler,
			final ChangeListener changeListener) {
		super(model, selectionType, controlsHandler, changeListener);
		final JPopupMenu popup = new JPopupMenu("Change behaviour");
		final JMenu menuCompat = new JMenu();
		menuCompat.setText("Change appearance");
		menuCompat.setMnemonic(KeyEvent.VK_A);
		final JMenuItem toButtons = new JMenuItem(new VisualChangeAction(
				ControlTypes.Buttons));
		toButtons.setText("to buttons");
		menuCompat.add(toButtons);
		final JMenuItem toCombobox = new JMenuItem(new VisualChangeAction(
				ControlTypes.ComboBox));
		menuCompat.add(toCombobox);
		toCombobox.setText("to drop down list");
		final JMenuItem toList = new JMenuItem(new VisualChangeAction(
				ControlTypes.List));
		toList.setText("to list");
		menuCompat.add(toList);
		final JMenuItem toSlider = new JMenuItem(new VisualChangeAction(
				ControlTypes.Slider));
		toSlider.setText("to slider");
		menuCompat.add(toSlider);
		popup.add(menuCompat);
		final JMenu moveMenu = new JMenu();
		moveMenu.setText("Move to");
		moveMenu.setMnemonic(KeyEvent.VK_M);
		final Set<Pair<SplitType, String>> containers = getControlsHandler()
				.findContainers();
		for (final Pair<SplitType, String> pair : containers) {
			if (pair.getRight() != null) {
				final JMenuItem posMenu = new JMenuItem(new MoveAction(pair));
				posMenu.setText(pair.getRight() + " [" + pair.getLeft() + "]");
				posMenu.getModel().addChangeListener(new ChangeListener() {
					private final JComponent container = getControlsHandler()
							.getContainer(pair.getLeft(), pair.getRight());
					private final Color origBackground = container == null ? null
							: container.getBackground();
					private final Color origForeground = container == null ? null
							: container.getForeground();

					@Override
					public void stateChanged(final ChangeEvent e) {
						if (posMenu.isArmed() && container != null) {
							container.setBackground(origForeground);
						}
						if (!posMenu.isArmed() && container != null) {
							container.setBackground(origBackground);
						}
					}

				});
				moveMenu.add(posMenu);
			}
		}
		popup.add(moveMenu);
		final JMenu changeType = new JMenu("Change type");
		changeType.setMnemonic(KeyEvent.VK_C);
		final JMenuItem toPrimary = new JMenuItem(new ChangeAction(
				SplitType.PrimarySplit));
		changeType.add(toPrimary);
		toPrimary.setMnemonic(KeyEvent.VK_P);
		toPrimary.setText("to primary");
		final JMenuItem toSecondary = new JMenuItem(new ChangeAction(
				SplitType.SeconderSplit));
		changeType.add(toSecondary);
		toSecondary.setMnemonic(KeyEvent.VK_S);
		toSecondary.setText("to secondary");
		popup.add(changeType);
		final MouseAdapter popupListener = new MouseAdapter() {
			/*
			 * (non-Javadoc)
			 * 
			 * @see java.awt.event.MouseAdapter#mousePressed(java.awt.event.MouseEvent)
			 */
			@Override
			public void mousePressed(final MouseEvent e) {
				showPopup(e);
			}

			/*
			 * (non-Javadoc)
			 * 
			 * @see java.awt.event.MouseAdapter#mouseReleased(java.awt.event.MouseEvent)
			 */
			@Override
			public void mouseReleased(final MouseEvent e) {
				showPopup(e);
			}

			private void showPopup(final MouseEvent e) {
				if (e.isPopupTrigger()) {
					popup.show(e.getComponent(), e.getX(), e.getY());
				}
			}
		};
		getComponentPanel().addMouseListener(popupListener);
		getPanel().addMouseListener(popupListener);
	}
}

/**
 * 
 */
package ie.tcd.imm.hits.knime.view.impl;

import ie.tcd.imm.hits.knime.view.ControlsHandler;
import ie.tcd.imm.hits.knime.view.SplitType;
import ie.tcd.imm.hits.util.Pair;
import ie.tcd.imm.hits.util.swing.VariableControl;
import ie.tcd.imm.hits.util.swing.VariableControl.ControlTypes;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
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

import edu.umd.cs.findbugs.annotations.DefaultAnnotation;

/**
 * A popup menu for the {@link AbstractVariableControl}s.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 * @param <ModelType>
 *            The type of the associated model.
 */
@DefaultAnnotation( { Nonnull.class, CheckReturnValue.class })
public class PopupMenu<ModelType> implements MouseListener {
	private final JPopupMenu popup;
	private final ControlsHandler<ModelType> controlsHandler;
	private final VariableControl<ModelType> control;

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
			controlsHandler.exchangeControls(control, controlsHandler
					.getVariableControlsAt(splitType).iterator().next());
		}

	}

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
			controlsHandler.move(control, position.getRight());
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
			controlsHandler.changeControlType(control, controlTypes);
		}
	}

	/**
	 * Constructs a {@link PopupMenu} and on popup selection click it will be
	 * popping up.
	 * 
	 * @param control
	 *            A {@link VariableControl}, this will be modified by the popup
	 *            options.
	 * @param split
	 *            This controls the added options.
	 * @param controlsHandler
	 *            This should the {@link ControlsHandler} for {@code control}.
	 */
	public PopupMenu(final VariableControl<ModelType> control,
			final SplitType split,
			final ControlsHandler<ModelType> controlsHandler) {
		super();
		this.control = control;
		this.controlsHandler = controlsHandler;
		popup = new JPopupMenu("Change behaviour");
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
		final Set<Pair<SplitType, String>> containers = this.controlsHandler
				.findContainers();
		for (final Pair<SplitType, String> pair : containers) {
			if (pair.getRight() != null && pair.getLeft() == split) {
				final JMenuItem posMenu = new JMenuItem(new MoveAction(pair));
				posMenu.setText(pair.getRight());
				posMenu.getModel().addChangeListener(new ChangeListener() {
					private final JComponent container = PopupMenu.this.controlsHandler
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
		if (split != SplitType.PrimarySplit) {
			changeType.add(toPrimary);
		}
		toPrimary.setMnemonic(KeyEvent.VK_P);
		toPrimary.setText("to primary");
		final JMenuItem toSecondary = new JMenuItem(new ChangeAction(
				SplitType.SeconderSplit));
		if (split != SplitType.SeconderSplit) {
			changeType.add(toSecondary);
		}
		toSecondary.setMnemonic(KeyEvent.VK_S);
		toSecondary.setText("to secondary");
		popup.add(changeType);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.awt.event.MouseListener#mouseClicked(java.awt.event.MouseEvent)
	 */
	@Override
	public void mouseClicked(final MouseEvent e) {
		// Do nothing
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.awt.event.MouseListener#mouseEntered(java.awt.event.MouseEvent)
	 */
	@Override
	public void mouseEntered(final MouseEvent e) {
		// Do nothing
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.awt.event.MouseListener#mouseExited(java.awt.event.MouseEvent)
	 */
	@Override
	public void mouseExited(final MouseEvent e) {
		// Do nothing
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.awt.event.MouseListener#mousePressed(java.awt.event.MouseEvent)
	 */
	@Override
	public void mousePressed(final MouseEvent e) {
		showPopup(e);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.awt.event.MouseListener#mouseReleased(java.awt.event.MouseEvent)
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
}

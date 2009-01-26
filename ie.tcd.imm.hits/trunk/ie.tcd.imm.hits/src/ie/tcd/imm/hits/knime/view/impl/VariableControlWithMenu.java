/**
 * 
 */
package ie.tcd.imm.hits.knime.view.impl;

import ie.tcd.imm.hits.knime.view.ControlsHandler;
import ie.tcd.imm.hits.knime.view.heatmap.SliderModel;
import ie.tcd.imm.hits.util.swing.SelectionType;
import ie.tcd.imm.hits.util.swing.VariableControl;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.swing.AbstractAction;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

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
	 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
	 * 
	 */
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

	private static final long serialVersionUID = -3325790267603732617L;

	/**
	 * @param model
	 * @param selectionType
	 * @param controlsHandler
	 */
	public VariableControlWithMenu(final SettingsModelListSelection model,
			final SelectionType selectionType,
			final ControlsHandler<SettingsModel> controlsHandler) {
		super(model, selectionType, controlsHandler);
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

/**
 * 
 */
package ie.tcd.imm.hits.knime.view.impl;

import ie.tcd.imm.hits.util.swing.SelectionType;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JToggleButton;

import org.knime.core.node.defaultnodesettings.SettingsModelFilterString;

/**
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 * 
 */
class ButtonsControl extends AbstractVariableControl {
	private static final long serialVersionUID = -1720492776562820263L;
	private static final boolean INCLUDE_INDICATOR = false;

	private List<JToggleButton> buttons = new ArrayList<JToggleButton>();

	/**
	 * @param model
	 *            A {@link SettingsModelFilterString} to store the preferences.
	 * @param selectionType
	 *            The {@link SelectionType} for this control.
	 */
	public ButtonsControl(final SettingsModelFilterString model,
			final SelectionType selectionType) {
		super(model, selectionType);
		updateComponent();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.knime.core.node.defaultnodesettings.DialogComponent#setEnabledComponents(boolean)
	 */
	@Override
	protected void setEnabledComponents(final boolean enabled) {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.knime.core.node.defaultnodesettings.DialogComponent#updateComponent()
	 */
	@Override
	protected void updateComponent() {
		final List<String> includeList = ((SettingsModelFilterString) getModel())
				.getIncludeList();
		final List<String> excludeList = ((SettingsModelFilterString) getModel())
				.getExcludeList();
		for (final String include : includeList) {
			buttons.add(new JToggleButton(include, INCLUDE_INDICATOR));
		}
		for (final String exclude : excludeList) {
			buttons.add(new JToggleButton(exclude, !INCLUDE_INDICATOR));
		}
		getPanel().removeAll();
		for (final JToggleButton button : buttons) {
			getPanel().add(button);
		}
	}
}

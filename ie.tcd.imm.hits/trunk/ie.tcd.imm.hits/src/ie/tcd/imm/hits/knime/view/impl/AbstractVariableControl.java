/**
 * 
 */
package ie.tcd.imm.hits.knime.view.impl;

import ie.tcd.imm.hits.knime.view.ControlsHandler;
import ie.tcd.imm.hits.util.swing.SelectionType;
import ie.tcd.imm.hits.util.swing.VariableControl;

import javax.swing.JPanel;
import javax.swing.JToolBar;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponent;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.defaultnodesettings.SettingsModelFilterString;
import org.knime.core.node.port.PortObjectSpec;

/**
 * The abstract common base class for the {@link VariableControl}
 * implementations in this package.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
abstract class AbstractVariableControl extends DialogComponent implements
		VariableControl<SettingsModel> {
	private static final long serialVersionUID = 4731533247697627529L;

	private final JToolBar panel = new JToolBar();

	private final SelectionType selectionType;

	private final ControlsHandler<SettingsModel> controlHandler;

	private final ChangeListener changeListener;

	/**
	 * Sets the initial parameters.
	 * 
	 * @param model
	 *            The model to use.
	 * @param selectionType
	 *            The supported {@link SelectionType}.
	 * @param controlHandler
	 *            The handler for possible transformations.
	 * @param changeListener
	 *            The {@link ChangeListener} associated to the {@code model}.
	 */
	public AbstractVariableControl(final SettingsModelListSelection model,
			final SelectionType selectionType,
			final ControlsHandler<SettingsModel> controlHandler,
			final ChangeListener changeListener) {
		super(model);
		panel.setBorder(new TitledBorder(model.getConfigName()));
		this.selectionType = selectionType;
		this.controlHandler = controlHandler;
		this.changeListener = new ChangeListener() {
			@Override
			public void stateChanged(final ChangeEvent e) {
				changeListener.stateChanged(e);
				updateComponent();
			}
		};
		getComponentPanel().add(getPanel());
		getPanel().setFloatable(false);
		model.addChangeListener(this.changeListener);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ie.tcd.imm.hits.util.swing.VariableControl#getSelectionType()
	 */
	@Override
	public SelectionType getSelectionType() {
		return selectionType;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ie.tcd.imm.hits.util.swing.VariableControl#getView()
	 */
	@Override
	public JPanel getView() {
		return getComponentPanel();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ie.tcd.imm.hits.util.swing.VariableControl#isFloating()
	 */
	@Override
	public boolean isFloatable() {
		return getPanel().isFloatable();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ie.tcd.imm.hits.util.swing.VariableControl#setFloating(boolean)
	 */
	@Override
	public void setFloatable(final boolean isFloatable) {
		getPanel().setFloatable(isFloatable);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.knime.core.node.defaultnodesettings.DialogComponent#checkConfigurabilityBeforeLoad(org.knime.core.node.port.PortObjectSpec[])
	 */
	@Override
	protected void checkConfigurabilityBeforeLoad(final PortObjectSpec[] specs)
			throws NotConfigurableException {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.knime.core.node.defaultnodesettings.DialogComponent#setToolTipText(java.lang.String)
	 */
	@Override
	public void setToolTipText(final String text) {
		getPanel().setToolTipText(text);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.knime.core.node.defaultnodesettings.DialogComponent#validateSettingsBeforeSave()
	 */
	@Override
	protected void validateSettingsBeforeSave() throws InvalidSettingsException {
		final SettingsModelFilterString model = (SettingsModelFilterString) getModel();
		for (final String excluded : model.getExcludeList()) {
			if (model.getIncludeList().contains(excluded)) {
				throw new InvalidSettingsException(excluded
						+ " is included too: " + model.getIncludeList());
			}
		}
	}

	/**
	 * @return the panel
	 */
	protected JToolBar getPanel() {
		return panel;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.knime.core.node.defaultnodesettings.DialogComponent#updateComponent()
	 */
	@Override
	protected void updateComponent() {
		panel.removeAll();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ie.tcd.imm.hits.util.swing.VariableControl#getControlsHandler()
	 */
	@Override
	public ControlsHandler<SettingsModel> getControlsHandler() {
		return controlHandler;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		// final int prime = 31;
		final int result = 1;
		// result = prime * result
		// + ((controlHandler == null) ? 0 : controlHandler.hashCode());
		// result = prime * result + ((panel == null) ? 0 : panel.hashCode());
		// result = prime * result
		// + ((selectionType == null) ? 0 : selectionType.hashCode());
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final AbstractVariableControl other = (AbstractVariableControl) obj;
		if (controlHandler == null) {
			if (other.controlHandler != null) {
				return false;
			}
		} else if (controlHandler != other.controlHandler) {
			return false;
		}
		if (panel == null) {
			if (other.panel != null) {
				return false;
			}
		} else if (panel != other.panel) {
			return false;
		}
		if (selectionType == null) {
			if (other.selectionType != null) {
				return false;
			}
		} else if (selectionType != other.selectionType) {
			return false;
		}
		return true;
	}

	/**
	 * @return the change listener of the {@link #getModel() model}.
	 */
	@Override
	public ChangeListener getModelChangeListener() {
		return changeListener;
	}
}

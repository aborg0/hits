/*
 * All rights reserved. (C) Copyright 2011, Trinity College Dublin
 */
package ie.tcd.imm.hits.knime.view.impl;

import ie.tcd.imm.hits.knime.view.ControlsHandler;
import ie.tcd.imm.hits.knime.view.ListSelection;
import ie.tcd.imm.hits.util.select.Selectable;
import ie.tcd.imm.hits.util.swing.SelectionType;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseListener;
import java.util.Collections;
import java.util.Set;
import java.util.SortedMap;

import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.ardverk.collection.Trie;
import org.knime.core.node.defaultnodesettings.SettingsModel;

/**
 * TODO Javadoc!
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
public class TextFieldControl<Model, Sel extends Selectable<Model>> extends
		AbstractVariableControl<Model, Sel> {
	private final JTextField field = new JTextField(20);

	/**
	 * @param model
	 * @param selectionType
	 * @param controlHandler
	 * @param changeListener
	 * @param domainModel
	 */
	public TextFieldControl(final SettingsModelListSelection model,
			final SelectionType selectionType,
			final ControlsHandler<SettingsModel, Model, Sel> controlHandler,
			final ChangeListener changeListener, final Sel domainModel) {
		super(model, selectionType, controlHandler, changeListener, domainModel);
		field.setName(model.getConfigName());
		updateComponent();
		switch (selectionType) {
		case MultipleAtLeastOne:
		case MultipleOrNone:
			if (model.getSelection().size() > 1) {
				model.setSelection(Collections.singleton(model.getSelection()
						.iterator().next()));
			}
			break;
		case Single:
			if (model.getSelection().size() > 1) {
				model.setSelection(Collections.singleton(model.getSelection()
						.iterator().next()));
			}
			break;
		case Unmodifiable:
			break;
		default:
			throw new UnsupportedOperationException(
					"Not supported selection type: " + selectionType);
		}
		field.addKeyListener(new KeyListener() {
			@Override
			public void keyTyped(final KeyEvent e) {
//				if (model.getPossibleValues().contains(field.getText())) {
//					final Set<String> selection;
//					selection = Collections.singleton(field.getText());
//					{
//						switch (selectionType) {
//						case Unmodifiable:
//							// Do nothing, cannot change.
//							break;
//						case Single:
//							if (selection.size() == 1) {
//								model.setSelection(selection);
//							}
//							break;
//						case MultipleAtLeastOne:
//							if (selection.size() >= 1) {
//								model.setSelection(selection);
//							}
//							break;
//						case MultipleOrNone:
//							model.setSelection(selection);
//							break;
//						default:
//							throw new UnsupportedOperationException(
//									"Not supported selection type: "
//											+ selectionType);
//						}
//						updateComponent();
//					}
//				}
			}

			@Override
			public void keyReleased(final KeyEvent e) {
				// Do nothing
			}

			@Override
			public void keyPressed(final KeyEvent e) {
				// Do nothing
			}
		});
		field.getDocument().addDocumentListener(new DocumentListener() {
			//
			// @Override
			// public void valueChanged(final ListSelectionEvent e) {
			// final Object[] selectedValues = list.getSelectedValues();
			// final Set<String> selection = new HashSet<String>();
			// for (final Object object : selectedValues) {
			// if (object instanceof String) {
			// final String str = (String) object;
			// selection.add(str);
			// }
			// }
			// switch (selectionType) {
			// case Unmodifiable:
			// // Do nothing, cannot change.
			// break;
			// case Single:
			// if (selection.size() == 1) {
			// model.setSelection(selection);
			// }
			// break;
			// case MultipleAtLeastOne:
			// if (selection.size() >= 1) {
			// model.setSelection(selection);
			// }
			// break;
			// case MultipleOrNone:
			// model.setSelection(selection);
			// break;
			// default:
			// throw new UnsupportedOperationException(
			// "Not supported selection type: " + selectionType);
			// }
			// updateComponent();
			// }

			private void insertSingleAlternative() {
			}

			@Override
			public void insertUpdate(final DocumentEvent e) {
				// if (e.getLength() != 1) {
				// return;
				// }
				if (field.getSelectedText() != null) {
					return;
				}

				final Trie<String, Object> trie = model.getPossibleOptions();
				final String text = field.getText();
				final SortedMap<String, Object> prefixMap = trie
						.prefixMap(text);
				if (prefixMap.size() == 1) {
					final String newText = prefixMap.firstKey();
					if (!text.equals(newText))
						SwingUtilities.invokeLater(new Runnable() {
							@Override
							public void run() {
								model.setSelection(Collections
										.singletonList(newText));
								field.setText(newText);
								field.setCaretPosition(e.getOffset() + 1);
								field.setSelectionStart(text.length());
								field.setSelectionEnd(field.getText().length());
							}
						});
				} else {
					// TODO popup
				}
			}

			@Override
			public void removeUpdate(final DocumentEvent e) {
				insertSingleAlternative();
			}

			@Override
			public void changedUpdate(final DocumentEvent e) {
			}

		});
		getPanel().add(field);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ie.tcd.imm.hits.util.swing.VariableControl#getType()
	 */
	@Override
	public ie.tcd.imm.hits.util.swing.VariableControl.ControlTypes getType() {
		return ControlTypes.TextField;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.knime.core.node.defaultnodesettings.DialogComponent#setEnabledComponents
	 * (boolean)
	 */
	@Override
	protected void setEnabledComponents(boolean enabled) {
		field.setEnabled(enabled);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * ie.tcd.imm.hits.knime.view.impl.AbstractVariableControl#updateComponent()
	 */
	@Override
	protected void updateComponent() {
		@SuppressWarnings("unchecked")
		ListSelection<String> model = (ListSelection<String>) getModel();
		final Set<String> selection = model.getSelection();
		final String newText = selection.iterator().next();
		if (!field.getText().equals(newText)) {
			field.setText(newText);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * ie.tcd.imm.hits.knime.view.impl.AbstractVariableControl#notifyChange(
	 * java.awt.event.MouseListener,
	 * ie.tcd.imm.hits.knime.view.impl.AbstractVariableControl.Change)
	 */
	@Override
	protected void notifyChange(
			MouseListener listener,
			ie.tcd.imm.hits.knime.view.impl.AbstractVariableControl.Change change) {
		switch (change) {
		case add:
			field.addMouseListener(listener);
			break;
		case remove:
			field.removeMouseListener(listener);
			break;
		default:
			throw new IllegalArgumentException("Not supported change type: "
					+ change);
		}
		super.notifyChange(listener, change);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((field == null) ? 0 : field.hashCode());
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		TextFieldControl<?, ?> other = (TextFieldControl<?, ?>) obj;
		if (field == null) {
			if (other.field != null)
				return false;
		} else if (field != other.field)
			return false;
		return true;
	}
}

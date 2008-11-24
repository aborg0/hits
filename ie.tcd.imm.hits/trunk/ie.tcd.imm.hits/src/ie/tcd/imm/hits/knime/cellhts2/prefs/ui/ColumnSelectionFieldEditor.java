/**
 * 
 */
package ie.tcd.imm.hits.knime.cellhts2.prefs.ui;

import ie.tcd.imm.hits.knime.cellhts2.prefs.Displayable;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

import org.eclipse.jface.preference.ListEditor;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.ListDialog;

/**
 * @author bakosg@tcd.ie
 */
public class ColumnSelectionFieldEditor<EnumType extends Enum<EnumType> & Displayable>
		extends ListEditor {

	public static final String STRING_SEPARATOR = "\t";
	public static final String STRING_SEPARATOR_PATTERN = STRING_SEPARATOR;
	private final Collection<EnumType> possibleValues;
	static {
		assert Pattern.compile(STRING_SEPARATOR_PATTERN) != null : "Wrong separator: "
				+ STRING_SEPARATOR_PATTERN;
		assert Pattern.matches(STRING_SEPARATOR_PATTERN, STRING_SEPARATOR) : "Wrong separator: "
				+ STRING_SEPARATOR_PATTERN;
	}

	public ColumnSelectionFieldEditor(final String name,
			final String labelText, final Composite parent,
			final Collection<EnumType> possibleValues) {
		super(name, labelText, parent);
		this.possibleValues = new ArrayList<EnumType>(possibleValues);

	}

	@Override
	protected String createList(final String[] items) {
		@SuppressWarnings("unchecked")
		final EnumType[] vals = (EnumType[]) Array.newInstance(possibleValues
				.iterator().next().getClass(), items.length);
		int i = 0;
		for (final String string : items) {
			boolean found = false;
			for (final EnumType possibleValue : possibleValues) {
				if (possibleValue.getDisplayText().equals(string)) {
					found = true;
					vals[i++] = possibleValue;
					break;
				}
			}
			if (!found) {
				throw new IllegalArgumentException(
						"Wrong argument, this is not a possible value: "
								+ string);
			}
		}
		return createList(vals);
	}

	public static <EnumType extends Enum<EnumType>> String createList(
			final EnumType... values) {
		final StringBuilder sb = new StringBuilder();
		for (final EnumType value : values) {
			sb.append(value.name()).append(STRING_SEPARATOR);
		}
		if (values.length > 0) {
			sb.setLength(sb.length());
		}
		return sb.toString();
	}

	@Override
	protected String getNewInputObject() {
		final ListDialog listDialog = new ListDialog(new Shell());
		listDialog.setInput(possibleValues);
		listDialog.setTitle("Select the new column");
		listDialog.setHelpAvailable(false);
		listDialog.setBlockOnOpen(true);
		listDialog.setAddCancelButton(true);
		listDialog.setLabelProvider(new LabelProvider() {
			@Override
			public String getText(final Object element) {
				if (element instanceof Displayable) {
					final Displayable e = ((Displayable) element);
					return e.getDisplayText();
				}
				return super.getText(element);
			}
		});
		final ArrayContentProvider cp = new ArrayContentProvider();
		listDialog.setContentProvider(cp);
		final int result = listDialog.open();
		switch (result) {
		case Window.OK:
			@SuppressWarnings("unchecked")
			final EnumType selectedEnum = ((EnumType) listDialog.getResult()[0]);
			return selectedEnum.getDisplayText();
		default:
			throw new IllegalArgumentException("No selection has done.");
		}
	}

	@Override
	protected String[] parseString(final String stringList) {
		@SuppressWarnings("unchecked")
		final List<? extends Enum> list = ColumnSelectionFieldEditor
				.parseString(possibleValues.iterator().next().getClass(),
						stringList);
		final String[] ret = new String[list.size()];
		int i = 0;
		for (final Enum<?> e : list) {
			ret[i++] = e instanceof Displayable ? ((Displayable) e)
					.getDisplayText() : e.name();
		}
		return ret;
	}

	public static <EnumType extends Enum<EnumType>> List<EnumType> parseString(
			final Class<EnumType> cls, final String stringList) {
		final String[] stringVals = stringList.split(STRING_SEPARATOR_PATTERN);
		final List<EnumType> ret = new ArrayList<EnumType>(stringVals.length);
		for (final String string : stringVals) {
			ret.add(Enum.<EnumType> valueOf(cls, string));
		}
		return ret;
	}
}
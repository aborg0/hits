/**
 * 
 */
package ie.tcd.imm.hits.knime.view.prefs;

import ie.tcd.imm.hits.knime.xls.ImporterNodePlugin;
import ie.tcd.imm.hits.util.Displayable;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.eclipse.jface.preference.ColorFieldEditor;
import org.eclipse.jface.preference.ComboFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import edu.umd.cs.findbugs.annotations.DefaultAnnotation;

/**
 * A preference page for the default colours.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
@DefaultAnnotation( { Nonnull.class, CheckReturnValue.class })
public class ColourPreferencePage extends FieldEditorPreferencePage implements
		IWorkbenchPreferencePage {
	/** The default title for this preference page. */
	static final String DEFAULT_TITLE = "Colour selection for heatmaps";

	/**
	 * Default constructor for {@link ColourPreferencePage}.
	 * 
	 * @see ColourPreferencePage#ColourPreferencePage(int)
	 */
	public ColourPreferencePage() {
		this(GRID);
	}

	/**
	 * Constructs {@link ColourPreferencePage} without image and the default
	 * title.
	 * 
	 * @param style
	 *            The style of the {@link PreferencePage}.
	 * @see FieldEditorPreferencePage#FieldEditorPreferencePage(int)
	 * @see ColourPreferencePage#ColourPreferencePage(String, int)
	 */
	public ColourPreferencePage(final int style) {
		this(DEFAULT_TITLE, style);
	}

	/**
	 * Constructs {@link ColourPreferencePage} without image.
	 * 
	 * @param title
	 *            The title of the {@link PreferencePage}.
	 * @param style
	 *            The style of the {@link PreferencePage}.
	 * @see FieldEditorPreferencePage#FieldEditorPreferencePage(String, int)
	 * @see ColourPreferencePage#ColourPreferencePage(String, ImageDescriptor,
	 *      int)
	 */
	public ColourPreferencePage(final String title, final int style) {
		this(title, null, style);
	}

	/**
	 * Constructs {@link ColourPreferencePage} with the specified parameters.
	 * 
	 * @param title
	 *            The title of the {@link PreferencePage}.
	 * @param image
	 *            The image of the {@link PreferencePage}. May be {@code null}.
	 * @param style
	 *            The style of the {@link PreferencePage}.
	 * @see FieldEditorPreferencePage#FieldEditorPreferencePage(String,
	 *      ImageDescriptor, int)
	 */
	public ColourPreferencePage(final String title, @Nullable
	final ImageDescriptor image, final int style) {
		super(title, image, style);
		setPreferenceStore(ImporterNodePlugin.getDefault().getPreferenceStore());
		setDescription("Set the default colour codes for your heatmaps.");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.preference.FieldEditorPreferencePage#createFieldEditors()
	 */
	@Override
	protected void createFieldEditors() {
		final ColorFieldEditor highColour = new ColorFieldEditor(
				ColourPreferenceConstants.UP_COLOUR, "Colour for high values",
				getFieldEditorParent());
		final ColorFieldEditor middleColour = new ColorFieldEditor(
				ColourPreferenceConstants.MIDDLE_COLOUR,
				"Colour for medium values", getFieldEditorParent());
		final ColorFieldEditor lowColour = new ColorFieldEditor(
				ColourPreferenceConstants.DOWN_COLOUR, "Colour for low values",
				getFieldEditorParent());
		final ComboFieldEditor highValue = new ComboFieldEditor(
				ColourPreferenceConstants.UP_VALUE, "Value for high values",
				createOptions(ColourPreferenceConstants.DEFAULT_UP_OPTIONS),
				getFieldEditorParent());
		final ComboFieldEditor middleValue = new ComboFieldEditor(
				ColourPreferenceConstants.MIDDLE_VALUE,
				"Value for medium values",
				createOptions(ColourPreferenceConstants.DEFAULT_MIDDLE_OPTIONS),
				getFieldEditorParent());
		final ComboFieldEditor lowValue = new ComboFieldEditor(
				ColourPreferenceConstants.DOWN_VALUE, "Value for low values",
				createOptions(ColourPreferenceConstants.DEFAULT_DOWN_OPTIONS),
				getFieldEditorParent());
		addField(highColour);
		highColour.load();
		addField(highValue);
		highValue.load();
		addField(middleColour);
		middleColour.load();
		addField(middleValue);
		middleValue.load();
		addField(lowColour);
		lowColour.load();
		addField(lowValue);
		lowValue.load();
	}

	/**
	 * Converts the enum options to text.
	 * 
	 * @param <EType>
	 *            The type of the enum.
	 * @param defaultOptions
	 *            The options as enums.
	 * @return The possible options' {@link Displayable#getDisplayText()} as
	 *         text.
	 */
	private static <EType extends Enum<EType> & Displayable> String[][] createOptions(
			final EType[] defaultOptions) {
		final String[][] ret = new String[defaultOptions.length][2];
		for (int i = 0; i < defaultOptions.length; i++) {
			if (defaultOptions[i] != null) {
				ret[i][0] = defaultOptions[i].getDisplayText();
				ret[i][1] = defaultOptions[i].name();
			}
		}
		return ret;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
	 */
	@Override
	public void init(final IWorkbench workbench) {
	}

}

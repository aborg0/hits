package ie.tcd.imm.hits.util.swing;

/**
 * Describes the selection type possible for a control. ({@link #Single},
 * {@link #MultipleAtLeastOne}, or {@link #MultipleOrNone})
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
public enum SelectionType {
	/** After construction the selection is no longer modifiable. */
	Unmodifiable,
	/** You have to select exactly one option. */
	Single,
	/** You can select as many options as possible, but must have at least one. */
	MultipleAtLeastOne,
	/** You can select as many options as possible. */
	MultipleOrNone;
}

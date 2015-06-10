package ie.tcd.imm.hits.image.loci.convert;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;

/**
 * <code>NodeDialog</code> for the "ConvertToImage" Node. Converts the images
 * from LOCI Reader to KNIME imaging format.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
public class ConvertToImageNodeDialog extends DefaultNodeSettingsPane {

	/**
	 * New pane for configuring the ConvertToImage node.
	 */
	protected ConvertToImageNodeDialog() {
		super();
		addDialogComponent(new DialogComponentBoolean(new SettingsModelBoolean(
				ConvertToImageNodeModel.CFGKEY_COMBINE_CHANNELS,
				ConvertToImageNodeModel.DEFAULT_COMBINE_CHANNELS),
				"Combine channels"));
	}
}

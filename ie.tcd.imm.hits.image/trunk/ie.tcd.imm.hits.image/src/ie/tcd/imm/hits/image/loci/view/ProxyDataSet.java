/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package ie.tcd.imm.hits.image.loci.view;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.rmi.RemoteException;

import loci.formats.FormatException;
import loci.formats.FormatReader;
import loci.formats.gui.BufferedImageReader;
import loci.visbio.data.Dataset;
import loci.visbio.state.Dynamic;
import visad.FunctionType;
import visad.ImageFlatField;
import visad.MathType;
import visad.RealTupleType;
import visad.RealType;
import visad.TupleType;
import visad.VisADException;

/**
 * TODO Javadoc!
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
public class ProxyDataSet extends Dataset {

	/**
	 * 
	 */
	public ProxyDataSet(final FormatReader reader) {
		super();
		this.reader = new BufferedImageReader(reader);
		initState(null);
	}

	/** Modifies this object's state to match that of the given object. */
	public void initState(final Dynamic dyn) {
		if (dyn != null && !isCompatible(dyn)) {
			return;
		}
		// super.initState(dyn);
		final Dataset data = (Dataset) dyn;

		// if (data != null) {
		// pattern = data.pattern;
		// }
		pattern = "";

		final int numTasks = 4;

		// initialize data reader
		// reader = new BufferedImageReader(new ChannelSeparator(new
		// FileStitcher(
		// true)));
		// reader.setMetadataStore(MetadataTools.createOMEXMLMetadata());

		// determine number of images per source file
		// status(1, numTasks, "Initializing dataset");
		// try {
		// reader.setId(pattern);
		// } catch (final Exception exc) {
		// System.err.println("Could not initialize the dataset. '" + pattern
		// + "' may be corrupt or invalid.");
		// if (VisBioFrame.DEBUG) {
		// exc.printStackTrace();
		// }
		// return;
		// }
		final int[] cLen = reader.getChannelDimLengths();
		lengths = new int[2 + cLen.length];
		lengths[0] = reader.getSizeT();
		lengths[1] = reader.getSizeZ();
		System.arraycopy(cLen, 0, lengths, 2, cLen.length);
		final String[] cTypes = reader.getChannelDimTypes();
		dims = new String[2 + cTypes.length];
		dims[0] = "Time";
		dims[1] = "Slice";
		System.arraycopy(cTypes, 0, dims, 2, cTypes.length);
		makeLabels();

		// load first image for analysis
		// status(2, numTasks, "Reading first image");
		BufferedImage img = null;
		try {
			img = reader.openImage(0);
		} catch (final IOException exc) {
			img = null;
		} catch (final FormatException exc) {
			img = null;
		} catch (final NullPointerException exc) {
			img = null;
		}
		if (img == null) {
			System.err.println("Could not read the first image. '" + pattern
					+ "' may be corrupt or invalid.");
			return;
		}
		ImageFlatField ff = null;
		try {
			ff = new ImageFlatField(img);
		} catch (final VisADException exc) {
			System.err.println("Could not construct ImageFlatField.");
			exc.printStackTrace();
			return;
		} catch (final RemoteException exc) {
			System.err.println("Could not construct ImageFlatField.");
			exc.printStackTrace();
			return;
		}

		// extract range components
		final FunctionType ftype = (FunctionType) ff.getType();
		final MathType range = ftype.getRange();
		if (range instanceof TupleType) {
			final TupleType rangeTuple = (TupleType) range;
			color = rangeTuple.getRealComponents();
		} else if (range instanceof RealType) {
			color = new RealType[] { (RealType) range };
		} else {
			System.err.println("Invalid range type ("
					+ range.getClass().getName() + ")");
			return;
		}

		// extract domain types
		final RealTupleType domain = ftype.getDomain();
		spatial = domain.getRealComponents();

		// construct metadata controls
		// status(3, numTasks, "Finishing");
		// controls = new DatasetWidget(this);

		// construct thumbnail handler
		// String path = new File(pattern).getParent();
		// if (path == null) path = "";
		// thumbs = new ThumbnailHandler(this,
		// path + File.separator + name + ".visbio");
		// status(5, numTasks, "Done");
	}

}

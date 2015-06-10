/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package ie.tcd.imm.hits.image.omero;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.openmicroscopy.shoola.env.Agent;
import org.openmicroscopy.shoola.env.config.Registry;
import org.openmicroscopy.shoola.env.event.AgentEvent;
import org.openmicroscopy.shoola.env.event.AgentEventListener;

/**
 * The {@link Agent} to access OMERO server. <br/>
 * Based on <a href=
 * "http://trac.openmicroscopy.org.uk/shoola/wiki/OmeroInsightHowToRetrieveData"
 * >this document</a>.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
public class OmeroViewerAgent implements Agent, AgentEventListener {
	private static Registry registry;

	/** Public no-arg constructor for load. */
	public OmeroViewerAgent() {
		super();
	}

	/** {@inheritDoc} */
	@Override
	public void activate() {
		// Do nothing
	}

	/** {@inheritDoc} */
	@Override
	public boolean canTerminate() {
		return true;
	}

	/** {@inheritDoc} */
	@SuppressWarnings("unchecked")
	@Override
	public Map<String, Set> hasDataToSave() {
		return Collections.emptyMap();
	}

	/** {@inheritDoc} */
	@Override
	public void setContext(final Registry ctx) {
		registry = ctx;
	}

	/** {@inheritDoc} */
	@Override
	public void terminate() {
		// Do nothing
	}

	/** {@inheritDoc} */
	@Override
	public void eventFired(final AgentEvent e) {
		System.out.println(e);
	}

	/**
	 * @return The registry.
	 */
	public static Registry getRegistry() {
		return registry;
	}
}

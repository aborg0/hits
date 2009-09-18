/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package ie.tcd.imm.hits.util.select;

import java.util.Collection;
import java.util.Set;

/**
 * TODO Javadoc!
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
public interface OptionalValues<T> {
	public Set<T> getActiveValues();

	public void setActiveValues(Collection<T> activeValues);
}

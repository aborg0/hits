/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package ie.tcd.imm.knime.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.knime.core.data.RowKey;
import org.knime.core.node.property.hilite.DefaultHiLiteMapper;

//import edu.umd.cs.findbugs.annotations.DefaultAnnotation;

/**
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
// @DefaultAnnotation( { Nonnull.class, CheckReturnValue.class })
public class TwoWayHiLiteMapper extends DefaultHiLiteMapper {

	private final Map<RowKey, RowKey> inverseMap = new HashMap<RowKey, RowKey>();

	/**
	 * @param map
	 */
	public TwoWayHiLiteMapper(final Map<RowKey, Set<RowKey>> map) {
		super(map);
		if (map != null) {
			for (final Entry<RowKey, Set<RowKey>> entry : map.entrySet()) {
				if (entry.getValue() != null)
					for (final RowKey val : entry.getValue()) {
						inverseMap.put(val, entry.getKey());
					}
			}
		}
	}

	/**
	 * @return the inverseMap
	 */
	public Map<RowKey, RowKey> getInverseMap() {
		return Collections.unmodifiableMap(inverseMap);
	}
}

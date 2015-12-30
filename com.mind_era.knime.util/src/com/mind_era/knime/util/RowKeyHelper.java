/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package com.mind_era.knime.util;

import java.util.Collection;

import org.knime.base.node.mine.cluster.hierarchical.view.ClusterViewNode;
import org.knime.base.node.viz.plotter.dendrogram.DendrogramNode;
import org.knime.core.data.RowKey;

/**
 * A simple utility class to help get the {@link RowKey} from the nodes.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
public class RowKeyHelper {

	/** Hide default constructor. */
	private RowKeyHelper() {
		super();
	}

	/**
	 * @param node
	 *            A leaf {@link DendrogramNode}.
	 * @return The associated {@link RowKey}.
	 */
	public static RowKey getKey(final DendrogramNode node) {
		if (node.getLeafDataPoint() != null) {
			return node.getLeafDataPoint().getKey();
		}
		if (node instanceof ClusterViewNode) {
			final ClusterViewNode cvn = (ClusterViewNode) node;
			return cvn.getLeafRowKey();
		}
		throw new IllegalStateException("No method found to get the row key.");
		// RowKey key;
		// try {
		// final Method getKey = node.getClass().getMethod("getLeafRowKey");
		// key = (RowKey) getKey.invoke(node);
		// } catch (final RuntimeException e) {
		// throw e;
		// } catch (final Exception e) {
		// throw new RuntimeException(e);
		// }
		// return key;
	}

	/**
	 * Collects the {@link RowKey}s of {@code node} to {@code ids}.
	 * 
	 * @param node
	 *            A {@link DendrogramNode}.
	 * @param ids
	 *            A mutable {@link Collection} of {@link RowKey} (will be
	 *            modified).
	 */
	public static void getRowKeys(final DendrogramNode node,
			final Collection<RowKey> ids) {
		if (node == null) {
			return;
		}
		if (node.isLeaf()) {
			final RowKey key = RowKeyHelper.getKey(node);
			ids.add(key);
			return;
		}
		getRowKeys(node.getFirstSubnode(), ids);
		getRowKeys(node.getSecondSubnode(), ids);
	}
}

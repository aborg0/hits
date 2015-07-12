/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package ie.tcd.imm.hits.util.interval;

import ie.tcd.imm.hits.util.interval.Interval.DefaultInterval;

import java.util.NavigableMap;
import java.util.TreeMap;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import edu.umd.cs.findbugs.annotations.DefaultAnnotation;

/**
 * Some tests to test {@link DefaultInterval} implementation of {@link Interval}
 * .
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
@SuppressWarnings("boxing")
@DefaultAnnotation( { Nonnull.class, CheckReturnValue.class })
public class SimpleIntervalTests {
	@DataProvider(name = "simple")
	public Object[][] simpleData() {
		return new Object[][] {
				{ new DefaultInterval<Double>(1.0, 2.0, true, true), 1.5, true },
				{ new DefaultInterval<Double>(1.0, 2.0, true, true), .5, false },
				{ new DefaultInterval<Double>(1.0, 2.0, true, true), 2.5, false },
				{ new DefaultInterval<Double>(1.0, 2.0, true, true), 1.0, true },
				{ new DefaultInterval<Double>(1.0, 2.0, true, true), 2.0, true } };
	}

	@Test(dataProvider = "simple")
	public void contains(final Interval<Double> interval, final Double value,
			final boolean expected) {
		Assert.assertEquals(interval.contains(value), expected);
	}

	private static final NavigableMap<Interval<Double>, Integer> singleMap = new TreeMap<Interval<Double>, Integer>();
	static {
		singleMap.put(new DefaultInterval<Double>(1.0, 2.0, true, true),
				Integer.valueOf(1));
	}
	private static final NavigableMap<Interval<Double>, Integer> twoElemMap = new TreeMap<Interval<Double>, Integer>();
	static {
		twoElemMap.putAll(singleMap);
		twoElemMap.put(new DefaultInterval<Double>(2.0, 3.0, false, true),
				Integer.valueOf(1));
	}
	private static final NavigableMap<Interval<Double>, Integer> threeElemMap = new TreeMap<Interval<Double>, Integer>();
	static {
		threeElemMap.putAll(twoElemMap);
		threeElemMap.put(new DefaultInterval<Double>(4.0, 6.0, false, false),
				Integer.valueOf(3));
	}

	@DataProvider(name = "map")
	public Object[][] mapData() {
		return new Object[][] { { singleMap, 0.0, false },
				{ singleMap, 1.0, true }, { singleMap, 2.0, true },
				{ singleMap, 4.0, false }, { twoElemMap, 0.0, false },
				{ twoElemMap, 1.0, true }, { twoElemMap, 2.0, true },
				{ twoElemMap, 4.0, false }, { threeElemMap, 0.0, false },
				{ threeElemMap, 1.0, true }, { threeElemMap, 2.0, true },
				{ threeElemMap, 4.0, false }, { threeElemMap, 5.0, true },
				{ threeElemMap, 6.0, false } };
	}

	@Test(dataProvider = "map")
	public void mapTests(final NavigableMap<Interval<Double>, ?> map,
			final Double value, final boolean expected) {
		final DefaultInterval<Double> val = new DefaultInterval<Double>(value,
				value, true, true);
		// final NavigableMap<Interval<Double>, ?> subMap = map.subMap(val,
		// true,
		// val, true);
		// Assert.assertEquals(!subMap.isEmpty(), expected);
		// Assert.assertEquals(subMap.size(), expected ? 1 : 0);
		final Interval<Double> higherKey = map.higherKey(val);
		final Interval<Double> lowerKey = map.lowerKey(val);
		Assert.assertEquals(higherKey != null && higherKey.contains(value)
				|| lowerKey != null && lowerKey.contains(value), expected);
	}
}

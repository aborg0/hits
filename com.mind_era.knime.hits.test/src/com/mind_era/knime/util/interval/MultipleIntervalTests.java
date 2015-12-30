/*
 * 
 */
package com.mind_era.knime.util.interval;

import java.util.Arrays;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.mind_era.knime.common.util.interval.Interval;
import com.mind_era.knime.common.util.interval.Interval.DefaultInterval;

/**
 *
 * @author Gabor Bakos
 */
@SuppressWarnings("boxing")
@RunWith(Parameterized.class)
public class MultipleIntervalTests {
	private final NavigableMap<Interval<Double>, ?> map;
	private final Double value;
	private final boolean expected;
	
	/**
	 * @param map
	 * @param value
	 * @param expected
	 */
	public MultipleIntervalTests(NavigableMap<Interval<Double>, ?> map,
			Double value, boolean expected) {
		super();
		this.map = map;
		this.value = value;
		this.expected = expected;
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

	@Parameters
	public static List<Object[]> mapData() {
		return Arrays.asList(new Object[][] { { singleMap, 0.0, false },
				{ singleMap, 1.0, true }, { singleMap, 2.0, true },
				{ singleMap, 4.0, false }, { twoElemMap, 0.0, false },
				{ twoElemMap, 1.0, true }, { twoElemMap, 2.0, true },
				{ twoElemMap, 4.0, false }, { threeElemMap, 0.0, false },
				{ threeElemMap, 1.0, true }, { threeElemMap, 2.0, true },
				{ threeElemMap, 4.0, false }, { threeElemMap, 5.0, true },
				{ threeElemMap, 6.0, false } });
	}

	@Test
	public void mapTests() {
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

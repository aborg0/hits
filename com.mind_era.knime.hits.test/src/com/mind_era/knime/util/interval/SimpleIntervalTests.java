/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package com.mind_era.knime.util.interval;

import java.util.Arrays;
import java.util.List;

import javax.annotation.CheckReturnValue;
import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.mind_era.knime.common.util.interval.Interval;
import com.mind_era.knime.common.util.interval.Interval.DefaultInterval;

/**
 * Some tests to test {@link DefaultInterval} implementation of {@link Interval}
 * .
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
@SuppressWarnings("boxing")
@ParametersAreNonnullByDefault
@CheckReturnValue
@RunWith(Parameterized.class)
public class SimpleIntervalTests {
	private final Interval<Double> interval;
	private final Double value;
	private final boolean expected;
	
	/**
	 * @param interval
	 * @param value
	 * @param expected
	 */
	public SimpleIntervalTests(Interval<Double> interval, Double value,
			boolean expected) {
		super();
		this.interval = interval;
		this.value = value;
		this.expected = expected;
	}

	@Parameters
	public static List<Object[]> simpleData() {
		return Arrays.asList(new Object[][] {
				{ new DefaultInterval<Double>(1.0, 2.0, true, true), 1.5, true },
				{ new DefaultInterval<Double>(1.0, 2.0, true, true), .5, false },
				{ new DefaultInterval<Double>(1.0, 2.0, true, true), 2.5, false },
				{ new DefaultInterval<Double>(1.0, 2.0, true, true), 1.0, true },
				{ new DefaultInterval<Double>(1.0, 2.0, true, true), 2.0, true } });
	}

	@Test
	public void contains() {
		Assert.assertEquals(interval.contains(value), expected);
	}
}

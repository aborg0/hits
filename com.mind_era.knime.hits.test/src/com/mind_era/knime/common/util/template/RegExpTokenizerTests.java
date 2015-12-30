/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package com.mind_era.knime.common.util.template;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import javax.annotation.CheckReturnValue;
import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.mind_era.knime.common.util.template.Token;
import com.mind_era.knime.common.util.template.TokenizeException;
import com.mind_era.knime.common.util.template.Tokenizer;
import com.mind_era.knime.common.util.template.TokenizerFactory;

/**
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
@ParametersAreNonnullByDefault
@CheckReturnValue
@RunWith(Parameterized.class)
public class RegExpTokenizerTests extends TokenizerTests {
	private final String input;
	private final List<Token> expectedTokens;

	/**  */
	private static final Pattern UNDERSCORE_PATTERN = Pattern.compile("_");

	/**
	 * @return Simple test cases for split by {@code _}.
	 */
	@Parameters
	public static List<Object[]> simpleTests() {
		return Arrays.asList(new Object[][] {
				{ "", Collections.<Token> emptyList() },
				{ "_", Collections.<Token> emptyList() },
				{ "__", Collections.<Token> emptyList() },
				{ "a_", Collections.singletonList(simple("a", 0)) },
				{ "a__", Collections.singletonList(simple("a", 0)) },
				{ "_a_", Collections.singletonList(simple("a", 1)) },
				{ "_a__", Collections.singletonList(simple("a", 1)) },
				{ "__a_", Collections.singletonList(simple("a", 2)) },
				{ "__a__", Collections.singletonList(simple("a", 2)) },
				{
						"a__b",
						Arrays.asList(new Token[] { simple("a", 0),
								simple("b", 3) }) },
				{
						"a__b_",
						Arrays.asList(new Token[] { simple("a", 0),
								simple("b", 3) }) },
				{
						"a__b__",
						Arrays.asList(new Token[] { simple("a", 0),
								simple("b", 3) }) },
				{
						"_a__b",
						Arrays.asList(new Token[] { simple("a", 1),
								simple("b", 4) }) },
				{
						"_a__b_",
						Arrays.asList(new Token[] { simple("a", 1),
								simple("b", 4) }) },
				{
						"_a__b__",
						Arrays.asList(new Token[] { simple("a", 1),
								simple("b", 4) }) },

		});
	}

	/**
	 * 
	 */
	public RegExpTokenizerTests(String input, List<Token> expectedTokens) {
		this.input = input;
		this.expectedTokens = expectedTokens;
	}

	@Override
	protected Tokenizer create() {
		return new TokenizerFactory().createSplitTokenizer(UNDERSCORE_PATTERN,
				0);
	}

	/**
	 * 
	 * @param input
	 * @param expectedTokens
	 * @throws TokenizeException
	 */
	@Test
	public void simple()
			throws TokenizeException {
		Assert.assertEquals(create().parse(input), expectedTokens);
	}
}

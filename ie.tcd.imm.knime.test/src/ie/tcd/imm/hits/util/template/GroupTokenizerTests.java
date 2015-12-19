/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package ie.tcd.imm.hits.util.template;

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


/**
 * Some tests to test the
 * {@link TokenizerFactory#createGroupingTokenizer(Pattern, Pattern, int)
 * GroupingTokenizer} implementation of {@link Tokenizer}.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
@ParametersAreNonnullByDefault
@CheckReturnValue
@RunWith(Parameterized.class)
public class GroupTokenizerTests extends TokenizerTests.Group {
	private final String input;
	private final List<Token> expected;
	
	/**
	 * @param input
	 * @param expected
	 */
	public GroupTokenizerTests(String input, List<Token> expected) {
		super();
		this.input = input;
		this.expected = expected;
	}

	/**
	 * @return Some simple test cases.
	 */
	@Parameters
	public static List<Object[]> generateDefaultData() {
		return Arrays.asList(new Object[][] {
				{ "", Collections.<Token> emptyList() },
				{ "a", Collections.<Token> singletonList(simple("a", 0)) },
				{ "{", Collections.<Token> singletonList(simple("{", 0)) },
				{ "${}", Collections.<Token> singletonList(group("${", "}", 0)) },
				{ "a${}", Arrays.asList(simple("a", 0), group("${", "}", 1)) },
				{ "${a}", Collections.singletonList(group("${", "}", 0, "a")) },
				{ "${}a", Arrays.asList(group("${", "}", 0), simple("a", 3)) },
				{
						"${}${}",
						Arrays.asList((Token) group("${", "}", 0), group("${",
								"}", 3)) },
				{
						"a${}${}",
						Arrays.asList(new Token[] { simple("a", 0),
								group("${", "}", 1), group("${", "}", 4) }) },
				// { "${a}${}", Arrays.asList((Token) group("${", "}", 0, "a")),
				// group("${", "}", 4) },// Gives error in TestNG 5.9.0.2
				{
						"a${}b${}",
						Arrays.asList(simple("a", 0), group("${", "}", 1),
								simple("b", 4), group("${", "}", 5)) },
				{
						"${a}b${}",
						Arrays.asList(group("${", "}", 0, "a"), simple("b", 4),
								group("${", "}", 5)) },
				{
						"a${}${b}",
						Arrays.asList(simple("a", 0), group("${", "}", 1),
								group("${", "}", 4, "b")) },
		// { "${}a${}",
		// Arrays.asList(group("${", "}", 0), simple("a", 3)),
		// group("${", "}", 4) },// Gives error in TestNG 5.9.0.2
		});
	}

	/**
	 * 
	 * @param input
	 * @param expected
	 * @throws TokenizeException
	 */
	@Test
	public void defaultTests()
			throws TokenizeException {
		Assert.assertEquals(create().parse(input), expected);
	}

	/**
	 * 
	 * @param input
	 * @param expected
	 * @throws TokenizeException
	 */
	@Test
	public void shiftedDefaultTests() throws TokenizeException {
		final TokenizerFactory tokenizerFactory = new TokenizerFactory();
		Assert.assertEquals(tokenizerFactory.createGroupingTokenizer(
				Group.GROUP_OPEN_PATTERN, GROUP_CLOSE_PATTERN, 1).parse("x" + input),
				shift(expected, 1));
		Assert.assertEquals(
				tokenizerFactory.createGroupingTokenizer(GROUP_OPEN_PATTERN,
						GROUP_CLOSE_PATTERN, 2).parse("xy" + input), shift(
						expected, 2));
	}
}

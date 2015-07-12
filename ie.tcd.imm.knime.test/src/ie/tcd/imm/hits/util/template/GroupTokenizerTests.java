/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package ie.tcd.imm.hits.util.template;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import edu.umd.cs.findbugs.annotations.DefaultAnnotation;

/**
 * Some tests to test the
 * {@link TokenizerFactory#createGroupingTokenizer(Pattern, Pattern, int)
 * GroupingTokenizer} implementation of {@link Tokenizer}.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
@DefaultAnnotation( { Nonnull.class, CheckReturnValue.class })
public class GroupTokenizerTests extends TokenizerTests {
	/**  */
	private static final Pattern GROUP_CLOSE_PATTERN = Pattern.compile("\\}");
	/**  */
	private static final Pattern GROUP_OPEN_PATTERN = Pattern.compile("\\$\\{");
	/**  */
	private static final Pattern ABS_PATTERN = Pattern.compile("\\|");

	/**
	 * @return Some simple test cases.
	 */
	@DataProvider(name = "default")
	public static Object[][] generateDefaultData() {
		return new Object[][] {
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
		};
	}

	/**
	 * 
	 * @param input
	 * @param expected
	 * @throws TokenizeException
	 */
	@Test(dataProvider = "default")
	public void defaultTests(final String input, final List<Token> expected)
			throws TokenizeException {
		Assert.assertEquals(create().parse(input), expected);
	}

	/**
	 * 
	 * @param input
	 * @param expected
	 * @throws TokenizeException
	 */
	@Test(dataProvider = "default")
	public void shiftedDefaultTests(final String input,
			final List<Token> expected) throws TokenizeException {
		final TokenizerFactory tokenizerFactory = new TokenizerFactory();
		Assert.assertEquals(tokenizerFactory.createGroupingTokenizer(
				GROUP_OPEN_PATTERN, GROUP_CLOSE_PATTERN, 1).parse("x" + input),
				shift(expected, 1));
		Assert.assertEquals(
				tokenizerFactory.createGroupingTokenizer(GROUP_OPEN_PATTERN,
						GROUP_CLOSE_PATTERN, 2).parse("xy" + input), shift(
						expected, 2));
	}

	/**
	 * @return Some data that should throw {@link TokenizeException}.
	 */
	@DataProvider(name = "failingTests")
	public String[][] failingTests() {
		return new String[][] { { "${" }, { "}" }, { "}${" }, { "${${" },
				{ "a${" }, { "a}" }, { "a}${" }, { "a${${" }, { "${b" },
				{ "}b" }, { "}${b" }, { "${${b" }, { "a${b" }, { "a}b" },
				{ "a}${b" }, { "a${${b" }, { "a${b${c" }, };
	}

	/**
	 * 
	 * @param input
	 * @throws TokenizeException
	 */
	@Test(dataProvider = "failingTests", expectedExceptions = TokenizeException.class)
	public void simpleThrowsExceptions(final String input)
			throws TokenizeException {
		create().parse(input);
	}

	/**
	 * @return Some sample data with {@code |} delimiter.
	 */
	@DataProvider(name = "absDelimiters")
	public static Object[][] sameDelimiterData() {
		return new Object[][] {
				{ "", Collections.<Token> emptyList() },
				{ "a", Collections.<Token> singletonList(simple("a", 0)) },
				{ "{", Collections.<Token> singletonList(simple("{", 0)) },
				{ "||", Collections.<Token> singletonList(group("|", "|", 0)) },
				{ "a||", Arrays.asList(simple("a", 0), group("|", "|", 1)) },
				{ "|a|", Collections.singletonList(group("|", "|", 0, "a")) },
				{ "||a", Arrays.asList(group("|", "|", 0), simple("a", 2)) },
				{
						"||||",
						Arrays.asList((Token) group("|", "|", 0), group("|",
								"|", 2)) },
				{
						"a||||",
						Arrays.asList(new Token[] { simple("a", 0),
								group("|", "|", 1), group("|", "|", 3) }) },
				// { "|a|||", Arrays.asList((Token) group("|", "|", 0, "a")),
				// group("|", "|", 3) },
				{
						"a||b||",
						Arrays.asList(simple("a", 0), group("|", "|", 1),
								simple("b", 3), group("|", "|", 4)) },
				{
						"|a|b||",
						Arrays.asList(group("|", "|", 0, "a"), simple("b", 3),
								group("|", "|", 4)) },
				{
						"a|||b|",
						Arrays.asList(simple("a", 0), group("|", "|", 1),
								group("|", "|", 3, "b")) },
		// { "||a||", Arrays.asList(group("|", "|", 0), simple("a", 2)),
		// group("|", "|", 3) },
		};
	}

	/**
	 * 
	 * @param input
	 * @param tokens
	 * @throws TokenizeException
	 */
	@Test(dataProvider = "absDelimiters")
	public void sameDelimiterTest(final String input, final List<Token> tokens)
			throws TokenizeException {
		Assert.assertEquals(new TokenizerFactory().createGroupingTokenizer(
				ABS_PATTERN, ABS_PATTERN).parse(input), tokens);
	}

	@Override
	protected Tokenizer create() {
		return new TokenizerFactory().createGroupingTokenizer(
				GROUP_OPEN_PATTERN, GROUP_CLOSE_PATTERN);
	}
}

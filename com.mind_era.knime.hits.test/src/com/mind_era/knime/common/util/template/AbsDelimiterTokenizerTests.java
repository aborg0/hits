/*
 * 
 */
package com.mind_era.knime.common.util.template;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.mind_era.knime.common.util.template.Token;
import com.mind_era.knime.common.util.template.TokenizeException;
import com.mind_era.knime.common.util.template.TokenizerFactory;

/**
 *
 * @author Gabor Bakos
 */
@RunWith(Parameterized.class)
public class AbsDelimiterTokenizerTests extends TokenizerTests.Group {
	/**  */
	private static final Pattern ABS_PATTERN = Pattern.compile("\\|");
	
	private final String input;
	private final List<Token> tokens;
	
	/**
	 * @param input
	 * @param tokens
	 */
	public AbsDelimiterTokenizerTests(String input, List<Token> tokens) {
		super();
		this.input = input;
		this.tokens = tokens;
	}

	/**
	 * @return Some sample data with {@code |} delimiter.
	 */
	@Parameters
	public static List<Object[]> sameDelimiterData() {
		return Arrays.asList(new Object[][] {
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
		});
	}

	/**
	 * 
	 * @param input
	 * @param tokens
	 * @throws TokenizeException
	 */
	@Test
	public void sameDelimiterTest()
			throws TokenizeException {
		Assert.assertEquals(new TokenizerFactory().createGroupingTokenizer(
				ABS_PATTERN, ABS_PATTERN).parse(input), tokens);
	}
}

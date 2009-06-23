/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package ie.tcd.imm.hits.util.template;

import ie.tcd.imm.hits.util.template.impl.AbstractTokenizer;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import edu.umd.cs.findbugs.annotations.DefaultAnnotation;

/**
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
@DefaultAnnotation( { Nonnull.class, CheckReturnValue.class })
public class RegExpTokenizerTests extends TokenizerTests {

	@DataProvider(name = "simple")
	public static Object[][] simpleTests() {
		return new Object[][] {
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

		};
	}

	/**
	 * 
	 */
	public RegExpTokenizerTests() {
		// TODO Auto-generated constructor stub
	}

	@Override
	protected Tokenizer create() {
		return new AbstractTokenizer(0, "_");
	}

	@Test(dataProvider = "simple")
	public void simple(final String input, final List<Token> expectedTokens)
			throws TokenizeException {
		Assert.assertEquals(create().parse(input), expectedTokens);
	}
}

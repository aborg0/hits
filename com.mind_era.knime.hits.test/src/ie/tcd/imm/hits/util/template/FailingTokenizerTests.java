/*
 * 
 */
package ie.tcd.imm.hits.util.template;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import ie.tcd.imm.hits.util.template.TokenizerTests.Group;

/**
 *
 * @author Gabor Bakos
 */
@RunWith(Parameterized.class)
public class FailingTokenizerTests extends Group {
	private final String input;

	/**
	 * String input
	 */
	public FailingTokenizerTests(String input) {
		this.input = input;
	}


	/**
	 * @return Some data that should throw {@link TokenizeException}.
	 */
	@Parameters
	public static List<String[]> failingTests() {
		return Arrays.asList(new String[][] { { "${" }, { "}" }, { "}${" }, { "${${" },
				{ "a${" }, { "a}" }, { "a}${" }, { "a${${" }, { "${b" },
				{ "}b" }, { "}${b" }, { "${${b" }, { "a${b" }, { "a}b" },
				{ "a}${b" }, { "a${${b" }, { "a${b${c" }, });
	}

	/**
	 * 
	 * @param input
	 * @throws TokenizeException
	 */
	@Test(expected = TokenizeException.class)
	public void simpleThrowsExceptions()
			throws TokenizeException {
		create().parse(input);
	}

}

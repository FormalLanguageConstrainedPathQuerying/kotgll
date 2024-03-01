package org.junit.tests.assertion;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collection;

import org.junit.ComparisonFailure;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class ComparisonFailureTest {
		
	private String expected, actual, message;
	
	public ComparisonFailureTest(String e, String a, String m) {
		expected = e;
		actual = a;
		message = m;
	}
	
	@Parameters(name = "compact-msg-{index}, exp=\"{1}\"")
	public static Collection<Object[]> data() {
		return Arrays.asList(new Object[][] {
			{ "a", "b", "expected:<[a]> but was:<[b]>" },
				
			{ "ba", "bc", "expected:<b[a]> but was:<b[c]>" },
				
			{ "ab", "cb", "expected:<[a]b> but was:<[c]b>" },
				
			{ "abc", "adc", "expected:<a[b]c> but was:<a[d]c>" },
			
			{ "ab", "abc", "expected:<ab[]> but was:<ab[c]>" },

			{ "abc", "ab", "expected:<ab[c]> but was:<ab[]>" },
			
			{ "abc", "abbc", "expected:<ab[]c> but was:<ab[b]c>" },

			{ "01234567890123456789PRE:hello:POST", 
				"01234567890123456789PRE:world:POST",
				"expected:<...4567890123456789PRE:[hello]:POST> but was:<...4567890123456789PRE:[world]:POST>" },
					
			{ "PRE:hello:01234567890123456789POST",
				"PRE:world:01234567890123456789POST",
				"expected:<PRE:[hello]:0123456789012345678...> but was:<PRE:[world]:0123456789012345678...>"	
			},
					
			{ "S&P500", "0", "expected:<[S&P50]0> but was:<[]0>" },
			
			{ "", "a", "expected:<[]> but was:<[a]>" },

			{ "a", "", "expected:<[a]> but was:<[]>" }

		});	
	}

	@Test
	public void compactFailureMessage() {
		ComparisonFailure failure = new ComparisonFailure("", expected, actual);
		assertEquals(message, failure.getMessage());
	}
	
}

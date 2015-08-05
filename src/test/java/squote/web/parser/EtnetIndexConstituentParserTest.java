package squote.web.parser;

import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

public class EtnetIndexConstituentParserTest {	
	@Test
	public void parse_givenHSIUrl_shouldParseSomeCodes() {
		List<String> list = EtnetIndexConstituentParser.parse("http://www.etnet.com.hk/www/tc/stocks/indexes_detail.php?subtype=HSI");
		assertTrue(list.size() > 30);
		assertTrue(list.stream().anyMatch(x->"2318".equals(x)));
	}
}
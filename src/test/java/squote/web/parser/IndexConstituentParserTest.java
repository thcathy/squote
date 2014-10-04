package squote.web.parser;
 
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;
 
public class IndexConstituentParserTest {
	MSCIChinaConstituentParser parser = new MSCIChinaConstituentParser();
 
	@Test
	public void testGetMSCIConstituent() {
		List<String> result = parser.parse().get();
		assertTrue("MSCI China index should contain over 50 stocks", result.size() > 50);
		assertTrue("MSCI China index should contain 941",result.contains("941"));
		assertTrue("MSCI China index should contain 2628",result.contains("2628"));
		assertTrue("MSCI China index should contain 857",result.contains("857"));
		assertTrue("MSCI China index should contain 992",result.contains("992"));
	}
}
package squote.web.parser;
 
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;
 
public class ISharesConstituentParserTest {
	 
	@Test
	public void testGetMSCIChinaConstituent() {
		List<String> result = ISharesConstituentParser.parseMSCIChina();
		assertTrue("MSCI China index should contain over 50 stocks", result.size() > 50);
		assertTrue("MSCI China index should contain 941",result.contains("941"));
		assertTrue("MSCI China index should contain 2628",result.contains("2628"));
		assertTrue("MSCI China index should contain 857",result.contains("857"));
		assertTrue("MSCI China index should contain 992",result.contains("992"));
	}
	
	//@Test
	public void testGetMSCIHKConstituent() {
		List<String> result = ISharesConstituentParser.parseMSCIHK();
		assertTrue("MSCI China index should contain 5",result.contains("5"));
		assertTrue("MSCI China index should contain 2",result.contains("2"));
		assertTrue("MSCI China index should contain 388",result.contains("388"));
		assertTrue("MSCI China index should contain over 30 stocks", result.size() > 30);
		
	}
}
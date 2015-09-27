package squote.web.parser;
 
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;
 
public class ISharesConstituentParserTest {
	 
	@Test
	public void parseMSCIChina_ShouldReturnCorrectStockCodes() {
		List<String> result = ISharesConstituentParser.parseMSCIChina();
		assertTrue("MSCI China index should contain over 50 stocks", result.size() > 50);
		assertTrue("MSCI China index should contain 941",result.contains("941"));
		assertTrue("MSCI China index should contain 2628",result.contains("2628"));
		assertTrue("MSCI China index should contain 857",result.contains("857"));
		assertTrue("MSCI China index should contain 992",result.contains("992"));
		assertFalse("Should not return any --", result.contains("--"));	
	}
	
	@Test
	public void parseMSCIHK_ShouldReturnCorrectStockCodes() {
		List<String> result = ISharesConstituentParser.parseMSCIHK();
		assertTrue("MSCI HK index should contain 11",result.contains("11"));
		assertTrue("MSCI HK index should contain 2",result.contains("2"));
		assertTrue("MSCI HK index should contain 388",result.contains("388"));
		assertTrue("MSCI HK index should contain over 30 stocks", result.size() > 30);
		assertFalse("Should not return any --", result.contains("--"));
	}
}
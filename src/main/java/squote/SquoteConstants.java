package squote;

import java.util.List;

import squote.web.parser.EtnetIndexConstituentParser;
import squote.web.parser.MSCIChinaConstituentParser;

public class SquoteConstants {
	public enum Side {
		BUY("買入", 1), SELL("沽出", -1);

		final public String chinese;
		final public int factor;

		Side(String chinese, int factor) {
			this.chinese = chinese;
			this.factor = factor;
		}
	}

	public enum IndexCode {
		HSI("Hang Seng Index", EtnetIndexConstituentParser.parse("http://www.etnet.com.hk/www/tc/stocks/indexes_detail.php?subtype=HSI")), 
		HSCEI("HS China Enterprises Index", EtnetIndexConstituentParser.parse("http://www.etnet.com.hk/www/tc/stocks/indexes_detail.php?subtype=cei")),
		HCCI("HS China Corp Index", EtnetIndexConstituentParser.parse("http://www.etnet.com.hk/www/tc/stocks/indexes_detail.php?subtype=cci")),
		MSCIChina("MSCI China Index", MSCIChinaConstituentParser.parse());
		
		final public String name;
		final public List<String> constituents;
		
		IndexCode(String name, List<String> constituents) {			
			this.name = name;
			this.constituents = constituents;
		}
	}	
}

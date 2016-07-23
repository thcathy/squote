package squote.web.parser;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thc.util.HttpClientImpl;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.CharEncoding.UTF_8;
 
public class ISharesConstituentParser {
	private static final Logger log = LoggerFactory.getLogger(ISharesConstituentParser.class);
	private static final ObjectReader jsonReader = new ObjectMapper().readerFor(Map.class);
	
	@SuppressWarnings("unchecked")	
	public static List<String> parseMSCIChina() {		
		String URL = "https://www.blackrock.com/hk/en/terms-and-conditions?targetUrl=%2Fhk%2Fen%2Fproducts%2F251576%2Fishares-msci-china-index-etf%2F1440663547017.ajax%3Ftab%3Dall%26fileType%3Djson&action=ACCEPT";

		try {
			Map<String, ?> value = jsonReader.readValue(new HttpClientImpl(UTF_8).newInstance().makeGetRequest(URL));
			List<Map<String,Object>> data = (List<Map<String, Object>>) value.get("aaData");
			List<String> results = data.stream()
									.filter(m -> m.get("colExchangeCode").equals("XHKG"))
									.map(m -> (String)m.get("colTicker"))
									.collect(Collectors.toList());
			return results;
		} catch (Exception e) {
			log.error("Fail to retrieve list of MSCI HK Constituents",e);
			return Collections.emptyList();
		}	
	}
  	
	@SuppressWarnings("unchecked")
	public static List<String> parseMSCIHK() {
		String URL = "https://www.ishares.com/us/products/239657/ishares-msci-hong-kong-etf/1467271812596.ajax?tab=all&fileType=json";

		try {
			Map<String, ?> value = jsonReader.readValue(new HttpClientImpl(UTF_8).newInstance().makeGetRequest(URL));
			List<Map<String,Object>> data = (List<Map<String, Object>>) value.get("aaData");
			List<String> results = data.stream()					
									.filter(m -> m.get("colExchange").equals("Hong Kong Exchanges And Clearing Ltd"))
									.map(m -> (String)m.get("colTicker"))
									.collect(Collectors.toList());
			return results;
		} catch (Exception e) {
			log.error("Fail to retrieve list of MSCI HK Constituents",e);
			return Collections.emptyList();
		}	
	}
}
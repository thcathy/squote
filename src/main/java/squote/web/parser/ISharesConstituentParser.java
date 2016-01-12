package squote.web.parser;
 
import static org.apache.commons.lang3.CharEncoding.UTF_8;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import thc.util.HttpClientImpl;
 
public class ISharesConstituentParser {
	private static final Logger log = LoggerFactory.getLogger(ISharesConstituentParser.class);
	
	@SuppressWarnings("unchecked")	
	public static List<String> parseMSCIChina() {		
		String URL = "https://www.blackrock.com/hk/en/terms-and-conditions?targetUrl=%2Fhk%2Fen%2Fproducts%2F251576%2Fishares-msci-china-index-etf%2F1440663547017.ajax%3Ftab%3Dall%26fileType%3Djson&action=ACCEPT";		
		ObjectMapper mapper = new ObjectMapper();
		
		try {			
			List<Map<String,Object>> data = (List<Map<String, Object>>) mapper.readValue(new HttpClientImpl(UTF_8).newInstance().makeGetRequest(URL), Map.class).get("aaData");			
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
		String URL = "https://www.ishares.com/us/products/239657/ishares-msci-hong-kong-etf/1449138789749.ajax?tab=all&fileType=json";		
	
		ObjectMapper mapper = new ObjectMapper();
		
		try {			
			List<Map<String,Object>> data = (List<Map<String, Object>>) mapper.readValue(new HttpClientImpl(UTF_8).newInstance().makeGetRequest(URL), Map.class).get("aaData");			
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
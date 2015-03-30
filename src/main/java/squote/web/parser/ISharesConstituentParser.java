package squote.web.parser;
 
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import thc.util.HttpClientImpl;
 
public class ISharesConstituentParser {
	private static final Logger log = LoggerFactory.getLogger(ISharesConstituentParser.class);
	
	public static String MSCIChinaConstituentsURL = "https://www.blackrock.com/hk/en/terms-and-conditions?targetUrl=%2Fhk%2Fen%2Fproducts%2F251576%2Fishares-msci-china-index-etf%2F1404292000448.ajax%3FfileType%3Dcsv%26fileName%3D2801_holdings%26dataType%3Dfund&action=ACCEPT";
		
	public static List<String> parseMSCIChina() {		
		try {
			List<String> lines = IOUtils.readLines(new HttpClientImpl("utf-8").newInstance().makeGetRequest(MSCIChinaConstituentsURL));
			List<String> results = lines.stream()
				.map(l->l.split("\",\""))
				.map(l->{System.out.println(Arrays.toString(l)); return l;})
				.filter(cs->cs.length > 4 && cs[3].contains("HKG"))
				.map(cs->cs[0].replaceAll("\"", ""))
				.collect(Collectors.toList());
			return results;
		} catch (Exception e) {
			log.error("Fail to retrieve list of MSCI China Constituents",e);
			return Collections.emptyList();
		}		
	}
  	
	public static List<String> parseMSCIHK() {
		String URL = "http://www.ishares.com/us/products/239657/ishares-msci-hong-kong-etf/1395165510754.ajax?fileType=csv&fileName=EWH_holdings&dataType=fund";
		
		try {
			List<String> lines = IOUtils.readLines(new HttpClientImpl("utf-8").newInstance().makeGetRequest(URL));
			List<String> results = lines.stream()				
				.map(line->line.split("\",\""))				
				.filter(line -> line.length > 12 && line[11].contains("Hong Kong Exchanges And Clearing Ltd"))
				.map(cs->cs[0].replaceAll("\"", ""))
				.collect(Collectors.toList());
			return results;
		} catch (Exception e) {
			log.error("Fail to retrieve list of MSCI HK Constituents",e);
			return Collections.emptyList();
		}	
	}
}
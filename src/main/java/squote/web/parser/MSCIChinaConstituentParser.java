package squote.web.parser;
 
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import thc.util.HttpClient;
 
public class MSCIChinaConstituentParser extends WebParser<List<String>> {
	private final Logger log = LoggerFactory.getLogger(getClass());
	
	public static String MSCIChinaConstituentsURL = "https://www.blackrock.com/hk/en/terms-and-conditions?targetUrl=%2Fhk%2Fen%2Fproducts%2F251576%2Fishares-msci-china-index-etf%2F1404292000448.ajax%3FfileType%3Dcsv%26fileName%3D2801_holdings%26dataType%3Dfund&action=ACCEPT";
	
	@Override
	public Optional<List<String>> parse() {		
		try {
			List<String> lines = IOUtils.readLines(new HttpClient("utf-8").makeGetRequest(MSCIChinaConstituentsURL));
			List<String> results = extractDataRow(lines).stream()
				.map(l->l.split(","))
				.filter(cs->cs[3].contains("HKG"))
				.map(cs->cs[0].replaceAll("\"", ""))
				.collect(Collectors.toList());
			return Optional.of(results);
		} catch (Exception e) {
			log.error("Fail to retrieve list of MSCI China Constituents",e);
			return Optional.empty();
		}		
	}
  
	private List<String> extractDataRow(List<String> lines) {
		lines = lines.subList(3, lines.size() - 2);
		return lines;
	}
}
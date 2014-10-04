package squote.web.parser;
 
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import thc.util.HttpClient;

import com.google.common.base.Optional;
 
public class MSCIChinaConstituentParser extends WebParser<List<String>> {
	private final Logger log = LoggerFactory.getLogger(getClass());
	
	public static String MSCIChinaConstituentsURL = "https://www.blackrock.com/hk/en/terms-and-conditions?targetUrl=%2Fhk%2Fen%2Fproducts%2F251576%2Fishares-msci-china-index-etf%2F1404292000448.ajax%3FfileType%3Dcsv%26fileName%3D2801_holdings%26dataType%3Dfund&action=ACCEPT";
	
	@Override
	public Optional<List<String>> parse() {
		List<String> results = new ArrayList<String>();
		try {
			List<String> lines = IOUtils.readLines(new HttpClient("utf-8").makeGetRequest(MSCIChinaConstituentsURL));
			lines = extractDataRow(lines);
			for (String line : lines) addIfHKGStock(results, line);
		} catch (Exception e) {
			log.error("Fail to retrieve list of MSCI China Constituents",e);
		}
		return Optional.of(results);
	}
 
	private void addIfHKGStock(List<String> results, String line) {
		String[] columns = line.split(",");
		if (columns[3].contains("HKG")) results.add(columns[0].replaceAll("\"", ""));
	}
 
	private List<String> extractDataRow(List<String> lines) {
		lines = lines.subList(3, lines.size() - 2);
		return lines;
	}
}
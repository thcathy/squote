package squote.web.parser;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.message.BasicHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import squote.domain.StockQuote;
import thc.util.HttpClient;
import thc.util.HttpClientImpl;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

public class SinaStockQuoteParser implements StockQuoteParser {
	protected final Logger log = LoggerFactory.getLogger(getClass());

	public static String SINA_STOCK_QUOTE_URL = "http://sina.com.hk/p/api/aastock/Stock/index/";
	
	private static final Header XML_HTTP_REQUEST = new BasicHeader("X-Requested-With", "XMLHttpRequest");
    private static final ObjectReader jsonReader = new ObjectMapper().readerFor(Map.class);

	private final String code;

	public SinaStockQuoteParser(String code) { this.code = code; }
	
	@Override
	public StockQuote getStockQuote() {
		StockQuote quote = new StockQuote(code);
		HttpClient client = new HttpClientImpl("UTF-8").newInstance();

		try
		{			
			InputStream stream = client.makeGetRequest(concatUrlWithParam(), XML_HTTP_REQUEST);
			parseResponse(quote, stream);
			
		} catch (Exception e) {
			log.error("Cannot parse stock code: {}", code);
		}
		return quote;
	}

	@SuppressWarnings("unchecked")
	private void parseResponse(StockQuote quote, InputStream stream)
			throws IOException {

		Map<String, String> value = jsonReader.readValue(stream);
		
		quote.setStockName(value.get("Desp"));
		quote.setPrice(value.get("Last"));
		quote.setChangeAmount(value.get("Change"));
		quote.setChange(value.get("PctChange"));
		quote.setHigh(value.get("High"));
		quote.setLow(value.get("Low"));
		quote.setPe(value.get("PERatio"));
		quote.setYield(value.get("Yield") + "%");
		quote.setLastUpdate(value.get("LastUpdate"));
		quote.setYearHigh(value.get("YearHigh"));
		quote.setYearLow(value.get("YearLow"));
	}

	private String concatUrlWithParam() {
		return SINA_STOCK_QUOTE_URL + StringUtils.leftPad(code, 5, '0');
	}

}

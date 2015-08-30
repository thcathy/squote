package thc.util;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.input.NullInputStream;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpClientImpl implements HttpClient {
	protected final Logger log = LoggerFactory.getLogger(getClass());
		
	private BasicCookieStore cookieStore = new BasicCookieStore();
	private CloseableHttpClient httpclient;
	private String encoding = "utf-8";
		
	public HttpClientImpl() {
	}
	
	/* (non-Javadoc)
	 * @see thc.util.HttpClient#newInstance()
	 */
	@Override
	public HttpClient newInstance() {
		HttpClientImpl instance = new HttpClientImpl();
		
		instance.httpclient = HttpClients.custom()
					.setDefaultCookieStore(cookieStore).setUserAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_10_4) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/44.0.2403.130 Safari/537.36")
					.build();
		
		instance.encoding = this.encoding;
		return instance;
	}
	
	public HttpClientImpl(String encoding) {
		this();
		this.encoding = encoding;
	}
	
	@Override
	public InputStream makeGetRequest(String url) {
		return makeRequest(new HttpGet(url));
	}
	
	@Override
	public InputStream makeRequest(HttpUriRequest request) {
		try {
			CloseableHttpResponse response1 = httpclient.execute(request);
		    HttpEntity entity = response1.getEntity();		    
		    		    
		    log.debug("Request status: {}", response1.getStatusLine());
		    log.debug("Request cookies: {}", cookieStore.getCookies());
		    
		    if (response1.getStatusLine().getStatusCode() < 300) return entity.getContent();
		} catch (Exception e) {
			log.info("Cannot make request",e);
		}
		return new NullInputStream(0);
	}
	
	@Override
	public Document getDocument(String url) {
		try {
			return Jsoup.parse(makeGetRequest(url), encoding, url);
		} catch (IOException e) {
			throw new RuntimeException("Fail to retrieve url: " + url + " with encoding: " + encoding);
		}
	}
}
package thc.util;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.input.NullInputStream;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpClient {
	protected final Logger log = LoggerFactory.getLogger(getClass());
	
	private final BasicCookieStore cookieStore = new BasicCookieStore();
	private final CloseableHttpClient httpclient;
	private String encoding = "utf-8";
	
	public HttpClient() {
		httpclient = HttpClients.custom().setDefaultCookieStore(cookieStore).setUserAgent("Mozilla/4.0 (compatible; MSIE 7.0; Windows NT 6.0; Trident/5.0)").build();
	}
	
	public HttpClient(String encoding) {
		this();
		this.encoding = encoding;
	}
	
	public InputStream makeGetRequest(String url) {
		HttpGet httpget = new HttpGet(url);
		
		try {
			CloseableHttpResponse response1 = httpclient.execute(httpget);
		    HttpEntity entity = response1.getEntity();		    
		    		    
		    log.debug("Request status: {}", response1.getStatusLine());
		    log.debug("Request cookies: {}", cookieStore.getCookies());
		    
		    return entity.getContent();
		} catch (Exception e) {			
			return new NullInputStream(0);
		}
	}
	
	public Document getDocument(String url) {
		try {
			return Jsoup.parse(makeGetRequest(url), encoding, url);
		} catch (IOException e) {
			throw new RuntimeException("Fail to retrieve url: " + url);
		}
	}
}

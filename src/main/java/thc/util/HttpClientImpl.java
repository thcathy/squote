package thc.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.input.NullInputStream;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.message.BasicHeader;
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
	
	private List<Header> defaultHeader() {
		ArrayList<Header> headers = new ArrayList<Header>();
	
		headers.add(new BasicHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8"));
		headers.add(new BasicHeader("Connection", "keep-alive"));
		headers.add(new BasicHeader("Accept-Encoding", "gzip, deflate, sdch"));
		headers.add(new BasicHeader("Accept-Language", "zh-TW,zh;q=0.8,en-US;q=0.6,en;q=0.4"));
			
		return headers;
	}
	
	/* (non-Javadoc)
	 * @see thc.util.HttpClient#newInstance()
	 */
	@Override
	public HttpClient newInstance() {
		HttpClientImpl instance = new HttpClientImpl();
		
		instance.httpclient = HttpClients.custom()
					.setDefaultCookieStore(cookieStore).setUserAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_10_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/44.0.2403.130 Safari/537.36")
					.setDefaultHeaders(defaultHeader())
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
			log.debug("Request url: {}", request.getURI());
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

	@Override
	public void setCookie(String name, String value, String path, String domain) {
		BasicClientCookie c = new BasicClientCookie(name, value);
		c.setPath(path);
		c.setDomain(domain);
		cookieStore.addCookie(c);
	}
}
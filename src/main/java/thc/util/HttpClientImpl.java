package thc.util;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.input.NullInputStream;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpClientImpl implements HttpClient {
	protected final Logger log = LoggerFactory.getLogger(getClass());
	
	private static final boolean hasProxyServer;
	private static final boolean hasProxyUser;
	private static final String proxyHost;
	private static final String proxyPort;
	private static final String proxyUsername;
	private static final String proxyPassword;
	
	private BasicCookieStore cookieStore = new BasicCookieStore();
	private CloseableHttpClient httpclient;
	private String encoding = "utf-8";
	
	static {		
		proxyHost = System.getProperty("http.proxyHost");
		proxyPort = System.getProperty("http.proxyPort");
		proxyUsername = System.getProperty("http.proxyUsername");
		proxyPassword = System.getProperty("http.proxyPassword");
		hasProxyServer = hasProxyServer();
		hasProxyUser = hasProxyServer && hasProxyUser();
	}
	
	public HttpClientImpl() {
	}
	
	/* (non-Javadoc)
	 * @see thc.util.HttpClient#newInstance()
	 */
	@Override
	public HttpClient newInstance() {
		HttpClientImpl instance = new HttpClientImpl();
		CredentialsProvider credsProvider = new BasicCredentialsProvider();
		if (hasProxyUser) {
	        credsProvider.setCredentials(
	                new AuthScope(proxyHost, Integer.valueOf(proxyPort)),
	                new UsernamePasswordCredentials(proxyUsername, proxyPassword));
		}
		
		if (hasProxyServer) {
			instance.httpclient = HttpClients.custom().setProxy(new HttpHost(proxyHost,Integer.valueOf(proxyPort))).setDefaultCredentialsProvider(credsProvider)
					.setDefaultCookieStore(cookieStore).setUserAgent("Mozilla/4.0 (compatible; MSIE 7.0; Windows NT 6.0; Trident/5.0)").build();
		} else {
			instance.httpclient = HttpClients.custom()
					.setDefaultCookieStore(cookieStore).setUserAgent("Mozilla/4.0 (compatible; MSIE 7.0; Windows NT 6.0; Trident/5.0)").build();
		}
		instance.encoding = this.encoding;
		return instance;
	}
	
	private static boolean hasProxyServer() {
		return StringUtils.isNotBlank(proxyHost) && NumberUtils.isDigits(proxyPort);
	}

	private static boolean hasProxyUser() {
		return StringUtils.isNotBlank(proxyUsername) && StringUtils.isNotBlank(proxyPassword);
	}

	public HttpClientImpl(String encoding) {
		this();
		this.encoding = encoding;
	}
	
	/* (non-Javadoc)
	 * @see thc.util.HttpClient#makeGetRequest(java.lang.String)
	 */
	@Override
	public InputStream makeGetRequest(String url) {
		HttpGet httpget = new HttpGet(url);
		
		try {
			CloseableHttpResponse response1 = httpclient.execute(httpget);
		    HttpEntity entity = response1.getEntity();		    
		    		    
		    log.debug("Request status: {}", response1.getStatusLine());
		    log.debug("Request cookies: {}", cookieStore.getCookies());
		    
		    if (response1.getStatusLine().getStatusCode() < 300) return entity.getContent();
		} catch (Exception e) {
		}
		return new NullInputStream(0);
	}
	
	/* (non-Javadoc)
	 * @see thc.util.HttpClient#getDocument(java.lang.String)
	 */
	@Override
	public Document getDocument(String url) {
		try {
			return Jsoup.parse(makeGetRequest(url), encoding, url);
		} catch (IOException e) {
			throw new RuntimeException("Fail to retrieve url: " + url + " with encoding: " + encoding);
		}
	}
}
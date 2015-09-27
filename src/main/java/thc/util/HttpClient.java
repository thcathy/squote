package thc.util;

import java.io.InputStream;

import org.apache.http.client.methods.HttpUriRequest;
import org.jsoup.nodes.Document;

public interface HttpClient {

	public HttpClient newInstance();

	public InputStream makeGetRequest(String url);

	public Document getDocument(String url);

	InputStream makeRequest(HttpUriRequest request);
	
	public void setCookie(String name, String value, String path, String domain);
}
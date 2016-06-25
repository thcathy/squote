package thc.util;

import java.io.InputStream;

import org.apache.http.Header;
import org.apache.http.client.methods.HttpUriRequest;
import org.jsoup.nodes.Document;

public interface HttpClient {

	public HttpClient newInstance();

	public InputStream makeGetRequest(String url, Header... headers);

	public Document getDocument(String url);

	InputStream makeRequest(HttpUriRequest request, Header... headers);
	
	public void setCookie(String name, String value, String path, String domain);
}
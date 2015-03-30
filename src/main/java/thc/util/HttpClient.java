package thc.util;

import java.io.InputStream;

import org.jsoup.nodes.Document;

public interface HttpClient {

	public HttpClient newInstance();

	public InputStream makeGetRequest(String url);

	public Document getDocument(String url);

}
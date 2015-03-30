package squote.service;

import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.apache.http.auth.UsernamePasswordCredentials;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.internal.matchers.Any;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.github.sendgrid.SendGrid;

import squote.SpringQuoteWebApplication;
import thc.util.HttpClient;
import thc.util.HttpClientImpl;

@RunWith(MockitoJUnitRunner.class)
public class CheckWebServiceTest {
	@Mock SendGrid sendGrid;
	
	@Test
	public void check_GivenUrlsCanVisit_ShouldNotSendEmail() {
		// Given
		String url = "http://www.google.com/";
		HttpClient httpClient = mock(HttpClient.class, RETURNS_DEEP_STUBS);
		when(httpClient.newInstance().makeGetRequest(url)).thenReturn(new ByteArrayInputStream("".getBytes() ));
		CheckWebService checker = new CheckWebService.Builder()
			.checkUrls(new String[] {url})
			.fromEmail("a").toEmail("b")
			.sendGrid(sendGrid)		
			.httpClient(httpClient)
			.build();
		
		// Run
		checker.check();
		
		// Verify
		verifyZeroInteractions(sendGrid);
	}
	
	@Test
	public void check_GivenUrlsCannotVisit_ShouldSendEmailPerUrl() {
		// Given
		String url1 = "abc.funfunspell.com";
		String url2 = "http://aklerj.com/";
		HttpClient httpClient = mock(HttpClient.class, RETURNS_DEEP_STUBS);
		when(httpClient.newInstance().makeGetRequest(Mockito.any())).thenReturn(new ByteArrayInputStream("".getBytes() ));
		CheckWebService checker = new CheckWebService.Builder()
			.checkUrls(new String[] {url1, url2})
			.fromEmail("a").toEmail("b")
			.sendGrid(sendGrid)	
			.httpClient(new HttpClientImpl())
			.build();
		
		// Run
		checker.check();
		
		// Verify
		Mockito.verify(sendGrid, times(2)).setFrom(Matchers.eq("a"));
		Mockito.verify(sendGrid, times(2)).addTo(Matchers.eq("b"));
		Mockito.verify(sendGrid, times(1)).setSubject(Matchers.contains(url1));
		Mockito.verify(sendGrid, times(1)).setSubject(Matchers.contains(url2));
		Mockito.verify(sendGrid, times(2)).send();
	}
}

package squote.service;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verifyZeroInteractions;

import org.apache.http.auth.UsernamePasswordCredentials;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.github.sendgrid.SendGrid;

import squote.SpringQuoteWebApplication;

@RunWith(MockitoJUnitRunner.class)
public class CheckWebTest {
	@Mock SendGrid sendGrid;
	
	@Test
	public void check_GivenUrlsCanVisit_ShouldNotSendEmail() {
		// Given
		CheckWebService checker = new CheckWebService.Builder()
			.checkUrls(new String[] {"http://www.google.com/"})
			.fromEmail("a").toEmail("b")
			.sendGrid(sendGrid)			
			.build();
		
		checker.check();
		
		// Verify
		verifyZeroInteractions(sendGrid);
	}
	
	@Test
	public void check_GivenUrlsCannotVisit_ShouldSendEmailPerUrl() {
		// Given
		String url1 = "abc.funfunspell.com";
		String url2 = "http://aklerj.com/";
		CheckWebService checker = new CheckWebService.Builder()
			.checkUrls(new String[] {url1, url2})
			.fromEmail("a").toEmail("b")
			.sendGrid(sendGrid)			
			.build();
		
		checker.check();
		
		// Verify
		Mockito.verify(sendGrid, times(2)).setFrom(Matchers.eq("a"));
		Mockito.verify(sendGrid, times(2)).addTo(Matchers.eq("b"));
		Mockito.verify(sendGrid, times(1)).setSubject(Matchers.contains(url1));
		Mockito.verify(sendGrid, times(1)).setSubject(Matchers.contains(url2));
		Mockito.verify(sendGrid, times(2)).send();
	}
}

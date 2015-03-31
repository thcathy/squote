package squote.service;

import static org.apache.commons.lang3.Validate.notBlank;
import static org.apache.commons.lang3.Validate.notEmpty;
import static org.apache.commons.lang3.Validate.notNull;

import java.io.InputStream;

import org.apache.commons.io.input.NullInputStream;
import org.apache.http.auth.Credentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import thc.util.HttpClient;
import thc.util.HttpClientImpl;

import com.github.sendgrid.SendGrid;

public class CheckWebService {
	protected final Logger log = LoggerFactory.getLogger(getClass());
	
	private final String[] urls;	
	private final String toEmail;	
	private final String fromEmail;
	//private final Credentials smtpAccount;
	private final SendGrid sendGrid;
	private final HttpClient clientFactory;
			
	public static class Builder {
		private String[] urls;
		private String fromEmail;
		private String toEmail;
		private SendGrid sendGrid;
		private HttpClient clientFactory;

		public Builder checkUrls(String[] urls) { this.urls = urls; return this;}
		public Builder fromEmail(String email) { this.fromEmail = email; return this; }
		public Builder toEmail(String email) { this.toEmail = email; return this; }
		public Builder sendGrid(SendGrid sendGrid) { this.sendGrid = sendGrid; return this; }
		public Builder httpClient(HttpClient clientFactory) { this.clientFactory = clientFactory; return this; }
		
		public CheckWebService build() {
			notEmpty(urls);
			notBlank(toEmail);
			notNull(sendGrid);
			notNull(clientFactory);
			return new CheckWebService(this); 
		}
	}
			
	private CheckWebService(Builder builder) {
		this.urls = builder.urls;
		this.fromEmail = builder.fromEmail;
		this.toEmail = builder.toEmail;
		this.sendGrid = builder.sendGrid;
		this.clientFactory = builder.clientFactory;
	}

	public void check() {
		for (String url : urls) {
			log.debug("Check {}", url);
			InputStream s = clientFactory.newInstance().makeGetRequest(url);
			if (s instanceof NullInputStream) {
				log.info("Fail to ping {}, sending email", url);
				sendPingFailEmail(url);			
			}
		}		
	}

	private void sendPingFailEmail(String url) {
		//SendGrid sendGrid = new SendGrid(smtpAccount.getUserPrincipal().getName(), smtpAccount.getPassword());
		sendGrid.addTo(toEmail);
		sendGrid.setFrom(fromEmail);
		sendGrid.setSubject("Fail to ping " + url);
		sendGrid.setText("Check it!");
		sendGrid.send();
	}
}

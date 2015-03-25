package squote.service;

import static org.apache.commons.lang3.Validate.notBlank;
import static org.apache.commons.lang3.Validate.notEmpty;
import static org.apache.commons.lang3.Validate.notNull;

import java.io.InputStream;

import org.apache.commons.io.input.NullInputStream;
import org.apache.commons.lang3.Validate;
import org.apache.http.auth.Credentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import thc.util.HttpClient;

import com.github.sendgrid.SendGrid;

public class CheckWebService {
	protected final Logger log = LoggerFactory.getLogger(getClass());
	
	private final String[] urls;	
	private final String toEmail;	
	private final String fromEmail;
	private final Credentials smtpAccount;
			
	public static class Builder {
		private String[] urls;
		private String fromEmail;
		private String toEmail;
		private Credentials smtpAccount;

		public Builder checkUrls(String[] urls) { this.urls = urls; return this;}
		public Builder fromEmail(String email) { this.fromEmail = email; return this; }
		public Builder toEmail(String email) { this.toEmail = email; return this; }
		public Builder smtpAccount(Credentials smtpAccount) { this.smtpAccount = smtpAccount; return this; }
		
		public CheckWebService build() {
			notEmpty(urls);
			notBlank(toEmail);
			notNull(smtpAccount);
			return new CheckWebService(this); 
		}
	}
			
	private CheckWebService(Builder builder) {
		this.urls = builder.urls;
		this.fromEmail = builder.fromEmail;
		this.toEmail = builder.toEmail;
		this.smtpAccount = builder.smtpAccount;
	}

	public void check() {
		for (String url : urls) {
			log.debug("Check {}", url);
			InputStream s = new HttpClient().makeGetRequest(url);
			if (s instanceof NullInputStream) {
				log.info("Fail to ping {}, sending email", url);
				sendPingFailEmail(url);			
			}
		}		
	}

	private void sendPingFailEmail(String url) {
		SendGrid sendGrid = new SendGrid(smtpAccount.getUserPrincipal().getName(), smtpAccount.getPassword());
		sendGrid.addTo(toEmail);
		sendGrid.setFrom(fromEmail);
		sendGrid.setSubject("Fail to ping " + url);
		sendGrid.setText("Check it!");
		sendGrid.send();
	}
}

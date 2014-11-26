package squote.service;

import java.io.InputStream;

import org.apache.commons.io.input.NullInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import thc.util.HttpClient;

import com.github.sendgrid.SendGrid;

public class CheckWebService {
	protected final Logger log = LoggerFactory.getLogger(getClass());
	
	private final String[] urls;
	
	private final String adminEmail;
	
	private final String appEmail;
	
	private final String smtpUsername;

	private final String smtpPassword;
	
	public CheckWebService(String[] urls, String adminEmail,
			String appEmail, String smtpUsername, String smtpPassword) {
		super();
		this.urls = urls;
		this.adminEmail = adminEmail;
		this.appEmail = appEmail;
		this.smtpUsername = smtpUsername;
		this.smtpPassword = smtpPassword;
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
		SendGrid sendGrid = new SendGrid(smtpUsername, smtpPassword);
		sendGrid.addTo(adminEmail);
		sendGrid.setFrom(appEmail);
		sendGrid.setSubject("Fail to ping " + url);
		sendGrid.setText("Check it!");
		sendGrid.send();
	}
}

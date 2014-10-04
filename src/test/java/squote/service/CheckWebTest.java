package squote.service;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import squote.SpringQuoteWebApplication;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = SpringQuoteWebApplication.class)
public class CheckWebTest {
	@Autowired CheckWebService checker;
	
	@Test
	public void testCheckShouldRunWithoutException() {
		checker.check();
	}
}

package squote.service;

import org.springframework.beans.factory.annotation.Autowired;

//@RunWith(SpringJUnit4ClassRunner.class)
//@ContextConfiguration(classes = SpringQuoteWebApplication.class)
public class CheckWebIntegrationTest {
	@Autowired CheckWebService checker;
	
	//@Test
	public void check_GivenCorrectConfig_ShouldShouldRunWithoutException() {
		checker.check();
	}
}

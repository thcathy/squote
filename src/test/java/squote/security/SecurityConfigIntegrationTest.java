package squote.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import squote.IntegrationTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class SecurityConfigIntegrationTest extends IntegrationTest {
	@Value("${jwt.testing.token}")
	private String testingIdToken;

	@Autowired private MockMvc mockMvc;

	// @Test
	// require valid token setup. test manually
	public void callUsingValidToken_returnOk() throws Exception {
		this.mockMvc.perform(
				get("/rest/fund/create/testingFund")
						.contentType(MediaType.APPLICATION_JSON)
						.header("Authorization", "Bearer " + testingIdToken)
		)
				.andExpect(status().isOk());
	}

}

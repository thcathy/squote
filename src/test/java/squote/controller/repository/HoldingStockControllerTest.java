package squote.controller.repository;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import squote.SpringQuoteWebApplication;

@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = SpringQuoteWebApplication.class)
@ActiveProfiles("dev")
public class HoldingStockControllerTest {
	@Autowired HoldingStockController controller;
	
	private MockMvc mockMvc;
	
	@Before
    public void setup() {    	
    	MockitoAnnotations.initMocks(this);
        this.mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }
	
	@Test
	public void rootPagePathIsCorrect() throws Exception {		
		mockMvc.perform(get("/holdingstock/").characterEncoding("utf-8"))
		.andExpect(status().isOk())
		.andExpect(view().name("holdingstock/list"));

	}
}

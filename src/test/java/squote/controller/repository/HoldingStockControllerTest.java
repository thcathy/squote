package squote.controller.repository;

import static org.junit.Assert.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.List;
import java.util.stream.IntStream;

import javax.validation.constraints.AssertTrue;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import squote.SpringQuoteWebApplication;
import squote.controller.QuoteController;
import squote.domain.HoldingStock;

@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = SpringQuoteWebApplication.class)
@ActiveProfiles("dev")
public class HoldingStockControllerTest {
	private Logger log = LoggerFactory.getLogger(HoldingStockControllerTest.class);
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
	
	@Test
	public void objectsAreSortedByCreatedDate() throws Exception {
		MvcResult mvcResult = mockMvc.perform(get("/holdingstock/").characterEncoding("utf-8"))
								.andExpect(status().isOk())
								.andExpect(view().name("holdingstock/list"))
								.andReturn();
		
		List<HoldingStock> stocks = (List<HoldingStock>) mvcResult.getModelAndView().getModelMap().get("holdingStocks");
		log.debug("Total stocks [{}]", stocks.size());
		
		boolean sortedByDate = IntStream.range(0, stocks.size()-1).allMatch(i -> stocks.get(i).getDate().getTime() <= stocks.get(i+1).getDate().getTime());
		assertTrue(sortedByDate);
	}
}

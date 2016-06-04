package squote.controller;

import com.google.common.base.Stopwatch;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
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
import org.springframework.ui.ModelMap;
import org.springframework.util.StopWatch;
import squote.SpringQuoteWebApplication;
import squote.SquoteConstants.IndexCode;
import squote.domain.MarketDailyReport;
import squote.domain.StockQuote;
import squote.domain.repository.HoldingStockRepository;
import squote.service.CentralWebQueryService;
import squote.web.parser.EtnetStockQuoteParser;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = SpringQuoteWebApplication.class)
@ActiveProfiles("dev")
public class QuoteControllerPerformanceTest {
	private Logger log = LoggerFactory.getLogger(QuoteControllerPerformanceTest.class);

	@Autowired QuoteController quoteController;
	@Autowired CentralWebQueryService queryService;
	
	private MockMvc mockMvc;
	   
    @Before
    public void setup() {    	
    	this.mockMvc = MockMvcBuilders.standaloneSetup(quoteController).build();        
    }

    @Test
	public void getSingleQuote_ShouldNotSpentDoubleTimeThanUsingSingleStockQuote() throws Exception {
		Stopwatch timer = Stopwatch.createStarted();

		queryService.submit(() -> new EtnetStockQuoteParser().parse("941").get()).join();

        long timeSpentOnSingleQuote = timer.elapsed(TimeUnit.MILLISECONDS);
        log.debug("get quote from Etnet stock quote parser took: {}", timer.stop());
        timer = Stopwatch.createStarted();

		mockMvc.perform(get("/quote/single/2800").characterEncoding(WebConstants.RESPONSE_ENCODING))
			.andExpect(status().isOk())
			.andExpect(content().contentType("application/xml"))
			.andExpect(xpath("/stockQuote[price=NA]").doesNotExist());

		long timeSpentOnWebRequest = timer.elapsed(TimeUnit.MILLISECONDS);
        log.debug("get quote by QuoteController.single took: {}", timer.stop());

        assertTrue(timeSpentOnSingleQuote > timeSpentOnWebRequest / 2);


	}
	
}

package squote.service;
 
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import squote.domain.MarketDailyReport;
import squote.domain.repository.MarketDailyReportRepository;
 
public class MarketReportServiceTest {
	MarketReportService service;
	
	@Before
	public void setupService() {
		service = new MarketReportService(Mockito.mock(MarketDailyReportRepository.class), Mockito.mock(CentralWebQueryService.class));
	}
	
	@Test
	public void getTodayMarketDailyReport_ShouldAlwaysReturnObj() {
		MarketDailyReport rpt = service.getTodayMarketDailyReport();
		assertTrue(rpt.getDate() > 0);
		assertTrue(rpt.getMoneyBase().getClosingBalance() > 0);
		assertTrue(rpt.getMoneyBase().getExchangeFund() > 0);
		assertTrue(rpt.getMoneyBase().getIndebtedness() > 0);
		assertTrue(rpt.getMoneyBase().getNotes() > 0);
		assertTrue(rpt.getMoneyBase().getTotal() > 0);
		assertEquals("HSI", rpt.getHsi().getStockCode());
		assertTrue("Hsi last update should start from 21th century", rpt.getHsi().getLastUpdate().startsWith("201"));
		assertEquals("HSCEI", rpt.getHscei().getStockCode());
		assertTrue("Hcesi last update should start from 21th century", rpt.getHscei().getLastUpdate().startsWith("201"));
	}
}
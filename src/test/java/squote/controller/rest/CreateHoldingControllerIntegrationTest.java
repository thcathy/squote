package squote.controller.rest;


import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import squote.IntegrationTest;
import squote.SquoteConstants;
import squote.SquoteConstants.Side;
import squote.domain.Fund;
import squote.domain.HoldingStock;
import squote.domain.repository.FundRepository;
import squote.domain.repository.HoldingStockRepository;
import squote.security.AuthenticationServiceStub;
import squote.service.HKEXMarketFeesCalculator;
import thc.util.DateUtils;

import java.math.BigDecimal;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

public class CreateHoldingControllerIntegrationTest extends IntegrationTest {
	@Autowired CreateHoldingController controller;
	@Autowired FundRepository fundRepo;
	@Autowired HoldingStockRepository holdingRepo;
	@Autowired AuthenticationServiceStub authenticationServiceStub;

	private String userId = "tester";
	private Fund testFund = createSimpleFund();

	private Fund createSimpleFund() {
		Fund f = new Fund(userId, "testfund");
		f.buyStock("2828", BigDecimal.valueOf(500), new BigDecimal(50000));
		f.buyStock("2800", BigDecimal.valueOf(1000), new BigDecimal(25000));
		return f;
	}
	
	private HoldingStock createSell2800Holding() {
		return new HoldingStock(userId, "2800", Side.SELL, 300, new BigDecimal("8190"), new Date(), null);
	}

	@BeforeEach
	public void setup() {
		fundRepo.save(testFund);
		authenticationServiceStub.userId = userId;
	}

	@AfterEach
	public void revert() {
		fundRepo.delete(testFund);
		authenticationServiceStub.userId = authenticationServiceStub.TESTER_USERID;
	}

	@Test
	public void createHoldingStockFromExecution_givenCorrectMsg_shouldReturnSuccessful() {
		long holdingQty = holdingRepo.count();
		String hscei = "10000";
		String scbSellMsg = "渣打:買入6000股883.HK 中國海洋石油\n";
		scbSellMsg += "已完成\n";
		scbSellMsg += "平均價HKD7.99\n";
		scbSellMsg += "O1512110016740"; 
		
		HoldingStock holding = controller.createHoldingFromExecution(scbSellMsg, hscei);
		assertEquals(6000, holding.getQuantity());
		assertEquals(Side.BUY, holding.getSide());
		assertEquals("883", holding.getCode());
		assertEquals(new BigDecimal(hscei), holding.getHsce());
		assertEquals(holdingQty + 1, holdingRepo.count());
		assertEquals(authenticationServiceStub.userId, holding.getUserId());
		assertEquals(new BigDecimal("164.07"), holding.fees.get(HKEXMarketFeesCalculator.INCLUDE_STAMP));
		assertEquals(new BigDecimal("102.07"), holding.fees.get(HKEXMarketFeesCalculator.EXCLUDE_STAMP));
	}
	
	@Test
	public void createHolding_givenEmptyMsg_shouldThrowException() {
		Exception exception = Assertions.assertThrows(IllegalArgumentException.class, () -> {
			controller.createHoldingFromExecution("", null);
		});
		assertEquals("Cannot create holding", exception.getMessage());
	}
	
	@Test
	public void createHolding_givenWrongMsg_shouldThrowException() {
		Exception exception = Assertions.assertThrows(IllegalArgumentException.class, () -> {
			controller.createHoldingFromExecution("渣打 but it is wrong messags", null);
		});
		assertEquals("Cannot create holding", exception.getMessage());
	}

	// @Test // ignore due to scb msg missing trade date. Trade date is the obj created time, will always success in getting HSI
	public void createHolding_whenCannotGetHcei_shouldThrowException() {
		String scbSellMsg = "渣打:買入17500股7288.HK\n" +
				"已完成\n" +
				"平均價HKD4.79\n" +
				"OSCBABT44566440";
		Exception exception = Assertions.assertThrows(RuntimeException.class, () -> {
			controller.createHoldingFromExecution(scbSellMsg, "0");
		});
		assertEquals("Cannot getHistoryPrice hcei", exception.getMessage());
	}
	
	@Test	
	public void postCreateHoldingStock_GivenTodayExeMsg_ShouldCreateHoldingStockUseIndexFromRealtimeQuote() throws Exception {
		String scbSellMsg = "渣打:買入6000股883.HK 中國海洋石油\n";
		scbSellMsg += "已完成\n";
		scbSellMsg += "平均價HKD7.99\n";
		scbSellMsg += "O" + DateUtils.toString(new Date(), "yyMMdd") + "00013235";
		long totalHoldings = holdingRepo.count();
		
		// Expect
		HoldingStock holding = controller.createHoldingFromExecution(scbSellMsg, "0");
		assertNotNull(holding);
		assertEquals("883", holding.getCode());
		assertEquals(6000, holding.getQuantity());
		assertEquals(new BigDecimal("47940.00"), holding.getGross());
		assertEquals(SquoteConstants.Side.BUY, holding.getSide());
		assertTrue(holding.getHsce().doubleValue() > 6000);
		assertEquals(totalHoldings+1, holdingRepo.count());
		assertEquals(authenticationServiceStub.userId, holding.getUserId());
	}
	
	@Test
	public void updatefund_GivenHoldingStock_ShouldUpdateAndPersist() throws Exception {
		HoldingStock holding = holdingRepo.save(createSell2800Holding());

		controller.updateFundByHolding(testFund.name, holding.getId(), new BigDecimal("20.98"));
		var fund = fundRepo.findByUserIdAndName(userId, testFund.name).get();
		holding = holdingRepo.findById(holding.getId()).get();
		assertNotNull(fund);
		assertEquals(BigDecimal.valueOf(700), fund.getHoldings().get(holding.getCode()).getQuantity());
		assertEquals(new BigDecimal("669.02"), fund.getProfit().setScale(2));
		assertEquals(fund.name, holding.getFundName());
	}

}

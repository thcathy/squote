package squote.controller.rest;

import com.google.common.collect.Lists;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import squote.IntegrationTest;
import squote.domain.Fund;
import squote.domain.FundHolding;
import squote.domain.repository.FundRepository;
import squote.security.AuthenticationServiceStub;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@AutoConfigureMockMvc
public class FundControllerIntegrationTest extends IntegrationTest {
	@Autowired FundController fundController;
	@Autowired FundRepository fundRepo;
	@Autowired AuthenticationServiceStub authenticationServiceStub;

	String userId = UUID.randomUUID().toString();

	private final Fund testFund = createSimpleFund();
	private final Fund testFund2 = createSimpleFund2();

	private Fund createSimpleFund() {
		Fund f = new Fund(userId, "testfund");
		f.buyStock("2828", BigDecimal.valueOf(500), BigDecimal.valueOf(50000));
		f.buyStock("2800", BigDecimal.valueOf(1000), BigDecimal.valueOf(25000));
		return f;
	}

	private Fund createSimpleFund2() {
		Fund f = new Fund(userId, "testfund2");
		f.buyStock("2828", BigDecimal.valueOf(400), BigDecimal.valueOf(44000));
		return f;
	}

	@BeforeEach
	public void setup() {
		authenticationServiceStub.userId = userId;
		fundRepo.save(testFund);
		fundRepo.save(testFund2);
	}

	@AfterEach
	public void revert() {
		fundRepo.delete(testFund);
	}

	@Test
	public void urlbuy_addStockAndSave() throws Exception {
		fundController.buy(testFund.name, "2800", "100", "100.1");

		Fund result = fundRepo.findByUserIdAndName(authenticationServiceStub.userId, "testfund").get();
		assertEquals(BigDecimal.valueOf(1100), result.getHoldings().get("2800").getQuantity());
		assertEquals(35010, result.getHoldings().get("2800").getGross().intValue());
	}

	@Test
	public void urlsell_minusStockAndSave() throws Exception {
		fundController.sell(testFund.name, "2828", "100", "120");
		
		Fund result = fundRepo.findByUserIdAndName(userId, testFund.name).get();
		assertEquals(BigDecimal.valueOf(400), result.getHoldings().get("2828").getQuantity());
		assertEquals(40000, result.getHoldings().get("2828").getGross().intValue());	// gross is deduce base on original gross price
	}
	
	@Test
	public void urlcreate_givenNewName_shouldCreateNewFund() throws Exception {
		fundController.create("newfund", Fund.FundType.STOCK);
		
		Fund result = fundRepo.findByUserIdAndName(userId, "newfund").get();
		assertEquals("newfund", result.name);
		assertEquals(0, result.getProfit().intValue());

		assertThat(fundRepo.findByUserId(userId).size()).isEqualTo(3);
	}
	
	@Test
	public void urlDelete_ShouldRemoveFund() throws Exception {
		fundController.delete("testfund");
			
		assertFalse(fundRepo.findByUserIdAndName(userId, "testfund").isPresent());
		assertThat(fundRepo.findByUserId(userId).size()).isEqualTo(1);
	}
	
	@Test
	public void urlremove_ShouldRemoveAStockFromFund() throws Exception {
		fundController.removeStock(testFund.name, "2800");
		
		Fund result = fundRepo.findByUserIdAndName(userId, testFund.name).get();
		assertEquals(null, result.getHoldings().get("2800"));
	}
	
	@Test
	public void payInterest_ShouldSaveSubstractedFund() throws Exception {
		fundController.payInterest(testFund.name, "2828", "100.5");
		
		Fund fund = fundRepo.findByUserIdAndName(userId, testFund.name).get();
		FundHolding newHolding = fund.getHoldings().get("2828");
		
		assertEquals(BigDecimal.valueOf(500), newHolding.getQuantity());
		assertEquals(new BigDecimal("49899.5"), newHolding.getGross());			
		assertEquals(new BigDecimal("99.7990"), newHolding.getPrice());
	}
	
	@Test
	public void addProfit_givenListOfValue_shouldAddedToProfit() throws Exception {
		fundController.addProfit(testFund.name, FundController.ValueAction.add, "101,50.5");
		fundController.addProfit(testFund.name, FundController.ValueAction.add, "98.2");

		Fund fund = fundRepo.findByUserIdAndName(userId, testFund.name).get();
		assertEquals(new BigDecimal("249.7"), fund.getProfit());
	}
	
	@Test
	public void addExpense_givenListOfValue_shouldMinusFromProfit() throws Exception {
		fundController.addProfit(testFund.name, FundController.ValueAction.subtract, "30,2.32,.89");
		fundController.addProfit(testFund.name, FundController.ValueAction.subtract, "5");

		Fund fund = fundRepo.findByUserIdAndName(userId, testFund.name).get();
		assertEquals(new BigDecimal("-38.21"), fund.getProfit());
	}
	
	@Test
	public void setProfit_givenValue_shouldBeTheProfit() throws Exception {
		fundController.setProfit(testFund.name, "123.456");

		Fund fund = fundRepo.findByUserIdAndName(userId, testFund.name).get();
		assertEquals(new BigDecimal("123.456"), fund.getProfit());
	}
	
	@Test
	public void getAll_shouldReturnAllFunds() throws Exception {
		List<Fund> result = Lists.newArrayList(fundController.getAll());

		assertThat(result.size()).isEqualTo(2);
		assertThat(result.stream().anyMatch(f -> f.name.equals("testfund"))).isTrue();
	}

	@Test
	public void cashout_shouldAddToAmount() throws Exception {
		fundController.cashOut(testFund.name, "123");
		fundController.cashOut(testFund.name, "0.456");

		Fund fund = fundRepo.findByUserIdAndName(userId, testFund.name).get();
		assertEquals(new BigDecimal("123.456"), fund.getCashoutAmount());
	}

	@Test
	public void cashin_shouldAddToAmount() {
		fundController.cashIn(testFund.name, "123");
		fundController.cashIn(testFund.name, "0.456");

		Fund fund = fundRepo.findByUserIdAndName(userId, testFund.name).get();
		assertEquals(new BigDecimal("123.456"), fund.getCashinAmount());
	}

	@Test
	public void buy_givenZeroQuantityAndPrice_shouldSuccess() {
		fundController.buy(testFund.name, "BTCUSDT", "0", "0");

		Fund fund = fundRepo.findByUserIdAndName(userId, testFund.name).get();
		assertNotNull(fund.getHoldings().get("BTCUSDT"));
	}

	@Test
	public void setFundType_shouldSuccess() {
		fundController.setType(testFund.name, Fund.FundType.STOCK);

		Fund fund = fundRepo.findByUserIdAndName(userId, testFund.name).get();
		assertEquals(Fund.FundType.STOCK, fund.getType());
	}

	@Test
	public void splitInterest_basicCase() {
		var result = fundController.splitInterest("2828", "900", "/testfund2/testfund");
		assertEquals(2, result.size());
		assertEquals("[testfund2] 2828: 400, 44000.00 > 43600.00, 110.00 > 109.00", result.get(0));
		assertEquals("[testfund] 2828: 500, 50000.00 > 49500.00, 100.00 > 99.00", result.get(1));
	}

	@Test
	public void splitInterest_singleFund() {
		var result = fundController.splitInterest("2800", "1000", "/testfund");
		assertEquals(1, result.size());
		assertEquals("[testfund] 2800: 1000, 25000.00 > 24000.00, 25.00 > 24.00", result.get(0));
	}

	@Test
	public void splitInterest_missingFund() {
		var result = fundController.splitInterest("2828", "900", "/not exist fund");
		assertEquals("Error: cannot find fund=[not exist fund]", result.get(0));
	}

	@Test
	public void splitInterest_missingCode() {
		var result = fundController.splitInterest("2800", "900", "/testfund2/testfund");
		assertEquals("Error: cannot find code=2800 in all funds=[testfund2, testfund]", result.get(0));
	}

	@Test
	public void splitInterest_wrongAmount() {
		var result = fundController.splitInterest("2828", "10xyz900", "/testfund2/testfund");
		assertEquals("Error: invalid amount=10xyz900", result.get(0));
	}
}

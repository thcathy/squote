package squote.service;

import com.futu.openapi.FTAPI_Conn_Qot;
import com.futu.openapi.FTAPI_Conn_Trd;
import com.futu.openapi.pb.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import squote.SquoteConstants;
import squote.domain.Market;
import squote.scheduletask.FutuClientConfig;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FutuAPIClientTest {
    FutuAPIClient client;

    @Mock private FTAPI_Conn_Trd FTAPIConnTrd;
    @Mock private FTAPI_Conn_Qot FTAPIConnQot;

    @BeforeEach
    void setUp() {
        client = new FutuAPIClient(FutuClientConfig.defaultConfig(), FTAPIConnTrd, FTAPIConnQot, "", false);
    }

    @Test
    void getHKStockAccountId_ReturnLongId() {
        long expectedAccountId = 456;

        var account = TrdCommon.TrdAcc.newBuilder()
                .setTrdEnv(TrdCommon.TrdEnv.TrdEnv_Real_VALUE)
                .setAccType(TrdCommon.TrdAccType.TrdAccType_Margin_VALUE)
                .addTrdMarketAuthList(TrdCommon.TrdMarket.TrdMarket_HK_VALUE)
                .setAccID(expectedAccountId)
                .build();
        var response = TrdGetAccList.Response.newBuilder()
                .setRetType(0)
                .setS2C(TrdGetAccList.S2C.newBuilder().addAccList(account))
                .build();

        when(FTAPIConnTrd.getAccList(any())).thenReturn(1);
        client.onReply_GetAccList(FTAPIConnTrd, 1, response);
        long actualAccountId = client.getHKStockAccountId();
        assertEquals(expectedAccountId, actualAccountId);
    }

    @Test
    void getHKStockExecutions_ReturnExecutionList() {
        var buy2828 = TrdCommon.OrderFill.newBuilder()
                .setCode("02828").setOrderID(1L).setQty(100).setPrice(10)
                .setTrdSide(TrdCommon.TrdSide.TrdSide_Buy_VALUE)
                .setFillID(1).setFillIDEx("1").setName("")
                .setCreateTime("2025-08-05 09:30:01.000")
                .setSecMarket(TrdCommon.TrdSecMarket.TrdSecMarket_HK_VALUE);
        var buy2828_2 = TrdCommon.OrderFill.newBuilder()
                .setCode("02828").setOrderID(1L).setQty(500).setPrice(10.24)
                .setTrdSide(TrdCommon.TrdSide.TrdSide_Buy_VALUE)
                .setSecMarket(TrdCommon.TrdSecMarket.TrdSecMarket_HK_VALUE)
                .setFillID(2).setFillIDEx("1").setName("")
                .setCreateTime("2025-08-05 09:35:02.000");
        var buy2800 = TrdCommon.OrderFill.newBuilder()
                .setCode("02800").setOrderID(2L).setQty(500).setPrice(18.1)
                .setTrdSide(TrdCommon.TrdSide.TrdSide_Sell_VALUE)
                .setSecMarket(TrdCommon.TrdSecMarket.TrdSecMarket_HK_VALUE)
                .setFillID(3).setFillIDEx("1").setName("")
                .setCreateTime("2025-08-05 09:32:19.373")
                .setUpdateTimestamp(3);
        var S2CBuilder = TrdGetHistoryOrderFillList.S2C.newBuilder();
        S2CBuilder.addOrderFillList(buy2828);
        S2CBuilder.addOrderFillList(buy2828_2);
        S2CBuilder.addOrderFillList(buy2800);
        var headerBuilder = TrdCommon.TrdHeader.newBuilder().setTrdEnv(1).setAccID(1).setTrdMarket(1);
        S2CBuilder.setHeader(headerBuilder);
        var response = TrdGetHistoryOrderFillList.Response.newBuilder()
                        .setRetType(0).setS2C(S2CBuilder).build();

        when(FTAPIConnTrd.getHistoryOrderFillList(any())).thenReturn(1);

        client.onReply_GetHistoryOrderFillList(FTAPIConnTrd, 1, response);
        var executions = client.getRecentExecutions(new Date(), Market.HK);
        assertEquals(2, executions.size());
        var exec2828 = executions.get("1");
        var exec2800 = executions.get("2");

        assertEquals("2828", exec2828.getCode());
        assertEquals(SquoteConstants.Side.BUY, exec2828.getSide());
        assertEquals(new BigDecimal("10.200"), exec2828.getPrice());
        assertEquals(new BigDecimal("600.0"), exec2828.getQuantity());
        assertEquals(1754357702000L, exec2828.getTime());
        assertEquals("1,2", exec2828.getFillIds());

        assertEquals("2800", exec2800.getCode());
        assertEquals(SquoteConstants.Side.SELL, exec2800.getSide());
        assertEquals(new BigDecimal("18.1"), exec2800.getPrice());
        assertEquals(new BigDecimal("500.0"), exec2800.getQuantity());
        assertEquals(1754357539373L, exec2800.getTime());
        assertEquals("3", exec2800.getFillIds());
    }

    @Test
    void getRecentExecutions_HK_beginTimeFormatting() {
        var captor = ArgumentCaptor.forClass(TrdGetHistoryOrderFillList.Request.class);
        when(FTAPIConnTrd.getHistoryOrderFillList(any())).thenReturn(1);
        var headerBuilder = TrdCommon.TrdHeader.newBuilder().setTrdEnv(1).setAccID(1).setTrdMarket(1);
        var s2CBuilder = TrdGetHistoryOrderFillList.S2C.newBuilder().setHeader(headerBuilder).build();
        client.onReply_GetHistoryOrderFillList(FTAPIConnTrd, 1,
                TrdGetHistoryOrderFillList.Response.newBuilder().setRetType(0).setS2C(s2CBuilder).build()
        );

        Date testDate = new Date(1754357702188L); // Aug 05 2025 09:35:02.188 HKT
        client.getRecentExecutions(testDate, Market.HK);
        verify(FTAPIConnTrd).getHistoryOrderFillList(captor.capture());
        TrdGetHistoryOrderFillList.Request request = captor.getValue();

        // +1 second
        assertEquals("2025-08-05 09:35:03.188", request.getC2S().getFilterConditions().getBeginTime());
    }

    @Test
    void getRecentExecutions_US_beginTimeFormatting() {
        var captor = ArgumentCaptor.forClass(TrdGetHistoryOrderFillList.Request.class);
        when(FTAPIConnTrd.getHistoryOrderFillList(any())).thenReturn(1);
        var headerBuilder = TrdCommon.TrdHeader.newBuilder().setTrdEnv(1).setAccID(1).setTrdMarket(1);
        var s2CBuilder = TrdGetHistoryOrderFillList.S2C.newBuilder().setHeader(headerBuilder).build();
        client.onReply_GetHistoryOrderFillList(FTAPIConnTrd, 1,
                TrdGetHistoryOrderFillList.Response.newBuilder().setRetType(0).setS2C(s2CBuilder).build()
        );

        Date testDate = new Date(1754357702188L); // Aug 04 2025 21:35:02.188 EST
        client.getRecentExecutions(testDate, Market.US);
        verify(FTAPIConnTrd).getHistoryOrderFillList(captor.capture());
        TrdGetHistoryOrderFillList.Request request = captor.getValue();

        // +1 second
        assertEquals("2025-08-04 21:35:03.188", request.getC2S().getFilterConditions().getBeginTime());
    }

    @Test
    void placeOrderRequest_USMarket_SetsSessionAndFillOutsideRTH() {
        var captor = ArgumentCaptor.forClass(TrdPlaceOrder.Request.class);
        var mockPacketId = Common.PacketID.newBuilder().setConnID(1L).setSerialNo(1).build();
        when(FTAPIConnTrd.nextPacketID()).thenReturn(mockPacketId);
        when(FTAPIConnTrd.placeOrder(any())).thenReturn(1);
        client.onReply_PlaceOrder(FTAPIConnTrd, 1, TrdPlaceOrder.Response.newBuilder().setRetType(0).build());

        client.placeOrder(SquoteConstants.Side.BUY, "AAPL.US", 100, 150.0);
        verify(FTAPIConnTrd).placeOrder(captor.capture());

        var request = captor.getValue();
        var c2s = request.getC2S();
        assertEquals(Common.Session.Session_ETH_VALUE, c2s.getSession());
        assertEquals(true, c2s.getFillOutsideRTH());
    }

    @Test
    void placeOrderRequest_HKMarket_DoesNotSetSessionAndFillOutsideRTH() {
        var captor = ArgumentCaptor.forClass(TrdPlaceOrder.Request.class);
        var mockPacketId = Common.PacketID.newBuilder().setConnID(1L).setSerialNo(1).build();
        when(FTAPIConnTrd.nextPacketID()).thenReturn(mockPacketId);
        when(FTAPIConnTrd.placeOrder(any())).thenReturn(1);
        client.onReply_PlaceOrder(FTAPIConnTrd, 1, TrdPlaceOrder.Response.newBuilder().setRetType(0).build());

        client.placeOrder(SquoteConstants.Side.BUY, "2800", 100, 18.5);
        verify(FTAPIConnTrd).placeOrder(captor.capture());

        var request = captor.getValue();
        var c2s = request.getC2S();
        assertEquals(0, c2s.getSession());
        assertEquals(false, c2s.getFillOutsideRTH());
    }

    @Test
    void getFlowSummary_SuccessfulResponse_MapsToFlowSummaryInfoList() {
        var flowInfo1 = TrdFlowSummary.FlowSummaryInfo.newBuilder()
                .setClearingDate("2025-05-30")
                .setSettlementDate("2025-05-30")
                .setCurrency(1)
                .setCashFlowType("Cash Dividend")
                .setCashFlowDirection(1)
                .setCashFlowAmount(7810.0)
                .setCashFlowRemark("INTERIM DISTRIBUTION - HKD0.22/UNIT")
                .setCashFlowID(792566)
                .build();
        var headerBuilder = TrdCommon.TrdHeader.newBuilder().setTrdEnv(1).setAccID(1).setTrdMarket(1);
        var s2c = TrdFlowSummary.S2C.newBuilder()
                .setHeader(headerBuilder)
                .addFlowSummaryInfoList(flowInfo1).build();
        var response = TrdFlowSummary.Response.newBuilder().setRetType(0).setS2C(s2c).build();

        when(FTAPIConnTrd.getFlowSummary(any())).thenReturn(1);
        client.onReply_GetFlowSummary(FTAPIConnTrd, 1, response);
        var flows = client.getFlowSummary(new Date(), Market.HK);
        assertEquals(1, flows.size());
        var firstFlow = flows.getFirst();
        assertEquals("2025-05-30", firstFlow.clearingDate());
        assertEquals("2025-05-30", firstFlow.settlementDate());
        assertEquals("HKD", firstFlow.currency());
        assertEquals("Cash Dividend", firstFlow.cashFlowType());
        assertEquals("In", firstFlow.cashFlowDirection());
        assertEquals(BigDecimal.valueOf(7810.0), firstFlow.cashFlowAmount());
        assertEquals("INTERIM DISTRIBUTION - HKD0.22/UNIT", firstFlow.cashFlowRemark());
        assertEquals("792566", firstFlow.cashFlowID());
    }

    @Test
    void onReply_GetFlowSummary_ErrorResponse_ReturnsEmptyList() {
        var response = TrdFlowSummary.Response.newBuilder().setRetType(-1).setRetMsg("Error message").build();
        when(FTAPIConnTrd.getFlowSummary(any())).thenReturn(1);
        client.onReply_GetFlowSummary(FTAPIConnTrd, 1, response);

        var result = client.getFlowSummary(new Date(), Market.HK);
        assertEquals(0, result.size());
    }

    @Test
    void getAvailableFunds_SuccessfulResponse_ReturnsHKDAndUSDFunds() {
        // Create cash info for HKD
        var hkdCashInfo = TrdCommon.AccCashInfo.newBuilder()
                .setCurrency(TrdCommon.Currency.Currency_HKD_VALUE)
                .setCash(50000.0).setAvailableBalance(45000.0).setNetCashPower(42000.0).build();
        // Create cash info for USD
        var usdCashInfo = TrdCommon.AccCashInfo.newBuilder()
                .setCurrency(TrdCommon.Currency.Currency_USD_VALUE)
                .setCash(10000.0).setAvailableBalance(9500.0).setNetCashPower(9000.0).build();
        // Create cash info for other currency (should be filtered out)
        var cnyInfo = TrdCommon.AccCashInfo.newBuilder()
                .setCurrency(TrdCommon.Currency.Currency_CNH_VALUE)
                .setCash(5000.0).setAvailableBalance(4500.0).setNetCashPower(4000.0).build();

        var funds = TrdCommon.Funds.newBuilder()
                .setPower(100000.0).setTotalAssets(200000.0).setCash(60000.0)
                .setMarketVal(140000.0).setFrozenCash(1000.0).setDebtCash(2000.0).setAvlWithdrawalCash(40000.0)
                .addCashInfoList(hkdCashInfo).addCashInfoList(usdCashInfo).addCashInfoList(cnyInfo).build();

        var s2c = TrdGetFunds.S2C.newBuilder()
                .setHeader(TrdCommon.TrdHeader.newBuilder()
                    .setTrdEnv(TrdCommon.TrdEnv.TrdEnv_Real_VALUE).setAccID(123456L).setTrdMarket(TrdCommon.TrdMarket.TrdMarket_HK_VALUE))
                .setFunds(funds).build();

        var response = TrdGetFunds.Response.newBuilder().setRetType(0).setS2C(s2c).build();

        when(FTAPIConnTrd.getFunds(any())).thenReturn(1);
        client.onReply_GetFunds(FTAPIConnTrd, 1, response);

        var availableFunds = client.getAvailableFunds();

        assertEquals(2, availableFunds.size());
        assertEquals(42000.0, availableFunds.get(Currency.getInstance("HKD")));
        assertEquals(9000.0, availableFunds.get(Currency.getInstance("USD")));
    }
}

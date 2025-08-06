package squote.service;

import com.futu.openapi.FTAPI_Conn_Qot;
import com.futu.openapi.FTAPI_Conn_Trd;
import com.futu.openapi.pb.TrdCommon;
import com.futu.openapi.pb.TrdGetAccList;
import com.futu.openapi.pb.TrdGetHistoryOrderFillList;
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
        var executions = client.getStockExecutions(new Date(), Market.HK);
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
    void getStockExecutions_HK_beginTimeFormatting() {
        var captor = ArgumentCaptor.forClass(TrdGetHistoryOrderFillList.Request.class);
        when(FTAPIConnTrd.getHistoryOrderFillList(any())).thenReturn(1);
        var headerBuilder = TrdCommon.TrdHeader.newBuilder().setTrdEnv(1).setAccID(1).setTrdMarket(1);
        var s2CBuilder = TrdGetHistoryOrderFillList.S2C.newBuilder().setHeader(headerBuilder).build();
        client.onReply_GetHistoryOrderFillList(FTAPIConnTrd, 1,
                TrdGetHistoryOrderFillList.Response.newBuilder().setRetType(0).setS2C(s2CBuilder).build()
        );

        Date testDate = new Date(1754357702188L); // Aug 05 2025 09:35:02.188 HKT
        client.getStockExecutions(testDate, Market.HK);
        verify(FTAPIConnTrd).getHistoryOrderFillList(captor.capture());
        TrdGetHistoryOrderFillList.Request request = captor.getValue();

        assertEquals("2025-08-05 09:35:02.188", request.getC2S().getFilterConditions().getBeginTime());
    }

    @Test
    void getStockExecutions_US_beginTimeFormatting() {
        var captor = ArgumentCaptor.forClass(TrdGetHistoryOrderFillList.Request.class);
        when(FTAPIConnTrd.getHistoryOrderFillList(any())).thenReturn(1);
        var headerBuilder = TrdCommon.TrdHeader.newBuilder().setTrdEnv(1).setAccID(1).setTrdMarket(1);
        var s2CBuilder = TrdGetHistoryOrderFillList.S2C.newBuilder().setHeader(headerBuilder).build();
        client.onReply_GetHistoryOrderFillList(FTAPIConnTrd, 1,
                TrdGetHistoryOrderFillList.Response.newBuilder().setRetType(0).setS2C(s2CBuilder).build()
        );

        Date testDate = new Date(1754357702188L); // Aug 04 2025 21:35:02.188 EST
        client.getStockExecutions(testDate, Market.US);
        verify(FTAPIConnTrd).getHistoryOrderFillList(captor.capture());
        TrdGetHistoryOrderFillList.Request request = captor.getValue();

        assertEquals("2025-08-04 21:35:02.188", request.getC2S().getFilterConditions().getBeginTime());
    }
}

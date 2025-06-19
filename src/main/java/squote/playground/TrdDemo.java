package squote.playground;

import com.futu.openapi.*;
import com.futu.openapi.pb.*;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;

public class TrdDemo implements FTSPI_Trd, FTSPI_Conn, FTSPI_Qot {
    FTAPI_Conn_Trd trd = new FTAPI_Conn_Trd();
    FTAPI_Conn_Qot qot = new FTAPI_Conn_Qot();
    long accId;
    String tradeCode;
    int inited = 0;

    public TrdDemo() {
//        var b = new BinanceAPIService();
//        var c = b.getAllPrices();
//        System.out.println(c);
        accId = Long.parseLong(System.getenv("FUTU_ACCID"));
        tradeCode = System.getenv("FUTU_TRADE_CODE");
        trd.setClientInfo("javaclient", 1); //Set client information
        trd.setConnSpi(this); //Set connection callback
        trd.setTrdSpi(this); //Set transaction callback
        qot.setClientInfo("javaclient", 1);
        qot.setConnSpi(this);
        qot.setQotSpi(this);
    }

    public void start() {
        var rsaKey = System.getenv("FUTUOPEND_RSAKEY");
        var host = System.getenv("FUTUOPEND_HOST");
        var port = System.getenv("FUTUOPEND_PORT");
        byte[] decodedBytes = Base64.getDecoder().decode(rsaKey);
        var decodedRsaKey = new String(decodedBytes);
        decodedRsaKey = decodedRsaKey.replace("\\n", "\n");
        trd.setRSAPrivateKey(decodedRsaKey);
        trd.initConnect(host, Integer.parseInt(port), true);
        qot.setRSAPrivateKey(decodedRsaKey);
        qot.initConnect(host, Integer.parseInt(port), true);
    }

    @Override
    public void onInitConnect(FTAPI_Conn client, long errCode, String desc)
    {
        System.out.printf("Trd onInitConnect: ret=%b desc=%s connID=%d", errCode, desc, client.getConnectID());
        if (errCode != 0)
            return;

        System.out.printf("Qot onInitConnect: ret=%b desc=%s connID=%d\n", errCode, desc, client.getConnectID());
        if (errCode != 0)
            return;

        inited++;

        if (inited >= 2) {
//            requestSnapshotQuote();
//                    getAccounts();
 //      getTrades();
//        getTodayFills();
//        getPendingOrders();
//        unlockTrade();
//        placeOrder();
//        cancelOrder();
//        requestKlines();
            requestVOOKlines();
//          requestVOOSnapshotQuote();
        }
    }

    private void placeOrder() {
        TrdCommon.TrdHeader header = TrdCommon.TrdHeader.newBuilder()
                .setAccID(accId)
                .setTrdEnv(TrdCommon.TrdEnv.TrdEnv_Real_VALUE)
                .setTrdMarket(TrdCommon.TrdMarket.TrdMarket_HK_VALUE)
                .build();
        TrdPlaceOrder.C2S c2s = TrdPlaceOrder.C2S.newBuilder()
                .setPacketID(trd.nextPacketID())
                .setHeader(header)
                .setTrdSide(TrdCommon.TrdSide.TrdSide_Buy_VALUE)
                .setOrderType(TrdCommon.OrderType.OrderType_Normal_VALUE)
                .setSecMarket(TrdCommon.TrdSecMarket.TrdSecMarket_HK_VALUE)
                .setTimeInForce(TrdCommon.TimeInForce.TimeInForce_GTC_VALUE)
                .setCode("02800")
                .setQty(500)
                .setPrice(15.1)
                .build();
        TrdPlaceOrder.Request req = TrdPlaceOrder.Request.newBuilder().setC2S(c2s).build();
        int seqNo = trd.placeOrder(req);
        System.out.printf("Send TrdPlaceOrder: %d\n", seqNo);
    }

    private void cancelOrder() {
        TrdCommon.TrdHeader header = TrdCommon.TrdHeader.newBuilder()
                .setAccID(accId)
                .setTrdEnv(TrdCommon.TrdEnv.TrdEnv_Real_VALUE)
                .setTrdMarket(TrdCommon.TrdMarket.TrdMarket_HK_VALUE)
                .build();
        TrdModifyOrder.C2S c2s = TrdModifyOrder.C2S.newBuilder()
                .setPacketID(trd.nextPacketID())
                .setHeader(header)
                .setOrderID(1841652347653646530L)
                .setModifyOrderOp(TrdCommon.ModifyOrderOp.ModifyOrderOp_Cancel_VALUE)
                .build();
        TrdModifyOrder.Request req = TrdModifyOrder.Request.newBuilder().setC2S(c2s).build();
        int seqNo = trd.modifyOrder(req);
        System.out.printf("Send TrdModifyOrder: %d\n", seqNo);
    }

    private void getPendingOrders() {
        TrdCommon.TrdHeader header = TrdCommon.TrdHeader.newBuilder()
                .setAccID(accId)
                .setTrdEnv(TrdCommon.TrdEnv.TrdEnv_Real_VALUE)
                .setTrdMarket(TrdCommon.TrdMarket.TrdMarket_HK_VALUE)
                .build();
        TrdGetOrderList.C2S c2s = TrdGetOrderList.C2S.newBuilder()
                .setHeader(header)
                .build();
        TrdGetOrderList.Request req = TrdGetOrderList.Request.newBuilder().setC2S(c2s).build();
        int seqNo = trd.getOrderList(req);
        System.out.printf("Send TrdGetOrderList: %d\n", seqNo);
    }

    private void getTodayFills() {
        TrdCommon.TrdHeader header = TrdCommon.TrdHeader.newBuilder()
                .setAccID(accId)
                .setTrdEnv(TrdCommon.TrdEnv.TrdEnv_Real_VALUE)
                .setTrdMarket(TrdCommon.TrdMarket.TrdMarket_HK_VALUE)
                .build();
        TrdGetOrderFillList.C2S c2s = TrdGetOrderFillList.C2S.newBuilder()
                .setHeader(header)
                .build();
        TrdGetOrderFillList.Request req = TrdGetOrderFillList.Request.newBuilder().setC2S(c2s).build();
        int seqNo = trd.getOrderFillList(req);
        System.out.printf("Send TrdGetOrderFillList: %d\n", seqNo);
    }

    private void getTrades() {
        TrdCommon.TrdHeader header = TrdCommon.TrdHeader.newBuilder()
                .setAccID(accId)
                .setTrdEnv(TrdCommon.TrdEnv.TrdEnv_Real_VALUE)
                .setTrdMarket(TrdCommon.TrdMarket.TrdMarket_HK_VALUE)
                .build();
        TrdCommon.TrdFilterConditions filter = TrdCommon.TrdFilterConditions.newBuilder()
                .setBeginTime("2025-02-12 00:00:00")
                .setEndTime("2025-02-13 00:00:00")
                .build();
        TrdGetHistoryOrderFillList.C2S c2s = TrdGetHistoryOrderFillList.C2S.newBuilder()
                .setHeader(header)
                .setFilterConditions(filter)
                .build();
        TrdGetHistoryOrderFillList.Request req = TrdGetHistoryOrderFillList.Request.newBuilder().setC2S(c2s).build();
        int seqNo = trd.getHistoryOrderFillList(req);
        System.out.printf("Send TrdGetHistoryOrderFillList: %d", seqNo);
    }

    private void unlockTrade() {
        TrdUnlockTrade.C2S c2s = TrdUnlockTrade.C2S.newBuilder()
                .setPwdMD5(tradeCode)
                .setUnlock(true)
                .setSecurityFirm(TrdCommon.SecurityFirm.SecurityFirm_FutuSecurities_VALUE)
                .build();
        TrdUnlockTrade.Request req = TrdUnlockTrade.Request.newBuilder().setC2S(c2s).build();
        int seqNo = trd.unlockTrade(req);
        System.out.printf("Send TrdUnlockTrade: %d\n", seqNo);
    }

    private void getAccounts() {
        TrdGetAccList.C2S c2s = TrdGetAccList.C2S.newBuilder()
                .setUserID(1)
                .setNeedGeneralSecAccount(true)
                .build();
        TrdGetAccList.Request req = TrdGetAccList.Request.newBuilder().setC2S(c2s).build();
        int seqNo = trd.getAccList(req);
        System.out.printf("Send TrdGetAccList: %d", seqNo);
    }

    private void requestKlines() {
        QotCommon.Security sec = QotCommon.Security.newBuilder()
                .setMarket(QotCommon.QotMarket.QotMarket_HK_Security_VALUE)
                .setCode("02800")
                .build();
        QotRequestHistoryKL.C2S c2s = QotRequestHistoryKL.C2S.newBuilder()
                .setRehabType(QotCommon.RehabType.RehabType_None_VALUE)
                .setKlType(QotCommon.KLType.KLType_5Min_VALUE)
                .setSecurity(sec)
                .setBeginTime("2025-01-01")
                .setEndTime("2025-01-22")
                .build();
        QotRequestHistoryKL.Request req = QotRequestHistoryKL.Request.newBuilder().setC2S(c2s).build();
        int seqNo = qot.requestHistoryKL(req);
        System.out.printf("Send QotRequestHistoryKL: %d\n", seqNo);
    }

    private void requestVOOKlines() {
        QotCommon.Security sec = QotCommon.Security.newBuilder()
                .setMarket(QotCommon.QotMarket.QotMarket_US_Security_VALUE)
                .setCode("VOO")
                .build();
        QotRequestHistoryKL.C2S c2s = QotRequestHistoryKL.C2S.newBuilder()
                .setRehabType(QotCommon.RehabType.RehabType_None_VALUE)
                .setKlType(QotCommon.KLType.KLType_5Min_VALUE)
                .setSecurity(sec)
                .setBeginTime("2024-01-01")
                .setEndTime("2024-12-31")
                .build();
        QotRequestHistoryKL.Request req = QotRequestHistoryKL.Request.newBuilder().setC2S(c2s).build();
        int seqNo = qot.requestHistoryKL(req);
        System.out.printf("Send QotRequestHistoryKL: %d\n", seqNo);
    }

    private void requestSnapshotQuote() {
        QotCommon.Security sec = QotCommon.Security.newBuilder()
                .setMarket(QotCommon.QotMarket.QotMarket_HK_Security_VALUE)
                .setCode("02800")
                .build();
        QotGetSecuritySnapshot.C2S c2s = QotGetSecuritySnapshot.C2S.newBuilder()
                .addSecurityList(sec)
                .build();
        QotGetSecuritySnapshot.Request req = QotGetSecuritySnapshot.Request.newBuilder().setC2S(c2s).build();
        int seqNo = qot.getSecuritySnapshot(req);
        System.out.printf("Send QotGetSecuritySnapshot: %d\n", seqNo);
    }

    // need Nasdaq basic subscription
    private void requestVOOSnapshotQuote() {
        QotCommon.Security sec = QotCommon.Security.newBuilder()
                .setMarket(QotCommon.QotMarket.QotMarket_US_Security_VALUE)
                .setCode("VOO")
                .build();
        QotGetSecuritySnapshot.C2S c2s = QotGetSecuritySnapshot.C2S.newBuilder()
                .addSecurityList(sec)
                .build();
        QotGetSecuritySnapshot.Request req = QotGetSecuritySnapshot.Request.newBuilder().setC2S(c2s).build();
        int seqNo = qot.getSecuritySnapshot(req);
        System.out.printf("Send QotGetSecuritySnapshot: %d\n", seqNo);
    }

    @Override
    public void onDisconnect(FTAPI_Conn client, long errCode) {
        System.out.printf("Trd onDisConnect: %d", errCode);
    }

    @Override
    public void onReply_ModifyOrder(FTAPI_Conn client, int nSerialNo, TrdModifyOrder.Response rsp) {
        if (rsp.getRetType() != 0) {
            System.out.printf("TrdModifyOrder failed: %s\n", rsp.getRetMsg());
        }
        else {
            try {
                String json = JsonFormat.printer().print(rsp);
                System.out.printf("Receive TrdModifyOrder: %s\n", json);
            } catch (InvalidProtocolBufferException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onReply_PlaceOrder(FTAPI_Conn client, int nSerialNo, TrdPlaceOrder.Response rsp) {
        if (rsp.getRetType() != 0) {
            System.out.printf("TrdPlaceOrder failed: %s\n", rsp.getRetMsg());
        }
        else {
            try {
                String json = JsonFormat.printer().print(rsp);
                System.out.printf("Receive TrdPlaceOrder: %s\n", json);
            } catch (InvalidProtocolBufferException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onReply_UnlockTrade(FTAPI_Conn client, int nSerialNo, TrdUnlockTrade.Response rsp) {
        if (rsp.getRetType() != 0) {
            System.out.printf("TrdUnlockTrade failed: %s\n", rsp.getRetMsg());
        }
        else {
            try {
                String json = JsonFormat.printer().print(rsp);
                System.out.printf("Receive TrdUnlockTrade: %s\n", json);
            } catch (InvalidProtocolBufferException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onReply_GetAccList(FTAPI_Conn client, int nSerialNo, TrdGetAccList.Response rsp) {
        if (rsp.getRetType() != 0) {
            System.out.printf("TrdGetAccList failed: %s", rsp.getRetMsg());
        }
        else {
            try {
                String json = JsonFormat.printer().print(rsp);
                System.out.printf("Receive TrdGetAccList: %s", json);
            } catch (InvalidProtocolBufferException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onReply_GetOrderList(FTAPI_Conn client, int nSerialNo, TrdGetOrderList.Response rsp) {
        if (rsp.getRetType() != 0) {
            System.out.printf("TrdGetOrderList failed: %s", rsp.getRetMsg());
        }
        else {
            try {
                String json = JsonFormat.printer().print(rsp);
                System.out.printf("Receive TrdGetOrderList: %s \n", json);
                var pendingOrders = rsp.getS2C().getOrderListList();
                pendingOrders.forEach(o -> System.out.println(o.toString().replaceAll("\n", " ")));
            } catch (InvalidProtocolBufferException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onReply_GetOrderFillList(FTAPI_Conn client, int nSerialNo, TrdGetOrderFillList.Response rsp) {
        if (rsp.getRetType() != 0) {
            System.out.printf("TrdGetOrderFillList failed: %s\n", rsp.getRetMsg());
        }
        else {
            try {
                var executions = rsp.getS2C().getOrderFillListList();
                String json = JsonFormat.printer().print(rsp);
                System.out.printf("Receive TrdGetOrderFillList: %s\n", json);
                System.out.println(executions);
            } catch (InvalidProtocolBufferException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onReply_GetHistoryOrderFillList(FTAPI_Conn client, int nSerialNo, TrdGetHistoryOrderFillList.Response rsp) {
        if (rsp.getRetType() != 0) {
            System.out.printf("TrdGetHistoryOrderFillList failed: %s", rsp.getRetMsg());
        }
        else {
            try {
                String json = JsonFormat.printer().print(rsp);
                System.out.printf("Receive TrdGetHistoryOrderFillList: %s", json);
                var executions = rsp.getS2C().getOrderFillListList();
                System.out.println(executions);
            } catch (InvalidProtocolBufferException e) {
                e.printStackTrace();
            }
        }
    }

    // need mkt data subscription
    @Override
    public void onReply_RequestHistoryKL(FTAPI_Conn client, int nSerialNo, QotRequestHistoryKL.Response rsp) {
        if (rsp.getRetType() != 0) {
            System.out.printf("QotRequestHistoryKL failed: %s\n", rsp.getRetMsg());
        }
        else {
            try {
                String json = JsonFormat.printer().print(rsp);
                Path path = Paths.get("/tmp/VOO-US-kline-5m-2024.json");
                Files.write(path, json.getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onReply_GetBasicQot(FTAPI_Conn client, int nSerialNo, QotGetBasicQot.Response rsp) {
        if (rsp.getRetType() != 0) {
            System.out.printf("QotGetBasicQot failed: %s\n", rsp.getRetMsg());
        }
        else {
            try {
                String json = JsonFormat.printer().print(rsp);
                System.out.printf("Receive QotGetBasicQot: %s\n", json);
            } catch (InvalidProtocolBufferException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onReply_GetSecuritySnapshot(FTAPI_Conn client, int nSerialNo, QotGetSecuritySnapshot.Response rsp) {
        if (rsp.getRetType() != 0) {
            System.out.printf("QotGetSecuritySnapshot failed: %s\n", rsp.getRetMsg());
        }
        else {
            try {
                String json = JsonFormat.printer().print(rsp);
                System.out.printf("Receive QotGetSecuritySnapshot: %s\n", json);
            } catch (InvalidProtocolBufferException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        FTAPI.init();
        TrdDemo trd = new TrdDemo();
        trd.start();

        while (true) {
            try {
                Thread.sleep(1000 * 600);
            } catch (InterruptedException exc) {

            }
        }
    }
}

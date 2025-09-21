package squote.playground;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import squote.SquoteConstants;
import squote.domain.Market;
import squote.service.ibkr.IBAPIClient;

import java.util.Calendar;

public class IBAPIClientPlayground {
    private static final Logger log = LoggerFactory.getLogger(IBAPIClientPlayground.class);

    private static final String HOST = System.getenv("IB_GATEWAY_HOST");
    private static final int PORT = 4001;
    private static final String REPORT_QUERY_TOKEN = System.getenv("IB_REPORT_QUERY_TOKEN");
    private static final String EXECUTION_REPORT_QUERY_ID = System.getenv("IB_EXECUTION_REPORT_QUERY_ID");
    private static final int BASE_CLIENT_ID = 1000;

    public static void main(String[] args) {
//        new IBAPIClientPlayground().execute();
        new IBAPIClientPlayground().getExecutionReport();
    }

    public void getExecutionReport() {
        var client = new IBAPIClient(REPORT_QUERY_TOKEN, EXECUTION_REPORT_QUERY_ID);

        var calendar = Calendar.getInstance();
        calendar.add(Calendar.MONTH, -3);
        var threeMonthsAgo = calendar.getTime();
        
        var executions = client.getHistoricalExecutions(threeMonthsAgo, Market.US);

        log.info("Retrieved {} executions from the last 3 months", executions.size());
        log.info("Execution Report - Last 3 Months: Total executions: {}", executions.size());
        
        if (!executions.isEmpty()) {
            log.info("Execution details:");
            executions.forEach((orderId, execution) -> {
                log.info("Order ID: {}, Execution: {}", orderId, execution);
            });
        } else {
            log.info("No executions found for the last 3 months.");
        }
    }

    public void execute() {
        log.info("Starting IBAPIClient playground tests...");
        IBAPIClient client = new IBAPIClient(HOST, PORT, BASE_CLIENT_ID, REPORT_QUERY_TOKEN, EXECUTION_REPORT_QUERY_ID);
        log.info("Successfully connected to IB Gateway");

        var response = client.placeOrder(SquoteConstants.Side.BUY, "SPHB.US", 1, 80);

        var orders = client.getPendingOrders(Market.US);
        System.out.println(orders.size());

        var cancelResponse = client.cancelOrder(orders.get(0).orderId(), orders.get(0).code());
        System.out.println(cancelResponse.errorCode());

        for (int i=0; i<1000000; i++) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        log.info("IBAPIClient playground tests completed.");
    }
} 

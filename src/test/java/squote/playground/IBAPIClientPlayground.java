package squote.playground;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import squote.domain.ExchangeCode;
import squote.domain.Order;

import java.util.List;

public class IBAPIClientPlayground {
    private static final Logger log = LoggerFactory.getLogger(IBAPIClientPlayground.class);

    private static final String HOST = System.getenv("IB_GATEWAY_HOST");
    private static final int PORT = 4001;
    private static final int BASE_CLIENT_ID = 1000;

    public static void main(String[] args) {
        new IBAPIClientPlayground().execute();
    }

    public void execute() {
        log.info("Starting IBAPIClient playground tests...");

//        testConnectionTimeout();
        // testIBGatewayConnection();
        testGetPendingOrders();

        log.info("IBAPIClient playground tests completed.");
    }

    private void testConnectionTimeout() {
        log.info("=== Testing connection timeout ===");
        long startTime = System.currentTimeMillis();
        try {
            new IBAPIClient("999.999.999.999", 9999, 1001);
            throw new RuntimeException("Expected timeout exception but connection succeeded");
        } catch (RuntimeException e) {
            long duration = System.currentTimeMillis() - startTime;
            log.info("✓ Connection timeout test passed: {}", e.getMessage());
        }
    }

    private void testIBGatewayConnection() {
        log.info("=== Testing IB Gateway connection ({}:{}) ===", HOST, PORT);
        try {
            IBAPIClient client = new IBAPIClient(HOST, PORT, BASE_CLIENT_ID + 1);
            log.info("✓ Successfully connected to IB Gateway");
            log.info("Connection status: {}", client.isConnected());
            client.close();
            log.info("Connection closed");
        } catch (RuntimeException e) {
            log.warn("✗ IB Gateway connection failed: {}", e.getMessage());
            log.info("Make sure IB Gateway is running on {}:{}", HOST, PORT);
        }
    }

    private void testGetPendingOrders() {
        log.info("=== Testing getPendingOrders (expecting no orders) ===");
        try {
            IBAPIClient client = new IBAPIClient(HOST, PORT, BASE_CLIENT_ID + 2);
            log.info("✓ Successfully connected to IB Gateway for pending orders test");
            
            List<Order> pendingOrders = client.getPendingOrders(ExchangeCode.Market.US);
            log.info("Retrieved {} pending orders", pendingOrders.size());
            
            if (pendingOrders.isEmpty()) {
                log.info("✓ Test passed: No pending orders returned as expected");
            } else {
                log.info("✓ Test completed: Found {} pending orders:", pendingOrders.size());
                for (Order order : pendingOrders) {
                    log.info("  - Order: {} {} {} @ {} (ID: {})", 
                        order.side(), order.quantity(), order.code(), 
                        order.price(), order.orderId());
                }
            }
            
            client.close();
            log.info("Connection closed");
        } catch (Exception e) {
            log.error("✗ getPendingOrders test failed: {}", e.getMessage(), e);
            log.info("Make sure IB Gateway is running on {}:{}", HOST, PORT);
        }
    }
} 

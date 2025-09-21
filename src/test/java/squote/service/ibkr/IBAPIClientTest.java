package squote.service.ibkr;

import com.ib.client.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import squote.SquoteConstants;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class IBAPIClientTest {
    @Mock private EJavaSignal mockSignal;
    @Mock private EClientSocket mockClient;
    @Mock private EReader mockReader;
    @Mock private Contract mockContract;
    @Mock private com.ib.client.Order mockIBOrder;
    @Mock private OrderState mockOrderState;

    private IBAPIClient ibApiClient;

    @BeforeEach
    void setUp() {
        ibApiClient = new IBAPIClient(mockSignal, mockClient, mockReader, "", "");
    }

    @Test
    void testPlaceOrderSuccess() {
        var orderId = 100;
        ibApiClient.nextValidId(orderId);
        when(mockOrderState.status()).thenReturn(OrderStatus.Submitted);
        ibApiClient.openOrder(orderId, new Contract(), mockIBOrder, mockOrderState);

        var response = ibApiClient.placeOrder(SquoteConstants.Side.BUY, "QQQ.US", 100, 500.0);

        assertThat(response.orderId()).isEqualTo(orderId);
    }

    @Test
    void testCancelOrder() {
        var orderId = 100;
        ibApiClient.orderStatus(orderId, "Cancelled", Decimal.ZERO, Decimal.ZERO, 0.0, 0, 0, 0.0, 0, null, 0.0);
        var response = ibApiClient.cancelOrder(orderId, "any");

        verify(mockClient).cancelOrder(anyInt(), any());
        assertThat(response.errorCode()).isEqualTo(-1);
    }

}


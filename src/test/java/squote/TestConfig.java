package squote;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import squote.security.AuthenticationService;
import squote.security.AuthenticationServiceStub;
import squote.service.BinanceAPIService;

@TestConfiguration
public class TestConfig {

    @Bean
    @Primary
    public AuthenticationService authenticationService() {
        return new AuthenticationServiceStub();
    }

    @Bean
    @Primary
    public BinanceAPIService binanceAPIService() {
        return new BinanceAPIService();
    }
}

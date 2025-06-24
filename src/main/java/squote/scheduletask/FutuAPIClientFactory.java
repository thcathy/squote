package squote.scheduletask;

import squote.service.FutuAPIClient;

@FunctionalInterface
public interface FutuAPIClientFactory {
    FutuAPIClient build(FutuClientConfig futuClientConfig);
}

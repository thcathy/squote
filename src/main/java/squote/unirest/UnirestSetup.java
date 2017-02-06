package squote.unirest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.mashape.unirest.http.ObjectMapper;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.utils.Base64Coder;
import org.apache.http.HttpHost;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.ssl.SSLContexts;
import thc.util.ProxySetting;

import javax.net.ssl.SSLContext;
import java.io.IOException;

/**
 * Created by wongtim on 21/07/2016.
 */
public class UnirestSetup {
    public static volatile int MAX_TOTAL_HTTP_CONNECTION = 20;
    public static volatile int MAX_HTTP_CONNECTION_PER_ROUTE = 20;
    public static volatile int HTTP_TIMEOUT = 300000;

    public static void setupAll() {
        Unirest.setConcurrency(MAX_TOTAL_HTTP_CONNECTION, MAX_HTTP_CONNECTION_PER_ROUTE);
        Unirest.setTimeouts(HTTP_TIMEOUT, HTTP_TIMEOUT);


        setDefaultHeaders();
        setupProxy();
        setupJackson();
    }

    private static void setupJackson() {
        Unirest.setObjectMapper(new ObjectMapper() {
            private com.fasterxml.jackson.databind.ObjectMapper jacksonObjectMapper = new com.fasterxml.jackson.databind.ObjectMapper();

            public <T> T readValue(String value, Class<T> valueType) {
                try {
                    return jacksonObjectMapper.readValue(value, valueType);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            public String writeValue(Object value) {
                try {
                    return jacksonObjectMapper.writeValueAsString(value);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    private static void setDefaultHeaders() {
        Unirest.setDefaultHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
        Unirest.setDefaultHeader("Connection", "keep-alive");
        Unirest.setDefaultHeader("Accept-Encoding", "gzip, deflate, sdch");
        Unirest.setDefaultHeader("Accept-Language", "en-US,en;q=0.8");
        Unirest.setDefaultHeader("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_10_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/44.0.2403.130 Safari/537.36");
    }

    private static void setupProxy() {
        ProxySetting proxySetting = new ProxySetting(
                System.getProperty("http.proxyHost"),
                System.getProperty("http.proxyPort"),
                System.getProperty("http.proxyUsername"),
                System.getProperty("http.proxyPassword"));
        if (proxySetting.hasProxyServer()) {
            Unirest.setProxy(new HttpHost(proxySetting.host, Integer.valueOf(proxySetting.port)));
            Unirest.setDefaultHeader("Authorization", "Basic " + Base64Coder.encodeString(proxySetting.username + ":" + proxySetting.password));
        }
    }

    private static CloseableHttpAsyncClient createSSLClient() {
        TrustStrategy acceptingTrustStrategy = (arg0, arg1) -> true;

        SSLContext sslContext = null;
        try {
            sslContext = SSLContexts.custom().loadTrustMaterial(null, acceptingTrustStrategy).build();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return HttpAsyncClients.custom()
                .setHostnameVerifier(SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER)
                .setSSLContext(sslContext).build();

    }
}




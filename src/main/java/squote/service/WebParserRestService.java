package squote.service;

import com.google.common.base.Strings;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.apache.commons.lang3.StringUtils;
import squote.domain.ForumThread;
import squote.domain.MonetaryBase;
import squote.domain.StockQuote;
import squote.unirest.UnirestSetup;

import java.util.Collection;
import java.util.concurrent.Future;

/**
 * Created by thcathy on 2/8/2016.
 */
public class WebParserRestService {
    private final String host;

    public WebParserRestService(String apiHost) {
        if (Strings.isNullOrEmpty(apiHost)) throw new IllegalArgumentException("Cannot create WebParserRestService without API server's host");

        if (!apiHost.contains("http://")) apiHost = "http://" + apiHost;
        if (!apiHost.endsWith("/")) apiHost = apiHost + "/";
        this.host = apiHost;
    }

    static {
        UnirestSetup.setupAll();
    }

    public Future<HttpResponse<ForumThread[]>> getForumThreads(String type, int batch) throws UnirestException {
        return Unirest.get(host + "rest/forum/list/{type}/{batch}")
                .routeParam("type", type)
                .routeParam("batch", Integer.toString(batch))
                .asObjectAsync(ForumThread[].class);
    }

    public Future<HttpResponse<StockQuote[]>> getIndexQuotes() {
        return Unirest.get(host + "rest/quote/indexes")
                .asObjectAsync(StockQuote[].class);
    }

    public Future<HttpResponse<StockQuote>> getFullQuote(String code) {
        return Unirest.get(host + "rest/quote/full/{code}")
                .routeParam("code", code)
                .asObjectAsync(StockQuote.class);
    }

    public Future<HttpResponse<StockQuote[]>> getRealTimeQuotes(Collection<String> codes) {
        return Unirest.get(host + "rest/quote/realtime/list/{codes}")
                .routeParam("codes", StringUtils.join(codes, ","))
                .asObjectAsync(StockQuote[].class);
    }

    public Future<HttpResponse<StockQuote[]>> getHSINetReports(String yyyymmdd) {
        return Unirest.get(host + "rest/index/report/hsinet/{yyyymmdd}")
                .routeParam("yyyymmdd", yyyymmdd)
                .asObjectAsync(StockQuote[].class);
    }

    public Future<HttpResponse<MonetaryBase>> getHKMAReport(String yyyymmdd) {
        return Unirest.get(host + "rest/hkma/report/{yyyymmdd}")
                .routeParam("yyyymmdd", yyyymmdd)
                .asObjectAsync(MonetaryBase.class);
    }

    public Future<HttpResponse<String[]>> getConstituents(String index) {
        return Unirest.get(host + "rest/index/constituents/{index}")
                .routeParam("index", index)
                .asObjectAsync(String[].class);
    }

    public Future<HttpResponse<String>> getHistoryPrice(String code, String preYear) {
        return Unirest.get(host + "/rest/quote/{code}/price/pre/{preYear}")
                .routeParam("code", code)
                .routeParam("preYear", preYear)
                .asObjectAsync(String.class);
    }
}

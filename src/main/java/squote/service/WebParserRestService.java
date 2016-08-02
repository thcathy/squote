package squote.service;

import com.google.common.base.Strings;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import squote.domain.ForumThread;
import squote.unirest.UnirestSetup;

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

}

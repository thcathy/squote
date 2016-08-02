package squote.service;

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

    static {
        UnirestSetup.setupAll();
    }

    public Future<HttpResponse<ForumThread[]>> getForumThreads(String type, int batch) throws UnirestException {

        return Unirest.get("http://192.168.99.100:8191/rest/forum/list/{type}/{batch}")
                .routeParam("type", type)
                .routeParam("batch", Integer.toString(batch))
                .asObjectAsync(ForumThread[].class);
    }
}

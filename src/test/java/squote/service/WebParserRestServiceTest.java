package squote.service;

import com.google.common.base.Stopwatch;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import squote.domain.ForumThread;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertTrue;

/**
 * Created by thcathy on 2/8/2016.
 */
public class WebParserRestServiceTest {
    private Logger log = LoggerFactory.getLogger(WebParserRestServiceTest.class);

    WebParserRestService service = new WebParserRestService();

    private static int MIN_NUM_OF_THREADS = 200;

    @Test
    public void getForumThreads_MusicPage1_ShouldReturn() throws Exception {
        Stopwatch timer = Stopwatch.createStarted();

        List<ForumThread> contents = Arrays.asList(service.getForumThreads("MUSIC", 1).get().getBody());

        log.info("list_MusicPage1_ShouldReturnDecendingForumThreadsNotOlderThanConfig took: {}", timer.stop());

        assertTrue("Number of thread " + contents.size() + " < " + MIN_NUM_OF_THREADS, contents.size() > MIN_NUM_OF_THREADS);

    }
}

package squote.service;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by thcathy on 2/8/2016.
 */
public class WebParserRestServiceTest {
    private Logger log = LoggerFactory.getLogger(WebParserRestServiceTest.class);

    @Test(expected = IllegalArgumentException.class)
    public void createServiceWithoutHost_ShouldThrowException() {
        new WebParserRestService(null);
    }

}

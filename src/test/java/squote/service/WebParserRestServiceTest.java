package squote.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebParserRestServiceTest {
    private Logger log = LoggerFactory.getLogger(WebParserRestServiceTest.class);

    @Test
    public void createServiceWithoutHost_ShouldThrowException() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            new WebParserRestService(null);
        });
    }

}

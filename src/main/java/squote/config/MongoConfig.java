package squote.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import squote.config.converters.FundReadConverter;
import squote.config.converters.FundWriteConverter;

import java.util.Arrays;

@Configuration
public class MongoConfig {

    public static final String DOT_REPLACEMENT = "___DOT___";

    @Bean
    public MongoCustomConversions customConversions() {
        return new MongoCustomConversions(Arrays.asList(
            new FundReadConverter(),
            new FundWriteConverter()
        ));
    }
}

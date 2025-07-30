package squote.config;

import org.bson.Document;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
public class MongoConfig extends AbstractMongoClientConfiguration {
    
    @Override
    protected String getDatabaseName() {
        return "squote";
    }
    
    @Override
    public MongoCustomConversions customConversions() {
        List<Converter<?, ?>> converters = new ArrayList<>();
        converters.add(new MapToDocumentConverter());
        converters.add(new DocumentToMapConverter());
        return new MongoCustomConversions(converters);
    }
    
    private static class MapToDocumentConverter implements Converter<Map<String, Object>, Document> {
        private static final String DOT_REPLACEMENT = "\\u002E"; // Unicode escape for dot
        
        @Override
        public Document convert(Map<String, Object> source) {
            Document document = new Document();
            source.forEach((key, value) -> {
                String escapedKey = key.replace(".", DOT_REPLACEMENT);
                document.put(escapedKey, value);
            });
            return document;
        }
    }
    
    private static class DocumentToMapConverter implements Converter<Document, Map<String, Object>> {
        private static final String DOT_REPLACEMENT = "\\u002E"; // Unicode escape for dot
        
        @Override
        public Map<String, Object> convert(Document source) {
            Map<String, Object> map = new HashMap<>();
            source.forEach((key, value) -> {
                String unescapedKey = key.replace(DOT_REPLACEMENT, ".");
                map.put(unescapedKey, value);
            });
            return map;
        }
    }
}

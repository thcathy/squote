package squote.config.converters;

import org.bson.Document;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import squote.config.MongoConfig;
import squote.domain.AlgoConfig;
import squote.domain.Fund;
import squote.domain.FundHolding;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ReadingConverter
public class FundReadConverter implements Converter<Document, Fund> {

    @Override
    public Fund convert(Document document) {
        String userId = document.getString("userId");
        String name = document.getString("name");
        
        Fund fund = new Fund(userId, name);
        
        // Set basic fields
        String id = document.getString("_id");
        if (id == null && document.getObjectId("_id") != null) {
            // Handle case where _id is stored as ObjectId but we need String
            id = document.getObjectId("_id").toString();
        }
        fund.setId(id);
        fund.setProfit(getBigDecimal(document, "profit"));
        fund.setNetProfit(getBigDecimal(document, "netProfit"));
        fund.setCashoutAmount(getBigDecimal(document, "cashoutAmount"));
        fund.setCashinAmount(getBigDecimal(document, "cashinAmount"));
        
        // Handle type field safely
        String typeStr = document.getString("type");
        if (typeStr != null) {
            fund.setType(Fund.FundType.valueOf(typeStr));
        }
        
        // Handle date field
        Date date = document.getDate("date");
        if (date != null) {
            fund.setDate(date);
        }
        
        // Decode dots in holdings map keys
        Document holdingsDoc = document.get("holdings", Document.class);
        if (holdingsDoc != null) {
            Map<String, FundHolding> holdings = new ConcurrentHashMap<>();
            for (Map.Entry<String, Object> entry : holdingsDoc.entrySet()) {
                String decodedKey = decodeDots(entry.getKey());
                Document holdingDoc = (Document) entry.getValue();
                FundHolding holding = convertToFundHolding(holdingDoc);
                holdings.put(decodedKey, holding);
            }
            fund.setHoldings(holdings);
        }
        
        // Decode dots in algoConfigs map keys
        Document algoConfigsDoc = document.get("algoConfigs", Document.class);
        if (algoConfigsDoc != null) {
            Map<String, AlgoConfig> algoConfigs = new ConcurrentHashMap<>();
            for (Map.Entry<String, Object> entry : algoConfigsDoc.entrySet()) {
                String decodedKey = decodeDots(entry.getKey());
                Document configDoc = (Document) entry.getValue();
                AlgoConfig config = convertToAlgoConfig(configDoc);
                algoConfigs.put(decodedKey, config);
            }
            fund.setAlgoConfigs(algoConfigs);
        }
        
        return fund;
    }
    
    private String decodeDots(String key) {
        return key.replace(MongoConfig.DOT_REPLACEMENT, ".");
    }
    
    private BigDecimal getBigDecimal(Document doc, String field) {
        Object value = doc.get(field);
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }
        return new BigDecimal(value.toString());
    }
    
    private FundHolding convertToFundHolding(Document doc) {
        String code = doc.getString("code");
        BigDecimal quantity = getBigDecimal(doc, "quantity");
        BigDecimal gross = getBigDecimal(doc, "gross");
        Date date = doc.getDate("date");
        
        FundHolding holding = new FundHolding(code, quantity, gross, date);
        Long latestTradeTime = doc.getLong("latestTradeTime");
        if (latestTradeTime != null) {
            holding.setLatestTradeTime(latestTradeTime);
        }
        return holding;
    }
    
    private AlgoConfig convertToAlgoConfig(Document doc) {
        return new AlgoConfig(
            doc.getString("code"),
            doc.getInteger("quantity", 0),
            doc.getDouble("basePrice"),
            doc.getInteger("stdDevRange", 0),
            doc.getDouble("stdDevMultiplier"),
            doc.getDouble("grossAmount")
        );
    }
}

package squote.config.converters;

import org.bson.Document;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;
import squote.domain.AlgoConfig;
import squote.domain.Fund;
import squote.domain.FundHolding;

import java.util.HashMap;
import java.util.Map;

import static squote.config.MongoConfig.DOT_REPLACEMENT;

@WritingConverter
public class FundWriteConverter implements Converter<Fund, Document> {

    @Override
    public Document convert(Fund fund) {
        Document document = new Document();
        
        // Copy basic fields
        if (fund.getId() != null) {
            document.put("_id", fund.getId());
        }
        document.put("name", fund.name);
        document.put("userId", fund.userId);
        document.put("date", fund.getDate());
        document.put("profit", fund.getProfit());
        document.put("netProfit", fund.getNetProfit());
        document.put("cashoutAmount", fund.getCashoutAmount());
        document.put("cashinAmount", fund.getCashinAmount());
        document.put("type", fund.getType());
        
        // Encode dots in holdings map keys
        if (fund.getHoldings() != null) {
            Map<String, Object> encodedHoldings = new HashMap<>();
            for (Map.Entry<String, FundHolding> entry : fund.getHoldings().entrySet()) {
                String encodedKey = encodeDots(entry.getKey());
                encodedHoldings.put(encodedKey, convertFundHolding(entry.getValue()));
            }
            document.put("holdings", encodedHoldings);
        }
        
        // Encode dots in algoConfigs map keys
        if (fund.getAlgoConfigs() != null) {
            Map<String, Object> encodedAlgoConfigs = new HashMap<>();
            for (Map.Entry<String, AlgoConfig> entry : fund.getAlgoConfigs().entrySet()) {
                String encodedKey = encodeDots(entry.getKey());
                encodedAlgoConfigs.put(encodedKey, convertAlgoConfig(entry.getValue()));
            }
            document.put("algoConfigs", encodedAlgoConfigs);
        }
        
        return document;
    }
    
    private String encodeDots(String key) {
        return key.replace(".", DOT_REPLACEMENT);
    }
    
    private Document convertFundHolding(FundHolding holding) {
        Document doc = new Document();
        doc.put("code", holding.getCode());
        doc.put("quantity", holding.getQuantity());
        doc.put("gross", holding.getGross());
        doc.put("date", holding.getDate());
        doc.put("latestTradeTime", holding.getLatestTradeTime());
        return doc;
    }
    
    private Document convertAlgoConfig(AlgoConfig config) {
        Document doc = new Document();
        doc.put("code", config.code());
        doc.put("quantity", config.quantity());
        doc.put("basePrice", config.basePrice());
        doc.put("stdDevRange", config.stdDevRange());
        doc.put("stdDevMultiplier", config.stdDevMultiplier());
        doc.put("grossAmount", config.grossAmount());
        return doc;
    }
}

package squote.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import squote.service.yahoo.YahooTicker;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Base64;

public class YahooProtobufParser {
    private static final Logger logger = LoggerFactory.getLogger(YahooProtobufParser.class);

    public static YahooTicker parseYahooTicker(String base64Data) {
        try {
            byte[] data = Base64.getDecoder().decode(base64Data);
            return parseYahooTicker(data);
        } catch (Exception e) {
            logger.error("Failed to decode base64 protobuf message", e);
            return null;
        }
    }

    private static YahooTicker parseYahooTicker(byte[] data) {
        var ticker = new YahooTicker();
        ByteBuffer buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
        
        while (buffer.hasRemaining()) {
            try {
                // Read field tag and wire type
                int tag = readVarint(buffer);
                int fieldNumber = tag >>> 3;
                int wireType = tag & 0x7;
                
                switch (fieldNumber) {
                    case 1: // id (string)
                        if (wireType == 2) { // Length-delimited
                            ticker.setId(readString(buffer));
                        }
                        break;
                    case 2: // price (float)
                        if (wireType == 5) { // 32-bit
                            ticker.setPrice(buffer.getFloat());
                        }
                        break;
                    case 3: // time (sint64)
                        if (wireType == 0) { // Varint with zigzag encoding
                            long rawTime = readVarint64(buffer);
                            long decodedTime = (rawTime >>> 1) ^ -(rawTime & 1);
                            long timeInMillis = decodedTime;
                            ticker.setTime(timeInMillis);
                        }
                        break;
                    case 4: // currency (string)
                        if (wireType == 2) {
                            ticker.setCurrency(readString(buffer));
                        }
                        break;
                    case 5: // exchange (string)
                        if (wireType == 2) {
                            ticker.setExchange(readString(buffer));
                        }
                        break;
                    case 6: // quoteType (enum)
                        if (wireType == 0) {
                            ticker.setQuoteType(YahooTicker.QuoteType.fromValue(readVarint(buffer)));
                        }
                        break;
                    case 7: // marketHours (enum)
                        if (wireType == 0) {
                            ticker.setMarketHours(YahooTicker.MarketHours.fromValue(readVarint(buffer)));
                        }
                        break;
                    case 8: // changePercent (float)
                        if (wireType == 5) {
                            ticker.setChangePercent(buffer.getFloat());
                        }
                        break;
                    case 9: // dayVolume (int64)
                        if (wireType == 0) {
                            ticker.setDayVolume(readVarint64(buffer));
                        }
                        break;
                    case 10: // dayHigh (float)
                        if (wireType == 5) {
                            ticker.setDayHigh(buffer.getFloat());
                        }
                        break;
                    case 11: // dayLow (float)
                        if (wireType == 5) {
                            ticker.setDayLow(buffer.getFloat());
                        }
                        break;
                    case 12: // change (float)
                        if (wireType == 5) {
                            ticker.setChange(buffer.getFloat());
                        }
                        break;
                    // Add more fields as needed...
                    default:
                        // Skip unknown fields
                        skipField(buffer, wireType);
                        break;
                }
            } catch (Exception e) {
                logger.debug("Error parsing field, continuing with next field", e);
                break; // Stop parsing if we hit an error
            }
        }
        
        return ticker;
    }
    
    /**
     * Read a varint from the buffer
     */
    private static int readVarint(ByteBuffer buffer) {
        int result = 0;
        int shift = 0;
        
        while (buffer.hasRemaining()) {
            byte b = buffer.get();
            result |= (b & 0x7F) << shift;
            if ((b & 0x80) == 0) {
                break;
            }
            shift += 7;
        }
        
        return result;
    }
    
    /**
     * Read a 64-bit varint from the buffer
     */
    private static long readVarint64(ByteBuffer buffer) {
        long result = 0;
        int shift = 0;
        
        while (buffer.hasRemaining()) {
            byte b = buffer.get();
            result |= (long)(b & 0x7F) << shift;
            if ((b & 0x80) == 0) {
                break;
            }
            shift += 7;
        }
        
        return result;
    }
    
    /**
     * Read a length-delimited string from the buffer
     */
    private static String readString(ByteBuffer buffer) {
        int length = readVarint(buffer);
        if (length <= 0 || buffer.remaining() < length) {
            return "";
        }
        
        byte[] stringBytes = new byte[length];
        buffer.get(stringBytes);
        return new String(stringBytes);
    }
    
    /**
     * Skip a field based on its wire type
     */
    private static void skipField(ByteBuffer buffer, int wireType) {
        switch (wireType) {
            case 0: // Varint
                readVarint(buffer);
                break;
            case 1: // 64-bit
                if (buffer.remaining() >= 8) {
                    buffer.position(buffer.position() + 8);
                }
                break;
            case 2: // Length-delimited
                int length = readVarint(buffer);
                if (length > 0 && buffer.remaining() >= length) {
                    buffer.position(buffer.position() + length);
                }
                break;
            case 5: // 32-bit
                if (buffer.remaining() >= 4) {
                    buffer.position(buffer.position() + 4);
                }
                break;
            default:
                // Unknown wire type, skip one byte
                if (buffer.hasRemaining()) {
                    buffer.get();
                }
                break;
        }
    }
} 

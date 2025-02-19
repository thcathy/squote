package thc.util;

public class TradingUtils {
    public static int roundToLotSize(double quantity, int lotSize) {
        double qty = quantity / lotSize;
        int roundedQty = (int) Math.floor(qty);
        return roundedQty * lotSize;
    }
}

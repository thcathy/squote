package squote.domain;

public record AlgoConfig(String code, int quantity, Double basePrice, int stdDevRange, double stdDevMultiplier, Double grossAmount, boolean sellOnly, int lotSize) {

    public AlgoConfig(String code, int quantity, Double basePrice, int stdDevRange, double stdDevMultiplier, Double grossAmount, boolean sellOnly) {
        this(code, quantity, basePrice, stdDevRange, stdDevMultiplier, grossAmount, sellOnly, 0);
    }

    @Override
    public String toString() {
        return String.format("AlgoConfig{ %s qty=%d@%.2f gross=%.2f stdDevRange=%d stdDevMultiplier=%.2f sellOnly=%s lotSize=%d }",
            code, quantity, basePrice, grossAmount != null ? grossAmount : 0.0, stdDevRange, stdDevMultiplier, sellOnly, lotSize);
    }
}

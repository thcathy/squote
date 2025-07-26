package squote.domain;

public record AlgoConfig(String code, int quantity, Double basePrice, int stdDevRange, double stdDevMultiplier, Double grossAmount) {

    @Override
    public String toString() {
        return String.format("AlgoConfig{ %s qty=%d@%.2f gross=%.2f stdDevRange=%d stdDevMultiplier=%.2f }", 
            code, quantity, basePrice, grossAmount != null ? grossAmount : 0.0, stdDevRange, stdDevMultiplier);
    }
}

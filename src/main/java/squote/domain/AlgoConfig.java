package squote.domain;

public record AlgoConfig(String code, int quantity, Double basePrice, int stdDevRange, double stdDevMultiplier) {

    @Override
    public String toString() {
        return String.format("AlgoConfig{ %s %d@%.2f stdDevRange=%d stdDevMultiplier=%.2f }", code, quantity, basePrice, stdDevRange, stdDevMultiplier);
    }
}

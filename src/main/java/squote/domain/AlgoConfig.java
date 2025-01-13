package squote.domain;

public record AlgoConfig(String code, int quantity, double basePrice) {

    @Override
    public String toString() {
        return String.format("AlgoConfig{ %s %d@%.2f }", code, quantity, basePrice);
    }
}

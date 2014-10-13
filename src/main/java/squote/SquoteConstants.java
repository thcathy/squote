package squote;
public class SquoteConstants {
	public enum Side {
		BUY("買入", 1), SELL("沽出", -1);

		final public String chinese;
		final public int factor;

		Side(String chinese, int factor) {
			this.chinese = chinese;
			this.factor = factor;
		}
	}
}

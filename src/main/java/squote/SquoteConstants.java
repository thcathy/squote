package squote;
public class SquoteConstants {
	public enum Side {
		BUY("買入"), SELL("沽出");

		final public String chinese;

		Side(String chinese) {
			this.chinese = chinese;
		}
	}
}

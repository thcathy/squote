package squote;

public class SquoteConstants {
	public static String NA = "NA";

	public enum Side {
		BUY("買入", 1), SELL("賣出", -1);

		final public String chinese;
		final public int factor;

		Side(String chinese, int factor) {
			this.chinese = chinese;
			this.factor = factor;
		}
	}

	public enum IndexCode {
		HSI,
		HSCEI,
		HCCI,
		MSCIChina,
		MSCIHK;
	}	
}

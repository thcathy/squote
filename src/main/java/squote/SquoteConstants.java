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

	public enum IndexCode {
		HSI("Hang Seng Index"), HSCEI("HS China Enterprises Index");
		
		final public String name;
		
		IndexCode(String name) {
			this.name = name;
		}
	}
}

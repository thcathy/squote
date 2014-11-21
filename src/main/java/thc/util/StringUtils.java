package thc.util;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public final class StringUtils {
	//private static Logger logger = LoggerFactory.getLogger(NumberUtils.class);
	
	// private constructor prevents instantiation
	private StringUtils() { throw new UnsupportedOperationException(); }

	public static Optional<String> extractText(String text, String regex) {
		Pattern p2 = Pattern.compile(regex);
		Matcher m2 = p2.matcher(text);
		if (m2.find()) {
			return Optional.of(m2.group());
		} else {			
			return Optional.empty();
		}		
	}
	
	public static String extractNumber(String str) {
		return str.replaceAll("[^\\.\\-0123456789]", "");
	}
}

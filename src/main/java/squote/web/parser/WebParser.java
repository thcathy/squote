package squote.web.parser;
 
import java.util.Optional;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
 
public abstract class WebParser<T> implements Callable<Optional<T>> {
	private static Logger log = LoggerFactory.getLogger(WebParser.class);
	
	abstract public Optional<T> parse();
	
	public Optional<T> call() throws Exception {
		return parse();
	}
}
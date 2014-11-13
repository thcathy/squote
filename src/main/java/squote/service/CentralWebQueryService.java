package squote.service;
 
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import squote.web.parser.WebParser;

public class CentralWebQueryService extends ConcurrentExecuteService {
	protected final Logger log = LoggerFactory.getLogger(getClass());
		
	public CentralWebQueryService(int poolSize) {
		super(poolSize);		
	}

	public List<?> parses(List<WebParser<?>> parsers) {
		return executeCallables(parsers);
	}
	
	public <T> Optional<T> parse(WebParser<T> parser) {
		return parser.parse();
	}
			
	public <T> Future<Optional<T>> submit(WebParser<T> parser) {
		return (Future<Optional<T>>) executor.submit(parser);
	}
	
}
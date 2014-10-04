package thc.util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ConcurrentUtils {
	private static Logger logger = LoggerFactory.getLogger(ConcurrentUtils.class);
	
	// private constructor prevents instantiation
	private ConcurrentUtils() { throw new UnsupportedOperationException(); }

	public static <T> List<T> collects(List<Future<T>> futures) {
		List<T> results = new ArrayList<T>();
		for (Future<T> f : futures) {
			try {
				results.add(f.get());
			} catch (Exception e) {
				logger.warn("Exception during collect result from future.", e);
			}
		}
		return results;
	}
	
	public static <T> T collect(Future<T> future) {
		try {
			return future.get();
		} catch (Exception e) {
			throw new RuntimeException("Exception during collect result from future.", e);
		}
	}
}

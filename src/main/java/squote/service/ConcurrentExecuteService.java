package squote.service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConcurrentExecuteService {
	protected static Logger log = LoggerFactory.getLogger(ConcurrentExecuteService.class);
	protected final ExecutorService threadPool;	
	protected final int poolSize;
	
	public ConcurrentExecuteService(int poolSize) {
		super();
		log.debug("Fixed thread pool size: {}", poolSize);
		this.poolSize = poolSize;
		threadPool = Executors.newFixedThreadPool(poolSize);
	}
	
	/**
	 * Given a list of jobs, executes parallel by the fixed thread pool.
	 * Blocking called until all jobs completed
	 */
	public void executeRunnables(List<? extends Runnable> jobs) {
		log.debug("Execute {} runnables",jobs.size());
		CountDownLatch latch = new CountDownLatch(jobs.size());
		try {
			jobs.forEach( j->threadPool.submit(new BatchRunner(j, latch)) );
			latch.await();
		} catch (InterruptedException e) {
			log.warn("Interrupted when batch execute jobs", e);
		}
	}
	
	/**
	 * Given a list of jobs, executes parallel by the fixed thread pool.
	 * Blocking called until all jobs completed and return 
	 */
	public <T extends Object> List<T> executeCallables(List<? extends Callable<T>> jobs) {
		log.debug("Execute {} callables",jobs.size());
		List<Future<T>> futures = new ArrayList<Future<T>>();
		List<T> results = new ArrayList<T>();
		
		for (Callable<T> j : jobs) futures.add(threadPool.submit(j));
		for (Future<T> f : futures) {
			try {
				results.add(f.get());
			} catch (Exception e) {
				log.warn("Exception found during executing", e);
			}
		}
		return results;
	}
	
	public <T extends Object> Future<T> submit(Callable<T> job) {
		return threadPool.submit(job);
	}
		
	static class BatchRunner implements Runnable {
		private final CountDownLatch latch;
		private final Runnable job;
		
		public BatchRunner(Runnable job, CountDownLatch latch) { this.job = job; this.latch = latch;	}
		
		@Override
		public void run() {
			try {
				job.run();
			} catch (Exception e) {
				log.warn("Exception found during executing", e);
			} finally {
				latch.countDown();
			}			
		}		
	}
	
}

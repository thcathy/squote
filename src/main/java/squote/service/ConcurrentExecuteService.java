package squote.service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConcurrentExecuteService {
	protected static Logger log = LoggerFactory.getLogger(ConcurrentExecuteService.class);
	protected final ExecutorService executor;	
	protected final int poolSize;
	
	public ExecutorService getExecutor() { return executor; }
	
	public ConcurrentExecuteService(int poolSize) {
		super();
		log.debug("Fixed thread pool size: {}", poolSize);
		this.poolSize = poolSize;
		executor = Executors.newFixedThreadPool(poolSize);
		
		Runtime.getRuntime().addShutdownHook(new Thread() {
		    public void run() {
		        executor.shutdownNow();		        
		    }
		});
	}
	
	/**
	 * Given a list of jobs, executes parallel by the fixed thread pool.
	 * Blocking called until all jobs completed
	 */
	public void executeRunnables(List<? extends Runnable> jobs) {
		log.debug("Execute {} runnables",jobs.size());
		CountDownLatch latch = new CountDownLatch(jobs.size());
		try {
			jobs.forEach( j->executor.submit(new BatchRunner(j, latch)) );
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
		
		CompletionService<T> service = new ExecutorCompletionService<T>(executor);
		
		for (Callable<T> j : jobs) service.submit(j);
		
		int jobSize = jobs.size();
		for (int i=0; i < jobSize; i++) {
			try {
				results.add(service.take().get());
			} catch (Exception e) {
				log.warn("Exception found during executing", e);
			}
		}
		
		return results;
	}
	
	public <T extends Object> Future<T> submit(Callable<T> job) {
		return executor.submit(job);
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

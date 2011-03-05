package dfh.profiler;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * This wee bit of code is the entire profiler. It keeps track of time elapsed
 * between method calls and prints out the statistics when you ask it to,
 * flushing its data structures in the process.
 * <p>
 * <b>Creation date:</b> Mar 5, 2011
 * 
 * @author David Houghton
 * 
 */
public class Timer {
	/**
	 * Struct to hold timing statistics.
	 * <p>
	 * <b>Creation date:</b> Mar 5, 2011
	 * 
	 * @author David Houghton
	 * 
	 */
	private static class Stats {
		long total = 0;
		int count = 0;
	}

	/**
	 * Where output goes. <code>System.err</code> by default.
	 */
	public static PrintStream out = System.err;
	/**
	 * If set to true, <code>Timer</code> will use {@link TreeMap} instead of
	 * {@link HashMap} in order to save a little memory.
	 * <p>
	 * After a single <code>Timer</code> has been marked as {@link #done()},
	 * this will have no effect.
	 */
	public static boolean conserveMemory = false;
	protected final String key;
	protected final long start;
	protected static Map<String, Stats> cache;
	private static Timer singleton;

	/**
	 * Initialize data structures to retain statistics.
	 */
	private static void initMaps() {
		if (conserveMemory)
			cache = new TreeMap<String, Timer.Stats>();
		else
			cache = new HashMap<String, Timer.Stats>();
	}

	/**
	 * Start timer.
	 * 
	 * @param key
	 *            name to associate with timer; must not be <code>null</code>
	 */
	public Timer(String key) {
		if (key == null)
			throw new RuntimeException("null key");
		this.key = key;
		start = System.currentTimeMillis();
	}

	/**
	 * Used for creation of singleton timer on which to invoke
	 * {@link #output(String, int, long)}.
	 */
	protected Timer() {
		key = null;
		start = 0;
	}

	/**
	 * Record time and invocation count.
	 */
	public final void done() {
		long done = System.currentTimeMillis();
		synchronized (Timer.class) {
			if (singleton == null) {
				singleton = this.instance();
				initMaps();
			}
			Stats total = cache.get(key);
			if (total == null) {
				total = new Stats();
				cache.put(key, total);
			}
			total.count++;
			total.total += done - start;
		}
	}

	@Override
	protected void finalize() {
		done();
	}

	public static synchronized void show() {
		List<String> keys = new ArrayList<String>(cache.keySet());

		if (singleton == null) {
			singleton = new Timer();
			initMaps();
		}
		Collections.sort(keys, singleton.comparator());
		out.println();
		for (String key : keys) {
			Stats s = cache.get(key);
			singleton.output(key, s.count, s.total);
		}
		cache.clear();
	}

	/**
	 * Returns {@link Comparator} for sorting output keys. Provided to allow
	 * overriding. If overridden, one must also override {@link #instance()}.
	 * 
	 * @return {@link Comparator} for sorting output keys
	 */
	public Comparator<? super String> comparator() {
		return new Comparator<String>() {
			@Override
			public int compare(String o1, String o2) {
				int comparison = (int) (cache.get(o2).total - cache.get(o1).total);
				if (comparison == 0)
					comparison = o1.compareTo(o2);
				return comparison;
			}
		};
	}

	/**
	 * Returns <code>Timer</code> on which to invoke
	 * {@link #output(String, int, long)} and {@link #comparator()} when
	 * {@link #show()} is invoked.
	 * 
	 * @return <code>Timer</code> on which to invoke
	 *         {@link #output(String, int, long)} when {@link #show()} is
	 *         invoked.
	 */
	public Timer instance() {
		Timer singleton = new Timer();
		return singleton;
	}

	/**
	 * Method invoked during output generation. Made an instance method to allow
	 * overriding. If you override this you must also override
	 * {@link #instance()} or {@link Timer} won't see it.
	 * 
	 * @param key
	 *            name given in {@link #Timer(String)}
	 * @param count
	 *            number of times constructor invoked
	 * @param totalTime
	 *            processing time in milliseconds
	 */
	public void output(String key, int count, long totalTime) {
		float tt = totalTime / 1000F;
		float mt = tt / count;
		out.printf("%s -- n: %d; avg: %.4f sec; total: %.2f sec%n", key, count,
				mt, tt);
	}
}

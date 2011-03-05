package dfh.profiler;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
	 * Where output goes. <code>System.err</code> by default.
	 */
	public static PrintStream out = System.err;
	private final String key;
	private final long start;
	private static final Map<String, long[]> totals = new HashMap<String, long[]>();
	private static final Map<String, int[]> counts = new HashMap<String, int[]>();

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
	private Timer() {
		key = null;
		start = 0;
	}

	/**
	 * Record time and invocation count.
	 */
	public void done() {
		long done = System.currentTimeMillis();
		synchronized (Timer.class) {
			long[] total = totals.get(key);
			if (total == null) {
				total = new long[] { done - start };
				totals.put(key, total);
			} else
				total[0] += done - start;
			int[] count = counts.get(key);
			if (count == null) {
				count = new int[] { 1 };
				counts.put(key, count);
			} else
				count[0]++;
		}
	}

	@Override
	protected void finalize() {
		done();
	}

	public static synchronized void show() {
		List<String> keys = new ArrayList<String>(totals.keySet());

		Collections.sort(keys, new Comparator<String>() {
			@Override
			public int compare(String o1, String o2) {
				int comparison = (int) (totals.get(o2)[0] - totals.get(o1)[0]);
				if (comparison == 0)
					comparison = o1.compareTo(o2);
				return comparison;
			}
		});
		Timer singleton = singleton();
		out.println();
		for (String key : keys) {
			long total = totals.get(key)[0];
			int count = counts.get(key)[0];
			singleton.output(key, count, total);
		}
		counts.clear();
		totals.clear();
	}

	/**
	 * @return <code>Timer</code> on which to invoke
	 *         {@link #output(String, int, long)} when {@link #show()} is
	 *         invoked.
	 */
	public static Timer singleton() {
		Timer singleton = new Timer();
		return singleton;
	}

	/**
	 * Method invoked during output generation. Made an instance method to allow
	 * overriding. If you override this you must also override
	 * {@link #singleton()} or {@link Timer} won't see it.
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

package dh.tool.thread.prifo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.concurrent.CancellationException;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A {@link dh.tool.thread.prifo.PrifoTask} which can only be executed one.
 * Created by hiep on 12/06/2014.
 */
public abstract class OncePrifoTask extends PrifoTask {
	private static final SimpleDateFormat SDF = new SimpleDateFormat("H:m:s,S");
	private static final Logger log = LoggerFactory.getLogger(OncePrifoTask.class);
	private volatile boolean used = false;
	private volatile boolean running = true;
	private volatile boolean finished = false;
	private volatile Calendar startTime = null;
	private volatile Calendar endTime = null;

	/**
	 * Other blocking method should use this lock
	 */
	protected final ReentrantLock lock = new ReentrantLock();

	@Override
	public void run() {
		try {
			checkCancellation();
			final ReentrantLock lock = this.lock;
			lock.lock();
			running = true;
			try {
				if (used) {
					throw new IllegalStateException(String.format("OncePrifoTask run twice (Started at %s, End: %s%s) - %s",
							SDF.format(getStartTime().getTime()),
							getEndTime() == null ? "null" : SDF.format(getEndTime().getTime()),
							isCancelled() ? " cancelled" : "",
							this));
				}
				startTime = Calendar.getInstance();
				used = true;
				perform();
			} finally {
				endTime = Calendar.getInstance();
				finished = true;
				running = false;
				lock.unlock();
			}
		}
		catch (CancellationException ex) {
			log.debug("Cancel " + ex.getMessage() + " " + toString());
		}
		catch (Exception ex) {
			log.error("Un-catch error", ex);
		}
	}

	public boolean isUsed() {
		return used;
	}

	public boolean isRunning() {
		return running;
	}

	public boolean isFinished() {
		return finished;
	}

	public Calendar getStartTime() {
		return startTime;
	}

	public Calendar getEndTime() {
		return endTime;
	}

	@Override
	public void onEnterQueue(PrifoQueue queue) {
		log.trace("EnterQueue " + queue.getName() + " size=" + queue.size() + " - " + toString());
	}

	@Override
	public void onDequeue(PrifoQueue queue) {
		log.trace("DeQueue "+queue.getName()+" size="+queue.size()+ " - "+toString());
	}

	abstract public void perform();
}

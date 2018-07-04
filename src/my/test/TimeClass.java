package my.test;
/**
 * 
 */


import java.io.Closeable;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.log4j.Logger;

/**
 * @author vaibhav
 *
 */
public final class TimeClass implements Closeable
{
	private static final Logger logger = Logger.getLogger(TimeClass.class);

	private String tagName;

	private AtomicLong totalRequests = new AtomicLong();

	private AtomicLong totalIntervalRequests = new AtomicLong();

	private AtomicLong totalTimeTaken = new AtomicLong();

	private AtomicLong intervalTimeTaken = new AtomicLong();

	private static Timer timer = new Timer(true);

	private PeriodicThread periodicThread = null;

	/**
	 * 
	 * @param tagName
	 * @param period
	 */
	public TimeClass(String tagName, long period)
	{
		this.tagName = tagName;
		periodicThread = new PeriodicThread();
		timer.scheduleAtFixedRate(periodicThread, new Date(), period);
	}

	public long start()
	{
		return System.currentTimeMillis();
	}

	public void end(long startTime)
	{
		long endTime = System.currentTimeMillis();
		totalRequests.addAndGet(1);
		totalTimeTaken.addAndGet(endTime - startTime);
		totalIntervalRequests.addAndGet(1);
		intervalTimeTaken.addAndGet(endTime - startTime);
	}

	public void end(long startTime, int count)
	{
		long endTime = System.currentTimeMillis();
		totalRequests.addAndGet(count);
		totalTimeTaken.addAndGet(endTime - startTime);
		totalIntervalRequests.addAndGet(count);
		intervalTimeTaken.addAndGet(endTime - startTime);
	}

	public void flush()
	{
		long totalReqInInterval = totalIntervalRequests.get();
		long timeTaken = intervalTimeTaken.get();
		totalIntervalRequests.addAndGet(-1 * totalReqInInterval);
		intervalTimeTaken.addAndGet(-1 * timeTaken);

		long internalAvg = -1;
		if (totalReqInInterval > 0)
		{
			internalAvg = timeTaken / totalReqInInterval;
		}
		long totalAvg = -1;
		if (totalRequests.get() > 0)
		{
			totalAvg = totalTimeTaken.get() / totalRequests.get();
		}
		if ((totalAvg >= 0) || (internalAvg >= 0))
		{
			StringBuilder statsLog = new StringBuilder();
			statsLog.append(tagName).append(":TotalReq: ").append(totalRequests).append(" :TotalTimeTaken: ").append(totalTimeTaken)
			.append(" :totalAvg: ").append(totalAvg).append(" :totalReqInInterval: ")
			.append(totalReqInInterval).append(" :timeTaken: ").append(timeTaken).append(" :IntervalAvg: ").append(internalAvg);
			logger.info(statsLog.toString());
		}
	}

	
	public void resetPeriod(long newPeriod)
	{
		try
		{
			close();
			periodicThread = new PeriodicThread();
			timer.scheduleAtFixedRate(periodicThread, new Date(), newPeriod);
		}
		catch (IOException e)
		{
			logger.error("Unable to reset Period", e);
		}
	}
	
	@Override
	public void close() throws IOException
	{
		timer.cancel();
		timer = null;
		stop();
	}

	public void stop()
	{
		if (this.periodicThread != null)
		{
			this.periodicThread.cancel();
			flush();
			periodicThread = null;
		}
	}

	public void setTagName(String tagName)
	{
		this.tagName = tagName;
	}

	public String getTagName()
	{
		return this.tagName;
	}
	
	private class PeriodicThread extends TimerTask
	{
		@Override
		public void run()
		{
			flush();
		}

	}
}

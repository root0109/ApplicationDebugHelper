package my.test;
/**
 * 
 */


import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.log4j.Logger;
/**
 * @author vaibhav
 *
 */
public final class DBQueryStatsManager
{
	private static final ConcurrentMap<String, DBQueryStats> queryStatsMap = new ConcurrentHashMap<String, DBQueryStats>();

	/*
	 *  set noOfTrackQueries = -1 , default value of 4 will be used
	 *  set queryThreshold = -1 , default value of 2000ms will be used
	 */
	public static void logQueryExecutionTime(String query, long queryStartTime, int queryThreshold, int noOfTrackQueries)
	{
		int time = (int) (System.currentTimeMillis() - queryStartTime);
		DBQueryStats queryStats = queryStatsMap.get(query);
		if (queryStats == null)
		{
			queryStats = new DBQueryStats(query, noOfTrackQueries, queryThreshold);
			DBQueryStats temp = queryStatsMap.putIfAbsent(query, queryStats);
			if (temp != null)
				queryStats = temp;
		}

		queryStats.addExecutionTime(time);
		queryStats.log(time);
	}
	
	private static class DBQueryStats
	{
		private static final Logger logger = Logger.getLogger(LatencyLogger.class);
		private static final int NUM_QUERIES_TO_TRACK = 4;
		private static final int DEFAULT_QUERY_EXECUTION_THRESHOLD_IN_MILLIS = 2000;
		
		private final String query;

		private AtomicLong totalQueries;

		private AtomicLong totalTime;

		private AtomicInteger lastQueryTimeIndex;
		
		private int queryThreshold;

		private int[] lastQueryTimes = null;

		public DBQueryStats(String query, int noOfTrackQueries, int queryThreshold)
		{
			this.query = query;
			if(noOfTrackQueries == -1)
				noOfTrackQueries = NUM_QUERIES_TO_TRACK;
			if(queryThreshold == -1)
				this.queryThreshold = DEFAULT_QUERY_EXECUTION_THRESHOLD_IN_MILLIS;
			this.lastQueryTimes = new int[noOfTrackQueries];
			this.queryThreshold = queryThreshold;
		}
		
		private void addExecutionTime(int time)
		{
			totalQueries.addAndGet(1);
			totalTime.addAndGet(time);

			if (lastQueryTimeIndex.get() == NUM_QUERIES_TO_TRACK)
				lastQueryTimeIndex.set(0);
			int index = (lastQueryTimeIndex.addAndGet(1)) % NUM_QUERIES_TO_TRACK;
			lastQueryTimes[index] = time;
		}

		private void log(int timeSpent)
		{
			if (timeSpent >= queryThreshold)
			{
				StringBuilder statsLog = new StringBuilder();
				statsLog.append("QUERY_STAT:query: ").append(query).append(" ,totalQueries: ").append(totalQueries).append(" ,totalTime: ")
				.append(totalTime).append(" ,avgTime: ").append(((totalQueries.get() != 0) ? (totalTime.get() / totalQueries.get()) : 0))
				.append(" ,lastQueryTimes:").append(Arrays.toString(lastQueryTimes));
				logger.info(statsLog.toString());
			}
		}
	}
}

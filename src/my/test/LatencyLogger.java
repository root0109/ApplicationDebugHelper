package my.test;
/**
 * 
 */


import java.util.LinkedHashMap;
import java.util.Map.Entry;

import org.apache.log4j.Logger;

/**
 * @author vaibhav
 * 
 * This can be used to log latency of various moving parts while serving a single request.
 *  you have to create checkpoints for one single request and measure the execution time/ latency of  that processing within that checkpoint
 *
 */
public final class LatencyLogger
{
	public interface CheckpointConstants
	{
		//Define checkpoint whose latency has to be measured here 
		public static final String API_START = "api_start";
		public static final String API_END = "api_end";
	}
	
	private static final Logger logger = Logger.getLogger(LatencyLogger.class);
	
	private long startTime;
	private long lastCheckPointTime;
	private final LinkedHashMap<CheckpointConstants, DeltaInfo> checkPoints;


	public LatencyLogger()
	{
		startTime = System.currentTimeMillis();
		lastCheckPointTime = startTime;
		checkPoints = new LinkedHashMap<CheckpointConstants, DeltaInfo>();
	}
	
	public LatencyLogger(LatencyLogger latencyLogger)
	{
		startTime = latencyLogger.startTime;
		lastCheckPointTime = latencyLogger.lastCheckPointTime;
		checkPoints = new LinkedHashMap<CheckpointConstants, DeltaInfo>();
		for (Entry<CheckpointConstants, DeltaInfo> cpInfo : latencyLogger.checkPoints.entrySet())
		{
			checkPoints.put(cpInfo.getKey(), new DeltaInfo(cpInfo.getValue().delta, cpInfo.getValue().count));
		}
	}
	
	public void addCheckPoint(CheckpointConstants checkPointName)
	{
		long currentTime = System.currentTimeMillis();
		int delta = (int) (currentTime - lastCheckPointTime);
		lastCheckPointTime = currentTime;

		DeltaInfo currentDelta = checkPoints.get(checkPointName);
		if (currentDelta != null)
		{
			currentDelta.delta += delta;
			++currentDelta.count;
		}
		else
		{
			currentDelta = new DeltaInfo(delta, 1);
			checkPoints.put(checkPointName, currentDelta);
		}
	}
	
	public void logCheckPointInfo(StringBuilder sb)
	{
		sb.append("st=").append(startTime).append(",");
		sb.append("tt=").append(System.currentTimeMillis() - startTime).append(",");
		for (Entry<CheckpointConstants, DeltaInfo> cpInfo : checkPoints.entrySet())
			sb.append(cpInfo.getKey()).append("=").append(cpInfo.getValue().delta).append(":").append(cpInfo.getValue().count).append(",");

		logger.info(sb.toString());
	}
	
	private class DeltaInfo
	{
		/**
		 * aggregate time taken since last checkpoint
		 */
		int delta;
		/**
		 * num times delta was aggregated
		 */
		int count;

		DeltaInfo(int delta, int count)
		{
			this.delta = delta;
			this.count = count;
		}
	}
}

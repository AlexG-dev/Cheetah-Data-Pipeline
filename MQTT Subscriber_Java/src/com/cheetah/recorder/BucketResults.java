package com.cheetah.recorder;

/**
 * A simple structure returned whenever the <i>LatencyBucket</i> is emptied. 
 * Contains the average of all entries, as well as the number of entries at the time of 
 * emptying.
 * @author Alex Gaudreault
 * @version 1.0.0, 2019-12-05
 * @since 1.0.0
 *
 */
public class BucketResults {

	/** Contains the calculated average of the bucket contents, when it was emptied. */
	private Double average;
	/** Contains the number of entries contained in the bucket, when it was emptied. */
	private Double numEntries;
	
	/**
	 * Initialization constructor for the <i>BucketResults</i> class. Takes the average and 
	 * number of entries contained in the <i>LatencyBucket</i>.
	 * @param average The average between all entries in the <i>LatencyBucket</i>.
	 * @param numEntries The number of entries contained in the <i>LatencyBucket</i>
	 * 
	 * @version 1.0.0, 2019-12-05
	 * @since 1.0.0
	 */
	public BucketResults(Double average, Double numEntries){
		this.average = average;
		this.numEntries = numEntries;
	}
	
	/**
	 * Returns the average between all bucket entries.
	 * @return The average between all bucket entries.
	 */
	public Double getAverage(){
		return this.average;
	}
	
	/**
	 * Returns the number of entries in the bucket.
	 * @return The number of entries added to the bucket.
	 */
	public Double getNumEntries(){
		return this.numEntries;
	}
}

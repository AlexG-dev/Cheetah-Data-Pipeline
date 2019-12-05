package com.cheetah.recorder;
import java.io.File;
import java.util.TimerTask;

/**
 * <b>LatencyBucket</b> is an extension of the <i>TimerTask</i> class, that way it 
 * may be used as a scheduled <i>Timer</i> task.
 * @author Alex Gaudreault
 *
 */
public class LatencyBucket extends TimerTask {

	private final boolean VERBOSE = true;
	
	/** The current bucket contents (the sum of all entries). */
	private Double bucketContents = 0.0;
	/** The number of entries currently in the bucket. */
	private Double bucketEntries = 0.0;
	
	/** An instance of our <i>CsvWriter</i> class, to allow logging to a CSV file. */
	private CsvWriter csvWriter;
	
	/**
	 * The initialization constructor for the <b>LatencyBucket</b> class. Initializes our 
	 * <i>CsvWriter</i> object using the output file defined by the calling method.
	 * @param outFile The output file to be used when initializing the <i>CsvWriter</i> instance.
	 * 
	 * @version 1.0.0, 2019-12-05
	 * @since 1.0.0
	 */
	public LatencyBucket(File outFile){
		csvWriter = new CsvWriter(outFile);
	}
	
	/**
	 * Overriden <i>run()</i> method from the <i>TimerTask</i> class. Called whenever a scheduled <i>Timer</i> 
	 * has reached it's next action-time.
	 * 
	 * @version 1.0.1, 2019-12-05
	 * @since 1.0.0
	 */
	@Override
	public void run() {
		
		if(this.bucketEntries == 0){
			if(VERBOSE)
			System.out.println("[INFO] No Entries to Log - Skip Writing to CSV.");
			return;
		}
		else{
			if(VERBOSE){
			System.out.println("[INFO] Logging Latency to CSV: ");
			System.out.println("\t Bucket Contents = " + this.bucketContents);
			System.out.println("\t Bucket Entries = " + this.bucketEntries);
			}
		}
		
		csvWriter.appendBucketResults(emptyBucket());
		
	}

	/**
	 * Use this method to add a new entry to the bucket. Adds the passed value to 
	 * <i>bucketContents</i> and increments <i>bucketEntries</i>.
	 * @param value The value to be added to the bucket contents.
	 */
	public void addToBucket(double value){
		bucketEntries++;
		bucketContents += value;
	}
	
	/**
	 * Empties the bucket by resetting <i>bucketContents</i> and <i>bucketEntries</i> to zero. 
	 * Returns the emptied contents as a <i>BucketResults</i> Object.
	 * @return The <i>BucketResults</i> object describing the state of the bucket before emptying.
	 */
	private BucketResults emptyBucket(){
		
		Double average = (bucketContents / bucketEntries);
		
		BucketResults results = new BucketResults(average, this.bucketEntries);
		
		this.bucketContents = 0.0;
		this.bucketEntries = 0.0;
		
		return results;
	}
	
}

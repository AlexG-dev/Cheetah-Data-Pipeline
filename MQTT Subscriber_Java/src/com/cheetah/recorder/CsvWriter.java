package com.cheetah.recorder;
import java.io.*;
import java.sql.Date;
import java.text.SimpleDateFormat;

/**
 * This class manages the reading and writing of a CSV file.
 * @author Alex Gaudreault
 * 
 * @version 1.0.3, 2019-12-05
 * @since 1.0.0
 *
 */
public class CsvWriter {

	/** Output file currently being written to by this object. */
	private File outFile;
	/** The <i>FileWriter</i> employed by this object. */
	private FileWriter out;
	
	/**
	 * The initialization constructor for this Class. Intializes <i>outFile</i> according to 
	 * the passed File, before creating/overwriting the file using <i>out</i>.
	 * 
	 * @param path The CSV file to create and write to.
	 */
	public CsvWriter(File path){
		outFile = path;
		
		if(!outFile.exists() || !outFile.isFile()){
			System.out.println("[INFO] Creating file @ '" + outFile.getAbsolutePath() + "'");
		}
		else{
			outFile.delete();
		}
		
		try {
			out = new FileWriter(path, false);
			out.write("TIME_UTC, LATENCY_AVG, NUM_MESSAGES\n");
			out.flush();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Appends the passed <i>BucketResults</i> average and number of entries, to the CSV 
	 * file. First column is the current time, second column in the bucket average and the 
	 * third column is the number of entries in the bucket.
	 * 
	 * @param br The results retrieved when emptying a <i>LatencyBucket</i>
	 */
	public void appendBucketResults(BucketResults br){
		
		try{
		out = new FileWriter(outFile, true);
		
		Double average = br.getAverage();
		Double numMsg = br.getNumEntries();
		
		System.out.println("\tAverage Lat. = " + average + " ms");
		
		if(!outFile.exists() || !outFile.isFile()){
			System.out.println("[INFO] Creating file @ '" + outFile.getAbsolutePath() + "'");
			outFile.mkdirs();
		}
		
		SimpleDateFormat formatter= new SimpleDateFormat("HH:mm:ss");
		Date date = new Date(System.currentTimeMillis());
		
		out.append(formatter.format(date) + ", " + average + ", " + numMsg + "\n");
		
		out.flush();
		out.close();
		}
		catch(IOException e){
			e.printStackTrace();
		}
	}
}

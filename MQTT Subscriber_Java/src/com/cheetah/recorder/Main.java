package com.cheetah.recorder;

import java.io.File;
import java.util.Scanner;

/**
 * The <b>main</b> class contains our main() method. This class also contains 
 * the default configuration parameters (Broker Address, Client ID, Subscr. Topic, etc).
 * @author Alex Gaudreault
 * @version 1.0.0, 2019-12-04
 * @since 1.0.0
 *
 */
public class Main {

	/** Default Broker Address used when no command-line arguments are defined. */
	static final String BROKER_ADDRESS = "tcp://35.209.240.115:1883";
	/** Default Client Identifier used when no command-line arguments are defined. */
	static final String CLIENT_ID = "latency-report-client";
	/** Default Subscription Topic used when no command-line arguments are defined. */
	static String SUB_TOPIC = "/devices/+/latency/report";
	/** Default MQTT QoS used when no command-line arguments are defined. */
	static int QOS = 0;
	/** Default output file (.csv) used when no command-line arguments are defined. */
	static File OUT_FILE = new File("latency_aggregation.csv");
	
	/** Scanner used to retrieve user input. */
	static Scanner input = new Scanner(System.in);
	
	/**
	 * Our main() method is the entry point for this project. It reads the command-line 
	 * arguments defined by the user, and assigns them to the <i>MQTTLatencyClient</i> instance.
	 * @param args Command-line arguments passed by the user, [SUB_TOPIC] [QOS] [OUT_FILE].
	 * @version 1.0.2, 2019-12-04
	 * @since 1.0.0
	 */
	public static void main(String[] args){
		
		// If arguments 3 command-line arguments are present, use them to initialize our MQTTLatencyClient
		if(args.length == 3){
			// Read Subscription Topic from args[0]
			SUB_TOPIC = args[0];
			
			// Read QoS from args[1] & validate
			try{
				if(Integer.parseInt(args[1]) > 2 || Integer.parseInt(args[1]) < 0){
					System.out.println("Invalid QoS specified [" + args[1] + "]... Aborting!");
					System.exit(1);
				}
			}
			catch(NumberFormatException e){
				System.out.println("Specified QoS is not an integer [" + args[1] + "]... Aborting!");
				System.exit(1);
			}
			
			// Read output file path from args[2]
			OUT_FILE = new File(args[2]);
			
			// Check if file exists, as well as ensure the file isn't a directory
			if(OUT_FILE.exists() && OUT_FILE.isFile()){
				System.out.println("[INFO] Specified file '" + args[2] + "' already exists... Do you wish to overwrite it? (Y/N):");
				char opt = '#';
				
				// Ask if the user wants to overwrite the existing file
				while((opt = input.nextLine().toLowerCase().charAt(0)) != 'y' && opt != 'n');
				
				if(opt == 'n'){
					System.out.println("Aborting...");
					System.exit(0);
				}
			}
			else if(OUT_FILE.isDirectory()){
				System.out.println("Specified file is a directory... Aborting!");
				System.exit(1);
			}
			
		}
		
		// Information + DEBUG
		System.out.println("\n************************************");
		System.out.println("Topic = " + SUB_TOPIC);
		System.out.println("QoS = " + QOS);
		System.out.println("Output File = " + OUT_FILE);
		System.out.println("************************************");
		
		// Initialize our MQTTLatencyClient Object
		@SuppressWarnings("unused")
		MQTTLatencyClient client = new MQTTLatencyClient(
				BROKER_ADDRESS,
				CLIENT_ID,
				SUB_TOPIC,
				QOS,
				OUT_FILE
		);
		
	}
	
}

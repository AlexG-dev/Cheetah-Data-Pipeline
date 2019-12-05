package com.cheetah.recorder;
import java.io.File;
import java.util.Timer;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

/**
 * <b>MQTTLatencyClient</b> is a wrapper class for Paho's <i>MqttAsyncClient</i> Class. 
 * Initializes and defines the callbacks for an <i>MqttAsyncClient</i> Object, and stores the 
 * subscribe topic, broker address and client ID.
 * @author Alex Gaudreault
 * @version 1.0.0, 2019-12-04
 * @since 1.0.0
 *
 */
public class MQTTLatencyClient{

	/** Defines the degree of information printed to the console. Change to true to use Verbose behaviour. */
	private final boolean VERBOSE = false;
	
	/** The wrapped instance of <i>MqttAsyncClient</i>. Initialized in the Class constructor. */
	protected MqttAsyncClient client;
	/** Memory persistence structure for <b>client</b> */
	private MemoryPersistence persistence;
	
	/** The MQTT topic which the Client is currently subscribed to. */
	private String subscribeTopic;
	/** The address of the broker which <b>client</b> is currently connected to. */
	private String brokerAddress;
	/** The identifier currently being used by <b>client</b>. */
	private String clientID;
	
	/** The MQTT QoS level being used by <b>client</b> when publishing and subscribing. */
	private int qos;
	
	/** An instance of our inner-class "<i>MessageProcessor</i>". */
	private MessageProcessor msgProcessor;
	
	/** Timer used to schedule the bucket emptying interval; Scheduled in the Class constructor. */
	private Timer emptyBucket;
	
	/** Instance of our <i>LatencyBucket</i> class, used to store and report latency information. */
	private LatencyBucket bucket;
	
	/**
	 * <b><u>Unimplemented/Unused</u></b>
	 */
	public MQTTLatencyClient(){
		//TODO: Define default constructor (if necessary).
	}
	
	/**
	 * The initalization constructor for the <i>MQTTLatencyClient</i> class. Takes the required 
	 * parameters from the calling method, to initalize an instance of <b>client</b> (<i>MqttAsyncClient</i> Object). 
	 * Connected this client to the broker and subscribes to [topic] with a QoS level of [qos], before scheduling our 
	 * <b>emptyBucket</b> <i>Timer</i> object.
	 * 
	 * @param broker The address of the MQTT broker to subscribe to.
	 * @param id The Identifier to be used by this client.
	 * @param topic The topic which will be subscribed to with QoS [qos].
	 * @param qos The QoS level which will be used when subscribing/publishing to the broker.
	 * @param outFile The output file where the latency results will be logged (.csv).
	 * 
	 * @version 1.0.1, 2019-12-04
	 * @since 1.0.0
	 */
	public MQTTLatencyClient(String broker, String id, String topic, int qos, File outFile){
		this.brokerAddress = broker;
		this.clientID = id;
		this.subscribeTopic = topic;
		this.qos = qos;
		
		this.msgProcessor = new MessageProcessor();
		this.bucket = new LatencyBucket(outFile);
		
		do{
			
			try {
				if(qos == 1 || qos == 2){
					this.persistence = new MemoryPersistence();
					this.client = new MqttAsyncClient(this.brokerAddress, this.clientID, this.persistence);
				}
				else{
					this.client = new MqttAsyncClient(broker, id);
				}

				System.out.print("Connecting... ");
				this.client.connect().waitForCompletion();
				System.out.println("Connected! ");
				this.client.subscribe(this.subscribeTopic, this.qos, this.msgProcessor);
				
			} catch (MqttException e) {
				System.out.println("Failed to Connect! Retrying...");
			}
			
		}while(!this.client.isConnected());
		
		emptyBucket = new Timer();
		emptyBucket.schedule(bucket, 0, 1000);
	}
	
	/**
	 * <b>MessageProcessor</b> class implements the <i>IMqttMessageListener</i> interface from the 
	 * Paho library. An object of this class may be bound to an <i>MqttAsyncClient</i>, which will 
	 * enable this class as the message processor for that client.
	 * @author Alex Gaudreault
	 * @version 1.0.0, 2019-12-04
	 * @since 1.0.0
	 *
	 */
	public class MessageProcessor implements IMqttMessageListener{

		/**
		 * Overrides the abstract <i>messageArrived()</i> method defined in the <i>IMqttMessageListener</i> 
		 * interface. This overriden method is called whenever the binding <i>MqttAsyncClient</i> object receives a 
		 * message on a topic it's subscribed to.
		 * @version 1.0.0, 2019-12-04
		 * @since 1.0.0
		 */
		@Override
		public void messageArrived(String topic, MqttMessage message) throws Exception {
			
			// JSON parser and JSON object
			JSONParser parser = new JSONParser();
			JSONObject msgJson = new JSONObject();
			
			// Parse JSON String into [msgJson] Object
			msgJson = (JSONObject) parser.parse(message.toString());
			
			if(VERBOSE){
				System.out.println("Received Message:");
				System.out.println("\tTopic = '" + topic + "'");
				System.out.println("\t** BEGIN PAYLOAD **");
				System.out.println("\t" + message);
				System.out.println("\t** END PAYLOAD **");
			}
			
			// *** Retrieve last_latency ***
			Long lastLatency = (Long) msgJson.get("last_latency");
			String deviceId = (String) msgJson.get("device_id");
			
			if(lastLatency == null){
				System.out.println("[ERROR] Last Latency is NULL - Ignoring message.");
				return;
			}
			
			// Check for presence of last latency
			// 	-> Exists: Increment messages received and add to bucket
			//  -> Doesn't: Ignore the message (treat as first message from device)
			if(lastLatency >= 0){
				bucket.addToBucket(lastLatency);
			}
			else{
				System.out.println("[INFO] Detected first message from ID = '" + msgJson.get("device_id") + "'.");
			}
			
			//System.out.print("Replying on '/devices/" + deviceId + "/latency/reply'... ");
			message.setQos(qos);
			client.publish("/devices/" + deviceId + "/latency/reply", message);
			//System.out.println("Success!");
		}

	}
}

package com.cheetahnetworks;

import com.cheetahnetworks.exceptions.InvalidMqttMessageException;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

/**
 * This class is a wrapper for Paho's MqttAsyncClient class. It creates, configures, and manages an instance of this class.
 *
 * @author Michael Boulerice
 */
public class Client implements Runnable { //TODO - Extend java.util.Thread
    /* MqttAsyncClient is the default, non-blocking client. Each instance of MqttAsyncClient spawns a separate thread for event handling and message processing
     * MqttClient on the other hand is simply a wrapper around MqttAsyncClient that is single threaded and blocking.
     * However, it is possible to use blocking calls in MqttAsyncClient by adding .waitForCompletion() to the end of any call.
     */
    private MqttAsyncClient client;
    private MqttCallback callback; // An interface for implementing functions that get called when certain events happen in the client.
    private MemoryPersistence persistence; // Memory buffer used to store unprocessed and in-flight messages.
    private MqttConnectOptions connectOptions; // Options for connecting to the broker.

    private String broker; // Broker URI
    private String clientID; // ID used to connect to broker - must be unique to the broker or will disconnect the last device to use this ID

    /*
     * Quality-of-Service level:
     *  0 - Send once, no guarantees. Comparable to UDP
     *  1 - Send AT LEAST once, guarantees message gets delivered, but may send multiple copies
     *  2 - Send AT MOST once, comparable to TCP
     */
    private int qos;

    // Name of physical device emulating virtual devices. Allows the use of multiple testbeds adding another layer to the topic structure.
    private String testbedID;

    // The topic the client will receive commands on, e.g. STOP
    private String commandTopic;
    // The topic the client will publish messages to
    private String latencyReportTopic;
    // The topic on which the client will receive responses from the CSV writer client
    private String latencyReplyTopic;

    // Added to every time the client sends a message
    private int numSentMessages;
    // The amount of milliseconds the client will wait after receiving a message before sending the next one, essentially throttles performance.
    // TODO - Move to a schedule based system so the message rate can be set to a fixed, accurate number
    private int waitPeriod;

    /**
     * Creates and configures a Paho client instance
     *
     * @param broker     The URI of the MQTT Broker to connect to
     * @param clientID   The unique ID the client will use to connect to the Broker
     * @param waitPeriod The number of milliseconds the client should wait after receiving a message before sending the next - serves to throttle message rate
     * @throws MqttException will be thrown in the case of failure during the creation of the internal Paho client instance
     */
    Client(String broker, String clientID, int waitPeriod) throws MqttException {
        this.broker = broker;
        this.clientID = clientID;
        System.out.println(this.clientID);
        persistence = new MemoryPersistence();
        connectOptions = new MqttConnectOptions();

        commandTopic = this.clientID + "/command";
        latencyReportTopic = this.clientID + "/latency/report";
        latencyReplyTopic = this.clientID + "/latency/report";

        qos = 0;

        numSentMessages = 0;
        this.waitPeriod = waitPeriod;

        // Anonymous class to implement the IMqttCallback interface
        callback = new MqttCallback() {
            // Called every time the client loses connection to the broker.
            @Override
            public void connectionLost(Throwable cause) {
                System.out.println(clientID + " Connect lost: " + cause.getCause());
            }

            // Called every time the client receives a message.
            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {

                // System.out.println("Received message: " + message.toString());

                // Sends next message only after receiving the last
                if (topic.equalsIgnoreCase(latencyReplyTopic))
                    sendNextMessage(message);

                // Currently stops the client if any message is received on the command topic.
                if (topic.equalsIgnoreCase(commandTopic))
                    close();
            }

            // Called every time the client sends a message, at QoS > 0 will be called on acknowledgement rather than on send.
            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                // System.out.println("Message delivery complete");
                numSentMessages++;
            }
        };

        // If the client disconnects, attempt to reconnect.
        connectOptions.setAutomaticReconnect(true);

        // If set to false, the broker will attempt to send any messages bound for this deviceID that couldn't be sent last session.
        connectOptions.setCleanSession(true);

        connectOptions.setMaxInflight(100);
        /* I think Paho uses an array to store its in flight messages rather than a more complex data structure
         * like a linked list. The results of this are that Paho will allocate all the memory needed to
         * hold the max number of in flight messages when .setMaxInflight() is called rather than allocating
         * memory as needed. Therefore the number max number of in flight messages should be as low as
         * possible to avoid wasting RAM.
         * - 100 is quite a liberal number, but setting it too low will cause crashes when the send rate is uncapped.
         * - 10 likely enough when rate limited to <1 message/second
         */

        client = new MqttAsyncClient(broker, this.clientID, persistence);

        client.setCallback(callback);

    }

    private void connect() {
        try {
            System.out.println(clientID + " Connecting to " + broker);
            client.connect(connectOptions).waitForCompletion(); // .waitForCompletion() = blocking call - program only regains control after the function returns
            if (!client.isConnected())
                throw new MqttException(MqttException.REASON_CODE_SERVER_CONNECT_ERROR);

            System.out.println(clientID + " Connected");

            client.subscribe(commandTopic, qos);
            client.subscribe(latencyReplyTopic, qos);

            //client.subscribe("#", 1); // Subscribe to all topics
        } catch (Exception e) {
            System.out.println(clientID + " Unable to connect");
            close();
        }
    }

    /**
     * Disconnects and closes client.
     */
    public void close() {
        try {
            if (client.isConnected())
                client.disconnect();

            client.close();

        } catch (MqttException me) {
            System.out.println(me);
            me.printStackTrace();
        }
    }

    /*
     * Creates and sends a new message based on the one that was just received.
     * Gets the timestamp from last message and compares it to the current time, and stores the difference in the last_latency field.
     * Then sets a new timestamp, and publishes new message.
     */
    private void sendNextMessage(MqttMessage message) throws InvalidMqttMessageException {
        try {

            long receiveTime = System.currentTimeMillis(); // Set time message was received

            Thread.sleep(waitPeriod); // Wait before sending next message

            JsonPayload json = new JsonPayload();
            json.deserialize(new String(message.getPayload())); // converting byte[] -> String requires passing to String through constructor

            json.setLastLatency(receiveTime - json.getTimestamp()); // Set new last_latency field
            json.setTimestamp(System.currentTimeMillis()); // Set timestamp field

            message.setPayload(json.serialize().getBytes());
            client.publish(latencyReportTopic, message);

        } catch (Exception e) {
            System.out.println(e);
            throw new InvalidMqttMessageException(message);
        }
    }

    /**
     * @return the unique ID the client is known to the Broker by
     */
    public String getClientID() {
        return clientID;
    }

    /*
     * Creates the first message to send the the CSV writer client
     * We use a JSON payload to organize our data
     * Sets the device_id, and time of creation in milliseconds
     * Sets the last_latency field to -1.0, indicating to the CSV writer that there has been no previous message.
     */
    private void sendInitialMessage() {
        JsonPayload msg = new JsonPayload();
        MqttMessage mqttMessage = new MqttMessage();

        // Create initial message payload String
        String json = "{"
                + "\"device_id\":\"" + clientID
                + "\"\"timestamp\":" + System.currentTimeMillis()
                + "\"last_latency\":" + -1.0
                + "}";

        try {

            msg.deserialize(json);
            mqttMessage.setPayload(msg.serialize().getBytes());
            client.publish(latencyReportTopic, mqttMessage);

        } catch (Exception e) {
            System.out.println(e);
        }
    }

    /**
     * Override for Runnable's run() method. called by Thread.start()
     */
    @Override
    public void run() {
        connect();
        sendInitialMessage();
    }

    int getNumSentMessages() {
        return numSentMessages;
    }
}

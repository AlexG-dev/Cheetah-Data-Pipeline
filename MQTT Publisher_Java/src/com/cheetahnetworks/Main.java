package com.cheetahnetworks;

import org.eclipse.paho.client.mqttv3.MqttException;

import java.util.ArrayList;

/**
 * Configures a set amount of Clients, starts them, and reports on their performance.
 *
 * @author Michael Boulerice
 */
public class Main {

    public static void main(String[] args) {

        final String BROKER = "tcp://35.209.240.115:1883"; // The Broker URI
        String clientID = "mike-desktop/test-device-"; // Stub String for creating device ID's
        final int NUM_CLIENTS = 15000; // Number of clients and threads to spawn. Each client has 2 threads: the master thread and the internal client thread
        int numSentMessages = 0; // Used to store the total number of sent messages across all clients
        long startTime = System.currentTimeMillis();
        double elapsedTime;
        boolean running = true;
        int waitPeriod = 5000; // milliseconds

        ArrayList<Client> clients = new ArrayList<Client>();
        ArrayList<Thread> threads = new ArrayList<Thread>(); //Currently two lists, because the Client class only implements Runnable as opposed to extending java.util.Thread

        try {
            // Configure and spawn clients
            for (int i = 0; i < NUM_CLIENTS; i++) {
                clients.add(new Client(BROKER, clientID + i, waitPeriod));
                threads.add(new Thread(clients.get(i)));
            }

            // Start the threads/clients
            for (int i = 0; i < NUM_CLIENTS; i++) { // Different for loops to prioritize client and thread creation
                threads.get(i).start();
                Thread.sleep(1); // Spread out the rate at which clients connect, helps avoid overloading the broker - TODO: Exponential Backoff
            }

            // Performance reporting loop
            while (running) {
                numSentMessages = 0;
                // Grab each clients' number of sent messages
                for (int i = 0; i < NUM_CLIENTS; i++) {
                    numSentMessages += clients.get(i).getNumSentMessages();
                }

                // Calculate the time the program has been running in seconds
                elapsedTime = (System.currentTimeMillis() - startTime) / 1000.0;

                // Display results
                System.out.println("Sent messages: " + numSentMessages + " | Elapsed time: " + elapsedTime + "s" + " | Send rate: " + numSentMessages / elapsedTime + "/s");

                // Wait for a second before repeating
                Thread.sleep(1000);
            }

            // When done, clean up
            for (int i = 0; i < NUM_CLIENTS; i++) {
                clients.get(i).close();
                threads.get(i).join();
            }

        } catch (MqttException me) { // For issues encountered by the clients
            System.out.println("reason " + me.getReasonCode());
            System.out.println("msg " + me.getMessage());
            System.out.println("loc " + me.getLocalizedMessage());
            System.out.println("cause " + me.getCause());
            System.out.println("excep " + me);
            me.printStackTrace();
        } catch (InterruptedException ie) { // For issues encountered by the threads
            System.out.println(ie);
            ie.printStackTrace();
        }


    }
}
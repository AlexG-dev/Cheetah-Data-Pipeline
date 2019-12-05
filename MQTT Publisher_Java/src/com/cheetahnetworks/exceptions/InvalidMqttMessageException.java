package com.cheetahnetworks.exceptions;

import org.eclipse.paho.client.mqttv3.MqttMessage;

/**
 * Thrown when an MQTT message received is not valid
 *
 * @author Michael Boulerice
 */
public class InvalidMqttMessageException extends Exception {
    public InvalidMqttMessageException(MqttMessage message) {
        super();
        System.out.println(message.getPayload());
    }

    public InvalidMqttMessageException(String message) {
        super();
        System.out.println(message);
    }
}

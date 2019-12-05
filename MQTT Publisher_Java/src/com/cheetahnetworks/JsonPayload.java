package com.cheetahnetworks;

import com.cheetahnetworks.exceptions.InvalidMqttMessageException;

import com.github.cliftonlabs.json_simple.JsonException;
import com.github.cliftonlabs.json_simple.JsonObject;
import com.github.cliftonlabs.json_simple.Jsoner;

import java.math.BigDecimal;

/**
 * Uses the Json-Simple library to manage the payloads of the messages being sent by the Client(s)
 *
 * @author Michael Boulerice
 */
public class JsonPayload {
    private JsonObject msg;
    private String deviceID;
    private Long timestamp;
    private Long lastLatency;

    public String getDeviceID() {
        return deviceID;
    }

    public void setDeviceID(String deviceID) {
        this.deviceID = deviceID;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public Long getLastLatency() {
        return lastLatency;
    }

    public void setLastLatency(Long lastLatency) {
        this.lastLatency = lastLatency;
    }

    /**
     * @param msg The serialized String to convert into a JSON object
     * @throws InvalidMqttMessageException Throws if the payload of the message is not valid.
     */
    public void deserialize(String msg) throws InvalidMqttMessageException {
        try {
            this.msg = (JsonObject) Jsoner.deserialize(msg);
            deviceID = (String) this.msg.get("device_id");
            timestamp = ((BigDecimal) this.msg.get("timestamp")).longValue();
            lastLatency = ((BigDecimal) this.msg.get("last_latency")).longValue();
        } catch (JsonException je) {
            System.out.println(je);
            throw new InvalidMqttMessageException(msg);
        }
    }

    /**
     * @return The payload of the Json message in serialized String format for transmission
     */
    public String serialize() {
        if (msg != null) {
            msg.put("device_id", deviceID);
            msg.put("timestamp", timestamp);
            msg.put("last_latency", lastLatency);
            return msg.toJson();
        } else {
            return null;
        }
    }
}
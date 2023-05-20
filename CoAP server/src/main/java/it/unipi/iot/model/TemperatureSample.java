package it.unipi.iot.model;

import java.sql.Timestamp;
import java.util.Calendar;

public class TemperatureSample {
    private int node; // Node ID
    private float temperature;

    private Timestamp timestamp; // set by the collector

    public TemperatureSample(int node, float temperature, Timestamp timestamp) {
        this.node = node;
        this.temperature = temperature;
        this.timestamp = timestamp;
    }

    /**
     * Function used to check if the sample is valid (if it has been done in the last 30sec)
     * @return  true if the timestamp is greater than 30 seconds ago, otherwise false
     */
    public boolean isValid ()
    {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.add(Calendar.SECOND, -30); // -30 seconds
        Timestamp thirtySecondsAgo = new Timestamp(calendar.getTime().getTime());
        return timestamp.after(thirtySecondsAgo);
    }

    public int getNode() {
        return node;
    }

    public void setNode(int node) {
        this.node = node;
    }

    public float getTemperature() {
        return temperature;
    }

    public void setTemperature(float temperature) {
        this.temperature = temperature;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "TemperatureSample{" +
                "node=" + node +
                ", temperature=" + temperature +
                ", timestamp=" + timestamp +
                '}';
    }
}

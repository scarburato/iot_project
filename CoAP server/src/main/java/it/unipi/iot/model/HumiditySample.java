package it.unipi.iot.model;

import java.sql.Timestamp;
import java.util.Calendar;

public class HumiditySample {
    private int node; // ID of the node
    private float humidity;

    private Timestamp timestamp; // set by the collector

    public HumiditySample(int node, float humidity, Timestamp timestamp) {
        this.node = node;
        this.humidity = humidity;
        this.timestamp = timestamp;
    }

    /**
     * Function used to check if the sample is valid (if it is related to a measure done in the last 30sec)
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

    public float getHumidity() {
        return humidity;
    }

    public void setHumidity(float humidity) {
        this.humidity = humidity;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "HumiditySample{" +
                "node=" + node +
                ", humidity=" + humidity +
                ", timestamp=" + timestamp +
                '}';
    }
}

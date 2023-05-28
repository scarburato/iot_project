package it.unipi.iot.model;

import java.sql.Timestamp;
import java.util.Calendar;

public class AirQualitySample {
    private int node; // Node ID
    private int co2;

    private Timestamp timestamp; // set by the collector


    public AirQualitySample(int node, int co2, Timestamp timestamp) {
        this.node = node;
        this.co2 = co2;
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

    public int getCo2() {
        return co2;
    }

    public void setCo2(int co2) {
        this.co2 = co2;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "AirQualitySample{" +
                "node=" + node +
                ", concentration=" + co2 +
                ", timestamp=" + timestamp +
                '}';
    }
}

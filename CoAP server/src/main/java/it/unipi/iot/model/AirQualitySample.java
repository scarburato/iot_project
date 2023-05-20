package it.unipi.iot.model;

import java.sql.Timestamp;
import java.util.Calendar;

public class AirQualitySample {
    private int node; // Node ID
    private int concentration;

    private Timestamp timestamp; // set by the collector


    public AirQualitySample(int node, int concentration, Timestamp timestamp) {
        this.node = node;
        this.concentration = concentration;
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

    public int getConcentration() {
        return concentration;
    }

    public void setConcentration(int concentration) {
        this.concentration = concentration;
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
                ", concentration=" + concentration +
                ", timestamp=" + timestamp +
                '}';
    }
}

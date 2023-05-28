package it.unipi.iot.model;

import java.sql.Timestamp;
import java.util.Calendar;

public class FloatLevelSample {
    private int node;
    private boolean lowLevel;
    private Timestamp timestamp;

    public FloatLevelSample(int node, boolean lowLevel) {
        this.node = node;
        this.lowLevel = lowLevel;
    }

    public boolean getLowLevel() {
        return lowLevel;
    }

    public void setQuantity(boolean lowLevel) {
        this.lowLevel = lowLevel;
    }

    @Override
    public String toString() {
        return "PresenceSample{ " +
                "lowLevel=" + lowLevel +
                '}';
    }
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
    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }


    public void setNode(int node) {
        this.node = node;
    }
}

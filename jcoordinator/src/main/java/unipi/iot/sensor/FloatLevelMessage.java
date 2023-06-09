package unipi.iot.sensor;

public class FloatLevelMessage implements TopicMessage{
    public long node; // Node ID
    public boolean isLevelLow;
    public Long getSensorId() {
        return node;
    }

    @Override
    public int getValue() {
        return isLevelLow ? 1 : 0;
    }
}

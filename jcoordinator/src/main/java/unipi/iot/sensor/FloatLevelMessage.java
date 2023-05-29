package unipi.iot.sensor;

public class FloatLevelMessage implements TopicMessage{
    public long node; // Node ID
    public boolean floatLevel;
    public Long getSensorId() {
        return node;
    }
}

package unipi.iot.sensor;

public class HumidityMessage implements TopicMessage{
    public long node; // Node ID
    public int humidity;
    public Long getSensorId() {
        return node;
    }
}

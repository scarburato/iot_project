package unipi.iot.sensor;

public class TemperatureMessage implements TopicMessage{
    public long node; // Node ID
    public int temperature;
    public Long getSensorId() {
        return node;
    }

    @Override
    public int getValue() {
        return temperature;
    }
}

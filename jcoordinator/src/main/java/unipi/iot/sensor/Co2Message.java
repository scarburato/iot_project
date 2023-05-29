package unipi.iot.sensor;

public class Co2Message implements TopicMessage {
    public long node; // Node ID
    public int co2;
    public Long getSensorId() {
        return node;
    }


}

package unipi.iot.actuator;

public interface Actuator {
    void sendMessage(String message);
    String getIp();
}

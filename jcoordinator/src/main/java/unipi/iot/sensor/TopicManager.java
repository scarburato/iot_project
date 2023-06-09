package unipi.iot.sensor;

import org.eclipse.paho.client.mqttv3.*;
import unipi.iot.actuator.ActuatorManager;

public interface TopicManager {
    TopicMessage parse(MqttMessage message);
    void callback(TopicMessage parsedMessage, ActuatorManager m);
}

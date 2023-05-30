package unipi.iot.sensor;

import com.google.gson.Gson;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import unipi.iot.actuator.ActuatorManager;
import unipi.iot.actuator.PumpManager;

public class FloatLevelManager implements TopicManager{
    private static final Gson parser = new Gson();
    public TopicMessage parse(MqttMessage message) {
        return parser.fromJson(new String(message.getPayload()), FloatLevelMessage.class);
    }

    public void callback(TopicMessage parsedMessage, ActuatorManager actManager) {
        FloatLevelMessage message = (FloatLevelMessage) parsedMessage;
        PumpManager manager = (PumpManager) actManager;

        // @TODO Inserisci nella base di dati

        manager.getAssociatedSensor(message.getSensorId()).sendMessage(
               message.isLevelLow ? "ON" : "OFF"
        );
    }
}

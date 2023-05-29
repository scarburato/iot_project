package unipi.iot.sensor;

import com.google.gson.Gson;
import org.eclipse.paho.client.mqttv3.*;
import unipi.iot.actuator.ActuatorManager;
import unipi.iot.actuator.FanManager;

public class Co2Manager implements TopicManager{
    private static final Gson parser = new Gson();
    public TopicMessage parse(MqttMessage message) {
        return parser.fromJson(new String(message.getPayload()), Co2Message.class);
    }

    public void callback(TopicMessage parsedMessage, ActuatorManager actManager) {
        Co2Message message = (Co2Message) parsedMessage;
        FanManager manager = (FanManager) actManager;

        // @TODO Inserisci nella base di dati

        manager.getAssociatedSensor(message.getSensorId()).sendMessage(
                message.co2 >= 500 ? "ON" : "OFF"
        );
    }
}

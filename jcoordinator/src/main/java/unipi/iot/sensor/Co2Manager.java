package unipi.iot.sensor;

import com.google.gson.Gson;
import org.eclipse.paho.client.mqttv3.*;
import unipi.iot.DBDriver;
import unipi.iot.actuator.Actuator;
import unipi.iot.actuator.ActuatorManager;
import unipi.iot.actuator.FanManager;

public class Co2Manager implements TopicManager{
    private static final Gson parser = new Gson();
    public int lastCo2Registered;
    public int threshold = 500;
    public TopicMessage parse(MqttMessage message) {
        return parser.fromJson(new String(message.getPayload()), Co2Message.class);
    }

    public void callback(TopicMessage parsedMessage, ActuatorManager actManager) {
        Co2Message message = (Co2Message) parsedMessage;
        FanManager manager = (FanManager) actManager;

        lastCo2Registered = message.co2;

        Actuator s = manager.getAssociatedSensor(message.getSensorId());

        if(message.getValue() >= threshold)
            s.sendMessage("ON");
        else if(message.getValue() <= threshold-200)
            s.sendMessage("OFF");

        DBDriver.getInstance().insertCO2Sample(message);
    }
}

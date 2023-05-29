package unipi.iot.sensor;

import com.google.gson.Gson;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import unipi.iot.actuator.AcManager;
import unipi.iot.actuator.ActuatorManager;
import unipi.iot.actuator.FanManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TemperatureManager implements TopicManager{
    private static final Gson parser = new Gson();

    private static class Statistics{
        public static class Datum {public int temperature; public long timestamp;}
        public static final int lowerBoundTemperature= 15;
        public static final int upperBoundTemperature = 25;
        private final List<Datum> data = new ArrayList<>();

        public void add(int temperature) {
            Datum datum = new Datum();
            datum.temperature = temperature;
            datum.timestamp = System.currentTimeMillis();

            data.add(datum);
        }
        public int getLowerBoundTemperature(){
            return lowerBoundTemperature;
        }

        public int getUpperBoundTemperature(){
            return upperBoundTemperature;
        }
        public void clean() {
            // ~ 30 secondi
            long thirtysecondsago = System.currentTimeMillis() - 30* 1000L;
            data.removeIf(datum -> datum.timestamp < thirtysecondsago);
        }

        public double average() {
            return data.stream()
                    .map(datum -> (double)datum.temperature) // take only the humidity
                    .reduce(0.0d, Double::sum) / data.size();
        }

        public double midRange(){
            return (lowerBoundTemperature + upperBoundTemperature) / 2;
        }
    }
    private final Map<Long, Statistics> sensorsStats = new HashMap<>();
    @Override
    public TopicMessage parse(MqttMessage message) {
        return parser.fromJson(new String(message.getPayload()), TemperatureMessage.class);
    }

    @Override
    public void callback(TopicMessage parsedMessage, ActuatorManager actManager) {
        TemperatureMessage message = (TemperatureMessage) parsedMessage;
        AcManager manager = (AcManager) actManager;

        // @TODO Inserisci nella base di dati

        if(! sensorsStats.containsKey(message.getSensorId()))
            sensorsStats.put(message.getSensorId(), new Statistics());

        Statistics sensorStats = sensorsStats.get(message.getSensorId());
        double oldAvg = sensorStats.average();
        sensorStats.add(message.temperature);
        sensorStats.clean();
        double avg = sensorStats.average();
        double midRange = sensorStats.midRange();

        if(avg < (sensorStats.getLowerBoundTemperature()+(midRange-sensorStats.getLowerBoundTemperature())/2)){
            // INC
        }else if(avg > (sensorStats.getUpperBoundTemperature() - (sensorStats.getUpperBoundTemperature() - midRange)/2)){
            // DEC
        }else{
            // OFF
        }

        manager.getAssociatedSensor(message.getSensorId()).sendMessage(
                message.temperature >= 500 ? "ON" : "OFF"
        );
    }
}

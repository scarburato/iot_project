package unipi.iot.sensor;

import com.google.gson.Gson;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import unipi.iot.DBDriver;
import unipi.iot.actuator.ActuatorManager;
import unipi.iot.actuator.HumidifierManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HumidityManager implements TopicManager{
    private static final Gson parser = new Gson();
    private static class Statistics{
        public static class Datum {public int humidity; public long timestamp;}
        public static final int lowerBoundHumidity = 40;
        public static final int upperBoundHumidity = 60;
        private final List<Datum> data = new ArrayList<>();

        public void add(int humidity) {
            Datum datum = new Datum();
            datum.humidity = humidity;
            datum.timestamp = System.currentTimeMillis();

            data.add(datum);
        }
        public int getLowerBoundHumidity(){
            return lowerBoundHumidity;
        }

        public int getUpperBoundHumidity(){
            return upperBoundHumidity;
        }
        public void clean() {
            // ~ 30 secondi
            long thirtysecondsago = System.currentTimeMillis() - 30* 1000L;
            data.removeIf(datum -> datum.timestamp < thirtysecondsago);
        }

        public double average() {
            return data.stream()
                    .map(datum -> (double)datum.humidity) // take only the humidity
                    .reduce(0.0d, Double::sum) / data.size();
        }

        public double midRange(){
            return (lowerBoundHumidity + upperBoundHumidity) / 2;
        }
    }
    private final Map<Long, HumidityManager.Statistics> sensorsStats = new HashMap<>();
    public TopicMessage parse(MqttMessage message) {
        return parser.fromJson(new String(message.getPayload()), HumidityMessage.class);
    }

    public void callback(TopicMessage parsedMessage, ActuatorManager actManager) {
        HumidityMessage message = (HumidityMessage) parsedMessage;
        HumidifierManager manager = (HumidifierManager) actManager;

        // @TODO Inserisci nella base di dati
        if(! sensorsStats.containsKey(message.getSensorId()))
            sensorsStats.put(message.getSensorId(), new Statistics());

        HumidityManager.Statistics sensorStats = sensorsStats.get(message.getSensorId());
        double oldAvg = sensorStats.average();
        sensorStats.add(message.humidity);
        sensorStats.clean();
        double avg = sensorStats.average();
        double midRange = sensorStats.midRange();
        String mes;
        if(avg < (sensorStats.getLowerBoundHumidity()+(midRange-sensorStats.getLowerBoundHumidity())/2)){
            // INC
            mes = "INC";
        }else if(avg > (sensorStats.getUpperBoundHumidity() - (sensorStats.getUpperBoundHumidity() - midRange)/2)){
            // DEC
            mes = "DEC";
        }else{
            // OFF
            mes = "OFF";
        }
        manager.getAssociatedSensor(message.getSensorId()).sendMessage(
                mes
        );
        DBDriver.getInstance().insertHumiditySample(message);
    }
}

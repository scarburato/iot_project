package it.unipi.iot.mqtt;

import com.google.gson.Gson;
import it.unipi.iot.CoapManager;
import it.unipi.iot.devices.DevicesHandler;
import it.unipi.iot.log.Logger;
import it.unipi.iot.model.AirQualitySample;
import it.unipi.iot.model.FloatLevelSample;
import it.unipi.iot.model.HumiditySample;
import it.unipi.iot.model.TemperatureSample;
import it.unipi.iot.mqtt.sensors.Co2;
import it.unipi.iot.mqtt.sensors.Humidity;
import it.unipi.iot.mqtt.sensors.Temperature;
import it.unipi.iot.mqtt.sensors.WaterFloat;
import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.eclipse.paho.client.mqttv3.*;

import java.net.SocketException;

public class Handler implements MqttCallback {
    private final String BROKER = "tcp://[::1]:1883";
    private final String CLIENT_ID = "SmartZooCollector";
    private final int SECONDS_TO_WAIT_FOR_RECONNECTION = 5;
    private final int MAX_RECONNECTION_ITERATIONS = 10;

    private MqttClient mqttClient = null;
    private Gson parser;
    private Humidity humidityCollector;
    private Temperature temperatureCollector;

    private Co2 co2Collector;
    private WaterFloat waterFloatCollector;

    private Logger logger;

    public Handler ()
    {
        parser = new Gson();
        logger = Logger.getInstance();
        humidityCollector = new Humidity();
        temperatureCollector = new Temperature();
        co2Collector = new Co2();
        waterFloatCollector = new WaterFloat();
        do {
            try {
                mqttClient = new MqttClient(BROKER, CLIENT_ID);
                System.out.println("Connecting to the broker: " + BROKER);
                mqttClient.setCallback( this );
                connectToBroker();
            }
            catch(MqttException me)
            {
                System.out.println("I could not connect, Retrying ...");
            }
        }while(!mqttClient.isConnected());
    }

    /**
     * This function is used to try to connect to the broker
     */
    private void connectToBroker () throws MqttException {
        mqttClient.connect();
        mqttClient.subscribe(Humidity.HUMIDITY_TOPIC);
        System.out.println("Subscribed to: " + Humidity.HUMIDITY_TOPIC);
        mqttClient.subscribe(Temperature.TEMPERATURE_TOPIC);
        System.out.println("Subscribed to: " + Temperature.TEMPERATURE_TOPIC);
        mqttClient.subscribe(Co2.CO2_TOPIC);
        System.out.println("Subscribed to: " + Co2.CO2_TOPIC);
        mqttClient.subscribe(WaterFloat.FLOAT_TOPIC);
        System.out.println("Subscribed to: " + WaterFloat.FLOAT_TOPIC);
    }

    /**
     * Function used to publish a message
     * @param topic     topic of the message
     * @param message   message to send
     */
    public void publishMessage (final String topic, final String message)
    {
        try
        {
            mqttClient.publish(topic, new MqttMessage(message.getBytes()));
        }
        catch(MqttException e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public void connectionLost(Throwable throwable) {
        System.out.println("Connection with the Broker lost!");
        // We have lost the connection, we have to try to reconnect after waiting some time
        // At each iteration we increase the time waited
        int iter = 0;
        do {
            iter++; // first iteration iter=1
            if (iter > MAX_RECONNECTION_ITERATIONS)
            {
                System.err.println("Reconnection with the broker not possible!");
                System.exit(-1);
            }
            try
            {
                Thread.sleep(SECONDS_TO_WAIT_FOR_RECONNECTION * 1000 * iter);
                System.out.println("New attempt to connect to the broker...");
                connectToBroker();
            }
            catch (MqttException | InterruptedException e)
            {
                e.printStackTrace();
            }
        } while (!this.mqttClient.isConnected());
        System.out.println("Connection with the Broker restored!");
    }

    @Override
    public void messageArrived(String topic, MqttMessage mqttMessage) throws Exception {
        String payload = new String(mqttMessage.getPayload());
        if (topic.equals(Humidity.HUMIDITY_TOPIC))
        {
            HumiditySample humiditySample = parser.fromJson(payload, HumiditySample.class);
            humidityCollector.addHumiditySample(humiditySample);
            float newAverage = humidityCollector.getAverage();
            float midRange = humidityCollector.getMidRange();
            if (newAverage < (humidityCollector.getLowerBoundHumidity() + (midRange - humidityCollector.getLowerBoundHumidity())/2))
            {
                if (!humidityCollector.getLastCommand().equals(Humidity.INC))
                {
                    logger.logHumidity("Average level of Humidity too low: " + newAverage + "%, increase it");
                    publishMessage(Humidity.HUMIDIFIER_TOPIC, Humidity.INC);
                    humidityCollector.setLastCommand(Humidity.INC);
                }
                else
                    logger.logHumidity("Average level of Humidity too low: " + newAverage + "%, but is increasing");
            }
            else if (newAverage > (humidityCollector.getUpperBoundHumidity() - (humidityCollector.getUpperBoundHumidity() - midRange)/2))
            {
                if (!humidityCollector.getLastCommand().equals(Humidity.DEC))
                {
                    logger.logHumidity("Average level of Humidity too high: " + newAverage + "%, decrease it");
                    publishMessage(Humidity.HUMIDIFIER_TOPIC, Humidity.DEC);
                    humidityCollector.setLastCommand(Humidity.DEC);
                }
                else
                    logger.logHumidity("Average level of Humidity too high: " + newAverage + "%, but is decreasing");
            }
            else
            {
                if (!humidityCollector.getLastCommand().equals(Humidity.OFF))
                {
                    logger.logHumidity("Correct average humidity level: " + newAverage + "%, switch off the humidifier/dehumidifier");
                    publishMessage(Humidity.HUMIDIFIER_TOPIC, Humidity.OFF);
                    humidityCollector.setLastCommand(Humidity.OFF);
                }
                else
                {
                    logger.logHumidity("Correct average humidity level: " + newAverage + "%");
                }
            }
        }
        else if (topic.equals(Temperature.TEMPERATURE_TOPIC))
        {
            TemperatureSample temperatureSample = parser.fromJson(payload, TemperatureSample.class);
            temperatureCollector.addTemperatureSample(temperatureSample);
            float newAverage = temperatureCollector.getAverage();
            float midRange = temperatureCollector.getMidRange();
            if (newAverage < (temperatureCollector.getLowerBoundTemperature() + (midRange - temperatureCollector.getLowerBoundTemperature())/2))
            {
                if (!temperatureCollector.getLastCommand().equals(Temperature.INC))
                {
                    logger.logTemperature("Average level of temperature too low: " + newAverage + "°C, increase it");
                    publishMessage(Temperature.AC_TOPIC, Temperature.INC);
                    temperatureCollector.setLastCommand(Temperature.INC);
                }
                else
                    logger.logTemperature("Average level of temperature too low: " + newAverage + "°C, but is increasing");
            }
            else if (newAverage > (temperatureCollector.getUpperBoundTemperature() - (temperatureCollector.getUpperBoundTemperature() - midRange)/2))
            {
                if (!temperatureCollector.getLastCommand().equals(Temperature.DEC))
                {
                    logger.logTemperature("Average level of temperature too high: " + newAverage + "°C, decrease it");
                    publishMessage(Temperature.AC_TOPIC, Temperature.DEC);
                    temperatureCollector.setLastCommand(Temperature.DEC);
                }
                else
                    logger.logTemperature("Average level of temperature too high: " + newAverage + "°C, but is decreasing");
            }
            else
            {
                if (!temperatureCollector.getLastCommand().equals(Temperature.OFF))
                {
                    logger.logTemperature("Correct average temperature level: " + newAverage +"°C, switch off the AC");
                    logger.logTemperature("Correct average temperature level: " + newAverage +"°C, switch off the AC");
                    publishMessage(Temperature.AC_TOPIC, Temperature.OFF);
                    temperatureCollector.setLastCommand(Temperature.OFF);
                }
                else
                    logger.logTemperature("Correct average temperature level: " + newAverage + "°C");
            }
        }
        else if (topic.equals(Co2.CO2_TOPIC)) {
            AirQualitySample airQualitySample = parser.fromJson(payload, AirQualitySample.class);
            //co2Collector.addSample(airQualitySample);
            DevicesHandler.getInstance().getAirQuality().gimmeÐat().put(airQualitySample.getCo2() >= 500 ? "ON" : "OFF", MediaTypeRegistry.TEXT_PLAIN);
        }
        else if(topic.equals(WaterFloat.FLOAT_TOPIC)){
            FloatLevelSample floatLevelSample = parser.fromJson(payload,FloatLevelSample.class);
            DevicesHandler.getInstance().getFloatSensor().gimmeÐat().put(floatLevelSample.getLowLevel() == true ? "ON":"OFF",MediaTypeRegistry.TEXT_PLAIN);
        }

    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {
        logger.logInfo("Message correctly delivered");
    }

    public Humidity getHumidityCollector() {
        return humidityCollector;
    }

    public Temperature getTemperatureCollector() {
        return temperatureCollector;
    }

    public static void main(String[] args) throws SocketException {
        Handler mqttNetworkHandler = new Handler();
        CoapManager server = new CoapManager();
        server.start();
    }
}

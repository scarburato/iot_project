package unipi.iot;

import com.google.gson.Gson;
import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.CoapServer;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.network.CoapEndpoint;
import org.eclipse.californium.core.network.config.NetworkConfig;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.eclipse.paho.client.mqttv3.*;
import unipi.iot.actuator.*;
import unipi.iot.sensor.*;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.eclipse.californium.core.network.config.NetworkConfig.Keys.COAP_PORT;

/**
 * Hello world!
 *
 */
public class Coordinator extends CoapServer implements MqttCallback
{
    private static final String BROKER = "tcp://[::1]:1883";
    private static final String CLIENT_ID = "SmartZooCollector";
    private static final Map<String, TopicManager> TOPICS = new HashMap<String, TopicManager>() {{
        put("co2", new Co2Manager());
        put("float", new FloatLevelManager());
        put("humidity", new HumidityManager());
    }};

    private static final Map<String, ActuatorManager> ACTUATORS = new HashMap<String, ActuatorManager>() {{
       put("fan", new FanManager());
       put("pump", new PumpManager());
       put("dehumidifier", new HumidifierManager());
       put("light", new LightManager());
    }};

    private static final Map<String, String> TOPIC_TO_ACTUATOR = new HashMap<String, String>() {{
        put("co2", "fan");
        put("float", "pump");
        put("humidity", "dehumidifier");
    }};

    public TopicManager getTopicManager(String topic) {
        return TOPICS.get(topic);
    }

    public ActuatorManager getActuatorManager(String t) {
        return ACTUATORS.get(t);
    }

    private static class CoapRegistrationResource extends CoapResource {
        public CoapRegistrationResource() {
            super("registration");
        }

        private Gson gson = new Gson();

        private static class RegistrationMessage {
            public String deviceType;
            public long sensorId;
        }

        @Override
        public void handlePOST(CoapExchange exchange) {
            final String ip = exchange.getSourceAddress().getHostAddress();
            try {
                System.err.println(exchange.getRequestText());
                RegistrationMessage m = gson.fromJson(exchange.getRequestText(), RegistrationMessage.class);

                System.out.println("New actuator at " + ip + " its sensor is " + m.sensorId + " payload is " + exchange.getRequestText());

                ACTUATORS.get(m.deviceType).registerNewActuator(m.sensorId, ip);

                DBDriver.getInstance().registerActuator(ip, m.deviceType);

                exchange.respond(CoAP.ResponseCode.CREATED, "Success".getBytes(StandardCharsets.UTF_8));
            }
            catch (Throwable e) {
                e.printStackTrace();
                System.out.println("Unable to register coap actuator! " + ip);
                exchange.respond(CoAP.ResponseCode.NOT_ACCEPTABLE, "Unsuccessful".getBytes(StandardCharsets.UTF_8));
            }

        }

        @Override
        public void handleDELETE(CoapExchange exchange) {
            RegistrationMessage m = gson.fromJson(exchange.getRequestText(), RegistrationMessage.class);
            String ip = exchange.getSourceAddress().getHostAddress();

            System.out.println("Actuator at " + ip + " is leaving the network, leaving sensor " + m.sensorId + " orphan!");

            ACTUATORS.get(m.deviceType).deleteActuator(m.sensorId);
        }
    }

    private MqttClient mqttClient = null;

    public void connectionLost(Throwable throwable) {
        throwable.printStackTrace();
        System.out.println(throwable.getMessage());
        System.out.println("CONNECTION LOST");
        System.exit(-1);
    }

    public void messageArrived(String topic, MqttMessage mqttMessage) throws Exception {
        TopicManager manager = TOPICS.get(topic);
        TopicMessage m = manager.parse(mqttMessage);

        System.out.println("Incoming message from " + m.getSensorId() + " with topic " + topic + " value=" + m.getValue());

        try {
            manager.callback(m, ACTUATORS.get(TOPIC_TO_ACTUATOR.get(topic)));
        } catch (Throwable e) {
            System.out.println("Failed to run callback() bc " + e.getMessage());
        }
    }

    public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {

    }

    public Coordinator() {
        do {
            try {
                mqttClient = new MqttClient(BROKER, CLIENT_ID);
                System.out.println("Connecting to the broker: " + BROKER);

                mqttClient.setCallback( this );
                mqttClient.connect();
                for(String topic : TOPICS.keySet()) {
                    mqttClient.subscribe(topic);
                    System.out.println("Subscribed to: " + topic);
                }
            }
            catch(MqttException me)
            {
                System.out.println("I could not connect, Retrying ...");
            }
        } while(!mqttClient.isConnected());

        // CoAP stuff
        this.add(new CoapRegistrationResource());
    }

    public static void main( String[] args ) throws UnknownHostException {
        Coordinator coordinator = new Coordinator();
        InetAddress addr = InetAddress.getByName("0.0.0.0");
        InetSocketAddress bindToAddress = new InetSocketAddress(addr, 5683);
        coordinator.addEndpoint(new CoapEndpoint(bindToAddress));
        coordinator.start();
    }
}

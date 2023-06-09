package unipi.iot.actuator;

import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.coap.MediaTypeRegistry;

public class Humidifier implements Actuator{
    private String ip;
    CoapClient coapClient;

    public Humidifier(String ip) {
        this.ip = ip;
        coapClient = new CoapClient("coap://[" + ip + "]/humidity/humidifier");
    }

    public void sendMessage(String message) {
        System.out.println();
        coapClient.put(message, MediaTypeRegistry.TEXT_PLAIN);
    }

    @Override
    public String getIp() {
        return ip;
    }
}

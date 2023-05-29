package unipi.iot.actuator;

import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.coap.MediaTypeRegistry;

public class Ac implements Actuator{
    private String ip;
    CoapClient coapClient;

    public Ac(String ip) {
        this.ip = ip;
        coapClient = new CoapClient("coap://[" + ip + "]/temperature/ac");
    }

    public void sendMessage(String message) {
        System.out.println();
        coapClient.put(message, MediaTypeRegistry.TEXT_PLAIN);
    }
}

package unipi.iot.actuator;

import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.coap.MediaTypeRegistry;

public class Fan implements Actuator {
    private String ip;
    CoapClient coapClient;

    public Fan(String ip) {
        this.ip = ip;
        coapClient = new CoapClient("coap://[" + ip + "]/air_quality/ventilation");
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

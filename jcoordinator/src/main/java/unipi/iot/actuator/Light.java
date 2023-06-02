package unipi.iot.actuator;

import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.coap.MediaTypeRegistry;

import java.util.HashMap;

public class Light implements Actuator{
    private String ip;
    private CoapClient coapColorEndpoint;
    private CoapClient coapSwitchEndpoint;
    public static enum Color {RED, YELLOW, GREEN};
    private static final HashMap<Color, String> colorValues = new HashMap<>() {{
        put(Color.RED, "RED");
        put(Color.YELLOW, "YELLOW");
        put(Color.GREEN, "GREEN");
    }};

    public Light(String ip) {
        this.ip = ip;
        coapColorEndpoint = new CoapClient("coap://[" + ip + "]/light/color");
        coapSwitchEndpoint = new CoapClient("coap://[" + ip + "]/light/switch");
    }
    @Override
    public void sendMessage(String message) {

    }

    public void setColor(Color color) {
        coapColorEndpoint.put("color=" + colorValues.get(color), MediaTypeRegistry.TEXT_PLAIN);
    }

    public void setSwitch(boolean state) {
        coapSwitchEndpoint.put("mode=" + (state ? "ON" : "OFF"), MediaTypeRegistry.TEXT_PLAIN);
    }
}

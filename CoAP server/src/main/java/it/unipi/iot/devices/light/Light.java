package it.unipi.iot.devices.light;

import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapHandler;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.MediaTypeRegistry;

import java.util.ArrayList;
import java.util.List;

public class Light {
    public static enum LightColor {
        GREEN,
        YELLOW,
        RED
    }

    private List<CoapClient> clientLightSwitchList = new ArrayList<>();
    private List<CoapClient> clientLightColorList = new ArrayList<>();

    public void registerLight(String ip) {
        System.out.print("[REGISTRATION] The light: [" + ip + "] is now registered");
        CoapClient newClientLightSwitch = new CoapClient("coap://[" + ip + "]/light/switch");
        CoapClient newClientLightColor = new CoapClient("coap://[" + ip + "]/light/color");

        clientLightSwitchList.add(newClientLightSwitch);
        clientLightColorList.add(newClientLightColor);
    }

    public void unregisterLight(String ip) {
        for(int i=0; i<clientLightSwitchList.size(); i++) {
            if(clientLightSwitchList.get(i).getURI().equals(ip)) {
                clientLightSwitchList.remove(i);
                clientLightColorList.remove(i);
            }
        }
    }

    public void lightSwitch(boolean on) {
        if(clientLightSwitchList == null)
            return;

        String msg = "mode=" + (on ? "ON" : "OFF");
        for(CoapClient clientLightSwitch: clientLightSwitchList) {
            clientLightSwitch.put(new CoapHandler() {
                @Override
                public void onLoad(CoapResponse coapResponse) {
                    if (coapResponse != null) {
                        if (!coapResponse.isSuccess())
                            System.out.print("[ERROR]Light Switch: PUT request unsuccessful");
                    }
                }

                @Override
                public void onError() {
                    System.err.print("[ERROR] Light Switch " + clientLightSwitch.getURI() + "]");
                }
            }, msg, MediaTypeRegistry.TEXT_PLAIN);
        }
    }

    public void changeLightColor(LightColor color) {
        if(clientLightColorList == null)
            return;

        String msg = "color=" + color.name();
        for(CoapClient clientLightColor: clientLightColorList) {
            clientLightColor.put(new CoapHandler() {
                @Override
                public void onLoad(CoapResponse coapResponse) {
                    if (!coapResponse.isSuccess())
                        System.out.print("[ERROR] Light Color: PUT request unsuccessful");
                }

                @Override
                public void onError() {
                    System.err.print("[ERROR] Light Color " + clientLightColor.getURI() + "]");
                }
            }, msg, MediaTypeRegistry.TEXT_PLAIN);
        }
    }
}

package it.unipi.iot;

import it.unipi.iot.devices.DevicesHandler;
import it.unipi.iot.devices.light.Light;
import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.CoapServer;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.server.resources.CoapExchange;

import java.net.SocketException;
import java.nio.charset.StandardCharsets;

public class CoapManager extends CoapServer {
    private final DevicesHandler coapDevicesHandler = DevicesHandler.getInstance();

    public CoapManager() throws SocketException {
        this.add(new CoapRegistrationResource());
    }

    // GET measures from sensors
    //public int getCO2Level() {
    //    return coapDevicesHandler.getCO2Level();
    //}

    // SET
    public void setLightColor(Light.LightColor lightColor) {
        coapDevicesHandler.setLightColor(lightColor);
    }

   // public void setCO2UpperBound(int co2UpperBound) {
   //     coapDevicesHandler.setCO2UpperBound(co2UpperBound);
   // }

    class CoapRegistrationResource extends CoapResource {
        public CoapRegistrationResource() {
            super("registration");
        }

        @Override
        public void handlePOST(CoapExchange exchange) {
            String deviceType = exchange.getRequestText();
            String ip = exchange.getSourceAddress().getHostAddress();
            boolean success = true;

            switch (deviceType) {
                case "pump":
                    coapDevicesHandler.registerFloatSensor(ip);
                    break;
                case "air_quality":
                    coapDevicesHandler.registerAirQuality(ip);
                    break;
                case "light":
                    coapDevicesHandler.registerLight(ip);
                    break;
                default:
                    success = false;
                    break;
            }

            if (success)
                exchange.respond(CoAP.ResponseCode.CREATED, "Success".getBytes(StandardCharsets.UTF_8));
            else
                exchange.respond(CoAP.ResponseCode.NOT_ACCEPTABLE, "Unsuccessful".getBytes(StandardCharsets.UTF_8));
        }

        @Override
        public void handleDELETE(CoapExchange exchange) {
            String[] request = exchange.getRequestText().split("-");
            String ip = request[0];
            String deviceType = request[1];
            boolean success = true;

            switch (deviceType) {
                case "air_quality":
                    coapDevicesHandler.unregisterAirQuality(ip);
                    break;
                case "light":
                    coapDevicesHandler.unregisterLight(ip);
                    break;
                default:
                    success = false;
                    break;
            }

            if (success)
                exchange.respond(CoAP.ResponseCode.DELETED, "Cancellation Completed!".getBytes(StandardCharsets.UTF_8));
            else
                exchange.respond(CoAP.ResponseCode.BAD_REQUEST, "Cancellation not allowed!".getBytes(StandardCharsets.UTF_8));
        }
    }

    public static void main(String[] argv) throws SocketException {
        CoapManager server = new CoapManager();
        //server.add(new CoAPResourceExample("hello"));
        server.start();
    }
        /*
        MyServer server = new MyServer();
        server.add(new CoAPResourceExample("hello"));
        server.start();
        */
}

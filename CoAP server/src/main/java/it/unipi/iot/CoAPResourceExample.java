package it.unipi.iot;

import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.core.server.resources.CoapExchange;

public class CoAPResourceExample extends CoapResource {
    public CoAPResourceExample(String name) {
        super(name);
        super.setObservable(true);
    }

    public void handleGET(CoapExchange exchange) {
        exchange.respond("hello world");
    }

    public void handlePOST(CoapExchange exchange) {
        //int number = Integer.parseInt(exchange.getQueryParameter("number"));

        int number = Integer.parseInt(new String(exchange.getRequestPayload()));
        number = number * number;

        Response response = new Response(CoAP.ResponseCode.CONTENT);
        if(exchange.getRequestOptions().getAccept() ==
                MediaTypeRegistry.APPLICATION_XML){
            response.getOptions().setContentFormat(MediaTypeRegistry.APPLICATION_XML);
            response.setPayload("<value>" + number +"</value>");
        }else if(exchange.getRequestOptions().getAccept() ==
                MediaTypeRegistry.APPLICATION_JSON){
            response.getOptions().setContentFormat(MediaTypeRegistry.APPLICATION_JSON);
            response.setPayload("{value: " + number +"}");
        } else
            response.setPayload(Integer.toString(number));
        exchange.respond(response);
    }
}

package it.unipi.iot.devices.dehumidifier;

import org.eclipse.californium.core.CoapClient;

import java.util.ArrayList;
import java.util.List;

public class Dehumidifier {
    private List<CoapClient> clientDehumidifierSystemList = new ArrayList<>();

    public Dehumidifier(){

    }

    public void registerDehumidifier(String ip){
        System.out.print("\n[REGISTRATION] The Dehumidifier system: [" + ip + "] is now registered\n>");

        CoapClient newClientAC = new CoapClient("coap://[" + ip + "]/humidity/dehumidifier");
        clientDehumidifierSystemList.add(newClientAC);
    }
}

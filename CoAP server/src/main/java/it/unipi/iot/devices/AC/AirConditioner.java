package it.unipi.iot.devices.AC;

import org.eclipse.californium.core.CoapClient;

import java.util.ArrayList;
import java.util.List;

public class AirConditioner {
    private List<CoapClient> clientAirConditionerSystemList = new ArrayList<>();

    public AirConditioner(){

    }

    public void registerAirConditioner(String ip) {
        System.out.print("\n[REGISTRATION] The Air Conditioner system: [" + ip + "] is now registered\n>");

        CoapClient newClientAC = new CoapClient("coap://[" + ip + "]/temperature/AC");
        clientAirConditionerSystemList.add(newClientAC);
    }

}

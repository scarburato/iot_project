package it.unipi.iot.devices.floatSensor;

import com.google.gson.Gson;
import it.unipi.iot.config.ConfigurationParameters;
import it.unipi.iot.db.DBDriver;
import it.unipi.iot.log.Logger;
import it.unipi.iot.model.FloatLevelSample;
import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapHandler;
import org.eclipse.californium.core.CoapObserveRelation;
import org.eclipse.californium.core.CoapResponse;

import java.util.concurrent.atomic.AtomicInteger;

public class FloatSensor {
    private CoapClient clientPresenceSensor;
    private CoapObserveRelation observePresence;

    private Gson parser;
    private Logger logger;
    private boolean lowLevel = false;

    FloatSensor(){
        ConfigurationParameters configurationParameters = ConfigurationParameters.getInstance();
        parser = new Gson();
        logger = Logger.getInstance();
    }
    public void registerFloatSensor(String ip) {
        System.out.print("\n[REGISTRATION] The presence sensor [" + ip + "] is now registered\n>");
        clientPresenceSensor = new CoapClient("coap://[" + ip + "]/float/level");

        observePresence = clientPresenceSensor.observe(new CoapHandler() {
            @Override
            public void onLoad(CoapResponse coapResponse) {
                String responseString = new String(coapResponse.getPayload());
                try {
                    FloatLevelSample floatSample = parser.fromJson(responseString, FloatLevelSample.class);
                    // TODO
                    //DBDriver.getInstance().insertFloatLevelSample(floatSample);
                    FloatLevelSample.set(floatSample.getLowLevel());
                } catch(Exception e) {
                    System.out.print("\n[ERROR] The presence sensor gave non-significant data\n>");
                }

                if(numberOfPeople.get() > 0 && !lightOn) {
                    if(light != null) {
                        logger.logPresence("There are people in the sauna, the light is switched ON");
                        light.lightSwitch(true);
                        lightOn = true;
                    }
                }

                if(numberOfPeople.get() == 0 && lightOn) {
                    if(light != null) {
                        logger.logPresence("The sauna is empty, the light is switched OFF");
                        light.lightSwitch(false);
                        lightOn = false;
                    }
                }

                if(!full && numberOfPeople.get() >= maxNumberOfPeople.get()) {
                    logger.logPresence("The sauna is FULL, it is not possible to enter");
                    full = true;
                }

                if(full && numberOfPeople.get() != maxNumberOfPeople.get()) {
                    logger.logPresence("The sauna is no longer full, you can enter now");
                    full = false;
                }
            }

            @Override
            public void onError() {
                System.err.print("\n[ERROR] Presence sensor " + clientPresenceSensor.getURI() + "]\n>");
            }
        });
    }

}

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


public class FloatSensor {
    private CoapClient clientPresenceSensor;
    private CoapObserveRelation observePresence;
    private boolean floatLevelsensor;
    private Gson parser;
    private Logger logger;
    private boolean lowLevel = false;

    public FloatSensor(){
        floatLevelsensor = true;
        ConfigurationParameters configurationParameters = ConfigurationParameters.getInstance();
        parser = new Gson();
        logger = Logger.getInstance();
    }
    public void registerFloatLevelSensor(String ip) {
        System.out.print("\n[REGISTRATION] The float level sensor [" + ip + "] is now registered\n>");
        clientPresenceSensor = new CoapClient("coap://[" + ip + "]/float/level");

        observePresence = clientPresenceSensor.observe(new CoapHandler() {
            @Override
            public void onLoad(CoapResponse coapResponse) {
                String responseString = new String(coapResponse.getPayload());
                try {
                    FloatLevelSample floatSample = parser.fromJson(responseString, FloatLevelSample.class);
                    // TODO
                    DBDriver.getInstance().insertFloatLevelSample(floatSample);
                    floatLevelsensor = floatSample.getLowLevel();
                } catch(Exception e) {
                    System.out.print("\n[ERROR] The float level sensor gave non-significant data\n>");
                }

                if(floatLevelsensor) {
                    logger.logFloatSensor("There is water, so there is no need to refill");

                }else{
                    logger.logFloatSensor("The water is finished. **REFILL**");
                    // da refillare
                }

            }
            @Override
            public void onError() {
                System.err.print("\n[ERROR] Presence sensor " + clientPresenceSensor.getURI() + "]\n>");
            }
        });
    }

    public CoapClient gimmeÐat() {
        return clientPresenceSensor;
    }
    public void unregisterFloatLevelSensor(String ip) {
    }
}

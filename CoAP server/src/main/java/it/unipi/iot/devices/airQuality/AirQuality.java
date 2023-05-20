package it.unipi.iot.devices.airQuality;

import com.google.gson.Gson;
import it.unipi.iot.config.ConfigurationParameters;
import it.unipi.iot.db.DBDriver;
import it.unipi.iot.log.Logger;
import it.unipi.iot.model.AirQualitySample;
import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapHandler;
import org.eclipse.californium.core.CoapObserveRelation;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.MediaTypeRegistry;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class AirQuality {
    private List<CoapClient> clientCO2SensorList = new ArrayList<>();
    private List<CoapClient> clientVentilationSystemList = new ArrayList<>();
    private List<CoapObserveRelation> observeCO2List = new ArrayList<>();

    private AtomicInteger co2Level;
    private Map<Integer, AirQualitySample> lastAirQualitySamples;
    private AtomicInteger upperBound;
    private boolean ventilationOn = false;

    private Gson parser;
    private Logger logger;

    public AirQuality() {
        co2Level = new AtomicInteger(300);
        lastAirQualitySamples = new HashMap<>();
        ConfigurationParameters configurationParameters = ConfigurationParameters.getInstance();
        upperBound = new AtomicInteger(configurationParameters.getUpperBoundAirQuality());
        parser = new Gson();
        logger = Logger.getInstance();
    }

    public void registerAirQuality(String ip) {
        System.out.print("\n[REGISTRATION] The Air Quality system: [" + ip + "] is now registered\n>");
        CoapClient newClientCO2Sensor = new CoapClient("coap://[" + ip + "]/air_quality/co2");
        clientCO2SensorList.add(newClientCO2Sensor);

        CoapClient newClientVentilationSystem = new CoapClient("coap://[" + ip + "]/air_quality/ventilation");
        clientVentilationSystemList.add(newClientVentilationSystem);

        CoapObserveRelation newObserveCO2 = newClientCO2Sensor.observe(new CoapHandler() {
            @Override
            public void onLoad(CoapResponse coapResponse) {
                String responseString = new String(coapResponse.getPayload());
                try {
                    AirQualitySample airQualitySample = parser.fromJson(responseString, AirQualitySample.class);
                    DBDriver.getInstance().insertAirQualitySample(airQualitySample);
                    airQualitySample.setTimestamp(new Timestamp(System.currentTimeMillis()));
                    lastAirQualitySamples.put(airQualitySample.getNode(), airQualitySample);
                    // remove old samples from the lastAirQualitySamples map
                    lastAirQualitySamples.entrySet().removeIf(entry -> !entry.getValue().isValid());
                    computeAverage();
                } catch (Exception e) {
                    System.out.print("\n[ERROR] The CO2 sensor gave non-significant data\n>");
                }

                if(!ventilationOn && co2Level.get() > upperBound.get()) {
                    logger.logAirQuality("CO2 level is HIGH: " + co2Level.get() + " ppm, the ventilation system is switched ON");
                    for (CoapClient clientVentilationSystem: clientVentilationSystemList) {
                        ventilationSystemSwitch(clientVentilationSystem,true);
                    }
                    ventilationOn = true;
                }

                // We don't turn off the ventilation as soon as the value is lower than the upper bound,
                // but we leave a margin so that we don't have to turn on the system again right away
                else if (ventilationOn && co2Level.get()  < upperBound.get()*0.7) {
                    logger.logAirQuality("CO2 level is now fine: " + co2Level.get() + " ppm. Switch OFF the ventilation system");
                    for (CoapClient clientVentilationSystem: clientVentilationSystemList) {
                        ventilationSystemSwitch(clientVentilationSystem,false);
                    }
                    ventilationOn = false;
                }

                else
                {
                    logger.logAirQuality("C02 level is fine: " + co2Level.get() + " ppm");
                }
            }

            @Override
            public void onError() {
                System.err.print("\n[ERROR] Air Quality " + newClientCO2Sensor.getURI() + "]\n>");
            }
        });

        observeCO2List.add(newObserveCO2);
    }

    private void computeAverage() {
        int size = lastAirQualitySamples.size();
        int sum = lastAirQualitySamples.values().stream()
                .map(AirQualitySample::getConcentration)
                .reduce(0, Integer::sum);

        co2Level.set(sum/size);
    }

    public void unregisterAirQuality(String ip) {
        for (int i = 0; i < clientCO2SensorList.size(); i++) {
            if (clientCO2SensorList.get(i).getURI().equals(ip)) {
                clientCO2SensorList.remove(i);
                clientVentilationSystemList.remove(i);
                observeCO2List.get(i).proactiveCancel();
                observeCO2List.remove(i);
            }
        }
    }

    public int getCO2Level() {
        return co2Level.get();
    }

    public void setUpperBound(int upperBound) {
        this.upperBound.set(upperBound);
    }

    private void ventilationSystemSwitch(CoapClient clientVentilationSystem, boolean on) {
        if(clientVentilationSystem == null)
            return;

        String msg = "mode=" + (on ? "ON" : "OFF");
        clientVentilationSystem.put(new CoapHandler() {
            @Override
            public void onLoad(CoapResponse coapResponse) {
                if(coapResponse != null) {
                    if(!coapResponse.isSuccess())
                        System.out.print("\n[ERROR] Ventilation System: PUT request unsuccessful\n>");
                }
            }

            @Override
            public void onError() {
                System.err.print("\n[ERROR] Ventilation System " + clientVentilationSystem.getURI() + "]\n>");
            }
        }, msg, MediaTypeRegistry.TEXT_PLAIN);
    }
}

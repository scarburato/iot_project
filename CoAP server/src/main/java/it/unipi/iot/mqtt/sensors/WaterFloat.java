package it.unipi.iot.mqtt.sensors;

import it.unipi.iot.db.DBDriver;
import it.unipi.iot.model.FloatLevelSample;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

public class WaterFloat {
    public static final String FLOAT_TOPIC = "float";
    public static final String PUMP_TOPIC = "pump";
    public static final String INC = "INC";
    public static final String DEC = "DEC";
    public static final String OFF = "OFF";

    private final Map<Integer, FloatLevelSample> lastFloatSamples;
    private float lowerBoundTemperature;
    private float upperBoundTemperature;
    private String lastCommand;

    public WaterFloat(){
        lastFloatSamples = new HashMap<>();
        lastCommand = OFF;
    }
    /**
     * Function that adds a new temperature sample
     * @param floatLevelSample    temperature sample received
     */
    public void addFloatSample (FloatLevelSample floatLevelSample)
    {
        System.out.println("Ora dovrei inserire " + floatLevelSample.getLowLevel() + "level nel db, salto...");
        floatLevelSample.setTimestamp(new Timestamp(System.currentTimeMillis()));
        lastFloatSamples.put(floatLevelSample.getNode(), floatLevelSample);
        DBDriver.getInstance().insertFloatLevelSample(floatLevelSample);

        // remove old samples from the lastTemperatureSamples map
        lastFloatSamples.entrySet().removeIf(entry -> !entry.getValue().isValid());
    }

    public String getLastCommand() {
        return lastCommand;
    }
}

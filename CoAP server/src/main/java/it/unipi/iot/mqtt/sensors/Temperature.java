package it.unipi.iot.mqtt.sensors;

import it.unipi.iot.config.ConfigurationParameters;
import it.unipi.iot.db.DBDriver;
import it.unipi.iot.model.TemperatureSample;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

public class Temperature {
    public final String TEMPERATURE_TOPIC = "temperature";
    public final String AC_TOPIC = "AC";
    public final String INC = "INC";
    public final String DEC = "DEC";
    public final String OFF = "OFF";

    private Map<Integer, TemperatureSample> lastTemperatureSamples;
    private float lowerBoundTemperature;
    private float upperBoundTemperature;
    private String lastCommand;

    public Temperature ()
    {
        lastTemperatureSamples = new HashMap<>();
        //ConfigurationParameters configurationParameters = ConfigurationParameters.getInstance();
        lowerBoundTemperature = 70;// = configurationParameters.getLowerBoundTemperature();
        upperBoundTemperature = 100; //configuratioupperBoundTemperaturenParameters.getUpperBoundTemperature();
        lastCommand = OFF;
    }

    /**
     * Function that adds a new temperature sample
     * @param temperatureSample    temperature sample received
     */
    public void addTemperatureSample (TemperatureSample temperatureSample)
    {
        System.out.println("Ora dovrei inserire " + temperatureSample.getTemperature() + "°C nel db, salto...");
        //temperatureSample.setTimestamp(new Timestamp(System.currentTimeMillis()));
        //lastTemperatureSamples.put(temperatureSample.getNode(), temperatureSample);
        //DBDriver.getInstance().insertTemperatureSample(temperatureSample);

        // remove old samples from the lastTemperatureSamples map
        //lastTemperatureSamples.entrySet().removeIf(entry -> !entry.getValue().isValid());
    }

    /**
     * Function that computes the average of the last temperature samples received
     * @return  the computed average
     */
    public float getAverage ()
    {
        int howMany = lastTemperatureSamples.size();
        float sum = lastTemperatureSamples.values().stream()
                .map(TemperatureSample::getTemperature) // take only the temperature
                .reduce((float) 0, Float::sum); // sum the values
        return sum / howMany;
    }

    /**
     * Function used to compute the mid range of the interval [lowerBoundTemperature, upperBoundTemperature]
     * @return the mid range of the interval
     */
    public float getMidRange ()
    {
        return (lowerBoundTemperature + upperBoundTemperature) / 2;
    }

    public float getLowerBoundTemperature() {
        return lowerBoundTemperature;
    }

    public void setLowerBoundTemperature(float lowerBoundTemperature) {
        this.lowerBoundTemperature = lowerBoundTemperature;
    }

    public float getUpperBoundTemperature() {
        return upperBoundTemperature;
    }

    public void setUpperBoundTemperature(float upperBoundTemperature) {
        this.upperBoundTemperature = upperBoundTemperature;
    }

    public String getLastCommand() {
        return lastCommand;
    }

    public void setLastCommand(String lastCommand) {
        this.lastCommand = lastCommand;
    }
}

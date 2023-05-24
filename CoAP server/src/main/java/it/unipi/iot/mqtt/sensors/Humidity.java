package it.unipi.iot.mqtt.sensors;

import it.unipi.iot.config.ConfigurationParameters;
import it.unipi.iot.db.DBDriver;
import it.unipi.iot.model.HumiditySample;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

public class Humidity {
    public final String HUMIDITY_TOPIC = "humidity";
    public final String HUMIDIFIER_TOPIC = "humidifier";
    public final String INC = "INC";
    public final String DEC = "DEC";
    public final String OFF = "OFF";

    private Map<Integer, HumiditySample> lastHumiditySamples;
    private float lowerBoundHumidity;
    private float upperBoundHumidity;
    private String lastCommand;

    public Humidity() {
        lastHumiditySamples = new HashMap<>();
        //ConfigurationParameters configurationParameters = ConfigurationParameters.getInstance();
        lowerBoundHumidity = 40; // = configurationParameters.getLowerBoundHumidity();
        upperBoundHumidity = 60; //configurationParameters.getUpperBoundHumidity();
        lastCommand = OFF;
    }

    /**
     * Function that adds a new humidity sample
     * @param humiditySample    humidity sample received
     */
    public void addHumiditySample (HumiditySample humiditySample)
    {
        humiditySample.setTimestamp(new Timestamp(System.currentTimeMillis()));
        lastHumiditySamples.put(humiditySample.getNode(), humiditySample);
        DBDriver.getInstance().insertHumiditySample(humiditySample);

        // remove old samples from the lastHumiditySamples map
        lastHumiditySamples.entrySet().removeIf(entry -> !entry.getValue().isValid());
    }

    /**
     * Function that computes the average of the last humidity samples received
     * @return  the computed average
     */
    public float getAverage ()
    {
        int howMany = lastHumiditySamples.size();
        float sum = lastHumiditySamples.values().stream()
                .map(HumiditySample::getHumidity) // take only the humidity
                .reduce((float) 0, Float::sum); // sum the values
        return sum / howMany;
    }

    /**
     * Function used to compute the mid range of the interval [lowerBoundHumidity, upperBoundHumidity]
     * @return the mid range of the interval
     */
    public float getMidRange ()
    {
        return (lowerBoundHumidity + upperBoundHumidity) / 2;
    }

    public float getLowerBoundHumidity() {
        return lowerBoundHumidity;
    }

    public float getUpperBoundHumidity() {
        return upperBoundHumidity;
    }

    public void setLowerBoundHumidity(float lowerBoundHumidity) {
        this.lowerBoundHumidity = lowerBoundHumidity;
    }

    public void setUpperBoundHumidity(float upperBoundHumidity) {
        this.upperBoundHumidity = upperBoundHumidity;
    }

    public String getLastCommand() {
        return lastCommand;
    }

    public void setLastCommand(String lastCommand) {
        this.lastCommand = lastCommand;
    }
}

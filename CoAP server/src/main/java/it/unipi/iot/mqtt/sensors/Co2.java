package it.unipi.iot.mqtt.sensors;

import it.unipi.iot.db.DBDriver;
import it.unipi.iot.model.AirQualitySample;
import it.unipi.iot.model.TemperatureSample;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

public class Co2 {
    public static final String CO2_TOPIC = "co2";
    public static final String VENTILATION_TOPIC = "ventilation";
    public static final String INC = "INC";
    public static final String DEC = "DEC";
    public static final String OFF = "OFF";

    private final Map<Integer, AirQualitySample> lastCo2Samples;
    private float lowerBoundCo2;
    private float upperBoundCo2;
    private String lastCommand;

    public Co2(){
        lastCo2Samples = new HashMap<>();
        //ConfigurationParameters configurationParameters = ConfigurationParameters.getInstance();
        // TO DO DA SISTEMARE CON VALORI PAPABILI
        lowerBoundCo2 = 70;// = configurationParameters.getLowerBoundTemperature();
        upperBoundCo2 = 100; //configuratioupperBoundTemperaturenParameters.getUpperBoundTemperature();
        lastCommand = OFF;
    }

    /**
     * Function that adds a new co2 sample
     * @param co2Sample    co2 sample received
     */
    public void addTemperatureSample (AirQualitySample co2Sample)
    {
        System.out.println("Ora dovrei inserire " + co2Sample.getConcentration() + "pbm nel db, salto...");
        co2Sample.setTimestamp(new Timestamp(System.currentTimeMillis()));
        lastCo2Samples.put(co2Sample.getNode(), co2Sample);
        DBDriver.getInstance().insertAirQualitySample(co2Sample);

        // remove old samples from the lastTemperatureSamples map
        lastCo2Samples.entrySet().removeIf(entry -> !entry.getValue().isValid());
    }

    /**
     * Function that computes the average of the last temperature samples received
     * @return  the computed average
     */
    public float getAverage ()
    {
        int howMany = lastCo2Samples.size();
        float sum = lastCo2Samples.values().stream()
                .map(AirQualitySample::getConcentration) // take only the concentration
                .reduce( 0, Integer::sum); // sum the values
        return sum / howMany;
    }

    /**
     * Function used to compute the mid range of the interval [lowerBoundTemperature, upperBoundTemperature]
     * @return the mid range of the interval
     */
    public float getMidRange ()
    {
        return (lowerBoundCo2 + upperBoundCo2) / 2;
    }

    public float getLowerBoundTemperature() {
        return lowerBoundCo2;
    }

    public void setLowerBoundTemperature(float lowerBoundCo2) {
        this.lowerBoundCo2 = lowerBoundCo2;
    }

    public float getUpperBoundTemperature() {
        return upperBoundCo2;
    }

    public void setUpperBoundTemperature(float upperBoundCo2) {
        this.upperBoundCo2 = upperBoundCo2;
    }

    public String getLastCommand() {
        return lastCommand;
    }

    public void setLastCommand(String lastCommand) {
        this.lastCommand = lastCommand;
    }
}

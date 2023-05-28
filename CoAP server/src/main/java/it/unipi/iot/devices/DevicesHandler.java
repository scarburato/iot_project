package it.unipi.iot.devices;

import it.unipi.iot.devices.airQuality.AirQuality;
import it.unipi.iot.devices.floatSensor.FloatSensor;
import it.unipi.iot.devices.light.Light;

public class DevicesHandler {
    private AirQuality airQuality = new AirQuality();
    private Light light = new Light();
    private FloatSensor floatSensor = new FloatSensor();
    private static DevicesHandler instance = null;

    private DevicesHandler() {
        //presenceSensor.addLight(light);
    }
    public static DevicesHandler getInstance() {
        if(instance == null)
            instance = new DevicesHandler();

        return instance;
    }

    /*      REGISTER AND UNREGISTER DEVICES     */
    public void registerAirQuality(String ip) {
        airQuality.registerAirQuality(ip);
    }

    public void registerFloatSensor(String ip){
        floatSensor.registerFloatLevelSensor(ip);
    }
    public void registerLight(String ip) {
        light.registerLight(ip);
    }

    public void unregisterAirQuality(String ip) {
        airQuality.unregisterAirQuality(ip);
    }

    public void unregisterLight(String ip) {
        light.unregisterLight(ip);
    }
    public void unregisterFloat(String ip){
        floatSensor.unregisterFloatLevelSensor(ip);
    }

    /*      GET MEASURES FROM SENSORS     */
    /* DA LEVARE: Spostato in MQTT
    public int getCO2Level() {
        return airQuality.getCO2Level();
    }
    */
    /*      SET     */
    public void setLightColor(Light.LightColor lightColor) {
        light.changeLightColor(lightColor);
    }

    public void setCO2UpperBound(int upperBound) {
        airQuality.setUpperBound(upperBound);
    }


}

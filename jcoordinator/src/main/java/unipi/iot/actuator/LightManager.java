package unipi.iot.actuator;

import java.util.ArrayList;
import java.util.List;

public class LightManager implements ActuatorManager{
    public List<Light> lights = new ArrayList<>();
    @Override
    public Actuator getAssociatedSensor(long id) {
        return null;
    }

    @Override
    public void registerNewActuator(long sensorId, String ip) {
        lights.add(new Light(ip));
    }

    @Override
    public void deleteActuator(long sensorId) {
        // no
    }
}

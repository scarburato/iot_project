package unipi.iot.actuator;

import java.util.HashMap;
import java.util.Map;

public class PumpManager implements ActuatorManager{
    private final Map<Long, Actuator> sensorsToActuators = new HashMap<Long, Actuator>();

    public Actuator getAssociatedSensor(long id) {
        return sensorsToActuators.get(id);
    }

    public void registerNewActuator(long sensorId, String ip) {
        sensorsToActuators.put(sensorId, new Pump(ip));
    }

    public void deleteActuator(long sensorID) {
        sensorsToActuators.remove(sensorID);
    }
}

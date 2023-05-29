package unipi.iot.actuator;

import java.util.HashMap;
import java.util.Map;

public class AcManager implements ActuatorManager{
    private final Map<Long, Actuator> sensorsToActuators = new HashMap<Long, Actuator>();

    public Actuator getAssociatedSensor(long id) {
        return sensorsToActuators.get(id);
    }

    public void registerNewActuator(long sensorId, String ip) {
        sensorsToActuators.put(sensorId, new Ac(ip));
    }

    public void deleteActuator(long sensorID) {
        sensorsToActuators.remove(sensorID);
    }
}

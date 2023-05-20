package it.unipi.iot.log;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

public class Logger {
    private static Logger instance;
    private static java.util.logging.Logger logger;

    public static Logger getInstance()
    {
        if(instance == null)
            instance = new Logger();

        return instance;
    }

    private Logger()
    {
        logger = java.util.logging.Logger.getLogger(Logger.class.getName());
        try {
            FileHandler fileHandler = new FileHandler("./info.log");
            logger.addHandler(fileHandler);
            fileHandler.setFormatter(new Formatter() {
                @Override
                public String format(LogRecord logRecord) {
                    return logRecord.getMessage() + "\n"; // only the message of the log
                }
            });
            logger.setUseParentHandlers(false);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * This function adds a new log in the file
     * @param topic     topic of the message
     * @param message   message to be shown
     */
    public void log (String topic, String message)
    {
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        logger.info("[" + topic + " - " + timestamp + "] " + message);
    }

    public void logTemperature (String message)
    {
        log("TEMPERATURE", message);
    }

    public void logHumidity (String message)
    {
        log("HUMIDITY", message);
    }

    public void logAirQuality (String message)
    {
        log("AIR QUALITY", message);
    }

    public void logFloatSensor (String message)
    {
        log("FLOAT LEVEL SENSOR", message);
    }

    public void logInfo (String message)
    {
        log("INFO", message);
    }
}

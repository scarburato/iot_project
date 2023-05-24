package it.unipi.iot.db;

import it.unipi.iot.config.ConfigurationParameters;
import it.unipi.iot.model.AirQualitySample;
import it.unipi.iot.model.HumiditySample;
import it.unipi.iot.model.TemperatureSample;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class DBDriver {
    private static DBDriver instance = null;

    private static String databaseIp;
    private static int databasePort;
    private static String databaseUsername;
    private static String databasePassword;
    private static String databaseName;

    public static DBDriver getInstance() {
        if(instance == null)
            instance = new DBDriver();

        return instance;
    }

    private DBDriver() {
        ConfigurationParameters configurationParameters = ConfigurationParameters.getInstance();
        databaseIp = configurationParameters.getDatabaseIp();
        databasePort = configurationParameters.getDatabasePort();
        databaseUsername = configurationParameters.getDatabaseUsername();
        databasePassword = configurationParameters.getDatabasePassword();
        databaseName = configurationParameters.getDatabaseName();
    }

    /**
     * @return the JDBC connection to be used to communicate with MySQL Database.
     *
     * @throws SQLException  in case the connection to the database fails.
     */
    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection("jdbc:mysql://"+ databaseIp + ":" + databasePort +
                        "/" + databaseName + "?zeroDateTimeBehavior=CONVERT_TO_NULL&serverTimezone=CET",
                databaseUsername, databasePassword);
    }

    /**
     * Insert the new sample received by the Air Quality sensor
     * @param airQualitySample  sample to be received
     */
    public void insertAirQualitySample(AirQualitySample airQualitySample) {
        try (
                Connection connection = getConnection();
                PreparedStatement statement = connection.prepareStatement("INSERT INTO air_quality (node, concentration) VALUES (?, ?)")
        )
        {
            statement.setInt(1, airQualitySample.getNode());
            statement.setInt(2, airQualitySample.getConcentration());
            statement.executeUpdate();
        }
        catch (final SQLException e)
        {
            e.printStackTrace();
        }
    }

    /**
     * Insert a new sample received by the humidity sensor
     * @param humiditySample    sample to be inserted
     */
    public void insertHumiditySample (HumiditySample humiditySample) {
        try (
                Connection connection = getConnection();
                PreparedStatement statement = connection.prepareStatement("INSERT INTO humidity (node, percentage) VALUES (?, ?)")
        )
        {
            statement.setInt(1, humiditySample.getNode());
            statement.setFloat(2, humiditySample.getHumidity());
            statement.executeUpdate();
        }
        catch (final SQLException e)
        {
            e.printStackTrace();
        }
    }

    /**
     * Insert a new sample received by the temperature sensor
     * @param temperatureSample     sample to be inserted
     */
    public void insertTemperatureSample(TemperatureSample temperatureSample) {
        try (
                Connection connection = getConnection();
                PreparedStatement statement = connection.prepareStatement("INSERT INTO temperature (`sensor's id`, degrees) VALUES (?, ?)")
        )
        {
            statement.setInt(1, temperatureSample.getNode());
            statement.setFloat(2, temperatureSample.getTemperature());
            statement.executeUpdate();
        }
        catch (final SQLException e)
        {
            e.printStackTrace();
        }
    }
}

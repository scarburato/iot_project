package unipi.iot;

import unipi.iot.actuator.Actuator;
import unipi.iot.sensor.Co2Message;
import unipi.iot.sensor.FloatLevelMessage;
import unipi.iot.sensor.HumidityMessage;
import unipi.iot.sensor.TemperatureMessage;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class DBDriver {
    private static DBDriver instance = null;
    private static final String databaseIp;
   // private static int databasePort;
    private static final String databaseUsername;
    private static final String databasePassword;
    private static final String databaseName;

    public static DBDriver getInstance() {
        if(instance == null)
            instance = new DBDriver();

        return instance;
    }

    static {
        databaseIp = "127.0.0.1";
       // databasePort = ;
        databaseUsername = "root";
        databasePassword = "root";
        databaseName = "smart_zoo";
    }

    /**
     * @return the JDBC connection to be used to communicate with MySQL Database.
     *
     * @throws SQLException  in case the connection to the database fails.
     */
    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection("jdbc:mysql://"+ databaseIp +
                        "/" + databaseName + "?zeroDateTimeBehavior=CONVERT_TO_NULL&serverTimezone=CET",
                databaseUsername, databasePassword);
    }

    public void registerActuator(String ip, String type) {
        try (
                Connection connection = getConnection();
                PreparedStatement statement = connection.prepareStatement(
                        "REPLACE INTO `actuator`(`ip`, `type`) VALUES (?, ?)");
        )
        {
            statement.setString(1, ip);
            statement.setString(2, type);
            statement.executeUpdate();
        }
        catch (final SQLException e)
        {
            e.printStackTrace();
            System.err.println("Skipping insert....");
        }
    }

    public void insertCO2Sample(Co2Message m) {
        try (
                Connection connection = getConnection();
                PreparedStatement statement = connection.prepareStatement("INSERT INTO `air quality`(`sensor's id`, concentration) VALUES (?, ?)")
        )
        {
            statement.setLong(1, m.node);
            statement.setDouble(2, m.co2);
            statement.executeUpdate();
        }
        catch (final SQLException e)
        {
            e.printStackTrace();
            System.err.println("Skipping insert....");
        }
    }
    public void insertHumiditySample(HumidityMessage m) {
        try (
                Connection connection = getConnection();
                PreparedStatement statement = connection.prepareStatement("INSERT INTO humidity (`sensor's id`, percentage) VALUES (?, ?)")
        )
        {
            statement.setLong(1, m.node);
            statement.setDouble(2, m.humidity);
            statement.executeUpdate();
        }
        catch (final SQLException e)
        {
            e.printStackTrace();
            System.err.println("Skipping insert....");
        }
    }

    public void insertFloatLevelSample(FloatLevelMessage m) {
        try (
                Connection connection = getConnection();
                PreparedStatement statement = connection.prepareStatement("INSERT INTO `float level`(`sensor's id`, `low level`) VALUES (?, ?)")
        )
        {
            statement.setLong(1, m.node);
            statement.setBoolean(2, m.isLevelLow);
            statement.executeUpdate();
        }
        catch (final SQLException e)
        {
            e.printStackTrace();
            System.err.println("Skipping insert....");
        }
    }

    public void insertTemperatureSample(TemperatureMessage m) {
        try (
                Connection connection = getConnection();
                PreparedStatement statement = connection.prepareStatement("INSERT INTO `air quality`(`sensor's id`, concentration) VALUES (?, ?)")
        )
        {
            statement.setLong(1, m.node);
            statement.setDouble(2, m.temperature);
            statement.executeUpdate();
        }
        catch (final SQLException e)
        {
            e.printStackTrace();
            System.err.println("Skipping insert....");
        }
    }

}

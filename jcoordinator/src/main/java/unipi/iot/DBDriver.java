package unipi.iot;

import unipi.iot.sensor.Co2Message;

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
}

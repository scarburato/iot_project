package unipi.iot;

import unipi.iot.sensor.HumidityManager;
import unipi.iot.sensor.HumidityMessage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.SocketException;

public class UserInterface {
    public static void main(String[] args) throws SocketException {
        Coordinator coordinator = new Coordinator();
        HumidityManager humidityManager = (HumidityManager) coordinator.getTopicManager("humidity");



        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
        String command;
        String[] parts;
        printCommandSelection();
        while (true) {
            System.out.print("> ");
            try {
                command = bufferedReader.readLine();
                parts = command.split(" ");

                switch (parts[0]) {
                    case "!help":
                        printHelp(parts);
                        break;
                    case "!get_humidity":
                        double avg = humidityManager.getAvg();
                        break;
                    case "!set_humidity":

                        break;
                    case "!get_air_quality":

                        break;
                    case "!set_air_quality":

                        break;
                    case "!set_light_color":

                        break;
                    case "!set_water_level_high":

                        break;
                    case "!set_water_level_low":

                        break;
                    case "!exit":
                        System.out.println("Hi!");
                        System.exit(0);
                        break;
                    default:
                        System.out.println("Command not recognized, try again\n");
                        break;
                }
                printCommandSelection();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    private static void printCommandSelection(){
       System.out.println(
                       "<><><><><><><> SMART ZOO CONTROLLER <><><><><><><>" +
                       "Choose the command:" +
                       "!help <command>    - Shows the details of a command \n " +
                       "!get_humidity \n" +
                       "!set_humidity \n" +
                       "!get_air_quality \n" +
                       "!set_air_quality \n" +
                       "!set_light_color <color> \n" +
                       "!set_water_level_high \n" +
                       "!set_water_level_low \n" +
                       "!exit \n"+
                        "");
    }

    private static void printHelp(String[] parts) {
        if(parts.length != 2) {
            System.out.println("Incorrect use of the command. Please use !help <command>\n");
        } else {
            switch (parts[1]) {
                case "set_water_level_high":
                case "!set_water_level_high":
                    System.out.println("!set_water_level_high set the water level to high.");
                case "set_water_level_low":
                case "!set_water_level_low":
                    System.out.println("!set_water_level_high set the water level to low.");
                case "!help":
                case "help":
                    System.out.println("!help shows the details of the command passed as parameter.\n");
                    break;
                case "!get_humidity":
                case "get_humidity":
                    System.out.println("!get_humidity allows to retrieve the percentage value of humidity in the air inside the Zoo.\n");
                    break;
                case "!set_humidity":
                case "set_humidity":
                    System.out.println("!set_humidity allows you to set the range within which the humidity level should be found inside the sauna.\n" +
                            "Two parameters are required: the lower and the upper bounds.\n");
                    break;
                case "!get_air_quality":
                case "get_air_quality":
                    System.out.println("!get_air_quality allows you to retrieve the CO2 level inside the sauna, expressed in parts per million (ppm).\n");
                    break;
                case "!set_air_quality":
                case "set_air_quality":
                    System.out.println("!set_air_quality allows you to set the maximum level of CO2 that can be inside the sauna.\n" +
                            "One parameter is required: the upper bound.\n");
                    break;
                case "!set_light_color":
                case "set_light_color":
                    System.out.println("!set_color allows you to set the color of the light inside the sauna.\n" +
                            "A parameter is required, i.e. the color, which can take three values: GREEN, YELLOW or RED. \n");
                    break;
                case "!exit":
                case "exit":
                    System.out.println("!exit allows you to terminate the program.\n");
                    break;
                default:
                    System.out.println("Command not recognized, try again\n");
                    break;
            }
        }
    }
}

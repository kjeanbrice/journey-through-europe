/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package journeythrougheurope.file;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.geometry.Point2D;
import javafx.scene.control.TextField;
import journeythrougheurope.application.Main.JourneyThroughEuropePropertyType;
import journeythrougheurope.botalgorithm.Edge;
import journeythrougheurope.game.JourneyThroughEuropeCity;
import journeythrougheurope.reader.XMLCityReader;
import journeythrougheurope.ui.JourneyThroughEuropeUI;
import journeythrougheurope.ui.PlayerManager;
import properties_manager.PropertiesManager;

/**
 *
 * @author Karl
 */
public class JourneyThroughEuropeFileLoader {

    private static final String CITY_DATA_FILE = "data/cities.txt";
    private static final String CITY_NEIGHBORS_FILE = "data/cityneighbors.xml";
    private static final double CONVERSION_FACTOR = .60;
    private HashMap<String, JourneyThroughEuropeCity> cityHashMap;
    private static final double GRID_1_WIDTH = 2000;
    private static final double GRID_1_HEIGHT = 2569;
    private static final double GRID_2_HEIGHT = 2579;
    private static final double GRID_3_WIDTH = 1973;

    public static String loadTextFile(String textFile) throws IOException {
        // ADD THE PATH TO THE FILE
        PropertiesManager props = PropertiesManager.getPropertiesManager();
        textFile = props.getProperty(JourneyThroughEuropePropertyType.DATA_PATH) + textFile;

        // WE'LL ADD ALL THE CONTENTS OF THE TEXT FILE TO THIS STRING
        String textToReturn = "";

        // OPEN A STREAM TO READ THE TEXT FILE
        FileReader fr = new FileReader(textFile);
        BufferedReader reader = new BufferedReader(fr);

        // READ THE FILE, ONE LINE OF TEXT AT A TIME
        String inputLine = reader.readLine();
        while (inputLine != null) {
            // APPEND EACH LINE TO THE STRING
            textToReturn += inputLine + "\n";

            // READ THE NEXT LINE
            inputLine = reader.readLine();
        }

        // RETURN THE TEXT
        return textToReturn;
    }

    public static ArrayList<JourneyThroughEuropeCity> loadMapGridData(int mapGrid, XMLCityReader cities) {
        ArrayList<JourneyThroughEuropeCity> cityData = new ArrayList<>();
        try {
            Scanner fileScan = new Scanner(new File(CITY_DATA_FILE));

            while (fileScan.hasNext()) {

                String cityName = fileScan.next();
                String cardColor = fileScan.next();
                int gridLocation = fileScan.nextInt();
                double gridX = 0;
                double gridY = 0;
                switch (gridLocation) {
                    case 1:
                        gridX = fileScan.nextDouble();
                        gridY = fileScan.nextDouble();
                        break;
                    case 2:
                        gridX = fileScan.nextDouble() + GRID_1_WIDTH;
                        gridY = fileScan.nextDouble();
                        break;
                    case 3:
                        gridX = fileScan.nextDouble();
                        gridY = fileScan.nextDouble() + GRID_1_HEIGHT;
                        break;
                    case 4:
                        gridX = fileScan.nextDouble() + GRID_3_WIDTH;
                        gridY = fileScan.nextDouble() + GRID_2_HEIGHT;
                        break;
                }

                if (gridLocation == (mapGrid + 1)) {
                    JourneyThroughEuropeCity cityTemp = cities.getCity(cityName);
                    cityTemp.setCardColor(cardColor);
                    cityTemp.setGridLocation(gridLocation);
                    cityTemp.setGridX(gridX * CONVERSION_FACTOR);
                    cityTemp.setGridY(gridY * CONVERSION_FACTOR);
                    cityTemp.setFront(cityName);

                    int size = cityTemp.getNeighboringLandCities().size() + cityTemp.getNeighboringSeaCities().size();
                    Edge[] edgeList = new Edge[size];
                    for (int i = 0; i < cityTemp.getNeighboringLandCities().size(); i++) {
                        edgeList[i] = new Edge(cities.getCity(cityTemp.getNeighboringLandCities().get(i)).getVertex(), 1);
                    }

                    int landSize = cityTemp.getNeighboringLandCities().size();
                    for (int i = 0; i < cityTemp.getNeighboringSeaCities().size(); i++) {
                        edgeList[i + landSize] = new Edge(cities.getCity(cityTemp.getNeighboringSeaCities().get(i)).getVertex(), 2);
                    }

                    cityTemp.setVertexAdjacencies(edgeList);
                    cityData.add(cityTemp);

                    System.out.println(cities.getCity(cityName).toString() + "\n");

                }
            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(JourneyThroughEuropeFileLoader.class.getName()).log(Level.SEVERE, null, ex);
        }

        return cityData;
    }

    public static ArrayList<PlayerManager> loadFile(JourneyThroughEuropeUI ui) {
        File fileToOpen = new File("data/game.dat");
        //String fileName = fileToOpen.getPath();
        try {
            if (fileToOpen.exists()) {
                // LET'S USE A FAST LOADING TECHNIQUE. WE'LL LOAD ALL OF THE
                // BYTES AT ONCE INTO A BYTE ARRAY, AND THEN PICK THAT APART.
                // THIS IS FAST BECAUSE IT ONLY HAS TO DO FILE READING ONCE
                byte[] bytes = new byte[Long.valueOf(fileToOpen.length()).intValue()];
                ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
                FileInputStream fis = new FileInputStream(fileToOpen);
                BufferedInputStream bis = new BufferedInputStream(fis);

                // HERE IT IS, THE ONLY READY REQUEST WE NEED
                bis.read(bytes);
                bis.close();

                // NOW WE NEED TO LOAD THE DATA FROM THE BYTE ARRAY
                DataInputStream dis = new DataInputStream(bais);
                
                ArrayList<PlayerManager> players = new ArrayList<PlayerManager>();
                int numPlayers = dis.readInt();
                for (int i = 0; i < numPlayers; i++) {
                    
                    int playerCardsLength = dis.readInt();
                    int playerGameHistoryLength = dis.readInt();
                    String playerName = dis.readUTF();
                    String currentCity = dis.readUTF();
                    
                    ArrayList<String> cards = new ArrayList<String>();
                    for(int j = 0; j<playerCardsLength; j++)
                        cards.add(dis.readUTF());
                    
                    ArrayList<String> moveHistory = new ArrayList<String>();
                    for(int j = 0; j<playerGameHistoryLength; j++)
                        moveHistory.add(dis.readUTF());
                    
                    boolean isHuman = dis.readBoolean();
                    
                    PlayerManager temp = new PlayerManager(new TextField(playerName),isHuman);
                    temp.setCurrentCity(currentCity);
                    temp.setCards(cards);
                    temp.setHomeCity(cards.get(0));
                    temp.setMoveHistory(moveHistory);
                    temp.setHomeGridLocation(ui.getGSM().processGetCityRequest(cards.get(0)).getGridLocation());
                    temp.setCurrentGridLocation(ui.getGSM().processGetCityRequest(currentCity).getGridLocation());
                    temp.setCurrentPosition(new Point2D(ui.getGSM().processGetCityRequest(currentCity).getGridX(),
                    ui.getGSM().processGetCityRequest(currentCity).getGridY()));                    
                    players.add(temp);
                    
                    System.out.println(temp.toString());
                }
                return players;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        throw new RuntimeException("No File To Load.");
    }

    public static void saveFile(ArrayList<PlayerManager> players) {
        String fileName = "data/game.dat";
        try {
            FileOutputStream fos = new FileOutputStream(fileName);
            DataOutputStream dos = new DataOutputStream(fos);

            dos.writeInt(players.size());
            for (int i = 0; i < players.size(); i++) {
                dos.writeInt(players.get(i).getCards().size());
                dos.writeInt(players.get(i).getMoveHistory().size());
                dos.writeUTF(players.get(i).getPlayerName());
                dos.writeUTF(players.get(i).getCurrentCity());
                

                ArrayList<String> cards = players.get(i).getCards();
                for (int j = 0; j < cards.size(); j++) {
                    dos.writeUTF(cards.get(j));
                }

                ArrayList<String> moveHistory = players.get(i).getMoveHistory();
                for (int j = 0; j < moveHistory.size(); j++) {
                    dos.writeUTF(moveHistory.get(j));
                }
                dos.writeBoolean(players.get(i).isHuman());
                
               System.out.println("FileLoader: Player Saved");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

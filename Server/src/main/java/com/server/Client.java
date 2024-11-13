package com.server;

import static java.lang.System.out;

import java.io.BufferedReader;
import java.io.Console;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.java_websocket.WebSocket;
import org.java_websocket.exceptions.WebsocketNotConnectedException;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.json.JSONObject;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.InputStream;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;


import javafx.application.Platform;

public class Client {

    public static UtilsWS wsClient;
    public static ArrayList<Element> productes;

    public static void connectToServer(String host, String port){
        String protocol = "ws";
        wsClient = UtilsWS.getSharedInstance(protocol + "://" + host);
    
        wsClient.onMessage(Client::wsMessage);
        wsClient.onError(Client::wsError);
    }
    
        private static void wsMessage(String response) {
            JSONObject msgObj = new JSONObject(response);
            switch (msgObj.getString("type")) {
                case "ping":
                    String pong = msgObj.getString("message");
                    out.println(pong);
                    break;
                case "bounce":
                    String msg = msgObj.getString("message");
                    out.println(msg);
                    break;
                case "products":
                    String products = msgObj.getString("message");
                    Gson gson = new Gson();
                    productes = gson.fromJson(products, new TypeToken<ArrayList<Element>>() {}.getType());
                    String productInfo = printProducts();
                    System.out.println(productInfo);
                    break;
                case "tags":
                    String tags = msgObj.getString("message");
                    System.out.println(tags);
                    break;
            }
        }

    private static String printProducts() {

        StringBuilder output = new StringBuilder();

        for (Element producte : productes) {
            String id = producte.getAttribute("id");
            String tags = producte.getAttribute("tags");
            NodeList nodeList0 = producte.getElementsByTagName("name");
            String name = nodeList0.item(0).getTextContent();
            NodeList nodeList1 = producte.getElementsByTagName("description");
            String description = nodeList1.item(0).getTextContent();
            NodeList nodeList2 = producte.getElementsByTagName("price");
            String price = nodeList2.item(0).getTextContent();
            NodeList nodeList3 = producte.getElementsByTagName("image");
            String image = nodeList3.item(0).getTextContent();


            output.append("Product ID: " + id + " | Name: " + name + " | Description: " + description + " | Price: " + price + " | Image: " + image + " | Tags: [" + tags + "]\n" );
        }

        return output.toString();
    }

    
        private static void wsError(String response) {
            String connectionRefused = "S'ha refusat la connexió";
                Platform.runLater(() -> {
                    out.println(connectionRefused);
                    wsClient = null;
                });
            }
    
        public static void main(String[] args) {
            LineReader reader = LineReaderBuilder.builder().build();
            System.out.println("Client open. Type 'help' to see available commands");
    
            try {
                while (true) {
                    String line = null;
                    try {
                        line = reader.readLine("> ");
                    } catch (UserInterruptException e) {
                        continue;
                    } catch (EndOfFileException e) {
                        break;
                    }
    
                    line = line.trim();
    
                    if (line.equalsIgnoreCase("connect")) {
                        connectToServer("localhost", "3000");
                        out.println("Connection was succesful.");
                    } else if (line.equalsIgnoreCase("products")) {
                        JSONObject message = new JSONObject();
                        message.put("type", "products");
                        message.put("message", "products");

                        wsClient.safeSend(message.toString());
                    } else if (line.equalsIgnoreCase("tags")) {
                        JSONObject message = new JSONObject();
                        message.put("type", "tags");
                        message.put("message", "tags");

                        wsClient.safeSend(message.toString()); 
                    } else if (line.equalsIgnoreCase("exit")) {
                        System.exit(0);
                    } else {
                        JSONObject message = new JSONObject();

                        message.put("message", line);
                        message.put("type", "bounce");
                        wsClient.safeSend(message.toString());
                    }
            }
        } finally {
            System.out.println("Server stopped.");
        }
    }

}


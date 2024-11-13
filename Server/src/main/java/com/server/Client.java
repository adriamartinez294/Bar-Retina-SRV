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

import org.w3c.dom.*;
import org.json.JSONObject;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import java.io.StringReader;
import org.xml.sax.InputSource;

import java.io.File;
import java.io.StringWriter;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.dom.DOMSource;
import org.w3c.dom.Document;


public class Client {

    public static UtilsWS wsClient;
    public static ArrayList<Element> productes = new ArrayList<>();

    public static void connectToServer(String host, String port){
        String protocol = "wss";
        wsClient = UtilsWS.getSharedInstance(protocol + "://" + host + ":" + port);

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
                String xmlString = msgObj.getString("message");

                try {
                    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                    DocumentBuilder builder = factory.newDocumentBuilder();
                    InputSource is = new InputSource(new StringReader(xmlString));
                    Document document = builder.parse(is);

                    NodeList products = document.getElementsByTagName("product");
                    for (int i = 0; i < products.getLength(); i++) {
                        Element product = (Element) products.item(i);
                        productes.add(product);
                    }

                    String print = printProducts();
                    System.out.println(print);
                } catch (Exception e) {
                    e.printStackTrace(); // Manejo de las excepciones
                }
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
        out.println(connectionRefused);
        wsClient = null;
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
                    connectToServer("barretina2.ieti.site", "443");
                    out.println("Connection was succesful.");
                } else if (line.equalsIgnoreCase("products")) {
                    JSONObject message = new JSONObject();
                    message.put("type", "products");
                    message.put("message", "products");

                    wsClient.safeSend(message.toString());
                } else if (line.equalsIgnoreCase("ping")) {
                    JSONObject message = new JSONObject();
                    message.put("type", "ping");
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
            System.out.println("Client Stopped.");
        }
    }
}

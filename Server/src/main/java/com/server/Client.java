package com.server;

import static java.lang.System.out;

import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.json.JSONObject;

import javafx.application.Platform;

public class Client {

    public static UtilsWS wsClient;

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
            }
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
                        connectToServer("barretina2.ieti.cat", "443");
                        out.println("Connection was succesful.");
                    } else if (line.equalsIgnoreCase("ping")) {
                        JSONObject message = new JSONObject();
                        message.put("type", "ping");
                        message.put("message", "ping");

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

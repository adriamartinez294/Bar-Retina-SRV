package com.project;

import static java.lang.System.out;

import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.json.JSONObject;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.paint.Color;

public class Client {

    public static UtilsWS wsClient;

    public void connectToServer(String host, String port){

        Task<Void> connectionTask = new Task<>() {
                @Override
                protected Void call() {
                    try {
                        String protocol = "ws";
    
                        wsClient = UtilsWS.getSharedInstance(protocol + "://" + host + ":" + port);
    
                        wsClient.onMessage(response -> Platform.runLater(() -> wsMessage(response)));
                        wsClient.onError(response -> Platform.runLater(() -> wsError(response)));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return null;
                }
            };
    
            new Thread(connectionTask).start();
        }

    private static void wsMessage(String response) {
        JSONObject msgObj = new JSONObject(response);
        switch (msgObj.getString("type")) {
            case "ping":
                String pong = msgObj.getString("message");
                out.println(pong);
            case "bounce":
                String msg = msgObj.getString("message");
                out.println(msg);
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

                if (line.equalsIgnoreCase("ping")) {
                    JSONObject message = new JSONObject();
                    message.put("type", "ping");
                    message.put("message", "ping");
                } else {
                    System.out.println("Unknown command. Type 'exit' to stop server gracefully.");
                }
            }
        } finally {
            System.out.println("Server stopped.");
        }
    }
}

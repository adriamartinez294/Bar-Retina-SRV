package com.server;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
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

public class Server extends WebSocketServer {

    private Map<WebSocket, String> clients;

    public Server(InetSocketAddress address) {
        super(address);
        clients = new ConcurrentHashMap<>();
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        System.out.println("WebSocket client connected: " + conn);
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        System.out.println("WebSocket client disconnected: " + conn);
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        JSONObject obj = new JSONObject(message);
    
        
        if (obj.has("type")) {
            String type = obj.getString("type");
    
            switch (type) {
                case "ping":
                    JSONObject msg = new JSONObject();
                    msg.put("message", "pong");
                    msg.put("type", "ping");
                    conn.send(msg.toString());
                    break;
                case "bounce":
                    JSONObject msg1 = new JSONObject();

                    String line = obj.getString("message");

                    msg1.put("message", line);
                    msg1.put("type", "bounce");
                    conn.send(msg1.toString());
                    break;
            }
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        ex.printStackTrace();
    }

    @Override
    public void onStart() {
        System.out.println("WebSocket server started on port: " + getPort());
        setConnectionLostTimeout(0);
        setConnectionLostTimeout(100);
    }


    private void broadcastMessage(String message, WebSocket sender) {
        for (Map.Entry<WebSocket, String> entry : clients.entrySet()) {
            WebSocket conn = entry.getKey();
            if (conn != sender) {
                try {
                    conn.send(message);
                } catch (WebsocketNotConnectedException e) {
                    System.out.println("Client " + entry.getValue() + " not connected.");
                    clients.remove(conn);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static String askSystemName() {
        StringBuilder resultat = new StringBuilder();
        String osName = System.getProperty("os.name").toLowerCase();
        try {
            ProcessBuilder processBuilder;
            if (osName.contains("win")) {
                // En Windows
                processBuilder = new ProcessBuilder("cmd.exe", "/c", "ver");
            } else {
                // En sistemas Unix/Linux
                processBuilder = new ProcessBuilder("uname", "-r");
            }
            Process process = processBuilder.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                resultat.append(line).append("\n");
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                return "Error: El proceso ha finalizado con código " + exitCode;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "Error: " + e.getMessage();
        }
        return resultat.toString().trim();
    }


    public static void main(String[] args) {
        String systemName = askSystemName();
    
        // WebSockets server
        Server server = new Server(new InetSocketAddress(3000));
        server.start();
    
        // Verificar si hay un terminal disponible
        Console console = System.console();
        if (console != null) {
            // Solo usa JLine si hay un terminal disponible
            LineReader reader = LineReaderBuilder.builder().build();
            System.out.println("Server running. Type 'exit' to gracefully stop it.");
    
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
    
                    if (line.equalsIgnoreCase("exit")) {
                        System.out.println("Stopping server...");
                        try {
                            server.stop(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        break;
                    } else {
                        System.out.println("Unknown command. Type 'exit' to stop server gracefully.");
                    }
                }
            } finally {
                System.out.println("Server stopped.");
                System.exit(0);
            }
        } else {
            // Modo no interactivo (por ejemplo, cuando se usa nohup)
            System.out.println("Server running in non-interactive mode.");
            // Aquí el servidor seguirá corriendo indefinidamente.
            // Podrías agregar alguna otra forma de detenerlo, como un comando o señal externa.
        }
    }
}
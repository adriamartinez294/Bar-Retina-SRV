package com.server;

import static java.lang.System.out;

import java.awt.image.BufferedImage;
import java.io.*;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import javafx.fxml.FXML;
import javafx.scene.image.Image;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.java_websocket.WebSocket;
import org.java_websocket.exceptions.WebsocketNotConnectedException;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.json.JSONArray;
import org.json.JSONObject;

import org.w3c.dom.*;
import org.json.JSONObject;

import javax.imageio.ImageIO;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;

import org.xml.sax.InputSource;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.dom.DOMSource;


public class Server extends WebSocketServer {

    private String base64Image;
    private Map<WebSocket, String> clients;

    public static Document productesXml;
    private static ArrayList<Element> productes;

    private static ArrayList<String> tags = new ArrayList<>(Arrays.asList("soda", "zero-sugar", "caffeine-free", "water", "non-carbonated", 
            "sparkling water", "isotonic", "beer", "alcohol-free", "baguette", 
            "eggs", "meat", "lactose", "seafood", "spicy", "poultry", 
            "vegetarian", "sandwich", "mixed", "burger", "main", "fish", 
            "starter", "potato"));
    
        public Server(InetSocketAddress address) {
            super(address);
            clients = new ConcurrentHashMap<>();
            productes = new ArrayList<>();
        }

    public static Document readXml(File xml) {
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(xml);
            doc.getDocumentElement().normalize();

            NodeList arrayProducts = doc.getElementsByTagName("product");
            System.out.println("Products: " + arrayProducts.getLength());

            for (int cnt = 0; cnt < arrayProducts.getLength(); cnt++) {
                Node nodeProduct = arrayProducts.item(cnt);
                if (nodeProduct.getNodeType() == Node.ELEMENT_NODE) {
                    Element elm = (Element) nodeProduct;
                }
            }

            return doc;

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public String documentToString(Document doc) throws TransformerException {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(doc), new StreamResult(writer));
        return writer.toString();
    }
    
        @Override
        public void onOpen(WebSocket conn, ClientHandshake handshake) {
            System.out.println("WebSocket client connected: " + conn);
            clients.put(conn, conn.getRemoteSocketAddress().toString());

        }
    
        @Override
        public void onClose(WebSocket conn, int code, String reason, boolean remote) {
            System.out.println("WebSocket client disconnected: " + conn);
            clients.remove(conn);
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
                    case "products":
                        try {
                            String xmlString = documentToString(productesXml);

                            JSONObject msg2 = new JSONObject();
                            msg2.put("message", xmlString);
                            msg2.put("type", "products");

                            conn.send(msg2.toString());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        break;
                    case "tags":
                        JSONObject msg3 = new JSONObject();

                        String tags = printTags();

                        msg3.put("message", tags);
                        msg3.put("type", "tags");

                        conn.send(msg3.toString());
                        break;

                    case "ready":
                        JSONObject msg4 = new JSONObject();

                        String tableMessage = obj.getString("message");
                        out.println(tableMessage);
                        msg4.put("type", "ready");
                        msg4.put("message", tableMessage);
                        broadcastMessage(msg4.toString(), null);
                        out.println("Message sent");
                        break;

                    case "productswithimage":
                        try {
                            // Construir un JSON con los productos y las imágenes en Base64
                            JSONObject msg2 = new JSONObject();
                            msg2.put("type", "productswithimage");

                            NodeList arrayProducts = productesXml.getElementsByTagName("product");
                            JSONArray productsWithImage = new JSONArray();

                            for (int i = 0; i < arrayProducts.getLength(); i++) {
                                Node nodeProduct = arrayProducts.item(i);
                                if (nodeProduct.getNodeType() == Node.ELEMENT_NODE) {
                                    Element product = (Element) nodeProduct;

                                    JSONObject productJson = new JSONObject();
                                    productJson.put("id", product.getAttribute("id"));
                                    productJson.put("tags", product.getAttribute("tags"));
                                    productJson.put("name", getTagValue("name", product));
                                    productJson.put("price", getTagValue("price", product));
                                    productJson.put("description", getTagValue("description", product));

                                    // Procesar la imagen y convertir a Base64
                                    String imageFileName = getTagValue("image", product);
                                    File imageFile = new File(System.getProperty("user.dir") + "/data/imgs/" + imageFileName);
                                    if (imageFile.exists()) {
                                        String base64Image = convertImageToBase64(imageFile);
                                        productJson.put("image", base64Image);
                                    } else {
                                        productJson.put("image", "Image not found");
                                    }
                                    productsWithImage.put(productJson);
                                }
                            }
                            out.println("Enviando datos....");
                            // Agregar productos al mensaje
                            msg2.put("message", productsWithImage);

                            // Enviar el mensaje al cliente
                            conn.send(msg2.toString());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
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

        private static String printTags() {
            StringBuilder output = new StringBuilder();

//            output.append("Tags: \n");

            for (String tag : tags) {
                output.append(tag + "\n");
            }

            return output.toString();
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
    
            String userDir = System.getProperty("user.dir");
            File xml = new File(userDir, "data/products.xml");
    
            productesXml = readXml(xml);
    
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
        }
    }

    public static String convertImageToBase64(File imageURL) {
        try {
            // Leer la imagen desde el archivo (puede ser .jpg o .png)
            BufferedImage bufferedImage = ImageIO.read(imageURL);

            // Crear un ByteArrayOutputStream para almacenar los bytes de la imagen
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            // Escribir la imagen en el ByteArrayOutputStream (puede ser .png o .jpg)
            String formatName = imageURL.getName().toLowerCase().endsWith(".png") ? "png" : "jpg";
            ImageIO.write(bufferedImage, formatName, outputStream);

            // Obtener el arreglo de bytes
            byte[] imageBytes = outputStream.toByteArray();

            // Codificar los bytes a Base64
            return Base64.getEncoder().encodeToString(imageBytes);

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static String getTagValue(String tag, Element element) {
        NodeList nodeList = element.getElementsByTagName(tag);
        if (nodeList.getLength() > 0) {
            return nodeList.item(0).getTextContent();
        }
        return null;
    }

}
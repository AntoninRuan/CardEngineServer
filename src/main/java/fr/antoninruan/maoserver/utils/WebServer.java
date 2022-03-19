package fr.antoninruan.maoserver.utils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.StringTokenizer;

public class WebServer {

    private static ServerSocket serverSocket;
    private static File f = new File("");
    protected static boolean run;

    public static void startWebServer(String serverAdress) {
        run = true;

        Thread serverThread = new Thread(() -> {
            try {
                serverSocket = new ServerSocket(5673, 5, InetAddress.getByName(serverAdress));
                System.out.println(serverSocket.getInetAddress().getHostAddress());
                serverSocket.setSoTimeout(1000);
                while (run) {
                    handleConnection(serverSocket);
                }
                serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }


        }, "WebServer");
        serverThread.start();
        System.out.println("Server started");

    }

    private static void handleConnection(ServerSocket serverSocket) {
        try (Socket socket = serverSocket.accept();
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))){

            StringTokenizer tokenizer = new StringTokenizer(reader.readLine());

            String method = tokenizer.nextToken().toUpperCase();

            if(method.equals("GET") || method.equals("HEAD")) {

                String request = tokenizer.nextToken().replace("%22", "\"").replace("%20", " ");
                String[] parsedRequest = request.split("\\?");

                String fileName = parsedRequest[0];
                String param = parsedRequest.length >= 2 ? parsedRequest[1] : "";

                try {
                    System.out.println(fileName);

                    if(fileName.startsWith("/emotes")) {
                        File file = new File(f.getAbsolutePath() + fileName);
                        System.out.println(file.getAbsolutePath());
                        if (file.exists() && file.getName().endsWith(".png")) {
                            FileReader fileReader = new FileReader(file);

                            ByteArrayOutputStream bots = new ByteArrayOutputStream();
                            BufferedImage image = ImageIO.read(file);
                            ImageIO.write(image, "png", bots);
                            writeHttpHeader(out, "200 OK", "image/png", bots.size());

                            socket.getOutputStream().write(bots.toByteArray());

                            socket.getOutputStream().flush();
                        } else {
                            String response = "Fichier non trouvé";
                            writeHttpHeader(out, "404 Not Found", "text/plain", response.length());
                            out.println(response);
                        }
                    } else {
                        String response = "Noulle";
                        writeHttpHeader(out, "404 Not Found", "text/plain", response.length());
                        out.println(response);
                    }

                } catch (Exception e) {
                    String response = "Une erreur s'est produite lors du traitement de la requête";

                    writeHttpHeader(out, "500 Internal Server Error", "text/plain", response.length());

                    socket.getOutputStream().write(response.getBytes(StandardCharsets.UTF_8));
                    socket.getOutputStream().flush();
                    e.printStackTrace();
                }


            } else {

                String response = "Méthode non supportée";

                writeHttpHeader(out, "501 Not Implemented", "text/plain", response.length());

                socket.getOutputStream().write(response.getBytes(StandardCharsets.UTF_8));
                socket.getOutputStream().flush();

            }


        } catch (SocketTimeoutException ignored){

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void stopWebServer() {
        run = false;
    }

    private static void writeHttpHeader(PrintWriter out, String status, String contentType, long contentLength) {
        out.println("HTTP/1.1 " + status);
        out.println("Server: MaoServer");
        out.println("Date: " + new Date());
        out.println("Content-type: " + contentType);
        out.println("Content-length: " + contentLength);
        out.println();
        out.flush();
    }

}

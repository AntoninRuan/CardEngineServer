package fr.antoninruan.maoserver;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import fr.antoninruan.maoserver.model.Card;
import fr.antoninruan.maoserver.model.Deck;
import fr.antoninruan.maoserver.model.Hand;
import fr.antoninruan.maoserver.model.PlayedStack;
import fr.antoninruan.maoserver.utils.RabbitMQManager;
import org.apache.commons.cli.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

public class Main {

    private static int lastPlayerId;

    private static HashMap<Integer, Hand> players = new HashMap<>();

    public static void main(String... args) {

        Options options = new Options();

        Option host = new Option("h", "host", true, "RabbitMQ Server Host");
        host.setRequired(true);
        options.addOption(host);

        Option port = new Option("p", "port", true, "RabbitMQ Server Port");
        port.setRequired(true);
        options.addOption(port);

        Option user = new Option("u", "user", true, "RabbitMQ Server User");
        user.setRequired(true);
        options.addOption(user);

        Option password = new Option("pw", "password", true, "RabbitMQ Server Password");
        password.setRequired(true);
        options.addOption(password);

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd;

        try {
            cmd = parser.parse(options, args);
            Deck.init();
            RabbitMQManager.init(cmd.getOptionValue("h"), Integer.parseInt(cmd.getOptionValue("p")), cmd.getOptionValue("u"), cmd.getOptionValue("pw"));

            log("Server prêt");

            startShell();

            RabbitMQManager.close();
            System.exit(0);

        } catch (ParseException e) {
            log(e.getMessage());
            formatter.printHelp("utility-name", options);

            System.exit(0);
        }


    }

    public static void log(String message) {
        System.out.println("\r" + message);
        System.out.print(">");
    }

    private static void startShell() {
        HelpFormatter formatter = new HelpFormatter();
        CommandLineParser parser = new DefaultParser();
        Scanner scanner = new Scanner(System.in);
        boolean stop = false;
        while (!stop) {
            System.out.print(">");
            String line = scanner.nextLine();
            String[] command = line.split(" ");
            if(command[0].equals("stop")) {
                stop = true;
            } else if(command[0].equals("gameinfo")) {
                Options gameInfo = new Options();

                Option players = new Option("p", "player", false, "Affiche les joueurs connectés");
                Option deck = new Option("d", "deck", false, "Affiche l'état actuel de la pioche");
                Option played = new Option("j", "played", false, "Affiche les cartes jouées");
                Option all = new Option("a", "all", false, "Affiche tous les informations du jeu");

                gameInfo.addOption(players);
                gameInfo.addOption(deck);
                gameInfo.addOption(played);
                gameInfo.addOption(all);

                try {
                    CommandLine commandLine = parser.parse(gameInfo, command);

                    if(commandLine.hasOption("a")) {
                        displayPlayers();
                        displayDeck();
                        displayPlayed();
                    } else {
                        if(commandLine.hasOption("d")) {
                            displayDeck();
                        } else if (commandLine.hasOption("j")) {
                            displayPlayed();
                        } else if(commandLine.hasOption("p")) {
                            displayPlayers();
                        }
                    }
                } catch (ParseException e) {
                    formatter.printHelp("utility-name", gameInfo);
                }
            }
        }
    }

    private static void displayPlayers() {
        System.out.print("Player connected: ");
        JsonArray array = new JsonArray();
        for (Hand h : Main.getPlayers().values()) {
            JsonObject player = new JsonObject();
            player.addProperty("id", h.getId());
            player.addProperty("name", h.getName());
            array.add(player);
        }
        System.out.println(array.toString());
    }

    private static void displayDeck() {
        System.out.print("Deck: ");
        JsonArray deck = new JsonArray();
        for (Card c : Deck.getCards()) {
            JsonObject card = new JsonObject();
            card.addProperty("value", c.getValue().toString());
            card.addProperty("suit", c.getSuit().toString());
            deck.add(card);
        }
        System.out.println(deck.toString());
    }

    private static void displayPlayed() {
        System.out.print("Player: ");
        JsonArray playedStack = new JsonArray();
        for (Card c : PlayedStack.getCards()) {
            JsonObject card = new JsonObject();
            card.addProperty("value", c.getValue().toString());
            card.addProperty("suit", c.getSuit().toString());
            playedStack.add(card);
        }
        System.out.println(playedStack.toString());
    }

    public static HashMap<Integer, Hand> getPlayers() {
        return players;
    }

    public static int nextPlayerId() {
        return lastPlayerId ++;
    }

}

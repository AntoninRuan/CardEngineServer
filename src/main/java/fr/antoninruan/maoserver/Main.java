package fr.antoninruan.maoserver;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import fr.antoninruan.maoserver.model.Card;
import fr.antoninruan.maoserver.model.cardcontainer.Deck;
import fr.antoninruan.maoserver.model.cardcontainer.Hand;
import fr.antoninruan.maoserver.model.cardcontainer.PlayedStack;
import fr.antoninruan.maoserver.utils.RabbitMQManager;
import org.apache.commons.cli.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

public class Main {

    private static int lastPlayerId;

    private static final ArrayList<Hand> players = new ArrayList<>();
    private static final Deck deck = new Deck();
    private static final PlayedStack playedStack = new PlayedStack();

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
            deck.init();
            RabbitMQManager.init(cmd.getOptionValue("h"), Integer.parseInt(cmd.getOptionValue("p")), cmd.getOptionValue("u"), cmd.getOptionValue("pw"));

            log("Server prêt");

            startShell();

            RabbitMQManager.close();
            System.exit(0);

        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("utility-name", options);

            System.exit(0);
        }


    }

    public static void log(String message) {
        System.out.println("\r" + message);
        System.out.print("> ");
    }

    private static void startShell() {
        HelpFormatter formatter = new HelpFormatter();
        CommandLineParser parser = new DefaultParser();
        Scanner scanner = new Scanner(System.in);
        boolean stop = false;
        while (!stop) {
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
                Option lastId = new Option("l", "last_id", false, "Affiche le prochain id à attribuer");

                gameInfo.addOption(players);
                gameInfo.addOption(deck);
                gameInfo.addOption(played);
                gameInfo.addOption(all);
                gameInfo.addOption(lastId);

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
                    if(commandLine.hasOption("l")) {
                        System.out.println("LastPlayerId=" + lastPlayerId);
                    }

                } catch (ParseException e) {
                    formatter.printHelp("utility-name", gameInfo);
                }
                System.out.print("> ");
            } else if (command[0].equals("kick")) {
                Options kick = new Options();

                Option id = new Option("i", "id", true, "The id of the player to kick");
                id.setRequired(true);
                kick.addOption(id);

                try {
                    CommandLine commandLine = parser.parse(kick, command);

                    int i = Integer.parseInt(commandLine.getOptionValue("i"));
                    RabbitMQManager.kickPlayer(i);
                } catch (ParseException | NumberFormatException e) {
                    formatter.printHelp("utility-name", kick);
                    System.out.println("> ");
                } catch (IOException e) {
                    e.printStackTrace();
                    System.out.println("> ");
                }
            }
        }
    }

    private static void displayPlayers() {
        System.out.print("Player connected: ");
        JsonArray array = new JsonArray();
        for (Hand h : Main.getPlayers()) {
            JsonObject player = new JsonObject();
            player.addProperty("id", h.getId());
            player.addProperty("name", h.getName());
            array.add(player);
        }
        System.out.println(array);
    }

    private static void displayDeck() {
        System.out.print("Deck: ");
        JsonArray deck = new JsonArray();
        for (Card c : Main.getDeck().getCards()) {
            JsonObject card = new JsonObject();
            card.addProperty("value", c.getValue().toString());
            card.addProperty("suit", c.getSuit().toString());
            deck.add(card);
        }
        System.out.println(deck);
    }

    private static void displayPlayed() {
        System.out.print("Played card: ");
        JsonArray playedStack = new JsonArray();
        for (Card c : Main.getPlayedStack().getCards()) {
            JsonObject card = new JsonObject();
            card.addProperty("value", c.getValue().toString());
            card.addProperty("suit", c.getSuit().toString());
            playedStack.add(card);
        }
        System.out.println(playedStack);
    }

    public static ArrayList<Hand> getPlayers() {
        return players;
    }

    public static void setLastPlayerId(int lastPlayerId) {
        Main.lastPlayerId = lastPlayerId;
    }

    public static int getLastPlayerId() {
        return lastPlayerId;
    }

    public static int nextPlayerId() {
        return lastPlayerId ++;
    }

    public static Deck getDeck() {
        return deck;
    }

    public static PlayedStack getPlayedStack() {
        return playedStack;
    }

    public static JsonObject getGameState() {
        JsonObject object = new JsonObject();

        object.add("deck", Main.getDeck().toJsonArray());

        JsonArray playedStack = new JsonArray();
        for (Card c : Main.getPlayedStack().getCards()) {
            JsonObject card = new JsonObject();
            card.addProperty("value", c.getValue().toString());
            card.addProperty("suit", c.getSuit().toString());
            playedStack.add(card);
        }
        object.add("played_stack", playedStack);

        JsonArray players = new JsonArray();

        for (Hand h : Main.getPlayers()) {
            JsonObject player = new JsonObject();
            player.addProperty("id", h.getId());
            player.addProperty("name", h.getName());
            JsonArray cards = new JsonArray();
            for (Card c : h.getCards()) {
                JsonObject card = new JsonObject();
                card.addProperty("value", c.getValue().toString());
                card.addProperty("suit", c.getSuit().toString());
                cards.add(card);
            }
            player.add("cards", cards);
            players.add(player);
        }

        object.add("players", players);

        return object;
    }

}

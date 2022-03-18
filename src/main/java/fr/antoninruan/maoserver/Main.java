package fr.antoninruan.maoserver;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import fr.antoninruan.maoserver.model.Card;
import fr.antoninruan.maoserver.model.cardcontainer.Deck;
import fr.antoninruan.maoserver.model.cardcontainer.Hand;
import fr.antoninruan.maoserver.model.cardcontainer.PlayedStack;
import fr.antoninruan.maoserver.utils.RabbitMQManager;
import fr.antoninruan.maoserver.utils.shell.CommandHandler;
import fr.antoninruan.maoserver.utils.shell.CustomPrintStream;
import org.apache.commons.cli.*;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

public class Main {

    private static int lastPlayerId;
    private static boolean stop = false;

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

            System.out.println("Server prêt");

            Terminal terminal = TerminalBuilder.builder().name("mao-server").build();
            LineReader lineReader = LineReaderBuilder.builder().terminal(terminal).build();

            CustomPrintStream printStream = new CustomPrintStream(System.out, lineReader);
            System.setOut(printStream);

            while (!stop) {
                String line = lineReader.readLine("> ");
                if (line == null) {
                    System.exit(0);
                } else
                    CommandHandler.handleCommand(line.split(" ")[0], line.split(" "));
            }

            RabbitMQManager.close();
            System.exit(0);

        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("utility-name", options);

            System.exit(0);
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    public static void stop() {
        stop = true;
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

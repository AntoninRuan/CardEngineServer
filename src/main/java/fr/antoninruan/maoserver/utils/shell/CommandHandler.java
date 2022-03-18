package fr.antoninruan.maoserver.utils.shell;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import fr.antoninruan.maoserver.Main;
import fr.antoninruan.maoserver.model.Card;
import fr.antoninruan.maoserver.model.cardcontainer.Hand;
import fr.antoninruan.maoserver.utils.RabbitMQManager;
import org.apache.commons.cli.*;

import java.io.IOException;

public class CommandHandler {

    private static final HelpFormatter formatter = new HelpFormatter();
    private static final CommandLineParser parser = new DefaultParser();

    public static void handleCommand(String cmd, String[] args) {
        switch (cmd) {
            case "stop" -> Main.stop();
            case "gameinfo" -> {
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
                    CommandLine commandLine = parser.parse(gameInfo, args);

                    if (commandLine.hasOption("a")) {
                        displayPlayers();
                        displayDeck();
                        displayPlayed();

                    } else {
                        if (commandLine.hasOption("d")) {
                            displayDeck();
                        } else if (commandLine.hasOption("j")) {
                            displayPlayed();
                        } else if (commandLine.hasOption("p")) {
                            displayPlayers();
                        }
                    }
                    if (commandLine.hasOption("l")) {
                        System.out.println("LastPlayerId=" + Main.getLastPlayerId());
                    }

                } catch (ParseException e) {
                    formatter.printHelp("utility-name", gameInfo);
                }
                System.out.print("> ");
            }
            case "kick" -> {
                Options kick = new Options();
                Option id = new Option("i", "id", true, "The id of the player to kick");
                id.setRequired(true);
                kick.addOption(id);
                try {
                    CommandLine commandLine = parser.parse(kick, args);

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
            case "help" -> {
                System.out.println("gameinfo: Permet d'accéder aux différentes informations de la partie");
                System.out.println("kick: Permet d'exclure un joueur de la partie");
                System.out.println("stop: Arrête le serveur");
            }
            default ->  {
                System.out.println("Unknown command, type help for more information");
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


}

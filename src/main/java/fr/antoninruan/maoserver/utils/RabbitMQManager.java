package fr.antoninruan.maoserver.utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.rabbitmq.client.*;
import fr.antoninruan.maoserver.Main;
import fr.antoninruan.maoserver.model.Card;
import fr.antoninruan.maoserver.model.Deck;
import fr.antoninruan.maoserver.model.Hand;
import fr.antoninruan.maoserver.model.PlayedStack;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class RabbitMQManager {

    private static final String EXCHANGE_GAME_UPDATES = "game_updates";
    private static final String QUEUE_GAME_ACTION = "game_actions";
    private static final String RPC_QUEUE_CONNECTION = "connection_queue";

    private static Channel channel;
    private static Connection connection;

    public static void init(String host, int port, String user, String password) {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(host);
        factory.setPort(port);
        factory.setUsername(user);
        factory.setPassword(password);
        factory.setAutomaticRecoveryEnabled(true);
        try {
            connection = factory.newConnection();
            channel = connection.createChannel();
            channel.addShutdownListener(e -> {
                if(!e.getMessage().contains("clean channel shutdown")) {
                    e.printStackTrace();
                }
            });
            channel.exchangeDeclare(EXCHANGE_GAME_UPDATES, "fanout");
            channel.queueDeclare(QUEUE_GAME_ACTION, true, false, false, null);
            channel.queueDeclare(RPC_QUEUE_CONNECTION, false, false, false, null);
            channel.queuePurge(RPC_QUEUE_CONNECTION);

            listenGameActions();
            listenConnection();
        } catch (TimeoutException | IOException e) {
            e.printStackTrace();
        }
    }

    public static void close() {
        try {
            channel.close();
            connection.close();
        } catch (IOException | TimeoutException e) {
            e.printStackTrace();
        }
    }

    public static void sendGameUpdates(String update) throws IOException {
        Main.log("Sending: " + update);
        channel.basicPublish(EXCHANGE_GAME_UPDATES, "", null, update.getBytes(StandardCharsets.UTF_8));
    }

    private static void listenConnection() throws IOException {
        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            Main.log("Connection received");
            AMQP.BasicProperties replyProps = new AMQP.BasicProperties
                    .Builder()
                    .correlationId(delivery.getProperties().getCorrelationId())
                    .build();

            String response = "";
            String broadcast = "";
            try {
                String name = new String(delivery.getBody(), StandardCharsets.UTF_8); // {name}

                if(Main.getPlayers().size() > 7) {
                    response = "{\"id\":-1}";
                } else {
                    Hand hand = new Hand(Main.nextPlayerId(), name);

                    JsonObject newPlayer = new JsonObject();
                    newPlayer.addProperty("type", "new_player");
                    newPlayer.addProperty("name", name);
                    newPlayer.addProperty("id", hand.getId());
                    broadcast = newPlayer.toString();

//                System.out.println("id=" + hand.getId());

                    JsonObject object = new JsonObject();
                    object.addProperty("id", hand.getId());

                    JsonArray deck = new JsonArray();
                    for (Card c : Deck.getCards()) {
                        JsonObject card = new JsonObject();
                        card.addProperty("value", c.getValue().toString());
                        card.addProperty("suit", c.getSuit().toString());
                        deck.add(card);
                    }
                    object.add("deck", deck);

                    JsonArray playedStack = new JsonArray();
                    for (Card c : PlayedStack.getCards()) {
                        JsonObject card = new JsonObject();
                        card.addProperty("value", c.getValue().toString());
                        card.addProperty("suit", c.getSuit().toString());
                        playedStack.add(card);
                    }
                    object.add("played_stack", playedStack);

                    JsonArray players = new JsonArray();

                    for (Hand h : Main.getPlayers().values()) {
                        JsonObject player = new JsonObject();
                        player.addProperty("id", h.getId());
                        player.addProperty("name", h.getName());
                        players.add(player);
                    }

                    object.add("players", players);


                    Main.getPlayers().put(hand.getId(), hand);

                    response = object.toString();
                }

//                System.out.println(response.toString());
            } catch (RuntimeException e) {
                e.printStackTrace();
            } finally {
                Main.log("Sending: " + response);
                channel.basicPublish("", delivery.getProperties().getReplyTo(), replyProps, response.getBytes(StandardCharsets.UTF_8));
//                System.out.println("sending: " + response);
                channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);
                final String finalBroadcast = broadcast;
                executor.schedule(() -> {
                    try {
                        channel.basicPublish(EXCHANGE_GAME_UPDATES, "new_player", null, finalBroadcast.getBytes(StandardCharsets.UTF_8));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }, 100, TimeUnit.MILLISECONDS);
            }
        };

        channel.basicConsume(RPC_QUEUE_CONNECTION, false, deliverCallback, (consumerTag -> { }));
    }

    private static void listenGameActions() throws IOException {

        DeliverCallback deliver = (consumerTag, delivery) -> {
            try {
                JsonObject message = JsonParser.parseString(new String(delivery.getBody(), StandardCharsets.UTF_8)).getAsJsonObject();
                Main.log("Receive: " + message.toString());
                String type = message.get("type").getAsString();
                if(type.equals("card_move")) {
                    String dest = message.get("destination").getAsString();
                    String from = message.get("source").getAsString();
                    if(from.equals("deck")) {
                        Card card = Deck.draw();
                        if(card != null) {
                            if(dest.equals("playedStack")) {
                                PlayedStack.addCard(card);
                            } else {
                                int destId = Integer.parseInt(dest);
                                Hand hand = Main.getPlayers().get(destId);
                                hand.add(card);
                            }
                            sendGameUpdates(message.toString());
                        }
                    } else if (from.equals("playedStack")) {
                        Card card = PlayedStack.pickLastCard();
                        if(card != null) {
                            if(dest.equals("deck")) {
                                Deck.put(card);
                            } else {
                                int destId = Integer.parseInt(dest);
                                Hand hand = Main.getPlayers().get(destId);
                                hand.add(card);
                            }
                            sendGameUpdates(message.toString());
                        }
                    } else {
                        Hand hand = Main.getPlayers().get(Integer.parseInt(from));
                        Card card = hand.getCard(message.get("card_id").getAsInt());
                        if(dest.equals("deck")) {
                            Deck.put(card);
                            sendGameUpdates(message.toString());
                            hand.remove(card);
                        } else if (dest.equals("playedStack")) {
                            PlayedStack.addCard(card);
                            sendGameUpdates(message.toString());
                            hand.remove(card);
                        } else {
                            int destId = Integer.parseInt(dest);
                            Hand target = Main.getPlayers().get(destId);
                            target.add(card);
                            sendGameUpdates(message.toString());
                            hand.remove(card);
                        }
                    }
                } else if(type.equals("shuffle")) {
                    Deck.shuffle();
                    JsonObject update = new JsonObject();
                    update.addProperty("type", "shuffle");
                    JsonArray deck = new JsonArray();
                    for (Card c : Deck.getCards()) {
                        JsonObject card = new JsonObject();
                        card.addProperty("value", c.getValue().toString());
                        card.addProperty("suit", c.getSuit().toString());
                        deck.add(card);
                    }
                    update.add("deck", deck);
                    sendGameUpdates(update.toString());

                } else if (type.equals("rollback")) {
                    for (Card c : new ArrayList<>(PlayedStack.getCards())) {
                        PlayedStack.getCards().remove(c);
                        Deck.put(c);
                    }
                    JsonObject update = new JsonObject();
                    update.addProperty("type", "rollback");
                    JsonArray deck = new JsonArray();
                    for (Card c : Deck.getCards()) {
                        JsonObject card = new JsonObject();
                        card.addProperty("value", c.getValue().toString());
                        card.addProperty("suit", c.getSuit().toString());
                        deck.add(card);
                    }
                    update.add("deck", deck);
                    sendGameUpdates(update.toString());
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        };

        channel.basicConsume(QUEUE_GAME_ACTION, true, deliver, consumerTag -> {});
    }

}

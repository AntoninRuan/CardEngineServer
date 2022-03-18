package fr.antoninruan.maoserver.utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.rabbitmq.client.*;
import fr.antoninruan.maoserver.Main;
import fr.antoninruan.maoserver.model.Card;
import fr.antoninruan.maoserver.model.cardcontainer.Hand;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class RabbitMQManager {

    private static final String EXCHANGE_GAME_UPDATES = "game_updates";
    private static final String QUEUE_GAME_ACTION = "game_actions";
    private static final String EXCHANGE_CHAT_UPDATE = "chat_update";
    private static final String QUEUE_MESSAGE_SENDING = "message_sending";
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
//        factory.setVirtualHost("card_engine");
        try {
            connection = factory.newConnection();
            channel = connection.createChannel();
            channel.addShutdownListener(e -> {
                if(!e.getMessage().contains("clean channel shutdown")) {
                    e.printStackTrace();
                }
            });
            channel.exchangeDeclare(EXCHANGE_GAME_UPDATES, "fanout");
            channel.exchangeDeclare(EXCHANGE_CHAT_UPDATE, "fanout");
            channel.queueDeclare(QUEUE_GAME_ACTION, true, false, false, null);
            channel.queueDeclare(QUEUE_MESSAGE_SENDING, true, false, false, null);
            channel.queueDeclare(RPC_QUEUE_CONNECTION, false, false, false, null);
            channel.queuePurge(RPC_QUEUE_CONNECTION);
            channel.queuePurge(QUEUE_GAME_ACTION);
            channel.queuePurge(QUEUE_MESSAGE_SENDING);

            listenGameActions();
            listenConnection();
            listenMessageSending();
        } catch (AuthenticationFailureException e) {
            e.printStackTrace();
            System.exit(-1);
        } catch (TimeoutException | IOException e) {
            e.printStackTrace();
            System.exit(-2);
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

    public static void sendGameUpdate(JsonObject update) throws IOException {
        System.out.println("Sending: (GameUpdate) " + update);
        channel.basicPublish(EXCHANGE_GAME_UPDATES, "", null, update.toString().getBytes(StandardCharsets.UTF_8));
    }

    public static void sendChatUpdate(JsonObject update) throws IOException {
        System.out.println("Sending: (ChatUpdate) " + update);
        channel.basicPublish(EXCHANGE_CHAT_UPDATE, "", null, update.toString().getBytes(StandardCharsets.UTF_8));
    }

    private static void listenConnection() throws IOException {
        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            System.out.println("Connection received");
            AMQP.BasicProperties replyProps = new AMQP.BasicProperties
                    .Builder()
                    .correlationId(delivery.getProperties().getCorrelationId())
                    .build();

            String response = "";
            String broadcast = "";
            try {
                String name = new String(delivery.getBody(), StandardCharsets.UTF_8);

                if(Main.getPlayers().size() > 7) {
                    response = "{\"id\":-1}";
                } else {
                    Hand hand = new Hand(Main.nextPlayerId(), name);

                    JsonObject newPlayer = new JsonObject();
                    newPlayer.addProperty("type", "new_player");
                    newPlayer.addProperty("name", name);
                    newPlayer.addProperty("id", hand.getId());
                    broadcast = newPlayer.toString();

                    JsonObject object = Main.getGameState();
                    object.addProperty("id", hand.getId());

                    Main.getPlayers().add(hand);

                    response = object.toString();
                }

            } catch (RuntimeException e) {
                e.printStackTrace();
            } finally {
                System.out.println("Sending: " + response);
                channel.basicPublish("", delivery.getProperties().getReplyTo(), replyProps, response.getBytes(StandardCharsets.UTF_8));
                channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);
                final String finalBroadcast = broadcast;
                executor.schedule(() -> {
                    try {
                        System.out.println("Sending: " + finalBroadcast);
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
                System.out.println("Receive: (GameAction)" + message.toString());
                String type = message.get("type").getAsString();
                switch (type) {
                    case "card_move" -> {
                        String dest = message.get("destination").getAsString();
                        String from = message.get("source").getAsString();
                        if (from.equals("deck")) {
                            Card card = Main.getDeck().draw();
                            if (card != null) {
                                if (dest.equals("playedStack")) {
                                    Main.getPlayedStack().add(card);
                                } else {
                                    int destId = Integer.parseInt(dest);
                                    Hand hand = Main.getPlayers().get(destId);
                                    hand.add(card);
                                }
                                message.add("moved_card", card.toJson());
                                sendGameUpdate(message);
                            }
                        } else if (from.equals("playedStack")) {
                            Card card = Main.getPlayedStack().pickLastCard();
                            if (card != null) {
                                if (dest.equals("deck")) {
                                    Main.getDeck().put(card);
                                } else {
                                    int destId = Integer.parseInt(dest);
                                    Hand hand = Main.getPlayers().get(destId);
                                    hand.add(card);
                                }
                                message.add("moved_card", card.toJson());
                                sendGameUpdate(message);
                            }
                        } else {
                            Hand hand = Main.getPlayers().get(Integer.parseInt(from));
                            Card card = hand.getCard(message.get("card_id").getAsInt());
                            if (dest.equals("deck")) {
                                Main.getDeck().put(card);
                                message.add("moved_card", card.toJson());
                                sendGameUpdate(message);
                                hand.remove(card);
                            } else if (dest.equals("playedStack")) {
                                Main.getPlayedStack().add(card);
                                message.add("moved_card", card.toJson());
                                sendGameUpdate(message);
                                hand.remove(card);
                            } else {
                                int destId = Integer.parseInt(dest);
                                Hand target = Main.getPlayers().get(destId);
                                target.add(card);
                                message.add("moved_card", card.toJson());
                                sendGameUpdate(message);
                                hand.remove(card);
                            }
                        }
                    }
                    case "shuffle" -> {
                        Main.getDeck().shuffle();
                        JsonObject update = new JsonObject();
                        update.addProperty("type", "shuffle");
                        JsonArray deck = new JsonArray();
                        for (Card c : Main.getDeck().getCards()) {
                            JsonObject card = new JsonObject();
                            card.addProperty("value", c.getValue().toString());
                            card.addProperty("suit", c.getSuit().toString());
                            deck.add(card);
                        }
                        update.add("deck", deck);
                        sendGameUpdate(update);
                    }
                    case "rollback" -> {
                        for (Card c : new ArrayList<>(Main.getPlayedStack().getCards())) {
                            Main.getPlayedStack().remove(c);
                            Main.getDeck().put(c);
                        }
                        JsonObject update = new JsonObject();
                        update.addProperty("type", "rollback");
                        update.add("deck", Main.getDeck().toJsonArray());
                        sendGameUpdate(update);
                    }
                    case "knock", "rub" -> sendGameUpdate(message);
                    case "player_leave" -> {
                        int leaveId = message.get("id").getAsInt();
                        kickPlayer(leaveId);
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        };

        channel.basicConsume(QUEUE_GAME_ACTION, true, deliver, consumerTag -> {});
    }

    private static void listenMessageSending() throws IOException {
        DeliverCallback deliver = (consumerTag, delivery) -> {
            try {
                JsonObject object = JsonParser.parseString(new String(delivery.getBody(), StandardCharsets.UTF_8)).getAsJsonObject();
                System.out.println("Receive: (ChatMessage) " + object.toString());
                sendChatUpdate(object);
            } catch (Exception e) {
                e.printStackTrace();
            }
        };

        channel.basicConsume(QUEUE_MESSAGE_SENDING, true, deliver, consumerTag ->  {});
    }

    public static void kickPlayer(int leaveId) throws IOException {
        Main.setLastPlayerId(Main.getLastPlayerId() - 1);
        Hand hand = Main.getPlayers().get(leaveId);
        for (Card card : new ArrayList<>(hand.getCards())) {
            hand.remove(card);
            Main.getDeck().put(card);
        }
        Main.getPlayers().remove(leaveId);
        for (Hand h : Main.getPlayers()) {
            if (h.getId() > leaveId) {
                h.setId(h.getId() - 1);
            }
        }
        JsonObject update = new JsonObject();
        update.addProperty("type", "player_leave");
        update.addProperty("id", leaveId);
        update.add("deck", Main.getDeck().toJsonArray());
        sendGameUpdate(update);
    }


}

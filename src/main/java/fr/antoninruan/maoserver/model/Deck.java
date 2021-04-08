package fr.antoninruan.maoserver.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Deck {

    private static ArrayList<Card> deck = new ArrayList<>();

    public static void init() {
        for (Card.Suit s : Card.Suit.values()) {
            for (Card.Value v : Card.Value.values()) {
                deck.add(new Card(s, v));
            }
        }
    }

    public static void shuffle() {
        Collections.shuffle(deck);
    }

    public static ArrayList<Card> getCards() {
        return deck;
    }

    public static int getSize() {
        return deck.size();
    }

    public static Card draw() {
        if (deck.isEmpty())
            return null;

        Card card = deck.get(deck.size() - 1);
        deck.remove(deck.size() - 1);
        return card;
    }

    public static void put(Card card) {
        deck.add(card);
    }

    public static JsonArray toJsonArray() {
        JsonArray deck = new JsonArray();
        for (Card c : Deck.getCards()) {
            JsonObject card = new JsonObject();
            card.addProperty("value", c.getValue().toString());
            card.addProperty("suit", c.getSuit().toString());
            deck.add(card);
        }
        return deck;
    }

}

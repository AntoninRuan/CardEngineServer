package fr.antoninruan.maoserver.model.cardcontainer;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import fr.antoninruan.maoserver.model.Card;

import java.util.Collections;

public class Deck extends CardContainer {

    public void init() {
        for (Card.Suit s : Card.Suit.values()) {
            for (Card.Value v : Card.Value.values()) {
                keys.add(new Card(s, v));
            }
        }
    }

    public void shuffle() {
        Collections.shuffle(keys);
    }

    public Card draw() {
        if (keys.isEmpty())
            return null;

        Card card = keys.get(keys.size() - 1);
        keys.remove(keys.size() - 1);
        return card;
    }

    public void put(Card card) {
        keys.add(card);
    }

    public JsonArray toJsonArray() {
        JsonArray deck = new JsonArray();
        for (Card c : keys) {
            JsonObject card = new JsonObject();
            card.addProperty("value", c.getValue().toString());
            card.addProperty("suit", c.getSuit().toString());
            deck.add(card);
        }
        return deck;
    }

}

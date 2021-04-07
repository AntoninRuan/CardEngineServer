package fr.antoninruan.maoserver.model;

import java.util.ArrayList;

public class PlayedStack {

    private static ArrayList<Card> keys = new ArrayList<>();

    public static ArrayList<Card> getCards() {
        return keys;
    }

    public static void addCard(Card card) {
        keys.add(card);
    }

    public static Card pickLastCard() {
        if(keys.isEmpty())
            return null;

        Card card = keys.get(keys.size() - 1);
        keys.remove(card);
        return card;
    }

}

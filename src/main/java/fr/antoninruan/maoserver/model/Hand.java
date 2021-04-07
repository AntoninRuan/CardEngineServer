package fr.antoninruan.maoserver.model;

import java.util.ArrayList;


public class Hand {

    private final int id;

    private String name;
    private ArrayList<Card> keys = new ArrayList<>();

    public Hand(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void add(Card card) {
        keys.add(card);
    }

    public void remove(Card card) {
        keys.remove(card);
    }

    public int getCardId(Card card) {
        return keys.indexOf(card);
    }

    public Card getCard(int i) {
        return keys.get(i);
    }


}

